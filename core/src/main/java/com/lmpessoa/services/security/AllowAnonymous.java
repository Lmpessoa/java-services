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
package com.lmpessoa.services.security;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Identifies a method or type that can be accessed by anonymous users.
 *
 * <p>
 * Methods marked by this annotation will ignore any further requirement for authorisation before
 * allowing a request to be fulfilled, even allowing non-identified users to call such methods.
 * </p>
 *
 * <p>
 * If a resource class is marked with this annotation, then every method in it will be callable
 * without a user identity. Note that this even ignores any possible {@link Authorize} marks in
 * them. To correctly protect a single method of a resource class it is sufficient to mark that
 * given method with the required {@code Authorize} annotation; there is no need to apply any
 * annotations to the resource class itself.
 * </p>
 *
 * @see Authorize
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface AllowAnonymous {}
