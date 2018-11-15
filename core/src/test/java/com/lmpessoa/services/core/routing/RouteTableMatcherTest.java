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
package com.lmpessoa.services.core.routing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.core.MediaType;
import com.lmpessoa.services.core.Route;
import com.lmpessoa.services.core.hosting.HttpException;
import com.lmpessoa.services.core.hosting.HttpRequest;
import com.lmpessoa.services.core.hosting.HttpRequestBuilder;
import com.lmpessoa.services.core.hosting.MethodNotAllowedException;
import com.lmpessoa.services.core.hosting.NotFoundException;
import com.lmpessoa.services.core.hosting.NotImplementedException;
import com.lmpessoa.services.core.routing.HttpMethod;
import com.lmpessoa.services.core.routing.MatchedRoute;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.core.services.IServiceMap;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.NullLogger;

public final class RouteTableMatcherTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private final ILogger log = new NullLogger();
   private IServiceMap serviceMap;
   private RouteTable table;

   @Before
   public void setup() throws NoSuchMethodException {
      serviceMap = new ServiceMap();
      table = new RouteTable(serviceMap, log);
      table.put("", TestResource.class);
   }

   @Test
   public void testMatchesGetRoot() throws NoSuchMethodException, HttpException, IOException {
      RouteMatch result = table.matches(HttpRequestBuilder.build(HttpMethod.GET.name(), "/test"));
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("get"), route.getMethod());
      assertArrayEquals(new Object[0], route.getMethodArgs());
      assertEquals("GET/Test", result.invoke());
   }

   @Test
   public void testMatchesGetOneArg() throws NoSuchMethodException, HttpException, IOException {
      RouteMatch result = table.matches(HttpRequestBuilder.build(HttpMethod.GET.name(), "/test/7"));
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("get", int.class), route.getMethod());
      assertArrayEquals(new Object[] { 7 }, route.getMethodArgs());
      assertEquals("GET/7", result.invoke());
   }

   @Test
   public void testMatchesConstrainedRoute() throws NoSuchMethodException, HttpException, IOException {
      RouteMatch result = table.matches(HttpRequestBuilder.build(HttpMethod.GET.name(), "/test/abcd"));
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("get", String.class), route.getMethod());
      assertArrayEquals(new Object[] { "abcd" }, route.getMethodArgs());
      assertEquals("GET/abcd", result.invoke());
   }

   @Test
   public void testMatchesConstrainedRouteTooShort() throws NoSuchMethodException, HttpException, IOException {
      thrown.expect(NotFoundException.class);
      RouteMatch result = table.matches(HttpRequestBuilder.build(HttpMethod.GET.name(), "/test/ab"));
      result.invoke();
   }

   @Test
   public void testMatchesConstrainedRouteTooLong() throws NoSuchMethodException, HttpException, IOException {
      thrown.expect(NotFoundException.class);
      RouteMatch result = table.matches(HttpRequestBuilder.build(HttpMethod.GET.name(), "/test/abcdefg"));
      result.invoke();
   }

   @Test
   public void testMatchesGetTwoArgs() throws NoSuchMethodException, HttpException, IOException {
      RouteMatch result = table.matches(HttpRequestBuilder.build(HttpMethod.GET.name(), "/test/6/9"));
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("get", int.class, int.class), route.getMethod());
      assertArrayEquals(new Object[] { 6, 9 }, route.getMethodArgs());
      assertEquals("GET/6+9", result.invoke());
   }

   @Test
   public void testMatchesPostRoot() throws NoSuchMethodException, HttpException, IOException {
      RouteMatch result = table.matches(HttpRequestBuilder.build(HttpMethod.POST.name(), "/test"));
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("post"), route.getMethod());
      assertArrayEquals(new Object[0], route.getMethodArgs());
      assertEquals("POST/Test", result.invoke());
   }

   @Test
   public void testMatchesPostOneArg() throws NoSuchMethodException, HttpException, IOException {
      RouteMatch result = table.matches(HttpRequestBuilder.build(HttpMethod.POST.name(), "/test/7"));
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("post", int.class), route.getMethod());
      assertArrayEquals(new Object[] { 7 }, route.getMethodArgs());
      assertEquals("POST/7", result.invoke());
   }

   @Test
   public void testMatchesUnregisteredPath() throws IOException {
      thrown.expect(NotFoundException.class);
      RouteMatch result = table.matches(HttpRequestBuilder.build(HttpMethod.GET.name(), "/none"));
      result.invoke();
   }

   @Test
   public void testMatchesUnregisteredMethod() throws IOException {
      thrown.expect(MethodNotAllowedException.class);
      RouteMatch result = table.matches(HttpRequestBuilder.build(HttpMethod.DELETE.name(), "/test/7"));
      result.invoke();
   }

   @Test
   public void testMatchesHttpException() throws NoSuchMethodException, HttpException, IOException {
      thrown.expect(NotImplementedException.class);
      RouteMatch result = table.matches(HttpRequestBuilder.build(HttpMethod.PATCH.name(), "/test"));
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("patch"), route.getMethod());
      result.invoke();
   }

   @Test
   public void testMatchesWithService() throws NoSuchMethodException, HttpException, IOException {
      Message message = new Message();
      serviceMap.useSingleton(Message.class, message);
      table.put("", ServiceTestResource.class);
      RouteMatch result = table.matches(HttpRequestBuilder.build(HttpMethod.GET.name(), "/service"));
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(ServiceTestResource.class, route.getResourceClass());
      assertEquals(ServiceTestResource.class.getMethod("get"), route.getMethod());
      assertArrayEquals(new Object[] { message }, route.getConstructorArgs());
      assertEquals(message.get(), result.invoke());
   }

   @Test
   public void testMatchesWithContent() throws IOException, NoSuchMethodException {
      HttpRequest request = new HttpRequestBuilder().setMethod("PUT")
               .setPath("/test/12")
               .setBody("id=12&name=Test&email=test%40test.com&checked=true")
               .setContentType(MediaType.FORM)
               .build();
      RouteMatch result = table.matches(request);
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("put", int.class, ContentObject.class), route.getMethod());
      assertEquals(2, route.getMethodArgs().length);
      assertEquals(12, route.getMethodArgs()[0]);
      Object obj = route.getMethodArgs()[1];
      assertNotNull(obj);
      assertTrue(obj instanceof ContentObject);
      ContentObject cobj = (ContentObject) route.getMethodArgs()[1];
      assertEquals(12, cobj.id);
      assertEquals("Test", cobj.name);
      assertEquals("test@test.com", cobj.email);
      assertTrue(cobj.checked);
      ;
   }

   public static class ContentObject {

      public int id;
      public String name;
      public String email;
      public boolean checked;
   }

   public static class TestResource {

      public String get() {
         return "GET/Test";
      }

      public String get(int i) {
         return "GET/" + i;
      }

      @Route("{alpha(3..6)}")
      public String get(String s) {
         return "GET/" + s;
      }

      public String get(int i, int j) {
         return "GET/" + i + "+" + j;
      }

      public String post() {
         return "POST/Test";
      }

      public String post(int i) {
         return "POST/" + i;
      }

      public void patch() throws NotImplementedException {
         throw new NotImplementedException();
      }

      public void put(int i, ContentObject content) {

      }
   }

   static class Message implements Supplier<String> {

      @Override
      public String get() {
         return "Message";
      }

   }

   @Route("service")
   public static class ServiceTestResource {

      private Message message;

      public ServiceTestResource(Message message) {
         this.message = message;
      }

      public String get() {
         return message.get();
      }
   }
}
