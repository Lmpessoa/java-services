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
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.ContentType;
import com.lmpessoa.services.core.HttpGet;
import com.lmpessoa.services.core.HttpInputStream;
import com.lmpessoa.services.core.Route;
import com.lmpessoa.services.core.hosting.ApplicationContext;
import com.lmpessoa.services.core.hosting.ApplicationResponder;
import com.lmpessoa.services.core.hosting.ApplicationServer;
import com.lmpessoa.services.core.hosting.ApplicationServerInfo;
import com.lmpessoa.services.core.routing.IRouteTable;
import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.util.logging.Logger;
import com.lmpessoa.services.util.logging.NullLogWriter;

public final class ApplicationResponseTest {

   private Logger log = new Logger(ApplicationResponseTest.class, new NullLogWriter());
   private ApplicationContext context;

   @Before
   public void setup() {
      ApplicationServerInfo info = mock(ApplicationServerInfo.class);
      ApplicationServer server = new ApplicationServer(ApplicationResponseTest.class, info, "Development", log);
      RouteTable routes = new RouteTable(server.getServices(), log);
      routes.put("", TestResource.class);
      context = new ApplicationContext(server, 5617, "test", routes);
      server.getServices().putRequestValue(IRouteTable.class, routes);
   }

   @Test
   public void testJobRequestEmpty() throws InterruptedException, IOException {
      String[] result = runJob("GET", "/test/empty");
      assertEquals("HTTP/1.1 204 No Content", result[0]);
   }

   @Test
   public void testJobRequestNotFound() throws InterruptedException, IOException {
      String[] result = runJob("GET", "/test/notfound");
      assertEquals("HTTP/1.1 404 Not Found", result[0]);
   }

   @Test
   public void testJobRequestWrongMethod() throws InterruptedException, IOException {
      String[] result = runJob("POST", "/test/empty");
      assertEquals("HTTP/1.1 405 Method Not Allowed", result[0]);
   }

   @Test
   public void testJobResquestString() throws InterruptedException, IOException {
      String[] result = runJob("GET", "/test");
      assertArrayEquals(new String[] { //
               "HTTP/1.1 200 OK", //
               "Content-Length: 4", //
               "Content-Type: text/plain", //
               "", //
               "Test" }, result);
   }

   @Test
   public void testJobRequestDownload() throws InterruptedException, IOException {
      String[] result = runJob("GET", "/test/download");
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
         Charset utf8 = Charset.forName("UTF-8");
         HttpInputStream result = new HttpInputStream(ContentType.TEXT, "Test".getBytes(utf8), utf8, "test.txt");
         result.setDownloadable(true);
         return result;
      }
   }

   private String[] runJob(String method, String path) throws InterruptedException, IOException {
      InputStream request = new HttpRequestBuilder().setMethod(method).setPath(path).buildAsStream();
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      Socket socket = mock(Socket.class);
      when(socket.getInputStream()).thenReturn(request);
      when(socket.getOutputStream()).thenReturn(result);
      ApplicationResponder app = new ApplicationResponder(context, socket);
      app.run();
      return new String(result.toByteArray()).split("\r\n");
   }
}
