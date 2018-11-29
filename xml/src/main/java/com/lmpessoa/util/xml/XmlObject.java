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
package com.lmpessoa.util.xml;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public final class XmlObject extends XmlElement {

   private final Map<String, XmlPrimitive> attrs = new LinkedHashMap<>();
   private final String name;

   private XmlElement value;

   public XmlObject(String name) {
      this.name = requireValidXmlName(name);
   }

   public String getName() {
      return name;
   }

   public void addAttribute(String name, XmlPrimitive value) {
      attrs.put(requireValidXmlName(name), Objects.requireNonNull(value));
   }

   public void addAttribute(String name, Boolean value) {
      attrs.put(requireValidXmlName(name), new XmlPrimitive(value));
   }

   public void addAttribute(String name, Character value) {
      attrs.put(requireValidXmlName(name), new XmlPrimitive(value));
   }

   public void addAttribute(String name, Number value) {
      attrs.put(requireValidXmlName(name), new XmlPrimitive(value));
   }

   public void addAttribute(String name, String value) {
      attrs.put(requireValidXmlName(name), new XmlPrimitive(value));
   }

   public Object getAttribute(String name) {
      return attrs.get(name);
   }

   public int getAttributeCount() {
      return attrs.size();
   }

   public Object removeAttribute(String name) {
      return attrs.remove(name);
   }

   public void clearAttributes() {
      attrs.clear();
   }

   public Map<String, Object> attributes() {
      return Collections.unmodifiableMap(attrs);
   }

   public void clear() {
      attrs.clear();
      value = null;
   }

   public void setValue(XmlElement value) {
      this.value = Objects.requireNonNull(value);
   }

   public XmlElement getValue() {
      return value;
   }

   @Override
   protected String buildXmlAtLevel(int indentLevel) {
      StringBuilder result = new StringBuilder();
      result.append(indentForLevel(indentLevel));
      result.append('<');
      result.append(name);
      if (!attrs.isEmpty()) {
         for (Entry<String, XmlPrimitive> attr : attrs.entrySet()) {
            result.append(' ');
            result.append(attr.getKey());
            result.append("=\"");
            result.append(attr.getValue().getAsString());
            result.append('"');
         }
      }
      if (value == null) {
         result.append("/>");
      } else {
         result.append('>');
         if (value instanceof XmlPrimitive) {
            result.append(((XmlPrimitive) value).getAsString());
         } else {
            result.append("\n");
            result.append(value.buildXmlAtLevel(indentLevel + 1));
            result.append("\n");
            result.append(indentForLevel(indentLevel));
         }
         result.append("</");
         result.append(name);
         result.append('>');
      }
      return result.toString();
   }

   private static final String NAME_START_CHAR = "[:A-Z_a-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD\u10000-\uEFFFF]";
   private static final String NAME_CHAR = NAME_START_CHAR
            + "[-.0-9\u00B7\u0300-\u036F\u203F-\u2040]";
   private static final String NAME = NAME_START_CHAR + "(?:" + NAME_CHAR + ")*";

   private static String requireValidXmlName(String name) {
      Objects.requireNonNull(name);
      if (!name.matches(NAME)) {
         throw new IllegalArgumentException("Not a valid XML name");
      }
      return name;
   }
}
