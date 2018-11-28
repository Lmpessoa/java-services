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
package com.lmpessoa.services.internal.routing;

import com.lmpessoa.services.routing.HttpMethod;

/**
 * Thrown when a method being added is already registered on a route table.
 */
public final class DuplicateMethodException extends Exception {

   private static final long serialVersionUID = 1L;

   /**
    * Creates a new {@code DuplicateMethodException} with the given detail.
    *
    * @param methodName the duplicated HTTP method.
    * @param methodPat the duplicated route pattern for the resource.
    * @param clazz the resource class that defined the duplicated method.
    */
   DuplicateMethodException(HttpMethod methodName, RoutePattern methodPat, Class<?> clazz) {
      super("Ignored redefining '" + methodName + " " + methodPat + "' using " + clazz.getName());
   }
}
