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
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class RouteTableTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private RouteTable routeMap;

   @Before
   public void setup() {
      routeMap = new RouteTable();
   }

   @Test
   public void testRoutesMapped() throws ParseException {
      routeMap.put(TestResource.class);
      assertTrue(routeMap.hasRoute("/test"));
      HttpMethod[] methods = routeMap.listMethodsOf("/test");
      Arrays.sort(methods);
      assertArrayEquals(new HttpMethod[] { HttpMethod.GET, HttpMethod.POST }, methods);
      assertTrue(routeMap.hasRoute("/test/{int}"));
      methods = routeMap.listMethodsOf("/test/{int}");
      Arrays.sort(methods);
      assertArrayEquals(new HttpMethod[] { HttpMethod.GET, HttpMethod.PATCH }, methods);
   }

   @Test
   public void testRoutesDuplicated() throws ParseException, NoSuchMethodException {
      routeMap.put(TestResource.class);
      routeMap.put(AnotherTestResource.class);
      HttpMethod[] methods = routeMap.listMethodsOf("/test/{int}");
      Arrays.sort(methods);
      assertArrayEquals(new HttpMethod[] { HttpMethod.GET, HttpMethod.POST, HttpMethod.PATCH },
               methods);
      assertEquals(TestResource.class.getMethod("patch", int.class),
               routeMap.getRouteMethod(HttpMethod.PATCH, "/test/{int}").getMethod());
   }

   @Test
   public void testRoutesWithEnum() {
      Collection<Exception> result = routeMap.put(HttpMethod.class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithArray() {
      Collection<Exception> result = routeMap.put(String[].class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithInterface() {
      Collection<Exception> result = routeMap.put(IRouteTable.class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithAnnotation() {
      Collection<Exception> result = routeMap.put(Route.class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithPrimitive() {
      Collection<Exception> result = routeMap.put(int.class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
   }

   @Test
   public void testRoutesWithAbstractClass() {
      Collection<Exception> result = routeMap.put(AbstractRouteType.class);
      assertEquals(1, result.size());
      assertTrue(result.toArray()[0] instanceof IllegalArgumentException);
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
}
