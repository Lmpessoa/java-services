/*
 * Copyright (c) 2017 Leonardo Pessoa
 * http://github.com/lmpessoa/java-services
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmpessoa.services.routing.content.Serializer;
import com.lmpessoa.util.ClassUtils;

final class ApplicationOptions implements IApplicationOptions {

   private final ApplicationThreadFactory threadFactory = new ApplicationThreadFactory();
   private final Application application;

   private ExecutorService threadPool = Executors.newCachedThreadPool(threadFactory);

   public ApplicationOptions(Application application) {
      this.application = application;
   }

   @Override
   public void acceptXmlRequests() {
      Method enableXml = ClassUtils.getDeclaredMethod(Serializer.class, "enableXml");
      enableXml.setAccessible(true);
      try {
         enableXml.invoke(null, true);
      } catch (IllegalAccessException | InvocationTargetException e) {
         e.printStackTrace(); // or ignore
      }
   }

   @Override
   public void addHandler(Class<?> handlerClass) {
      application.getMediator().addHandler(handlerClass);
   }

   @Override
   public void limitConcurrentJobs(int maxJobs) {
      threadPool = Executors.newFixedThreadPool(maxJobs, threadFactory);
   }

   ExecutorService getThreadPool() {
      return threadPool;
   }
}
