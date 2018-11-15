/*
 * Copyright (c) 2018 Leonardo Pessoa
 * https://github.com/lmpessoa/java-services
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lmpessoa.services.util.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * A Handler that publishes log messages to a remote syslog server.
 * <p>
 * <b>Configuration:</b><br/>
 * Syslog handlers expect to be informed at least the {@code host} (as an IP address or
 * fully-qualified domain name) of the syslog server to which messages ate to be sent to. If no
 * {@code port} is defined, the default port ({@code 514}) will be used.
 * </p>
 * <p>
 * The syslog handler support messages to be transported both via {@code UDP} (old style and
 * unreliable) or {@code TCP} (more modern and reliable). By default, messages are transported via
 * {@code TCP} but can be changed through the {@code transport} parameter on the application
 * settings file.
 * </p>
 * <p>
 * Also messages are sent by default using a more modern format defined by {@code IETF} (see
 * <a href=" https://tools.ietf.org/html/rfc5424">RFC 5424</a>) but can be reverted to old style
 * {@code BSD} format (see <a href=" https://tools.ietf.org/html/rfc3164">RCF 3164</a>) using the
 * {@code format} parameter in the application settings file.
 * </p>
 * <p>
 * Unless it is a very specific requirement, we strongly recommend sticking to the default
 * {@code transport} and {@code format}.
 * </p>
 * <p>
 * Messages sent with the {@code SyslogHandler} can only be directed to the local facilities of the
 * syslog specification. The specific facility can be defined using the {@code facility} parameter,
 * which accepts values from 0 through 7 (inclusive).
 * </p>
 * <p>
 * The following is an example of configuration for a syslog handler to send messages at or above
 * severity {@code ERROR} to a remote host listening on port 1514 using encryption:
 * </p>
 *
 * <pre>
 * log:
 * - type: syslog
 *   above: error
 *   host: stats.leeow.io
 *   port: 1514
 *   secure: yes
 * </pre>
 */
public final class SyslogHandler extends Handler {

   public static final Charset UTF_8 = Charset.forName("UTF-8");
   public static final int DEFAULT_PORT = 514;

   private static final Map<Severity, Integer> numeric;
   private static final int RFC_5424_VERSION = 1;

   private final AtomicInteger sequence = new AtomicInteger(0);
   private final InetAddress host;
   private final String appName;
   private final int port;

   private SyslogMessageFormat format = SyslogMessageFormat.IETF;
   private SyslogTransportFormat transport = SyslogTransportFormat.TCP;
   private boolean secure = false;
   private int timeout = 500;
   private int facility = 0;

   private DataSender sender = null;

   static {
      EnumMap<Severity, Integer> values = new EnumMap<>(Severity.class);
      values.put(Severity.FATAL, 1);
      values.put(Severity.ERROR, 3);
      values.put(Severity.WARNING, 4);
      values.put(Severity.INFO, 6);
      values.put(Severity.DEBUG, 7);
      numeric = values;
   }

   /**
    * Creates a new {@code SyslogHandler} with the given arguments.
    *
    * @param appName the name of the application to be identified in the syslog server.
    * @param host the host name or IP address where logged messages are to be sent to.
    * @param port the port of the host to be connected to.
    * @param filter the filter used to test if messages should be handled by this Handler.
    * @throws UnknownHostException if no IP address for the host could be found, or if a scope_id
    *            was specified for a global IPv6 address.
    */
   public SyslogHandler(String appName, String host, int port, Predicate<LogEntry> filter)
      throws UnknownHostException {
      super(filter);
      this.host = InetAddress.getByName(host);
      this.appName = appName;
      this.port = port;
   }

   /**
    * Sets the facility to direct log messages on the syslog server to.
    *
    * @param facility the number of the facility on the syslog server. Must be a number between 0
    *           and 7 (inclusive).
    */
   public void setFacility(int facility) {
      this.facility = facility;
      if (facility < 0 || facility > 7) {
         throw new IllegalArgumentException("Facility must be between 0 and 7");
      }
   }

   /**
    * Sets the message format to be used when sending log messages to the server.
    *
    * @param format the format to be used when sending log messages to the server.
    */
   public void setFormat(SyslogMessageFormat format) {
      this.format = Objects.requireNonNull(format);
   }

   /**
    * Sets whether to use a secure connection to send messages to the syslog server.
    * <p>
    * Note that sending secure messages are only supported using {@code TCP} transport.
    * </p>
    *
    * @param secure {@code true} to send messages securely or {@code false} otherwise.
    */
   public void setSecure(boolean secure) {
      this.secure = secure;
   }

   /**
    * Sets the amount of time to wait for a {@code TCP} connection to be established.
    *
    * @param timeout the amount of time to wait before failing to connect.
    */
   public void setTimeout(int timeout) {
      this.timeout = timeout;
      if (timeout < 0) {
         throw new IllegalArgumentException("Timeout cannot be negative");
      }
   }

   /**
    * Sets the transport mechanism to be used when sending messages to the syslog server.
    *
    * @param transport the transport mechanism to be used when sending messages to the syslog
    *           server.
    */
   public void setTransport(SyslogTransportFormat transport) {
      this.transport = Objects.requireNonNull(transport);
   }

   @Override
   protected void prepare() {
      try {
         if (transport == SyslogTransportFormat.UDP) {
            sender = new UdpDataSender();
         } else if (transport == SyslogTransportFormat.TCP) {
            sender = new TcpDataSender();
         }
      } catch (Exception e) {
         throw new IllegalStateException(e);
      }
   }

   @Override
   protected void append(LogEntry entry) {
      String[] messages = getMessages(entry);
      Arrays.stream(messages).map(m -> produceMessage(entry, m).trim()).forEach(sender::send);
   }

   @Override
   protected void finished() {
      if (sender != null) {
         sender.close();
      }
   }

   private String[] getMessages(LogEntry entry) {
      if (transport == SyslogTransportFormat.TCP) {
         return entry.getMessage().split("\\.");
      }
      return new String[] { entry.getMessage() };
   }

   private String produceMessage(LogEntry entry, String message) {
      String hostname = entry.getConnection().getLocalAddress().getHostName();
      DateTimeFormatter formatter;
      int pri = (facility + 16) * 8 + numeric.get(entry.getSeverity());
      switch (format) {
         case BSD:
            formatter = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss", Locale.US);
            return String.format("<%d>%s %s %s %s", pri, formatter.format(entry.getTime()),
                     hostname, appName == null ? "-" : appName, message.trim());
         case IETF:
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            String remoteIp = entry.getConnection().getRemoteAddress().getHostAddress();
            int msgId = sequence.incrementAndGet();
            return String.format(
                     "<%d>%d %s %s %s - ID%d [origin clientIp=\"%s\" className=\"%s\"] %s", pri,
                     RFC_5424_VERSION, formatter.format(entry.getTime()), hostname,
                     appName == null ? "-" : appName, msgId, remoteIp, entry.getClassName(),
                     message.trim());
         default:
            throw new IllegalArgumentException("Unknown syslog format");
      }
   }

   private static interface DataSender {

      void send(String message);

      void close();
   }

   // - UdpDataSender -----

   private class UdpDataSender implements DataSender {

      private final DatagramSocket socket;

      public UdpDataSender() throws IOException {
         socket = new DatagramSocket();
      }

      @Override
      public void send(String message) {
         byte[] data = message.getBytes(UTF_8);
         DatagramPacket packet = new DatagramPacket(data, data.length, host, port);
         try {
            socket.send(packet);
         } catch (IOException e) {
            throw new IllegalStateException(e);
         }
      }

      @Override
      public void close() {
         socket.close();
      }
   }

   // - TcpDataSender -----

   private class TcpDataSender implements DataSender {

      private final Socket socket;
      private final Writer writer;

      public TcpDataSender() throws IOException {
         if (secure) {
            socket = SSLSocketFactory.getDefault().createSocket();
         } else {
            socket = SocketFactory.getDefault().createSocket();
         }
         socket.setKeepAlive(true);
         socket.connect(new InetSocketAddress(host, port), timeout);
         writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8));
      }

      @Override
      public void send(String message) {
         try {
            writer.write(message);
            writer.write("\r\n");
            writer.flush();
         } catch (IOException e) {
            throw new IllegalStateException(e);
         }
      }

      @Override
      public void close() {
         try {
            writer.close();
         } catch (IOException e) {
            // Ignore
         }
         try {
            socket.shutdownOutput();
         } catch (IOException e) {
            // Ignore
         }
         try {
            socket.close();
         } catch (IOException e) {
            // Ignore
         }
      }
   }
}
