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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public final class Headers {

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

   /**
    * Splits an HTTP header value in a component map.
    *
    * <p>
    * An HTTP header value is composed of a single direct value and, optionally, by multiple
    * key/value pairs separated by semicolons. This method parses a given HTTP header value in this
    * format into a {@link Map} where keys can be easily read. The main value of the header value is
    * stored in this map with an empty string as key.
    * </p>
    *
    * @param headerValue the HTTP header value to be split.
    * @return a map containing the main HTTP header value and any associated key/value pairs.
    */
   public static Map<String, String> split(String headerValue) {
      int pos = 0;
      int i = pos;
      headerValue += ";";
      Map<String, String> result = new HashMap<>();
      while (pos < headerValue.length()) {
         char ch = headerValue.charAt(i);
         if (ch == '"' || ch == '\'') {
            i = skipString(i, headerValue);
         } else if (ch == ';') {
            String[] subvalue = headerValue.substring(pos, i).split("=");
            if (result.isEmpty()) {
               result.put("", subvalue[0].trim());
            } else {
               subvalue[0] = subvalue[0].trim();
               subvalue[1] = subvalue[1].trim();
               Matcher m = Pattern.compile("\"([^\"]*)\"").matcher(subvalue[1]);
               if (m.find()) {
                  subvalue[1] = m.group(1);
               }
               result.put(subvalue[0], subvalue[1]);
            }
            pos = i + 1;
         }
         i += 1;
      }
      return result;
   }

   /**
    * Returns a normalised version of the given header name.
    * <p>
    * As a standard, HTTP header names are formatted in all lower case with the initial of each word
    * in upper case. Words in the header name are separated by dashes. Exceptions to this rule are
    * also treated by this method.
    * </p>
    *
    * @param headerName the name of the header to be normalised.
    * @return the normalised version of the given header name.
    */
   public static String normalise(String headerName) {
      Objects.requireNonNull(headerName);
      headerName = headerName.trim();
      if (!headerName.matches("[a-zA-Z][a-zA-Z0-9-]*")) {
         throw new BadRequestException("Illegal HTTP header name: " + headerName);
      }
      for (Field f : Headers.class.getDeclaredFields()) {
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

   private static int skipString(int pos, String str) {
      char delimiter = str.charAt(pos);
      pos += 1;
      while (str.charAt(pos) != delimiter) {
         pos += 1;
      }
      return pos;
   }

   private Headers() {
   }
}
