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

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

abstract class ConstrainedExecutable<T extends Executable> extends ConstrainedElement<T> {

   private final ConstrainedCrossParameter crossParams;
   private final ConstrainedReturnValue returnValue;

   private Set<ConstrainedParameter> params;

   ConstrainedExecutable(T exec) {
      super(exec);
      this.crossParams = new ConstrainedCrossParameter(this);
      this.returnValue = new ConstrainedReturnValue(this);
   }

   @Override
   String getName() {
      return getElement().getName();
   }

   @Override
   boolean hasConstraints() {
      return !getParameters().isEmpty() || getCrossParameters().hasConstraints() || getReturnValue().hasConstraints();
   }

   @Override
   Map<Class<?>, Set<ConstraintAnnotation>> getConstraints() {
      return Collections.emptyMap();
   }

   @Override
   Class<?> getEnclosingType() {
      return getElement().getDeclaringClass();
   }

   ConstrainedReturnValue getReturnValue() {
      return returnValue;
   }

   ConstrainedCrossParameter getCrossParameters() {
      return crossParams;
   }

   Set<ConstrainedParameter> getParameters() {
      if (params == null) {
         Set<ConstrainedParameter> result = new HashSet<>();
         for (Parameter param : getElement().getParameters()) {
            result.add(ConstrainedElement.of(param));
         }
         params = Collections.unmodifiableSortedSet(new TreeSet<>(result));
      }
      return params;
   }
}
