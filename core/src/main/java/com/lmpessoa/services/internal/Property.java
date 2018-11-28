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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Properties are structured as a tree of name/value pairs. Some of these values will be another
 * property while others will be simple values like a string or integer.
 * <p>
 * A root property may be created from the contents of a file or a string. The format of such
 * content might be either YAML or JSON.
 * </p>
 */
public final class Property {

   /**
    * Represents an empty property. An empty property neither has a single value nor children thus
    * represents no value at all.
    */
   public static final Property EMPTY = new Property(null, (String) null);

   private final String name;

   final PropertyValue value;

   /**
    * Creates a new {@code Property} by parsing the contents in the given file.
    *
    * @param file the file containing the property values
    * @return the property from the contents of the given file or {#code null} if the content is not
    *         valid.
    * @throws IOException if there is an error while reading from the file.
    */
   public static Property fromFile(File file) throws IOException {
      if (!file.isFile()) {
         throw new IllegalArgumentException(ErrorMessage.INVALID_FILE.get());
      }
      final List<String> lines = Files.readAllLines(file.toPath());
      String content = String.join("\n", lines.stream().toArray(String[]::new));
      return fromString(content);
   }

   /**
    * Creates a new {@code Property} by parsing the contents in the file with given name.
    *
    * @param filename the name of the file containing the property values.
    * @return the property from the contents of the given file or {#code null} if the content is not
    *         valid.
    * @throws IOException if there is an error while reading from the file.
    */
   public static Property fromFile(String filename) throws IOException {
      return fromFile(new File(filename));
   }

   /**
    * Creates a new {@code Property} by parsing the contents of the given string.
    *
    * @param content the string containing the property values.
    * @return the property from the given string or {@code null} if the content is not valid.
    */
   public static Property fromString(String content) {
      Property result = tryParseYamlContent(content);
      if (result == null) {
         result = tryParseJsonContent(content);
      }
      return result;
   }

   /**
    * Returns the name of this property.
    * <p>
    * Depending on the format and declaration of properties, they may bear an unique identifier
    * related to its parent property. This identifier may be a string name or an integer name as a
    * string if they came from a nameless list.
    * </p>
    *
    * @return the name of this property.
    */
   public String getName() {
      return name;
   }

   /**
    * Returns the children of this property with the given name.
    * <p>
    * Child property names may be composed to navigate through multiple children at once. Such names
    * must be separated by dots. If the final property does not exists, an empty property will be
    * returned instead.
    * </p>
    *
    * @param propertyName the name of the child property to retrieve.
    * @return the child property with the given name or an empty property.
    */
   public Property get(String propertyName) {
      String[] names = propertyName.split("\\.", 2);
      Property result = value.get(names[0]);
      if (names.length > 1 && result != null) {
         result = result.get(names[1]);
      }
      return result;
   }

   /**
    * Returns the value of this property.
    *
    * @return the value of this property or {#code null} if this property has no single value.
    */
   public String getValue() {
      return value.get();
   }

   /**
    * Returns the value of this property or a default value, if the property is empty or not a
    * single value property.
    *
    * @param defaultValue a default value to be returned if the property does not bear a value.
    * @return the value of this property or the given default value.
    */
   public String getValueOrDefault(String defaultValue) {
      String result = value.get();
      return result == null ? defaultValue : result;
   }

   /**
    * Returns the integer value of this property.
    * <p>
    * Integer value of properties may be defined as hexadecimal values (prefixed with "0x"), binary
    * values (prefixed with "0b") or octal values (prefixed with "0"). Otherwise values are treated
    * as regular decimal values.
    * </p>
    *
    * @return the value of this property as an integer.
    */
   public int getIntValue() {
      String result = value.get();
      if (result != null) {
         if (result.startsWith("0x")) {
            return Integer.parseInt(result.substring(2), 16);
         } else if (result.startsWith("0b")) {
            return Integer.parseInt(result.substring(2), 2);
         } else if (result.startsWith("0")) {
            return Integer.parseInt(result.substring(1), 8);
         }
      }
      return Integer.parseInt(result);
   }

   /**
    * Returns the value of this property converted to an integer or a default value, if the value is
    * not a valid integer value, the property is empty, or if the property is not a single value
    * (and thus has children).
    *
    * @param defaultValue a default value to be returned if the property does not bear a valid
    *           integer value.
    * @return the value of this property as an integer or the given default value.
    */
   public int getIntValueOrDefault(int defaultValue) {
      try {
         return getIntValue();
      } catch (Exception e) {
         return defaultValue;
      }
   }

   /**
    * Returns the value of this property converted to a boolean.
    * <p>
    * Note that this method will only return whether the value of this property matches any of the
    * valid string values to be considered true. Otherwise, this method will return false.
    * </p>
    *
    * @return the value of this property as a boolean.
    */
   public boolean getBoolValue() {
      String result = this.value.get();
      return result.equalsIgnoreCase("true") || result.equalsIgnoreCase("yes");
   }

   /**
    * Returns the value of this property converted to a boolean or a default value, if the value is
    * not a valid boolean value, the property is empty, or if the property is not a single value
    * (and thus has children).
    *
    * @param defaultValue a default value to be returned if the property does not bear a valid
    *           boolean value.
    * @return the value of this property as a boolean or the given default value.
    */
   public boolean getBoolValueOrDefault(boolean defaultValue) {
      String result = value.get();
      if ("true".equalsIgnoreCase(result) || "false".equalsIgnoreCase(result)) {
         return "true".equals(result);
      }
      if ("yes".equalsIgnoreCase(result) || "no".equalsIgnoreCase(result)) {
         return "yes".equals(result);
      }
      return defaultValue;
   }

   /**
    * Returns a collection of the child properties of this property.
    * <p>
    * Properties are structured as a tree of properties. If a property has children, this method
    * will return each children immediate children of this property. For properties which do not
    * have children (see {@link #hasChildren()}), this method will return an empty list.
    * </p>
    *
    * @return a collection of the child properties of this property.
    */
   public Collection<Property> values() {
      return value.values();
   }

   /**
    * Returns whether this property has children properties.
    * <p>
    * Properties are structured as a tree of properties. This method will indicate whether this
    * property is a leaf and has no child properties or if it is a node with possible multiple
    * children.
    * </p>
    *
    * @return {@code true} if this property has children, {@code false} otherwise.
    */
   public boolean hasChildren() {
      return value instanceof MapPropertyValue;
   }

   /**
    * Returns whether this property is empty.
    * <p>
    * An empty property contains no value that may be read or children properties. It is most likely
    * that an empty property does not actually occur in its parent.
    * </p>
    *
    * @return {@code true} if this property is empty, {@code false} otherwise.
    */
   public boolean isEmpty() {
      return value.isEmpty();
   }

   @Override
   public String toString() {
      return value.toString();
   }

   Property(String name, String value) {
      this.value = new StringPropertyValue(value);
      this.name = name;
   }

   Property(String name, Map<String, Property> values) {
      this.value = new MapPropertyValue(values);
      this.name = name;
   }

   private static Property tryParseYamlContent(String content) {
      YamlReader yamlReader = new YamlReader(content);
      Object result;
      try {
         result = yamlReader.read();
      } catch (YamlException e) { // NOSONAR
         return null;
      }
      return createPropertyFromYaml("", result);
   }

   private static Property createPropertyFromYaml(String name, Object object) {
      if (object == null) {
         return null;
      } else if (object instanceof String) {
         return new Property(name, (String) object);
      } else if (object instanceof HashMap) {
         Map<?, ?> map = (Map<?, ?>) object;
         Map<String, Property> result = new HashMap<>();
         for (Entry<?, ?> entry : map.entrySet()) {
            Property p = createPropertyFromYaml(entry.getKey().toString(), entry.getValue());
            if (p != null) {
               result.put(p.getName(), p);
            }
         }
         return new Property(name, result);
      } else if (object instanceof ArrayList) {
         List<?> list = (List<?>) object;
         Map<String, Property> result = new HashMap<>();
         for (int i = 0; i < list.size(); ++i) {
            Property p = createPropertyFromYaml(String.valueOf(i), list.get(i));
            if (p != null) {
               result.put(p.getName(), p);
            }
         }
         return new Property(name, result);
      }
      return null;
   }

   private static Property tryParseJsonContent(String content) {
      JsonElement result;
      try {
         result = new JsonParser().parse(content);
      } catch (JsonParseException e) {
         return null;
      }
      return createPropertyFromJson("", result);
   }

   private static Property createPropertyFromJson(String name, JsonElement element) {
      if (element.isJsonNull()) {
         return null;
      } else if (element.isJsonPrimitive()) {
         return new Property(name, element.getAsString());
      } else if (element.isJsonObject()) {
         JsonObject object = element.getAsJsonObject();
         Map<String, Property> result = new HashMap<>();
         for (Entry<String, JsonElement> entry : object.entrySet()) {
            Property p = createPropertyFromJson(entry.getKey(), entry.getValue());
            if (p != null) {
               result.put(p.getName(), p);
            }
         }
         return new Property(name, result);
      } else if (element.isJsonArray()) {
         JsonArray array = element.getAsJsonArray();
         Map<String, Property> result = new HashMap<>();
         for (int i = 0; i < array.size(); ++i) {
            Property p = createPropertyFromJson(String.valueOf(i), array.get(i));
            if (p != null) {
               result.put(p.getName(), p);
            }
         }
         return new Property(name, result);
      }
      return null;
   }

   private static String escapeString(String value) {
      return String.format("\"%s\"", value.replaceAll("\"", "\\\""));
   }

   private static interface PropertyValue {

      default Property get(String key) {
         return Property.EMPTY;
      }

      default String get() {
         return null;
      }

      default Collection<Property> values() {
         return Collections.emptyList();
      }

      default boolean isEmpty() {
         return true;
      }
   }

   private static class StringPropertyValue implements PropertyValue {

      private final String value;

      public StringPropertyValue(String value) {
         this.value = value;
      }

      @Override
      public String get() {
         return value;
      }

      @Override
      public boolean isEmpty() {
         return value != null;
      }

      @Override
      public String toString() {
         return escapeString(value);
      }
   }

   private static class MapPropertyValue implements PropertyValue {

      private final Map<String, Property> values;

      public MapPropertyValue(Map<String, Property> values) {
         this.values = values;
      }

      @Override
      public Property get(String key) {
         return values.containsKey(key) ? values.get(key) : Property.EMPTY;
      }

      @Override
      public Collection<Property> values() {
         return values.values();
      }

      @Override
      public boolean isEmpty() {
         return values.isEmpty();
      }

      @Override
      public String toString() {
         StringBuilder result = new StringBuilder();
         result.append('{');
         for (Entry<String, Property> entry : values.entrySet()) {
            result.append(escapeString(entry.getKey()));
            result.append(':');
            result.append(entry.getValue());
            result.append(',');
         }
         result.deleteCharAt(result.length() - 1);
         result.append('}');
         return result.toString();
      }
   }
}
