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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class HttpRequestImpl implements HttpRequest {

   private static final String UTF8 = "UTF-8";

   private final String queryString;
   private final HeaderMap headers;
   private final String protocol;
   private final byte[] content;
   private final String method;
   private final String path;

   private Map<String, String> cookies;
   private Map<String, Collection<String>> query;

   @Override
   public String getMethod() {
      return method;
   }

   @Override
   public String getPath() {
      return path;
   }

   @Override
   public String getProtocol() {
      return protocol;
   }

   @Override
   public String getQueryString() {
      return queryString;
   }

   @Override
   public long getContentLength() {
      String length = headers.get("Content-Length");
      return length != null ? Long.parseLong(length) : 0;
   }

   @Override
   public String getContentType() {
      return headers.get("Content-Type");
   }

   @Override
   public InputStream getBody() {
      if (content != null) {
         return new ByteArrayInputStream(content);
      }
      return null;
   }

   @Override
   public HeaderMap getHeaders() {
      return headers;
   }

   @Override
   public String getHeader(String headerName) {
      return headers.get(headerName);
   }

   @Override
   public synchronized Map<String, Collection<String>> getQuery() {
      if (query == null && queryString != null) {
         final Map<String, List<String>> result = new HashMap<>();
         for (String var : queryString.split("&")) {
            String[] parts = var.split("=", 2);
            try {
               parts[0] = URLDecoder.decode(parts[0], UTF8);
               parts[1] = URLDecoder.decode(parts[1], UTF8);
            } catch (UnsupportedEncodingException e) {
               // Ignore
            }
            if (!result.containsKey(parts[0])) {
               result.put(parts[0], new ArrayList<>());
            }
            result.get(parts[0]).add(parts[1]);
         }
         query = Collections.unmodifiableMap(result);
      }
      return query;
   }

   @Override
   public synchronized Map<String, String> getCookies() {
      if (cookies == null && headers.contains("Cookie")) {
         Map<String, String> result = new HashMap<>();
         String tmp = headers.get("Cookie");
         if (tmp != null) {
            Arrays.asList(tmp.split(";"))
                     .stream() //
                     .map(s -> s.split("=", 2)) //
                     .forEach(s -> result.put(s[0].trim(), s[1].trim()));
         }
         this.cookies = Collections.unmodifiableMap(result);
      }
      return cookies;
   }

   @Override
   public String toString() {
      return String.format("%s %s %s", method, path + (queryString != null ? "?" + queryString : ""), protocol);
   }

   HttpRequestImpl(InputStream clientStream) throws IOException {
      String requestLine = readLine(clientStream);
      if (requestLine.isEmpty()) {
         throw new SocketTimeoutException();
      }
      String[] parts = requestLine.split(" ");
      this.method = parts[0];
      this.protocol = parts[2];
      parts = parts[1].split("\\?", 2);
      String thePath = parts[0];
      HeaderMap headerMap = new HeaderMap();
      if (thePath.startsWith("http://") || thePath.startsWith("https://")) {
         int index = thePath.indexOf('/', thePath.indexOf("://") + 3);
         headerMap.add("Host", thePath.substring(0, index));
         thePath = thePath.substring(index);
      }
      this.path = thePath;
      this.queryString = parts.length > 1 ? parts[1] : null;
      String headerLine;
      while ((headerLine = readLine(clientStream)) != null && !headerLine.isEmpty()) {
         String[] head = headerLine.split(":", 2);
         if (head.length != 2) {
            throw new IllegalStateException("Illegal header line: '" + headerLine + "'");
         }
         headerMap.add(head[0].trim(), head[1].trim());
      }
      headerMap.freeze();
      this.headers = headerMap;
      if (headerMap.contains("Content-Type")) {
         if (!headerMap.contains("Content-Length")) {
            throw new LengthRequiredException();
         }
         long contentLength = getContentLength();
         if (contentLength < 0 || contentLength > Integer.MAX_VALUE) {
            throw new PayloadTooLargeException();
         }
         byte[] data = new byte[(int) getContentLength()];
         while (clientStream.available() < data.length) {
            // Do nothing, just sit and wait
         }
         int read = clientStream.read(data);
         if (read != data.length) {
            throw new AssertionError(
                     "Error reading from client (expected: " + data.length + " bytes, found: " + read + " bytes)");
         }
         this.content = data;
      } else {
         this.content = new byte[0];
      }
   }

   private String readLine(InputStream input) throws IOException {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      int[] b = new int[] { 0, 0 };
      while ((b[1] = input.read()) > -1 && !Arrays.equals(b, new int[] { '\r', '\n' })) {
         result.write(new byte[] { (byte) b[1] });
         b[0] = b[1];
      }
      return result.toString(UTF8).trim();
   }
}
