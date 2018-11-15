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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.lmpessoa.services.core.MediaType;

final class HttpRequestWrapper implements HttpRequest {

   private final HttpServletRequest request;

   private Map<String, Collection<String>> query = null;
   private Map<String, Collection<String>> form = null;
   private HeaderMap headers = null;
   private byte[] content;

   HttpRequestWrapper(HttpServletRequest request) {
      this.request = request;
   }

   @Override
   public String getMethod() {
      return request.getMethod();
   }

   @Override
   public String getPath() {
      return request.getPathInfo();
   }

   @Override
   public String getProtocol() {
      return request.getProtocol();
   }

   @Override
   public String getQueryString() {
      return request.getQueryString();
   }

   @Override
   public long getContentLength() {
      return request.getContentLengthLong();
   }

   @Override
   public String getContentType() {
      return request.getContentType();
   }

   @Override
   public InputStream getBody() throws IOException {
      if (content == null) {
         try (InputStream is = request.getInputStream()) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) > 0) {
               os.write(buffer, 0, len);
            }
            content = os.toByteArray();
         }
      }
      if (content != null) {
         return new ByteArrayInputStream(content);
      }
      return null;
   }

   @Override
   public synchronized HeaderMap getHeaders() {
      if (headers == null) {
         HeaderMap result = new HeaderMap();
         Enumeration<String> headerNames = request.getHeaderNames();
         while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            result.add(header, request.getHeader(header));
         }
         result.freeze();
         headers = result;
      }
      return headers;
   }

   @Override
   public String getHeader(String headerName) {
      return request.getHeader(headerName);
   }

   @Override
   public Map<String, Collection<String>> getQuery() {
      String queryString = request.getQueryString();
      if (query == null && queryString != null) {
         final Map<String, List<String>> result = new HashMap<>();
         for (String var : queryString.split("&")) {
            String[] parts = var.split("=", 2);
            try {
               parts[0] = URLDecoder.decode(parts[0], request.getCharacterEncoding());
               parts[1] = URLDecoder.decode(parts[1], request.getCharacterEncoding());
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
   public Map<String, Collection<String>> getForm() {
      if (form == null && MediaType.FORM.equals(getContentType())) {
         Map<String, List<String>> result = new HashMap<>();
         try {
            getBody();
         } catch (IOException e) {
            request.getServletContext().log(e.getMessage(), e);
            return null;
         }
         for (String var : new String(content).split("&")) {
            String[] parts = var.split("=", 2);
            try {
               parts[0] = URLDecoder.decode(parts[0], request.getCharacterEncoding());
               parts[1] = URLDecoder.decode(parts[1], request.getCharacterEncoding());
            } catch (UnsupportedEncodingException e) {
               // Ignore
            }
            if (!result.containsKey(parts[0])) {
               result.put(parts[0], new ArrayList<>());
            }
            result.get(parts[0]).add(parts[1]);
         }
         form = Collections.unmodifiableMap(result);
      }
      return form;
   }

   @Override
   public Map<String, String> getCookies() {
      Map<String, String> result = new HashMap<>();
      for (Cookie cookie : request.getCookies()) {
         result.put(cookie.getName(), cookie.getValue());
      }
      return result;
   }
}
