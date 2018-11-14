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
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.HttpGet;
import com.lmpessoa.services.core.MediaType;
import com.lmpessoa.services.core.Route;
import com.lmpessoa.services.core.routing.IRouteOptions;
import com.lmpessoa.services.util.ConnectionInfo;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.NullLogger;

public final class ApplicationResponseTest {

   private ILogger log = new NullLogger();
   private ApplicationServer server;
   private Application app;

   public static void configure(IRouteOptions routes) {
      routes.addArea("", "^com.lmpessoa.services");
   }

   @Before
   public void setup() throws NoSuchMethodException, IllegalAccessException, UnknownHostException {
      server = mock(ApplicationServer.class);
      when(server.getResourceClasses()).thenReturn(Arrays.asList(TestResource.class));
      when(server.getEnvironment()).thenReturn(() -> "Development");
      when(server.getLogger()).thenReturn(log);
      app = new Application(server, ApplicationResponseTest.class);
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
      String[] result = runJob(
               new HttpRequestBuilder().setMethod("POST").setPath("/test/empty").build());
      assertEquals("HTTP/1.1 405 Method Not Allowed", result[0]);
   }

   @Test
   public void testJobResquestString() throws InterruptedException, IOException {
      String[] result = runJob(new HttpRequestBuilder().setPath("/test").build());
      assertArrayEquals(new String[] { //
               "HTTP/1.1 200 OK", //
               "Content-Type: text/plain", //
               "Content-Length: 4", //
               "", //
               "Test" }, result);
   }

   @Test
   public void testJobRequestDownload() throws InterruptedException, IOException {
      String[] result = runJob(new HttpRequestBuilder().setPath("/test/download").build());
      assertArrayEquals(new String[] { //
               "HTTP/1.1 200 OK", //
               "Content-Type: text/plain", //
               "Content-Length: 4", //
               "Content-Disposition: attachment; filename=\"test.txt\"", //
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
      public HttpResultInputStream download() {
         return new HttpResultInputStream(MediaType.TEXT, "Test".getBytes(), "test.txt");
      }
   }

   private String[] runJob(HttpRequest request) throws InterruptedException {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      Socket socket = mock(Socket.class);
      app.respondTo(request, new ConnectionInfo(socket), result);
      return new String(result.toByteArray()).split("\r\n");
   }
}
