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

import java.io.IOException;
import java.net.URL;

import com.lmpessoa.services.core.ContentType;
import com.lmpessoa.services.core.HttpInputStream;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.NonTraced;

@NonTraced
final class FaviconHandler {

   private static final String FAVICON = "/favicon.ico";
   private final NextHandler next;

   public FaviconHandler(NextHandler next) {
      this.next = next;
   }

   public Object invoke(HttpRequest request, IApplicationInfo info, ILogger log) {
      if (request.getPath().endsWith(FAVICON)) {
         Class<?> startupClass = info.getStartupClass();
         URL iconUrl = null;
         if (startupClass != null) {
            iconUrl = startupClass.getResource(FAVICON);
         }
         if (iconUrl == null) {
            iconUrl = FaviconHandler.class.getResource(FAVICON);
         }
         if (iconUrl != null) {
            try {
               return new HttpInputStream(ContentType.ICO, iconUrl.openStream());
            } catch (IOException e) {
               log.error(e);
            }
         }
      }
      return next.invoke();
   }
}
