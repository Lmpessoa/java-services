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

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

import com.lmpessoa.services.util.ClassUtils;

final class FormSerializer implements IContentReader {

   @Override
   public <T> T read(byte[] content, String contentType, Class<T> resultClass) {
      String charset = Serializer.getContentTypeVariable(contentType, "charset");
      Charset encoding = Charset.forName(charset == null ? "utf-8" : charset);
      String contentStr = new String(content, encoding);
      return read(contentStr, resultClass);
   }

   static Field findField(String fieldName, Class<?> type) {
      Class<?> superType = type;
      while (superType != null) {
         try {
            return superType.getDeclaredField(fieldName);
         } catch (NoSuchFieldException e) {
            superType = superType.getSuperclass() == Object.class ? null : superType.getSuperclass();
         } catch (NullPointerException e) {
            break;
         }
      }
      return null;
   }

   private <T> T read(String content, Class<T> resultClass) {
      Map<String, Object> values = Serializer.parseQueryString(content);
      try {
         T result = resultClass.newInstance();
         for (Entry<String, Object> entry : values.entrySet()) {
            Field field = findField(entry.getKey(), resultClass);
            if (field == null) {
               return null;
            }
            Class<?> fieldType = field.getType();
            Object value = entry.getValue();
            if (value instanceof String[]) {
               value = String.join(",", (String[]) value);
            }
            Object fieldValue = fieldType == String.class || fieldType == String[].class ? entry.getValue()
                     : ClassUtils.cast(value.toString(), field.getType());
            field.setAccessible(true);
            field.set(result, fieldValue);
         }
         return result;
      } catch (IllegalArgumentException e) {
         throw new TypeConvertException(e);
      } catch (InstantiationException | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }
}
