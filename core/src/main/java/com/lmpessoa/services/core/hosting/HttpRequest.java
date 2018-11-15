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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * Captures the raw HTTP request data.
 */
public interface HttpRequest {

   /**
    * Returns the method of this HTTP request.
    *
    * <p>
    * Usually the value returned here will be one of GET, POST, PUT, DELETE or HEAD but other values
    * may be returned.
    * </p>
    *
    * @return the method of this HTTP request.
    */
   String getMethod();

   /**
    * Returns the path of this HTTP request.
    *
    * <p>
    * Note that the engine makes no distinction between a requested path and path info, as in other
    * platforms as it does not work with partial matches.
    * </p>
    * <p>
    * To have access to query parameters of this HTTP request, refer to {@see #getQuery()) and
    * {@see #getQueryString()}.
    * </p>
    *
    * @return the path of this HTTP request.
    */
   String getPath();

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
    * Returns the content length of the body of this HTTP request.
    *
    * <p>
    * Note that this method will not return effectively how many bytes are there in the request body
    * but only the value send with the request headers.
    * </p>
    *
    * @return the content length of the body of this HTTP request, or <code>0</code> if this value is
    * not present.
    */
   long getContentLength();

   /**
    * Returns the content type of the body of this HTTP request.
    *
    * @return the content type of the body of this HTTP request.
    */
   String getContentType();

   /**
    * Returns a stream with the content of this HTTP request.
    *
    * @return a stream with the content of this HTTP request.
    * @throws IOException
    */
   InputStream getBody();

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

   /**
    * Returns a map of the cookies sent with this HTTP request.
    *
    * @return a map of the cookies sent with this HTTP request.
    */
   Map<String, String> getCookies();
}
