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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Class used to hold several useful methods when dealing with classes.
 */
public final class ClassUtils {

   /**
    * Finds all public methods in a given class which match the given predicate.
    *
    * @param clazz the class to search for methods.
    * @param predicate the condition used to evaluate methods.
    * @return an array of all methods that match the given predicate. Note that the list may be empty
    * if no methods match the given predicate.
    */
   public static Method[] findMethods(Class<?> clazz, Predicate<? super Method> predicate) {
      return Arrays.stream(clazz.getMethods()).filter(predicate).toArray(Method[]::new);
   }

   /**
    * Returns a <code>Constructor</code> object that reflects the specified public constructor of the
    * given class.
    *
    * <p>
    * This method behaves exactly like {@link Class#getConstructor(Class...)} except that if no such
    * constructor exists, this method will return null instead of throwing an exception.
    * </p>
    *
    * @param clazz the class from which to find the constructor.
    * @param parameterTypes the list of parameter types of the desired constructor.
    * @return the <code>Constructor</code> object of the public constructor that matches the specified
    * list of parameter types.
    */
   @SuppressWarnings("unchecked")
   public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... parameterTypes) {
      Objects.requireNonNull(clazz);
      return (Constructor<T>) Arrays.stream(clazz.getConstructors())
               .filter(c -> Arrays.equals(parameterTypes, c.getParameterTypes()))
               .findFirst()
               .orElse(null);
   }

   /**
    * Returns a <code>Method</code> object that reflects the specified public method of the given
    * class.
    *
    * <p>
    * This method behaves exactly like {@link Class#getMethod(String, Class...)} excepts that if no
    * such method exists, this method will return null instead of throwing an exception.
    * </p>
    *
    * @param clazz the class from which to find the method.
    * @param methodName the name of the desired method.
    * @param parameterTypes the list of parameter types of the desired method.
    * @return the <code>Method</code> object of the public method that matches the specified name and
    * list of parameter types.
    */
   public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
      Objects.requireNonNull(clazz);
      Objects.requireNonNull(methodName);
      return Arrays.stream(clazz.getMethods())
               .filter(m -> methodName.equals(m.getName()))
               .filter(m -> Arrays.equals(parameterTypes, m.getParameterTypes()))
               .findFirst()
               .orElse(null);
   }

   /**
    * Returns a <code>Method</code> object that reflects the specified declared method of the given
    * class.
    *
    * <p>
    * This method extends the behaviour of {@link Class#getDeclaredMethod(String, Class...)} in that if
    * no such method exists, this method will return null instead of throwing an exception. Also, since
    * this method is used to access a method otherwise invisible to the calling class, if a
    * <code>Method</code> object is returned, it is already accessible through reflection.
    * </p>
    *
    * @param clazz the class from which to find the method.
    * @param methodName the name of the desired method.
    * @param parameterTypes the list of parameter types of the desired method.
    * @return the <code>Method</code> object of the declared method that matches the specified name and
    * list of parameter types.
    */
   public static Method getDeclaredMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
      Objects.requireNonNull(clazz);
      Objects.requireNonNull(methodName);
      Method result = Arrays.stream(clazz.getDeclaredMethods())
               .filter(m -> methodName.equals(m.getName()))
               .filter(m -> Arrays.equals(parameterTypes, m.getParameterTypes()))
               .findFirst()
               .orElse(null);
      return result;
   }

   /**
    * Returns whether the given class is a concrete class.
    *
    * <p>
    * A concrete class is an actual class (not an array, enum, primitive, interface, etc.) which is not
    * abstract and thus can be instantiated.
    * </p>
    *
    * @param clazz the type to check.
    * @return <code>true</code> if the type is a concrete class, <code>false</code> otherwise.
    */
   public static boolean isConcreteClass(Class<?> clazz) {
      Objects.requireNonNull(clazz);
      return !clazz.isArray() && !clazz.isEnum() && !clazz.isInterface() && !clazz.isPrimitive()
               && !Modifier.isAbstract(clazz.getModifiers());
   }

   /**
    * Scans the project the given class belongs to and returns a list of all classes that exist in the
    * same project as the given class.
    *
    * <p>
    * The returned list will contain all names of the existing classes in the project, not the
    * {@link Class} objects for the classes themselves. Any code using this method is responsible for
    * converting the names into class objects as they see fit.
    * </p>
    *
    * <p>
    * One must also note that this list will contain both public and non-public classes in the project
    * of the given class. Any distinction between public and non-public classes must also be performed
    * by the code calling this method.
    * </p>
    *
    * @param clazz a class in the project to be scanned.
    * @return a list of all classes that exist in the same project as the given class.
    * @throws IOException if there is an error while reading classes from a JAR file.
    */
   public static Collection<String> scanInProjectOf(Class<?> clazz) throws IOException {
      String location = findClassLocation(clazz);
      return location.startsWith("jar:") ? findClassesInJar(location.substring(9))
               : findClassesInPath(location.substring(5) + "/", "");
   }

   private static String findClassLocation(Class<?> clazz) {
      String pathOfClass = File.separator + clazz.getName().replaceAll("\\.", File.separator) + ".class";
      String location = clazz.getResource(pathOfClass).toString();
      return location.substring(0, location.length() - pathOfClass.length());
   }

   private static Collection<String> findClassesInJar(String location) throws IOException {
      List<String> result = new ArrayList<>();
      try (JarFile jar = new JarFile(location.substring(9, location.length() - 1))) {
         Enumeration<JarEntry> entries = jar.entries();
         while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String className = entry.getName().replace('/', '.');
            className = className.substring(0, className.length() - 6);
            result.add(className);
         }
      }
      return result;
   }

   private static Collection<String> findClassesInPath(String root, String currentPackage) {
      List<String> result = new ArrayList<>();
      File currentRoot = new File(root + currentPackage.replace('.', '/'));
      for (File entry : currentRoot.listFiles()) {
         if (entry.isDirectory()) {
            String newPackage = (currentPackage.length() > 0 ? currentPackage + '.' : "") + entry.getName();
            result.addAll(findClassesInPath(root, newPackage));
         } else {
            String className = entry.getName();
            className = className.substring(0, className.length() - 6);
            result.add((currentPackage.length() > 0 ? currentPackage + '.' : "") + className);
         }
      }
      return result;
   }

   private ClassUtils() {
      // Does nothing
   }

}
