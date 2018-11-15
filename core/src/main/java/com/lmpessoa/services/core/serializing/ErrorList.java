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
package com.lmpessoa.services.core.serializing;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ErrorList {

   private final List<ErrorEntry> errors = new ArrayList<>();
   private transient final Collection<String> fieldNames;
   private transient final Object sourceObject;

   public void add(String message) {
      errors.add(new ErrorEntry(Objects.requireNonNull(message)));
   }

   public void add(String fieldName, String message) {
      if (!fieldNames.contains(Objects.requireNonNull(fieldName))) {
         throw new NoSuchElementException();
      }
      errors.add(new ErrorEntry(Objects.requireNonNull(message), fieldName));
   }

   public boolean isEmpty() {
      return errors.isEmpty();
   }

   public boolean hasMessages() {
      return errors.stream().anyMatch(e -> e.getField() == null);
   }

   public boolean hasMessages(String fieldName) {
      Objects.requireNonNull(fieldName);
      return errors.stream().anyMatch(e -> e.getField() != null && e.getField().equals(fieldName));
   }

   public String[] getFields() {
      return errors.stream().map(e -> e.getField()).distinct().filter(e -> e != null).toArray(
               String[]::new);
   }

   public String[] getMessages() {
      return errors.stream().filter(e -> e.getField() == null).map(e -> e.getMessage()).toArray(
               String[]::new);
   }

   public String[] getMessages(String fieldName) {
      if (!fieldNames.contains(Objects.requireNonNull(fieldName))) {
         return new String[0];
      }
      return errors.stream()
               .filter(e -> e.getField() != null && e.getField().equals(fieldName))
               .map(e -> e.getMessage())
               .toArray(String[]::new);
   }

   public String[] getAllMessages() {
      return errors.stream().map(e -> e.getMessage()).toArray(String[]::new);
   }

   public Object getSourceObject() {
      return sourceObject;
   }

   ErrorList(Object sourceObject) {
      this.sourceObject = Objects.requireNonNull(sourceObject);
      this.fieldNames = Arrays.stream(sourceObject.getClass().getFields())
               .filter(f -> !Modifier.isStatic(f.getModifiers())
                        && !Modifier.isTransient(f.getModifiers())
                        && !Modifier.isVolatile(f.getModifiers()))
               .map(f -> f.getName())
               .collect(Collectors.toList());
   }

   private static class ErrorEntry {

      private final String message;
      private final String field;

      ErrorEntry(String message) {
         this(message, null);
      }

      ErrorEntry(String message, String fieldName) {
         this.message = message;
         this.field = fieldName;
      }

      public String getMessage() {
         return message;
      }

      public String getField() {
         return field;
      }
   }
}
