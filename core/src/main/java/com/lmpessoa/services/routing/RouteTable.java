/*
 * A light and easy engine for developing web APIs and microservices.
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
package com.lmpessoa.services.routing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

final class RouteTable implements IRouteTable {

   private static final String ERROR = "Attempted to register ";
   private final Map<RoutePattern, Map<HttpMethod, MethodEntry>> endpoints = new HashMap<>();

   static String findArea(Class<?> clazz) {
      return "";
   }

   @Override
   public Collection<Exception> putAll(Collection<Class<?>> classes) {
      return putClasses(classes.stream().collect(Collectors.toMap(c -> c, RouteTable::findArea)));
   }

   @Override
   public Collection<Exception> putAll(String area, Collection<Class<?>> classes) {
      return putClasses(classes.stream().collect(Collectors.toMap(c -> c, c -> area)));
   }

   private Collection<Exception> putClasses(Map<Class<?>, String> classes) {
      List<Exception> exceptions = new ArrayList<>();
      synchronized (this) {
         for (Entry<Class<?>, String> entry : classes.entrySet()) {
            try {
               Class<?> clazz = entry.getKey();
               validateResourceClass(clazz);
               String area = entry.getValue();
               RoutePattern classPat = RoutePattern.build(area, clazz);
               for (Method method : clazz.getMethods()) {
                  putMethod(clazz, classPat, method, exceptions);
               }
            } catch (Exception e) {
               exceptions.add(e);
            }
         }
      }
      return Collections.unmodifiableCollection(exceptions);
   }

   private void putMethod(Class<?> clazz, RoutePattern classPat, Method method,
      List<Exception> exceptions) {
      try {
         if (method.getDeclaringClass() == Object.class) {
            return;
         }
         HttpMethod[] methodNames = findMethod(method);
         if (methodNames.length == 0) {
            return;
         }
         RoutePattern methodPat = RoutePattern.build(classPat, method);
         if (!endpoints.containsKey(methodPat)) {
            endpoints.put(methodPat, new HashMap<>());
         }
         Map<HttpMethod, MethodEntry> map = endpoints.get(methodPat);
         for (HttpMethod methodName : methodNames) {
            if (map.containsKey(methodName)) {
               throw new DuplicateMethodException(methodName, methodPat, clazz);
            }
            map.put(methodName, new MethodEntry(clazz, method, classPat.getVariableCount()));
         }
      } catch (Exception e) {
         exceptions.add(e);
      }
   }

   private HttpMethod[] findMethod(Method method) {
      Annotation[] methods = new Annotation[] { method.getAnnotation(HttpGet.class),
               method.getAnnotation(HttpPost.class), method.getAnnotation(HttpPut.class),
               method.getAnnotation(HttpPatch.class), method.getAnnotation(HttpDelete.class),
               method.getAnnotation(HttpOptions.class) };
      HttpMethod[] result = Arrays.stream(methods)
               .filter(Objects::nonNull)
               .map(a -> HttpMethod
                        .valueOf(a.annotationType().getSimpleName().substring(4).toUpperCase()))
               .toArray(HttpMethod[]::new);
      if (result.length > 0) {
         return result;
      }
      String methodName = method.getName();
      for (HttpMethod value : HttpMethod.values()) {
         if (value.name().toLowerCase().equals(methodName)) {
            return new HttpMethod[] { value };
         }
      }
      return new HttpMethod[0];
   }

   private void validateResourceClass(Class<?> clazz) {
      if (clazz.isArray()) {
         throw new IllegalArgumentException(
                  ERROR + "an array type: " + clazz.getComponentType() + "[]");
      }
      if (clazz.isAnnotation()) {
         throw new IllegalArgumentException(ERROR + "an annotation: " + clazz.getName());
      }
      if (clazz.isInterface()) {
         throw new IllegalArgumentException(ERROR + "an interface: " + clazz.getName());
      }
      if (clazz.isAnonymousClass()) {
         throw new IllegalArgumentException(ERROR + "an anonymous class: " + clazz.getName());
      }
      if (clazz.isEnum()) {
         throw new IllegalArgumentException(ERROR + "an enumeration: " + clazz.getName());
      }
      if (clazz.isPrimitive()) {
         throw new IllegalArgumentException(ERROR + "a primitive type: " + clazz.getName());
      }
      if (Modifier.isAbstract(clazz.getModifiers())) {
         throw new IllegalArgumentException(ERROR + "an abstract class: " + clazz.getName());
      }
   }

   boolean hasRoute(String route) throws ParseException {
      RoutePattern pat = new RoutePattern(RoutePatternParser.parse(route, RoutePattern.types));
      return endpoints.containsKey(pat);
   }

   HttpMethod[] listMethodsOf(String route) throws ParseException {
      RoutePattern pat = new RoutePattern(RoutePatternParser.parse(route, RoutePattern.types));
      Map<HttpMethod, MethodEntry> map = endpoints.get(pat);
      if (map == null) {
         return new HttpMethod[0];
      }
      return map.keySet().toArray(new HttpMethod[0]);
   }

   MethodEntry getRouteMethod(HttpMethod method, String route) throws ParseException {
      RoutePattern pat = new RoutePattern(RoutePatternParser.parse(route, RoutePattern.types));
      Map<HttpMethod, MethodEntry> map = endpoints.get(pat);
      return map.get(method);
   }
}
