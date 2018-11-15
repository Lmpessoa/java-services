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

import java.util.Objects;

import com.lmpessoa.services.core.HttpInputStream;

final class HttpResult {

   private final HttpInputStream contentStream;
   private final HttpRequest request;
   private final int contentLength;
   private final int statusCode;
   private final Object object;

   HttpResult(HttpRequest request, int statusCode, Object contentObject, HttpInputStream contentStream) {
      this.request = Objects.requireNonNull(request);
      this.statusCode = statusCode;
      this.object = contentObject;
      this.contentStream = contentStream;
      int size = 0;
      try {
         size = contentStream.available();
      } catch (Exception e) {
         // Just ignore for now
      }
      this.contentLength = size;
   }

   public int getStatusCode() {
      return statusCode;
   }

   public Object getObject() {
      return object;
   }

   public HttpInputStream getInputStream() {
      return contentStream;
   }

   @Override
   public String toString() {
      String userAgent = request.getHeaders().get(HeaderMap.USER_AGENT);
      String resultType = contentStream != null ? contentStream.getContentType() : "(empty)";
      return String.format("\"%s %s %s\" %s %s %s \"%s\"", request.getMethod(),
               request.getPath() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""),
               request.getProtocol(), statusCode, contentLength, resultType, userAgent);
   }
}
