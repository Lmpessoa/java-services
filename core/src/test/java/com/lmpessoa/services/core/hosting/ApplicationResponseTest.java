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

import static com.lmpessoa.services.core.routing.HttpMethod.GET;
import static com.lmpessoa.services.core.routing.HttpMethod.POST;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.concurrent.ExecutionService;
import com.lmpessoa.services.core.hosting.ApplicationContext;
import com.lmpessoa.services.core.hosting.ApplicationRequestJob;
import com.lmpessoa.services.core.hosting.ApplicationServer;
import com.lmpessoa.services.core.hosting.ApplicationSettings;
import com.lmpessoa.services.core.hosting.ContentType;
import com.lmpessoa.services.core.hosting.Headers;
import com.lmpessoa.services.core.hosting.HttpInputStream;
import com.lmpessoa.services.core.routing.HttpGet;
import com.lmpessoa.services.core.routing.HttpMethod;
import com.lmpessoa.services.core.routing.IRouteTable;
import com.lmpessoa.services.core.routing.Route;
import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.core.security.Authorize;
import com.lmpessoa.services.core.security.ClaimType;
import com.lmpessoa.services.core.security.GenericIdentity;
import com.lmpessoa.services.core.security.IIdentity;
import com.lmpessoa.services.core.security.IIdentityProvider;
import com.lmpessoa.services.util.logging.Logger;
import com.lmpessoa.services.util.logging.NullHandler;

public final class ApplicationResponseTest {

   private Logger log = new Logger(new NullHandler());
   private ApplicationContext context;

   @Before
   public void setup() {
      ApplicationSettings settings = mock(ApplicationSettings.class);
      when(settings.getStartupClass()).then(n -> ApplicationResponseTest.class);
      when(settings.getEnvironment()).thenReturn(() -> "Development");
      when(settings.getLogger()).thenReturn(log);
      when(settings.getJobExecutor()).thenReturn(new ExecutionService(0, log));
      when(settings.getValidationService()).thenCallRealMethod();
      ApplicationServer server = new ApplicationServer(settings);
      server.getOptions().useIdentity(new IIdentityProvider() {

         @Override
         public IIdentity getIdentity(String format, String token) {
            if (token == null) {
               return null;
            }
            GenericIdentity id = new GenericIdentity();
            id.addClaim(ClaimType.DISPLAY_NAME, "Jane Doe");
            return id;
         }
      });
      RouteTable routes = server.getOptions().getRoutes();
      routes.put("", TestResource.class);
      context = new ApplicationContext(server, 5617, "test", routes);
      server.getOptions().getServices().putRequestValue(IRouteTable.class, routes);
   }

   @Test
   public void testJobRequestEmpty() throws InterruptedException, IOException {
      String[] result = runJob(GET, "/test/empty");
      assertEquals("HTTP/1.1 204 No Content", result[0]);
   }

   @Test
   public void testJobRequestNotFound() throws InterruptedException, IOException {
      String[] result = runJob(GET, "/test/notfound");
      assertEquals("HTTP/1.1 404 Not Found", result[0]);
   }

   @Test
   public void testJobRequestWrongMethod() throws InterruptedException, IOException {
      String[] result = runJob(POST, "/test/empty");
      assertEquals("HTTP/1.1 405 Method Not Allowed", result[0]);
   }

   @Test
   public void testJobResquestString() throws InterruptedException, IOException {
      String[] result = runJob(GET, "/test");
      assertArrayEquals(new String[] { //
               "HTTP/1.1 200 OK", //
               "Content-Type: text/plain; charset=\"utf-8\"", //
               "Content-Length: 4", //
               "", //
               "Test" }, result);
   }

   @Test
   public void testJobRequestDownload() throws InterruptedException, IOException {
      String[] result = runJob(GET, "/test/download");
      assertArrayEquals(new String[] { //
               "HTTP/1.1 200 OK", //
               "Content-Type: text/plain; charset=\"utf-8\"", //
               "Content-Length: 4", //
               "Content-Disposition: attachment; filename=\"test.txt\"", //
               "", //
               "Test" }, result);
   }

   @Test
   public void testJobRequestAllowed() throws InterruptedException, IOException {
      String[] result = runJob(GET, "/test/empty", true);
      assertEquals("HTTP/1.1 204 No Content", result[0]);
   }

   @Test
   public void testJobRequestUnauthenticated() throws InterruptedException, IOException {
      String[] result = runJob(POST, "/test");
      assertEquals("HTTP/1.1 401 Unauthorized", result[0]);
   }

   @Test
   public void testJobRequestAuthenticated() throws InterruptedException, IOException {
      String[] result = runJob(POST, "/test", true);
      assertArrayEquals(new String[] { "HTTP/1.1 200 OK", //
               "Content-Type: text/plain; charset=\"utf-8\"", //
               "Content-Length: 4", //
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
         Charset utf8 = Charset.forName("UTF-8");
         HttpInputStream result = new HttpInputStream(ContentType.TEXT, "Test".getBytes(utf8), utf8, "test.txt");
         result.setDownloadable(true);
         return result;
      }

      @Authorize
      public String post() {
         return "Test";
      }
   }

   private String[] runJob(HttpMethod method, String path) throws InterruptedException, IOException {
      return runJob(method, path, false);
   }

   private String[] runJob(HttpMethod method, String path, boolean useIdentity) throws IOException {
      HttpRequestBuilder builder = new HttpRequestBuilder().setMethod(method).setPath(path);
      if (useIdentity) {
         builder.addHeader(Headers.AUTHORIZATION, "Token sample");
      }
      InputStream request = builder.buildAsStream();
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      Socket socket = mock(Socket.class);
      when(socket.getInputStream()).thenReturn(request);
      when(socket.getOutputStream()).thenReturn(result);
      ApplicationRequestJob app = new ApplicationRequestJob(context, socket);
      app.run();
      return new String(result.toByteArray()).split("\r\n");
   }
}
