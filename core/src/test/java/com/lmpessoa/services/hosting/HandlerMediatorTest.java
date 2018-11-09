/*
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
package com.lmpessoa.services.hosting;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.hosting.HandlerMediator;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.hosting.NextHandler;
import com.lmpessoa.services.hosting.ResultHandler;
import com.lmpessoa.services.routing.IRouteTable;
import com.lmpessoa.services.routing.MatchedRoute;
import com.lmpessoa.services.routing.RouteTableBridge;
import com.lmpessoa.services.services.IServiceMap;

public final class HandlerMediatorTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private HandlerMediator mediator;
   private IServiceMap services;
   private Request request;
   private Result result;

   @Before
   public void setup() {
      services = IServiceMap.newInstance();

      mediator = new HandlerMediator(services);
      mediator.addHandler(TransformingHandler.class);
      mediator.addHandler(RejectingHandler.class);
      mediator.addHandler(RespondingHandler.class);

      request = new Request();
      result = new Result();

      services.putSingleton(Request.class, request);
      services.putSingleton(Result.class, result);
   }

   @Test
   public void testRespondingChain() {
      request.code = 0;
      mediator.invoke();
      assertEquals("OK", result.message);
      assertEquals(2, result.code);
   }

   @Test
   public void testRejectingChain() {
      request.code = 2;
      mediator.invoke();
      assertEquals("Error", result.message);
      assertEquals(4, result.code);
   }

   @Test
   public void testTransformingChain() {
      request.code = 1;
      mediator.invoke();
      assertEquals("OK Computer", result.message);
      assertEquals(2, result.code);
   }

   @Test
   public void testRejectAbstractClass() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Handler must be a concrete class");
      mediator.addHandler(AbstractTestHandler.class);
   }

   @Test
   public void testRejectWrongConstructor() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Handler must implement a required constructor");
      mediator.addHandler(WrongConstructorHandler.class);
   }

   @Test
   public void testRejectMultipleMethods() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Handler must have exaclty one method named 'invoke'");
      mediator.addHandler(MultipleInvokeHandler.class);
   }

   @Test
   public void testMediatorInvocation() throws IOException {
      mediator = new HandlerMediator(services);
      mediator.addHandler(ResultHandler.class);

      IRouteTable routes = RouteTableBridge.get(services);
      routes.put(TestResource.class);

      HttpRequest request = new HttpRequestBuilder().setMethod("GET").setPath("/test").build();
      MatchedRoute result = RouteTableBridge.match(routes, request);
      services.putTransient(MatchedRoute.class, () -> result);

      Object invokeResult = mediator.invoke();
      assertEquals("Test", invokeResult);
   }

   public static class Request {

      public int code;
   }

   public static class Result {

      public String message;
      public int code;
   }

   public static class RespondingHandler {

      public RespondingHandler(NextHandler next) {
         // Ignore, will never forward
      }

      public void invoke(Result result) {
         result.message = "OK";
         result.code = 2;
      }
   }

   public static class RejectingHandler {

      private final NextHandler next;

      public RejectingHandler(NextHandler next) {
         this.next = next;
      }

      public void invoke(Request request, Result result) {
         if (request.code == 2) {
            result.message = "Error";
            result.code = 4;
         } else {
            next.invoke();
         }
      }
   }

   public static class TransformingHandler {

      private final NextHandler next;

      public TransformingHandler(NextHandler next) {
         this.next = next;
      }

      public void invoke(Request request, Result result) {
         next.invoke();
         if (request.code == 1) {
            result.message += " Computer";
         }
      }
   }

   public static abstract class AbstractTestHandler {

      public void invoke(Request request) {
         // Test method, does nothing
      }
   }

   public static class WrongConstructorHandler extends AbstractTestHandler {

      public WrongConstructorHandler(int i, NextHandler next) {
         // Test method, does nothing
      }
   }

   public static class MultipleInvokeHandler extends AbstractTestHandler {

      public MultipleInvokeHandler(NextHandler next) {
         // Test method, does nothing
      }

      public void invoke(int i, Request request) {
         // Test method, does nothing
      }
   }

   public static class TestResource {

      public String get() {
         return "Test";
      }
   }
}
