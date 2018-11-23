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
package com.lmpessoa.services.core.internal.hosting;

import static com.lmpessoa.services.core.services.Reuse.ALWAYS;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.hosting.NextResponder;
import com.lmpessoa.services.core.internal.services.ServiceMap;
import com.lmpessoa.services.core.services.Service;

public final class NextHandlerTest {

   private ApplicationOptions app;
   private List<Class<?>> handlers;
   private ServiceMap services;
   private Request request;

   @Before
   public void setup() {
      app = new ApplicationOptions(null);

      handlers = new ArrayList<>();
      handlers.add(TransformingHandler.class);
      handlers.add(RejectingHandler.class);
      handlers.add(RespondingHandler.class);

      request = new Request();
      services = app.getServices();
      services.put(Request.class, request);
   }

   @Test
   public void testRespondingChain() {
      request.code = 0;
      NextResponder handler = new NextResponderImpl(services, handlers, app);
      Result result = (Result) handler.invoke();
      assertEquals("OK", result.message);
      assertEquals(2, result.code);
   }

   @Test
   public void testRejectingChain() {
      request.code = 2;
      NextResponder handler = new NextResponderImpl(services, handlers, app);
      Result result = (Result) handler.invoke();
      assertEquals("Error", result.message);
      assertEquals(4, result.code);
   }

   @Test
   public void testTransformingChain() {
      request.code = 1;
      NextResponder handler = new NextResponderImpl(services, handlers, app);
      Result result = (Result) handler.invoke();
      assertEquals("OK Computer", result.message);
      assertEquals(2, result.code);
   }

   @Service(reuse = ALWAYS)
   public static class Request {

      public int code;
   }

   public static class Result {

      public final String message;
      public final int code;

      public Result(int code, String message) {
         this.message = message;
         this.code = code;
      }
   }

   public static class RespondingHandler {

      public RespondingHandler(NextResponder next) {
         // Ignore, will never forward
      }

      public Result invoke() {
         return new Result(2, "OK");
      }
   }

   public static class RejectingHandler {

      private final NextResponder next;

      public RejectingHandler(NextResponder next) {
         this.next = next;
      }

      public Result invoke(Request request) {
         if (request.code == 2) {
            return new Result(4, "Error");
         } else {
            return (Result) next.invoke();
         }
      }
   }

   public static class TransformingHandler {

      private final NextResponder next;

      public TransformingHandler(NextResponder next) {
         this.next = next;
      }

      public Result invoke(Request request) {
         Result result = (Result) next.invoke();
         if (request.code == 1) {
            result = new Result(result.code, result.message + " Computer");
         }
         return result;
      }
   }

   public static class TestResource {

      public String get() {
         return "Test";
      }
   }
}
