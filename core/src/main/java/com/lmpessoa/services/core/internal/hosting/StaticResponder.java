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
package com.lmpessoa.services.core.internal.hosting;

import java.io.IOException;
import java.net.URL;

import com.lmpessoa.services.core.HttpInputStream;
import com.lmpessoa.services.core.InternalServerError;
import com.lmpessoa.services.core.MethodNotAllowedException;
import com.lmpessoa.services.core.NotFoundException;
import com.lmpessoa.services.core.hosting.HttpRequest;
import com.lmpessoa.services.core.hosting.IApplicationInfo;
import com.lmpessoa.services.core.hosting.NextResponder;
import com.lmpessoa.services.core.internal.serializing.Serializer;
import com.lmpessoa.services.core.routing.HttpMethod;
import com.lmpessoa.services.core.routing.RouteMatch;

final class StaticResponder {

   private final ApplicationOptions options;
   private final NextResponder next;

   public StaticResponder(NextResponder next, ApplicationOptions options) {
      this.options = options;
      this.next = next;
   }

   public Object invoke(IApplicationInfo info, HttpRequest request, RouteMatch route) {
      final String staticPath = options.getStaticPath();
      if (staticPath != null && request.getMethod() == HttpMethod.GET
               && (route instanceof NotFoundException
                        || route instanceof MethodNotAllowedException)) {
         Class<?> startupClass = info.getStartupClass();
         URL resource = startupClass.getResource(staticPath + request.getPath());
         if (resource != null) {
            String path = resource.getPath();
            String extension = path.substring(path.lastIndexOf('.') + 1);
            String mimetype = Serializer.getContentTypeFromExtension(extension, startupClass);
            try {
               return new HttpInputStream(resource.openStream(), mimetype);
            } catch (IOException e) {
               throw new InternalServerError(e);
            }
         }
      }
      return next.invoke();
   }
}
