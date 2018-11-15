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
import java.util.stream.Collectors;

final class HttpRequestImpl implements HttpRequest {

   private static final String UTF8 = "UTF-8";

   private final Map<String, List<String>> headers;
   private final String queryString;
   private final String protocol;
   private final byte[] content;
   private final String method;
   private final String path;

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
      String length = getHeader("Content-Length");
      return length != null ? Long.parseLong(length) : 0;
   }

   @Override
   public String getContentType() {
      return getHeader("Content-Type");
   }

   @Override
   public InputStream getBody() {
      if (content != null) {
         return new ByteArrayInputStream(content);
      }
      return null;
   }

   @Override
   public String[] getHeaderNames() {
      return headers.keySet().toArray(new String[0]);
   }

   @Override
   public String getHeader(String headerName) {
      List<String> header = headers.get(headerName);
      return header != null ? header.get(0) : null;
   }

   @Override
   public String[] getHeaderValues(String headerName) {
      List<String> header = headers.get(headerName);
      String[] emptyResult = new String[0];
      return header != null ? header.toArray(emptyResult) : emptyResult;
   }

   @Override
   public boolean containsHeaders(String headerName) {
      return headers.containsKey(headerName);
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
      Map<String, List<String>> headerMap = new HashMap<>();
      if (thePath.startsWith("http://") || thePath.startsWith("https://")) {
         int index = thePath.indexOf('/', thePath.indexOf("://") + 3);
         List<String> hostList = new ArrayList<>();
         hostList.add(thePath.substring(0, index));
         headerMap.put(Headers.HOST, hostList);
         thePath = thePath.substring(index);
      }
      this.path = thePath;
      this.queryString = parts.length > 1 ? parts[1] : null;
      this.headers = extractHeaders(clientStream, headerMap);
      if (headerMap.containsKey(Headers.CONTENT_TYPE)) {
         if (!headerMap.containsKey(Headers.CONTENT_LENGTH)) {
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

   private Map<String, List<String>> extractHeaders(InputStream clientStream, Map<String, List<String>> headerMap)
      throws IOException {
      String headerLine;
      while ((headerLine = readLine(clientStream)) != null && !headerLine.isEmpty()) {
         String[] head = headerLine.split(":", 2);
         if (head.length != 2) {
            throw new IllegalStateException("Illegal header line: '" + headerLine + "'");
         }
         String headerName = Headers.normalise(head[0]);
         if (!headerMap.containsKey(headerName)) {
            headerMap.put(headerName, new ArrayList<>());
         }
         headerMap.get(headerName).add(head[1].trim());
      }
      return headerMap.entrySet().stream().collect(
               Collectors.toMap(Map.Entry::getKey, e -> Collections.unmodifiableList(e.getValue())));
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
