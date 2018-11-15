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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.ContentType;
import com.lmpessoa.services.core.HttpGet;
import com.lmpessoa.services.core.HttpInputStream;
import com.lmpessoa.services.core.Route;
import com.lmpessoa.services.core.hosting.ApplicationConfig;
import com.lmpessoa.services.core.hosting.ApplicationContext;
import com.lmpessoa.services.core.hosting.ApplicationServlet;
import com.lmpessoa.services.core.hosting.HttpRequest;
import com.lmpessoa.services.core.hosting.HttpResponse;
import com.lmpessoa.services.core.routing.IRouteOptions;
import com.lmpessoa.services.util.ConnectionInfo;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.NullLogger;

public final class ApplicationResponseTest {

   private ILogger log = new NullLogger();
   private ApplicationContext context;
   private ApplicationServlet app;

   public static void configure(IRouteOptions routes) {
      routes.addArea("", "^com.lmpessoa.services");
   }

   @Before
   public void setup() throws ServletException {
      context = mock(ApplicationContext.class);
      when(context.getAttribute("service.startup.class")).thenReturn(ApplicationResponseTest.class);
      when(context.getEnvironment()).thenReturn(() -> "Development");
      when(context.getLogger()).thenReturn(log);
      app = new ApplicationServlet(TestResource.class);
      app.init(new ApplicationConfig(context, "test"));
   }

   @Test
   public void testJobRequestEmpty() throws InterruptedException, IOException {
      String[] result = runJob(HttpRequestBuilder.build("/test/empty"));
      assertEquals("HTTP/1.1 204 No Content", result[0]);
   }

   @Test
   public void testJobRequestNotFound() throws InterruptedException, IOException {
      String[] result = runJob(HttpRequestBuilder.build("/test/notfound"));
      assertEquals("HTTP/1.1 404 Not Found", result[0]);
   }

   @Test
   public void testJobRequestWrongMethod() throws InterruptedException, IOException {
      String[] result = runJob(HttpRequestBuilder.build("POST", "/test/empty"));
      assertEquals("HTTP/1.1 405 Method Not Allowed", result[0]);
   }

   @Test
   public void testJobResquestString() throws InterruptedException, IOException {
      String[] result = runJob(HttpRequestBuilder.build("/test"));
      assertArrayEquals(new String[] { //
               "HTTP/1.1 200 OK", //
               "Content-Length: 4", //
               "Content-Type: text/plain; charset=utf-8", //
               "", //
               "Test" }, result);
   }

   @Test
   public void testJobRequestDownload() throws InterruptedException, IOException {
      String[] result = runJob(HttpRequestBuilder.build("/test/download"));
      assertArrayEquals(new String[] { //
               "HTTP/1.1 200 OK", //
               "Content-Disposition: attachment; filename=\"test.txt\"", //
               "Content-Length: 4", //
               "Content-Type: text/plain; charset=utf-8", //
               "", //
               "Test" }, result);
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

      @HttpGet
      @Route("download")
      public HttpInputStream download() {
         HttpInputStream result = new HttpInputStream(ContentType.TEXT, "Test".getBytes(), "test.txt");
         result.setDownloadable(true);
         return result;
      }
   }

   private String[] runJob(HttpRequest request) throws InterruptedException {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      HttpResponse response = new HttpResponse(result);
      ConnectionInfo conn = mock(ConnectionInfo.class);
      app.service(request, conn, response);
      response.commit(log);
      return new String(result.toByteArray()).split("\r\n");
   }
}
