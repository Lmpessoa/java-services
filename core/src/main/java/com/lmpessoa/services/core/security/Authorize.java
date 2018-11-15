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
package com.lmpessoa.services.core.security;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Identifies a method that can only be accessed by an authorised user.
 * <p>
 * Methods marked with this annotation are ensured by the engine they can only be accessed by a
 * valid user. Resource classes can also be marked with this annotation meaning each method of the
 * marked class will require the same authorisation specified at the class level. If method and
 * class are both marked with {@code Authorize} the user must fulfil both requirements to be able
 * to access the given method.
 * </p>
 * <p>
 * The {@code Authorize} annotation may require users to have certain roles or pass a given policy
 * in order to be allowed to call the annotated method. If the annotation does not specify a role or
 * policy, the engine will only ensure the request was made by an identified user.
 * </p>
 * <p>
 * The requirement for a method of an annotated resource class can be lifted by applying the
 * {@link AllowAnonymous} annotation to the desired method.
 * </p>
 */
@Retention(RUNTIME)
@Target({ METHOD, TYPE })
public @interface Authorize {

   /**
    * The list of roles an identity must have in order to be authorised to access a method protected by
    * this annotation.
    */
   String[] roles() default {};

   /**
    * The name of the policy an identity must fulfill in order to be authorised to access a method
    * protected by this annotation.
    */
   String policy() default "";
}
