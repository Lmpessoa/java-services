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
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

final class HttpRequestWrapper implements HttpRequest {

   private static final String UTF_8 = "utf-8";
   private Map<String, Collection<String>> query = null;
   private byte[] content;

   private final String queryString;
   private final HeaderMap headers;
   private final Cookie[] cookies;
   private final Charset charset;
   private final String protocol;
   private final String method;
   private final String path;

   HttpRequestWrapper(HttpServletRequest request) throws IOException {
      this.method = request.getMethod();
      this.path = request.getPathInfo();
      this.protocol = request.getProtocol();
      this.queryString = request.getQueryString();
      headers = new HeaderMap();
      Enumeration<String> headerNames = request.getHeaderNames();
      while (headerNames.hasMoreElements()) {
         String header = headerNames.nextElement();
         headers.add(header, request.getHeader(header));
      }
      headers.freeze();
      if (headers.contains("Content-Type")) {
         if (!headers.contains("Content-Length")) {
            throw new LengthRequiredException();
         }
         long contentLength = getContentLength();
         if (getContentLength() > Integer.MAX_VALUE || getContentLength() < 0) {
            throw new PayloadTooLargeException();
         }
         try (InputStream is = request.getInputStream()) {
            while (is.available() < contentLength) {
               // Does nothing, just wait
            }
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            while (os.size() < contentLength) {
               int len = is.read(buffer, 0, Math.min(4096, (int) contentLength - os.size()));
               os.write(buffer, 0, len);
            }
            this.content = os.toByteArray();
         }
      } else {
         this.content = new byte[0];
      }
      String encoding = request.getCharacterEncoding();
      if (encoding == null) {
         encoding = UTF_8;
      }
      this.charset = Charset.forName(encoding);
      this.cookies = request.getCookies();
   }

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
      String result = headers.get(HeaderMap.CONTENT_LENGTH);
      if (result != null) {
         return Integer.parseInt(result);
      }
      return 0;
   }

   @Override
   public String getContentType() {
      return headers.get(HeaderMap.CONTENT_TYPE);
   }

   @Override
   public InputStream getBody() {
      if (content != null) {
         return new ByteArrayInputStream(content);
      }
      return null;
   }

   @Override
   public synchronized HeaderMap getHeaders() {
      return headers;
   }

   @Override
   public String getHeader(String headerName) {
      return headers.get(headerName);
   }

   @Override
   public Map<String, Collection<String>> getQuery() {
      if (query == null && queryString != null) {
         final Map<String, List<String>> result = new HashMap<>();
         for (String var : queryString.split("&")) {
            String[] parts = var.split("=", 2);
            try {
               parts[0] = URLDecoder.decode(parts[0], UTF_8);
               parts[1] = URLDecoder.decode(parts[1], UTF_8);
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
   public Map<String, String> getCookies() {
      Map<String, String> result = new HashMap<>();
      for (Cookie cookie : cookies) {
         result.put(cookie.getName(), cookie.getValue());
      }
      return result;
   }

   public Charset getCharset() {
      return charset;
   }
}
