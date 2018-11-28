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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintViolation;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.Path.Node;

import com.lmpessoa.services.Localized;
import com.lmpessoa.services.internal.ClassUtils;
import com.lmpessoa.services.internal.hosting.Localization;
import com.lmpessoa.services.internal.validating.PathNode.ExecutablePathNode;
import com.lmpessoa.services.validating.ErrorSet;

final class ErrorSetImpl implements ErrorSet {

   private static final Localized.MessageSet MESSAGES = Localized
            .messagesAt("messages/ValidationMessages.properties");

   static final ErrorSet EMPTY = new ErrorSetImpl(Collections.<Violation>emptySet());

   private final List<Entry> errors;

   /**
    * Returns whether no error messages have been registered in this set.
    *
    * @return {@code true} if no error messages have been registered in this set, {#code false}
    *         otherwise.
    */
   @Override
   public boolean isEmpty() {
      return errors.isEmpty();
   }

   /**
    * Returns the amount of error messages in this set.
    *
    * @return the amount of error messages in this set.
    */
   @Override
   public int size() {
      return errors.size();
   }

   /**
    * Returns an iterator over the error messages in this set.
    *
    * @return an iterator over the error messages in this set.
    */
   @Override
   public Iterator<Entry> iterator() {
      return errors.iterator();
   }

   /**
    * Returns a sequential {@code Stream} with the errors in this set.
    *
    * @return a sequential {@code Stream} over the errors in this set.
    */
   @Override
   public Stream<Entry> stream() {
      return errors.stream();
   }

   ErrorSetImpl(Collection<Violation> violations) {
      errors = violations.stream().map(ViolationEntry::new).collect(Collectors.toList());
   }

   ErrorSetImpl(Set<ConstraintViolation<Object>> violations) {
      errors = violations.stream().map(ConstraintViolationEntry::new).collect(Collectors.toList());
   }

   private static String toArrayOrString(Object value) {
      if (value.getClass().isArray()) {
         return "{" + Arrays.stream((Object[]) value) //
                  .map(ErrorSetImpl::toArrayOrString)
                  .collect(Collectors.joining(",")) + "}";
      }
      Method toString = ClassUtils.getMethod(value.getClass(), "toString");
      if (toString.getDeclaringClass() == Object.class) {
         return "<" + value.getClass().getName() + ">";
      }
      return value.toString();
   }

   private final class ViolationEntry implements Entry {

      private final Violation violation;

      @Override
      public String getPathEntry() {
         String pathStr = violation.getEntry().toString();
         if (isExecutableRoot(violation.getEntry())) {
            String[] entryParts = pathStr.split("\\.", 2);
            return entryParts.length == 2 ? entryParts[1] : entryParts[0];
         }
         return pathStr;
      }

      @Override
      public String getMessage(Locale... locales) {
         return MESSAGES.get(violation.getMessageTemplate())
                  .with(violation.getConstraintAttributes());
      }

      @Override
      public String getMessageTemplate() {
         return violation.getMessageTemplate();
      }

      @Override
      public String getInvalidValue() {
         Object value = ConstraintAnnotation.unwrapOptional(violation.getInvalidValue());
         return value == null ? "null" : toArrayOrString(value);
      }

      ViolationEntry(Violation violation) {
         this.violation = violation;
      }

      private boolean isExecutableRoot(PathNode entry) {
         if (entry.getParent() != null) {
            return isExecutableRoot(entry.getParent());
         }
         return entry instanceof ExecutablePathNode;
      }
   }

   private final class ConstraintViolationEntry implements Entry {

      private final ConstraintViolation<?> violation;

      @Override
      public String getPathEntry() {
         Path violationPath = violation.getPropertyPath();
         if (isExecutableRoot(violationPath)) {
            String[] entryParts = violationPath.toString().split("\\.", 2);
            return entryParts.length == 2 ? entryParts[1] : entryParts[0];
         }
         return violationPath.toString();
      }

      @Override
      public String getMessage(Locale... locales) {
         Locale[] oldLocales = Localization.getLocales();
         try {
            if (locales.length != 0) {
               Localization.setLocales(locales);
            }
            return violation.getMessage();
         } finally {
            Localization.setLocales(oldLocales);
         }
      }

      @Override
      public String getMessageTemplate() {
         return violation.getMessageTemplate();
      }

      @Override
      public String getInvalidValue() {
         Object value = ConstraintAnnotation.unwrapOptional(violation.getInvalidValue());
         Path.Node lastNode = null;
         Iterator<Path.Node> nodes = violation.getPropertyPath().iterator();
         while (nodes.hasNext()) {
            lastNode = nodes.next();
         }
         if (lastNode != null && lastNode.getKind() == ElementKind.PARAMETER
                  && violation.getExecutableParameters() != null) {
            value = violation.getExecutableParameters()[((Path.ParameterNode) lastNode)
                     .getParameterIndex()];
         }
         return value == null ? "null" : toArrayOrString(value);
      }

      ConstraintViolationEntry(ConstraintViolation<?> violation) {
         this.violation = violation;
      }

      private boolean isExecutableRoot(Path propertyPath) {
         Node root = propertyPath.iterator().next();
         return root.getKind() == ElementKind.METHOD || root.getKind() == ElementKind.CONSTRUCTOR;
      }
   }
}
