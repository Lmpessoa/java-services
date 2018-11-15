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
package com.lmpessoa.services.core.hosting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.lmpessoa.services.core.routing.HttpMethod;
import com.lmpessoa.services.core.routing.RouteMatch;

final class StaticResponder {

   private static String staticPath = null;

   private final NextResponder next;

   public StaticResponder(NextResponder next) {
      this.next = next;
   }

   public Object invoke(IApplicationSettings app, HttpRequest request, RouteMatch route) {
      if (staticPath != null && request.getMethod() == HttpMethod.GET
               && (route instanceof NotFoundException
                        || route instanceof MethodNotAllowedException)) {
         Class<?> startupClass = app.getStartupClass();
         URL resource = startupClass.getResource(staticPath + request.getPath());
         if (resource != null) {
            String path = resource.getPath();
            String extension = path.substring(path.lastIndexOf('.') + 1);
            String mimetype = getMimeTypeFromExtension(extension, startupClass);
            try {
               return new HttpInputStream(mimetype, resource.openStream());
            } catch (IOException e) {
               throw new InternalServerError(e);
            }
         }
      }
      return next.invoke();
   }

   static String getStaticPath() {
      return staticPath;
   }

   static void setStaticPath(String staticPath) {
      StaticResponder.staticPath = staticPath;
   }

   private String getMimeTypeFromExtension(String extension, Class<?> startupClass) {
      String result = getMimeFromResourceIn(extension, StaticResponder.class);
      if (result == null) {
         result = getMimeFromResourceIn(extension, startupClass);
         if (result == null) {
            result = ContentType.BINARY;
         }
      }
      return result;
   }

   private String getMimeFromResourceIn(String extension, Class<?> baseClass) {
      Map<String, String> result = new HashMap<>();
      // During tests, one file is copied over the other so we need this
      String filename = baseClass == StaticResponder.class ? "/file.types" : "/mime.types";
      try (InputStream mimes = baseClass.getResourceAsStream(filename)) {
         BufferedReader br = new BufferedReader(new InputStreamReader(mimes));
         String line;
         while ((line = br.readLine()) != null) {
            String[] lineParts = line.trim().split(" ");
            for (int i = 1; i < lineParts.length; ++i) {
               if (!lineParts[i].trim().isEmpty()) {
                  result.put(lineParts[i].trim(), lineParts[0].trim());
               }
            }
         }
      } catch (IOException e) {
         // Should not happen but in any case...
         return null;
      }
      return result.get(extension);
   }
}
