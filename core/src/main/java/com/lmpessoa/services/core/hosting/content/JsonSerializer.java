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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

final class JsonSerializer implements IContentReader, IContentProducer {

   @Override
   public <T> T read(byte[] content, String contentType, Class<T> resultClass) {
      String charset = Serializer.getContentTypeVariable(contentType, "charset");
      Charset encoding = Charset.forName(charset == null ? Serializer.UTF_8 : charset);
      String contentStr = new String(content, encoding);
      return read(contentStr, resultClass);
   }

   @Override
   public InputStream produce(Object obj) {
      try {
         Gson gson = new GsonBuilder().create();
         byte[] data = gson.toJson(obj).getBytes(Charset.forName(Serializer.UTF_8));
         return new ByteArrayInputStream(data);
      } catch (RuntimeException e) {
         throw e;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private <T> T read(String content, Class<T> clazz) {
      try {
         Gson gson = new GsonBuilder().setPrettyPrinting().create();
         return gson.fromJson(content, clazz);
      } catch (RuntimeException e) {
         throw e;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
}
