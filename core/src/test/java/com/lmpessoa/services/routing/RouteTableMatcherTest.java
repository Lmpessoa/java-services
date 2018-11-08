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
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.core.HttpException;
import com.lmpessoa.services.core.MethodNotAllowedException;
import com.lmpessoa.services.core.NotFoundException;
import com.lmpessoa.services.core.NotImplementedException;

public final class RouteTableMatcherTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private RouteTable table;

   @Before
   public void setup() {
      table = new RouteTable();
      table.put(TestResource.class);
   }

   @Test
   public void testMatchesGetRoot() throws NoSuchMethodException, HttpException {
      MatchedRoute result = table.matches(HttpMethod.GET, "/test");
      assertNotNull(result);
      assertEquals(TestResource.class, result.getResourceClass());
      assertEquals(TestResource.class.getMethod("get"), result.getMethod());
      assertArrayEquals(new Object[0], result.getMethodArgs());
      assertEquals("GET/Test", result.invoke());
   }

   @Test
   public void testMatchesGetOneArg() throws NoSuchMethodException, HttpException {
      MatchedRoute result = table.matches(HttpMethod.GET, "/test/7");
      assertNotNull(result);
      assertEquals(TestResource.class, result.getResourceClass());
      assertEquals(TestResource.class.getMethod("get", int.class), result.getMethod());
      assertArrayEquals(new Object[] { 7 }, result.getMethodArgs());
      assertEquals("GET/7", result.invoke());
   }

   @Test
   public void testMatchesGetTwoArgs() throws NoSuchMethodException, HttpException {
      MatchedRoute result = table.matches(HttpMethod.GET, "/test/6/9");
      assertNotNull(result);
      assertEquals(TestResource.class, result.getResourceClass());
      assertEquals(TestResource.class.getMethod("get", int.class, int.class), result.getMethod());
      assertArrayEquals(new Object[] { 6, 9 }, result.getMethodArgs());
      assertEquals("GET/6+9", result.invoke());
   }

   @Test
   public void testMatchesPostRoot() throws NoSuchMethodException, HttpException {
      MatchedRoute result = table.matches(HttpMethod.POST, "/test");
      assertNotNull(result);
      assertEquals(TestResource.class, result.getResourceClass());
      assertEquals(TestResource.class.getMethod("post"), result.getMethod());
      assertArrayEquals(new Object[0], result.getMethodArgs());
      assertEquals("POST/Test", result.invoke());
   }

   @Test
   public void testMatchesPostOneArg() throws NoSuchMethodException, HttpException {
      MatchedRoute result = table.matches(HttpMethod.POST, "/test/7");
      assertNotNull(result);
      assertEquals(TestResource.class, result.getResourceClass());
      assertEquals(TestResource.class.getMethod("post", int.class), result.getMethod());
      assertArrayEquals(new Object[] { 7 }, result.getMethodArgs());
      assertEquals("POST/7", result.invoke());
   }

   @Test
   public void testMatchesUnregisteredPath() throws NotFoundException, MethodNotAllowedException {
      thrown.expect(NotFoundException.class);
      table.matches(HttpMethod.GET, "/none");
   }

   @Test
   public void testMatchesUnregisteredMethod() throws NotFoundException, MethodNotAllowedException {
      thrown.expect(MethodNotAllowedException.class);
      table.matches(HttpMethod.DELETE, "/test/7");
   }

   @Test
   public void testMatchesHttpException() throws NoSuchMethodException, HttpException {
      thrown.expect(NotImplementedException.class);
      MatchedRoute result = table.matches(HttpMethod.PATCH, "/test");
      assertNotNull(result);
      assertEquals(TestResource.class, result.getResourceClass());
      assertEquals(TestResource.class.getMethod("patch"), result.getMethod());
      result.invoke();
   }

   public static class TestResource {

      public String get() {
         return "GET/Test";
      }

      public String get(int i) {
         return "GET/" + i;
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
   }
}
