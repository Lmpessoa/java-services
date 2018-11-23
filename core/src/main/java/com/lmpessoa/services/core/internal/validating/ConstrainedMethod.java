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
package com.lmpessoa.services.core.internal.validating;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class ConstrainedMethod extends ConstrainedExecutable<Method> {

   static boolean isGetter(Method method) {
      Class<?> returnType = method.getReturnType();
      String name = method.getName();
      return !Modifier.isStatic(method.getModifiers()) && returnType != void.class
               && method.getParameterCount() == 0 && (name.matches("get[A-Z].*")
                        || returnType == boolean.class && name.matches("is[A-Z].*"));
   }

   ConstrainedMethod(Method method) {
      super(method);
   }

   boolean isGetter() {
      return isGetter(getElement());
   }

   String getPropertyName() {
      if (isGetter()) {
         String name = getName();
         name = name.substring(name.startsWith("get") ? 3 : 2);
         return Character.toLowerCase(name.charAt(0)) + name.substring(1);
      }
      return null;
   }
}
