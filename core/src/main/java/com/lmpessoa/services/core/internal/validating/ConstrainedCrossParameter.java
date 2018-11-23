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

import static javax.validation.ConstraintTarget.PARAMETERS;

import java.lang.reflect.Executable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class ConstrainedCrossParameter extends ConstrainedElement<Executable> {

   private final ConstrainedExecutable<?> parent;

   private Map<Class<?>, Set<ConstraintAnnotation>> constraints;

   ConstrainedCrossParameter(ConstrainedExecutable<?> parent) {
      super(parent.getElement());
      this.parent = parent;
   }

   @Override
   String getName() {
      return "<cross-parameter>";
   }

   @Override
   Executable getElement() {
      return parent.getElement();
   }

   @Override
   Class<?> getEnclosingType() {
      return parent.getEnclosingType();
   }

   @Override
   Map<Class<?>, Set<ConstraintAnnotation>> getConstraints() {
      if (constraints == null) {
         constraints = super.getConstraints().entrySet()
                  .stream()
                  .collect(Collectors.toMap(Map.Entry::getKey,
                           e -> e.getValue()
                                    .stream()
                                    .filter(a -> a.getValidationAppliesTo() == PARAMETERS)
                                    .collect(Collectors.toSet()),
                           (k1, k2) -> k1, LinkedHashMap::new));
         constraints = constraints.entrySet().stream().filter(e -> !e.getValue().isEmpty()).collect(
                  Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k1,
                           LinkedHashMap::new));
      }
      return constraints;
   }
}
