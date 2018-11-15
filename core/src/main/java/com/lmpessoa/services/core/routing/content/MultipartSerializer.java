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
package com.lmpessoa.services.core.routing.content;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lmpessoa.services.core.HttpInputStream;
import com.lmpessoa.services.core.hosting.BadRequestException;
import com.lmpessoa.services.core.hosting.HeaderMap;
import com.lmpessoa.services.core.hosting.InternalServerError;

final class MultipartSerializer implements IContentReader {

   @Override
   public <T> T read(byte[] content, String contentType, Class<T> resultClass) {
      Map<String, String> disp = getHeaderValues(contentType);
      String boundary = disp.get("boundary");
      if (boundary == null) {
         throw new BadRequestException();
      }
      Collection<MultipartEntry> entries = parseMultipart(content, boundary);
      try {
         return mapToObject(entries, resultClass);
      } catch (InstantiationException | IllegalAccessException e) {
         throw new InternalServerError(e);
      }
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
         Map<String, String> disp = getHeaderValues(entry.headers.get(HeaderMap.CONTENT_DISPOSITION));
         if (!"form-data".equals(disp.get(""))) {
            throw new BadRequestException();
         }
         if (!disp.containsKey("name")) {
            throw new BadRequestException();
         }
         String contentType = entry.headers.get(HeaderMap.CONTENT_TYPE);
         Object value = null;
         if (contentType == null) {
            value = new String(entry.content);
         } else if (contentType.startsWith("multipart/mixed;")) {
            value = parseMixedMultipart(entry, contentType);
         } else {
            String filename = disp.get("filename");
            value = new HttpInputStream(contentType, entry.content, filename); // NOSONAR
         }
         String name = disp.get("name");
         Field field = FormSerializer.findField(name, resultClass);
         Class<?> fieldType = field.getType();
         if (fieldType.isArray() && fieldType.getComponentType().isAssignableFrom(HttpInputStream.class)) {
            if (value instanceof Collection<?>) {
               value = ((Collection<InputStream>) value).toArray(new HttpInputStream[0]);
            } else if (value instanceof HttpInputStream) {
               value = new HttpInputStream[] { (HttpInputStream) value };
            }
         } else if (value instanceof String && field.getType() != String.class) {
            value = FormSerializer.convert((String) value, field.getType());
         }
         field.setAccessible(true);
         field.set(result, value);
      }
      return result;
   }

   private Collection<InputStream> parseMixedMultipart(MultipartEntry entry, String contentType) {
      Map<String, String> mixedDisp = getHeaderValues(contentType);
      String mixedBoundary = mixedDisp.get("boundary");
      Collection<MultipartEntry> mixedEntries = parseMultipart(entry.content, mixedBoundary);
      List<InputStream> result = new ArrayList<>();
      for (MultipartEntry mixedEntry : mixedEntries) {
         Map<String, String> mixedEntryDisp = getHeaderValues(mixedEntry.headers.get(HeaderMap.CONTENT_DISPOSITION));
         if (!"file".equals(mixedEntryDisp.get(""))) {
            throw new BadRequestException();
         }
         String mixedFilename = mixedEntryDisp.get("filename");
         String mixedContentType = mixedEntry.headers.get(HeaderMap.CONTENT_TYPE);
         result.add(new HttpInputStream(mixedContentType, mixedEntry.content, mixedFilename));
      }
      return result;
   }

   private Map<String, String> getHeaderValues(String header) {
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

   private static class MultipartEntry {

      Map<String, String> headers = new HashMap<>();
      byte[] content;
   }
}
