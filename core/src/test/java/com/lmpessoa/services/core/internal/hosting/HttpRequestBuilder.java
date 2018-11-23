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
package com.lmpessoa.services.core.internal.hosting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lmpessoa.services.core.hosting.Headers;
import com.lmpessoa.services.core.hosting.HttpRequest;
import com.lmpessoa.services.core.routing.HttpMethod;

public final class HttpRequestBuilder {

   private Map<String, List<String>> headers = new HashMap<>();
   private Map<String, String> cookies = new HashMap<>();
   private HttpMethod method = HttpMethod.GET;
   private String version = "1.1";
   private String query = null;
   private String path = null;
   private String body = null;

   public HttpRequestBuilder setMethod(HttpMethod method) {
      this.method = method;
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

   public HttpRequestBuilder setVersion(String version) {
      this.version = version;
      return this;
   }

   public HttpRequestBuilder setContentType(String contentType) {
      headers.put(Headers.CONTENT_TYPE, Arrays.asList(contentType));
      return this;
   }

   public HttpRequestBuilder setHost(String host) {
      headers.put(Headers.HOST, Arrays.asList(host));
      return this;
   }

   public HttpRequestBuilder setBody(String body) {
      headers.put(Headers.CONTENT_LENGTH, Arrays.asList(String.valueOf(body.length())));
      this.body = body;
      return this;
   }

   public HttpRequestBuilder addHeader(String name, String value) {
      this.headers.put(Headers.normalise(name), Arrays.asList(value));
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
      result.append(" HTTP/");
      result.append(version);
      result.append("\r\n");
      headers.entrySet().stream().forEach(e -> {
         e.getValue().stream().forEach(v -> {
            result.append(e.getKey());
            result.append(": ");
            result.append(v);
            result.append("\r\n");
         });
      });
      if (body != null) {
         result.append("\r\n");
         result.append(body);
      }
      return new ByteArrayInputStream(result.toString().getBytes());
   }

   public HttpRequest build() throws IOException {
      return new HttpRequestImpl(buildAsStream());
   }
}
