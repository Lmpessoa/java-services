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
package com.lmpessoa.services.internal.hosting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.lmpessoa.services.hosting.Headers;

public final class HeadersTest {

   @Test
   public void testSplitSimpleValue() {
      Map<String, String> result = Headers.split("text/plain");
      assertEquals(1, result.size());
      assertTrue(result.containsKey(""));
      assertEquals("text/plain", result.get(""));
   }

   @Test
   public void testSplitMultipleValues() {
      Map<String, String> result = Headers.split("text/plain; charset=\"utf-8\"");
      assertEquals(2, result.size());
      assertTrue(result.containsKey(""));
      assertTrue(result.containsKey("charset"));
      assertEquals("text/plain", result.get(""));
      assertEquals("utf-8", result.get("charset"));
   }

   @Test
   public void testSplitWithSemicolon() {
      Map<String, String> result = Headers.split("text/plain; filename=\"text;1.txt\"");
      assertEquals(2, result.size());
      assertTrue(result.containsKey(""));
      assertTrue(result.containsKey("filename"));
      assertEquals("text/plain", result.get(""));
      assertEquals("text;1.txt", result.get("filename"));
   }
}
