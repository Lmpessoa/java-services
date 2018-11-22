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

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Objects;

public final class XmlComment extends XmlElement {

   private final String value;

   public XmlComment(String value) {
      this.value = Objects.requireNonNull(value);
   }

   public String getValue() {
      return value;
   }

   @Override
   protected String buildXmlAtLevel(int indentLevel) {
      StringBuilder result = new StringBuilder();
      String[] lines = new BufferedReader(new StringReader(value)).lines().toArray(String[]::new);
      result.append(indentForLevel(indentLevel));
      result.append("<!--");
      if (lines.length <= 1) {
         result.append(' ');
         result.append(String.join("", lines));
         result.append(' ');
      } else {
         result.append("\n");
         for (String line : lines) {
            result.append(indentForLevel(indentLevel + 1));
            result.append(line);
            result.append("\n");
         }
         result.append(indentForLevel(indentLevel));
      }
      result.append("-->");
      return result.toString();
   }

}
