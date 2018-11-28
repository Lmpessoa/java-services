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
package com.lmpessoa.services;

import com.lmpessoa.services.internal.hosting.HttpException;
import com.lmpessoa.services.validating.ErrorSet;

/**
 * Thrown when the received request is not valid.
 */
public final class BadRequestException extends HttpException {

   private static final long serialVersionUID = 1L;
   private final ErrorSet errors;

   /**
    * Creates a new {@code BadRequestException}.
    */
   public BadRequestException() {
      super(400);
      this.errors = null;
   }

   /**
    * Creates a new {@code BadRequestException} with the given detail message.
    *
    * @param message the detail message.
    */
   public BadRequestException(String message) {
      super(400, message);
      this.errors = null;
   }

   /**
    * Creates a new {@code BadRequestException} with the given detail messages.
    *
    * @param errors the detail messages.
    */
   public BadRequestException(ErrorSet errors) {
      super(400);
      this.errors = errors;
   }

   /**
    * Creates a new {@code BadRequestException} with the given cause.
    *
    * @param cause the cause for the exception.
    */
   public BadRequestException(Throwable t) {
      super(400, t);
      errors = null;
   }

   /**
    * Returns a set of errors describing the problems with the current request.
    *
    * @return a set of errors describing the problems with the current request, or {@code null} if
    *         no such set is present.
    */
   public ErrorSet getErrors() {
      return errors;
   }
}
