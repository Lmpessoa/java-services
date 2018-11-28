/*
 * Copyright (c) 2018 Leonardo Pessoa
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
package com.lmpessoa.services.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.lmpessoa.services.internal.ClassUtils;
import com.lmpessoa.services.logging.Severity;

public class ClassUtilsTest {

   @Test
   public void testCastTrueToBoolean() {
      boolean b = ClassUtils.cast("true", boolean.class);
      assertTrue(b);
   }

   @Test
   public void testCastYesToBoolean() {
      boolean b = ClassUtils.cast("yes", boolean.class);
      assertTrue(b);
   }

   @Test
   public void testCastToInteger() {
      Integer i = ClassUtils.cast("7", Integer.class);
      assertEquals(7, i.intValue());
   }

   @Test
   public void testCastToInt() {
      int i = ClassUtils.cast("7", int.class);
      assertEquals(7, i);
   }

   @Test
   public void testCastToIntArray() {
      int[] i = ClassUtils.cast("7, 42", int[].class);
      assertArrayEquals(new int[] { 7, 42 }, i);
   }

   @Test
   public void testCastToEnum() {
      Severity s = ClassUtils.cast("ERROR", Severity.class);
      assertEquals(Severity.ERROR, s);
   }

   @Test
   public void testCastToEnumLowercase() {
      Severity s = ClassUtils.cast("error", Severity.class);
      assertEquals(Severity.ERROR, s);
   }

   @Test
   public void testCastToEnumArray() {
      Severity[] s = ClassUtils.cast("ERROR, INFO", Severity[].class);
      assertArrayEquals(new Severity[] { Severity.ERROR, Severity.INFO }, s);
   }

   @Test
   public void testCastToString() {
      String s = ClassUtils.cast("ERROR", String.class);
      assertEquals("ERROR", s);
   }

   @Test
   public void testCastToStringArray() {
      String[] s = ClassUtils.cast("ERROR, INFO", String[].class);
      assertArrayEquals(new String[] { "ERROR", "INFO" }, s);
   }
}
