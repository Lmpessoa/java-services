/*
 * Copyright (c) 2017 Leonardo Pessoa
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
package com.lmpessoa.services.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.lmpessoa.services.Internal;

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

   public static Constructor<?>[] findConstructor(Class<?> clazz, Predicate<? super Constructor<?>> predicate) {
      return Arrays.stream(clazz.getConstructors()).filter(predicate).toArray(Constructor<?>[]::new);
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
               .filter(m -> !m.isSynthetic())
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
      return Arrays.stream(clazz.getDeclaredMethods())
               .filter(m -> methodName.equals(m.getName()))
               .filter(m -> Arrays.equals(parameterTypes, m.getParameterTypes()))
               .filter(m -> !m.isSynthetic())
               .findFirst()
               .orElse(null);
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
      String location = findLocation(clazz);
      if (location == null) {
         return null;
      }
      if (location.startsWith("jar:")) {
         return findClassesInJar(location.substring(9, location.length() - 1));
      }
      return findClassesInPath(location.substring(5) + "/", "");
   }

   /**
    * Creates a new instance of the given class using the given argument list.
    *
    * <p>
    * Objects created with this method must have a constructor that matches exactly the type of the
    * arguments given. To use a constructor with specific argument types, use
    * {@link #newInstance(Class, Class[], Object...)}.
    * </p>
    *
    * <p>
    * If any error occurs while creating the object, this method will simply return null.
    * </p>
    *
    * @param clazz the class to be instantiated.
    * @param params the list of arguments to be used to create the instance.
    * @return the newly created object or <code>null</code> if it was not possible to create it.
    */
   public static <T> T newInstance(Class<T> clazz, Object... params) {
      Class<?>[] paramTypes = Arrays.stream(params).map(Object::getClass).toArray(Class<?>[]::new);
      return newInstance(clazz, paramTypes, params);
   }

   /**
    * Creates a new instance of the given class using the given argument list.
    *
    * <p>
    * Objects created with this method must have a constructor that matches the list of argument types
    * given. The values in the argument list may be subclasses of the effective argument types.
    * </p>
    *
    * <p>
    * If any error occurs while creating the object, this method will simply return null.
    * </p>
    *
    * @param clazz the class to be instantiated.
    * @param paramTypes the list of the argument types of the desired constructor.
    * @param params the list of arguments to be used to create the instance.
    * @return the newly created object or <code>null</code> if it was not possible to create it.
    */
   public static <T> T newInstance(Class<T> clazz, Class<?>[] paramTypes, Object... params) {
      try {
         Constructor<T> construct = clazz.getDeclaredConstructor(paramTypes);
         construct.setAccessible(true);
         return construct.newInstance(params);
      } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
         // Ignore for now
      }
      return null;
   }

   /**
    * Prevents access to an internal method.
    *
    * <p>
    * The <code>@Internal</code> annotation is useful only for documentation purposes and will be shown
    * in generated Javadoc. This, however, does not ensure the annotated method or classes cannot be
    * called outside the project they are declared. Calling this method as the first line of a method
    * creates a fence that ensures only methods declared in the same project can call the protected
    * method.
    * </p>
    *
    * @param class1 a class to compare if on the same project as the other
    * @param class2 a class to compare if on the same project as the other
    * @throws SecurityException if the caller method cannot access the called method.
    */
   public static void checkInternalAccess(Class<?> class1, Class<?> class2) {
      String calledLocation = new File(findLocation(class1)).getParent();
      String callerLocation = new File(findLocation(class2)).getParent();
      if (!calledLocation.equals(callerLocation)) {
         SecurityException ex = new AccessControlException("Cannot call an internal class");
         StackTraceElement[] stack = ex.getStackTrace();
         stack = Arrays.stream(stack) //
                  .filter(t -> t.getClassName() != ClassUtils.class.getName()) //
                  .toArray(StackTraceElement[]::new);
         ex.setStackTrace(stack);
         throw ex;
      }
   }

   /**
    * Prevents access to an internal method.
    *
    * <p>
    * The <code>@Internal</code> annotation is useful only for documentation purposes and will be shown
    * in generated Javadoc. This, however, does not ensure the annotated method or classes cannot be
    * called outside the project they are declared. Calling this method as the first line of a method
    * creates a fence that ensures only methods declared in the same project can call the protected
    * method.
    * </p>
    *
    * @throws SecurityException if the caller method cannot access the called method.
    */
   public static void checkInternalAccess() {
      try {
         StackTraceElement[] stack = Thread.currentThread().getStackTrace();
         checkInternalAccess(Class.forName(stack[2].getClassName()), Class.forName(stack[3].getClassName()));
      } catch (ClassNotFoundException e) {
         // Should not happen; ignore for now
      }
   }

   @Internal
   public static String findLocation(Class<?> clazz) {
      if (clazz == null) {
         return null;
      }
      String pathOfClass = File.separator + clazz.getName().replaceAll("\\.", File.separator) + ".class";
      String location = clazz.getResource(pathOfClass).toString();
      return location.substring(0, location.length() - pathOfClass.length());
   }

   private static Collection<String> findClassesInJar(String location) throws IOException {
      List<String> result = new ArrayList<>();
      try (JarFile jar = new JarFile(location)) {
         Enumeration<JarEntry> entries = jar.entries();
         while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String className = entry.getName().replace('/', '.');
            if (!className.endsWith(".class")) {
               continue;
            }
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
