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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.validation.GroupSequence;
import javax.validation.groups.Default;

abstract class ConstrainedElement<T extends AnnotatedElement> {

   private static final Map<AnnotatedElement, ConstrainedElement<?>> elements = new HashMap<>();

   private final T element;

   private Map<Class<?>, Set<ConstraintAnnotation>> constraints;

   @Override
   public String toString() {
      return element.toString();
   }

   static ConstrainedExecutable<?> of(Executable exec) {
      if (exec instanceof Constructor) {
         return of((Constructor<?>) exec);
      }
      return of((Method) exec);
   }

   static ConstrainedConstructor of(Constructor<?> constructor) {
      return getOrDefault(constructor, () -> new ConstrainedConstructor(constructor));
   }

   static ConstrainedField of(Field field) {
      return getOrDefault(field, () -> new ConstrainedField(field));
   }

   static ConstrainedMethod of(Method method) {
      return getOrDefault(method, () -> new ConstrainedMethod(method));
   }

   static ConstrainedParameter of(Parameter param) {
      return getOrDefault(param, () -> new ConstrainedParameter(param));
   }

   static ConstrainedType of(Class<?> clazz) {
      return getOrDefault(clazz, () -> new ConstrainedType(clazz));
   }

   ConstrainedElement(T element) {
      this.element = element;
   }

   abstract String getName();

   abstract Class<?> getEnclosingType();

   boolean hasConstraints() {
      return !getConstraints().isEmpty();
   }

   Map<Class<?>, Set<ConstraintAnnotation>> getConstraints() {
      if (constraints == null) {
         GroupSequence gseq = getEnclosingType().getAnnotation(GroupSequence.class);
         Set<Class<?>> groups = gseq == null ? Collections.singleton(Default.class)
                  : Arrays.asList(gseq.value()).stream().collect(Collectors.toCollection(LinkedHashSet::new));

         Map<Class<?>, Set<ConstraintAnnotation>> result = new LinkedHashMap<>();
         groups.forEach(g -> result.put(g, new HashSet<>()));

         Set<ConstraintAnnotation> values = ConstraintAnnotation.of(getElement());
         values.forEach(c -> {
            Optional<Class<?>> firstGroup = groups.stream().filter(c.getGroups()::contains).findFirst();
            firstGroup.ifPresent(g -> result.get(g).add(c));
         });
         this.constraints = result.entrySet().stream().filter(e -> !e.getValue().isEmpty()).collect(
                  Collectors.toMap(Map.Entry::getKey, e -> Collections.unmodifiableSet(e.getValue())));
      }
      return constraints;
   }

   T getElement() {
      return element;
   }

   @SuppressWarnings("unchecked")
   private static <T extends ConstrainedElement<?>> T getOrDefault(AnnotatedElement element, Supplier<T> fallback) {
      if (!elements.containsKey(element)) {
         T fallbackValue = fallback.get();
         elements.put(element, fallbackValue);
      }
      return (T) elements.get(element);
   }
}
