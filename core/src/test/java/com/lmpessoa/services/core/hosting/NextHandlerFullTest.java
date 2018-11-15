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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.core.ContentType;
import com.lmpessoa.services.core.HttpGet;
import com.lmpessoa.services.core.MediaType;
import com.lmpessoa.services.core.Route;
import com.lmpessoa.services.core.hosting.ApplicationInfo;
import com.lmpessoa.services.core.hosting.ApplicationOptions;
import com.lmpessoa.services.core.hosting.ApplicationServer;
import com.lmpessoa.services.core.hosting.HttpRequest;
import com.lmpessoa.services.core.hosting.HttpResult;
import com.lmpessoa.services.core.hosting.HttpResultInputStream;
import com.lmpessoa.services.core.hosting.IApplicationInfo;
import com.lmpessoa.services.core.hosting.InternalServerError;
import com.lmpessoa.services.core.hosting.NotImplementedException;
import com.lmpessoa.services.core.routing.IRouteTable;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.routing.RouteTableBridge;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.NullLogger;

public final class NextHandlerFullTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private final ILogger log = new NullLogger();
   private ApplicationOptions app;
   private ServiceMap services;

   private HttpRequest request;
   private IRouteTable routes;
   private RouteMatch route;

   @Before
   public void setup() throws NoSuchMethodException {
      services = new ServiceMap();
      services.useSingleton(ILogger.class, log);
      services.useSingleton(IApplicationInfo.class, new ApplicationInfo(NextHandlerFullTest.class));
      services.useTransient(RouteMatch.class, () -> route);

      routes = RouteTableBridge.get(services, log);
      routes.put("", TestResource.class);

      app = new ApplicationOptions();
   }

   public HttpResult perform(String path) throws IOException {
      request = new HttpRequestBuilder() //
               .setMethod("GET") //
               .setPath(path) //
               .build();
      services.useSingleton(HttpRequest.class, request);
      route = RouteTableBridge.match(routes, request);
      return (HttpResult) app.getFirstHandler(services).invoke();
   }

   public String readAll(InputStream is) throws IOException {
      byte[] data = new byte[is.available()];
      is.read(data);
      return new String(data, Charset.forName("UTF-8"));
   }

   @Test
   public void testMediatorWithString() throws IOException {
      HttpResult result = perform("/test/string");
      assertEquals(200, result.getStatusCode());
      assertEquals("Test", result.getObject());
      assertNotNull(result.getInputStream());
      assertEquals(MediaType.TEXT, result.getInputStream().getContentType());
   }

   @Test
   public void testMediatorEmpty() throws IOException {
      HttpResult result = perform("/test/empty");
      assertEquals(204, result.getStatusCode());
      assertNull(result.getObject());
   }

   @Test
   public void testMediatorNotFound() throws IOException {
      HttpResult result = perform("/test/impl");
      assertEquals(501, result.getStatusCode());
   }

   @Test
   public void testMediatorError() throws IOException {
      HttpResult result = perform("/test/error");
      assertEquals(500, result.getStatusCode());
      assertTrue(result.getObject() instanceof InternalServerError);
      InternalServerError e = (InternalServerError) result.getObject();
      assertTrue(e.getCause() instanceof IllegalStateException);
   }

   @Test
   public void testMediatorWithObject() throws IOException {
      HttpResult result = perform("/test/object");
      assertEquals(200, result.getStatusCode());
      assertNotNull(result.getInputStream());
      assertEquals(MediaType.JSON, result.getInputStream().getContentType());
      String content = readAll(result.getInputStream());
      assertEquals("{\"id\":12,\"message\":\"Test\"}", content);
   }

   @Test
   public void testMediatorWithStream() throws IOException {
      HttpResult result = perform("/test/binary");
      assertEquals(200, result.getStatusCode());
      assertNotNull(result.getInputStream());
      assertEquals(MediaType.BINARY, result.getInputStream().getContentType());
      String content = readAll(result.getInputStream());
      assertEquals("Test", content);
   }

   @Test
   public void testMediatorWithTypedStream() throws IOException {
      HttpResult result = perform("/test/typed");
      assertEquals(200, result.getStatusCode());
      assertNotNull(result.getInputStream());
      assertEquals(MediaType.YAML, result.getInputStream().getContentType());
      String content = readAll(result.getInputStream());
      assertEquals("Test", content);
   }

   @Test
   public void testMediatorWithResultStream() throws IOException {
      HttpResult result = perform("/test/result");
      assertEquals(200, result.getStatusCode());
      assertNotNull(result.getInputStream());
      assertEquals(MediaType.YAML, result.getInputStream().getContentType());
      String content = readAll(result.getInputStream());
      assertEquals("Test", content);
   }

   @Test
   public void testMediatorWithFavicon() throws IOException, URISyntaxException {
      HttpResult result = perform("/test/favicon.ico");
      assertEquals(200, result.getStatusCode());
      assertNotNull(result.getInputStream());
      assertEquals(MediaType.ICO, result.getInputStream().getContentType());
      File favicon = new File(ApplicationServer.class.getResource("/favicon.ico").toURI());
      assertEquals(favicon.length(), result.getInputStream().available());
   }

   public static class TestObject {

      public final int id;
      public final String message;

      public TestObject(int id, String message) {
         this.message = message;
         this.id = id;
      }
   }

   public static class TestResource {

      @HttpGet
      @Route("string")
      public String string() {
         return "Test";
      }

      @HttpGet
      @Route("empty")
      public void empty() {
         // Test method, does nothing
      }

      @HttpGet
      @Route("impl")
      public void notimpl() {
         throw new NotImplementedException();
      }

      @HttpGet
      @Route("error")
      public void error() {
         throw new IllegalStateException("Test");
      }

      @HttpGet
      @Route("object")
      public TestObject object() {
         return new TestObject(12, "Test");
      }

      @HttpGet
      @Route("binary")
      public byte[] binary() {
         return "Test".getBytes();
      }

      @HttpGet
      @Route("typed")
      @ContentType(MediaType.YAML)
      public byte[] typed() {
         return "Test".getBytes();
      }

      @HttpGet
      @Route("result")
      @ContentType(MediaType.ATOM)
      public InputStream result() {
         return new HttpResultInputStream(MediaType.YAML, "Test".getBytes());
      }
   }
}
