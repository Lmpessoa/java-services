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

import static com.lmpessoa.services.core.routing.HttpMethod.DELETE;
import static com.lmpessoa.services.core.routing.HttpMethod.GET;
import static com.lmpessoa.services.core.routing.HttpMethod.PATCH;
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
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.concurrent.Async;
import com.lmpessoa.services.core.concurrent.ExecutionService;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.Logger;
import com.lmpessoa.services.util.logging.NullHandler;

public final class AsyncResponderTest {

   private static final ILogger log = new Logger(new NullHandler());
   private static final String BASE_URL = "https://lmpessoa.com/feedback/";

   private ApplicationOptions app;
   private ExecutionService executor;
   private AsyncResponder handler;
   private ConnectionInfo connect;
   private HttpRequest request;
   private NextResponder next;
   private RouteMatch match;

   private String runnableResult;

   @Before
   public void setup() {
      app = new ApplicationOptions(null);
      app.useAsync();
      connect = new ConnectionInfo(mock(Socket.class), "https://lmpessoa.com/");
      request = mock(HttpRequest.class);
      when(request.getMethod()).thenReturn(GET);
      when(request.getPath()).thenReturn("/test");
      runnableResult = null;
      next = () -> match.invoke();
      handler = new AsyncResponder(next, app);
      executor = new ExecutionService(1, log);
      AsyncResponder.setExecutor(executor);
   }

   @Test
   public void testAsyncMethod() throws NoSuchMethodException, MalformedURLException,
      InterruptedException, ExecutionException {
      match = matchOfMethod("asyncMethod");

      Object result = handler.invoke(request, match, connect);
      assertTrue(result instanceof Redirect);
      Redirect redirect = (Redirect) result;
      assertEquals(202, redirect.getStatusCode());
      String url = redirect.getUrl(connect).toExternalForm();
      assertTrue(url.startsWith(BASE_URL));
      Future<?> fresult = executor.get(url.substring(BASE_URL.length()));
      assertEquals("test", fresult.get());
   }

   @Test
   public void testCallableResult() throws NoSuchMethodException, MalformedURLException,
      InterruptedException, ExecutionException {
      match = matchOfMethod("callableResult");

      Object result = handler.invoke(request, match, connect);
      assertTrue(result instanceof Redirect);
      Redirect redirect = (Redirect) result;
      assertEquals(202, redirect.getStatusCode());
      String url = redirect.getUrl(connect).toExternalForm();
      assertTrue(url.startsWith(BASE_URL));
      Future<?> fresult = executor.get(url.substring(BASE_URL.length()));
      assertEquals("test", fresult.get());
   }

   @Test
   public void testRunnableResult() throws NoSuchMethodException, MalformedURLException,
      InterruptedException, ExecutionException {
      match = matchOfMethod("runnableResult");

      Object result = handler.invoke(request, match, connect);
      assertTrue(result instanceof Redirect);
      Redirect redirect = (Redirect) result;
      assertEquals(202, redirect.getStatusCode());
      String url = redirect.getUrl(connect).toExternalForm();
      assertTrue(url.startsWith(BASE_URL));
      Future<?> fresult = executor.get(url.substring(BASE_URL.length()));
      assertNull(fresult.get());
      assertEquals("test", runnableResult);
   }

   @Test
   public void testCheckExecutionResult() throws NoSuchMethodException, MalformedURLException,
      InterruptedException, ExecutionException {
      match = matchOfMethod("sleeper");

      Object result = handler.invoke(request, match, connect);
      assertTrue(result instanceof Redirect);
      Redirect redirect = (Redirect) result;
      String url = redirect.getUrl(connect).getPath();

      match = null;
      when(request.getPath()).thenReturn(url);
      result = handler.invoke(request, match, connect);
      assertTrue(result instanceof Future);
      Future<?> fresult = (Future<?>) result;
      assertFalse(fresult.isDone());
      fresult.get();
      assertTrue(fresult.isDone());
   }

   @Test
   public void testCheckRedirectResult()
      throws NoSuchMethodException, InterruptedException, MalformedURLException {
      match = matchOfMethod("redirect");

      Object result = handler.invoke(request, match, connect);
      Redirect redirect = (Redirect) result;
      String url = redirect.getUrl(connect).getPath();
      Thread.sleep(100);

      match = null;
      when(request.getPath()).thenReturn(url);
      result = handler.invoke(request, match, connect);
      assertTrue(result instanceof Redirect);
      redirect = (Redirect) result;
      assertEquals(303, redirect.getStatusCode());
      url = redirect.getUrl(connect).toExternalForm();
      assertEquals("https://lmpessoa.com/test", url);
   }

   @Test
   public void testCheckUrlResult()
      throws NoSuchMethodException, InterruptedException, MalformedURLException {
      match = matchOfMethod("redirectUrl");

      Object result = handler.invoke(request, match, connect);
      Redirect redirect = (Redirect) result;
      String url = redirect.getUrl(connect).getPath();
      Thread.sleep(100);

      match = null;
      when(request.getPath()).thenReturn(url);
      result = handler.invoke(request, match, connect);
      assertTrue(result instanceof Redirect);
      redirect = (Redirect) result;
      assertEquals(303, redirect.getStatusCode());
      url = redirect.getUrl(connect).toExternalForm();
      assertEquals("https://lmpessoa.com/test", url);
   }

   @Test(expected = NotFoundException.class)
   public void testNonExistentResult() {
      when(request.getPath()).thenReturn("/feedback/test");
      handler.invoke(request, match, connect);
   }

   @Test(expected = MethodNotAllowedException.class)
   public void testPatchResult() throws NoSuchMethodException, MalformedURLException {
      match = matchOfMethod("sleeper");

      handler.invoke(request, match, connect);
      Object result = handler.invoke(request, match, connect);
      Redirect redirect = (Redirect) result;
      String url = redirect.getUrl(connect).getPath();

      match = null;
      when(request.getPath()).thenReturn(url);
      when(request.getMethod()).thenReturn(PATCH);
      handler.invoke(request, match, connect);
   }

   @Test(expected = CancellationException.class)
   public void testCancelAsyncTask() throws NoSuchMethodException, MalformedURLException,
      InterruptedException, ExecutionException {
      match = matchOfMethod("sleeper");

      handler.invoke(request, match, connect);
      Object result = handler.invoke(request, match, connect);
      Redirect redirect = (Redirect) result;
      String url = redirect.getUrl(connect).getPath();

      match = null;
      when(request.getPath()).thenReturn(url);
      when(request.getMethod()).thenReturn(DELETE);
      result = handler.invoke(request, match, connect);

      assertTrue(result instanceof Future);
      Future<?> fresult = (Future<?>) result;
      assertTrue(fresult.isDone());
      assertTrue(fresult.isCancelled());
      fresult.get();
   }

   @Test(expected = InterruptedException.class)
   public void testCancelAsyncTaskRunning() throws NoSuchMethodException, MalformedURLException,
      InterruptedException, ExecutionException {
      match = matchOfMethod("sleeper");

      Object result = handler.invoke(request, match, connect);
      Redirect redirect = (Redirect) result;
      String url = redirect.getUrl(connect).getPath();
      Thread.sleep(10);

      match = null;
      when(request.getPath()).thenReturn(url);
      when(request.getMethod()).thenReturn(DELETE);
      result = handler.invoke(request, match, connect);

      assertTrue(result instanceof Future);
      Future<?> fresult = (Future<?>) result;
      assertTrue(fresult.isDone());
      assertTrue(fresult.isCancelled());
      fresult.get();
   }

   private RouteMatch matchOfMethod(String methodName) throws NoSuchMethodException {
      final Method method = AsyncResponderTest.class.getMethod(methodName);
      return new RouteMatch() {

         @Override
         public Object invoke() {
            try {
               return method.invoke(AsyncResponderTest.this);
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
      Thread.sleep(50);
   }

   @Async
   public Redirect redirect() {
      return Redirect.to("/test");
   }

   @Async
   public URL redirectUrl() throws MalformedURLException {
      return new URL("https://lmpessoa.com/test");
   }
}
