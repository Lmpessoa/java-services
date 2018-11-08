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
 * An <code>HttpException</code> is an abstract exception class that represents one of the HTTP
 * client error status codes (4xx) or server error status codes (5xx).
 *
 * <p>
 * If a method choses to throw an <code>HttpException</code> the engine understants that that
 * exception represents the intended result to return to the sender of the request.
 * </p>
 *
 * <p>
 * Any other exception can be thrown from any resource method. These will be understood by the
 * engine as if an internal server error (HTTP status code 500) has happened and that is what will
 * be returned to the sender of the request in this case.
 * </p>
 */
public abstract class HttpException extends Exception {

   private static final long serialVersionUID = 1L;

   HttpException() {
      super();
   }

   HttpException(String message) {
      super(message);
   }

   HttpException(Throwable cause) {
      super(cause);
   }

   HttpException(String message, Throwable cause) {
      super(message, cause);
   }

   /**
    * Returns the HTTP status code of this exception.
    *
    * @return the HTTP status code of this exception.
    */
   public abstract int getStatusCode();
}
