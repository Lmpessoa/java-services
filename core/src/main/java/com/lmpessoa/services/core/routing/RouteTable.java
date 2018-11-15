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
package com.lmpessoa.services.core.routing;

import java.io.IOException;
import java.io.InputStream;
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

import com.lmpessoa.services.Internal;
import com.lmpessoa.services.core.HttpDelete;
import com.lmpessoa.services.core.HttpGet;
import com.lmpessoa.services.core.HttpOptions;
import com.lmpessoa.services.core.HttpPatch;
import com.lmpessoa.services.core.HttpPost;
import com.lmpessoa.services.core.HttpPut;
import com.lmpessoa.services.core.NonResource;
import com.lmpessoa.services.core.hosting.HttpRequest;
import com.lmpessoa.services.core.hosting.InternalServerError;
import com.lmpessoa.services.core.hosting.MethodNotAllowedException;
import com.lmpessoa.services.core.hosting.NotFoundException;
import com.lmpessoa.services.core.routing.content.Serializer;
import com.lmpessoa.services.core.services.IServiceMap;
import com.lmpessoa.services.core.services.NoSingleMethodException;
import com.lmpessoa.services.util.ClassUtils;
import com.lmpessoa.services.util.logging.ILogger;

@Internal
public final class RouteTable implements IRouteTable {

   private static final Map<Class<?>, Function<String, ?>> specialCases;

   private final Map<RoutePattern, Map<HttpMethod, MethodEntry>> endpoints = new HashMap<>();
   private final RouteOptions options = new RouteOptions();
   private final IServiceMap serviceMap;
   private final ILogger log;

   private Collection<Exception> lastExceptions;

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

   RouteTable(IServiceMap serviceMap, ILogger log) {
      this.serviceMap = serviceMap;
      this.log = log;
   }

   @Override
   public void put(Class<?> clazz) {
      putAll(options.findArea(clazz), Arrays.asList(clazz));
   }

   @Override
   public void putAll(Collection<Class<?>> classes) {
      putClasses(classes.stream().filter(c -> options.findArea(c) != null).collect(
               Collectors.toMap(c -> c, options::findArea)));
   }

   @Override
   public void putAll(String area, Collection<Class<?>> classes) {
      putClasses(classes.stream().filter(c -> area != null).collect(Collectors.toMap(c -> c, c -> area)));
   }

   @Internal
   @Override
   public String findArea(String packageName) {
      ClassUtils.checkInternalAccess();
      return options.findArea(packageName);
   }

   @Internal
   public RouteMatch matches(HttpRequest request) {
      ClassUtils.checkInternalAccess();
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
         return new NotFoundException();
      } else {
         return new MethodNotAllowedException();
      }
   }

   @Internal
   @Override
   public IRouteOptions getOptions() {
      ClassUtils.checkInternalAccess();
      return options;
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

   Collection<Exception> getLastExceptions() {
      return lastExceptions;
   }

   private Object parseContentBody(HttpRequest request, Class<?> contentClass) {
      InputStream body;
      try {
         body = request.getBody();
      } catch (IOException e) {
         log.error(e);
         return null;
      }
      if (request.getContentType() != null && body != null && request.getContentLength() > 0) {
         try (Scanner scanner = new Scanner(body)) {
            scanner.useDelimiter("\\A");
            if (scanner.hasNext()) {
               return Serializer.parse(request.getContentType(), scanner.next(), contentClass);
            }
         }
      }
      return null;
   }

   private void putClasses(Map<Class<?>, String> classes) {
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
      this.lastExceptions = exceptions;
      exceptions.forEach(log::warning);
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
               log.info("Route '%s %s' is already assigned to another method; ignored", methodName, methodPat);
               exceptions.add(new DuplicateMethodException(methodName, methodPat, clazz));
               continue;
            }
            Constructor<?> constructor = clazz.getConstructors()[0];
            map.put(methodName,
                     new MethodEntry(clazz, method, constructor.getParameterCount(), methodPat.getContentClass()));
            log.info("Mapped route '%s %s' to method %s", methodName, methodPat, getMethodString(method));

         }
      } catch (Exception e) {
         exceptions.add(e);
      }
   }

   private String getMethodString(Method method) {
      StringBuilder result = new StringBuilder();
      result.append(method.getDeclaringClass().getTypeName());
      result.append('.');
      result.append(method.getName());
      result.append('(');
      for (Class<?> param : method.getParameterTypes()) {
         if (result.charAt(result.length() - 1) != '(') {
            result.append(", ");
         }
         result.append(param.getTypeName());
      }
      result.append(')');
      if (void.class != method.getReturnType()) {
         result.append(": ");
         result.append(method.getReturnType().getTypeName());
      }

      return result.toString();
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
            Object obj = serviceMap.contains(param) ? serviceMap.get(param) : convert(matcher.group(group++), param);
            result.add(obj);
         } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new InternalServerError(e);
         }
      }
      return result;
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
