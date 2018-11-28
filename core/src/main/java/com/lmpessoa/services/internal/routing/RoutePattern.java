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

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.lmpessoa.services.HttpInputStream;
import com.lmpessoa.services.Query;
import com.lmpessoa.services.Route;
import com.lmpessoa.services.internal.ErrorMessage;
import com.lmpessoa.services.internal.parsing.ITemplatePart;
import com.lmpessoa.services.internal.parsing.IVariablePart;
import com.lmpessoa.services.internal.parsing.LiteralPart;
import com.lmpessoa.services.internal.parsing.ParseException;
import com.lmpessoa.services.internal.parsing.TypeMismatchException;
import com.lmpessoa.services.internal.services.NoSingleMethodException;
import com.lmpessoa.services.internal.services.ServiceMap;

final class RoutePattern implements Comparable<RoutePattern> {

   private static final Pattern areaPattern = Pattern
            .compile("^(\\/)?[a-zA-Z0-9%_-]+(\\/[a-zA-Z0-9%_-]+)?$");
   private static final String SEPARATOR = "/";

   private final List<ITemplatePart> parts;
   private final Class<?> resourceClass;
   private final Class<?> contentClass;
   private Pattern pattern = null;

   @Override
   public String toString() {
      if (parts.isEmpty()) {
         return SEPARATOR;
      }
      StringBuilder result = new StringBuilder();
      for (ITemplatePart part : parts) {
         result.append(part.toString());
      }
      return result.toString();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof RoutePattern) {
         return toString().equals(obj.toString());
      }
      return false;
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }

   @Override
   public int compareTo(RoutePattern other) {
      List<ITemplatePart> copy = new ArrayList<>(parts);
      VariableRoutePart var = extractCatchAllFrom(copy);
      List<ITemplatePart> otherCopy = new ArrayList<>(other.parts);
      VariableRoutePart otherVar = extractCatchAllFrom(otherCopy);
      int diff;
      for (int i = 0; i < Math.min(copy.size(), otherCopy.size()); ++i) {
         ITemplatePart part = copy.get(i);
         ITemplatePart otherPart = otherCopy.get(i);
         if (part instanceof LiteralPart && otherPart instanceof LiteralPart) {
            String str = ((LiteralPart) part).getValue();
            String otherStr = ((LiteralPart) otherPart).getValue();
            diff = otherStr.length() - str.length();
            if (diff == 0) {
               diff = str.compareTo(otherStr);
            }
            if (diff != 0) {
               return diff;
            }
         } else if (part instanceof VariableRoutePart && otherPart instanceof VariableRoutePart) {
            diff = ((VariableRoutePart) otherPart).compareTo((VariableRoutePart) part);
            if (diff != 0) {
               return diff;
            }
         } else if (part instanceof LiteralPart) {
            return -1;
         } else {
            return 1;
         }
      }
      if (var != null && otherVar != null) {
         diff = otherVar.compareTo(var);
         if (diff != 0) {
            return diff;
         }
      } else if (otherVar == null && var != null) {
         return 1;
      } else if (otherVar != null) {
         return -1;
      }
      return other.parts.size() - parts.size();
   }

   public static String getResourceName(Class<?> clazz) {
      String[] nameParts = clazz.getName().replaceAll("\\$", ".").split("\\.");
      String name = nameParts[nameParts.length - 1].replaceAll("([A-Z])", "_$1")
               .toLowerCase()
               .replaceAll("^_", "");
      if (name.endsWith("_resource")) {
         name = name.substring(0, name.length() - 8).replaceAll("_$", "");
      }
      return name;
   }

   RoutePattern(List<ITemplatePart> parts, Class<?> resourceClass, Class<?> contentClass) {
      this.contentClass = contentClass;
      this.resourceClass = resourceClass;
      List<ITemplatePart> result = new ArrayList<>();
      StringBuilder literal = new StringBuilder();
      for (ITemplatePart part : parts) {
         if (part instanceof LiteralPart) {
            literal.append(((LiteralPart) part).getValue());
         } else {
            if (literal.length() != 0) {
               String value = literal.toString();
               if (((VariableRoutePart) part).isCatchAll() && value.endsWith(SEPARATOR)) {
                  value = value.substring(0, value.length() - 1);
               }
               result.add(new LiteralPart(value));
               literal.delete(0, literal.length());
            }
            result.add(part);
         }
      }
      if (literal.length() > 0) {
         result.add(new LiteralPart(literal.toString()));
      }
      if (!result.isEmpty() && result.get(result.size() - 1) instanceof LiteralPart) {
         LiteralPart lit = (LiteralPart) result.get(result.size() - 1);
         String litValue = lit.getValue();
         if (litValue.endsWith(SEPARATOR) && !SEPARATOR.equals(litValue)) {
            result.remove(lit);
            lit = new LiteralPart(litValue.replaceAll("/$", ""));
            result.add(lit);
         }
      }
      this.parts = Collections.unmodifiableList(result);
   }

   static RoutePattern build(String area, Class<?> clazz, ServiceMap serviceMap,
      RouteOptions options) throws NoSingleMethodException {
      final Constructor<?>[] constructors = clazz.getConstructors();
      if (constructors.length != 1) {
         throw new NoSingleMethodException(
                  ErrorMessage.TOO_MANY_CONSTRUCTORS.with(clazz.getName(), constructors.length));
      }
      Parameter[] params = validateParamsToRoute(serviceMap, constructors);
      String routePath = getRouteFor(getResourceName(clazz), constructors[0], params, clazz);
      if (area != null && !area.isEmpty()) {
         if (!areaPattern.matcher(area).find()) {
            throw new IllegalArgumentException(ErrorMessage.INVALID_AREA_NAME.with(area));
         }
         routePath = SEPARATOR + area.replaceAll("^/", "") + routePath;
         routePath = routePath.replaceAll("/$", "");
      }
      routePath = routePath.replaceAll("^/" + options.getAreaIndex(area), "");
      List<ITemplatePart> result = RoutePatternParser.parse(clazz, constructors[0], routePath);
      validateRouteParams(result, constructors[0].getParameters(), params);
      return new RoutePattern(result, clazz, null);
   }

   static RoutePattern build(RoutePattern resource, Method method) {
      List<Parameter> paramList = new ArrayList<>();
      Arrays.stream(method.getParameters()) //
               .filter(p -> !p.isAnnotationPresent(Query.class))
               .forEach(paramList::add);
      Class<?> contentParamType = null;
      if (!paramList.isEmpty()) {
         contentParamType = paramList.get(0).getType();
         if (contentParamType == InputStream.class || contentParamType == HttpInputStream.class
                  || contentParamType != String.class && !contentParamType.isArray()
                           && !contentParamType.isInterface() && !contentParamType.isPrimitive()
                           && !Modifier.isAbstract(contentParamType.getModifiers())
                           && !hasValueOfMethod(contentParamType)
                           && hasParameterlessConstructor(contentParamType)) {
            paramList.remove(0);
         } else {
            contentParamType = null;
         }
      }
      Parameter[] params = paramList.toArray(new Parameter[0]);
      Class<?> resourceClass = resource != null ? resource.getResourceClass()
               : method.getDeclaringClass();
      String routePath = getRouteFor(null, method, params, method);
      List<ITemplatePart> result = RoutePatternParser.parse(resourceClass, method, routePath);
      validateRouteParams(result, method.getParameters(), params);
      List<ITemplatePart> pattern = new ArrayList<>();
      if (resource != null) {
         pattern.addAll(resource.parts);
      }
      result.forEach(pattern::add);
      return new RoutePattern(pattern, resourceClass, contentParamType);
   }

   Collection<VariableRoutePart> getVariables() {
      return parts.stream() //
               .filter(p -> p instanceof VariableRoutePart)
               .map(p -> (VariableRoutePart) p)
               .collect(Collectors.toList());
   }

   int getVariableCount() {
      return (int) parts.stream().filter(p -> p instanceof VariableRoutePart).count();
   }

   Class<?> getContentClass() {
      return contentClass;
   }

   Class<?> getResourceClass() {
      return resourceClass;
   }

   Pattern getPattern() {
      if (pattern == null) {
         StringBuilder result = new StringBuilder();
         result.append('^');
         for (ITemplatePart part : parts) {
            if (part instanceof LiteralPart) {
               String value = ((LiteralPart) part).getValue();
               try {
                  result.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                           .replaceAll("%2F", "/"));
               } catch (UnsupportedEncodingException e) {
                  // Shall never happen, but...
                  result.append(value);
               }
            } else {
               result.append(((VariableRoutePart) part).getRegexPattern());
            }
         }
         result.append('$');
         pattern = Pattern.compile(result.toString());
      }
      return pattern;
   }

   String getPathWithArgs(Object[] args) {
      StringBuilder result = new StringBuilder();
      int i = 0;
      for (ITemplatePart part : parts) {
         if (part instanceof IVariablePart) {
            result.append(args[i]);
            i += 1;
         } else {
            result.append(((LiteralPart) part).getValue());
         }
      }
      return result.toString();
   }

   private static Parameter[] validateParamsToRoute(ServiceMap serviceMap,
      final Constructor<?>[] constructors) {
      Parameter[] params = Arrays.stream(constructors[0].getParameters())
               .filter(p -> !serviceMap.contains(p.getType()))
               .toArray(Parameter[]::new);
      Optional<Parameter> invalid = Arrays.stream(params)
               .filter(p -> !hasValueOfMethod(p.getType()))
               .findFirst();
      if (invalid.isPresent()) {
         Class<?> invalidClass = invalid.get().getType();
         String paramClassName = invalidClass.isArray() ? invalidClass.getComponentType() + "[]"
                  : invalidClass.getName();
         throw new TypeMismatchException(ErrorMessage.INVALID_ROUTE_PART.with(paramClassName));
      }
      return params;
   }

   private static String getRouteFor(String prefix, Executable exec, Parameter[] params,
      AnnotatedElement source) {
      Route route = source.getAnnotation(Route.class);
      if (route != null) {
         String result = route.value();
         if (!result.startsWith(SEPARATOR)) {
            result = SEPARATOR + result;
         }
         return result.replaceAll("\\.", "\\\\.");
      }
      StringBuilder result = new StringBuilder();
      if (prefix != null) {
         result.append(SEPARATOR);
         result.append(prefix);
      }
      if (exec instanceof Method && !isHttpMethodName(((Method) exec).getName())) {
         result.append(SEPARATOR);
         result.append(((Method) exec).getName());
      }
      for (Parameter param : params) {
         int i = getParameterIndex(param, exec.getParameters());
         Class<?> paramClass = param.getType();
         if (param.isVarArgs()) {
            paramClass = paramClass.getComponentType();
         }
         if (paramClass != String.class && !hasValueOfMethod(paramClass)) {
            String paramClassName = paramClass.getName();
            if (paramClass.isArray()) {
               paramClassName = paramClass.getComponentType().getName() + "[]";
            }
            throw new TypeMismatchException(ErrorMessage.INVALID_ROUTE_PART.with(paramClassName));
         }
         result.append(SEPARATOR);
         result.append('{');
         result.append(i);
         result.append('}');
      }
      if (result.length() == 0) {
         return SEPARATOR;
      }
      return result.toString();
   }

   private static int getParameterIndex(Parameter param, Parameter[] params) {
      for (int i = 0; i < params.length; ++i) {
         if (param == params[i]) {
            return i;
         }
      }
      throw new IllegalStateException(ErrorMessage.ILLEGAL_PARAMETER.get());
   }

   private static boolean isHttpMethodName(String name) {
      return "get".equals(name) || "post".equals(name) || "put".equals(name) || "patch".equals(name)
               || "delete".equals(name) || "options".equals(name);
   }

   private static boolean hasValueOfMethod(Class<?> clazz) {
      if (clazz.isPrimitive() || clazz.isArray()) {
         return true;
      }
      try {
         Method method = clazz.getMethod("valueOf", String.class);
         return Modifier.isStatic(method.getModifiers());
      } catch (NoSuchMethodException | SecurityException e) {
         return false;
      }
   }

   private static void validateRouteParams(List<ITemplatePart> route, Parameter[] allParams,
      Parameter[] usedParams) {
      long count = route.stream() //
               .filter(p -> p instanceof VariableRoutePart)
               .count();
      if (count != usedParams.length) {
         throw new ParseException(ErrorMessage.MISSING_PARAMETERS.with(count, usedParams.length),
                  0);
      }
      for (ITemplatePart part : route) {
         if (part instanceof VariableRoutePart) {
            VariableRoutePart var = (VariableRoutePart) part;
            Class<?> param = allParams[var.getParameterIndex()].getType();
            if (param != String.class && !var.isAssignableTo(param)) {
               throw new TypeMismatchException(ErrorMessage.CANNOT_CAST_VALUE
                        .with(var.getParameterIndex(), param.getName()));
            }
         }
      }
   }

   private static boolean hasParameterlessConstructor(Class<?> clazz) {
      for (Constructor<?> c : clazz.getConstructors()) {
         if (c.getParameterCount() == 0) {
            return true;
         }
      }
      return false;
   }

   private VariableRoutePart extractCatchAllFrom(List<ITemplatePart> parts) {
      ITemplatePart part = parts.get(parts.size() - 1);
      if (part instanceof VariableRoutePart && ((VariableRoutePart) part).isCatchAll()) {
         parts.remove(parts.size() - 1);
         return (VariableRoutePart) part;
      }
      return null;
   }
}
