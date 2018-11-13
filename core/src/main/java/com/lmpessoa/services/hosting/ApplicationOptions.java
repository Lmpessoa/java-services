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
package com.lmpessoa.services.hosting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmpessoa.services.routing.content.Serializer;
import com.lmpessoa.services.services.AbstractOptions;
import com.lmpessoa.util.ClassUtils;

final class ApplicationOptions extends AbstractOptions implements IApplicationOptions {

   private final ApplicationThreadFactory threadFactory;
   private final Application app;

   private ExecutorService threadPool;
   private InetAddress addr = null;
   private int port = 5617;

   public ApplicationOptions(Application app) {
      this.threadFactory = new ApplicationThreadFactory(app.getLogger());
      this.threadPool = Executors.newCachedThreadPool(threadFactory);
      this.app = app;
   }

   @Override
   public void usePort(int port) {
      protectConfiguration();
      this.port = port;
   }

   @Override
   public void bindToAddress(String addr) {
      protectConfiguration();
      if (addr != null) {
         try {
            this.addr = InetAddress.getByName(addr);
         } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unknown host: " + addr, e);
         }
      } else {
         this.addr = null;
      }
   }

   @Override
   public void acceptXmlRequests() {
      protectConfiguration();
      Method enableXml = ClassUtils.getDeclaredMethod(Serializer.class, "enableXml");
      enableXml.setAccessible(true);
      try {
         enableXml.invoke(null, true);
      } catch (IllegalAccessException | InvocationTargetException e) {
         app.getLogger().debug(e);
      }
   }

   @Override
   public void addHandler(Class<?> handlerClass) {
      protectConfiguration();
      app.getMediator().addHandler(handlerClass);
   }

   @Override
   public void limitConcurrentJobs(int maxJobs) {
      protectConfiguration();
      threadPool = Executors.newFixedThreadPool(maxJobs, threadFactory);
   }

   ExecutorService getThreadPool() {
      return threadPool;
   }

   int getPort() {
      return port;
   }

   InetAddress getBindAddress() {
      return addr;
   }
}
