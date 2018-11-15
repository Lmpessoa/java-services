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
package com.lmpessoa.services.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

public final class ConnectionInfo {

   private final boolean secure;
   private final Socket socket;
   private final String host;
   private final int port;

   /**
    * Returns the local port number to which this socket is bound.
    *
    * <p>
    * If the socket was bound prior to being closed, then this method will continue to return the local
    * port number after the socket is closed.
    * </p>
    *
    * @return the local port number to which this socket is bound or -1 if the socket is not bound yet.
    */
   public int getLocalPort() {
      return socket.getLocalPort();
   }

   /**
    * Returns the address to which the socket is connected.
    *
    * <p>
    * If the socket was connected prior to being closed, then this method will continue to return the
    * connected address after the socket is closed.
    * </p>
    *
    * @return the remote IP address to which this socket is connected, or null if the socket is not
    * connected.
    */
   public InetAddress getRemoteAddress() {
      return socket.getInetAddress();
   }

   /**
    * Returns the remote port number to which this socket is connected.
    *
    * <p>
    * If the socket was connected prior to being closed, then this method will continue to return the
    * connected port number after the socket is closed.
    * </p>
    *
    * @return the remote port number to which this socket is connected, or 0 if the socket is not
    * connected yet.
    */
   public int getRemotePort() {
      return socket.getPort();
   }

   /**
    * Returns the server name of this application.
    * <p>
    * This method returns the host name of the server as requested by the user agent and parsed from
    * the <code>Host</code> header of the request. If this information is not available, "localhost"
    * will be returned instead.
    * </p>
    *
    * @return the host name of the server in the current request.
    */
   public String getServerName() {
      return host;
   }

   /**
    * Returns the port number the server uses to listen for requests.
    *
    * @return the port number the server uses to listen for requests.
    */
   public int getServerPort() {
      return port;
   }

   /**
    * Returns whether the current request was made using HTTPS.
    *
    * @return <code>true</code> if the current request was made using HTTPS, <code>false</code>
    * otherwise.
    */
   public boolean isSecure() {
      return secure;
   }

   /**
    * Closes this connection.
    *
    * <p>
    * It is not recommended applications explicitly close connections.
    * </p>
    *
    * <p>
    * Once a connection has been closed, it is not available for further networking use and thus will
    * prevent an unsent response from being sent back to the user agent.
    * </p>
    *
    * @throws IOException if an I/O error occurs when closing this socket.
    */
   public void close() throws IOException {
      socket.close();
   }

   /**
    * Creates a new <code>ConnectionInfo</code> for the given socket.
    * 
    * @param socket the socket connection used to receive data from a user agent.
    * @param hostValue the host information of the application server.
    */
   public ConnectionInfo(Socket socket, String hostValue) {
      this.socket = socket;
      if (hostValue != null) {
         String[] hostParts = hostValue.split(":");
         this.secure = "https".equals(hostParts[0]);
         int portValue;
         if (hostParts[hostParts.length - 1].matches("\\d+")) {
            portValue = Integer.parseInt(hostParts[hostParts.length - 1]);
            hostParts = Arrays.copyOf(hostParts, hostParts.length - 1);
         } else {
            portValue = secure ? 443 : 80;
         }
         this.port = portValue;
         String hostName = hostParts[hostParts.length - 1];
         if (hostName.startsWith("//")) {
            hostName = hostName.substring(2);
         }
         if (hostName.endsWith("/")) {
            hostName = hostName.substring(0, hostName.length() - 1);
         }
         this.host = hostName;
      } else {
         this.secure = false;
         this.host = "localhost";
         this.port = -1;
      }
   }
}
