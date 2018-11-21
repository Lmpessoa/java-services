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
package com.lmpessoa.services.core.validating;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintViolation;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.Path.Node;

import com.lmpessoa.services.core.validating.ErrorSet.Message;
import com.lmpessoa.services.core.validating.PathNode.ExecutablePathNode;
import com.lmpessoa.services.util.ClassUtils;

/**
 * Represents errors in an object validation.
 *
 * <p>
 * Objects received through the body of an HTTP request may contain a validation method which will
 * ensure the object received is valid. Instances of {@code ErrorSet} are passed to his validation
 * method to hold error messages that arise from such validation.
 * </p>
 *
 * <p>
 * In order for objects to be validated, classes must implement a {@code validate(ErrorSet)} method.
 * Developers may also choose to implement the {@link Validable} interface, which will only provide
 * the correctly required method signature for {@code validate()}. The implementation of the
 * interface is not mandatory.
 * </p>
 */
public final class ErrorSet implements Iterable<Message> {

   static final ErrorSet EMPTY = new ErrorSet(Collections.<Violation>emptySet());

   private final List<Message> errors;

   /**
    * Returns whether no error messages have been registered in this set.
    *
    * @return {@code true} if no error messages have been registered in this set, {#code false}
    *         otherwise.
    */
   public boolean isEmpty() {
      return errors.isEmpty();
   }

   /**
    * Returns the amount of error messages in this set.
    *
    * @return the amount of error messages in this set.
    */
   public int size() {
      return errors.size();
   }

   /**
    * Returns an iterator over the error messages in this set.
    *
    * @return an iterator over the error messages in this set.
    */
   @Override
   public Iterator<Message> iterator() {
      return errors.iterator();
   }

   /**
    * Returns a sequential {@code Stream} with the errors in this set.
    *
    * @return a sequential {@code Stream} over the errors in this set.
    */
   public Stream<Message> stream() {
      return errors.stream();
   }

   ErrorSet(Collection<Violation> violations) {
      errors = violations.stream().map(Message::new).collect(Collectors.toList());
   }

   ErrorSet(Set<ConstraintViolation<Object>> violations) {
      errors = violations.stream().map(Message::new).collect(Collectors.toList());
   }

   /**
    * Represents a validation error message.
    */
   public static class Message {

      private final transient String template;
      private final String path;
      private final String message;
      private final String invalidValue;

      Message(Violation violation) {
         // entry
         String pathStr = violation.getEntry().toString();
         if (isExecutableRoot(violation.getEntry())) {
            String[] entryParts = pathStr.split("\\.", 2);
            this.path = entryParts.length == 2 ? entryParts[1] : entryParts[0];
         } else {
            this.path = pathStr;
         }

         // invalidValue
         Object value = ConstraintAnnotation.unwrapOptional(violation.getInvalidValue());
         this.invalidValue = value == null ? "null" : toArrayOrString(value);

         // message
         this.template = violation.getMessageTemplate();
         this.message = violation.getMessage();
      }

      Message(ConstraintViolation<?> violation) {
         // entry
         Path violationPath = violation.getPropertyPath();
         if (isExecutableRoot(violation.getPropertyPath())) {
            String[] entryParts = violationPath.toString().split("\\.", 2);
            this.path = entryParts.length == 2 ? entryParts[1] : entryParts[0];
         } else {
            this.path = violationPath.toString();
         }

         // invalidValue
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
         this.invalidValue = value == null ? "null" : toArrayOrString(value);

         // message
         this.template = violation.getMessageTemplate();
         this.message = violation.getMessage();
      }

      /**
       * Returns the path representation to the element where this message originated.
       *
       * @return the path representation to the element where this message originated.
       */
      public String getPathEntry() {
         return path;
      }

      /**
       * Returns the text message of this message.
       *
       * @return the text message of this message
       */
      public String getValue() {
         return message;
      }

      /**
       * Returns the raw (non-interpolated) text message of this message.
       *
       * @return the raw (non-interpolated) text message of this message.
       */
      public String getTemplate() {
         return template;
      }

      /**
       * Returns a representation of the value that originated this error message.
       *
       * @return a representation of the value that originated this error message.
       */
      public String getInvalidValue() {
         return invalidValue;
      }

      private static String toArrayOrString(Object value) {
         if (value.getClass().isArray()) {
            return "{" + Arrays.stream((Object[]) value) //
                     .map(Message::toArrayOrString)
                     .collect(Collectors.joining(",")) + "}";
         }
         Method toString = ClassUtils.getMethod(value.getClass(), "toString");
         if (toString.getDeclaringClass() == Object.class) {
            return "<" + value.getClass().getName() + ">";
         }
         return value.toString();
      }

      private boolean isExecutableRoot(PathNode entry) {
         if (entry.getParent() != null) {
            return isExecutableRoot(entry.getParent());
         }
         return entry instanceof ExecutablePathNode;
      }

      private boolean isExecutableRoot(Path propertyPath) {
         Node root = propertyPath.iterator().next();
         return root.getKind() == ElementKind.METHOD || root.getKind() == ElementKind.CONSTRUCTOR;
      }
   }
}
