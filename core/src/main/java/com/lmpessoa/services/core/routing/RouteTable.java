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

import java.io.ByteArrayOutputStream;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import com.lmpessoa.services.core.hosting.Configurable;
import com.lmpessoa.services.core.hosting.InternalServerError;
import com.lmpessoa.services.core.hosting.MethodNotAllowedException;
import com.lmpessoa.services.core.hosting.NotFoundException;
import com.lmpessoa.services.core.serializing.Serializer;
import com.lmpessoa.services.core.services.NoSingleMethodException;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.ClassUtils;
import com.lmpessoa.services.util.logging.ILogger;

/**
 * Maps methods to routes of the application.
 *
 * <p>
 * A {@code RouteTable} will hold information about certain methods of a class and create routes
 * that can be used to retrieve those methods thus creating a map between methods and routes.
 * </p>
 *
 * <p>
 * Routes are registered for a whole class instead of individual methods. However only methods whose
 * name match one of the HTTP methods will be registered. Methods with other names can also be
 * registered to respond to an HTTP method by using one of the {@code @Http...} annotations. Methods
 * can bear multiple {@code Http...} method annotations, meaning the method will respond for any of
 * the methods annotated.
 * </p>
 *
 * <p>
 * The route produced by the {@code RouteTable} itself can be customised by using the {@link Route}
 * annotation in classes and methods. If a route table finds that a method has the same route path
 * and method of an already registered method, the second registration will be ignored and an
 * informative log entry will be registered.
 * </p>
 *
 * <p>
 * Classes and methods annotated with {@code @NonResource} will not be registered by a
 * {@code RouteTable}.
 * </p>
 */
public final class RouteTable implements IRouteTable, Configurable<IRouteOptions> {

   private final Map<RoutePattern, Map<HttpMethod, MethodEntry>> endpoints = new ConcurrentHashMap<>();
   private final RouteOptions options = new RouteOptions();
   private final ServiceMap services;
   private final ILogger log;

   private Collection<Exception> lastExceptions;

   /**
    * Creates a new {@code RouteTable} with the given arguments.
    *
    * @param services a service map used to resolve arguments to resource construction.
    * @param log a logger for error messages.
    */
   public RouteTable(ServiceMap services, ILogger log) {
      this.services = Objects.requireNonNull(services);
      this.log = Objects.requireNonNull(log);
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

   @Override
   public String findArea(String packageName) {
      return options.findArea(packageName);
   }

   @Override
   public IRouteOptions getOptions() {
      return options;
   }

   /**
    * Returns a route match for the given request.
    *
    * <p>
    * In other words, this method is responsible for reading information from a request and return an
    * object that can be used to invoke the target method with the parameters extracted from such
    * request. Note that this method, instead of throwing an exception if there is no match for the
    * request, returns the intended exceptions as route matches. Invoking those route matches raises
    * the respective exception.
    * </p>
    *
    * @param request the information from the request.
    * @return an object that represents the matched route.
    */
   public RouteMatch matches(IRouteRequest request) {
      boolean found = false;
      for (Entry<RoutePattern, Map<HttpMethod, MethodEntry>> entry : endpoints.entrySet()) { // NOSONAR
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

   /**
    * Returns the path to a method in this route table.
    *
    * <p>
    * This method does the inverse of {@link #matches(IRouteRequest)}. It receives information about a
    * method (including arguments) and returns a path in this route table that leads to that method.
    * </p>
    *
    * <p>
    * Note however that there is no guarantee the method will be reached through the returned path
    * since other factors during the call to {@code matches(...)} can lead to a different method (such
    * as the HTTP method used and body arguments).
    * </p>
    *
    * @param clazz
    * @param methodName
    * @param args
    * @return
    */
   public String findPathTo(Class<?> clazz, String methodName, Object... args) {
      Objects.requireNonNull(clazz);
      Objects.requireNonNull(methodName);
      for (Entry<RoutePattern, Map<HttpMethod, MethodEntry>> endpoint : endpoints.entrySet()) {
         for (Entry<HttpMethod, MethodEntry> entry : endpoint.getValue().entrySet()) {
            final MethodEntry methodEntry = entry.getValue();
            if (clazz == methodEntry.getResourceClass() && isMethodArgsCompatible(methodEntry, methodName, args)) {
               return endpoint.getKey().getPathWithArgs(args);
            }
         }
      }
      return null;
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

   private Object parseContentBody(IRouteRequest request, Class<?> contentClass) {
      InputStream body = request.getBody();
      byte[] content = readContentBody(body);
      if (request.getContentType() != null && content != null && request.getContentLength() > 0) {
         return Serializer.toObject(content, request.getContentType(), contentClass);
      }
      return null;
   }

   private byte[] readContentBody(InputStream input) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int len;
      try {
         while ((len = input.read(buffer)) != -1) {
            output.write(buffer, 0, len);
         }
      } catch (Exception e) {
         return null;
      }
      return output.toByteArray();
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
               RoutePattern classPat = RoutePattern.build(area, clazz, services, options);
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
            endpoints.put(methodPat, new ConcurrentHashMap<>());
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
         if (value.name().toLowerCase().equals(methodName)) { // NOSONAR
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
            Object obj = services.contains(param) ? services.get(param) : convert(matcher.group(group++), param);
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
      clazz = ClassUtils.box(clazz);
      if (clazz == Character.class) {
         return value;
      }
      Method valueOf = clazz.getMethod("valueOf", String.class);
      return valueOf.invoke(null, value);
   }

   private boolean isMethodArgsCompatible(MethodEntry methodEntry, String methodName, Object[] args) {
      Method method = methodEntry.getMethod();
      if (!method.getName().equals(methodName)) {
         return false;
      }
      Class<?>[] methodArgs = ClassUtils.box(method.getParameterTypes());
      if (methodEntry.getContentClass() != null) {
         methodArgs = Arrays.copyOf(methodArgs, methodArgs.length - 1);
      }
      if (methodArgs.length != args.length) {
         return false;
      }
      for (int i = 0; i < methodArgs.length; ++i) {
         if (!methodArgs[i].isAssignableFrom(args[i].getClass())) {
            return false;
         }
      }
      return true;
   }
}
