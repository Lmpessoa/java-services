/*
 * Copyright (c) 2017 Leonardo Pessoa
 * http://github.com/lmpessoa/java-services
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
package com.lmpessoa.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

public final class ClassUtils {

   public static Method[] findMethods(Class<?> clazz, Predicate<? super Method> predicate) {
      return Arrays.stream(clazz.getMethods()).filter(predicate).toArray(Method[]::new);
   }

   @SuppressWarnings("unchecked")
   public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... parameterTypes) {
      Objects.requireNonNull(clazz);
      return (Constructor<T>) Arrays.stream(clazz.getConstructors())
               .filter(c -> Arrays.equals(parameterTypes, c.getParameterTypes()))
               .findFirst()
               .orElse(null);
   }

   public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
      Objects.requireNonNull(clazz);
      Objects.requireNonNull(methodName);
      return Arrays.stream(clazz.getMethods())
               .filter(m -> methodName.equals(m.getName()))
               .filter(m -> Arrays.equals(parameterTypes, m.getParameterTypes()))
               .findFirst()
               .orElse(null);
   }

   public static Method getDeclaredMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
      Objects.requireNonNull(clazz);
      Objects.requireNonNull(methodName);
      return Arrays.stream(clazz.getDeclaredMethods())
               .filter(m -> methodName.equals(m.getName()))
               .filter(m -> Arrays.equals(parameterTypes, m.getParameterTypes()))
               .findFirst()
               .orElse(null);
   }

   public static boolean isConcreteClass(Class<?> clazz) {
      Objects.requireNonNull(clazz);
      return !clazz.isArray() && !clazz.isEnum() && !clazz.isInterface() && !clazz.isPrimitive()
               && !Modifier.isAbstract(clazz.getModifiers());
   }

   private ClassUtils() {
      // Does nothing
   }

}
