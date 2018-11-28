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
package com.lmpessoa.services;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Enables a resource or end-point to be associated with a non-conventional route.
 *
 * <p>
 * The {@code @Route} annotation can be applied to both resource classes and methods to alter the
 * route produced to reach them. Thus it is possible to have multiple resource classes responding
 * for the same base path. Consider the following example:
 * </p>
 *
 * <pre>
 * public class SampleResource {
 *
 *    public String get() {
 *       // ...
 *    }
 * }
 *
 * &#64;Route("sample")
 * public class AnotherResource {
 *
 *    public void post() {
 *       // ...
 *    }
 * }
 * </pre>
 *
 * <p>
 * In this example, a request for {@code 'GET /sample'} will be responded by the
 * {@code SampleResource} class while {@code 'POST /sample'} will be responde by the
 * {@code AnotherResource} class.
 * </p>
 *
 * <p>
 * In methods, the {@code @Route} annotation can change how arguments are meant to be read from the
 * path as well as creating a sub-path for the route in order to distinguish paths with the same set
 * or arguments, for example:
 * </p>
 *
 * <pre>
 * public class SampleResource {
 *
 *    &#64;Route("test/{s}/{i}")
 *    public String get(int i, String s) {
 *       // will respond for 'GET /sample/test/abc/7' with i = 7 and s = "abc"
 *    }
 * }
 * </pre>
 *
 * <p>
 * Routes defined this way will still be checked for consistency (i.e. right number and type of
 * variables present) and uniqueness in its context.
 * </p>
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface Route {

   String value();
}
