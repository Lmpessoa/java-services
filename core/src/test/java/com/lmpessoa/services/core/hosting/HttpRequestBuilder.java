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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.lmpessoa.services.core.hosting.HttpRequest;
import com.lmpessoa.services.core.hosting.HttpRequestImpl;

public final class HttpRequestBuilder {

   private String method = "GET";
   private String path = null;
   private String protocol = "HTTP/1.1";
   private String query = null;
   private Map<String, String> headers = new HashMap<>();
   private Map<String, String> cookies = new HashMap<>();
   private String body;

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

   public HttpRequestBuilder setProtocol(String protocol) {
      this.protocol = protocol.toUpperCase();
      return this;
   }

   public HttpRequestBuilder setQueryString(String query) {
      this.query = query;
      return this;
   }

   public HttpRequestBuilder setContentType(String contentType) {
      this.headers.put("Content-Type", contentType);
      return this;
   }

   public HttpRequestBuilder setHost(String host) {
      this.headers.put("Host", host);
      return this;
   }

   public HttpRequestBuilder setBody(String body) {
      this.headers.put("Content-Length", String.valueOf(body.length()));
      this.body = body;
      return this;
   }

   public HttpRequestBuilder addHeader(String name, String value) {
      this.headers.put(name, value);
      return this;
   }

   public HttpRequestBuilder addCookie(String name, String value) {
      this.cookies.put(name, value);
      return this;
   }

   public HttpRequest build() throws IOException {
      return new HttpRequestImpl(buildInputStream());
   }

   private InputStream buildInputStream() {
      StringBuilder sb = new StringBuilder();
      sb.append(method);
      sb.append(' ');
      sb.append(path);
      if (query != null) {
         sb.append('?');
         sb.append(query);
      }
      sb.append(' ');
      sb.append(protocol);
      sb.append("\r\n");

      for (Entry<String, String> header : headers.entrySet()) {
         if (!"Cookie".equals(header.getKey())) {
            sb.append(header.getKey());
            sb.append(": ");
            sb.append(header.getValue());
            sb.append("\r\n");
         }
      }

      StringBuilder cookies = new StringBuilder();
      for (Entry<String, String> cookie : this.cookies.entrySet()) {
         if (cookies.length() != 0) {
            sb.append("; ");
         }
         sb.append(cookie.getKey());
         sb.append('=');
         sb.append(cookie.getValue());
      }
      sb.append("Cookie: ");
      sb.append(cookies.toString());
      sb.append("\r\n");

      if (body != null) {
         sb.append("\r\n");
         sb.append(body);
      }

      return new ByteArrayInputStream(sb.toString().getBytes());
   }
}
