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
package com.lmpessoa.services.hosting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.lmpessoa.services.internal.ValuesMapBuilder;
import com.lmpessoa.services.internal.serializing.Serializer;
import com.lmpessoa.services.routing.HttpMethod;

public final class HttpRequestBuilder {

   private ValuesMapBuilder headers = new ValuesMapBuilder();
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
      String[] parts = path.split("\\?", 2);
      this.path = parts[0];
      if (parts.length > 1) {
         this.query = parts[1];
      }
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
      headers.add(Headers.CONTENT_TYPE, contentType);
      return this;
   }

   public HttpRequestBuilder setHost(String host) {
      headers.add(Headers.HOST, host);
      return this;
   }

   public HttpRequestBuilder setBody(String body) {
      headers.add(Headers.CONTENT_LENGTH, String.valueOf(body.length()));
      this.body = body;
      return this;
   }

   public HttpRequestBuilder addHeader(String name, String value) {
      this.headers.add(Headers.normalise(name), value);
      return this;
   }

   public HttpRequest build() throws IOException {
      final ValuesMap headers = this.headers.build();
      final HttpMethod method = this.method;
      final String version = this.version;
      final String queryStr = this.query;
      final String path = this.path;

      ValuesMap tmp = Serializer.parseHttpForm(queryStr);
      if (tmp == null) {
         tmp = ValuesMap.empty();
      }
      final ValuesMap query = tmp;

      return new HttpRequest() {

         @Override
         public HttpMethod getMethod() {
            return method;
         }

         @Override
         public String getPath() {
            return path;
         }

         @Override
         public String getQueryString() {
            return queryStr;
         }

         @Override
         public ValuesMap getQuery() {
            return query;
         }

         @Override
         public String getProtocol() {
            return String.format("HTTP/%s", version);
         }

         @Override
         public ValuesMap getHeaders() {
            return headers;
         }

         @Override
         public InputStream getContentBody() {
            return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
         }

         @Override
         public ValuesMap getForm() {
            return ValuesMap.empty();
         }

      };
   }
}
