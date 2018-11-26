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
package com.lmpessoa.services.core.internal.validating;

import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.lmpessoa.services.core.InternalServerError;
import com.lmpessoa.services.util.Localization;
import com.lmpessoa.services.util.parsing.ITemplatePart;
import com.lmpessoa.services.util.parsing.LiteralPart;
import com.lmpessoa.services.util.parsing.SimpleParser;
import com.lmpessoa.services.util.parsing.SimpleVariablePart;

final class Violation {

   private final ConstraintAnnotation constraint;
   private final String messageTemplate;
   private final PathNode entry;
   private final Object value;

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof Violation)) {
         return false;
      }
      Violation other = (Violation) obj;
      return constraint == other.constraint;
   }

   @Override
   public int hashCode() {
      return constraint.hashCode();
   }

   Violation(ConstraintAnnotation constraint, PathNode entry) {
      this(constraint, entry, constraint.getMessage(), entry.getValue());
   }

   Violation(ConstraintAnnotation constraint, PathNode entry, String messageTemplate) {
      this(constraint, entry, messageTemplate, entry.getValue());
   }

   Violation(ConstraintAnnotation constraint, PathNode entry, String messageTemplate,
      Object invalidValue) {
      this.constraint = Objects.requireNonNull(constraint);
      this.entry = Objects.requireNonNull(entry);
      this.messageTemplate = messageTemplate;
      this.value = invalidValue;
   }

   static String getProperty(String name, Locale[] locales) {
      if (name.indexOf('.') == -1) {
         return null;
      }
      Localization.Messages messages = Localization.messagesOf("/validation/ValidationMessages",
               locales);
      return messages.get(name);
   }

   PathNode getEntry() {
      return entry;
   }

   String getMessage(Locale[] locales) {
      try {
         return interpolate(getMessageTemplate(), locales);
      } catch (ParseException e) {
         throw new InternalServerError(e);
      }
   }

   String getMessageTemplate() {
      return messageTemplate;
   }

   Object getInvalidValue() {
      return value;
   }

   private String interpolate(String variable, Locale[] locales) throws ParseException {
      Objects.requireNonNull(variable);
      StringBuilder result = new StringBuilder();
      List<ITemplatePart> parts = SimpleParser.parse(variable,
               "[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*");
      for (ITemplatePart part : parts) {
         if (part instanceof LiteralPart) {
            result.append(((LiteralPart) part).getValue());
         } else {
            String name = ((SimpleVariablePart) part).getName();
            String varValue = getValueOfVariable(name, locales);
            result.append(varValue);
         }
      }
      return result.toString();
   }

   private String getValueOfVariable(String name, Locale[] locales) throws ParseException {
      String result;
      if (isConstraintDefinitionVariable(name)) {
         result = null;
      } else if (name.indexOf('.') >= 0) {
         result = getProperty(name, locales);
         result = result == null ? null : interpolate(result, locales);
      } else {
         Object objValue = constraint.get(name);
         result = objValue == null ? null : objValue.toString();
      }
      if (result == null) {
         result = String.format("{%s}", name);
      }
      return result;
   }

   private boolean isConstraintDefinitionVariable(String name) {
      return "groups".equals(name) || "payload".equals(name) || "message".equals(name)
               || "validationAppliesTo".equals(name);
   }
}
