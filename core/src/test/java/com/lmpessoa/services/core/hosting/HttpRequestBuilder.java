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
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.lmpessoa.services.core.hosting.HeaderMap;
import com.lmpessoa.services.core.hosting.HttpRequest;

public final class HttpRequestBuilder {

   private Map<String, String> cookies = new HashMap<>();
   private HeaderMap headers = new HeaderMap();
   private String method = "GET";
   private String query = null;
   private String path = null;
   private String body = null;

   public HttpRequestBuilder setMethod(String method) {
      if (method.indexOf(' ') != -1) {
         throw new IllegalArgumentException("Method cannot contain spaces");
      }
      this.method = method.toUpperCase();
      return this;
   }

   public HttpRequestBuilder setPath(String path) {
      this.path = path;
      return this;
   }

   public HttpRequestBuilder setQueryString(String query) {
      this.query = query;
      return this;
   }

   public HttpRequestBuilder setContentType(String contentType) {
      this.headers.set("Content-Type", contentType);
      return this;
   }

   public HttpRequestBuilder setHost(String host) {
      this.headers.set("Host", host);
      return this;
   }

   public HttpRequestBuilder setBody(String body) {
      this.headers.set("Content-Length", String.valueOf(body.length()));
      this.body = body;
      return this;
   }

   public HttpRequestBuilder addHeader(String name, String value) {
      this.headers.set(name, value);
      return this;
   }

   public HttpRequestBuilder addCookie(String name, String value) {
      this.cookies.put(name, value);
      return this;
   }

   public InputStream buildAsStream() {
      StringBuilder result = new StringBuilder();
      result.append(method);
      result.append(' ');
      result.append(path);
      if (query != null) {
         result.append('?');
         result.append(query);
      }
      result.append(" HTTP/1.1\r\n");
      headers.stream().forEach(e -> {
         result.append(e.getKey());
         result.append(": ");
         result.append(e.getValue());
         result.append("\r\n");
      });
      if (body != null) {
         result.append("\r\n");
         result.append(body);
      }
      return new ByteArrayInputStream(result.toString().getBytes());
   }

   public HttpRequest build() {
      return new HttpRequest() {

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
            return "HTTP/1.1";
         }

         @Override
         public String getQueryString() {
            return query;
         }

         @Override
         public long getContentLength() {
            try {
               return Long.parseLong(headers.get("Content-Length"));
            } catch (Exception e) {
               return 0;
            }
         }

         @Override
         public String getContentType() {
            return headers.get("Content-Type");
         }

         @Override
         public InputStream getBody() {
            return new ByteArrayInputStream(body.getBytes());
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
         public Map<String, Collection<String>> getQuery() {
            // TODO Auto-generated method stub
            return null;
         }
      };
   }
}
