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
package com.lmpessoa.services.concurrent;

import com.lmpessoa.services.routing.RouteMatch;
import com.lmpessoa.services.security.IIdentity;

/**
 * Represents an asynchronous request.
 *
 * <p>
 * Applications are not expected to provide classes implementing this interface. It is used only
 * when implementing an {@link IAsyncRequestMatcher} in order to provide information about executing
 * and queued asynchronous requests in order to allow these to find if one queued job matched the
 * given route.
 * </p>
 */
public interface AsyncRequest {

   /**
    * Returns the identity of the client that created this asynchronous request.
    *
    * @return the identity of the client that created this asynchronous request or {@code null} if
    *         the request was created anonymously.
    */
   IIdentity getIdentity();

   /**
    * Returns the matched route information about this asynchronous request.
    *
    * @return the matched route information about this asynchronous request.
    */
   RouteMatch getRoute();
}
