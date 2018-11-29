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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.lmpessoa.services.ContentType;
import com.lmpessoa.services.HttpInputStream;
import com.lmpessoa.services.NotAcceptableException;
import com.lmpessoa.services.UnsupportedMediaTypeException;
import com.lmpessoa.services.hosting.Headers;
import com.lmpessoa.services.internal.ClassUtils;
import com.lmpessoa.services.internal.CoreMessage;
import com.lmpessoa.services.internal.hosting.InternalServerError;

public abstract class Serializer {

   private static final Map<String, Class<? extends Serializer>> handlers = new HashMap<>();

   static {
      handlers.put(ContentType.JSON, JsonSerializer.class);
      handlers.put(ContentType.FORM, SimpleFormSerializer.class);
      handlers.put(ContentType.MULTIPART_FORM, MultipartFormSerializer.class);
      handlers.put("*/*", JsonSerializer.class);
   }

   public static <T> T toObject(byte[] content, String contentType, Class<T> type) {
      Map<String, String> contentTypeMap = Headers.split(contentType);
      String realContentType = contentTypeMap.get("");
      if (!handlers.containsKey(realContentType)) {
         throw new UnsupportedMediaTypeException(
                  CoreMessage.UNEXPECTED_CONTENT_TYPE.with(realContentType));
      }
      Serializer ser = instanceOf(realContentType);
      if (ser == null) {
         throw new UnsupportedMediaTypeException(
                  CoreMessage.UNEXPECTED_CONTENT_TYPE.with(realContentType));
      }
      try {
         return ser.read(content, type, contentTypeMap);
      } catch (Exception e) {
         throw new InternalServerError(e);
      }
   }

   public static HttpInputStream fromObject(Object object, String[] accepts, Locale[] locales) {
      if (accepts.length == 0) {
         accepts = new String[] { ContentType.JSON };
      }
      for (String contentType : accepts) {
         Serializer ser = instanceOf(contentType, locales);
         if (ser != null) {
            String result = ser.write(object);
            if (result != null) {
               return new HttpInputStream(result.getBytes(UTF_8), contentType, UTF_8);
            }
         }
      }
      throw new NotAcceptableException();
   }

   public static void enableXml(boolean enable) {
      if (enable && !handlers.containsKey(ContentType.XML)) {
         handlers.put(ContentType.XML, XmlSerializer.class);
      } else if (!enable && handlers.containsKey(ContentType.XML)) {
         handlers.remove(ContentType.XML);
      }
   }

   public static String getContentTypeFromExtension(String extension, Class<?> startupClass) {
      String result = getMimeFromResourceIn(extension, Serializer.class);
      if (result == null && startupClass != null) {
         result = getMimeFromResourceIn(extension, startupClass);
      }
      if (result == null) {
         result = ContentType.BINARY;
      }
      return result;
   }

   protected <T> T read(byte[] content, Class<T> type, Map<String, String> contentType) {
      Charset charset = UTF_8;
      if (contentType.containsKey("charset")) {
         String charsetName = contentType.get("charset");
         try {
            charset = Charset.forName(charsetName);
         } catch (Exception e) {
            throw new UnsupportedMediaTypeException(
                     CoreMessage.UNEXPECTED_ENCODING.with(charsetName));
         }
      }
      String contentStr = new String(content, charset);
      return read(contentStr, type);
   }

   protected abstract <T> T read(String content, Class<T> type);

   protected abstract String write(Object object);

   protected static final Field findField(String fieldName, Class<?> type) {
      Class<?> superType = type;
      while (superType != null) {
         try {
            return superType.getDeclaredField(fieldName);
         } catch (NoSuchFieldException e) {
            superType = superType.getSuperclass() == Object.class ? null
                     : superType.getSuperclass();
         } catch (NullPointerException e) {
            break;
         }
      }
      return null;
   }

   protected static final boolean isStaticOrTransientOrVolatile(Field field) {
      int modifiers = field.getModifiers();
      return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)
               || Modifier.isVolatile(modifiers);
   }

   private static String getMimeFromResourceIn(String extension, Class<?> baseClass) {
      // During tests, one file is copied over the other so we need this
      String filename = baseClass == Serializer.class ? "/file.types" : "/mime.types";
      try (InputStream mimes = baseClass.getResourceAsStream(filename)) {
         BufferedReader br = new BufferedReader(new InputStreamReader(mimes));
         String line;
         while ((line = br.readLine()) != null) {
            String[] lineParts = line.trim().split("\\s+");
            for (int i = 1; i < lineParts.length; ++i) {
               if (!lineParts[i].isEmpty() && lineParts[i].equals(extension)) {
                  return lineParts[0].trim();
               }
            }
         }
      } catch (IOException e) {
         // Should not happen but in any case...
      }
      return null;
   }

   private static Serializer instanceOf(String contentType, Locale... locales) {
      Class<? extends Serializer> clazz = handlers.get(contentType);
      try {
         Constructor<? extends Serializer> constructor = ClassUtils.getConstructor(clazz,
                  Locale[].class);
         if (constructor != null) {
            return constructor.newInstance(new Object[] { locales });
         }
         return clazz.newInstance();
      } catch (Exception e) {
         return null;
      }
   }
}
