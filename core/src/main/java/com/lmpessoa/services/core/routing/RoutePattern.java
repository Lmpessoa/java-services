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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.lmpessoa.services.core.Route;
import com.lmpessoa.services.core.services.NoSingleMethodException;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.parsing.ITemplatePart;
import com.lmpessoa.services.util.parsing.LiteralPart;
import com.lmpessoa.services.util.parsing.TypeMismatchException;

final class RoutePattern {

   private static final Pattern areaPattern = Pattern.compile("^(\\/)?[a-zA-Z0-9%_-]+(\\/[a-zA-Z0-9%_-]+)?$");
   private static final IntRouteType intType = new IntRouteType();
   private static final String SEPARATOR = "/";

   private final List<ITemplatePart> parts;
   private final Class<?> contentClass;
   private Pattern pattern = null;

   static RoutePattern build(String area, Class<?> clazz, ServiceMap serviceMap, RouteOptions options)
      throws NoSingleMethodException, ParseException {
      final Constructor<?>[] constructors = clazz.getConstructors();
      if (constructors.length != 1) {
         throw new NoSingleMethodException("Class " + clazz.getName() + " must have only one constructor",
                  constructors.length);
      }
      Route route = clazz.getAnnotation(Route.class);
      Class<?>[] paramTypes = Arrays.stream(constructors[0].getParameterTypes())
               .filter(c -> !serviceMap.contains(c))
               .toArray(Class<?>[]::new);
      Optional<Class<?>> invalid = Arrays.stream(paramTypes).filter(c -> !hasValueOfMethod(c)).findFirst();
      if (invalid.isPresent()) {
         Class<?> invalidClass = invalid.get();
         String paramClassName = invalidClass.isArray() ? invalidClass.getComponentType() + "[]"
                  : invalidClass.getName();
         throw new TypeMismatchException(paramClassName + " is not an acceptable route part");
      }
      String routePath = route != null ? route.value() : buildRouteFromParams(getResourceName(clazz), paramTypes);
      if (!routePath.startsWith(SEPARATOR)) {
         routePath = SEPARATOR + routePath;
      }
      if (area != null && !area.isEmpty()) {
         if (!areaPattern.matcher(area).find()) {
            throw new IllegalArgumentException("Invalid area: " + area);
         }
         routePath = SEPARATOR + area.replaceAll("^/", "") + routePath;
         routePath = routePath.replaceAll("/$", "");
      }
      routePath = routePath.replaceAll("^/" + options.getAreaIndex(area), "");
      List<ITemplatePart> result = RoutePatternParser.parse(routePath, options);
      if (route != null) {
         validateRoute(result, paramTypes);
      }
      return new RoutePattern(result, null);
   }

   static RoutePattern build(RoutePattern resource, Method method, RouteOptions options) throws ParseException {
      Route route = method.getAnnotation(Route.class);
      List<Class<?>> params = new ArrayList<>(Arrays.asList(method.getParameterTypes()));
      Class<?> lastArgument = null;
      if (!params.isEmpty()) {
         lastArgument = params.get(params.size() - 1);
         if (lastArgument != String.class && !lastArgument.isArray() && !lastArgument.isInterface()
                  && !lastArgument.isPrimitive() && !Modifier.isAbstract(lastArgument.getModifiers())
                  && !hasValueOfMethod(lastArgument) && hasParameterlessConstructor(lastArgument)) {
            params.remove(params.size() - 1);
         } else {
            lastArgument = null;
         }
      }
      Class<?>[] paramTypes = params.toArray(new Class<?>[0]);
      String routePath = route != null ? route.value() : buildRouteFromParams(null, paramTypes);
      if (!routePath.startsWith(SEPARATOR)) {
         routePath = SEPARATOR + routePath;
      }
      List<ITemplatePart> result = RoutePatternParser.parse(routePath, options);
      if (route != null) {
         validateRoute(result, paramTypes);
      }
      List<ITemplatePart> pattern = new ArrayList<>();
      if (resource != null) {
         pattern.addAll(resource.parts);
      }
      result.forEach(pattern::add);
      return new RoutePattern(pattern, lastArgument);
   }

   private static String buildRouteFromParams(String prefix, Class<?>[] parameterTypes) throws TypeMismatchException {
      StringBuilder result = new StringBuilder();
      if (prefix != null) {
         result.append('/');
         result.append(prefix);
      }
      for (Class<?> paramClass : parameterTypes) {
         if (paramClass != String.class && !hasValueOfMethod(paramClass)) {
            String paramClassName = paramClass.getName();
            if (paramClass.isArray()) {
               paramClassName = paramClass.getComponentType().getName() + "[]";
            }
            throw new TypeMismatchException(paramClassName + " is not an acceptable route part");
         }
         result.append("/{");
         result.append(getRouteTypeOf(paramClass));
         result.append('}');
      }
      if (result.length() == 0) {
         result.append('/');
      }
      return result.toString();
   }

   private static boolean hasValueOfMethod(Class<?> clazz) {
      if (intType.isAssignableTo(clazz)) {
         return true;
      }
      try {
         Method method = clazz.getMethod("valueOf", String.class);
         return Modifier.isStatic(method.getModifiers());
      } catch (NoSuchMethodException | SecurityException e) {
         return false;
      }
   }

   private static String getRouteTypeOf(Class<?> clazz) {
      return intType.isAssignableTo(clazz) ? "int" : "any";
   }

   private static void validateRoute(List<ITemplatePart> route, Class<?>[] parameterTypes) throws ParseException {
      long count = route.stream().filter(p -> p instanceof AbstractRouteType).count();
      if (count != parameterTypes.length) {
         throw new ParseException(
                  "Wrong parameter count in route (found: " + count + ", expected: " + parameterTypes.length + ")", 0);
      }
      List<Class<?>> parameters = new ArrayList<>();
      parameters.addAll(Arrays.asList(parameterTypes));
      for (ITemplatePart part : route) {
         if (part instanceof AbstractRouteType) {
            AbstractRouteType var = (AbstractRouteType) part;
            Class<?> param = parameters.remove(0);
            if (param != String.class && !var.isAssignableTo(param)) {
               throw new TypeMismatchException("Cannot cast '" + var.getName() + "' to " + param.getName());
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

   public static String getResourceName(Class<?> clazz) {
      String[] nameParts = clazz.getName().replaceAll("\\$", ".").split("\\.");
      String name = nameParts[nameParts.length - 1].replaceAll("([A-Z])", "_$1").toLowerCase().replaceAll("^_", "");
      if (name.endsWith("_resource")) {
         name = name.substring(0, name.length() - 8).replaceAll("_$", "");
      }
      return name;
   }

   RoutePattern(List<ITemplatePart> parts, Class<?> contentClass) {
      this.contentClass = contentClass;
      List<ITemplatePart> result = new ArrayList<>();
      StringBuilder literal = new StringBuilder();
      for (ITemplatePart part : parts) {
         if (part instanceof LiteralPart) {
            literal.append(((LiteralPart) part).getValue());
         } else {
            if (literal.length() != 0) {
               result.add(new LiteralPart(literal.toString()));
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

   int getVariableCount() {
      return (int) parts.stream().filter(p -> p instanceof AbstractRouteType).count();
   }

   Class<?> getContentClass() {
      return contentClass;
   }

   Pattern getPattern() {
      if (pattern == null) {
         StringBuilder result = new StringBuilder();
         result.append('^');
         for (ITemplatePart part : parts) {
            if (part instanceof LiteralPart) {
               result.append(((LiteralPart) part).getValue().replaceAll("([\\\\/$^?\\{\\}\\[\\]\\(\\)-])", "\\\\$1"));
            } else {
               result.append('(');
               result.append(((AbstractRouteType) part).getRegex().replaceAll("([\\(\\)])", "\\\\$1"));
               result.append(')');
            }
         }
         result.append('$');
         pattern = Pattern.compile(result.toString());
      }
      return pattern;
   }

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
}

// There will be only four types for route constraint on content:
// HEX = [0-9a-zA-Z]
// NUM(BER) = [0-9]
// ALPHA = [a-zA-Z]
// ANY = [^/]

// Also there will only be designators for length:
// (n,) = minimum length of n
// (,n) = maximum length of n (minimum is at most zero)
// (n,m) = minumum length of n and maximum of m
// (n) = exact length of n
// [nothing] = any length

// This makes sorting simple:
// HEX > NUM > ALPHA > ANY
// bigger min length first
// smaller max length first
// else they are equals
