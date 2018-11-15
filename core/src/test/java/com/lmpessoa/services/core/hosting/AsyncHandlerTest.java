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
package com.lmpessoa.services.core.hosting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lmpessoa.services.core.Async;
import com.lmpessoa.services.core.concurrent.ExecutionService;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.util.ConnectionInfo;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.Logger;
import com.lmpessoa.services.util.logging.NullLogWriter;

public final class AsyncHandlerTest {

   private static final ILogger log = new Logger(AsyncHandlerTest.class, new NullLogWriter());
   private static final ExecutionService executor = new ExecutionService(1, log);
   private static final ApplicationOptions options = new ApplicationOptions();
   private static final String baseUrl = "https://lmpessoa.com" + options.getAsyncFeedbackPath();

   private AsyncHandler handler;
   private ConnectionInfo connect;
   private HttpRequest request;
   private NextHandler next;
   private RouteMatch match;

   private String runnableResult;

   @BeforeClass
   public static void classSetup() {
      AsyncHandler.setExecutor(executor);
   }

   @Before
   public void setup() {
      connect = new ConnectionInfo(mock(Socket.class), "https://lmpessoa.com/");
      request = mock(HttpRequest.class);
      when(request.getPath()).thenReturn("/test");
      runnableResult = null;
      next = () -> match.invoke();
      handler = new AsyncHandler(next);
   }

   @Test
   public void testAsyncMethod() throws NoSuchMethodException, MalformedURLException,
      InterruptedException, ExecutionException, IllegalAccessException, InvocationTargetException {
      match = matchOfMethod("asyncMethod");

      Object result = handler.invoke(options, request, match);
      assertTrue(result instanceof Redirect);
      Redirect redirect = (Redirect) result;
      assertEquals(202, redirect.getStatusCode());
      String url = redirect.getUrl(connect).toExternalForm();
      assertTrue(url.startsWith(baseUrl));
      Future<?> fresult = executor.get(url.substring(baseUrl.length()));
      assertEquals("test", fresult.get());
   }

   @Test
   public void testCallableResult() throws NoSuchMethodException, MalformedURLException,
      InterruptedException, ExecutionException, IllegalAccessException, InvocationTargetException {
      match = matchOfMethod("callableResult");

      Object result = handler.invoke(options, request, match);
      assertTrue(result instanceof Redirect);
      Redirect redirect = (Redirect) result;
      assertEquals(202, redirect.getStatusCode());
      String url = redirect.getUrl(connect).toExternalForm();
      assertTrue(url.startsWith(baseUrl));
      Future<?> fresult = executor.get(url.substring(baseUrl.length()));
      assertEquals("test", fresult.get());
   }

   @Test
   public void testRunnableResult() throws NoSuchMethodException, MalformedURLException,
      InterruptedException, ExecutionException, IllegalAccessException, InvocationTargetException {
      match = matchOfMethod("runnableResult");

      Object result = handler.invoke(options, request, match);
      assertTrue(result instanceof Redirect);
      Redirect redirect = (Redirect) result;
      assertEquals(202, redirect.getStatusCode());
      String url = redirect.getUrl(connect).toExternalForm();
      assertTrue(url.startsWith(baseUrl));
      Future<?> fresult = executor.get(url.substring(baseUrl.length()));
      assertNull(fresult.get());
      assertEquals("test", runnableResult);
   }

   @Test
   public void testCheckExecutionResult() throws NoSuchMethodException, MalformedURLException,
      InterruptedException, ExecutionException {
      match = matchOfMethod("sleeper");

      Object result = handler.invoke(options, request, match);
      assertTrue(result instanceof Redirect);
      Redirect redirect = (Redirect) result;
      String url = redirect.getUrl(connect).getPath();

      match = null;
      when(request.getPath()).thenReturn(url);
      result = handler.invoke(options, request, match);
      assertTrue(result instanceof Future);
      Future<?> fresult = (Future<?>) result;
      assertFalse(fresult.isDone());
      fresult.get();
      assertTrue(fresult.isDone());
   }

   @Test(expected = NotFoundException.class)
   public void testNonExistentResult() {
      when(request.getPath()).thenReturn("/feedback/test");
      handler.invoke(options, request, match);
   }

   private RouteMatch matchOfMethod(String methodName) throws NoSuchMethodException {
      final Method method = AsyncHandlerTest.class.getMethod(methodName);
      return new RouteMatch() {

         @Override
         public Object invoke() {
            try {
               return method.invoke(AsyncHandlerTest.this);
            } catch (IllegalAccessException | InvocationTargetException e) {
               throw new RuntimeException(e);
            }
         }

         @Override
         public Method getMethod() {
            return method;
         }
      };
   }

   @Async
   public String asyncMethod() {
      return "test";
   }

   public Callable<String> callableResult() {
      return () -> "test";
   }

   public Runnable runnableResult() {
      return () -> runnableResult = "test";
   }

   @Async
   public void sleeper() throws InterruptedException {
      Thread.sleep(10);
   }
}
