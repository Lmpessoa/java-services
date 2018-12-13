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
package com.lmpessoa.services.internal.serializing;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.lmpessoa.services.hosting.ValuesMap;
import com.lmpessoa.services.internal.ClassUtils;
import com.lmpessoa.services.internal.ValuesMapBuilder;
import com.lmpessoa.services.internal.hosting.InternalServerError;

final class SimpleFormSerializer extends Serializer {

   @Override
   protected <T> T read(String content, Class<T> type) {
      ValuesMap values = parse(content);
      try {
         T result = type.newInstance();
         for (String key : values.keySet()) {
            Field field = findField(key, type);
            setValueToField(values.getAll(key), field, result);
         }
         return result;
      } catch (Exception e) {
         throw new InternalServerError(e);
      }
   }

   @Override
   protected String write(Object content) {
      return null;
   }

   protected static ValuesMap parse(String valueSet) {
      ValuesMapBuilder result = new ValuesMapBuilder();
      if (valueSet != null && !valueSet.isEmpty()) {
         String[][] values = Arrays.stream(valueSet.split("&")).map(s -> s.split("=", 2)).toArray(
                  String[][]::new);
         for (String[] value : values) {
            try {
               String key = URLDecoder.decode(value[0], StandardCharsets.UTF_8.name());
               if (key.endsWith("[]")) {
                  key = key.substring(0, key.length() - 2);
               }
               result.add(key, URLDecoder.decode(value[1], StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
               // Might never reach here but be safe
            }
         }
      }
      return result.build();
   }

   private void setValueToField(String[] value, Field field, Object result)
      throws IllegalAccessException {
      if (field != null && !isStaticOrTransientOrVolatile(field)) {
         String svalue = String.join(",", value);
         Object fieldValue = ClassUtils.cast(svalue, field.getType());
         field.setAccessible(true);
         field.set(result, fieldValue);
      }
   }
}
