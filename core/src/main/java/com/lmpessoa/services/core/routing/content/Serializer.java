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

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.lmpessoa.services.Internal;
import com.lmpessoa.services.core.ContentType;
import com.lmpessoa.services.core.HttpInputStream;
import com.lmpessoa.services.util.ClassUtils;

/**
 * The <code>Serializer</code> class provides an abstraction to transform HTTP content between plain
 * text and objects.
 */
public final class Serializer {

   private static final Map<String, Class<?>> handlers = new HashMap<>();

   static final String UTF_8 = "utf-8";

   static {
      handlers.put(ContentType.JSON, JsonSerializer.class);
      handlers.put(ContentType.FORM, FormSerializer.class);
      handlers.put(ContentType.MULTIPART_FORM, MultipartSerializer.class);
      handlers.put("*/*", JsonSerializer.class);
   }

   /**
    * Parses the given content into an object of the given class.
    *
    * @param contentType the type of the content.
    * @param content the content to be parsed.
    * @param resultClass the class of the object to be returned.
    * @return the content parsed into an object of the given class.
    */
   @Internal
   public static <T> T read(String contentType, byte[] content, Class<T> resultClass) {
      ClassUtils.checkInternalAccess();
      String realType = contentType.split(";")[0];
      if (handlers.containsKey(realType)) {
         Class<?> handlerClass = handlers.get(realType);
         if (IContentReader.class.isAssignableFrom(handlerClass)) {
            try {
               Object handler = handlerClass.newInstance();
               return ((IContentReader) handler).read(content, contentType, resultClass);
            } catch (Exception e) {
               throw new SerializationException("Error deserialising content", e);
            }
         }
      }
      throw new SerializationException("Cannot deserialise from " + contentType);
   }

   @Internal
   public static HttpInputStream produce(String[] accepts, Object obj)
      throws InstantiationException, IllegalAccessException {
      ClassUtils.checkInternalAccess();
      for (String contentType : accepts) {
         if (handlers.containsKey(contentType)) {
            Class<?> handlerClass = handlers.get(contentType);
            if (IContentProducer.class.isAssignableFrom(handlerClass)) {
               Object handler = handlerClass.newInstance();
               InputStream is = ((IContentProducer) handler).produce(obj);
               return new HttpInputStream(contentType, is);
            }
         }
      }
      return null;
   }

   @Internal
   public static void enableXml(boolean enable) {
      ClassUtils.checkInternalAccess();
      if (enable && !handlers.containsKey(ContentType.XML)) {
         handlers.put(ContentType.XML, XmlSerializer.class);
      } else if (!enable && handlers.containsKey(ContentType.XML)) {
         handlers.remove(ContentType.XML);
      }
   }

   static Map<String, Object> parseQueryString(String body) {
      String[][] values = Arrays.stream(body.split("&")).map(s -> s.split("=", 2)).toArray(String[][]::new);
      Map<String, List<String>> groups = new HashMap<>();
      for (String[] value : values) {
         try {
            String key = URLDecoder.decode(value[0], UTF_8);
            if (key.endsWith("[]")) {
               key = key.substring(0, key.length() - 2);
            }
            if (!groups.containsKey(key)) {
               groups.put(key, new ArrayList<>());
            }
            groups.get(key).add(URLDecoder.decode(value[1], UTF_8));
         } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
         }
      }
      Map<String, Object> result = new HashMap<>();
      for (Entry<String, List<String>> entry : groups.entrySet()) {
         String key = entry.getKey();
         List<String> value = entry.getValue();
         if (value.size() > 1) {
            result.put(key, value.toArray(new String[0]));
         } else if (value.size() == 1) {
            result.put(key, value.get(0));
         }
      }
      return result;
   }

   static String getContentTypeVariable(String contentType, String variable) {
      String[] parts = contentType.split(";");
      for (int i = 1; i < parts.length; ++i) {
         if (parts[i].trim().startsWith(variable + '=')) {
            return parts[i].split("=", 2)[1];
         }
      }
      return null;
   }

   private Serializer() {
      // Does nothing
   }
}
