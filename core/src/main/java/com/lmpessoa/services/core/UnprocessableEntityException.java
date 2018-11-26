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
package com.lmpessoa.services.core;

import com.lmpessoa.services.core.internal.hosting.HttpException;

/**
 * Thrown when the server is unable to process the instructions contained in the request entity.
 *
 * <p>
 * Applications should throw this exception to indicate that the server understands the content type
 * of the request entity (hence a <i>415 Unsupported Media Type</i> status code is inappropriate),
 * and the syntax of the request entity is correct (thus a <i>400 Bad Request</i> status code is
 * inappropriate) but still it contains a set of instructions that cannot be processed by the
 * application.
 * </p>
 */
public class UnprocessableEntityException extends HttpException {

   private static final long serialVersionUID = 1L;

   /**
    * Creates a new {@code UnprocessableEntityException}.
    */
   public UnprocessableEntityException() {
      super(422);
   }

   /**
    * Creates a new {@code UnprocessableEntityException} with the given detail message.
    *
    * @param message the detail message.
    */
   public UnprocessableEntityException(String message) {
      super(422, message);
   }
}
