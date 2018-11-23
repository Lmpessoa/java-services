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
package com.lmpessoa.services.core.validating;

import static com.lmpessoa.services.core.services.Reuse.ALWAYS;

import java.lang.reflect.Method;

import com.lmpessoa.services.core.services.Service;

/**
 * Provides means to validate data on applications.
 * <p>
 * Applications are not expected to use instances of validation services directly. Instead,
 * published methods and content object should use constraint validation annotations which will be
 * evaluated during the execution of calls to the paths of the application.
 * </p>
 */
@Service(reuse = ALWAYS)
public interface IValidationService {

   /**
    * Validates the constraints on the given object.
    *
    * @param object the object to be validated.
    * @return a set of errors found while performing the validation, or {@code null} if no errors
    *         were found.
    */
   ErrorSet validate(Object object);

   /**
    * Validates the constraints placed on the parameters of the given method.
    *
    * @param object the object instance used to invoke the method.
    * @param method the method to be validated.
    * @param paramValues the arguments to be used to invoke the method.
    * @return a set of errors found while performing the validation, or {@code null} if no errors
    *         were found.
    */
   ErrorSet validateParameters(Object object, Method method, Object[] paramValues);

   /**
    * Validates the return value constraints of the given method.
    *
    * @param object the object instance used to invoke the method.
    * @param method the method to be validated.
    * @param returnValue the value returned by the method invocation.
    * @return a set of errors found while performing the validation, or {@code null} if no errors
    *         were found.
    */
   ErrorSet validateReturnValue(Object object, Method method, Object returnValue);
}
