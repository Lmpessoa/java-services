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

import java.util.Iterator;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.lmpessoa.services.core.validating.ErrorSet.Entry;

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
public interface ErrorSet extends Iterable<Entry> {

   /**
    * Returns whether no error messages have been registered in this set.
    *
    * @return {@code true} if no error messages have been registered in this set, {#code false}
    *         otherwise.
    */
   boolean isEmpty();

   /**
    * Returns the amount of error messages in this set.
    *
    * @return the amount of error messages in this set.
    */
   int size();

   /**
    * Returns an iterator over the error messages in this set.
    *
    * @return an iterator over the error messages in this set.
    */
   @Override
   Iterator<Entry> iterator();

   /**
    * Returns a sequential {@code Stream} with the errors in this set.
    *
    * @return a sequential {@code Stream} over the errors in this set.
    */
   default Stream<Entry> stream() {
      return StreamSupport.stream(spliterator(), false);
   }

   /**
    * Represents a validation error message.
    */
   public static interface Entry {

      /**
       * Returns the path representation to the element where this message originated.
       *
       * @return the path representation to the element where this message originated.
       */
      String getPathEntry();

      /**
       * Returns the text message of this message.
       *
       * @return the text message of this message
       */
      String getMessage(Locale... locales);

      /**
       * Returns the raw (non-interpolated) text message of this message.
       *
       * @return the raw (non-interpolated) text message of this message.
       */
      String getMessageTemplate();

      /**
       * Returns a representation of the value that originated this error message.
       *
       * @return a representation of the value that originated this error message.
       */
      String getInvalidValue();
   }
}
