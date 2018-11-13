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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.HttpGet;
import com.lmpessoa.services.core.Route;
import com.lmpessoa.services.hosting.Application;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.hosting.RequestHandlerJob;

public final class RequestJobTest {

   private Application app;

   @Before
   public void setup() throws NoSuchMethodException {
      app = new Application(RequestJobTest.class, null, 5616, null);
      app.doConfiguration();
      app.getRouteTable().put("", TestResource.class);
   }

   private String[] runJob(HttpRequest request) throws InterruptedException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Future<?> result = app.submitJob(new RequestHandlerJob(app, request, out));
      while (!result.isDone()) {
         // Does nothing, just sit and wait
      }
      return new String(out.toByteArray()).split("\r\n");
   }

   @Test
   public void testJobRequestEmpty() throws InterruptedException, IOException {
      String[] result = runJob(new HttpRequestBuilder().setPath("/test/empty").build());
      assertEquals("HTTP/1.1 204 No Content", result[0]);
   }

   @Test
   public void testJobRequestNotFound() throws InterruptedException, IOException {
      String[] result = runJob(new HttpRequestBuilder().setPath("/test/notfound").build());
      assertEquals("HTTP/1.1 404 Not Found", result[0]);
   }

   @Test
   public void testJobRequestWrongMethod() throws InterruptedException, IOException {
      String[] result = runJob(new HttpRequestBuilder().setMethod("POST").setPath("/test/empty").build());
      assertEquals("HTTP/1.1 405 Method Not Allowed", result[0]);
   }

   @Test
   public void testJobResquestString() throws InterruptedException, IOException {
      String[] result = runJob(new HttpRequestBuilder().setPath("/test").build());
      assertEquals(5, result.length);
      assertEquals("HTTP/1.1 200 OK", result[0]);
      assertEquals("Content-Type: text/plain", result[1]);
      assertEquals("Content-Length: 4", result[2]);
      assertEquals("Test", result[4]);
   }

   public static class TestResource {

      @HttpGet
      @Route("empty")
      public void empty() {
         // Test method, does nothing
      }

      public String get() {
         return "Test";
      }
   }
}
