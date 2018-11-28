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
import com.lmpessoa.services.routing.RouteMatch;

/**
 * Thrown when the requested resource exists but is not allow to respond to the requested method.
 */
public final class MethodNotAllowedException extends HttpException implements RouteMatch {

   private static final long serialVersionUID = 1L;

   /**
    * Creates a new {@code MethodNotAllowedException}.
    */
   public MethodNotAllowedException() {
      super(405);
   }

   /**
    * Creates a new {@code MethodNotAllowedException} with the given detail message.
    *
    * @param message the detail message.
    */
   public MethodNotAllowedException(String message) {
      super(405, message);
   }

   @Override
   public Object invoke() {
      throw this;
   }
}
