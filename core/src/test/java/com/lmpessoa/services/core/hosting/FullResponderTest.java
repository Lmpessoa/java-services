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
import static com.lmpessoa.services.core.routing.HttpMethod.PATCH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.core.hosting.ApplicationOptions;
import com.lmpessoa.services.core.hosting.ApplicationServer;
import com.lmpessoa.services.core.hosting.ConnectionInfo;
import com.lmpessoa.services.core.hosting.ContentType;
import com.lmpessoa.services.core.hosting.HeaderEntry;
import com.lmpessoa.services.core.hosting.Headers;
import com.lmpessoa.services.core.hosting.HttpInputStream;
import com.lmpessoa.services.core.hosting.HttpRequest;
import com.lmpessoa.services.core.hosting.HttpRequestImpl;
import com.lmpessoa.services.core.hosting.HttpResult;
import com.lmpessoa.services.core.hosting.IApplicationSettings;
import com.lmpessoa.services.core.hosting.NotImplementedException;
import com.lmpessoa.services.core.hosting.Redirect;
import com.lmpessoa.services.core.routing.HttpGet;
import com.lmpessoa.services.core.routing.HttpMethod;
import com.lmpessoa.services.core.routing.HttpPatch;
import com.lmpessoa.services.core.routing.HttpPost;
import com.lmpessoa.services.core.routing.MatchedRouteBridge;
import com.lmpessoa.services.core.routing.QueryParam;
import com.lmpessoa.services.core.routing.Route;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.core.serializing.Serializer;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.core.validating.IValidationService;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.Logger;
import com.lmpessoa.services.util.logging.NullHandler;

public final class FullResponderTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private final Logger log = new Logger(new NullHandler());
   private final ConnectionInfo connect;
   private ApplicationOptions app;
   private ServiceMap services;

   private HttpRequest request;
   private RouteTable routes;
   private RouteMatch route;

   public FullResponderTest() {
      Socket socket = mock(Socket.class);
      connect = new ConnectionInfo(socket, "https://lmpessoa.com/");
   }

   @Before
   public void setup() {
      app = new ApplicationOptions(null);
      services = app.getServices();
      services.put(ILogger.class, log);
      services.put(ConnectionInfo.class, (Supplier<ConnectionInfo>) () -> connect);
      IApplicationSettings settings = mock(IApplicationSettings.class);
      when(settings.getStartupClass()).then(n -> FullResponderTest.class);
      services.put(IApplicationSettings.class, settings);
      services.put(RouteMatch.class, (Supplier<RouteMatch>) () -> route);
      services.put(IValidationService.class, IValidationService.newInstance());

      routes = app.getRoutes();
      routes.put("", TestResource.class);
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
      assertNotNull(result.getInputStream());
      assertEquals(ContentType.TEXT, result.getInputStream().getType());
   }

   @Test
   public void testMediatorEmpty() throws IOException {
      HttpResult result = perform("/test/empty");
      assertEquals(204, result.getStatusCode());
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
      HttpInputStream is = result.getInputStream();
      assertEquals(ContentType.TEXT, is.getType());
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      is.sendTo(out);
      assertEquals("java.lang.IllegalStateException: Test", new String(out.toByteArray(), is.getEncoding()));
   }

   @Test
   public void testMediatorWithObject() throws IOException {
      HttpResult result = perform("/test/object");
      assertEquals(200, result.getStatusCode());
      assertNotNull(result.getInputStream());
      assertEquals(ContentType.JSON, result.getInputStream().getType());
      String content = readAll(result.getInputStream());
      assertEquals("{\"id\":12,\"message\":\"Test\"}", content);
   }

   @Test
   public void testMediatorWithObjectPost() throws IOException, NoSuchMethodException {
      HttpResult result = performFile("/http/multi_post_request.txt");
      assertEquals(200, result.getStatusCode());
      assertNotNull(result.getInputStream());
      assertEquals(ContentType.JSON, result.getInputStream().getType());
      String content = readAll(result.getInputStream());
      assertEquals("12", content);

      assertTrue(MatchedRouteBridge.isMatchedRoute(route));
      assertEquals(TestResource.class.getMethod("object", TestObject.class),
               MatchedRouteBridge.getMatchedRouteMethod(route));
      Object[] args = MatchedRouteBridge.getMatchedRouteMethodArgs(route);
      assertEquals(1, args.length);

      assertTrue(args[0] instanceof TestObject);
      TestObject obj = (TestObject) args[0];
      assertEquals(12, obj.id);
      assertEquals("Test", obj.message);

      assertTrue(obj.file instanceof HttpInputStream);
      HttpInputStream file = (HttpInputStream) obj.file;
      assertEquals("file1.txt", file.getFilename());
      assertEquals(ContentType.TEXT, file.getType());
      content = readAll(file);
      assertEquals("...contents of file1.txt...", content);
   }

   @Test
   public void testMediatorWithStream() throws IOException {
      HttpResult result = perform("/test/binary");
      assertEquals(200, result.getStatusCode());
      assertNotNull(result.getInputStream());
      assertEquals(ContentType.BINARY, result.getInputStream().getType());
      String content = readAll(result.getInputStream());
      assertEquals("Test", content);
   }

   @Test
   public void testMediatorWithTypedStream() throws IOException {
      HttpResult result = perform("/test/typed");
      assertEquals(200, result.getStatusCode());
      assertNotNull(result.getInputStream());
      assertEquals(ContentType.YAML, result.getInputStream().getType());
      String content = readAll(result.getInputStream());
      assertEquals("Test", content);
   }

   @Test
   public void testMediatorWithResultStream() throws IOException {
      HttpResult result = perform("/test/result");
      assertEquals(200, result.getStatusCode());
      assertNotNull(result.getInputStream());
      assertEquals(ContentType.YAML, result.getInputStream().getType());
      String content = readAll(result.getInputStream());
      assertEquals("Test", content);
   }

   @Test
   public void testMediatorWithFavicon() throws IOException, URISyntaxException {
      HttpResult result = perform("/test/favicon.ico");
      assertEquals(200, result.getStatusCode());
      assertNotNull(result.getInputStream());
      assertEquals(ContentType.ICO, result.getInputStream().getType());
      File favicon = new File(ApplicationServer.class.getResource("/favicon.ico").toURI());
      assertEquals(favicon.length(), result.getInputStream().available());
   }

   @Test
   public void testMediatorWithRedirect() throws IOException {
      HttpResult result = perform("/test/redirect");
      assertEquals(302, result.getStatusCode());
      assertNull(result.getInputStream());
      for (HeaderEntry entry : result.getHeaders()) {
         if (entry.getKey().equals(Headers.LOCATION)) {
            assertEquals("https://lmpessoa.com/test/7", entry.getValue());
         }
      }
   }

   @Test
   public void testMediatorWithInvalidParamToJson() throws IOException {
      HttpResult result = performFile("/http/invalid_patch_request.txt");
      assertEquals(400, result.getStatusCode());
      assertNotNull(result.getInputStream());
      assertEquals(ContentType.JSON, result.getInputStream().getType());
      byte[] data = new byte[result.getInputStream().available()];
      result.getInputStream().read(data);
      String content = new String(data, Serializer.UTF_8);
      assertEquals("{\"errors\":[{\"path\":\"value.invalid\","
               + "\"message\":\"Some error message\",\"invalidValue\":\"null\"}]}", content);
   }

   @Test
   public void testMediatorWithInvalidParamToXml() throws IOException {
      Serializer.enableXml(true);
      HttpResult result = performFile("/http/invalid_patch_request.txt");
      assertEquals(400, result.getStatusCode());
      assertNotNull(result.getInputStream());
      assertEquals(ContentType.XML, result.getInputStream().getType());
      byte[] data = new byte[result.getInputStream().available()];
      result.getInputStream().read(data);
      String content = new String(data, Serializer.UTF_8);
      assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + "<errors>\n"
               + "  <error path=\"value.invalid\" message=\"Some error message\" invalidValue=\"null\"/>\n"
               + "</errors>\n", content);
   }

   @Test
   public void testMediatorWithoutInvalidParam() {
      HttpResult result = perform(PATCH, "/test/invalid");
      assertEquals(204, result.getStatusCode());
      assertNull(result.getInputStream());
   }

   @Test
   public void testMediatorWithoutQueryParam() throws IOException {
      HttpResult result = performFile("/http/query_get_request.txt");
      assertEquals(400, result.getStatusCode());
      assertNotNull(result.getInputStream());
      assertEquals(ContentType.TEXT, result.getInputStream().getType());
      byte[] data = new byte[result.getInputStream().available()];
      result.getInputStream().read(data);
      String content = new String(data, Serializer.UTF_8);
      assertEquals("java.lang.IllegalArgumentException", content);
   }

   private HttpResult perform(String path) {
      return perform(GET, path);
   }

   private HttpResult perform(HttpMethod method, String path) {
      request = new HttpRequestBuilder().setMethod(method).setPath(path).build();
      services.put(HttpRequest.class, (Supplier<HttpRequest>) () -> request);
      route = routes.matches(request);
      return (HttpResult) app.getFirstResponder().invoke();
   }

   private HttpResult performFile(String resource) throws IOException {
      try (InputStream res = FullResponderTest.class.getResourceAsStream(resource)) {
         request = new HttpRequestImpl(res);
         services.put(HttpRequest.class, (Supplier<HttpRequest>) () -> request);
         route = routes.matches(request);
         return (HttpResult) app.getFirstResponder().invoke();
      }
   }

   public static class TestObject {

      public int id;
      public String message;
      public InputStream file;

      public TestObject() {
      }

      public TestObject(int id, String message) {
         this.message = message;
         this.id = id;
      }
   }

   public static class InvalidTestObject extends TestObject {

      @NotNull(message = "Some error message")
      private final String invalid = null;
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

      @HttpPost
      @Route("object")
      public int object(TestObject value) {
         return value.id;
      }

      @HttpPatch
      @Route("invalid")
      public void invalid(@Valid InvalidTestObject value) {
         // Nothing to do here
      }

      @HttpGet
      @Route("binary")
      public byte[] binary() {
         return "Test".getBytes();
      }

      @HttpGet
      @Route("typed")
      @ContentType(ContentType.YAML)
      public byte[] typed() {
         return "Test".getBytes();
      }

      @HttpGet
      @Route("result")
      @ContentType(ContentType.ATOM)
      public InputStream result() {
         return new HttpInputStream(ContentType.YAML, "Test".getBytes());
      }

      @HttpGet
      @Route("redirect")
      public Redirect redirect() {
         return Redirect.to("/test/7");
      }

      @HttpGet
      @Route("query")
      public String query(@QueryParam int id) {
         return String.valueOf(id);
      }
   }
}
