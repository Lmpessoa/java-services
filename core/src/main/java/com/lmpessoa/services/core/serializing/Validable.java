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
package com.lmpessoa.services.core.serializing;

/**
 * Establishes the common interface for validating deserialised object.
 *
 * <p>
 * The adoption of this interface is not actually mandatory for object to be validated but may help
 * identify by other means classes whose objects are validated and the signature of the method that
 * is used in this validation process.
 * </p>
 */
public interface Validable {

   /**
    * Validates this object.
    * <p>
    * Classes implementing this method must perform a validation of the object according to any
    * rules it sees fit and registering any errors in the given error list. Classes are not expected
    * to raise any exceptions from this method.
    * </p>
    *
    * @param errors an object that will hold the list of validation errors.
    */
   void validate(ErrorList errors);
}
