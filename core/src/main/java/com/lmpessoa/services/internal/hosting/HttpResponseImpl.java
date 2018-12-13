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
package com.lmpessoa.services.internal.hosting;

import com.lmpessoa.services.HttpInputStream;
import com.lmpessoa.services.hosting.Headers;
import com.lmpessoa.services.hosting.HttpResponse;
import com.lmpessoa.services.hosting.ValuesMap;
import com.lmpessoa.services.internal.ValuesMapBuilder;

final class HttpResponseImpl implements HttpResponse {

   private final HttpInputStream stream;
   private final String dateHeader;
   private final int statusCode;
   private final int length;

   @Override
   public int getStatusCode() {
      return statusCode;
   }

   @Override
   public ValuesMap getHeaders() {
      ValuesMapBuilder result = new ValuesMapBuilder();
      if (dateHeader != null) {
         result.add(Headers.DATE, dateHeader);
      }
      return result.build();
   }

   @Override
   public HttpInputStream getContentBody() {
      return stream;
   }

   @Override
   public String toString() {
      String resultType = stream != null ? stream.getType() : "(empty)";
      return String.format("%s %s %s", statusCode, length, resultType);
   }

   HttpResponseImpl(int statusCode, HttpInputStream stream, String dateHeader) {
      this.statusCode = statusCode;
      this.dateHeader = dateHeader;
      this.stream = stream;
      int size = 0;
      try {
         size = stream.available();
      } catch (Exception e) {
         // Just ignore for now
      }
      this.length = size;
   }
}
