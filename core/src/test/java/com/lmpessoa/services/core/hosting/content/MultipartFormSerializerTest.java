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
package com.lmpessoa.services.core.hosting.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.junit.Test;

import com.lmpessoa.services.core.ContentType;
import com.lmpessoa.services.core.HttpInputStream;
import com.lmpessoa.services.core.hosting.content.MultipartSerializer;

public final class MultipartFormSerializerTest {

   private static final String CONTENT_TYPE = "multipart/form-data; boundary=AaB03x";

   private MultipartSerializer serializer = new MultipartSerializer();

   @Test
   public void testSimpleFieldsOnly() throws IOException {
      byte[] content = getContent("no_streams.txt");
      Test1 result = serializer.read(content, CONTENT_TYPE, Test1.class);
      assertEquals(12, result.id);
      assertEquals("Test", result.name);
   }

   @Test
   public void testSingleVariableWithNoFile() throws IOException {
      byte[] content = getContent("no_streams.txt");
      Test2 result = serializer.read(content, CONTENT_TYPE, Test2.class);
      assertEquals(12, result.id);
      assertEquals("Test", result.name);
      assertNull(result.files);
   }

   @Test
   public void testSingleVariable() throws IOException {
      byte[] content = getContent("single_stream.txt");
      Test2 result = serializer.read(content, CONTENT_TYPE, Test2.class);
      assertEquals(12, result.id);
      assertEquals("Test", result.name);
      assertNotNull(result.files);
      assertFile1(result.files);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testMultipleFilesWithSingleVariable() throws IOException {
      byte[] content = getContent("multi_streams.txt");
      serializer.read(content, CONTENT_TYPE, Test2.class);
   }

   @Test
   public void testArrayWithOneFile() throws IOException {
      byte[] content = getContent("single_stream.txt");
      Test3 result = serializer.read(content, CONTENT_TYPE, Test3.class);
      assertEquals(12, result.id);
      assertEquals("Test", result.name);
      assertNotNull(result.files);
      assertEquals(1, result.files.length);
      assertFile1(result.files[0]);
   }

   @Test
   public void testArrayWithMultipleFiles() throws IOException {
      byte[] content = getContent("multi_streams.txt");
      Test3 result = serializer.read(content, CONTENT_TYPE, Test3.class);
      assertEquals(12, result.id);
      assertEquals("Test", result.name);
      assertNotNull(result.files);
      assertEquals(2, result.files.length);
      assertFile1(result.files[0]);
      assertFile2(result.files[1]);
   }

   private void assertFile1(InputStream file) throws IOException {
      assertTrue(file instanceof HttpInputStream);
      HttpInputStream file1 = (HttpInputStream) file;
      assertEquals(ContentType.TEXT, file1.getType());
      assertEquals("file1.txt", file1.getFilename());
      String resultContent = new String(getContent(file1));
      assertEquals("...contents of file1.txt...", resultContent);
   }

   private void assertFile2(InputStream file) throws IOException {
      assertTrue(file instanceof HttpInputStream);
      HttpInputStream file2 = (HttpInputStream) file;
      assertEquals(ContentType.GIF, file2.getType());
      assertEquals("file2.gif", file2.getFilename());
      String resultContent = new String(getContent(file2));
      assertEquals("...contents of file2.gif...", resultContent);
   }

   private byte[] getContent(InputStream in) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int len;
      while ((len = in.read(buffer)) != -1) {
         out.write(buffer, 0, len);
      }
      return out.toByteArray();
   }

   private byte[] getContent(String filename) throws IOException {
      try (InputStream res = this.getClass().getResourceAsStream("/serializer/" + filename)) {
         return getContent(res);
      }
   }

   public static class Test1 {

      public int id;
      public String name;
   }

   public static class Test2 extends Test1 {

      public InputStream files;
   }

   public static class Test3 extends Test1 {

      public InputStream[] files;
   }

   public static class Test4 extends Test1 {

      public Collection<InputStream> files;
   }
}
