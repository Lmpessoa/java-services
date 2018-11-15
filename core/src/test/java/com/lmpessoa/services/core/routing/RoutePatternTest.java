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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.time.DayOfWeek;
import java.util.Observer;
import java.util.regex.Matcher;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.BrokerObserver;
import com.lmpessoa.services.core.Route;
import com.lmpessoa.services.core.routing.AbstractRouteType;
import com.lmpessoa.services.core.routing.RouteOptions;
import com.lmpessoa.services.core.routing.RoutePattern;
import com.lmpessoa.services.core.services.NoSingleMethodException;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.parsing.TypeMismatchException;

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
   public void testClassWithMultipleConstructors() throws NoSingleMethodException, ParseException {
      thrown.expect(NoSingleMethodException.class);
      RoutePattern.build("", MultipleInitResource.class, serviceMap, options);
   }

   @Test
   public void testClassNoParams() throws NoSingleMethodException, ParseException {
      RoutePattern pat = RoutePattern.build("", TestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/test", pat.toString());
   }

   @Test
   public void testClassUnknownArgument() throws NoSingleMethodException, ParseException {
      thrown.expect(TypeMismatchException.class);
      thrown.expectMessage("java.lang.Exception is not an acceptable route part");
      RoutePattern.build("", UnknownResource.class, serviceMap, options);
   }

   @Test
   public void testClassWithValidSimpleArea() throws NoSingleMethodException, ParseException {
      RoutePattern pat = RoutePattern.build("api", TestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/api/test", pat.toString());
   }

   @Test
   public void testClassWithValidDualArea() throws NoSingleMethodException, ParseException {
      RoutePattern pat = RoutePattern.build("api/v1", TestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/api/v1/test", pat.toString());
   }

   @Test
   public void testClassWithInvalidArea() throws NoSingleMethodException, ParseException {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid area: api+1");
      RoutePattern.build("api+1", TestResource.class, serviceMap, options);
   }

   @Test
   public void testClassCompositeName() throws NoSingleMethodException, ParseException {
      RoutePattern pat = RoutePattern.build("", SimpleTestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/simple_test", pat.toString());
   }

   @Test
   public void testClassSimpleNameOneParam() throws NoSingleMethodException, ParseException {
      RoutePattern pat = RoutePattern.build("", UserResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/user/{int}", pat.toString());
   }

   @Test
   public void testClassCompositeNameOneParam() throws NoSingleMethodException, ParseException {
      RoutePattern pat = RoutePattern.build("", UserOrderResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/user_order/{int}", pat.toString());
   }

   @Test
   public void testClassRouteNoParams() throws NoSingleMethodException, ParseException {
      RoutePattern pat = RoutePattern.build("", OrderResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/user_order", pat.toString());
   }

   @Test
   public void testClassRouteOneParam() throws NoSingleMethodException, ParseException {
      RoutePattern pat = RoutePattern.build("", ExtraOrderResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/user/{int}/order", pat.toString());
   }

   @Test
   public void testClassServiceNoArguments() throws NoSingleMethodException, ParseException {
      serviceMap.useSingleton(Observer.class, BrokerObserver.class);
      RoutePattern pat = RoutePattern.build("", ServicedTestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/test", pat.toString());
   }

   @Test
   public void testClassServiceOneArgument() throws NoSingleMethodException, ParseException {
      serviceMap.useSingleton(Observer.class, BrokerObserver.class);
      RoutePattern pat = RoutePattern.build("", MixedServiceTestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/test/{int}", pat.toString());
   }

   @Test
   public void testClassServiceNotRegistered() throws NoSingleMethodException, ParseException {
      thrown.expect(TypeMismatchException.class);
      thrown.expectMessage(Observer.class.getName() + " is not an acceptable route part");
      RoutePattern.build("", ServicedTestResource.class, serviceMap, options);
   }

   // Method ----------

   @Test
   public void testMethodNoParams() throws NoSuchMethodException, ParseException {
      RoutePattern pat = RoutePattern.build(null, TestResource.class.getMethod("test"), options);
      assertNotNull(pat);
      assertEquals("/", pat.toString());
   }

   @Test
   public void testMethodOneParam() throws NoSuchMethodException, ParseException {
      RoutePattern pat = RoutePattern.build(null, TestResource.class.getMethod("test", int.class), options);
      assertNotNull(pat);
      assertEquals("/{int}", pat.toString());
   }

   @Test
   public void testMethodTwoParam() throws NoSuchMethodException, ParseException {
      RoutePattern methodPattern = RoutePattern.build(null,
               TestResource.class.getMethod("test", int.class, String.class), options);
      assertNotNull(methodPattern);
      assertEquals("/{int}/{any}", methodPattern.toString());
   }

   @Test
   public void testMethodWithEnumParam() throws NoSuchMethodException, ParseException {
      RoutePattern pat = RoutePattern.build(null, TestResource.class.getMethod("test", DayOfWeek.class), options);
      assertNotNull(pat);
      assertEquals("/{any}", pat.toString());
   }

   @Test
   public void testMethodRouteNoParams() throws NoSuchMethodException, ParseException {
      RoutePattern pat = RoutePattern.build(null, TestResource.class.getMethod("routed"), options);
      assertNotNull(pat);
      assertEquals("/route", pat.toString());
   }

   @Test
   public void testMethodRouteRightParams() throws NoSuchMethodException, ParseException {
      RoutePattern pat = RoutePattern.build(null, TestResource.class.getMethod("routed", int.class, String.class),
               options);
      assertNotNull(pat);
      assertEquals("/route{int}-{alpha}", pat.toString());
   }

   @Test
   public void testMethodRouteFewerParams() throws NoSuchMethodException, ParseException {
      thrown.expect(ParseException.class);
      thrown.expectMessage("Wrong parameter count in route (found: 1, expected: 2)");
      RoutePattern.build(null, TestResource.class.getMethod("wrong", int.class, int.class), options);
   }

   @Test
   public void testMethodRouteTooManyParams() throws NoSuchMethodException, ParseException {
      thrown.expect(ParseException.class);
      thrown.expectMessage("Wrong parameter count in route (found: 1, expected: 0)");
      RoutePattern.build(null, TestResource.class.getMethod("wrong"), options);
   }

   @Test
   public void testMethodRouteUnknownType() throws NoSuchMethodException, ParseException {
      thrown.expect(ParseException.class);
      thrown.expectMessage("Unknown route type: year");
      RoutePattern.build(null, TestResource.class.getMethod("wrong", int.class), options);
   }

   @Test
   public void testMethodRouteWithConstraint() throws NoSuchMethodException, ParseException {
      RoutePattern pat = RoutePattern.build(null, TestResource.class.getMethod("routed", String.class), options);
      assertNotNull(pat);
      assertEquals("/{alpha(3..)}", pat.toString());
   }

   @Test
   public void testMethodRouteWithTwoVariables() throws NoSuchMethodException, ParseException {
      thrown.expect(ParseException.class);
      thrown.expectMessage("A literal must separate two variables");
      RoutePattern.build(null, TestResource.class.getMethod("wrong", String.class), options);
   }

   @Test
   public void testMethodRouteWithResource() throws NoSingleMethodException, ParseException, NoSuchMethodException {
      RoutePattern pat = RoutePattern.build("", TestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertEquals("/test", pat.toString());
      pat = RoutePattern.build(pat, TestResource.class.getMethod("routed"), options);
      assertNotNull(pat);
      assertEquals("/test/route", pat.toString());
   }

   @Test
   public void testMethodRouteWithWrongType() throws NoSuchMethodException, ParseException {
      thrown.expect(TypeMismatchException.class);
      thrown.expectMessage("Cannot cast 'alpha' to int");
      RoutePattern.build(null, TestResource.class.getMethod("wrong", String.class, int.class), options);
   }

   @Test
   public void testMethodWithContentBody() throws NoSuchMethodException, ParseException {
      RoutePattern pat = RoutePattern.build(null, TestResource.class.getMethod("content", SimpleTestResource.class),
               options);
      assertNotNull(pat);
      assertEquals("/", pat.toString());
      assertEquals(SimpleTestResource.class, pat.getContentClass());
   }

   @Test
   public void testMethodWithWrongContentBody() throws NoSuchMethodException, ParseException {
      thrown.expect(TypeMismatchException.class);
      thrown.expectMessage("java.lang.String[] is not an acceptable route part");
      RoutePattern.build(null, TestResource.class.getMethod("content", String[].class), options);
   }

   // Pattern ----------

   @Test
   public void testPatternBasic() throws NoSingleMethodException, ParseException {
      RoutePattern pat = RoutePattern.build("", TestResource.class, serviceMap, options);
      assertNotNull(pat);
      assertTrue(pat.getPattern().matcher("/test").find());
   }

   @Test
   public void testPatternWithArgument() throws NoSingleMethodException, ParseException, NoSuchMethodException {
      RoutePattern pat = RoutePattern.build("", TestResource.class, serviceMap, options);
      assertNotNull(pat);
      pat = RoutePattern.build(pat, TestResource.class.getMethod("test", int.class), options);
      assertNotNull(pat);
      Matcher matcher = pat.getPattern().matcher("/test/12");
      assertTrue(matcher.find());
      assertEquals("12", matcher.group(1));
   }

   @Test
   public void testPatternWithWrongArgument() throws NoSingleMethodException, ParseException, NoSuchMethodException {
      RoutePattern pat = RoutePattern.build("", TestResource.class, serviceMap, options);
      assertNotNull(pat);
      pat = RoutePattern.build(pat, TestResource.class.getMethod("test", int.class), options);
      assertNotNull(pat);
      Matcher matcher = pat.getPattern().matcher("/test/ab");
      assertFalse(matcher.find());
   }

   // Custom types ----------

   @Test
   public void testCustomType() throws NoSuchMethodException, ParseException {
      options.addType("custom", CustomRouteType.class);
      RoutePattern pat = RoutePattern.build(null, TestResource.class.getMethod("custom", Custom.class), options);
      assertNotNull(pat);
      assertEquals("/{any}", pat.toString());
      Matcher matcher = pat.getPattern().matcher("/a5c4");
      assertTrue(matcher.find());
   }

   @Test
   public void testCustomTypeWithLabel() throws NoSuchMethodException, ParseException {
      options.addType("custom", CustomRouteType.class);
      RoutePattern pat = RoutePattern.build(null, TestResource.class.getMethod("customLabel", Custom.class), options);
      assertNotNull(pat);
      assertEquals("/{custom(4)}", pat.toString());
      Matcher matcher = pat.getPattern().matcher("/a5c4");
      assertTrue(matcher.find());
   }

   // URI Production ----------

   @Test
   public void testBuildParameterless() throws NoSuchMethodException, ParseException, NoSingleMethodException {
      RoutePattern parentPat = RoutePattern.build(null, TestResource.class, serviceMap, options);
      RoutePattern pat = RoutePattern.build(parentPat, TestResource.class.getMethod("test"), options);
      String url = pat.getPathWithArgs(new Object[0]);
      assertEquals("/test", url);
   }

   @Test
   public void testBuildParameterlessWithArgs() throws NoSuchMethodException, ParseException, NoSingleMethodException {
      RoutePattern parentPat = RoutePattern.build(null, TestResource.class, serviceMap, options);
      RoutePattern pat = RoutePattern.build(parentPat, TestResource.class.getMethod("test"), options);
      String url = pat.getPathWithArgs(new Object[] { 1 });
      assertEquals("/test", url);
   }

   @Test
   public void testBuildWithArgs() throws NoSuchMethodException, ParseException, NoSingleMethodException {
      RoutePattern parentPat = RoutePattern.build(null, TestResource.class, serviceMap, options);
      RoutePattern pat = RoutePattern.build(parentPat, TestResource.class.getMethod("test", int.class), options);
      String url = pat.getPathWithArgs(new Object[] { 1 });
      assertEquals("/test/1", url);
   }

   @Test
   public void testBuildWithFewerArgs() throws NoSuchMethodException, ParseException, NoSingleMethodException {
      thrown.expect(ArrayIndexOutOfBoundsException.class);
      RoutePattern parentPat = RoutePattern.build(null, TestResource.class, serviceMap, options);
      RoutePattern pat = RoutePattern.build(parentPat, TestResource.class.getMethod("test", int.class, String.class),
               options);
      pat.getPathWithArgs(new Object[] { 1 });
   }

   @Test
   public void testBuildWithWrongArgs() throws NoSuchMethodException, ParseException, NoSingleMethodException {
      RoutePattern parentPat = RoutePattern.build(null, TestResource.class, serviceMap, options);
      RoutePattern pat = RoutePattern.build(parentPat, TestResource.class.getMethod("test", int.class, String.class),
               options);
      String url = pat.getPathWithArgs(new Object[] { "test", 1 });
      assertEquals("/test/test/1", url);
   }

   @Test
   public void testBuildWithRouteArgs() throws NoSuchMethodException, ParseException, NoSingleMethodException {
      RoutePattern parentPat = RoutePattern.build(null, TestResource.class, serviceMap, options);
      RoutePattern pat = RoutePattern.build(parentPat, TestResource.class.getMethod("routed", int.class, String.class),
               options);
      String url = pat.getPathWithArgs(new Object[] { 1, "test" });
      assertEquals("/test/route1-test", url);
   }

   @Test
   public void testBuildWithWrongRouteArgs() throws NoSuchMethodException, ParseException, NoSingleMethodException {
      RoutePattern parentPat = RoutePattern.build(null, TestResource.class, serviceMap, options);
      RoutePattern pat = RoutePattern.build(parentPat,
               TestResource.class.getMethod("content", SimpleTestResource.class), options);
      String url = pat.getPathWithArgs(new Object[0]);
      assertEquals("/test", url);
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

      @Route("{int}/view")
      public void wrong() {
         // Test method, does nothing
      }

      @Route("{year}")
      public void wrong(int i) {
         // Test method, does nothing
      }

      @Route("{int}/view")
      public void wrong(int i, int j) {
         // Test method, does nothing
      }

      @Route("{alpha}{int}")
      public void wrong(String s) {
         // Test method, does nothing
      }

      @Route("{int}.{alpha}")
      public void wrong(String s, int i) {
         // Test method, does nothing
      }

      @Route("route")
      public void routed() {
         // Test method, does nothing
      }

      @Route("route{int}-{alpha}")
      public void routed(int i, String s) {
         // Test method, does nothing
      }

      @Route("{alpha(3..)}")
      public void routed(String s) {
         // Test method, does nothing
      }

      public void content(SimpleTestResource res) {
         // Test method, does nothing
      }

      public void content(String[] args) {
         // Test method, does nothing
      }

      public void custom(Custom value) {
         // Test method, does nothing
      }

      @Route("{custom}")
      public void customLabel(Custom value) {
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

   @Route("user/{int}/order")
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

      public ServicedTestResource(Observer observer) {
         // Test method, does nothing
      }
   }

   @Route("test/{int}")
   public static class MixedServiceTestResource {

      public MixedServiceTestResource(int i, Observer observer) {
         // Test method, does nothing
      }
   }

   public static class CustomRouteType extends AbstractRouteType {

      public CustomRouteType() {
         super(4, 4);
      }

      @Override
      protected String getRegex() {
         return "[a-z][0-9][a-z][0-9]";
      }

      @Override
      protected boolean isAssignableTo(Class<?> clazz) {
         return clazz == Custom.class;
      }
   }

   public static class Custom {

      private final String value;

      public Custom(String value) {
         this.value = value;
      }

      public static Custom valueOf(String string) {
         return new Custom(string);
      }

      @Override
      public String toString() {
         return value;
      }
   }
}