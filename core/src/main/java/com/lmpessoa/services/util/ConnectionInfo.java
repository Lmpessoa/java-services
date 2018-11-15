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

   public int getLocalPort() {
      return socket.getLocalPort();
   }

   public InetAddress getRemoteAddress() {
      return socket.getInetAddress();
   }

   public int getRemotePort() {
      return socket.getPort();
   }

   public String getServerName() {
      return host;
   }

   public int getServerPort() {
      return port;
   }

   public boolean isSecure() {
      return secure;
   }

   public void close() throws IOException {
      socket.close();
   }

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
         this.host = hostName;
      } else {
         this.secure = false;
         this.host = null;
         this.port = -1;
      }
   }
}
