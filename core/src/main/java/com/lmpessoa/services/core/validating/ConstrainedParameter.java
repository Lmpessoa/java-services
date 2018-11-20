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

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;

final class ConstrainedParameter extends ConstrainedContainer<Parameter> implements Comparable<ConstrainedParameter> {

   private final int index;

   @Override
   public int compareTo(ConstrainedParameter o) {
      return getIndex() - o.getIndex();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (!(obj instanceof ConstrainedParameter)) {
         return false;
      }
      return compareTo((ConstrainedParameter) obj) == 0;
   }

   @Override
   public int hashCode() {
      return getElement().hashCode();
   }

   ConstrainedParameter(Parameter param) {
      super(param);
      Parameter[] params = param.getDeclaringExecutable().getParameters();
      int value = -1;
      for (int i = 0; i < params.length; ++i) {
         if (params[i] == param) {
            value = i;
            break;
         }
      }
      this.index = value;
   }

   @Override
   String getName() {
      return getElement().getName();
   }

   @Override
   AnnotatedType getAnnotatedType() {
      return getElement().getAnnotatedType();
   }

   @Override
   Class<?> getElementType() {
      return getElement().getType();
   }

   @Override
   Class<?> getEnclosingType() {
      return getElement().getDeclaringExecutable().getDeclaringClass();
   }

   int getIndex() {
      return index;
   }
}
