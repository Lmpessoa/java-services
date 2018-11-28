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
package com.lmpessoa.services.internal.parsing;

import java.util.List;
import java.util.Objects;

import com.lmpessoa.services.internal.ErrorMessage;

/**
 * A simple parser.
 * <p>
 * This parser is a reusable parser where variable parts contain only a name reference or similar
 * data.
 * </p>
 */
public final class SimpleParser extends AbstractParser<SimpleVariablePart> {

   private final String variablePattern;

   /**
    * Parses a simple template where variables follow the common Java variable name pattern.
    *
    * @param template the template to be parsed.
    * @return the list of parts found in the given template.
    * @throws ParseException if an error has been reached unexpectedly while parsing.
    */
   public static List<ITemplatePart> parse(String template) {
      return new SimpleParser(template, "[a-zA-Z_][a-zA-Z0-9_]*").parse();
   }

   /**
    * Parses a simple template where variables follow the given name pattern.
    *
    * @param template the template to be parsed.
    * @param variablePattern the name pattern variables must match.
    * @return the list of parts found in the given template.
    * @throws ParseException if an error has been reached unexpectedly while parsing.
    */
   public static List<ITemplatePart> parse(String template, String variablePattern)
      throws ParseException {
      return new SimpleParser(template, variablePattern).parse();
   }

   @Override
   protected SimpleVariablePart parseVariable(int pos, String variablePart) {
      if (!variablePart.matches(variablePattern)) {
         throw new ParseException(ErrorMessage.INVALID_VARIABLE_NAME.with(variablePart), pos);
      }
      return new SimpleVariablePart(variablePart);
   }

   private SimpleParser(String template, String variablePattern) {
      super(template, true, null);
      this.variablePattern = Objects.requireNonNull(variablePattern);
   }
}
