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
package com.lmpessoa.services.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.lmpessoa.services.util.Property;

public class PropertyBuilder {

   private final Map<String, String> values = new HashMap<>();

   public PropertyBuilder set(String property, String value) {
      values.put(property, value);
      return this;
   }

   public Property build() {
      return Property.fromString(String.format("{%s}", process(values)));
   }

   private String process(Map<String, String> source) {
      Map<String, Map<String, String>> group = new HashMap<>();
      StringBuilder result = new StringBuilder();
      for (Entry<String, String> entry : source.entrySet()) {
         String[] parts = entry.getKey().split("\\.", 2);
         if (parts.length == 1) {
            result.append('"');
            result.append(entry.getKey());
            result.append("\":\"");
            result.append(entry.getValue());
            result.append("\",");
            continue;
         }
         if (!group.containsKey(parts[0])) {
            group.put(parts[0], new HashMap<>());
         }
         Map<String, String> part = group.get(parts[0]);
         part.put(parts[1], entry.getValue());
      }
      if (!group.isEmpty()) {
         for (Entry<String, Map<String, String>> entry : group.entrySet()) {
            result.append('"');
            result.append(entry.getKey());
            result.append("\":{");
            result.append(process(entry.getValue()));
            result.append("},");
         }
      }
      return result.toString().substring(0, result.length() - 1);
   }
}
