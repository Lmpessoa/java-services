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
package com.lmpessoa.services.core.concurrent;

/**
 * Provides means to further configure asynchronous execution support by the application.
 *
 * <p>
 * Methods in this interface can be used to configure the behaviour of the asynchronous execution of
 * requests.
 * </p>
 */
public interface IAsyncOptions {

   void useFeedbackPath(String feedbackPath);

   /**
    * Declares the default rejection rule to be used for asynchronous calls if none is explicitly
    * declared.
    *
    * <p>
    * Use this method to provide a default rejection rule for all asynchronous methods (marked with
    * {@code @Async}) instead of through each individual instance of that annotation (using the
    * {@code reject} parameter).
    * </p>
    *
    * @param defaultValue the default rejection rule for asynchronous jobs.
    *
    * @see Async
    * @see AsyncReject
    */
   void useDefaultRejectionRule(AsyncReject defaultValue);

   /**
    * Defines the default route matcher to be used for asynchronous class if none is explicitly
    * declared.
    *
    * <p>
    * Use this method to provide a default route matcher for all asynchronous methods (marked with
    * {@code @Async}) instead of through each individual instance of that annotation (using the
    * {@code matchWith} parameter}).
    * </p>
    *
    * @param matcherClass the matcher class to be used for resolution of concurrent asynchronous
    *           jobs.
    *
    * @see @Async
    * @see IAsyncRequestMatcher
    */
   void useDefaultRouteMatcher(Class<? extends IAsyncRequestMatcher> matcherClass);
}
