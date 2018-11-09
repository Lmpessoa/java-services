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
package com.lmpessoa.services.routing.content;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

import com.lmpessoa.services.core.MediaType;
import com.lmpessoa.services.routing.content.Serializer;

public final class SerializerTest {

   @Test
   public void testParseJson() {
      String content = "{\"id\": 12, \"name\": \"Test\", \"email\": "
               + "[ \"test@test.com\", \"test@test.org\" ], \"checked\": true}";
      TestObject result = Serializer.parse(MediaType.JSON, content, TestObject.class);
      assertNotNull(result);
      assertEquals(12, result.id);
      assertEquals("Test", result.name);
      assertArrayEquals(new String[] { "test@test.com", "test@test.org" }, result.email);
      assertTrue(result.checked);
   }

   @Test
   public void testParseXml() {
      String content = "<?xml version=\"1.0\"?><object><id>12</id><name>Test</name>"
               + "<email>test@test.com</email><email>test@test.org</email><checked>true</checked></object>";
      TestObject result = Serializer.parse(MediaType.XML, content, TestObject.class);
      assertNotNull(result);
      assertEquals(12, result.id);
      assertEquals("Test", result.name);
      assertArrayEquals(new String[] { "test@test.com", "test@test.org" }, result.email);
      assertTrue(result.checked);
   }

   @Test
   public void testParseForm() {
      String content = "id=12&name=Test&email=test%40test.com&email=test%40test.org&checked=true";
      TestObject result = Serializer.parse(MediaType.FORM, content, TestObject.class);
      assertNotNull(result);
      assertEquals(12, result.id);
      assertEquals("Test", result.name);
      assertArrayEquals(new String[] { "test@test.com", "test@test.org" }, result.email);
      assertTrue(result.checked);
   }

   @XmlRootElement(name = "object")
   public static class TestObject {

      public int id;
      public String name;
      public String[] email;
      public boolean checked;
   }
}