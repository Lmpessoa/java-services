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
package com.lmpessoa.services.views.liquid.parsing;

public enum Symbol {
   // This list should be kept in decrescent size order
   TAG_START_TRIM("{%-"), TAG_END_TRIM("-%}"), OBJECT_END_TRIM("-}}"), OBJECT_START_TRIM("{{-"),
   //
   TAG_START("{%"), TAG_END("%}"), OBJECT_START("{{"), OBJECT_END("}}"), EQUALS("=="),
   NOT_EQUALS("!="), DIFFERENT("<>"), LESS_OR_EQUALS("<="), GREATER_OR_EQUALS(">="), RANGE(".."),
   //
   LESS_THAN("<"), GREATER_THAN(">"), ASSIGN("="), COLON(":"), COMMA(","), PIPE("|"), DOT("."),
   OPEN_PARENS("("), CLOSE_PARENS(")"), OPEN_BRACES("["), CLOSE_BRACES("]");

   private final String value;

   private Symbol(String value) {
      this.value = value;
   }

   public String value() {
      return value;
   }

   public boolean isSpecial() {
      String name = this.name();
      return name.startsWith("TAG_") || name.startsWith("OBJECT_");
   }

   public static Symbol of(String value) {
      for (Symbol s : values()) {
         if (value.equals(s.value)) {
            return s;
         }
      }
      return null;
   }
}
