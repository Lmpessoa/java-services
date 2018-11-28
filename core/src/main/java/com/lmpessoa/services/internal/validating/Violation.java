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
package com.lmpessoa.services.internal.validating;

import java.util.Map;
import java.util.Objects;

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

   PathNode getEntry() {
      return entry;
   }

   String getMessageTemplate() {
      return messageTemplate;
   }

   Map<String, Object> getConstraintAttributes() {
      return constraint.getAttributes();
   }

   Object getInvalidValue() {
      return value;
   }
}
