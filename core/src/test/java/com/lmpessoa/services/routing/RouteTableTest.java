/*
 * A light and easy engine for developing web APIs and microservices.
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
package com.lmpessoa.services.routing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Observer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.BrokerObserver;
import com.lmpessoa.services.core.HttpGet;
import com.lmpessoa.services.core.HttpPatch;
import com.lmpessoa.services.core.HttpPost;
import com.lmpessoa.services.core.HttpPut;
import com.lmpessoa.services.core.Route;
import com.lmpessoa.services.services.IServiceMap;
import com.lmpessoa.util.parsing.TypeMismatchException;

public final class RouteTableTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private IServiceMap serviceMap;
   private RouteTable table;

   @Before
   public void setup() {
      serviceMap = IServiceMap.newInstance();
      table = new RouteTable(serviceMap);
   }

   @Test
   public void testRoutesMapped() throws ParseException {
      Collection<Exception> result = table.put(TestResource.class);
      assertEquals(0, result.size());
      assertTrue(table.hasRoute("/test"));
      HttpMethod[] methods = table.listMethodsOf("/test");
      Arrays.sort(methods);
      assertArrayEquals(new HttpMethod[] { HttpMethod.GET, HttpMethod.POST }, methods);
      assertTrue(table.hasRoute("/test/{int}"));
      methods = table.listMethodsOf("/test/{int}");
      Arrays.sort(methods);
      assertArrayEquals(new HttpMethod[] { HttpMethod.GET, HttpMethod.PATCH }, methods);
   }

   @Test
   public void testRoutesDuplicated() throws ParseException, NoSuchMethodException {
      Collection<Exception> result = table.put(TestResource.class);
      assertEquals(0, result.size());
      result = table.put(AnotherTestResource.class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof DuplicateMethodException);
      HttpMethod[] methods = table.listMethodsOf("/test/{int}");
      Arrays.sort(methods);
      assertArrayEquals(new HttpMethod[] { HttpMethod.GET, HttpMethod.POST, HttpMethod.PATCH },
               methods);
      assertEquals(TestResource.class.getMethod("patch", int.class),
               table.getRouteMethod(HttpMethod.PATCH, "/test/{int}").getMethod());
   }

   @Test
   public void testRoutesWithEnum() {
      Collection<Exception> result = table.put(HttpMethod.class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithArray() {
      Collection<Exception> result = table.put(String[].class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithInterface() {
      Collection<Exception> result = table.put(IRouteTable.class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithAnnotation() {
      Collection<Exception> result = table.put(Route.class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithPrimitive() {
      Collection<Exception> result = table.put(int.class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithAbstractClass() {
      Collection<Exception> result = table.put(AbstractRouteType.class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesAnnotated() throws ParseException {
      Collection<Exception> result = table.put(AnnotatedResource.class);
      assertEquals(0, result.size());
      assertTrue(table.hasRoute("/test"));
      HttpMethod[] methods = table.listMethodsOf("/test");
      Arrays.sort(methods);
      assertArrayEquals(new HttpMethod[] { HttpMethod.GET, HttpMethod.POST }, methods);
      methods = table.listMethodsOf("/test/{int}");
      Arrays.sort(methods);
      assertArrayEquals(new HttpMethod[] { HttpMethod.PUT, HttpMethod.PATCH }, methods);
      Method methodPut = table.getRouteMethod(HttpMethod.PUT, "/test/{int}").getMethod();
      Method methodPatch = table.getRouteMethod(HttpMethod.PATCH, "/test/{int}").getMethod();
      assertSame(methodPut, methodPatch);
   }

   @Test
   public void testRouteDuplicated() throws ParseException {
      Collection<Exception> result = table.put(DuplicateTestResource.class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof DuplicateMethodException);
   }

   @Test
   public void testRouteWithContent() throws ParseException {
      Collection<Exception> result = table.put(ContentTestResource.class);
      assertEquals(0, result.size());
      assertTrue(table.hasRoute("/test"));
      assertArrayEquals(new HttpMethod[] { HttpMethod.POST }, table.listMethodsOf("/test"));
      assertEquals(AnotherTestResource.class,
               table.getRouteMethod(HttpMethod.POST, "/test").getContentClass());
   }

   @Test
   public void testRouteWithUnregisteredService() throws ParseException {
      Collection<Exception> result = table.put(ServiceTestResource.class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof TypeMismatchException);
      assertFalse(table.hasRoute("/test"));
   }

   @Test
   public void testRouteWithInjectedService() throws ParseException {
      Observer o = new BrokerObserver();
      serviceMap.putSingleton(Observer.class, o);
      Collection<Exception> result = table.put(ServiceTestResource.class);
      assertEquals(0, result.size());
      assertTrue(table.hasRoute("/test"));
      assertArrayEquals(new HttpMethod[] { HttpMethod.GET }, table.listMethodsOf("/test"));
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

      @HttpGet
      public void someMethod() {
         // Test method, does nothing
      }

      @HttpPost
      public void anotherMethod() {
         // Test method, does nothing
      }

      @HttpPut
      @HttpPatch
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

      public ServiceTestResource(Observer observer) {
         // Test method, does nothing
      }

      public void get() {
         // Test method, does nothing
      }
   }
}
