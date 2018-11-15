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
package com.lmpessoa.services.core.hosting.content;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.core.ContentType;
import com.lmpessoa.services.core.hosting.content.FormSerializer;
import com.lmpessoa.services.core.hosting.content.TypeConvertException;

public final class FormSerializerTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private FormSerializer serializer = new FormSerializer();

   @Test
   public void testParseWithSingleValue() {
      Test1 test = serializer.read("value=Test".getBytes(), ContentType.FORM, Test1.class);
      assertNotNull(test);
      assertEquals("Test", test.value);
   }

   @Test
   public void testParseWithMultipleValues() {
      Test2 test = serializer.read("value=Test&id=12&checked=true".getBytes(), ContentType.FORM, Test2.class);
      assertNotNull(test);
      assertEquals("Test", test.value);
      assertEquals(12, test.id);
      assertTrue(test.checked);
   }

   @Test
   public void testParseWithPartialValues() {
      Test2 test = serializer.read("value=Test&id=12".getBytes(), ContentType.FORM, Test2.class);
      assertNotNull(test);
      assertEquals("Test", test.value);
      assertEquals(12, test.id);
      assertFalse(test.checked);
   }

   @Test
   public void testParseNotWithMissingValues() {
      Test2 test = serializer.read("value=Test&id=12&flag=true".getBytes(), ContentType.FORM, Test2.class);
      assertNull(test);
   }

   @Test
   public void testProduceSubclass() {
      Test3 test = serializer.read("value=Test&id=12&checked=true".getBytes(), ContentType.FORM, Test3.class);
      assertNotNull(test);
      assertEquals("Test", test.value);
      assertEquals(12, test.id);
      assertTrue(test.checked);
   }

   @Test
   public void testParseWithArray1() {
      Test4 test = serializer.read("values=1&values=2&values=3".getBytes(), ContentType.FORM, Test4.class);
      assertNotNull(test);
      assertArrayEquals(new int[] { 1, 2, 3 }, test.values);
   }

   @Test
   public void testParseWithArray2() {
      Test4 test = serializer.read("values[]=1&values[]=2&values[]=3".getBytes(), ContentType.FORM, Test4.class);
      assertNotNull(test);
      assertArrayEquals(new int[] { 1, 2, 3 }, test.values);
   }

   @Test
   public void testParseTypeMismatch() {
      thrown.expect(TypeConvertException.class);
      serializer.read("value=1&value=2&id=1".getBytes(), ContentType.FORM, Test2.class);
   }

   @Test
   public void testParseNonNumber() {
      thrown.expect(TypeConvertException.class);
      serializer.read("id=a".getBytes(), ContentType.FORM, Test2.class);
   }

   public static class Test1 {

      public String value;
   }

   public static class Test2 {

      public String value;
      public int id;
      public transient boolean checked = false;
   }

   public static class Test3 extends Test2 {

   }

   public static class Test4 {

      public int[] values;
   }
}
