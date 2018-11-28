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

import java.util.Map;
import java.util.UUID;

/**
 * Represents a matcher for asynchronous request routes.
 *
 * <p>
 * By default, each and every method call marked with {@link @Async} will be accepted and handled by
 * the engine. Developers may change the strategy used to accept or reject new request using either
 * the {@code reject} or the {@code matchWith} properties defined by that annotation. In order to
 * use the {@code matchWith} property, developers must provide it with a class that implements this
 * interface.
 * </p>
 *
 * <p>
 * Classes that implements this interface must implement a single method which must decide whether
 * the current request matches any of the queued asynchronous tasks pending completion and must
 * return an identifier that uniquely represents a queued job (if the route matches one using its
 * own rules).
 * </p>
 *
 * @see Async
 */
@FunctionalInterface
public interface IAsyncRequestMatcher {

   /**
    * Tries to match a given route with the items in a list of queued jobs.
    *
    * <p>
    * Developers should treat this method as a rule to identify whether an asynchronous job request
    * ({@code request}) matches any existing queued asynchronous job (in {@code queued}) and return
    * its identifier if a match is found.
    * </p>
    *
    * @param request an object representing the current request to match.
    * @param queued a collection of asynchronous requests queued for execution.
    * @return the identifier of the matched request or {@code null} if no queued request matches the
    *         current request, according to the rule implemented by this matcher.
    */
   UUID match(AsyncRequest request, Map<UUID, AsyncRequest> queued);
}
