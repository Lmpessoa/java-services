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
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lmpessoa.services.views.liquid.parsing.Symbol;

final class LiquidTokenizer implements Iterator<Token> {

   private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");
   private static final Pattern STARTERS = Pattern.compile("\\{+[\\{\\%]-?");
   private static final List<String> RAW_TAGS = Arrays.asList("raw", "comment");

   private final String template;
   private final Matcher matcher;

   private Predicate<Token> closer = null;
   private String raw_tag = null;
   private int pos = 0;

   @Override
   public String toString() {
      return template;
   }

   @Override
   public boolean hasNext() {
      return pos < template.length();
   }

   @Override
   public Token next() {
      Token result = closer == null ? nextTextToken() : nextLiquidToken();
      if (closer != null && closer.test(result)) {
         closer = null;
      }
      return result;
   }

   public Token nextLiteralUntilTag(String tagName) {
      int s0 = pos;
      while (matcher.find(pos)) {
         Token token = new Token(TokenType.SYMBOL, s0, matcher.group());
         final int start = matcher.start();
         pos = matcher.end();
         if (!token.isTagStart()) {
            continue;
         }
         token = nextLiquidToken();
         if (token == null) {
            pos = template.length();
            return new Token(TokenType.LITERAL, s0, template.substring(s0));
         }
         if (!tagName.equals(token.value())) {
            continue;
         }
         pos = start;
         return new Token(TokenType.LITERAL, s0, template.substring(s0, start));
      }
      return null;
   }

   public Token peek() {
      int s0 = pos;
      Token result = closer == null ? nextTextToken() : nextLiquidToken();
      pos = s0;
      return result;
   }

   LiquidTokenizer(String template) {
      template = template.replace("\r\n", "\n");
      this.matcher = STARTERS.matcher(template);
      this.template = template;
   }

   private Token nextTextToken() {
      final int s0 = pos;
      if (raw_tag != null) {
         Pattern end = Pattern.compile("\\{%-?\\s*end" + raw_tag + "\\s*[^%-]*-?%}");
         Matcher m = end.matcher(template);
         if (m.find(pos)) {
            pos = m.start();
            raw_tag = null;
            return new Token(TokenType.LITERAL, s0, template.substring(s0, pos));
         } else {
            return new Token(TokenType.LITERAL, s0, template.substring(s0));
         }
      }
      while (matcher.find(pos)) {
         if (pos == matcher.start()) {
            Token result = new Token(TokenType.SYMBOL, pos, matcher.group());
            pos = matcher.end();
            if (!result.isObjectStart() && !result.isTagStart()) {
               continue;
            } else if (result.isTagStart()) {
               final int s1 = pos;
               Token token = nextLiquidToken();
               pos = s1;
               if (token != null && token.isIdentifier() && RAW_TAGS.contains(token.value())) {
                  raw_tag = token.value();
               }
            }
            closer = getCloserFunction(result.value());
            return result;
         } else {
            pos = matcher.start();
            return new Token(TokenType.LITERAL, s0, template.substring(s0, pos));
         }
      }
      pos = template.length();
      return new Token(TokenType.LITERAL, pos, template.substring(s0));
   }

   private Predicate<Token> getCloserFunction(String value) {
      if ("{{".equals(value) || "{{-".equals(value)) {
         return t -> t.isObjectEnd();
      } else if ("{%".equals(value) || "{%-".equals(value)) {
         return t -> t.isTagEnd();
      }
      throw new IllegalArgumentException(value);
   }

   private Token nextLiquidToken() {
      while (" \t\r\n".indexOf(template.charAt(pos)) >= 0) {
         pos += 1;
      }
      Map<TokenType, Supplier<String>> map = new EnumMap<>(TokenType.class);
      map.put(TokenType.CONSTANT, this::nextConstantToken);
      map.put(TokenType.STRING, this::nextStringToken);
      map.put(TokenType.NUMBER, this::nextNumberToken);
      map.put(TokenType.SYMBOL, this::nextSymbolToken);
      map.put(TokenType.IDENTIFIER, this::nextIdentifierToken);
      map.put(TokenType.INVALID, this::nextInvalidToken);
      TokenType type = null;
      String token = null;
      final int s0 = pos;
      for (Entry<TokenType, Supplier<String>> entry : map.entrySet()) {
         token = entry.getValue().get();
         if (token != null) {
            type = entry.getKey();
            break;
         }
      }
      Token result = new Token(type, s0, token);
      return result;
   }

   private String nextConstantToken() {
      String result = null;
      final String part = template.substring(pos, Math.min(pos + 5, template.length()))
               .toLowerCase();
      if (part.startsWith("true")) {
         result = "true";
      } else if (part.startsWith("false")) {
         result = "false";
      } else if (part.startsWith("nil")) {
         result = "nil";
      }
      if (result != null) {
         if ("!@#$%^&*()_+-=[]\\{|;:'\",./<>? \t\n"
                  .indexOf(template.charAt(pos + result.length())) >= 0) {
            pos += result.length();
         } else {
            result = null;
         }
      }
      return result;
   }

   private String nextStringToken() {
      char ch = template.charAt(pos);
      if (ch != '"' && ch != '\'') {
         return null;
      }
      int strEnd = template.indexOf(ch, pos + 1) + 1;
      if (strEnd == -1) {
         strEnd = template.length();
      }
      int lineEnd = template.indexOf("\n", pos + 1);
      if (lineEnd == -1) {
         lineEnd = template.length();
      }
      int end = Math.min(strEnd, lineEnd);
      String result = template.substring(pos, end);
      pos += result.length();
      return result;
   }

   private String nextNumberToken() {
      final int s0 = pos;
      StringBuilder sb = new StringBuilder();
      char ch = template.charAt(pos);
      if (ch == '-') {
         sb.append(ch);
         pos += 1;
         ch = template.charAt(pos);
      }
      while ("0123456789".indexOf(ch) >= 0) {
         sb.append(ch);
         pos += 1;
         ch = template.charAt(pos);
      }
      if (sb.length() > 0 && ch == '.') {
         final int s1 = pos;
         StringBuilder frac = new StringBuilder();
         frac.append(ch);
         pos += 1;
         ch = template.charAt(pos);
         while ("0123456789".indexOf(ch) >= 0) {
            frac.append(ch);
            pos += 1;
            ch = template.charAt(pos);
         }
         if (frac.length() > 1) {
            sb.append(frac);
         } else {
            pos = s1;
         }
      }
      String result = sb.toString();
      if (sb.length() == 0 || result.startsWith("-.") || result.startsWith(".")
               || result.endsWith(".") || "!@#$%^&*()_+-=[]\\{|;:'\",./<>? \t\n".indexOf(ch) < 0) {
         pos = s0;
         return null;
      }
      return result;
   }

   private String nextSymbolToken() {
      for (Symbol s : Symbol.values()) {
         String symbol = s.value();
         if (template.startsWith(symbol, pos)) {
            char ch = pos + symbol.length() >= template.length() ? ' '
                     : Character.toLowerCase(template.charAt(pos + symbol.length()));
            if (s.isSpecial()
                     || "-0123456789abcdefghijklmnopqrstuvwxyz_%}'\" \t\n".indexOf(ch) >= 0) {
               pos += symbol.length();
               return symbol;
            }
         }
      }
      return null;
   }

   private String nextIdentifierToken() {
      Matcher m = IDENTIFIER.matcher(template);
      if (m.find(pos) && m.start() == pos) {
         String result = m.group();
         char ch = template.charAt(pos + result.length());
         if ("=!<>.,[:| \t\n".indexOf(ch) >= 0) {
            pos += result.length();
            return result;
         }
      }
      return null;
   }

   private String nextInvalidToken() {
      int end = Arrays.stream(new String[] { " ", "\t", "\n" }) //
               .mapToInt(s -> template.indexOf(s, pos))
               .filter(s -> s >= 0)
               .min()
               .orElse(template.length());
      String result = template.substring(pos, end);
      pos += result.length();
      return result;
   }
}
