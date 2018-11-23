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
package com.lmpessoa.services.core.internal.hosting;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Objects;

import com.lmpessoa.services.core.hosting.IHostEnvironment;
import com.lmpessoa.services.core.hosting.NextResponder;
import com.lmpessoa.services.core.internal.concurrent.ExecutionService;
import com.lmpessoa.services.core.internal.routing.RouteTable;
import com.lmpessoa.services.core.internal.services.ServiceMap;
import com.lmpessoa.services.core.security.IIdentityProvider;
import com.lmpessoa.services.util.logging.internal.Logger;

class ApplicationContext implements Runnable {

   private final ApplicationServerImpl server;
   private final RouteTable routes;
   private final String name;
   private final int port;

   private ServerSocket socket;

   @Override
   public void run() {
      Thread.currentThread().setName(name + "-context");
      try (ServerSocket serverSocket = new ServerSocket(port, 0,
               InetAddress.getLoopbackAddress())) {
         socket = serverSocket;
         socket.setSoTimeout(1);
         while (socket != null && !socket.isClosed()) {
            acceptClientToHandle();
         }
      } catch (IOException e) {
         getLogger().error(e);
      }
   }

   @Override
   public String toString() {
      return String.format("%d/%s", port, name);
   }

   ApplicationContext(ApplicationServerImpl server, int port, String name, RouteTable routes) {
      this.server = Objects.requireNonNull(server);
      this.port = port <= 0 ? 5617 : port;
      this.routes = routes;
      this.name = name;
   }

   void stop() {
      socket = null;
   }

   Class<?> getStartupClass() {
      return server.getSettings().getStartupClass();
   }

   Logger getLogger() {
      return server.getSettings().getLogger();
   }

   IHostEnvironment getEnvironment() {
      return server.getSettings().getEnvironment();
   }

   ServiceMap getServices() {
      return server.getOptions().getServices();
   }

   ExecutionService getExecutor() {
      return server.getSettings().getMainExecutor();
   }

   NextResponder getFirstResponder() {
      return server.getOptions().getFirstResponder();
   }

   RouteTable getRouteTable() {
      return routes;
   }

   IIdentityProvider getIdenityProvider() {
      return server.getOptions().getIdentityProvider();
   }

   int getPort() {
      return port;
   }

   String getName() {
      return name;
   }

   private void acceptClientToHandle() {
      try {
         Socket client = socket.accept();
         ApplicationRequestJob job = new ApplicationRequestJob(this, client);
         getExecutor().submit(job, "request");
      } catch (SocketTimeoutException e) {
         // just ignore
      } catch (IOException e) {
         getLogger().error(e);
      }
   }
}
