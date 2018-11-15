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
import com.lmpessoa.services.core.routing.AbstractRouteType;
import com.lmpessoa.services.core.routing.DuplicateMethodException;
import com.lmpessoa.services.core.routing.HttpMethod;
import com.lmpessoa.services.core.routing.IRouteTable;
import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.logging.Logger;
import com.lmpessoa.services.util.logging.NullLogWriter;
import com.lmpessoa.services.util.parsing.TypeMismatchException;

public final class RouteTableTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private final Logger log = new Logger(RouteTableTest.class, new NullLogWriter());
   private ServiceMap serviceMap;
   private RouteTable table;

   @Before
   public void setup() throws NoSuchMethodException {
      serviceMap = new ServiceMap();
      table = new RouteTable(serviceMap, log);
   }

   @Test
   public void testRoutesMapped() throws ParseException {
      table.put("", TestResource.class);
      Collection<Exception> result = table.getLastExceptions();
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
      table.put("", TestResource.class);
      Collection<Exception> result = table.getLastExceptions();
      assertEquals(0, result.size());
      table.put("", AnotherTestResource.class);
      result = table.getLastExceptions();
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof DuplicateMethodException);
      HttpMethod[] methods = table.listMethodsOf("/test/{int}");
      Arrays.sort(methods);
      assertArrayEquals(new HttpMethod[] { HttpMethod.GET, HttpMethod.POST, HttpMethod.PATCH }, methods);
      assertEquals(TestResource.class.getMethod("patch", int.class),
               table.getRouteMethod(HttpMethod.PATCH, "/test/{int}").getMethod());
   }

   @Test
   public void testRoutesWithEnum() {
      table.put("", HttpMethod.class);
      Collection<Exception> result = table.getLastExceptions();
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithArray() {
      table.put("", String[].class);
      Collection<Exception> result = table.getLastExceptions();
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithInterface() {
      table.put("", IRouteTable.class);
      Collection<Exception> result = table.getLastExceptions();
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithAnnotation() {
      table.put("", Route.class);
      Collection<Exception> result = table.getLastExceptions();
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithPrimitive() {
      table.put("", int.class);
      Collection<Exception> result = table.getLastExceptions();
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithAbstractClass() {
      table.put("", AbstractRouteType.class);
      Collection<Exception> result = table.getLastExceptions();
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesAnnotated() throws ParseException {
      table.put("", AnnotatedResource.class);
      Collection<Exception> result = table.getLastExceptions();
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
      table.put("", DuplicateTestResource.class);
      Collection<Exception> result = table.getLastExceptions();
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof DuplicateMethodException);
   }

   @Test
   public void testRouteWithContent() throws ParseException {
      table.put("", ContentTestResource.class);
      Collection<Exception> result = table.getLastExceptions();
      assertEquals(0, result.size());
      assertTrue(table.hasRoute("/test"));
      assertArrayEquals(new HttpMethod[] { HttpMethod.POST }, table.listMethodsOf("/test"));
      assertEquals(AnotherTestResource.class, table.getRouteMethod(HttpMethod.POST, "/test").getContentClass());
   }

   @Test
   public void testRouteWithUnregisteredService() throws ParseException {
      table.put("", ServiceTestResource.class);
      Collection<Exception> result = table.getLastExceptions();
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof TypeMismatchException);
      assertFalse(table.hasRoute("/test"));
   }

   @Test
   public void testRouteWithInjectedService() throws ParseException {
      Observer o = new BrokerObserver();
      serviceMap.useSingleton(Observer.class, o);
      table.put("", ServiceTestResource.class);
      Collection<Exception> result = table.getLastExceptions();
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
