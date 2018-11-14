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
package com.lmpessoa.services.core.hosting;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

final class HttpServerJob implements Runnable {

   private final ApplicationServer server;
   private final Application app;

   private ServerSocket socket;

   HttpServerJob(ApplicationServer server, Application app) {
      this.server = server;
      this.app = app;
   }

   @Override
   public void run() {
      int port = getPort();
      InetAddress addr = server.getApplicationInfo().getBindAddress();
      try (ServerSocket serverSocket = new ServerSocket(port, 0, addr)) {
         socket = serverSocket;
         serverSocket.setSoTimeout(1);
         while (socket != null && !socket.isClosed()) {
            acceptClient();
         }
      } catch (IOException e) {
         app.getLogger().error(e);
      }
   }

   private void acceptClient() {
      try {
         Socket client = socket.accept();
         Runnable job = new HttpRequestJob(app, client);
         server.getThreadPool().execute(job);
      } catch (SocketTimeoutException e) {
         // just ignore
      } catch (IOException e) {
         app.getLogger().error(e);
      }
   }

   public void stop() {
      socket = null;
   }

   public int getPort() {
      return server.getApplicationInfo().getPort();
   }
}
