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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Identifies methods that should be executed asynchronously.
 *
 * <p>
 * Usually user agents usually drop waiting for a server response after 20 seconds on average.
 * Unfortunately some methods may take considerable time to return their effective result and user
 * agents are most likely to stop waiting before the result is actually ready.
 * </p>
 *
 * <p>
 * By marking a method (or a whole resource class) as asynchronous, upon calling the method, the
 * user agent is given another link where it can check back later if the method execution has
 * finished and its effective result once completed. The link can be checked as many times desired
 * until the result is available.
 * </p>
 *
 * <p>
 * If an entire class is marker <code>@Async</code> each and every method call in that class will be
 * executed asynchronously.
 * </p>
 *
 * <p>
 * For deployers, note that the execution of asynchronous methods shares the same thread pool used
 * to handle request received by the application, thus if limited it should be taken into
 * consideration that too many asynchronous methods being called concurrently may block the server
 * ability to respond to newer request.
 * </p>
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Async {}
