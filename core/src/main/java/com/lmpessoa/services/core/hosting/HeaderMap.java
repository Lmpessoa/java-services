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
package com.lmpessoa.services.core.hosting;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents a map of HTTP header values.
 *
 * <p>
 * HTTP headers follow the same format for both request and response and may contain multiple values
 * in two formats:
 * </p>
 *
 * <ol>
 * <li>list of values (separated by commas, colons, etc.), or</li>
 * <li>multiple entries with the same name.</li>
 * </ol>
 *
 * <p>
 * Methods that get or set values for a header do not try to parse lists of values as they may me
 * implemented using different separators for different headers (especially custom headers). Thus,
 * these methods consume or return the unmodified string value given to this map for the desired
 * header.
 * </p>
 *
 * <p>
 * This map also normalises the name of the header given such that it matches the expected format
 * used in HTTP headers, thus names can be provided with any desired capitalisation. Constants with
 * the names of most common HTTP headers are also provided for convenience.
 * </p>
 */
public final class HeaderMap {

   public static final String ACCEPT = "Accept";
   public static final String ACCEPT_CHARSET = "Accept-Charset";
   public static final String ACCEPT_ENCODING = "Accept-Encoding";
   public static final String ACCEPT_LANGUAGE = "Accept-Language";
   public static final String AGE = "Age";
   public static final String ALLOW = "Allow";
   public static final String AUTHORIZATION = "Authorization";
   public static final String CACHE_CONTROL = "Cache-Control";
   public static final String CONNECTION = "Connection";
   public static final String CONTENT_DISPOSITION = "Content-Disposition";
   public static final String CONTENT_ENCODING = "Content-Encoding";
   public static final String CONTENT_LENGTH = "Content-Length";
   public static final String CONTENT_TYPE = "Content-Type";
   public static final String COOKIE = "Cookie";
   public static final String DATE = "Date";
   public static final String ETAG = "ETag";
   public static final String EXPIRES = "Expires";
   public static final String HOST = "Host";
   public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
   public static final String LAST_MODIFIED = "Last-Modified";
   public static final String LOCATION = "Location";
   public static final String P3P = "P3P";
   public static final String RANGE = "Range";
   public static final String SERVER = "Server";
   public static final String SET_COOKIE = "Set-Cookie";
   public static final String USER_AGENT = "User-Agent";
   public static final String TE = "TE";
   public static final String WARNING = "Warning";
   public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

   private final Map<String, List<String>> values = new HashMap<>();
   private boolean modifiable = true;

   /**
    * Changes the value(s) of the given header to the given values.
    *
    * <p>
    * Note that, different from {@link #add(String, String...)}, this method will replace all
    * previously set values for the given header with the values given in the call to this method.
    * </p>
    *
    * @param headerName the name of the header to set values for.
    * @param values the values to set for the given header.
    */
   public void set(String headerName, String... values) {
      if (!modifiable) {
         throw new UnsupportedOperationException();
      }
      String header = normalise(headerName);
      List<String> valueList = new ArrayList<>(values.length);
      Arrays.stream(values).filter(Objects::nonNull).forEach(valueList::add);
      this.values.put(header, valueList);
   }

   /**
    * Adds the given value(s) to the given header.
    *
    * <p>
    * Note that, different from {@link #set(String, String...)}, this method will not replace the
    * previously set values for the given header but add the values given in the call to this method to
    * the list of existing values.
    * </p>
    *
    * @param headerName the name of the header to add values for.
    * @param values the values to add for the given header.
    */
   public void add(String headerName, String... values) {
      if (!modifiable) {
         throw new UnsupportedOperationException();
      }
      String header = normalise(headerName);
      List<String> valueList = this.values.get(header);
      if (valueList == null) {
         valueList = new ArrayList<>();
         this.values.put(header, valueList);
      }
      Arrays.stream(values).filter(Objects::nonNull).forEach(valueList::add);
   }

   /**
    * Returns the first registered value for the given header.
    *
    * <p>
    * This method is a simple shortcut to be used when the given header is expected to have only one
    * value or when the first value is meant to be the usable one. To get a list of all values for a
    * header, use {@link #getAll(String)} instead.
    * </p>
    *
    * @param headerName the name of the header to get values of.
    * @return the first registered value for the given header or <code>null</code> if the header is not
    * set in this map.
    */
   public String get(String headerName) {
      List<String> valueList = getAll(headerName);
      if (valueList != null && !valueList.isEmpty()) {
         return valueList.get(0);
      }
      return null;
   }

   /**
    * Returns a list of all registered values for the given header.
    *
    * @param headerName the name of the header to get values of.
    * @return the list of registered values for the given header or <code>null</code> if the header is
    * not set in this map.
    */
   public List<String> getAll(String headerName) {
      String header = normalise(headerName);
      List<String> headerList = values.get(header);
      if (headerList == null) {
         return null;
      }
      return Collections.unmodifiableList(headerList);
   }

   /**
    * Removes all values for the given header from this map.
    *
    * @param headerName the name of the header to remove values.
    * @return the list of values previously associated with this header.
    */
   public List<String> remove(String headerName) {
      if (!modifiable) {
         throw new UnsupportedOperationException();
      }
      String header = normalise(headerName);
      return values.remove(header);
   }

   /**
    * Returns a list of all headers set in this map.
    *
    * <p>
    * A header name returned in this list may contain more than one value associated. This is not
    * indicated anywhere in the result.
    * </p>
    *
    * @return a list of all headers set in this map.
    */
   public Set<String> getHeaderNames() {
      return values.keySet();
   }

   /**
    * Returns whether this map contains values for the given header name.
    *
    * @param headerName the name of the header to evaluate.
    * @return <code>true</code> if this map has values associated with the given header name or
    * <code>false</code> otherwise.
    */
   public boolean contains(String headerName) {
      String header = normalise(headerName);
      return values.containsKey(header);
   }

   /**
    * Returns whether this map is empty.
    *
    * @return <code>true</code> if this map is empty or <code>false</code> otherwise.
    */
   public boolean isEmpty() {
      return values.isEmpty();
   }

   /**
    * Returns a stream for working with headers in this map.
    *
    * <p>
    * This stream returns the same type of objects from a regular {@link java.util.Map} but note that
    * trying to call {@link Map.Entry#setValue(Object)} in these objects will throw an
    * {@link UnsupportedOperationException}.
    * </p>
    *
    * @return
    */
   public Stream<Map.Entry<String, String>> stream() {
      List<Map.Entry<String, String>> result = new ArrayList<>();
      for (Map.Entry<String, List<String>> entry : values.entrySet()) {
         for (String value : entry.getValue()) {
            result.add(new HeaderEntry(entry.getKey(), value));
         }
      }
      return result.stream();
   }

   @Override
   public String toString() {
      return stream().map(Object::toString).reduce("", (r, s) -> r + s + "\r\n");
   }

   void freeze() {
      modifiable = false;
   }

   private static String normalise(String headerName) {
      Objects.requireNonNull(headerName);
      if (!headerName.matches("[a-zA-Z][a-zA-Z0-9-]*")) {
         throw new IllegalArgumentException("Illegal HTTP header name");
      }
      for (Field f : HeaderMap.class.getDeclaredFields()) {
         if (Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers())) {
            try {
               String value = (String) f.get(null);
               if (headerName.equalsIgnoreCase(value)) {
                  return value;
               }
            } catch (IllegalArgumentException | IllegalAccessException e) {
               // Since we're inside the class itself, these won't happen
            }
         }
      }
      String[] parts = headerName.split("-");
      parts = Arrays.stream(parts)
               .filter(s -> !s.isEmpty())
               .map(String::toLowerCase)
               .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
               .toArray(String[]::new);
      return String.join("-", parts);
   }

   private static class HeaderEntry implements Map.Entry<String, String> {

      private final String value;
      private final String key;

      @Override
      public String getKey() {
         return key;
      }

      @Override
      public String getValue() {
         return value;
      }

      @Override
      public String setValue(String value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public String toString() {
         return String.format("%s: %s", key, value);
      }

      HeaderEntry(String key, String value) {
         this.key = key;
         this.value = value;
      }
   }
}
