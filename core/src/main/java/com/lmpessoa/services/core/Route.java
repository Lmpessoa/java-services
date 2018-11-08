/*
 * A light and easy engine for developing web APIs and microservices.
 * Copyright (c) 2017 Leonardo Pessoa
 * http://github.com/lmpessoa/java-services
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
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Enables a resource or end-point to be associated with a non-conventional route.
 * <p>
 * Routes defined through this annotation will still be checked for consistency (i.e. right number
 * of variables) and uniqueness in its context.
 * </p>
 *
 * <p>
 * Using the <code>@Route</code> annotation also enables route variables to be constrained to an
 * length that can be specified around parenthesis after the name of the route variable type. The
 * following list presents the accepted forms in which the size of a route variable segment can be
 * constrained.
 * </p>
 * <ul>
 * <li><strong>xxx</strong> - accepts a value of any size</li>
 * <li><strong>xxx(6)</strong> - accepts only values with the exact size of 6 characters</li>
 * <li><strong>xxx(6..)</strong> - accepts only values at least 6 characters long</li>
 * <li><strong>xxx(..9)</strong> - accepts only values at most 9 characters long</li>
 * <li><strong>xxx(6..9)</strong> - accepts only values between 6 and 9 characters long</li>
 * </ul>
 *
 * <p>
 * Note however that some route types might have a fixed length and will not support custom
 * specification of length constraints.
 * </p>
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface Route {

   String value();
}
