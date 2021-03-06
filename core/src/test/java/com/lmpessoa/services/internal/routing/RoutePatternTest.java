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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.regex.Matcher;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.Route;
import com.lmpessoa.services.internal.parsing.ParseException;
import com.lmpessoa.services.internal.parsing.TypeMismatchException;
import com.lmpessoa.services.internal.services.NoSingleMethodException;
import com.lmpessoa.services.internal.services.ServiceMap;
import com.lmpessoa.services.test.services.Singleton;
import com.lmpessoa.services.test.services.SingletonImpl;

public final class RoutePatternTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private ServiceMap serviceMap;
   private RouteOptions options;

   @Before
   public void setup() {
      serviceMap = new ServiceMap();
      options = new RouteOptions();
   }

   // Class ----------

   @Test
   public void testClassWithMultipleConstructors() throws NoSingleMethodException {
      thrown.expect(NoSingleMethodException.class);
      RoutePattern.build("", MultipleInitResource.class, serviceMap, options);
   }

   @Test
   public void testClassNoParams() throws NoSingleMethodException {
      RoutePattern pat = RoutePattern.build("", TestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/test", pat.toString());
   }

   @Test
   public void testClassUnknownArgument() throws NoSingleMethodException {
      thrown.expect(TypeMismatchException.class);
      thrown.expectMessage("java.lang.Exception is not an acceptable route part");
      RoutePattern.build("", UnknownResource.class, serviceMap, options);
   }

   @Test
   public void testClassWithValidSimpleArea() throws NoSingleMethodException {
      RoutePattern pat = RoutePattern.build("api", TestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/api/test", pat.toString());
   }

   @Test
   public void testClassWithValidDualArea() throws NoSingleMethodException {
      RoutePattern pat = RoutePattern.build("api/v1", TestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/api/v1/test", pat.toString());
   }

   @Test
   public void testClassWithInvalidArea() throws NoSingleMethodException {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid area name: api+1");
      RoutePattern.build("api+1", TestResource.class, serviceMap, options);
   }

   @Test
   public void testClassCompositeName() throws NoSingleMethodException {
      RoutePattern pat = RoutePattern.build("", SimpleTestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/simple_test", pat.toString());
   }

   @Test
   public void testClassSimpleNameOneParam() throws NoSingleMethodException {
      RoutePattern pat = RoutePattern.build("", UserResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/user/(\\d+)", pat.toString());
   }

   @Test
   public void testClassCompositeNameOneParam() throws NoSingleMethodException {
      RoutePattern pat = RoutePattern.build("", UserOrderResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/user_order/(\\d+)", pat.toString());
   }

   @Test
   public void testClassRouteNoParams() throws NoSingleMethodException {
      RoutePattern pat = RoutePattern.build("", OrderResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/user_order", pat.toString());
   }

   @Test
   public void testClassRouteOneParam() throws NoSingleMethodException {
      RoutePattern pat = RoutePattern.build("", ExtraOrderResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/user/(\\d+)/order", pat.toString());
   }

   @Test
   public void testClassServiceNoArguments() throws NoSingleMethodException {
      serviceMap.put(Singleton.class, SingletonImpl.class);
      RoutePattern pat = RoutePattern.build("", ServicedTestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/test", pat.toString());
   }

   @Test
   public void testClassServiceOneArgument() throws NoSingleMethodException {
      serviceMap.put(Singleton.class, SingletonImpl.class);
      RoutePattern pat = RoutePattern.build("", MixedServiceTestResource.class, serviceMap,
               options);
      assertNotNull(pat);
      assertEquals("/test/(\\d+)", pat.toString());
   }

   @Test
   public void testClassServiceNotRegistered() throws NoSingleMethodException {
      thrown.expect(TypeMismatchException.class);
      thrown.expectMessage(Singleton.class.getName() + " is not an acceptable route part");
      RoutePattern.build("", ServicedTestResource.class, serviceMap, options);
   }

   // Method ----------

   @Test
   public void testMethodNoParams() throws NoSuchMethodException {
      RoutePattern pat = RoutePattern.build(null, TestResource.class.getMethod("test"));
      assertNotNull(pat);
      assertEquals("/test", pat.toString());
   }

   @Test
   public void testMethodOneParam() throws NoSuchMethodException {
      RoutePattern pat = RoutePattern.build(null, TestResource.class.getMethod("test", int.class));
      assertNotNull(pat);
      assertEquals("/test/(\\d+)", pat.toString());
   }

   @Test
   public void testMethodTwoParam() throws NoSuchMethodException {
      RoutePattern methodPattern = RoutePattern.build(null,
               TestResource.class.getMethod("test", int.class, String.class));
      assertNotNull(methodPattern);
      assertEquals("/test/(\\d+)/([^\\/]+)", methodPattern.toString());
   }

   @Test
   public void testMethodWithEnumParam() throws NoSuchMethodException {
      RoutePattern pat = RoutePattern.build(null,
               TestResource.class.getMethod("test", DayOfWeek.class));
      assertNotNull(pat);
      assertEquals("/test/([^\\/]+)", pat.toString());
   }

   @Test
   public void testMethodRouteNoParams() throws NoSuchMethodException {
      RoutePattern pat = RoutePattern.build(null, TestResource.class.getMethod("routed"));
      assertNotNull(pat);
      assertEquals("/route", pat.toString());
   }

   @Test
   public void testMethodRouteRightParams() throws NoSuchMethodException {
      RoutePattern pat = RoutePattern.build(null,
               TestResource.class.getMethod("routed", int.class, String.class));
      assertNotNull(pat);
      assertEquals("/route(\\d+)-([^\\/]+)", pat.toString());
   }

   @Test
   public void testMethodRouteFewerParams() throws NoSuchMethodException {
      thrown.expect(ParseException.class);
      thrown.expectMessage("Wrong parameter count in route (found: 1, expected: 2)");
      RoutePattern.build(null, TestResource.class.getMethod("wrong", int.class, int.class));
   }

   @Test
   public void testMethodRouteTooManyParams() throws NoSuchMethodException {
      thrown.expect(ParseException.class);
      thrown.expectMessage("Wrong parameter count in route (found: 1, expected: 0)");
      RoutePattern.build(null, TestResource.class.getMethod("wrong"));
   }

   @Test
   public void testMethodRouteWithConstraint() throws NoSuchMethodException {
      RoutePattern pat = RoutePattern.build(null,
               TestResource.class.getMethod("routed", String.class));
      assertNotNull(pat);
      assertEquals("/routed/([^\\/]{3,})", pat.toString());
   }

   @Test
   public void testMethodRouteWithTwoVariables() throws NoSuchMethodException {
      thrown.expect(ParseException.class);
      thrown.expectMessage("A literal must separate two variables");
      RoutePattern.build(null, TestResource.class.getMethod("wrong", String.class));
   }

   @Test
   public void testMethodRouteWithResource() throws NoSingleMethodException, NoSuchMethodException {
      RoutePattern pat = RoutePattern.build("", TestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/test", pat.toString());
      pat = RoutePattern.build(pat, TestResource.class.getMethod("routed"));
      assertNotNull(pat);
      assertEquals("/test/route", pat.toString());
   }

   @Test
   public void testMethodRouteWithInvertedArguments()
      throws NoSuchMethodException, NoSingleMethodException {
      RoutePattern pat = RoutePattern.build(null,
               TestResource.class.getMethod("routed", String.class, int.class));
      assertNotNull(pat);
      assertEquals("/(\\d+).([^\\/]+)", pat.toString());
   }

   @Test
   public void testMethodWithContentBody() throws NoSuchMethodException {
      RoutePattern pat = RoutePattern.build(null,
               TestResource.class.getMethod("content", SimpleTestResource.class));
      assertNotNull(pat);
      assertEquals("/content", pat.toString());
      assertEquals(SimpleTestResource.class, pat.getContentClass());
   }

   @Test
   public void testMethodWithWrongContentBody() throws NoSuchMethodException {
      thrown.expect(TypeMismatchException.class);
      thrown.expectMessage("java.time.LocalDate is not an acceptable route part");
      RoutePattern.build(null, TestResource.class.getMethod("content", LocalDate.class));
   }

   @Test
   public void testMethodWithVarargs() throws NoSuchMethodException {
      RoutePattern pat = RoutePattern.build(null,
               TestResource.class.getMethod("catchall", String[].class));
      assertNotNull(pat);
      assertEquals("/catchall((?:\\/[^\\/]+)*)", pat.toString());
   }

   @Test
   public void testMethodWithNotEmptyVarargs() throws NoSuchMethodException {
      RoutePattern pat = RoutePattern.build(null,
               TestResource.class.getMethod("catchall", int.class, String[].class));
      assertNotNull(pat);
      assertEquals("/catchall/(\\d+)((?:\\/[^\\/]+)+)", pat.toString());
   }

   // Pattern ----------

   @Test
   public void testPatternBasic() throws NoSingleMethodException {
      RoutePattern pat = RoutePattern.build("", TestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertTrue(pat.getPattern().matcher("/test").find());
   }

   @Test
   public void testPatternWithArgument() throws NoSingleMethodException, NoSuchMethodException {
      RoutePattern pat = RoutePattern.build("", TestResource.class, serviceMap, options);
      assertNotNull(pat);
      pat = RoutePattern.build(pat, TestResource.class.getMethod("test", int.class));
      assertNotNull(pat);
      Matcher matcher = pat.getPattern().matcher("/test/test/12");
      assertTrue(matcher.find());
      assertEquals("12", matcher.group(1));
   }

   @Test
   public void testPatternWithWrongArgument()
      throws NoSingleMethodException, NoSuchMethodException {
      RoutePattern pat = RoutePattern.build("", TestResource.class, serviceMap, options);
      assertNotNull(pat);
      pat = RoutePattern.build(pat, TestResource.class.getMethod("test", int.class));
      assertNotNull(pat);
      Matcher matcher = pat.getPattern().matcher("/test/ab");
      assertFalse(matcher.find());
   }

   // URI Production ----------

   @Test
   public void testBuildParameterless() throws NoSuchMethodException, NoSingleMethodException {
      RoutePattern parentPat = RoutePattern.build(null, TestResource.class, serviceMap, options);
      RoutePattern pat = RoutePattern.build(parentPat, TestResource.class.getMethod("test"));
      String url = pat.getPathWithArgs(new Object[0]);
      assertEquals("/test/test", url);
   }

   @Test
   public void testBuildParameterlessWithArgs()
      throws NoSuchMethodException, NoSingleMethodException {
      RoutePattern parentPat = RoutePattern.build(null, TestResource.class, serviceMap, options);
      RoutePattern pat = RoutePattern.build(parentPat, TestResource.class.getMethod("test"));
      String url = pat.getPathWithArgs(new Object[] { 1 });
      assertEquals("/test/test", url);
   }

   @Test
   public void testBuildWithArgs() throws NoSuchMethodException, NoSingleMethodException {
      RoutePattern parentPat = RoutePattern.build(null, TestResource.class, serviceMap, options);
      RoutePattern pat = RoutePattern.build(parentPat,
               TestResource.class.getMethod("test", int.class));
      String url = pat.getPathWithArgs(new Object[] { 1 });
      assertEquals("/test/test/1", url);
   }

   @Test
   public void testBuildWithFewerArgs() throws NoSuchMethodException, NoSingleMethodException {
      thrown.expect(ArrayIndexOutOfBoundsException.class);
      RoutePattern parentPat = RoutePattern.build(null, TestResource.class, serviceMap, options);
      RoutePattern pat = RoutePattern.build(parentPat,
               TestResource.class.getMethod("test", int.class, String.class));
      pat.getPathWithArgs(new Object[] { 1 });
   }

   @Test
   public void testBuildWithWrongArgs() throws NoSuchMethodException, NoSingleMethodException {
      RoutePattern parentPat = RoutePattern.build(null, TestResource.class, serviceMap, options);
      RoutePattern pat = RoutePattern.build(parentPat,
               TestResource.class.getMethod("test", int.class, String.class));
      String url = pat.getPathWithArgs(new Object[] { "test", 1 });
      assertEquals("/test/test/test/1", url);
   }

   @Test
   public void testBuildWithRouteArgs() throws NoSuchMethodException, NoSingleMethodException {
      RoutePattern parentPat = RoutePattern.build(null, TestResource.class, serviceMap, options);
      RoutePattern pat = RoutePattern.build(parentPat,
               TestResource.class.getMethod("routed", int.class, String.class));
      String url = pat.getPathWithArgs(new Object[] { 1, "test" });
      assertEquals("/test/route1-test", url);
   }

   @Test
   public void testBuildWithWrongRouteArgs() throws NoSuchMethodException, NoSingleMethodException {
      RoutePattern parentPat = RoutePattern.build(null, TestResource.class, serviceMap, options);
      RoutePattern pat = RoutePattern.build(parentPat,
               TestResource.class.getMethod("content", SimpleTestResource.class));
      String url = pat.getPathWithArgs(new Object[0]);
      assertEquals("/test/content", url);
   }

   // Test data ----------

   public static class MultipleInitResource {

      public MultipleInitResource() {
         // Test method, does nothing
      }

      public MultipleInitResource(int i) {
         // Test method, does nothing
      }
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

      @Route("{0}/view")
      public void wrong() {
         // Test method, does nothing
      }

      @Route("{0}/view")
      public void wrong(int i, int j) {
         // Test method, does nothing
      }

      @Route("{0}{1}")
      public void wrong(String s) {
         // Test method, does nothing
      }

      @Route("{1}.{0}")
      public void routed(String s, int i) {
         // Test method, does nothing
      }

      @Route("route")
      public void routed() {
         // Test method, does nothing
      }

      @Route("route{0}-{1}")
      public void routed(int i, String s) {
         // Test method, does nothing
      }

      public void routed(@Size(min = 3) String s) {
         // Test method, does nothing
      }

      public void content(SimpleTestResource res) {
         // Test method, does nothing
      }

      public void content(LocalDate date) {
         // Test method, does nothing
      }

      public void catchall(String... path) {
         // Test method, does nothing
      }

      public void catchall(int i, @NotEmpty String... path) {
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

   @Route("user_order")
   public static class OrderResource {}

   @Route("user/{0}/order")
   public static class ExtraOrderResource {

      public ExtraOrderResource(int i) {
         // Test method, does nothing
      }
   }

   public static class UnknownResource {

      public UnknownResource(Exception e) {
         // Test method, does nothing
      }
   }

   @Route("test")
   public static class ServicedTestResource {

      public ServicedTestResource(Singleton observer) {
         // Test method, does nothing
      }
   }

   @Route("test/{0}")
   public static class MixedServiceTestResource {

      public MixedServiceTestResource(int i, Singleton observer) {
         // Test method, does nothing
      }
   }
}
