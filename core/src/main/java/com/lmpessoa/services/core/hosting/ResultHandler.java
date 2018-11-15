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
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.lmpessoa.services.core.ContentType;
import com.lmpessoa.services.core.MediaType;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.routing.content.Serializer;
import com.lmpessoa.services.util.logging.NonTraced;

@NonTraced
final class ResultHandler {

   private NextHandler next;

   public ResultHandler(NextHandler next) {
      this.next = next;
   }

   public HttpResult invoke(HttpRequest request, RouteMatch route) {
      Object obj = getResultObject();
      int statusCode = getStatusCode(obj);
      HttpResultInputStream is = getContentBody(obj, request, route != null ? route.getMethod() : null);
      return new HttpResult(request, statusCode, obj, is);
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

   private HttpResultInputStream getContentBody(Object obj, HttpRequest request, Method method) {
      Object objx = obj;
      while (objx instanceof InternalServerError) {
         objx = ((InternalServerError) obj).getCause();
      }
      if (objx instanceof Throwable && !(objx instanceof HttpException)) {
         objx = ((Throwable) obj).getMessage();
      }
      if (objx == null || objx instanceof HttpException) {
         return null;
      }
      Object result = objx;
      if (result instanceof String) {
         result = ((String) result).getBytes(Charset.forName("UTF-8"));
      } else if (result instanceof ByteArrayOutputStream) {
         result = ((ByteArrayOutputStream) result).toByteArray();
      }
      if (result instanceof byte[]) {
         result = new ByteArrayInputStream((byte[]) result);
      }
      if (result instanceof HttpResultInputStream) {
         return (HttpResultInputStream) result;
      } else if (result instanceof InputStream) {
         ContentType contentAnn = method == null ? null : method.getAnnotation(ContentType.class);
         String contentType;
         if (contentAnn != null) {
            contentType = contentAnn.value();
         } else if (objx instanceof String) {
            contentType = MediaType.TEXT;
         } else {
            contentType = MediaType.BINARY;
         }
         return new HttpResultInputStream(contentType, (InputStream) result);
      }
      String[] accepts;
      if (request.getHeaders().contains("Accept")) {
         List<String> acceptList = new ArrayList<>();
         request.getHeaders().getAll("Accept").stream().map(s -> s.split(",")).forEach(
                  s -> Arrays.stream(s).map(String::trim).forEach(acceptList::add));
         accepts = acceptList.toArray(new String[0]);
      } else {
         accepts = new String[] { MediaType.JSON };
      }
      try {
         return Serializer.produce(accepts, obj);
      } catch (InstantiationException | IllegalAccessException e) {
         throw new InternalServerError(e);
      }
   }
}
