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
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.validation.Valid;

abstract class ConstrainedContainer<T extends AnnotatedElement> extends ConstrainedElement<T> {

   private static final List<Class<?>> COLLECTIONS = Arrays.asList(Optional.class, List.class, Iterable.class,
            Map.class);

   private Set<ConstrainedTypeArgument> typeArgs;

   ConstrainedContainer(T element) {
      super(element);
   }

   @Override
   boolean hasConstraints() {
      return !getConstraints().isEmpty() || mustBeValid()
               || getTypeArguments().stream().anyMatch(ConstrainedTypeArgument::hasConstraints);
   }

   boolean mustBeValid() {
      return getElement().isAnnotationPresent(Valid.class);
   }

   abstract AnnotatedType getAnnotatedType();

   abstract Class<?> getElementType();

   Class<?> getContainerType() {
      final Class<?> clazz = getElementType();
      if (clazz == null) {
         return null;
      }
      return COLLECTIONS.stream().filter(c -> c.isAssignableFrom(clazz)).findFirst().orElse(null);
   }

   Set<ConstrainedTypeArgument> getTypeArguments() {
      if (typeArgs == null) {
         AnnotatedType type = getAnnotatedType();
         Set<ConstrainedTypeArgument> result = new TreeSet<>();
         if (type instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType aptype = (AnnotatedParameterizedType) type;
            AnnotatedType[] actualTypes = aptype.getAnnotatedActualTypeArguments();
            for (int i = 0; i < actualTypes.length; ++i) {
               AnnotatedType actualType = actualTypes[i];
               ConstrainedTypeArgument t = new ConstrainedTypeArgument(this, actualType, i);
               if (t.hasConstraints()) {
                  result.add(t);
               }
            }
         }
         typeArgs = Collections.unmodifiableSet(result);
      }
      return typeArgs;
   }
}
