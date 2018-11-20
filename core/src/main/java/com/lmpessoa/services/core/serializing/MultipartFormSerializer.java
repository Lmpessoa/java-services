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
package com.lmpessoa.services.core.serializing;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lmpessoa.services.core.hosting.BadRequestException;
import com.lmpessoa.services.core.hosting.Headers;
import com.lmpessoa.services.core.hosting.HttpInputStream;
import com.lmpessoa.services.core.hosting.InternalServerError;
import com.lmpessoa.services.util.ClassUtils;

final class MultipartFormSerializer extends Serializer {

   @Override
   protected <T> T read(byte[] content, Class<T> type, Map<String, String> contentType) {
      String boundary = contentType.get("boundary");
      if (boundary == null) {
         throw new BadRequestException();
      }
      Collection<MultipartEntry> entries = parseMultipart(content, boundary);
      try {
         return mapToObject(entries, type);
      } catch (InstantiationException | IllegalAccessException e) {
         throw new InternalServerError(e);
      }
   }

   @Override
   protected String write(Object content) {
      return null;
   }

   private Collection<MultipartEntry> parseMultipart(byte[] content, String boundaryStr) {
      byte[] boundary = ("--" + boundaryStr).getBytes();
      ByteArrayReader reader = new ByteArrayReader(content);
      if (!Arrays.equals(boundary, reader.read(boundary.length))) {
         throw new BadRequestException();
      }
      boundary = ("\r\n--" + boundaryStr).getBytes();
      List<MultipartEntry> result = new ArrayList<>();
      while (reader.readLine().isEmpty()) {
         MultipartEntry entry = new MultipartEntry();
         String line;
         do {
            line = reader.readLine();
            String[] parts = line.split(":", 2);
            if (parts.length == 2) {
               entry.headers.put(parts[0].trim(), parts[1].trim());
            }
         } while (!line.isEmpty());
         entry.content = reader.readUntil(boundary);
         result.add(entry);
      }
      return result;
   }

   @SuppressWarnings("unchecked")
   private <T> T mapToObject(Collection<MultipartEntry> entries, Class<T> resultClass)
      throws InstantiationException, IllegalAccessException {
      T result = resultClass.newInstance();
      for (MultipartEntry entry : entries) {
         Map<String, String> disp = Headers.split(entry.headers.get(Headers.CONTENT_DISPOSITION));
         if (!"form-data".equals(disp.get(""))) {
            throw new BadRequestException();
         }
         if (!disp.containsKey("name")) {
            throw new BadRequestException();
         }
         String contentType = entry.headers.get(Headers.CONTENT_TYPE);
         Object value = null;
         if (contentType == null) {
            value = new String(entry.content);
         } else if (contentType.startsWith("multipart/mixed;")) {
            value = parseMixedMultipart(entry, contentType);
         } else {
            String filename = disp.get("filename");
            value = new HttpInputStream(contentType, entry.content, filename);
         }
         String name = disp.get("name");
         Field field = findField(name, resultClass);
         if (isStaticOrTransientOrVolatile(field)) {
            continue;
         }
         Class<?> fieldType = field.getType();
         if (fieldType.isArray()
                  && fieldType.getComponentType().isAssignableFrom(HttpInputStream.class)) {
            if (value instanceof Collection<?>) {
               value = ((Collection<InputStream>) value).toArray(new HttpInputStream[0]);
            } else if (value instanceof HttpInputStream) {
               value = new HttpInputStream[] { (HttpInputStream) value };
            }
         } else if (value instanceof String && field.getType() != String.class) {
            value = ClassUtils.cast((String) value, field.getType());
         }
         field.setAccessible(true);
         field.set(result, value);
      }
      return result;
   }

   private Collection<InputStream> parseMixedMultipart(MultipartEntry entry, String contentType) {
      Map<String, String> mixedDisp = Headers.split(contentType);
      String mixedBoundary = mixedDisp.get("boundary");
      Collection<MultipartEntry> mixedEntries = parseMultipart(entry.content, mixedBoundary);
      List<InputStream> result = new ArrayList<>();
      for (MultipartEntry mixedEntry : mixedEntries) {
         Map<String, String> mixedEntryDisp = Headers
                  .split(mixedEntry.headers.get(Headers.CONTENT_DISPOSITION));
         if (!"file".equals(mixedEntryDisp.get(""))) {
            throw new BadRequestException();
         }
         String mixedFilename = mixedEntryDisp.get("filename");
         String mixedContentType = mixedEntry.headers.get(Headers.CONTENT_TYPE);
         result.add(new HttpInputStream(mixedContentType, mixedEntry.content, mixedFilename));
      }
      return result;
   }

   private static class MultipartEntry {

      Map<String, String> headers = new HashMap<>();
      byte[] content;
   }
}
