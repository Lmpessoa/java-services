/*
 * Copyright (c) 2017 Leonardo Pessoa
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
package com.lmpessoa.services.logging;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.hosting.ConnectionInfo;
import com.lmpessoa.services.internal.logging.ConsoleHandler;
import com.lmpessoa.services.internal.logging.FileHandler;
import com.lmpessoa.services.internal.logging.Logger;
import com.lmpessoa.services.internal.logging.SyslogHandler;
import com.lmpessoa.services.internal.logging.SyslogMessageFormat;
import com.lmpessoa.services.internal.logging.SyslogTransportFormat;
import com.lmpessoa.services.internal.parsing.ParseException;

public final class LogHandlerTest {

   private static final String MESSAGE = "[42.0.1.7       ] c.l.services.logging.LogHandlerTest  : Test";

   private static Logger useHandler(Handler handler) throws UnknownHostException {
      if (handler instanceof FormattedHandler) {
         try {
            ((FormattedHandler) handler).setTemplate("[{Severity}] {Message}");
         } catch (ParseException e) {
            // Just ignore; shall not happen
         }
      }
      Socket socket = mock(Socket.class);
      when(socket.getInetAddress())
               .thenReturn(InetAddress.getByAddress(new byte[] { 42, 0, 1, 7 }));
      when(socket.getLocalAddress()).thenReturn(InetAddress.getLocalHost());
      Logger log = new Logger();
      log.addSupplier(ConnectionInfo.class,
               () -> new ConnectionInfo(socket, "https://lmpessoa.com"));
      log.addVariable("Remote.Host", ConnectionInfo.class, c -> c.getRemoteAddress().getHostName());
      log.addVariable("Remote.Addr", ConnectionInfo.class,
               c -> c.getRemoteAddress().getHostAddress());
      log.addHandler(handler);
      return log;
   }

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void testConsoleHandlerError() throws InterruptedException, UnknownHostException {
      PrintStream originalOut = System.out;
      PrintStream originalErr = System.err;
      ByteArrayOutputStream replaceOut = new ByteArrayOutputStream();
      ByteArrayOutputStream replaceErr = new ByteArrayOutputStream();
      System.setOut(new PrintStream(replaceOut));
      System.setErr(new PrintStream(replaceErr));

      Logger log = useHandler(new ConsoleHandler(Severity.atOrAbove(Severity.INFO)));
      log.error("Test");
      log.join();
      assertEquals(0, replaceOut.toByteArray().length);
      assertEquals("[ERROR] Test\n", new String(replaceErr.toByteArray()));

      System.setOut(originalOut);
      System.setErr(originalErr);
   }

   @Test
   public void testConsoleHandlerWarning() throws InterruptedException, UnknownHostException {
      PrintStream originalOut = System.out;
      PrintStream originalErr = System.err;
      ByteArrayOutputStream replaceOut = new ByteArrayOutputStream();
      ByteArrayOutputStream replaceErr = new ByteArrayOutputStream();
      System.setOut(new PrintStream(replaceOut));
      System.setErr(new PrintStream(replaceErr));

      Logger log = useHandler(new ConsoleHandler(Severity.atOrAbove(Severity.INFO)));
      log.warning("Test");
      log.join();
      assertEquals(0, replaceErr.toByteArray().length);
      assertEquals("[WARNING] Test\n", new String(replaceOut.toByteArray()));

      System.setOut(originalOut);
      System.setErr(originalErr);
   }

   @Test
   public void testConsoleHandlerHighlight() throws InterruptedException, UnknownHostException {
      PrintStream originalOut = System.out;
      PrintStream originalErr = System.err;
      ByteArrayOutputStream replaceOut = new ByteArrayOutputStream();
      ByteArrayOutputStream replaceErr = new ByteArrayOutputStream();
      System.setOut(new PrintStream(replaceOut));
      System.setErr(new PrintStream(replaceErr));

      ConsoleHandler handler = new ConsoleHandler(Severity.atOrAbove(Severity.INFO));
      handler.setHighlight(Severity.WARNING);
      Logger log = useHandler(handler);
      log.warning("Test");
      log.join();
      assertEquals(0, replaceOut.toByteArray().length);
      assertEquals("[WARNING] Test\n", new String(replaceErr.toByteArray()));

      System.setOut(originalOut);
      System.setErr(originalErr);
   }

   @Test
   public void testFileHandler() throws InterruptedException, IOException {
      File tmp = File.createTempFile("test", ".log");
      tmp.deleteOnExit();

      Logger log = useHandler(new FileHandler(tmp, Severity.atOrAbove(Severity.INFO)));
      log.warning("Test");
      log.join();
      try (BufferedReader buffer = new BufferedReader(new FileReader(tmp))) {
         assertEquals("[WARNING] Test", buffer.readLine());
      }
   }

   @Test
   public void testSyslogFacilityBelowZero() throws UnknownHostException {
      thrown.expect(IllegalArgumentException.class);
      SyslogHandler handler = new SyslogHandler("Test", "localhost", 5140,
               Severity.atOrAbove(Severity.INFO));
      handler.setFacility(-1);
   }

   @Test
   public void testSyslogFacilityAboveSeven() throws UnknownHostException {
      thrown.expect(IllegalArgumentException.class);
      SyslogHandler handler = new SyslogHandler("Test", "localhost", 5140,
               Severity.atOrAbove(Severity.INFO));
      handler.setFacility(8);
   }

   @Test
   public void testSyslogHandlerUdp5424() throws InterruptedException, IOException {
      try (DatagramSocket dgram = new DatagramSocket(5140)) {
         sendSyslogTestUdp5424();
         byte[] data = new byte[1024];
         DatagramPacket packet = new DatagramPacket(data, data.length);
         dgram.receive(packet);
         data = Arrays.copyOf(data, packet.getLength());
         String dataStr = new String(data);
         assertEquals("<132>1 ", dataStr.substring(0, 7));
         dataStr.substring(7, 31);
         assertEquals(" ", dataStr.substring(31, 32));
         String hostname = InetAddress.getLocalHost().getHostName();
         assertEquals(hostname, dataStr.substring(32, 32 + hostname.length()));
         assertEquals(" Test - ID1 - \u00EF\u00BB\u00BF" + MESSAGE,
                  dataStr.substring(32 + hostname.length()));
      }
   }

   private static void sendSyslogTestUdp5424() throws UnknownHostException, InterruptedException {
      SyslogHandler handler = new SyslogHandler("Test", "localhost", 5140,
               Severity.atOrAbove(Severity.INFO));
      handler.setFacility(0);
      handler.setTransport(SyslogTransportFormat.UDP);
      Logger log = useHandler(handler);
      log.warning("Test");
      log.join();
   }

   @Test
   public void testSyslogHandlerUdp3164() throws InterruptedException, IOException {
      try (DatagramSocket dgram = new DatagramSocket(5140)) {
         sendSyslogTestUdp3164();
         byte[] data = new byte[1024];
         DatagramPacket packet = new DatagramPacket(data, data.length);
         dgram.receive(packet);
         data = Arrays.copyOf(data, packet.getLength());
         String dataStr = new String(data);
         assertEquals("<148>", dataStr.substring(0, 5));
         dataStr.substring(5, 20);
         assertEquals(" ", dataStr.substring(20, 21));
         String hostname = InetAddress.getLocalHost().getHostName();
         assertEquals(hostname, dataStr.substring(21, 21 + hostname.length()));
         assertEquals(" Test " + MESSAGE, dataStr.substring(21 + hostname.length()));
      }
   }

   private static void sendSyslogTestUdp3164() throws UnknownHostException, InterruptedException {
      SyslogHandler handler = new SyslogHandler("Test", "localhost", 5140,
               Severity.atOrAbove(Severity.INFO));
      handler.setFacility(2);
      handler.setFormat(SyslogMessageFormat.BSD);
      handler.setTransport(SyslogTransportFormat.UDP);
      Logger log = useHandler(handler);
      log.warning("Test");
      log.join();
   }

   @Test
   public void testSyslogHandlerTcp5424() throws InterruptedException, IOException {
      try (ScannerServer server = new ScannerServer(5140, "\r\n")) {
         sendSyslogTestTcp5424();
         String dataStr = server.next();
         assertEquals("<164>1 ", dataStr.substring(0, 7));
         dataStr.substring(7, 31);
         assertEquals(" ", dataStr.substring(31, 32));
         String hostname = InetAddress.getLocalHost().getHostName();
         assertEquals(hostname, dataStr.substring(32, 32 + hostname.length()));
         assertEquals(" Test - ID1 - \u00EF\u00BB\u00BF" + MESSAGE,
                  dataStr.substring(32 + hostname.length()));
      }
   }

   private static void sendSyslogTestTcp5424() throws UnknownHostException, InterruptedException {
      SyslogHandler handler = new SyslogHandler("Test", "localhost", 5140,
               Severity.atOrAbove(Severity.INFO));
      handler.setFacility(4);
      Logger log = useHandler(handler);
      log.warning("Test");
      log.join();
   }

   @Test
   public void testSyslogHandlerTcp3164() throws InterruptedException, IOException {
      try (ScannerServer server = new ScannerServer(5140, "\r\n")) {
         sendSyslogTestTcp3164();
         String dataStr = server.next();
         assertEquals("<180>", dataStr.substring(0, 5));
         dataStr.substring(5, 20);
         assertEquals(" ", dataStr.substring(20, 21));
         String hostname = InetAddress.getLocalHost().getHostName();
         assertEquals(hostname, dataStr.substring(21, 21 + hostname.length()));
         assertEquals(" Test " + MESSAGE, dataStr.substring(21 + hostname.length()));
      }
   }

   private static void sendSyslogTestTcp3164() throws UnknownHostException, InterruptedException {
      SyslogHandler handler = new SyslogHandler("Test", "localhost", 5140,
               Severity.atOrAbove(Severity.INFO));
      handler.setFacility(6);
      handler.setFormat(SyslogMessageFormat.BSD);
      Logger log = useHandler(handler);
      log.warning("Test");
      log.join();
   }

   // These tests are ensured to work because they were specified after testing the
   // SyslogHandler class with as actual syslog server and thus made only to evaluate
   // if the message received by the other end of the connection was the same message
   // expected to have been sent (which was already tested with an actual syslog server)

   // docker run --rm -d -p 5140:514/tcp -p 5140:514/udp \
   // -v "$PWD/syslog-ng.conf":/etc/syslog-ng/syslog-ng.conf \
   // --name syslog balabit/syslog-ng
   public static void main(String[] args) throws UnknownHostException, InterruptedException {
      sendSyslogTestUdp5424();
      sendSyslogTestUdp3164();
      sendSyslogTestTcp5424();
      sendSyslogTestTcp3164();
   }
}
