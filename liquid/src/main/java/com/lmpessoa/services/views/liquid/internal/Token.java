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
package com.lmpessoa.services.views.liquid.internal;

import java.util.Arrays;

import com.lmpessoa.services.views.liquid.parsing.Symbol;

final class Token {

   private final TokenType type;

   private final String value;
   private final int pos;

   Token(TokenType type, int pos, String value) {
      this.value = value;
      this.type = type;
      this.pos = pos;
   }

   int position() {
      return pos;
   }

   int length() {
      return value.length();
   }

   public String value() {
      return value;
   }

   @Override
   public String toString() {
      return String.format("%s('%s')", type, value);
   }

   public boolean isLiteral() {
      return this.type == TokenType.LITERAL;
   }

   public boolean isTagStart() {
      return isSymbolEither(Symbol.TAG_START, Symbol.TAG_START_TRIM);
   }

   public boolean isTagEnd() {
      return isSymbolEither(Symbol.TAG_END, Symbol.TAG_END_TRIM);
   }

   public boolean isObjectStart() {
      return isSymbolEither(Symbol.OBJECT_START, Symbol.OBJECT_START_TRIM);
   }

   public boolean isObjectEnd() {
      return isSymbolEither(Symbol.OBJECT_END, Symbol.OBJECT_END_TRIM);
   }

   public boolean isConstant() {
      return this.type == TokenType.CONSTANT;
   }

   public boolean isString() {
      return this.type == TokenType.STRING;
   }

   public boolean isNumber() {
      return this.type == TokenType.NUMBER;
   }

   public boolean isSymbol() {
      return this.type == TokenType.SYMBOL;
   }

   public boolean isSymbol(Symbol value) {
      return this.type == TokenType.SYMBOL && value.value().equals(this.value);
   }

   public boolean isSymbolEither(Symbol... values) {
      return this.type == TokenType.SYMBOL
               && Arrays.stream(values).map(Symbol::value).anyMatch(s -> s.equals(this.value));
   }

   public boolean isIdentifier() {
      return this.type == TokenType.IDENTIFIER;
   }

   public boolean isInvalid() {
      return this.type == TokenType.INVALID;
   }
}
