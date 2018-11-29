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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.lmpessoa.services.views.liquid.LiquidEngine;
import com.lmpessoa.services.views.liquid.parsing.BlockTagElement;
import com.lmpessoa.services.views.liquid.parsing.BlockTagElementBuilder;
import com.lmpessoa.services.views.liquid.parsing.BreakTagElement;
import com.lmpessoa.services.views.liquid.parsing.Element;
import com.lmpessoa.services.views.liquid.parsing.Expression;
import com.lmpessoa.services.views.liquid.parsing.LiquidParseContext;
import com.lmpessoa.services.views.liquid.parsing.Literal;
import com.lmpessoa.services.views.liquid.parsing.NumberLiteral;
import com.lmpessoa.services.views.liquid.parsing.StringLiteral;
import com.lmpessoa.services.views.liquid.parsing.Symbol;
import com.lmpessoa.services.views.liquid.parsing.TagElement;
import com.lmpessoa.services.views.liquid.parsing.TagElementBuilder;
import com.lmpessoa.services.views.liquid.parsing.Value;
import com.lmpessoa.services.views.liquid.parsing.Variable;
import com.lmpessoa.services.views.templating.TemplateParseException;
import com.lmpessoa.services.views.templating.TemplateParseException.ParseError;

public final class LiquidParser implements LiquidParseContext {

   private final Map<TagElementBuilder, Token> tokens = new HashMap<>();
   private final Stack<Collection<Element>> blocks = new Stack<>();
   private final Stack<BlockTagElementBuilder> tags = new Stack<>();
   private final Collection<ParseError> errors = new ArrayList<>();
   private final LiquidTokenizer template;
   private final LiquidEngine engine;
   private final String filename;

   public static Object[] arrayOf(Object value) {
      if (value == null) {
         return new Object[0];
      } else if (value.getClass().isArray()) {
         return (Object[]) value;
      } else if (value instanceof Collection<?>) {
         return ((Collection<?>) value).toArray();
      } else if (value instanceof Map<?, ?>) {
         return ((Map<?, ?>) value).entrySet()
                  .stream()
                  .map(e -> new Object[] { e.getKey(), e.getValue() })
                  .toArray();
      }
      return new Object[0];
   }

   public static Element parse(LiquidEngine engine, String filename, String template)
      throws TemplateParseException {
      Collection<Element> result = new LiquidParser(engine, filename, template).parse();
      return new ElementBlock(result.toArray(new Element[0]));
   }

   @Override
   public Expression readExpression(boolean optional) {
      Value left = readValue(optional);
      if (left == null) {
         return null;
      }
      Token token = template.peek();
      Operator oper = Operator.of(token.value());
      if (oper == null) {
         return left;
      }
      template.next();
      Expression right = readExpression(false);
      return new OperatorExpression(oper, left, right);
   }

   @Override
   public Expression readFilteredExpression(boolean optional) {
      Expression expr = readExpression(optional);
      if (expr == null) {
         return null;
      }
      FilteredExpression result = new FilteredExpression(expr);
      Token token = template.peek();
      while (token.isSymbol(Symbol.PIPE)) {
         template.next();
         token = template.next();
         if (!token.isIdentifier()) {
            report(token, "A filter identifier was expected here");
            return null;
         }
         String funcName = token.value();
         if (engine.getFilter(funcName) == null) {
            report(token, "A filter with this name is not known");
            return null;
         }
         Collection<Expression> args = new ArrayList<>();
         token = template.peek();
         if (token.isSymbol(Symbol.COLON)) {
            template.next();
            expr = readExpression(false);
            args.add(expr);
            token = template.peek();
            while (token.isSymbol(Symbol.COMMA)) {
               template.next();
               expr = readExpression(false);
               args.add(expr);
               token = template.peek();
            }
         }
         result.addFilter(funcName, args.toArray(new Expression[0]));
      }
      return result;
   }

   @Override
   public Value readValue(boolean optional) {
      Token token = template.peek();
      Value result = readLiteral(true);
      if (result == null) {
         result = readVariable(true);
      }
      if (result == null && !optional) {
         report(token, "A variable or literal was expected here");
      }
      return result;
   }

   @Override
   public Literal<?> readLiteral(boolean optional) {
      Token token = template.peek();
      Literal<?> result = readString(true);
      if (result == null) {
         result = readNumber(true);
         if (result == null) {
            if (token.isIdentifier()) {
               switch (token.value()) {
                  case "true":
                     result = Literal.TRUE;
                     break;
                  case "false":
                     result = Literal.FALSE;
                     break;
                  case "nil":
                     result = Literal.NIL;
                     break;
               }
            }
         }
      }
      if (result == null && !optional) {
         report(token, "A literal value was expected here");
      }
      return result;
   }

   @Override
   public Variable readVariable(boolean optional) {
      Token token = template.peek();
      if (token.isIdentifier()) {
         StringBuilder result = new StringBuilder();
         result.append(token.value());
         template.next();
         token = template.peek();
         if (token.isSymbol(Symbol.OPEN_BRACES)) {
            template.next();
            result.append('[');
            result.append(readValue(false));
            expectSymbol(Symbol.CLOSE_BRACES, false);
            result.append(']');
            token = template.peek();
         }
         if (token.isSymbol(Symbol.DOT)) {
            result.append('.');
            template.next();
            result.append(readVariable(false).name());
         }
         return new Variable(result.toString());
      }
      if (!optional) {
         report(token, "A variable was expected here");
      }
      return null;
   }

   @Override
   public Variable readSimpleVariable(boolean optional) {
      Token token = template.peek();
      if (token.isIdentifier()) {
         return new Variable(template.next().value());
      }
      if (!optional) {
         report(token, "A variable was expected here");
      }
      return null;
   }

   @Override
   public StringLiteral readString(boolean optional) {
      Token token = template.peek();
      if (token.isString()) {
         template.next();
         String value = token.value();
         char ch = value.charAt(0);
         value = value.substring(1);
         if (value.charAt(value.length() - 1) == ch) {
            value = value.substring(0, value.length() - 1);
         } else {
            report(token, "This string was not terminated");
         }
         return new StringLiteral(value);
      }
      if (!optional) {
         report(token, "A string literal was expected here");
      }
      return null;
   }

   @Override
   public Value readStringOrVariable(boolean optional) {
      Value result = readString(true);
      if (result == null) {
         result = readVariable(true);
      }
      if (result == null && !optional) {
         report(template.peek(), "A string or variable was expected here");
      }
      return result;
   }

   @Override
   public NumberLiteral readNumber(boolean optional) {
      Token token = template.peek();
      if (token.isNumber()) {
         template.next();
         return new NumberLiteral(token.value());
      }
      if (!optional) {
         report(token, "A number literal was expected here");
      }
      return null;
   }

   @Override
   public Value readNumberOrVariable(boolean optional) {
      Value result = readNumber(true);
      if (result == null) {
         result = readVariable(true);
      }
      if (result == null && !optional) {
         report(template.peek(), "A number or variable was expected here");
      }
      return result;
   }

   @Override
   public Value readRangeOrVariable(boolean optional) {
      Token token = template.peek();
      Value result = null;
      if (token.isSymbol(Symbol.OPEN_PARENS)) {
         template.next();
         Value left = readNumberOrVariable(false);
         expectSymbol(Symbol.RANGE, false);
         Value right = readNumberOrVariable(false);
         expectSymbol(Symbol.CLOSE_PARENS, false);
         result = new Range(left, right);
      } else {
         result = readVariable(true);
      }
      if (result == null && !optional) {
         report(token, "A range or variable was expected here");
      }
      return result;
   }

   @Override
   public boolean expectSymbol(Symbol symbol, boolean optional) {
      Token token = template.peek();
      if (token.isSymbol(symbol)) {
         template.next();
         return true;
      }
      if (!optional) {
         report(token, "A '" + symbol.value() + "' was expected here");
      }
      return false;
   }

   @Override
   public boolean expectIdentifier(String identifier, boolean optional) {
      Token token = template.peek();
      if (token.isIdentifier() && token.value().equals(identifier)) {
         template.next();
         return true;
      }
      if (!optional) {
         report(token, "'" + identifier + "' was expected here");
      }
      return false;
   }

   @Override
   public void report(String message) {
      report(template.next(), message);
   }

   private LiquidParser(LiquidEngine engine, String filename, String template) {
      this.template = new LiquidTokenizer(template);
      this.blocks.add(new ArrayList<>());
      this.filename = filename;
      this.engine = engine;
   }

   private Collection<Element> parse() throws TemplateParseException {
      while (template.hasNext()) {
         Collection<Element> block = blocks.peek();
         Token token = template.next();
         if (token.isLiteral()) {
            block.add(new PlainTextElement(token.value()));
         } else if (token.isObjectStart()) {
            ObjectElement obj = readObject();
            if (obj != null) {
               block.add(obj);
            }
         } else if (token.isTagStart()) {
            readTag();
         }
      }
      while (!tags.isEmpty()) {
         blocks.pop();
         TagElementBuilder tag = tags.pop();
         report(tokens.remove(tag), "This tag block was not terminated");
      }
      if (!errors.isEmpty()) {
         throw new TemplateParseException(filename, template.toString(), errors);
      }
      return blocks.pop();
   }

   private ObjectElement readObject() {
      ObjectElement result = null;
      result = new ObjectElement(readFilteredExpression(false));
      Token token = template.next();
      if (!token.isObjectEnd()) {
         report(token, "This symbol was not expected here");
         while (!token.isObjectEnd()) {
            token = template.next();
         }
      }
      return result;
   }

   private TagElement readTag() {
      TagElementBuilder builder = null;
      TagElement result = null;
      final Token tagName = template.next();
      Token token = tagName;
      if (token.isIdentifier()) {
         builder = engine.getTagBuilder(tagName.value());
         if (builder != null) {
            result = builder.build(tagName.value(), this);
            token = template.next();
            if (!token.isTagEnd()) {
               report(token, "This symbol was not expected here");
            }
         } else {
            report(token, "This tag was not recognised");
         }
      } else {
         report(token, "A tag identifier was expected here");
      }
      while (!token.isTagEnd()) {
         token = template.next();
      }
      if (result != null) {
         Collection<Element> block = blocks.peek();
         if (builder instanceof BlockTagElementBuilder && result instanceof BlockTagElement) {
            tokens.put(builder, tagName);
            block.add(result);
            tags.add((BlockTagElementBuilder) builder);
            blocks.add(new ArrayList<>());
            if (engine.getTagBuilder("end" + result.name()) == null) {
               engine.registerTagBuilder("end" + result.name(), MarkerTagElement.Builder.class);
            }
         } else if (result instanceof BreakTagElement) {
            BlockTagElementBuilder bbuilder = tags.peek();
            if (builder != null && (isEndTag(bbuilder, result)
                     || bbuilder.acceptsBreak((BreakTagElement) result))) {
               Element[] elements = blocks.pop().toArray(new Element[0]);
               blocks.peek().add(new ElementBlock(elements));
               blocks.peek().add(result);
               if (!isEndTag(bbuilder, result)) {
                  blocks.add(new ArrayList<>());
               } else {
                  tokens.remove(tags.pop());
               }
            } else {
               report(token, "Did not expect this tag here: " + result.name());
            }
         } else {
            block.add(result);
         }
      }
      return result;
   }

   private boolean isEndTag(BlockTagElementBuilder builder, TagElement tag) {
      String tagName = tokens.get(builder).value();
      return tag.name().equals("end" + tagName);
   }

   private void report(Token token, String message) {
      errors.add(new ParseError(token.position(), token.length(), message));
   }
}
