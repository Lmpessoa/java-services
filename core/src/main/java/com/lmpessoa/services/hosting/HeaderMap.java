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
package com.lmpessoa.services.hosting;

import java.util.Set;

/**
 * Represents headers in an HTTP request.
 */
public interface HeaderMap {

   /**
    * Returns whether this map contains any value associated with the given header name.
    *
    * @param headerName the name of the header to test for the presence of values.
    * @return {@code true} if this map has values associated with the given header name, or
    *         {@code false} otherwise.
    */
   boolean contains(String headerName);

   /**
    * Returns the first value associated of the header with the given header name on this map.
    *
    * @param headerName the name of the header to retrieve the value for.
    * @return the first value associated with the given header name, or {@code null} if no such
    *         value exists.
    */
   String get(String headerName);

   /**
    * Returns any values associated with the given header name on this map.
    *
    * @param headerName the name of the header to retrieve the value for.
    * @return the values associates with the given header name, or an empty array if no such values
    *         exist.
    */
   String[] getAll(String headerName);

   /**
    * Returns a set of header names present in this map.
    *
    * @return a set of header names present in this map.
    */
   Set<String> nameSet();
}