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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.GroupSequence;
import javax.validation.groups.Default;

import com.lmpessoa.services.util.ClassUtils;

final class ConstrainedType extends ConstrainedElement<Class<?>> {

   private Optional<ConstrainedType> superClass;
   private Set<ConstrainedMethod> getters;
   private Set<ConstrainedField> fields;

   ConstrainedType(Class<?> clazz) {
      super(clazz);
   }

   @Override
   String getName() {
      return getElement().getSimpleName();
   }

   @Override
   boolean hasConstraints() {
      return getElement().isAnnotationPresent(GroupSequence.class) || !getConstraints().isEmpty()
               || getFields().stream().anyMatch(ConstrainedField::hasConstraints)
               || getGetterMethods().stream().anyMatch(ConstrainedMethod::hasConstraints);
   }

   @Override
   Class<?> getEnclosingType() {
      return getElement();
   }

   Set<Class<?>> getDefaultSequence() {
      GroupSequence sequence = getElement().getAnnotation(GroupSequence.class);
      if (sequence == null) {
         return Collections.singleton(Default.class);
      }
      return Arrays.asList(sequence.value()).stream().collect(
               Collectors.toCollection(LinkedHashSet::new));
   }

   Optional<ConstrainedType> getSuperclass() {
      if (superClass == null) {
         if (getElement().getSuperclass() != Object.class) {
            superClass = Optional.of(ConstrainedElement.of(getElement().getSuperclass()));
         } else {
            superClass = Optional.empty();
         }
      }
      return superClass;
   }

   Set<ConstrainedField> getFields() {
      if (fields == null) {
         Set<ConstrainedField> result = new HashSet<>();
         result.addAll(Arrays.stream(getElement().getDeclaredFields())
                  .filter(f -> !Modifier.isStatic(f.getModifiers()))
                  .map(ConstrainedElement::of)
                  .collect(Collectors.toSet()));
         Optional<ConstrainedType> parent = getSuperclass();
         parent.ifPresent(p -> result.addAll(p.getFields()));
         fields = Collections.unmodifiableSet(result);
      }
      return fields;
   }

   Set<ConstrainedMethod> getGetterMethods() {
      if (getters == null) {
         Set<ConstrainedMethod> result = new HashSet<>();
         result.addAll(Arrays.stream(getElement().getDeclaredMethods())
                  .filter(ConstrainedMethod::isGetter)
                  .map(ConstrainedElement::of)
                  .filter(ConstrainedMethod::hasConstraints)
                  .collect(Collectors.toSet()));
         Optional<ConstrainedType> parent = getSuperclass();
         parent.ifPresent(p -> result.addAll(p.getGetterMethods()));
         getters = Collections.unmodifiableSet(result);
      }
      return getters;
   }

   Optional<ConstrainedMethod> getMethod(String methodName, Class<?>... paramTypes) {
      Method method = ClassUtils.getMethod(getElement(), methodName, paramTypes);
      return method == null ? Optional.empty() : Optional.of(ConstrainedElement.of(method));
   }
}
