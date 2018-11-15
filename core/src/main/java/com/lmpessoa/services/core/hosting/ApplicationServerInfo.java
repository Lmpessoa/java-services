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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

final class ApplicationServerInfo {

   private final Map<String, String> properties;

   public Optional<String> getProperty(String name) {
      return Optional.ofNullable(properties.get(name));
   }

   public Map<String, String> getProperties(String name) {
      Map<String, String> result = new HashMap<>();
      for (Entry<String, String> entry : properties.entrySet()) {
         if (entry.getKey().startsWith(name + '.')) {
            result.put(entry.getKey().substring(name.length() + 1), entry.getValue());
         }
      }
      return result;
   }

   public Optional<Integer> getIntProperty(String name) {
      String value = properties.get(name);
      if (value != null && value.matches("\\d+")) {
         return Optional.of(Integer.parseInt(value));
      }
      return Optional.empty();
   }

   ApplicationServerInfo(Class<?> startupClass) {
      File location = findLocation(startupClass);
      this.properties = Collections.unmodifiableMap(getConfigInfo(location));
   }

   ApplicationServerInfo(String type, Reader reader) throws IOException {
      this.properties = Collections.unmodifiableMap(parseConfigMapFile(type, reader));
   }

   static Map<String, String> parseConfigMapFile(String type, Reader reader) throws IOException {
      switch (type) {
         case "YML":
            YamlReader yamlReader = new YamlReader(reader);
            return flattenMap(null, (Map<?, ?>) yamlReader.read());
         case "JSON":
            Type typeToken = new TypeToken<Map<Object, Object>>() {}.getType();
            return flattenMap(null, new Gson().fromJson(reader, typeToken));
         case "PROPERTIES":
            Properties props = new Properties();
            props.load(reader);
            Map<String, String> result = new HashMap<>();
            for (Entry<?, ?> entry : props.entrySet()) {
               result.put(entry.getKey().toString(), entry.getValue().toString());
            }
            return result;
         default:
            throw new IllegalArgumentException("Unknown configuration file type: " + type);
      }
   }

   private static File findLocation(Class<?> startupClass) {
      String pathOfClass = File.separator + startupClass.getName().replaceAll("\\.", File.separator) + ".class";
      String fullPathOfClass = startupClass.getResource(startupClass.getSimpleName() + ".class").toString();
      String result = fullPathOfClass.substring(0, fullPathOfClass.length() - pathOfClass.length());
      if (result.startsWith("jar:")) {
         int lastSep = result.lastIndexOf(File.separator);
         result = result.substring(4, lastSep);
      }
      if (result.startsWith("file:")) {
         result = result.substring(5);
         while (result.startsWith(File.separator + File.separator)) {
            result = result.substring(1);
         }
         String sep = File.separator;
         if (sep.equals("\\")) {
            sep = "\\\\";
         }
         if (result.matches(String.format("%s[a-zA-Z]:%s.*", sep, sep))) {
            result = result.substring(1).replaceAll("/", "\\");
         }
      }
      if (result.matches("[a-zA-Z0-9]+:" + File.separator + ".*")) {
         return null;
      }
      return new File(result);
   }

   private static Map<String, String> getConfigInfo(File appRoot) {
      try {
         File result = new File(appRoot, "settings.yml");
         if (!result.exists()) {
            result = new File(appRoot, "settings.json");
         }
         if (!result.exists()) {
            result = new File(appRoot, "application.properties");
         }
         if (result.exists()) {
            String type = result.getName().substring(result.getName().lastIndexOf('.') + 1).toUpperCase();
            try (FileReader reader = new FileReader(result)) {
               return parseConfigMapFile(type, reader);
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
      return Collections.emptyMap();
   }

   private static Map<String, String> flattenMap(String parentName, Map<?, ?> map) {
      Map<String, String> result = new HashMap<>();
      for (Entry<?, ?> entry : map.entrySet()) {
         String thisName;
         if (parentName != null && !parentName.isEmpty()) {
            thisName = String.format("%s.%s", parentName, entry.getKey());
         } else {
            thisName = entry.getKey().toString();
         }
         if (entry.getValue() instanceof Map<?, ?>) {
            Map<String, String> submap = flattenMap(thisName, (Map<?, ?>) entry.getValue());
            result.putAll(submap);
         } else if (entry.getValue() instanceof List<?>) {
            Map<String, String> submap = flattenList(thisName, (List<?>) entry.getValue());
            result.putAll(submap);
         } else {
            result.put(thisName, entry.getValue().toString());
         }
      }
      return result;
   }

   private static Map<String, String> flattenList(String parentName, List<?> list) {
      Map<String, String> result = new HashMap<>();
      for (int i = 0; i < list.size(); ++i) {
         String thisName = String.format("%s.%d", parentName, i);
         Object entry = list.get(i);
         if (entry instanceof Map<?, ?>) {
            Map<String, String> submap = flattenMap(thisName, (Map<?, ?>) entry);
            result.putAll(submap);
         } else if (entry instanceof List<?>) {
            Map<String, String> submap = flattenList(thisName, (List<?>) entry);
            result.putAll(submap);
         } else {
            result.put(thisName, entry.toString());
         }
      }
      return result;
   }
}
