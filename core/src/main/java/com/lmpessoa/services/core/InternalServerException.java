/*
 * A light and easy engine for developing web APIs and microservices.
 * Copyright (c) 2017 Leonardo Pessoa
 * http://github.com/lmpessoa/java-services
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
package com.lmpessoa.services.core;

/**
 * Thrown when an internal server error has happened.
 *
 * <p>
 * Any exception other than <code>HttpException</code>s can be thrown from any resource method.
 * These will be understood by the engine as if an internal server error has happened and are
 * wrapped in an instance of this class.
 * </p>
 */
public final class InternalServerException extends HttpException {

   private static final long serialVersionUID = 1L;

   /**
    * Creates a new <code>InternalServerException</code> with the given detail message.
    *
    * @param message the detail message.
    */
   public InternalServerException(String message) {
      super(message);
   }

   /**
    * Creates a new <code>InternalServerException</code> with the given cause.
    *
    * @param cause the cause.
    */
   public InternalServerException(Throwable cause) {
      super(cause);
   }

   /**
    * Creates a new <code>InternalServerException</code> with the given detail message and cause.
    *
    * @param message the detail message.
    * @param cause the cause.
    */
   public InternalServerException(String message, Throwable cause) {
      super(message, cause);
   }

   @Override
   public int getStatusCode() {
      return 500;
   }
}
