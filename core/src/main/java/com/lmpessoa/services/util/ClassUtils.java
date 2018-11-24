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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Class used to hold several useful methods when dealing with classes.
 */
public final class ClassUtils {

   /**
    * Ensures a class is not a primitive class.
    *
    * <p>
    * If the given class is a primitive, its non-primitive version will be returned instead. Other
    * classes will remain unchanged.
    * </p>
    *
    * @param clazz the class to ensure not to be a primitive class.
    * @return the class ensured not to be a primitive class.
    */
   public static Class<?> box(Class<?> clazz) {
      if (clazz == byte.class) {
         return Byte.class;
      } else if (clazz == short.class) {
         return Short.class;
      } else if (clazz == int.class) {
         return Integer.class;
      } else if (clazz == long.class) {
         return Long.class;
      } else if (clazz == boolean.class) {
         return Boolean.class;
      } else if (clazz == float.class) {
         return Float.class;
      } else if (clazz == double.class) {
         return Double.class;
      } else if (clazz == char.class) {
         return Character.class;
      }
      return clazz;
   }

   /**
    * Ensures classes in a list are not primitive classes.
    * <p>
    * If any given class is a primitive, its non-primitive version will be returned instead. Other
    * classes will remain unchanged.
    * </p>
    *
    * @param classes the classes to ensure not to be primitive.
    * @return the classes with primitives replaces by their non-primitive versions.
    */
   public static Class<?>[] box(Class<?>... classes) {
      return Arrays.stream(classes).map(ClassUtils::box).toArray(Class<?>[]::new);
   }

   /**
    * Converts the given string value to a value of the given type.
    * <p>
    * Values can only be converted to arrays, primitive types and types that contain a static
    * {@code #valueOf(String)} method. The type of array elements must also obey this rule.
    * </p>
    *
    * @param value the value to be converted.
    * @param type the type to convert the value to.
    * @return the value converted to the given type
    * @throws IllegalArgumentException if the value cannot be converted to the given type.
    */
   @SuppressWarnings("unchecked")
   public static <T> T cast(String value, Class<T> type) {
      if (value == null) {
         if (type.isPrimitive()) {
            switch (type.getName()) {
               case "boolean":
                  value = "false";
                  break;
               case "char":
                  value = "\0";
                  break;
               default:
                  value = "0";
                  break;
            }
         } else {
            return null;
         }
      }
      if (type == String.class) {
         return (T) value;
      }
      if (type.isArray()) {
         String[] values = value.split(",");
         if (value.isEmpty()) {
            values = new String[0];
         }
         Class<?> atype = type.getComponentType();
         T result = (T) Array.newInstance(atype, values.length);
         for (int i = 0; i < values.length; ++i) {
            Object cvalue = values[i].trim();
            if (atype != String.class) {
               cvalue = cast(values[i].trim(), atype);
            }
            Array.set(result, i, cvalue);
            if (cvalue != null) {
               Array.set(result, i, cvalue);
            }
         }
         return result;
      }
      return internalCast(value, type);
   }

   /**
    * Finds all public methods in a given class which match the given predicate.
    *
    * @param clazz the class to search for methods.
    * @param predicate the condition used to evaluate methods.
    * @return an array of all methods that match the given predicate. Note that the list may be
    *         empty if no methods match the given predicate.
    */
   public static Method[] findMethods(Class<?> clazz, Predicate<? super Method> predicate) {
      return Arrays.stream(clazz.getMethods()).filter(predicate).toArray(Method[]::new);
   }

   /**
    * Returns a list of <code>Constructor</code> objects that match the given predicate.
    *
    * @param clazz the class from which to find the constructors.
    * @param predicate a predicate used to filter constructors.
    * @return a list of <code>Constructor</code> objects that match the given predicate or an empty
    *         list if none match the predicate.
    */
   public static Constructor<?>[] findConstructor(Class<?> clazz,
      Predicate<? super Constructor<?>> predicate) {
      return Arrays.stream(clazz.getConstructors()).filter(predicate).toArray(
               Constructor<?>[]::new);
   }

   /**
    * Returns a <code>Constructor</code> object that reflects the specified public constructor of
    * the given class.
    *
    * <p>
    * This method behaves exactly like {@link Class#getConstructor(Class...)} except that if no such
    * constructor exists, this method will return null instead of throwing an exception.
    * </p>
    *
    * @param clazz the class from which to find the constructor.
    * @param parameterTypes the list of parameter types of the desired constructor.
    * @return the <code>Constructor</code> object of the public constructor that matches the
    *         specified list of parameter types.
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
    * @return the <code>Method</code> object of the public method that matches the specified name
    *         and list of parameter types.
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

   public static Field getField(Class<?> clazz, String fieldName) {
      Objects.requireNonNull(clazz);
      Objects.requireNonNull(fieldName);
      Class<?> arg = clazz;
      while (arg != Object.class) {
         for (Field f : arg.getDeclaredFields()) {
            if (fieldName.equals(f.getName())) {
               return f;
            }
         }
         arg = arg.getSuperclass();
      }
      return null;
   }

   /**
    * Returns whether the given class is a concrete class.
    *
    * <p>
    * A concrete class is an actual class (not an array, enum, primitive, interface, etc.) which is
    * not abstract and thus can be instatiated.
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
    * Scans the project the given class belongs to and returns a list of all classes that exist in
    * the same project as the given class.
    *
    * <p>
    * The returned list will contain all names of the existing classes in the project, not the
    * {@link Class} objects for the classes themselves. Any code using this method is responsible
    * for converting the names into class objects as they see fit.
    * </p>
    *
    * <p>
    * One must also note that this list will contain both public and non-public classes in the
    * project of the given class. Any distinction between public and non-public classes must also be
    * performed by the code calling this method.
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

   public static String findLocation(Class<?> clazz) {
      if (clazz == null) {
         return null;
      }
      String pathOfClass = File.separator + clazz.getName().replaceAll("\\.", File.separator)
               + ".class";
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
            String newPackage = (currentPackage.length() > 0 ? currentPackage + '.' : "")
                     + entry.getName();
            result.addAll(findClassesInPath(root, newPackage));
         } else {
            String className = entry.getName();
            className = className.substring(0, className.length() - 6);
            result.add((currentPackage.length() > 0 ? currentPackage + '.' : "") + className);
         }
      }
      return result;
   }

   @SuppressWarnings("unchecked")
   private static <T> T internalCast(String value, Class<T> type) {
      Class<?> atype = ClassUtils.box(type);
      if (atype == Boolean.class) {
         Object result = "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
         return (T) result;
      }
      if (atype == Character.class) {
         if (value.length() != 1) {
            throw new IllegalArgumentException("Value must have exactly one character");
         }
         return (T) Character.valueOf(value.charAt(0));
      }
      if (atype.isEnum()) {
         for (Field f : atype.getDeclaredFields()) {
            if (f.isEnumConstant() && f.getName().equalsIgnoreCase(value)) {
               try {
                  return (T) f.get(null);
               } catch (IllegalArgumentException | IllegalAccessException e) {
                  // Shall not happend but...
               }
            }
         }
      }
      if (atype == UUID.class) {
         return (T) UUID.fromString(value);
      }
      Method valueOf;
      try {
         valueOf = atype.getMethod("valueOf", String.class);
      } catch (NoSuchMethodException e) {
         throw new IllegalArgumentException(e);
      }
      if (!Modifier.isStatic(valueOf.getModifiers())) {
         throw new IllegalArgumentException(
                  new NoSuchMethodException("Method 'valueOf' is not static"));
      }
      try {
         Object cvalue = valueOf.invoke(null, value);
         if (cvalue != null && atype != type) {
            Method primValue = cvalue.getClass().getMethod(type.getName() + "Value");
            cvalue = primValue.invoke(cvalue);
         }
         return (T) cvalue;
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
         throw new IllegalArgumentException(e);
      }
   }

   private ClassUtils() {
      // Does nothing
   }
}
