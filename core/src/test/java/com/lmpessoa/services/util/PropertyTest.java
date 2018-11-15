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
package com.lmpessoa.services.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.lmpessoa.services.util.Property;

public class PropertyTest {

   @Test
   public void testFromJsonString() {
      Property result = Property.fromString("\"test\"");
      assertNotNull(result);
      assertFalse(result.hasChildren());
      assertEquals("test", result.getValue());
   }

   @Test
   public void testFromJsonObject() {
      Property result = Property.fromString("{\"test\":\"value\"}");
      assertNotNull(result);
      assertTrue(result.hasChildren());
      Property r2 = result.get("test");
      assertNotNull(r2);
      assertFalse(r2.hasChildren());
      assertEquals("value", r2.getValue());
   }

   @Test
   public void testFromJsonArray() {
      Property result = Property
               .fromString("[{\"value\":42,\"key\":1},{\"value\":7,\"key\":2},{\"value\":12,\"key\":3}]");
      assertNotNull(result);
      assertTrue(result.hasChildren());
      String[] values = result.values()
               .stream()
               .map(p -> p.get("value"))
               .filter(p -> p != null)
               .map(p -> p.getValue())
               .toArray(String[]::new);
      assertArrayEquals(new String[] { "42", "7", "12" }, values);
      values = result.values().stream().map(p -> p.get("key")).filter(p -> p != null).map(p -> p.getValue()).toArray(
               String[]::new);
      assertArrayEquals(new String[] { "1", "2", "3" }, values);
   }

   @Test
   public void testNestedFromJson() {
      Property result = Property.fromString("{\"first\":{\"second\":{\"third\":\"test\"}}}");
      assertNotNull(result);
      assertTrue(result.hasChildren());
      Property r2 = result.get("first");
      assertNotNull(r2);
      assertTrue(r2.hasChildren());
      assertNotNull(r2.get("second"));
      assertSame(result.get("first.second"), r2.get("second"));
      r2 = r2.get("second");
      assertNotNull(r2);
      assertTrue(r2.hasChildren());
      assertNotNull(r2.get("third"));
      assertSame(result.get("first.second.third"), r2.get("third"));
      r2 = r2.get("third");
      assertNotNull(r2);
      assertFalse(r2.hasChildren());
      assertEquals("test", r2.getValue());
   }

   @Test
   public void testFromYamlObject() {
      Property result = Property.fromString("test: value");
      assertNotNull(result);
      assertTrue(result.hasChildren());
      Property r2 = result.get("test");
      assertNotNull(r2);
      assertFalse(r2.hasChildren());
      assertEquals("value", r2.getValue());
   }

   @Test
   public void testFromYamlArray() {
      Property result = Property.fromString("- value: 42\n  key: 1\n- value: 7\n  key: 2\n- value: 12\n  key: 3");
      assertNotNull(result);
      assertTrue(result.hasChildren());
      String[] values = result.values()
               .stream()
               .map(p -> p.get("value"))
               .filter(p -> p != null)
               .map(p -> p.getValue())
               .toArray(String[]::new);
      assertArrayEquals(new String[] { "42", "7", "12" }, values);
      values = result.values().stream().map(p -> p.get("key")).filter(p -> p != null).map(p -> p.getValue()).toArray(
               String[]::new);
      assertArrayEquals(new String[] { "1", "2", "3" }, values);
   }

   @Test
   public void testNestedFromYaml() {
      Property result = Property.fromString("first:\n  second: \n    third: test");
      assertNotNull(result);
      assertTrue(result.hasChildren());
      Property r2 = result.get("first");
      assertNotNull(r2);
      assertTrue(r2.hasChildren());
      assertNotNull(r2.get("second"));
      assertSame(result.get("first.second"), r2.get("second"));
      r2 = r2.get("second");
      assertNotNull(r2);
      assertTrue(r2.hasChildren());
      assertNotNull(r2.get("third"));
      assertSame(result.get("first.second.third"), r2.get("third"));
      r2 = r2.get("third");
      assertNotNull(r2);
      assertFalse(r2.hasChildren());
      assertEquals("test", r2.getValue());
   }
}
