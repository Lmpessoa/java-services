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
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents errors in an object validation.
 * <p>
 * Objects received through the body of an HTTP request may contain a validation method which will
 * ensure the object received is valid. Instances of {@code ErrorList} are passed to his validation
 * method to hold error messages that arise from such validation.
 * </p>
 * <p>
 * In order for objects to be validated, classes must implement a {@code validate(ErrorList)}
 * method. Developers may also choose to implement the {@link Validable} interface, which will only
 * provide the correctly required method signature for {@code validate()}. The implementation of the
 * interface is not mandatory.
 * </p>
 *
 * @see Validable
 */
public final class ErrorList {

   private final List<Message> errors = new ArrayList<>();
   private transient final Collection<String> fieldNames;
   private transient final Object object;

   /**
    * Adds an error message associated with the evaluated object.
    * <p>
    * Messages added to the list through this method are not associated with any fields. Instead,
    * these messages are associated with the evaluated object as a whole.
    * </p>
    *
    * @param message the message to be associated with the evaluated object.
    */
   public void add(String message) {
      errors.add(new Message(Objects.requireNonNull(message)));
   }

   /**
    * Adds an error message associated with the field with the given name on the evaluated object.
    * <p>
    * Note that the object must actually have a field with the given name in order to store the
    * message. Otherwise, this method will throw an exception.
    * </p>
    *
    * @param fieldName the name of the field in the evaluated object.
    * @param message the message to be associated with the given field name in the evaluated object.
    * @throws NoSuchElementException if the evaluated object has no field with the given name.
    */
   public void add(String fieldName, String message) {
      if (!fieldNames.contains(Objects.requireNonNull(fieldName))) {
         throw new NoSuchElementException();
      }
      errors.add(new Message(Objects.requireNonNull(message), fieldName));
   }

   /**
    * Returns whether no error messages have been registered in this map.
    *
    * @return {@code true} if no error messages have been registered in this map, {#code false}
    *         otherwise.
    */
   public boolean isEmpty() {
      return errors.isEmpty();
   }

   /**
    * Returns the object this error list is about.
    *
    * @return the object this error list is about.
    */
   public Object getObject() {
      return object;
   }

   /**
    * Performs the given action for each message of this list until all elements have been processed
    * or the action throws an exception. Unless otherwise specified by the implementing class,
    * actions are performed in the order of iteration (if an iteration order is specified).
    * Exceptions thrown by the action are relayed to the caller.
    *
    * @param action the action to be performed in each element.
    * @throws NullPointerException if the specified action is {@code null}.
    */
   public void forEach(Consumer<Message> action) {
      errors.forEach(action);
   }

   ErrorList(Object object) {
      this.object = Objects.requireNonNull(object);
      List<String> fields = new ArrayList<>();
      Class<?> type = object.getClass();
      while (type != null && type != Object.class) {
         Arrays.stream(type.getDeclaredFields())
                  .filter(f -> !Modifier.isStatic(f.getModifiers())
                           && !Modifier.isTransient(f.getModifiers())
                           && !Modifier.isVolatile(f.getModifiers()))
                  .map(f -> f.getName())
                  .forEach(f -> fields.add(f));
         type = type.getSuperclass();
      }
      this.fieldNames = Collections.unmodifiableCollection(fields);
   }

   public static class Message {

      private final String message;
      private final String field;

      Message(String message) {
         this(message, null);
      }

      Message(String message, String fieldName) {
         this.message = message;
         this.field = fieldName;
      }

      public String getValue() {
         return message;
      }

      public String getField() {
         return field;
      }
   }
}
