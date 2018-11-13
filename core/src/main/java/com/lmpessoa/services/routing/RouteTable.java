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
package com.lmpessoa.services.routing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import com.lmpessoa.services.core.HttpDelete;
import com.lmpessoa.services.core.HttpGet;
import com.lmpessoa.services.core.HttpOptions;
import com.lmpessoa.services.core.HttpPatch;
import com.lmpessoa.services.core.HttpPost;
import com.lmpessoa.services.core.HttpPut;
import com.lmpessoa.services.core.NonResource;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.hosting.InternalServerError;
import com.lmpessoa.services.hosting.MethodNotAllowedException;
import com.lmpessoa.services.hosting.NotFoundException;
import com.lmpessoa.services.routing.content.Serializer;
import com.lmpessoa.services.services.IServiceMap;
import com.lmpessoa.services.services.NoSingleMethodException;
import com.lmpessoa.util.ClassUtils;

final class RouteTable implements IRouteTable {

   private static final Map<Class<?>, Function<String, ?>> specialCases;

   private final Map<RoutePattern, Map<HttpMethod, MethodEntry>> endpoints = new HashMap<>();
   private final RouteOptions options = new RouteOptions();
   private final IServiceMap serviceMap;
   private final Method serviceGetter;

   static {
      Map<Class<?>, Function<String, ?>> special = new HashMap<>();
      special.put(long.class, Long::valueOf);
      special.put(int.class, Integer::valueOf);
      special.put(short.class, Short::valueOf);
      special.put(byte.class, Byte::valueOf);
      special.put(float.class, Float::valueOf);
      special.put(double.class, Double::valueOf);
      special.put(boolean.class, Boolean::valueOf);
      specialCases = Collections.unmodifiableMap(special);
   }

   RouteTable(IServiceMap serviceMap) throws NoSuchMethodException {
      this.serviceGetter = serviceMap.getClass().getMethod("get", Class.class);
      this.serviceGetter.setAccessible(true);
      this.serviceMap = serviceMap;
   }

   @Override
   public Collection<Exception> put(Class<?> clazz) {
      return putAll(options.findArea(clazz), Arrays.asList(clazz));
   }

   @Override
   public Collection<Exception> putAll(Collection<Class<?>> classes) {
      return putClasses(classes.stream().filter(c -> options.findArea(c) != null).collect(
               Collectors.toMap(c -> c, options::findArea)));
   }

   @Override
   public Collection<Exception> putAll(String area, Collection<Class<?>> classes) {
      return putClasses(classes.stream().filter(c -> area != null).collect(Collectors.toMap(c -> c, c -> area)));
   }

   @Override
   public String findArea(String packageName) {
      return options.findArea(packageName);
   }

   boolean hasRoute(String route) throws ParseException {
      RoutePattern pat = new RoutePattern(RoutePatternParser.parse(route, options), null);
      return endpoints.containsKey(pat);
   }

   HttpMethod[] listMethodsOf(String route) throws ParseException {
      RoutePattern pat = new RoutePattern(RoutePatternParser.parse(route, options), null);
      Map<HttpMethod, MethodEntry> map = endpoints.get(pat);
      if (map == null) {
         return new HttpMethod[0];
      }
      return map.keySet().toArray(new HttpMethod[0]);
   }

   MethodEntry getRouteMethod(HttpMethod method, String route) throws ParseException {
      RoutePattern pat = new RoutePattern(RoutePatternParser.parse(route, options), null);
      Map<HttpMethod, MethodEntry> map = endpoints.get(pat);
      return map.get(method);
   }

   MatchedRoute matches(HttpMethod method, String path) {
      return matches(new HttpRequest() {

         @Override
         public String getMethod() {
            return method.name();
         }

         @Override
         public String getPath() {
            return path;
         }
      });
   }

   MatchedRoute matches(HttpRequest request) {
      boolean found = false;
      for (Entry<RoutePattern, Map<HttpMethod, MethodEntry>> entry : endpoints.entrySet()) {
         RoutePattern route = entry.getKey();
         Matcher matcher = route.getPattern().matcher(request.getPath());
         if (!matcher.find()) {
            continue;
         }
         found = true;
         HttpMethod method = HttpMethod.valueOf(request.getMethod());
         MethodEntry methodEntry = entry.getValue().get(method);
         if (methodEntry == null) {
            continue;
         }
         List<Class<?>> params = new ArrayList<>();
         Class<?> resourceClass = methodEntry.getResourceClass();
         Constructor<?> constructor = resourceClass.getConstructors()[0];
         params.addAll(Arrays.asList(constructor.getParameterTypes()));
         Method methodCall = methodEntry.getMethod();
         params.addAll(Arrays.asList(methodCall.getParameterTypes()));
         Object contentObject = null;
         if (methodEntry.getContentClass() != null) {
            params.remove(params.size() - 1);
            contentObject = parseContentBody(request, methodEntry.getContentClass());
         }
         List<Object> result = convertParams(matcher, params);
         if (methodEntry.getContentClass() != null) {
            result.add(contentObject);
         }
         return new MatchedRoute(methodEntry, result.toArray());
      }
      if (!found) {
         throw new NotFoundException();
      } else {
         throw new MethodNotAllowedException();
      }
   }

   IRouteOptions getOptions() {
      return options;
   }

   private Object parseContentBody(HttpRequest request, Class<?> contentClass) {
      if (request.getContentType() != null && request.getBody() != null && request.getContentLength() > 0) {
         try (Scanner scanner = new Scanner(request.getBody())) {
            scanner.useDelimiter("\\A");
            if (scanner.hasNext()) {
               return Serializer.parse(request.getContentType(), scanner.next(), contentClass);
            }
         }
      }
      return null;
   }

   private Collection<Exception> putClasses(Map<Class<?>, String> classes) {
      List<Exception> exceptions = new ArrayList<>();
      synchronized (this) {
         for (Entry<Class<?>, String> entry : classes.entrySet()) {
            try {
               Class<?> clazz = entry.getKey();
               if (clazz.isAnnotationPresent(NonResource.class)) {
                  continue;
               }
               validateResourceClass(clazz);
               String area = entry.getValue();
               RoutePattern classPat = RoutePattern.build(area, clazz, serviceMap, options);
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

   private void putMethod(Class<?> clazz, RoutePattern classPat, Method method, List<Exception> exceptions) {
      try {
         if (method.getDeclaringClass() == Object.class) {
            return;
         }
         HttpMethod[] methodNames = findMethod(method);
         if (methodNames.length == 0) {
            return;
         }
         RoutePattern methodPat = RoutePattern.build(classPat, method, options);
         if (!endpoints.containsKey(methodPat)) {
            endpoints.put(methodPat, new HashMap<>());
         }
         Map<HttpMethod, MethodEntry> map = endpoints.get(methodPat);
         for (HttpMethod methodName : methodNames) {
            if (map.containsKey(methodName)) {
               throw new DuplicateMethodException(methodName, methodPat, clazz);
            }
            Constructor<?> constructor = clazz.getConstructors()[0];
            map.put(methodName,
                     new MethodEntry(clazz, method, constructor.getParameterCount(), methodPat.getContentClass()));
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
               .map(a -> HttpMethod.valueOf(a.annotationType().getSimpleName().substring(4).toUpperCase()))
               .toArray(HttpMethod[]::new);
      if (result.length > 0) {
         return result;
      }
      String methodName = method.getName();
      for (HttpMethod value : HttpMethod.values()) {
         // TO SONARQUBE: This is actually meant to be this way and behaves differently
         // from what SonarQube is suggesting that should be implemented. Ignore SonarQube here.
         if (value.name().toLowerCase().equals(methodName)) {
            return new HttpMethod[] { value };
         }
      }
      return new HttpMethod[0];
   }

   private void validateResourceClass(Class<?> clazz) throws NoSingleMethodException {
      if (!ClassUtils.isConcreteClass(clazz) || !Modifier.isPublic(clazz.getModifiers())) {
         throw new IllegalArgumentException("Resource class must be a public concrete class");
      }
      Constructor<?>[] constructors = clazz.getConstructors();
      if (constructors.length != 1) {
         throw new NoSingleMethodException("Class " + clazz.getName() + " must have only one constructor",
                  constructors.length);
      }
   }

   private List<Object> convertParams(Matcher matcher, List<Class<?>> params) {
      List<Object> result = new ArrayList<>();
      int group = 1;
      for (Class<?> param : params) {
         try {
            Object obj = serviceMap.contains(param) ? getService(param) : convert(matcher.group(group++), param);
            result.add(obj);
         } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new InternalServerError(e);
         }
      }
      return result;
   }

   private Object getService(Class<?> clazz) throws IllegalAccessException, InvocationTargetException {
      return serviceGetter.invoke(serviceMap, clazz);
   }

   private Object convert(String value, Class<?> clazz)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
      if (clazz == String.class) {
         return value;
      }
      if (specialCases.containsKey(clazz)) {
         Function<String, ?> valueOf = specialCases.get(clazz);
         return valueOf.apply(value);
      } else {
         Method valueOf = clazz.getMethod("valueOf", String.class);
         return valueOf.invoke(null, value);
      }
   }
}
