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
package com.lmpessoa.services.core.serializing;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.lmpessoa.services.core.hosting.ContentType;
import com.lmpessoa.services.core.hosting.HttpInputStream;
import com.lmpessoa.services.core.hosting.InternalServerError;
import com.lmpessoa.services.core.hosting.NotAcceptableException;
import com.lmpessoa.services.core.hosting.UnsupportedMediaTypeException;

/**
 * Represents a serialisation format.
 * <p>
 * Subclasses of {@code Serializer} provide means to convert object to and from a string
 * representation suitable for transmission over the HTTP protocol.
 * </p>
 * <p>
 * This class is not intended to be used directly by applications. Instead, choose to return the
 * object which should be serialised from the application code and the engine will handle all
 * serialisation automatically.
 * </p>
 */
public abstract class Serializer {

   public static final Charset UTF_8 = Charset.forName("UTF-8");

   private static final Map<String, Class<? extends Serializer>> handlers = new HashMap<>();

   static {
      handlers.put(ContentType.JSON, JsonSerializer.class);
      handlers.put(ContentType.FORM, SimpleFormSerializer.class);
      handlers.put(ContentType.MULTIPART_FORM, MultipartFormSerializer.class);
      handlers.put("*/*", JsonSerializer.class);
   }

   /**
    * Converts the given content into an object of the given type.
    *
    * @param content the content to be converted into an object representation.
    * @param contentType the content type of the content.
    * @param type the type of the object to be returned.
    * @return an object representation of given content.
    * @throws UnsupportedMediaTypeException if the content type or encoding are not supported.
    */
   public static <T> T toObject(byte[] content, String contentType, Class<T> type) {
      Map<String, String> contentTypeMap = getHeaderValues(contentType);
      String realContentType = contentTypeMap.get("");
      if (!handlers.containsKey(realContentType)) {
         throw new UnsupportedMediaTypeException("Cannot read from type: " + realContentType);
      }
      Serializer ser = instanceOf(realContentType);
      try {
         return ser.read(content, type, contentTypeMap);
      } catch (Exception e) {
         throw new InternalServerError(e);
      }
   }

   /**
    * Converts the given object into a representation in an acceptable format.
    *
    * @param object the object to be converted.
    * @param accepts a list of content types into which the object is accepted to be converted.
    * @return a representation of the given object with the used format.
    */
   public static HttpInputStream fromObject(Object object, String[] accepts) {
      for (String contentType : accepts) {
         Serializer ser = instanceOf(contentType);
         if (ser != null) {
            String result = ser.write(object);
            if (result != null) {
               return new HttpInputStream(contentType, result.getBytes(UTF_8), UTF_8);
            }
         }
      }
      throw new NotAcceptableException();
   }

   /**
    * Enables or disables support for processing XML requests.
    *
    * @param enable {@code true} to enable support for XML requests, or {@code false} to disable it.
    */
   public static void enableXml(boolean enable) {
      if (enable && !handlers.containsKey(ContentType.XML)) {
         handlers.put(ContentType.XML, XmlSerializer.class);
      } else if (!enable && handlers.containsKey(ContentType.XML)) {
         handlers.remove(ContentType.XML);
      }
   }

   protected <T> T read(byte[] content, Class<T> type, Map<String, String> contentType) throws Exception {
      Charset charset = Charset.forName("UTF-8");
      if (contentType.containsKey("charset")) {
         String charsetName = contentType.get("charset");
         try {
            charset = Charset.forName(charsetName);
         } catch (Exception e) {
            throw new UnsupportedMediaTypeException("Unrecognized encoding: " + charsetName);
         }
      }
      String contentStr = new String(content, charset);
      return read(contentStr, type);
   }

   protected <T> T read(String content, Class<T> type) throws Exception {
      throw new UnsupportedOperationException();
   }

   protected abstract String write(Object object);

   protected static final Map<String, String> getHeaderValues(String header) {
      String[] parts = header.split(";");
      Map<String, String> result = new HashMap<>();
      result.put("", parts[0].trim());
      for (int i = 1; i < parts.length; ++i) {
         String[] subparts = parts[i].split("=", 2);
         subparts[1] = subparts[1].trim();
         if (subparts[1].startsWith("\"")) {
            subparts[1] = subparts[1].substring(1, subparts[1].length() - 1);
         }
         result.put(subparts[0].trim(), subparts[1].trim());
      }
      return result;
   }

   protected static final Field findField(String fieldName, Class<?> type) {
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

   protected static final boolean isStaticOrTransientOrVolatile(Field field) {
      int modifiers = field.getModifiers();
      return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || Modifier.isVolatile(modifiers);
   }

   private static Serializer instanceOf(String contentType) {
      Class<? extends Serializer> clazz = handlers.get(contentType);
      try {
         return clazz.newInstance();
      } catch (Exception e) {
         return null;
      }
   }
}
