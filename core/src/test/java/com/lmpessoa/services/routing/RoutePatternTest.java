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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.text.ParseException;
import java.time.DayOfWeek;

import org.junit.Test;

public final class RoutePatternTest {

   @Test
   public void testClassNoParams() throws NoSingleMethodException, ParseException {
      RoutePattern pat = RoutePattern.build("", TestResource.class);
      assertNotNull(pat);
      assertEquals("/test", pat.toString());
   }

   @Test
   public void testClassCompositeName() throws NoSingleMethodException, ParseException {
      RoutePattern pat = RoutePattern.build("", SimpleTestResource.class);
      assertNotNull(pat);
      assertEquals("/simple_test", pat.toString());
   }

   @Test
   public void testClassSimpleNameOneParam() throws NoSingleMethodException, ParseException {
      RoutePattern pat = RoutePattern.build("", UserResource.class);
      assertNotNull(pat);
      assertEquals("/user/{int}", pat.toString());
   }

   @Test
   public void testClassCompositeNameOneParam() throws NoSingleMethodException, ParseException {
      RoutePattern pat = RoutePattern.build("", UserOrderResource.class);
      assertNotNull(pat);
      assertEquals("/user_order/{int}", pat.toString());
   }

   @Test
   public void testMethodNoParams() throws NoSuchMethodException, ParseException {
      RoutePattern pat = RoutePattern.build(null, TestResource.class.getMethod("test"));
      assertNotNull(pat);
      assertEquals("/", pat.toString());
   }

   @Test
   public void testMethodOneParam() throws NoSuchMethodException, ParseException {
      RoutePattern pat = RoutePattern.build(null, TestResource.class.getMethod("test", int.class));
      assertNotNull(pat);
      assertEquals("/{int}", pat.toString());
   }

   @Test
   public void testMethodTwoParam() throws NoSuchMethodException, ParseException {
      RoutePattern methodPattern = RoutePattern.build(null,
               TestResource.class.getMethod("test", int.class, String.class));
      assertNotNull(methodPattern);
      assertEquals("/{int}/{any}", methodPattern.toString());
   }

   @Test
   public void testMethodWithEnumParam() throws NoSuchMethodException, ParseException {
      RoutePattern pat = RoutePattern.build(null,
               TestResource.class.getMethod("test", DayOfWeek.class));
      assertNotNull(pat);
      assertEquals("/{any}", pat.toString());
   }

   public static class TestResource {

      public void test() {
         // Test method, does nothing
      }

      public void test(int i) {
         // Test method, does nothing
      }

      public void test(int i, String s) {
         // Test method, does nothing
      }

      public void test(DayOfWeek weedkay) {
         // Test method, does nothing
      }
   }

   public static class SimpleTestResource {}

   public static class UserResource {

      public UserResource(int i) {
         // Test method, does nothing
      }
   }

   public static class UserOrderResource {

      public UserOrderResource(int i) {
         // Test method, does nothing
      }
   }

}
