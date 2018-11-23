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

import java.io.IOException;
import java.net.URL;

import com.lmpessoa.services.core.hosting.ContentType;
import com.lmpessoa.services.core.hosting.HttpInputStream;
import com.lmpessoa.services.core.hosting.HttpRequest;
import com.lmpessoa.services.core.hosting.IApplicationInfo;
import com.lmpessoa.services.core.hosting.InternalServerError;
import com.lmpessoa.services.core.hosting.MethodNotAllowedException;
import com.lmpessoa.services.core.hosting.NextResponder;
import com.lmpessoa.services.core.hosting.NotFoundException;
import com.lmpessoa.services.core.routing.HttpMethod;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.util.logging.ILogger;

final class FaviconResponder {

   private static final String FAVICON = "/favicon.ico";
   private final NextResponder next;

   public FaviconResponder(NextResponder next) {
      this.next = next;
   }

   public Object invoke(HttpRequest request, RouteMatch route, IApplicationInfo info, ILogger log) {
      try {
         return next.invoke();
      } catch (NotFoundException | MethodNotAllowedException e) {
         if (request.getMethod() == HttpMethod.GET && request.getPath().endsWith(FAVICON)) {
            return getDefaultFavicon(info.getStartupClass());
         }
         throw e;
      }
   }

   private HttpInputStream getDefaultFavicon(Class<?> startupClass) {
      URL iconUrl = null;
      if (startupClass != null) {
         iconUrl = startupClass.getResource(FAVICON);
      }
      if (iconUrl == null) {
         iconUrl = FaviconResponder.class.getResource(FAVICON);
      }
      try {
         return new HttpInputStream(iconUrl.openStream(), ContentType.ICO);
      } catch (IOException e) {
         throw new InternalServerError(e);
      }
   }
}
