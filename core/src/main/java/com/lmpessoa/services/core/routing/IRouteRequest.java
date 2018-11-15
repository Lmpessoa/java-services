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
package com.lmpessoa.services.core.routing;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a request for a route match.
 *
 * <p>
 * A route request is comprised of a series of data that can be used to find a method and its
 * arguments from a {@link RouteTable}.
 * </p>
 */
public interface IRouteRequest {

   /**
    * Returns the method of this HTTP request.
    *
    * <p>
    * Usually the value returned here will be one of GET, POST, PUT, DELETE or HEAD but other values
    * may be returned.
    * </p>
    *
    * @return the method of this HTTP request or {@code null} if the method is not recognised.
    */
   HttpMethod getMethod();

   /**
    * Returns the path of this HTTP request.
    *
    * <p>
    * Note that the engine makes no distinction between a requested path and path info, as in other
    * platforms as it does not work with partial matches.
    * </p>
    * <p>
    * To have access to query parameters of this HTTP request, refer to {@see #getQueryString()}.
    * </p>
    *
    * @return the path of this HTTP request.
    * @see QueryParam
    */
   String getPath();

   /**
    * Returns the query parameters of this HTTP request as a single string.
    *
    * @return the query parameters of this HTTP request.
    * @see QueryParam
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
    * @return the content length of the body of this HTTP request, or <code>0</code> if this value
    *         is not present.
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
}
