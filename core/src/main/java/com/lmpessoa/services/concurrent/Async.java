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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.lmpessoa.services.internal.concurrent.DefaultRequestMatcher;

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
 * By marking a method as asynchronous, upon calling that method, the engine will schedule the
 * method to be executed at a later time and make available for the client a link where it can check
 * the status of the asynchronous processing and retrieve the result of such operation.The link can
 * be checked as many times desired until the result is available.
 * </p>
 *
 * <p>
 * Also, by default, each and every method call marked with {@code @Async} will be accepted and
 * handled by the engine. Developers may change the strategy used to accept or reject new request
 * using either property defined in this annotation. {@code reject} accepts two additional values:
 *
 * <ul>
 * <li>{@code SAME_PATH} in which all subsequent requests for the same path (including HTTP method)
 * will be rejected if another request is being processed, and</li>
 * <li>{@code SAME_CONTENT} which considers not only path and HTTP method but also whether any
 * content object from both requests are equivalent;</li>
 * <li>{@code SAME_IDENTITY} is the same as the {@code SAME_PATH} rule except that it also considers
 * if the request was made by the same user (if identified); and</li>
 * <li>{@code SAME_REQUEST} is the same as {@code SAME_CONTENT} and {@code SAME_IDENTITY}
 * combined.</li>
 * </ul>
 *
 * Developers requiring more control whether a new asynchronous request should be accepted/rejected
 * may instead provide an class implementing {@link IAsyncRequestMatcher} to the {@code rejectWith}
 * property.
 * </p>
 *
 * <p>
 * Each client requesting an operation rejected by the engine will receive the same link for
 * checking the result of the asynchronous operation. This link may also be used to abort the
 * execution of the asynchronous operation if the given path is called with the {@code DELETE} HTTP
 * method. However, if an identified user makes the request for the asynchronous operation, only
 * that same user will be allowed to abort the operation.
 * </p>
 *
 * <p>
 * If an entire class is marked with {@code @Async} then each and every method call in that class
 * will be executed asynchronously unless marked with the {@link NotAsync} annotation.
 * </p>
 *
 * <p>
 * For deployers, note that the execution of asynchronous methods shares the same thread pool used
 * to handle request received by the application, thus if limited it should be taken into
 * consideration that too many asynchronous methods being called concurrently may block the server
 * ability to respond to newer request. In this case, we recommend also setting a different limit
 * for asynchronous tasks as it will create a separate poll for these calls.
 * </p>
 *
 * @see NotAsync
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface Async {

   AsyncReject reject() default AsyncReject.DEFAULT;

   Class<? extends IAsyncRequestMatcher> rejectWith() default DefaultRequestMatcher.class;

}
