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
package com.lmpessoa.services.internal.hosting;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.lmpessoa.services.hosting.HeaderMap;

public final class HeaderMapImpl implements HeaderMap {

   private final Map<String, List<String>> headers;

   @Override
   public boolean contains(String headerName) {
      return headers.containsKey(headerName);
   }

   @Override
   public String get(String headerName) {
      List<String> values = headers.get(headerName);
      if (values != null && !values.isEmpty()) {
         return values.get(0);
      }
      return null;
   }

   @Override
   public String[] getAll(String headerName) {
      List<String> values = headers.get(headerName);
      String[] result = new String[0];
      if (values != null && !values.isEmpty()) {
         result = values.toArray(result);
      }
      return result;
   }

   @Override
   public Set<String> nameSet() {
      return headers.keySet();
   }

   public HeaderMapImpl(Map<String, List<String>> headers) {
      this.headers = Objects.requireNonNull(headers);
   }
}
