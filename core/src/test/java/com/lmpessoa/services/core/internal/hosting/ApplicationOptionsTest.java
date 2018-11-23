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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.core.hosting.NextResponder;

public final class ApplicationOptionsTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private ApplicationOptions app;

   @Before
   public void setup() {
      app = new ApplicationOptions(null);
   }

   @Test
   public void testRejectAbstractClass() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Responder must be a concrete class");
      app.useResponder(AbstractTestResponder.class);
   }

   @Test
   public void testRejectWrongConstructor() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Responder must implement a required constructor");
      app.useResponder(WrongConstructorResponder.class);
   }

   @Test
   public void testRejectMultipleMethods() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Responder must have exaclty one method named 'invoke'");
      app.useResponder(MultipleInvokeResponder.class);
   }

   public abstract static class AbstractTestResponder {

      public void invoke() {
         // Test method, does nothing
      }
   }

   public static class WrongConstructorResponder extends AbstractTestResponder {

      public WrongConstructorResponder(int i, NextResponder next) {
         // Test method, does nothing
      }
   }

   public static class MultipleInvokeResponder extends AbstractTestResponder {

      public MultipleInvokeResponder(NextResponder next) {
         // Test method, does nothing
      }

      public void invoke(int i) {
         // Test method, does nothing
      }
   }
}
