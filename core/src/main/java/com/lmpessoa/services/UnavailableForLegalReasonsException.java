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
package com.lmpessoa.services;

import com.lmpessoa.services.internal.hosting.HttpException;

/**
 * Thrown when denying access to the resource as a consequence of a legal demand.
 */
public final class UnavailableForLegalReasonsException extends HttpException {

   private static final long serialVersionUID = 1L;

   /**
    * Creates a new instance of {@code UnavailableForLegalReasonsException}.
    */
   public UnavailableForLegalReasonsException() {
      super(451);
   }

   /**
    * Creates a new instance of {@code UnavailableForLegalReasonsException} with the given detail
    * message.
    *
    * @param message the detail message.
    */
   public UnavailableForLegalReasonsException(String message) {
      super(451, message);
   }

   /**
    * Creates a new instance of {@code UnavailableForLegalReasonsException} with the given cause.
    *
    * @param cause the cause for the exception
    */
   public UnavailableForLegalReasonsException(Throwable cause) {
      super(451, cause);
   }
}
