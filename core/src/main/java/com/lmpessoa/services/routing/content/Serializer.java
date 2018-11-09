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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.lmpessoa.services.core.MediaType;
import com.lmpessoa.services.hosting.HttpResultInputStream;

/**
 * The <code>Serializer</code> class provides an abstraction to transform HTTP content between plain
 * text and objects.
 */
public final class Serializer {

   private static final Map<String, Object> handlers = new HashMap<>();

   static {
      handlers.put(MediaType.JSON, new JsonSerializer());
      handlers.put(MediaType.XML, new XmlSerializer());
      handlers.put(MediaType.FORM, new FormSerializer());
   }

   /**
    * Parses the given content into an object of the given class.
    *
    * @param contentType the type of the content.
    * @param content the content to be parsed.
    * @param resultClass the class of the object to be returned.
    * @return the content parsed into an object of the given class.
    */
   public static <T> T parse(String contentType, String content, Class<T> resultClass) {
      if (handlers.containsKey(contentType)) {
         Object handler = handlers.get(contentType);
         if (handler instanceof IContentParser) {
            try {
               return ((IContentParser) handler).parse(content, resultClass);
            } catch (Exception e) {
               e.printStackTrace(); // or ignore
            }
         }
      }
      return null;
   }

   public static HttpResultInputStream produce(String[] accepts, Object obj) {
      for (String contentType : accepts) {
         if (handlers.containsKey(contentType)) {
            Object handler = handlers.get(contentType);
            if (handler instanceof IContentProducer) {
               try {
                  InputStream is = ((IContentProducer) handler).produce(obj);
                  return new HttpResultInputStream(contentType, is);
               } catch (Exception e) {
                  e.printStackTrace(); // or ignore
               }
            }
         }
      }
      return null;
   }

   private Serializer() {
      // Does nothing
   }
}
