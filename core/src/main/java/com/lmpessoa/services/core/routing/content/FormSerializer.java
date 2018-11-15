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
package com.lmpessoa.services.core.routing.content;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

final class FormSerializer implements IContentReader {

   private static final Map<Class<?>, Class<?>> primitives = new HashMap<>();

   static {
      primitives.put(char.class, Character.class);
      primitives.put(int.class, Integer.class);
   }

   @SuppressWarnings("unchecked")
   public static <T> T convert(String value, Class<T> type) {
      if (value == null) {
         return null;
      }
      Class<?> atype = type;
      if (primitives.containsKey(type)) {
         atype = primitives.get(type);
      } else if (type.isPrimitive()) {
         String name = type.getSimpleName();
         name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
         try {
            atype = Class.forName("java.lang." + name);
         } catch (ClassNotFoundException e) {
            throw new TypeConvertException(e);
         }
      }
      Method valueOf;
      try {
         valueOf = atype.getMethod("valueOf", String.class);
      } catch (NoSuchMethodException e) {
         throw new TypeConvertException(e);
      }
      if (!Modifier.isStatic(valueOf.getModifiers())) {
         throw new TypeConvertException(new NoSuchMethodException("Method 'valueOf' is not static"));
      }
      try {
         return (T) valueOf.invoke(null, value);
      } catch (IllegalAccessException | InvocationTargetException e) {
         throw new TypeConvertException(e);
      }
   }

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

   Object convertToValue(Object value, Class<?> clazz) {
      if (value.getClass().isArray() != clazz.isArray()) {
         throw new IllegalArgumentException("Could not convert value to type");
      }
      if (value instanceof String[]) {
         Class<?> type = clazz.getComponentType();
         String[] svalue = (String[]) value;
         Object result = Array.newInstance(type, svalue.length);
         for (int i = 0; i < svalue.length; ++i) {
            Object cvalue = convert(svalue[i], type);
            Array.set(result, i, cvalue);
            if (cvalue != null) {
               try {
                  Method primValue = cvalue.getClass().getMethod(type.getName() + "Value");
                  Array.set(result, i, primValue.invoke(cvalue));
               } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                  // Ignore
               }
            }
         }
         return result;
      } else {
         return convert((String) value, clazz);
      }
   }

   private <T> T read(String content, Class<T> resultClass) {
      Map<String, Object> values = Serializer.parseQueryString(content);
      try {
         T result = resultClass.newInstance();
         for (Entry<String, Object> value : values.entrySet()) {
            Field field = findField(value.getKey(), resultClass);
            if (field == null) {
               return null;
            }
            Class<?> fieldType = field.getType();
            Object fieldValue = fieldType == String.class || fieldType == String[].class ? value.getValue()
                     : convertToValue(value.getValue(), field.getType());
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
