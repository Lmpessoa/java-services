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
 * Represents the built-in rules for rejection of a new async task.
 *
 * <p>
 * By default, all asynchronous tasks are accepted by the engine. However this may lead to duplicate
 * operations being executed, one after the other. This may be controlled by changing the rejection
 * rule for the asynchronous method using one of the given values in this enumeration to indicate a
 * queued task should be treated as similar to one being evaluated for rejection.
 * </p>
 *
 * <ul>
 * <li>{@code NEVER} is the default value and indicates a new asynchronous request will never be
 * rejected;</li>
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
 * <p>
 * Developers interested in using the {@code SAME_CONTENT} or {@code SAME_REQUEST} rules must ensure
 * their content classes implement correctly comparison of those instances through the
 * {@code #equals(Object)} method, otherwise they will behave as if not evaluating content.
 * </p>
 *
 * @see Async
 * @see AsyncRejectionRule
 */
public enum AsyncReject {
   /**
    * Indicates the engine should use the default rejection rule defined in the configuration of the
    * application.
    */
   DEFAULT,
   /**
    * Indicates that the engine should never reject a new asynchronous call.
    */
   NEVER,
   /**
    * Indicates that the engine should reject a new async call if another with the same path is
    * already queued for execution.
    */
   SAME_PATH,
   /**
    * Indicates that the engine should reject a new async call if another with the same path and
    * content object (if present) is already queued for execution. Developers interested in using
    * this rules must ensure their content classes implement correctly comparison of those instances
    * through the {@code #equals(Object)} method.
    */
   SAME_CONTENT,
   /**
    * Indicates that the engine should reject a new async call if another with the same path
    * requested by the same user is already queued for execution.
    */
   SAME_IDENTITY,
   /**
    * Indicates that the engine should reject a new async call if another with the same path and
    * content object (if present) requested by the same user is already queued for execution.
    * Developers interested in using this rules must ensure their content classes implement
    * correctly comparison of those instances through the {@code #equals(Object)} method.
    */
   SAME_REQUEST;
}
