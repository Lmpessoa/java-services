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
package com.lmpessoa.services.hosting;

import java.util.Collections;
import java.util.Set;

/**
 * Represents headers in an HTTP request.
 */
public interface ValuesMap {

   /**
    * Returns whether this map contains any value associated with the given header name.
    *
    * @param key the name of the key to test for the presence of values.
    * @return {@code true} if this map has values associated with the given key, or {@code false}
    *         otherwise.
    */
   boolean contains(String key);

   /**
    * Returns the first value associated with the given key on this map.
    *
    * @param key the name of the key to retrieve the value for.
    * @return the first value associated with the given key, or {@code null} if no such value
    *         exists.
    */
   String get(String key);

   /**
    * Returns any values associated with the given key name on this map.
    *
    * @param key the name of the header to retrieve the value for.
    * @return the values associates with the given key, or an empty array if no such values exist.
    */
   String[] getAll(String key);

   /**
    * Returns a set of the keys present in this map.
    *
    * @return a set of the keys present in this map.
    */
   Set<String> keySet();

   static ValuesMap empty() {
      return new ValuesMap() {

         @Override
         public boolean contains(String key) {
            return false;
         }

         @Override
         public String get(String key) {
            return null;
         }

         @Override
         public String[] getAll(String key) {
            return null;
         }

         @Override
         public Set<String> keySet() {
            return Collections.emptySet();
         }

      };
   }
}
