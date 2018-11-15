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
package com.lmpessoa.services.util.parsing;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * The <code>AbstractTemplateParser</code> implements a generic reusable parser for texts that
 * follow a standardised pattern. This pattern consists of variable parts identified by being
 * enclosed in curly braces; everything else is considered literal.
 * </p>
 *
 * <p>
 * Subclasses must implement the method {@link #parseVariable(int, String)} in order to produce the
 * actual variable representation to be returned by that parse. Also, subclasses are recommended to
 * maintain the visibility of the methods in this class and also implement a static method which
 * will both instantiate the parser and run the parsing process. Since the parser is not required
 * after the parsing process is finished, it can be discarded afterwards.
 * </p>
 *
 * <pre>
 * public class SampleTemplateParser extends AbstractTemplateParser<SampleVariablePart>
 *
 *    public static ITemplatePart[] parse(String template) {
 *       return new SampleTemplateParser(template).parse();
 *    }
 * }
 * </pre>
 *
 * <p>
 * Whenever the template requires a variable to be seen as a literal, it must be escaped by
 * preceding it with a backslash (<code>\</code>).
 * </p>
 *
 * @param <T> the actual type of variable parts returned by the template parser.
 */
public abstract class AbstractParser<T extends IVariablePart> {

   private static final Map<Character, Character> BLOCKS;

   private final boolean forceLiteralSeparator;
   private final String variablePrefix;
   private final String template;
   private int pos;

   static {
      Map<Character, Character> blocks = new HashMap<>();
      blocks.put('(', ')');
      blocks.put('[', ']');
      blocks.put('{', '}');
      BLOCKS = Collections.unmodifiableMap(blocks);
   }

   /**
    * Creates a new parser to parse the given template.
    * <p>
    * This constructor only initialises the underlying parser for the concrete subclass with the
    * template itself to be parsed and a flag to indicate whether or not the parser should enforce a
    * literal separator between two variables.
    * </p>
    *
    * <p>
    * As a rule of thumb a literal separator is never required for templates that are used to generate
    * content; on the other hand, when trying to match a string with a template, a literal would help
    * better identify when one variable ends and the other begins.
    * </p>
    *
    * @param template the template to be parsed.
    * @param forceLiteralSeparator whether the template must force a literal separator between two
    * variables.
    */
   protected AbstractParser(String template, boolean forceLiteralSeparator, Character forceVariablePrefix) {
      this.template = template;
      this.forceLiteralSeparator = forceLiteralSeparator;
      this.variablePrefix = forceVariablePrefix != null ? forceVariablePrefix + "{" : "{";
   }

   /**
    * Parses the template in this parser and returns a representation of its structure.
    *
    * @return a list of parts extracted from the template.
    * @throws ParseException if an error has been reached unexpectedly while parsing.
    */
   protected final List<ITemplatePart> parse() throws ParseException {
      pos = 0;
      List<ITemplatePart> result = new ArrayList<>();
      ITemplatePart lastPart = null;
      while (pos < template.length()) {
         ITemplatePart part;
         if (template.startsWith(variablePrefix, pos)) {
            if (forceLiteralSeparator && lastPart != null && lastPart instanceof IVariablePart) {
               throw new ParseException("A literal must separate two variables", pos);
            }
            part = parseVariable();
         } else {
            part = parseLiteral();
         }
         if (part != null) {
            result.add(part);
            lastPart = part;
         }
      }
      return result;
   }

   /**
    * Returns an object which represents a variable with the given content.
    *
    * <p>
    * Subclasses must provide an implementation of this method that converts the contents of the
    * variable (the text part enclosed in curly braces in the template) into another object which can
    * be queried about the proper contents of the variable.
    * </p>
    *
    * <p>
    * Subclasses implementing this method are allowed to throw new <code>ParseException</code>s to
    * indicate errors in the expected format of the variable, e.g. to indicate the variable is not in a
    * valid format.
    * </p>
    *
    * @param pos the position in the template where the variable occurred.
    * @param variablePart the content of the curly braces that makes this variable.
    * @return an instance of the variable type which represents the variable with the given content.
    * @throws ParseException if an error has been reached unexpectedly while parsing.
    */
   protected abstract T parseVariable(int pos, String variablePart) throws ParseException;

   private IVariablePart parseVariable() throws ParseException {
      int varPos = pos;
      if (variablePrefix.length() == 2) {
         pos += 1;
      }
      String variablePart = readWithBlock('}');
      pos += 1;
      return parseVariable(varPos, variablePart);
   }

   private String readWithBlock(char endChar) throws ParseException {
      StringBuilder result = new StringBuilder();
      while (pos < template.length()) {
         pos += 1;
         if (pos == template.length()) {
            throw new ParseException("Unexpected end of the template", pos - 1);
         }
         char ch = template.charAt(pos);
         if (BLOCKS.containsKey(ch)) {
            result.append(ch);
            String sub = readWithBlock(BLOCKS.get(ch));
            result.append(sub);
            result.append(BLOCKS.get(ch));
         } else {
            if (ch == endChar) {
               return result.toString();
            }
            result.append(ch);
         }
      }
      throw new ParseException("Unexpected end of the template", pos);
   }

   private LiteralPart parseLiteral() {
      StringBuilder result = new StringBuilder();
      while (pos < template.length()) {
         int varPos = template.indexOf(variablePrefix, pos);
         if (varPos > 0 && "\\{".equals(template.substring(varPos - 1, varPos + 1))) {
            result.append(template.substring(pos, varPos - 1));
            result.append('{');
            pos = varPos + 1;
         } else {
            if (varPos < 0) {
               varPos = template.length();
            }
            result.append(template.substring(pos, varPos));
            pos = varPos;
            break;
         }
      }
      return result.length() == 0 ? null : new LiteralPart(result.toString());
   }
}
