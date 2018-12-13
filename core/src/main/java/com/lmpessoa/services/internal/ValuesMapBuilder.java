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
package com.lmpessoa.services.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.lmpessoa.services.hosting.ValuesMap;

public class ValuesMapBuilder {

   private Map<String, List<String>> values = new ConcurrentHashMap<>();

   public ValuesMapBuilder add(String key, String value) {
      if (!values.containsKey(key)) {
         values.put(key, new ArrayList<>());
      }
      values.get(key).add(value);
      return this;
   }

   public ValuesMap build() {
      final Map<String, List<String>> values = Collections.unmodifiableMap(this.values);
      return new ValuesMap() {

         @Override
         public boolean contains(String key) {
            return values.containsKey(key);
         }

         @Override
         public String get(String key) {
            if (values.containsKey(key)) {
               return values.get(key).get(0);
            }
            return null;
         }

         @Override
         public String[] getAll(String key) {
            if (values.containsKey(key)) {
               return values.get(key).toArray(new String[0]);
            }
            return null;
         }

         @Override
         public Set<String> keySet() {
            return values.keySet();
         }

      };
   }
}
