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
package com.lmpessoa.services.internal.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.lmpessoa.services.internal.ErrorMessage;
import com.lmpessoa.services.routing.IRouteOptions;

final class RouteOptions implements IRouteOptions {

   private final Map<String, Pattern> packages = new HashMap<>();
   private final Map<String, String> indices = new HashMap<>();

   RouteOptions() {
      addArea("", ".resources$");
   }

   @Override
   public void addArea(String areaPath, String packageExpr, String defaultResource) {
      if (!Pattern.matches("^([a-zA-Z0-9]+(\\/[a-zA-Z0-9]+)?)?$", areaPath)) {
         throw new IllegalArgumentException(ErrorMessage.INVALID_AREA_PATH.with(areaPath));
      }
      String packExpr = packageExpr.replaceAll("\\.", "\\\\.");
      packages.put(areaPath, Pattern.compile(packExpr));
      String defRes = defaultResource.toLowerCase();
      if (!Pattern.matches("^[a-z][a-z0-9]*$", defRes)) {
         throw new IllegalArgumentException(ErrorMessage.INVALID_DEFAULT_RESOURCE.with(defRes));
      }
      if (!"index".equals(defRes)) {
         indices.put(areaPath, defRes);
      }
   }

   String findArea(Class<?> clazz) {
      if (clazz.isPrimitive() || clazz.isArray()) {
         return null;
      }
      return findArea(clazz.getPackage().getName());
   }

   String findArea(String packageName) {
      for (Entry<String, Pattern> entry : packages.entrySet()) {
         if (entry.getValue().matcher(packageName).find()) {
            return entry.getKey();
         }
      }
      return null;
   }

   String getAreaIndex(String area) {
      return indices.getOrDefault(area, "index");
   }
}
