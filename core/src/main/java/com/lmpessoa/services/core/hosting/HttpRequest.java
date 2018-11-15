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
package com.lmpessoa.services.core.hosting;

import java.util.Collection;
import java.util.Map;

import com.lmpessoa.services.core.routing.IRouteRequest;
import com.lmpessoa.services.core.services.Reuse;
import com.lmpessoa.services.core.services.Service;

/**
 * Represents the raw HTTP request data.
 */
@Service(Reuse.REQUEST)
public interface HttpRequest extends IRouteRequest {

   /**
    * Returns the protocol used with this HTTP request.
    * <p>
    * The protocol returned by this method includes both the protocol name and the version used.
    * </p>
    *
    * @return the protocol used with this HTTP request.
    */
   String getProtocol();

   /**
    * Returns the query parameters of this HTTP request as a single string.
    *
    * @return the query parameters of this HTTP request.
    */
   String getQueryString();

   /**
    * Returns a map with the headers of this HTTP request.
    *
    * @return a map with the headers of this HTTP request.
    */
   HeaderMap getHeaders();

   /**
    * Returns the first value associated with the given header name of this HTTP request.
    *
    * @param headerName the name of the header to retrieve.
    * @return the first value associated with the given header name, or <code>null</code> if no such
    * value exists.
    */
   String getHeader(String headerName);

   /**
    * Returns a map with the parsed values of the query string of this HTTP request.
    *
    * @return a map with the parsed values of the query string of this HTTP request.
    */
   Map<String, Collection<String>> getQuery();
}
