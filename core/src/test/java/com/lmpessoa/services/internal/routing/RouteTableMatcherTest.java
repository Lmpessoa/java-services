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
package com.lmpessoa.services.internal.routing;

import static com.lmpessoa.services.routing.HttpMethod.DELETE;
import static com.lmpessoa.services.routing.HttpMethod.PATCH;
import static com.lmpessoa.services.routing.HttpMethod.POST;
import static com.lmpessoa.services.routing.HttpMethod.PUT;
import static com.lmpessoa.services.services.Reuse.ALWAYS;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Supplier;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.BadRequestException;
import com.lmpessoa.services.ContentType;
import com.lmpessoa.services.MethodNotAllowedException;
import com.lmpessoa.services.NotFoundException;
import com.lmpessoa.services.NotImplementedException;
import com.lmpessoa.services.Put;
import com.lmpessoa.services.Query;
import com.lmpessoa.services.Route;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.internal.hosting.HttpRequestBuilder;
import com.lmpessoa.services.internal.services.ServiceMap;
import com.lmpessoa.services.internal.validating.ValidationService;
import com.lmpessoa.services.routing.RouteMatch;
import com.lmpessoa.services.services.Service;
import com.lmpessoa.services.validating.ErrorSet;
import com.lmpessoa.services.validating.IValidationService;

public final class RouteTableMatcherTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private ServiceMap serviceMap;
   private RouteTable table;

   @Before
   public void setup() {
      serviceMap = new ServiceMap();
      serviceMap.put(IValidationService.class, ValidationService.instance());
      table = new RouteTable(serviceMap);
      table.put("", TestResource.class);
   }

   @Test
   public void testMatchesGetRoot() throws NoSuchMethodException, IOException {
      RouteMatch result = table.matches(new HttpRequestBuilder().setPath("/test").build());
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("get"), route.getMethod());
      assertArrayEquals(new Object[0], route.getMethodArgs());
      assertEquals("GET/Test", result.invoke());
   }

   @Test
   public void testMatchesGetOneArg() throws NoSuchMethodException, IOException {
      RouteMatch result = table.matches(new HttpRequestBuilder().setPath("/test/7").build());
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("get", int.class), route.getMethod());
      assertArrayEquals(new Object[] { 7 }, route.getMethodArgs());
      assertEquals("GET/7", result.invoke());
   }

   @Test
   public void testMatchesConstrainedRoute() throws NoSuchMethodException, IOException {
      RouteMatch result = table.matches(new HttpRequestBuilder().setPath("/test/abcd").build());
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("get", String.class), route.getMethod());
      assertArrayEquals(new Object[] { "abcd" }, route.getMethodArgs());
      assertEquals("GET/abcd", result.invoke());
   }

   @Test
   public void testMatchesConstrainedRouteTooShort() throws NoSuchMethodException, IOException {
      // With the advent of catchall route tests, this test no longer throws a 404 error
      RouteMatch result = table.matches(new HttpRequestBuilder().setPath("/test/ab").build());
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("get", String[].class), route.getMethod());
      assertArrayEquals(new Object[] { new String[] { "ab" } }, route.getMethodArgs());
      assertEquals("GET/ab", route.invoke());
   }

   @Test
   public void testMatchesConstrainedRouteTooLong() throws NoSuchMethodException, IOException {
      // With the advent of catchall route tests, this test no longer throws a 404 error
      RouteMatch result = table.matches(new HttpRequestBuilder().setPath("/test/abcdefg").build());
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("get", String[].class), route.getMethod());
      assertArrayEquals(new Object[] { new String[] { "abcdefg" } }, route.getMethodArgs());
      assertEquals("GET/abcdefg", route.invoke());
   }

   @Test
   public void testMatchesGetTwoArgs() throws NoSuchMethodException, IOException {
      RouteMatch result = table.matches(new HttpRequestBuilder().setPath("/test/6/9").build());
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("get", int.class, int.class), route.getMethod());
      assertArrayEquals(new Object[] { 6, 9 }, route.getMethodArgs());
      assertEquals("GET/6+9", result.invoke());
   }

   @Test
   public void testMatchesPostRoot() throws NoSuchMethodException, IOException {
      RouteMatch result = table
               .matches(new HttpRequestBuilder().setMethod(POST).setPath("/test").build());
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("post"), route.getMethod());
      assertArrayEquals(new Object[0], route.getMethodArgs());
      assertEquals("POST/Test", result.invoke());
   }

   @Test
   public void testMatchesPostOneArg() throws NoSuchMethodException, IOException {
      RouteMatch result = table
               .matches(new HttpRequestBuilder().setMethod(POST).setPath("/test/7").build());
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
      RouteMatch result = table.matches(new HttpRequestBuilder().setPath("/none").build());
      result.invoke();
   }

   @Test
   public void testMatchesUnregisteredMethod() throws IOException {
      thrown.expect(MethodNotAllowedException.class);
      RouteMatch result = table
               .matches(new HttpRequestBuilder().setMethod(DELETE).setPath("/test/7").build());
      result.invoke();
   }

   @Test
   public void testMatchesHttpException() throws NoSuchMethodException, IOException {
      thrown.expect(NotImplementedException.class);
      RouteMatch result = table
               .matches(new HttpRequestBuilder().setMethod(PATCH).setPath("/test").build());
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("patch"), route.getMethod());
      result.invoke();
   }

   @Test
   public void testMatchesWithService() throws NoSuchMethodException, IOException {
      Message message = new Message();
      serviceMap.put(Message.class, message);
      table.put("", ServiceTestResource.class);
      RouteMatch result = table.matches(new HttpRequestBuilder().setPath("/service").build());
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(ServiceTestResource.class, route.getResourceClass());
      assertEquals(ServiceTestResource.class.getMethod("get"), route.getMethod());
      assertArrayEquals(new Object[] { message }, route.getConstructorArgs());
      assertEquals(message.get(), result.invoke());
   }

   @Test
   public void testMatchesWithoutContent() throws NoSuchMethodException, IOException {
      HttpRequest request = new HttpRequestBuilder().setMethod(PUT).setPath("/test/12").build();
      RouteMatch result = table.matches(request);
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("put", ContentObject.class, int.class),
               route.getMethod());
      assertEquals(2, route.getMethodArgs().length);
      assertEquals(12, route.getMethodArgs()[1]);
      assertNull(route.getMethodArgs()[0]);
   }

   @Test
   public void testMatchesWithContent() throws NoSuchMethodException, IOException {
      HttpRequest request = new HttpRequestBuilder().setMethod(PUT)
               .setPath("/test/12")
               .setBody("id=12&name=Test&email=test.com&checked=false")
               .setContentType(ContentType.FORM)
               .build();
      RouteMatch result = table.matches(request);
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("put", ContentObject.class, int.class),
               route.getMethod());
      assertEquals(2, route.getMethodArgs().length);
      assertEquals(12, route.getMethodArgs()[1]);
      Object obj = route.getMethodArgs()[0];
      assertNotNull(obj);
      assertTrue(obj instanceof ContentObject);
      ContentObject cobj = (ContentObject) obj;
      assertEquals(12, cobj.id);
      assertEquals("Test", cobj.name);
      assertEquals("test.com", cobj.email);
      assertFalse(cobj.checked);
   }

   @Test
   public void testMatchesWithInvalidContent() throws NoSuchMethodException, IOException {
      HttpRequest request = new HttpRequestBuilder().setMethod(PUT)
               .setPath("/test/valid/12")
               .setBody("id=12&name=Test&email=test@test.com")
               .setContentType(ContentType.FORM)
               .build();
      RouteMatch result = table.matches(request);
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("valid", ContentObject.class, int.class),
               route.getMethod());
      assertEquals(2, route.getMethodArgs().length);
      assertEquals(12, route.getMethodArgs()[1]);
      Object obj = route.getMethodArgs()[0];
      assertNotNull(obj);
      assertTrue(obj instanceof ContentObject);
      ContentObject cobj = (ContentObject) obj;
      assertEquals(12, cobj.id);
      assertEquals("Test", cobj.name);
      assertEquals("test@test.com", cobj.email);
      assertFalse(cobj.checked);

      try {
         result.invoke();
         throw new IllegalStateException();
      } catch (BadRequestException e) {
         assertNotNull(e.getErrors());
         assertFalse(e.getErrors().isEmpty());
         Iterator<ErrorSet.Entry> messages = e.getErrors().iterator();

         ErrorSet.Entry message = messages.next();
         assertEquals("content.checked", message.getPathEntry());
         assertEquals("false", message.getInvalidValue());
         assertEquals("{javax.validation.constraints.AssertTrue.message}",
                  message.getMessageTemplate());

         assertFalse(messages.hasNext());
      } catch (Exception e) {
         Assert.fail();
      }
   }

   @Test
   public void testMatchesWithValidContent() throws NoSuchMethodException, IOException {
      HttpRequest request = new HttpRequestBuilder().setMethod(PUT)
               .setPath("/test/valid/12")
               .setBody("id=12&name=Test&email=test@test.com&checked=true")
               .setContentType(ContentType.FORM)
               .build();
      RouteMatch result = table.matches(request);
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("valid", ContentObject.class, int.class),
               route.getMethod());
      assertEquals(2, route.getMethodArgs().length);
      assertEquals(12, route.getMethodArgs()[1]);
      Object obj = route.getMethodArgs()[0];
      assertNotNull(obj);
      assertTrue(obj instanceof ContentObject);
      ContentObject cobj = (ContentObject) obj;
      assertEquals(12, cobj.id);
      assertEquals("Test", cobj.name);
      assertEquals("test@test.com", cobj.email);
      assertTrue(cobj.checked);

      result.invoke();
   }

   @Test
   public void testMatchesWithQueryParam() throws NoSuchMethodException, IOException {
      HttpRequest request = new HttpRequestBuilder().setPath("/test/query")
               .setQueryString("type=class&id=7")
               .build();
      RouteMatch result = table.matches(request);
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("getNew", Integer.class), route.getMethod());
      assertArrayEquals(new Object[] { 7 }, route.getMethodArgs());
      Object invokeResult = route.invoke();
      assertTrue(invokeResult instanceof String);
      assertEquals("7", invokeResult);
   }

   @Test
   public void testMatchesWithMissingQueryParam() throws NoSuchMethodException, IOException {
      HttpRequest request = new HttpRequestBuilder().setPath("/test/query")
               .setQueryString("type=class&ids=7")
               .build();
      RouteMatch result = table.matches(request);
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("getNew", Integer.class), route.getMethod());
      assertArrayEquals(new Object[] { null }, route.getMethodArgs());
      Object invokeResult = route.invoke();
      assertTrue(invokeResult instanceof String);
      assertEquals("null", invokeResult);
   }

   @Test
   public void testMatchesWithWrongQueryParam() throws NoSuchMethodException, IOException {
      thrown.expect(BadRequestException.class);
      HttpRequest request = new HttpRequestBuilder().setPath("/test/query/12")
               .setQueryString("id=class")
               .build();
      RouteMatch result = table.matches(request);
      assertTrue(result instanceof BadRequestException);
      BadRequestException exception = (BadRequestException) result;
      assertEquals(TestResource.class, exception.getResourceClass());
      assertEquals(TestResource.class.getMethod("getNew", int.class, int.class),
               exception.getMethod());
      exception.invoke();
   }

   @Test
   public void testMatchCatchAllWithNoPath() throws NoSuchMethodException, IOException {
      HttpRequest request = new HttpRequestBuilder().setPath("/test/catchall").build();
      RouteMatch result = table.matches(request);
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("catchall", String[].class), route.getMethod());
      assertArrayEquals(new Object[] { new String[0] }, route.getMethodArgs());
   }

   @Test
   public void testMatchCatchAllWithSinglePath() throws NoSuchMethodException, IOException {
      HttpRequest request = new HttpRequestBuilder().setPath("/test/catchall/7").build();
      RouteMatch result = table.matches(request);
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("catchall", String[].class), route.getMethod());
      assertArrayEquals(new Object[] { new String[] { "7" } }, route.getMethodArgs());
   }

   @Test
   public void testMatchCatchAllWithMultiplePath() throws NoSuchMethodException, IOException {
      HttpRequest request = new HttpRequestBuilder().setPath("/test/catchall/path/to/real/object")
               .build();
      RouteMatch result = table.matches(request);
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("catchall", String[].class), route.getMethod());
      assertArrayEquals(new Object[] { new String[] { "path", "to", "real", "object" } },
               route.getMethodArgs());
   }

   @Test
   public void testMatchCatchAllWithOtherMethods() throws NoSuchMethodException, IOException {
      HttpRequest request = new HttpRequestBuilder().setPath("/test/path/to/real/object").build();
      RouteMatch result = table.matches(request);
      assertTrue(result instanceof MatchedRoute);
      MatchedRoute route = (MatchedRoute) result;
      assertEquals(TestResource.class, route.getResourceClass());
      assertEquals(TestResource.class.getMethod("get", String[].class), route.getMethod());
      assertArrayEquals(new Object[] { new String[] { "path", "to", "real", "object" } },
               route.getMethodArgs());
   }

   public static class ContentObject {

      public int id;

      public String name;

      @Email
      public String email;

      @AssertTrue
      public boolean checked;
   }

   public static class TestResource {

      public String get() {
         return "GET/Test";
      }

      public String get(int i) {
         return "GET/" + i;
      }

      public String get(@Size(min = 3, max = 6) String s) {
         return "GET/" + s;
      }

      public String get(String... path) {
         return "GET/" + String.join(",", path);
      }

      public String get(int i, int j) {
         return "GET/" + i + "+" + j;
      }

      @Route("query")
      public String getNew(@Query Integer id) {
         return String.valueOf(id);
      }

      @Route("query/{0}")
      public String getNew(int cid, @Query int id) {
         return String.valueOf(cid + id);
      }

      public String post() {
         return "POST/Test";
      }

      public String post(int i) {
         return "POST/" + i;
      }

      public void patch() {
         throw new NotImplementedException();
      }

      public void put(ContentObject content, int i) {
         // Nothing to do here
      }

      @Put
      @Route("valid/{1}")
      public void valid(@Valid ContentObject content, int i) {
         // Nothing to do here
      }

      @Route("catchall/{0}")
      public void catchall(String... path) {
         // Nothing to do here
      }
   }

   @Service(reuse = ALWAYS)
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
