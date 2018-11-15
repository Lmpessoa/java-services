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
package com.lmpessoa.services.core.hosting;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.hosting.HeaderMap;

public final class HeaderMapTest {

   private HeaderMap subject;

   @Before
   public void setup() {
      subject = new HeaderMap();
   }

   @Test
   public void testSplitSimpleValue() {
      Map<String, String> result = HeaderMap.split("text/plain");
      assertEquals(1, result.size());
      assertTrue(result.containsKey(""));
      assertEquals("text/plain", result.get(""));
   }

   @Test
   public void testSplitMultipleValues() {
      Map<String, String> result = HeaderMap.split("text/plain; charset=\"utf-8\"");
      assertEquals(2, result.size());
      assertTrue(result.containsKey(""));
      assertTrue(result.containsKey("charset"));
      assertEquals("text/plain", result.get(""));
      assertEquals("utf-8", result.get("charset"));
   }

   @Test
   public void testSplitWithSemicolon() {
      Map<String, String> result = HeaderMap.split("text/plain; filename=\"text;1.txt\"");
      assertEquals(2, result.size());
      assertTrue(result.containsKey(""));
      assertTrue(result.containsKey("filename"));
      assertEquals("text/plain", result.get(""));
      assertEquals("text;1.txt", result.get("filename"));
   }

   @Test
   public void testAddValue1() {
      subject.add("X", "1", "2");
      assertEquals("1", subject.get("X"));
      assertArrayEquals(new String[] { "1", "2" }, subject.getAll("X").toArray(new String[0]));
   }

   @Test
   public void testAddValue2() {
      subject.add("X", "1");
      subject.add("X", "2");
      assertEquals("1", subject.get("X"));
      assertArrayEquals(new String[] { "1", "2" }, subject.getAll("X").toArray(new String[0]));
   }

   @Test
   public void testSetReplacesAdd() {
      subject.add("X", "1");
      subject.set("X", "2");
      assertEquals("2", subject.get("X"));
      assertArrayEquals(new String[] { "2" }, subject.getAll("X").toArray(new String[0]));
   }

   @Test
   public void testAddAfterSet() {
      subject.add("X", "1");
      subject.set("X", "2");
      subject.add("X", "3");
      assertEquals("2", subject.get("X"));
      assertArrayEquals(new String[] { "2", "3" }, subject.getAll("X").toArray(new String[0]));
   }

   @Test(expected = UnsupportedOperationException.class)
   public void testFrozenMap() {
      subject.add("X", "1");
      subject.freeze();
      subject.set("X", "2");
   }
}
