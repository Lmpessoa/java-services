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
package com.lmpessoa.services.internal.routing;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import com.lmpessoa.services.BadRequestException;
import com.lmpessoa.services.Delete;
import com.lmpessoa.services.Get;
import com.lmpessoa.services.HttpInputStream;
import com.lmpessoa.services.MethodNotAllowedException;
import com.lmpessoa.services.NotFoundException;
import com.lmpessoa.services.NotPublished;
import com.lmpessoa.services.Options;
import com.lmpessoa.services.Patch;
import com.lmpessoa.services.Post;
import com.lmpessoa.services.Put;
import com.lmpessoa.services.Query;
import com.lmpessoa.services.Route;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.internal.ClassUtils;
import com.lmpessoa.services.internal.CoreMessage;
import com.lmpessoa.services.internal.serializing.Serializer;
import com.lmpessoa.services.internal.services.NoSingleMethodException;
import com.lmpessoa.services.internal.services.ServiceMap;
import com.lmpessoa.services.routing.HttpMethod;
import com.lmpessoa.services.routing.IRouteOptions;
import com.lmpessoa.services.routing.IRouteTable;
import com.lmpessoa.services.routing.RouteMatch;
import com.lmpessoa.services.validating.IValidationService;

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
public final class RouteTable implements IRouteTable {

   private final Map<RoutePattern, Map<HttpMethod, MethodEntry>> endpoints = new ConcurrentSkipListMap<>();
   private final RouteOptions options = new RouteOptions();
   private final ServiceMap services;

   /**
    * Creates a new {@code RouteTable} with the given arguments.
    *
    * @param services a service map used to resolve arguments to resource construction.
    * @param log a logger for error messages.
    */
   public RouteTable(ServiceMap services) {
      this.services = Objects.requireNonNull(services);
   }

   @Override
   public Collection<RouteEntry> put(Class<?> clazz) {
      return putAll(options.findArea(clazz), Arrays.asList(clazz));
   }

   @Override
   public Collection<RouteEntry> putAll(Collection<Class<?>> classes) {
      return putClasses(classes.stream().filter(c -> options.findArea(c) != null).collect(
               Collectors.toMap(c -> c, options::findArea)));
   }

   @Override
   public Collection<RouteEntry> putAll(String area, Collection<Class<?>> classes) {
      return putClasses(classes.stream().filter(c -> area != null).collect(
               Collectors.toMap(c -> c, c -> area)));
   }

   @Override
   public String findArea(String packageName) {
      return options.findArea(packageName);
   }

   public IRouteOptions getOptions() {
      return options;
   }

   /**
    * Returns a route match for the given request.
    *
    * <p>
    * In other words, this method is responsible for reading information from a request and return
    * an object that can be used to invoke the target method with the parameters extracted from such
    * request. Note that this method, instead of throwing an exception if there is no match for the
    * request, returns the intended exceptions as route matches. Invoking those route matches raises
    * the respective exception.
    * </p>
    *
    * @param request the information from the request.
    * @return an object that represents the matched route.
    */
   public RouteMatch matches(HttpRequest request) {
      boolean found = false;
      for (Entry<RoutePattern, Map<HttpMethod, MethodEntry>> entry : endpoints.entrySet()) {
         RoutePattern route = entry.getKey();
         Matcher matcher = route.getPattern().matcher(request.getPath());
         if (!matcher.find()) {
            continue;
         }
         found = true;
         HttpMethod method = request.getMethod();
         MethodEntry methodEntry = entry.getValue().get(method);
         if (methodEntry == null) {
            continue;
         }
         List<Parameter> params = new ArrayList<>();
         Class<?> resourceClass = methodEntry.getResourceClass();
         Constructor<?> constructor = resourceClass.getConstructors()[0];
         params.addAll(Arrays.asList(constructor.getParameters()));
         Method methodCall = methodEntry.getMethod();
         params.addAll(Arrays.asList(methodCall.getParameters()));
         if (methodEntry.getContentClass() != null) {
            params.remove(constructor.getParameterCount());
         }
         Map<String, List<String>> query = parseQueryString(request.getQueryString());
         List<Object> result;
         try {
            result = convertParams(params, route, matcher, query);
         } catch (IllegalArgumentException e) {
            return new BadRequestException(resourceClass, methodCall, e);
         }
         boolean hasContent = false;
         if (methodEntry.getContentClass() != null) {
            Object contentObject = parseContentBody(request, methodEntry.getContentClass());
            result.add(constructor.getParameterCount(), contentObject);
            hasContent = true;
         }
         return new MatchedRoute(services.get(IValidationService.class), methodEntry,
                  result.toArray(), hasContent);
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
    * This method does the inverse of {@link #matches(IRouteRequest)}. It receives information about
    * a method (including arguments) and returns a path in this route table that leads to that
    * method.
    * </p>
    *
    * <p>
    * Note however that there is no guarantee the method will be reached through the returned path
    * since other factors during the call to {@code matches(...)} can lead to a different method
    * (such as the HTTP method used and body arguments).
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
            if (clazz == methodEntry.getResourceClass()
                     && isMethodArgsCompatible(methodEntry, methodName, args)) {
               String result = endpoint.getKey().getPathWithArgs(args);
               String queryString = buildQueryString(methodEntry.getMethod(), args);
               if (queryString != null) {
                  result += "?" + queryString;
               }
               return result;
            }
         }
      }
      return null;
   }

   private String buildQueryString(Method method, Object[] args) {
      StringBuilder result = new StringBuilder();
      for (int i = 0; i < method.getParameterCount(); ++i) {
         Parameter param = method.getParameters()[i];
         Query query = param.getAnnotation(Query.class);
         if (query != null && args[i] != null) {
            String queryName = "##default".equals(query.value()) ? param.getName() : query.value();
            Object[] values = args[i].getClass().isArray() ? (Object[]) args[i]
                     : new Object[] { args[i] };
            for (Object value : values) {
               if (value != null) {
                  result.append("&");
                  result.append(queryName);
                  result.append("=");
                  result.append(value.toString());
               }
            }
         }
      }
      if (result.length() > 1) {
         return result.toString().substring(1);
      }
      return null;
   }

   boolean hasRoute(String route) {
      for (RoutePattern pat : endpoints.keySet()) {
         if (route.equals(pat.toString())) {
            return true;
         }
      }
      return false;
   }

   HttpMethod[] listMethodsOf(String route) {
      for (Entry<RoutePattern, Map<HttpMethod, MethodEntry>> entry : endpoints.entrySet()) {
         if (route.equals(entry.getKey().toString())) {
            return entry.getValue().keySet().toArray(new HttpMethod[0]);
         }
      }
      return new HttpMethod[0];
   }

   MethodEntry getRouteMethod(HttpMethod method, String route) {
      for (Entry<RoutePattern, Map<HttpMethod, MethodEntry>> entry : endpoints.entrySet()) {
         if (route.equals(entry.getKey().toString())) {
            return entry.getValue().get(method);
         }
      }
      return null;
   }

   private Object parseContentBody(HttpRequest request, Class<?> contentClass) {
      InputStream body = request.getContentBody();
      if (body != null) {
         if (contentClass == InputStream.class || contentClass == HttpInputStream.class) {
            return new HttpInputStream(body, request.getContentType());
         }
         byte[] content = readContentBody(body);
         if (request.getContentType() != null && content != null
                  && request.getContentLength() > 0) {
            return Serializer.toObject(content, request.getContentType(), contentClass);
         }
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

   private Collection<RouteEntry> putClasses(Map<Class<?>, String> classes) {
      List<RouteEntry> result = new ArrayList<>();
      synchronized (this) {
         for (Entry<Class<?>, String> entry : classes.entrySet()) {
            Class<?> clazz = entry.getKey();
            try {
               if (clazz.isAnnotationPresent(NotPublished.class)) {
                  continue;
               }
               validateResourceClass(clazz);
               String area = entry.getValue();
               RoutePattern classPat = RoutePattern.build(area, clazz, services, options);
               for (Method method : clazz.getMethods()) {
                  if (!method.isAnnotationPresent(NotPublished.class)) {
                     result.addAll(putMethod(clazz, classPat, method));
                  }
               }
            } catch (Exception e) {
               result.add(new RouteEntry(clazz, e));
            }
         }
      }
      return Collections.unmodifiableCollection(result);
   }

   private Collection<RouteEntry> putMethod(Class<?> clazz, RoutePattern classPat, Method method) {
      if (method.getDeclaringClass() == Object.class) {
         return Collections.emptyList();
      }
      HttpMethod[] verbs = findMethod(method);
      if (verbs.length == 0) {
         return Collections.emptyList();
      }
      List<RouteEntry> result = new ArrayList<>();
      try {
         RoutePattern methodPat = RoutePattern.build(classPat, method);
         if (!endpoints.containsKey(methodPat)) {
            endpoints.put(methodPat, new ConcurrentHashMap<>());
         }
         Map<HttpMethod, MethodEntry> map = endpoints.get(methodPat);
         for (HttpMethod verb : verbs) {
            if (map.containsKey(verb)) {
               MethodEntry entry = map.get(verb);
               result.add(new RouteEntry(method, String.format("%s %s", verb, methodPat),
                        entry.getMethod()));
               continue;
            }
            Constructor<?> constructor = clazz.getConstructors()[0];
            map.put(verb, new MethodEntry(clazz, method, constructor.getParameterCount(),
                     methodPat.getContentClass()));
            result.add(new RouteEntry(method, String.format("%s %s", verb, methodPat)));

         }
      } catch (Exception e) {
         result.add(new RouteEntry(method, e));
      }
      return result;
   }

   private HttpMethod[] findMethod(Method method) {
      Annotation[] methods = new Annotation[] { method.getAnnotation(Get.class),
               method.getAnnotation(Post.class), method.getAnnotation(Put.class),
               method.getAnnotation(Patch.class), method.getAnnotation(Delete.class),
               method.getAnnotation(Options.class) };
      HttpMethod[] result = Arrays.stream(methods)
               .filter(Objects::nonNull)
               .map(a -> HttpMethod.valueOf(a.annotationType().getSimpleName().toUpperCase()))
               .toArray(HttpMethod[]::new);
      if (result.length > 0) {
         return result;
      }
      String methodName = method.getName();
      Optional<HttpMethod> optionalResult = Arrays.stream(HttpMethod.values())
               .filter(m -> m.name().equalsIgnoreCase(methodName))
               .findFirst();
      if (optionalResult.isPresent()) {
         return new HttpMethod[] { optionalResult.get() };
      }
      return new HttpMethod[] { HttpMethod.GET };
   }

   private void validateResourceClass(Class<?> clazz) throws NoSingleMethodException {
      if (!ClassUtils.isConcreteClass(clazz) || !Modifier.isPublic(clazz.getModifiers())) {
         throw new IllegalArgumentException(CoreMessage.RESOURCE_NOT_CONCRETE.get());
      }
      Constructor<?>[] constructors = clazz.getConstructors();
      if (constructors.length != 1) {
         throw new NoSingleMethodException(
                  CoreMessage.TOO_MANY_CONSTRUCTORS.with(clazz.getName(), constructors.length));
      }
   }

   private List<Object> convertParams(List<Parameter> params, RoutePattern route, Matcher matcher,
      Map<String, List<String>> query) {
      Map<VariableRoutePart, String> paramValues = new HashMap<>();
      int group = 1;
      for (VariableRoutePart part : route.getVariables()) {
         paramValues.put(part, matcher.group(group++));
      }
      List<Object> result = new ArrayList<>(params.size());
      for (Parameter param : params) {
         Class<?> type = param.getType();
         if (services.contains(type)) {
            result.add(services.get(type));
         } else if (param.isAnnotationPresent(Query.class)) {
            Query qp = param.getAnnotation(Query.class);
            List<String> values = query
                     .get("##default".equals(qp.value()) ? param.getName() : qp.value());
            result.add(ClassUtils.cast(values != null ? String.join(",", values) : null, type));
         } else {
            for (Entry<VariableRoutePart, String> entry : paramValues.entrySet()) {
               if (entry.getKey().isSimilarTo(param)) {
                  String value = entry.getValue();
                  if (param.isVarArgs() && value.length() > 1) {
                     value = String.join(",", value.substring(1).split("/"));
                  }
                  result.add(type == String.class ? value : ClassUtils.cast(value, type));
                  break;
               }
            }
         }
      }
      return result;
   }

   private Map<String, List<String>> parseQueryString(String queryString) {
      final Map<String, List<String>> result = new HashMap<>();
      if (queryString != null) {
         for (String var : queryString.split("&")) {
            String[] parts = var.split("=", 2);
            try {
               if (parts.length == 1) {
                  parts = new String[] { parts[0], "true" };
               }
               parts[0] = URLDecoder.decode(parts[0], UTF_8.name());
               parts[1] = URLDecoder.decode(parts[1], UTF_8.name());
            } catch (UnsupportedEncodingException e) {
               // Ignore; never happens
            }
            if (parts[0].endsWith("[]")) {
               parts[0] = parts[0].substring(0, parts[0].length() - 2);
            }
            if (!result.containsKey(parts[0])) {
               result.put(parts[0], new ArrayList<>());
            }
            result.get(parts[0]).add(parts[1]);
         }
      }
      return Collections.unmodifiableMap(result);
   }

   private boolean isMethodArgsCompatible(MethodEntry methodEntry, String methodName,
      Object[] args) {
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
         if (!methodArgs[i].isInstance(args[i])) {
            return false;
         }
      }
      return true;
   }
}
