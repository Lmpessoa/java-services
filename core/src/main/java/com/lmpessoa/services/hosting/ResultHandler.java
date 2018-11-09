/*
 * Copyright (c) 2017 Leonardo Pessoa
 * http://github.com/lmpessoa/java-services
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

import java.util.Arrays;

import com.lmpessoa.services.core.MediaType;
import com.lmpessoa.services.routing.content.Serializer;

final class ResultHandler {

   private NextHandler next;

   public ResultHandler(NextHandler next) {
      this.next = next;
   }

   public HttpResult invoke(HttpRequest request) {
      Object obj = getResultObject();
      int statusCode = getStatusCode(obj);
      HttpResultInputStream is = getContentBody(obj, request);
      return new HttpResult(statusCode, obj, is);
   }

   private Object getResultObject() {
      try {
         return next.invoke();
      } catch (Throwable t) {
         // This is correct; we capture every kind of exception here
         return t;
      }
   }

   private int getStatusCode(Object obj) {
      if (obj == null) {
         return 204;
      } else if (obj instanceof IHttpStatus) {
         return ((IHttpStatus) obj).getStatusCode();
      }
      return 200;
   }

   private HttpResultInputStream getContentBody(Object obj, HttpRequest request) {
      String[] accepts;
      if (request.getHeaders().containsKey("Accept")) {
         accepts = Arrays.stream(request.getHeaders().get("Accept").split("\\.")) //
                  .map(String::trim) //
                  .toArray(String[]::new);
      } else {
         accepts = new String[] { MediaType.JSON };
      }
      return Serializer.produce(accepts, obj);
   }
}
