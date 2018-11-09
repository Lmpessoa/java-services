/*
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
package com.lmpessoa.services.services;

/**
 * Thrown when a single constructor or method for a given name is not found on a class.
 *
 * <p>
 * A <code>NoSingleMethodException</code> closely resembles a {@link NoSuchMethodException} except
 * that instead of not finding the method or constructor it found more than one and thus cannot
 * decide which one to use.
 * </p>
 */
public final class NoSingleMethodException extends Exception {

   private static final long serialVersionUID = 1L;

   /**
    * Creates a new <code>NoSingleMethodException</code> with the given detail message.
    *
    * @param message the detail message.
    * @param found the number of actual methods found.
    */
   public NoSingleMethodException(String message, int found) {
      super(message + " (found: " + found + ')');
   }
}
