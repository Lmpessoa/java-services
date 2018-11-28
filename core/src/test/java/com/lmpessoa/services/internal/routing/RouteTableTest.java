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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.Patch;
import com.lmpessoa.services.Post;
import com.lmpessoa.services.Put;
import com.lmpessoa.services.Route;
import com.lmpessoa.services.internal.parsing.TypeMismatchException;
import com.lmpessoa.services.internal.routing.RouteEntry;
import com.lmpessoa.services.internal.routing.RouteTable;
import com.lmpessoa.services.internal.routing.VariableRoutePart;
import com.lmpessoa.services.internal.services.ServiceMap;
import com.lmpessoa.services.routing.HttpMethod;
import com.lmpessoa.services.routing.IRouteTable;
import com.lmpessoa.services.test.services.Singleton;
import com.lmpessoa.services.test.services.SingletonImpl;

public final class RouteTableTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private ServiceMap serviceMap;
   private RouteTable table;

   @Before
   public void setup() {
      serviceMap = new ServiceMap();
      table = new RouteTable(serviceMap);
   }

   @Test
   public void testRoutesMapped() {
      Collection<RouteEntry> result = table.put("", TestResource.class);
      assertFalse(hasDuplicate(result));
      assertFalse(hasException(result).isPresent());
      assertTrue(table.hasRoute("/test"));
      HttpMethod[] methods = table.listMethodsOf("/test");
      Arrays.sort(methods);
      assertArrayEquals(new HttpMethod[] { HttpMethod.GET, HttpMethod.POST }, methods);
      assertTrue(table.hasRoute("/test/(\\d+)"));
      methods = table.listMethodsOf("/test/(\\d+)");
      Arrays.sort(methods);
      assertArrayEquals(new HttpMethod[] { HttpMethod.GET, HttpMethod.PATCH }, methods);
   }

   @Test
   public void testRoutesDuplicated() throws NoSuchMethodException {
      Collection<RouteEntry> result = table.put("", TestResource.class);
      assertFalse(hasDuplicate(result));
      assertFalse(hasException(result).isPresent());
      result = table.put("", AnotherTestResource.class);
      assertTrue(hasDuplicate(result));
      assertFalse(hasException(result).isPresent());
      HttpMethod[] methods = table.listMethodsOf("/test/(\\d+)");
      Arrays.sort(methods);
      assertArrayEquals(new HttpMethod[] { HttpMethod.GET, HttpMethod.POST, HttpMethod.PATCH },
               methods);
      assertEquals(TestResource.class.getMethod("patch", int.class),
               table.getRouteMethod(HttpMethod.PATCH, "/test/(\\d+)").getMethod());
   }

   @Test
   public void testRoutesWithEnum() {
      Collection<RouteEntry> result = table.put("", HttpMethod.class);
      Optional<Exception> e = hasException(result);
      assertTrue(e.isPresent());
      assertTrue(e.get() instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithArray() {
      Collection<RouteEntry> result = table.put("", String[].class);
      Optional<Exception> e = hasException(result);
      assertTrue(e.isPresent());
      assertTrue(e.get() instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithInterface() {
      Collection<RouteEntry> result = table.put("", IRouteTable.class);
      Optional<Exception> e = hasException(result);
      assertTrue(e.isPresent());
      assertTrue(e.get() instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithAnnotation() {
      Collection<RouteEntry> result = table.put("", Route.class);
      Optional<Exception> e = hasException(result);
      assertTrue(e.isPresent());
      assertTrue(e.get() instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithPrimitive() {
      Collection<RouteEntry> result = table.put("", int.class);
      Optional<Exception> e = hasException(result);
      assertTrue(e.isPresent());
      assertTrue(e.get() instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithAbstractClass() {
      Collection<RouteEntry> result = table.put("", VariableRoutePart.class);
      Optional<Exception> e = hasException(result);
      assertTrue(e.isPresent());
      assertTrue(e.get() instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesAnnotated() {
      Collection<RouteEntry> result = table.put("", AnnotatedResource.class);
      assertFalse(hasDuplicate(result));
      assertFalse(hasException(result).isPresent());
      assertTrue(table.hasRoute("/test"));
      HttpMethod[] methods = table.listMethodsOf("/test");
      Arrays.sort(methods);
      assertArrayEquals(new HttpMethod[] { HttpMethod.GET, HttpMethod.POST }, methods);
      methods = table.listMethodsOf("/test/(\\d+)");
      Arrays.sort(methods);
      assertArrayEquals(new HttpMethod[] { HttpMethod.PUT, HttpMethod.PATCH }, methods);
      Method methodPut = table.getRouteMethod(HttpMethod.PUT, "/test/(\\d+)").getMethod();
      Method methodPatch = table.getRouteMethod(HttpMethod.PATCH, "/test/(\\d+)").getMethod();
      assertSame(methodPut, methodPatch);
   }

   @Test
   public void testRouteDuplicated() {
      Collection<RouteEntry> result = table.put("", DuplicateTestResource.class);
      assertTrue(hasDuplicate(result));
      assertFalse(hasException(result).isPresent());
   }

   @Test
   public void testRouteWithContent() {
      Collection<RouteEntry> result = table.put("", ContentTestResource.class);
      assertFalse(hasDuplicate(result));
      assertFalse(hasException(result).isPresent());
      assertTrue(table.hasRoute("/test"));
      assertArrayEquals(new HttpMethod[] { HttpMethod.POST }, table.listMethodsOf("/test"));
      assertEquals(AnotherTestResource.class,
               table.getRouteMethod(HttpMethod.POST, "/test").getContentClass());
   }

   @Test
   public void testRouteWithUnregisteredService() {
      Collection<RouteEntry> result = table.put("", ServiceTestResource.class);
      assertFalse(hasDuplicate(result));
      Optional<Exception> e = hasException(result);
      assertTrue(e.isPresent());
      assertTrue(e.get() instanceof TypeMismatchException);
      assertFalse(table.hasRoute("/test"));
   }

   @Test
   public void testRouteWithInjectedService() {
      Singleton o = new SingletonImpl();
      serviceMap.put(Singleton.class, o);
      Collection<RouteEntry> result = table.put("", ServiceTestResource.class);
      assertFalse(hasDuplicate(result));
      assertFalse(hasException(result).isPresent());
      assertTrue(table.hasRoute("/test"));
      assertArrayEquals(new HttpMethod[] { HttpMethod.GET }, table.listMethodsOf("/test"));
   }

   @Test
   public void testRouteProduced() {
      table.put("", TestResource.class);
      String url = table.findPathTo(TestResource.class, "get");
      assertEquals("/test", url);
   }

   @Test
   public void testRouteProducedWithArgs() {
      table.put("", TestResource.class);
      String url = table.findPathTo(TestResource.class, "get", 1);
      assertEquals("/test/1", url);
   }

   @Test
   public void testRouteProduceNoMethod() {
      table.put("", TestResource.class);
      assertNull(table.findPathTo(TestResource.class, "post", 1));
   }

   private boolean hasDuplicate(Collection<RouteEntry> entries) {
      return entries.stream().anyMatch(r -> r.getDuplicateOf() != null);
   }

   private Optional<Exception> hasException(Collection<RouteEntry> entries) {
      return entries.stream().map(RouteEntry::getError).filter(Objects::nonNull).findAny();
   }

   public static class TestResource {

      public void get() {
         // Test method, does nothing
      }

      public void get(int i) {
         // Test method, does nothing
      }

      public void post() {
         // Test method, does nothing
      }

      public void patch(int i) {
         // Test method, does nothing
      }
   }

   @Route("test")
   public static class AnotherTestResource {

      public void post(int i) {
         // Test method, dows nothing
      }

      public void patch(int i) {
         // Test method, does nothing
      }
   }

   @Route("test")
   public static class AnnotatedResource {

      @Route("")
      public void someMethod() {
         // Test method, does nothing
      }

      @Post
      @Route("")
      public void anotherMethod() {
         // Test method, does nothing
      }

      @Put
      @Patch
      @Route("{0}")
      public void invalidAnnotations(int i) {
         // Test method, does nothing
      }
   }

   @Route("test")
   public static class DuplicateTestResource {

      public void post() {
         // Test method, does nothing
      }

      public void post(AnotherTestResource res) {
         // Test method, does nothing
      }
   }

   @Route("test")
   public static class ContentTestResource {

      public void post(AnotherTestResource res) {
         // Test method, does nothing
      }
   }

   @Route("test")
   public static class ServiceTestResource {

      public ServiceTestResource(Singleton observer) {
         // Test method, does nothing
      }

      public void get() {
         // Test method, does nothing
      }
   }
}
