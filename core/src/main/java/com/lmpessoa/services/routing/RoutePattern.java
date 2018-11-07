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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.lmpessoa.utils.parsing.ITemplatePart;
import com.lmpessoa.utils.parsing.LiteralPart;
import com.lmpessoa.utils.parsing.TypeMismatchException;

final class RoutePattern {

   private static final Pattern areaPattern = Pattern
            .compile("^(\\/)?[a-zA-Z0-9%_-]+(\\/[a-zA-Z0-9%_-]+)?$");
   static final Map<String, Class<? extends AbstractRouteType>> types = new HashMap<>();
   private static final IntRouteType intType = new IntRouteType();
   private static final String SEPARATOR = "/";

   private final List<ITemplatePart> parts;

   static {
      types.put("hex", HexRouteType.class);
      types.put("int", IntRouteType.class);
      types.put("alpha", AlphaRouteType.class);
      types.put("any", AnyRouteType.class);
   }

   static RoutePattern build(String area, Class<?> clazz)
      throws NoSingleMethodException, ParseException {
      final Constructor<?>[] constructors = clazz.getConstructors();
      if (constructors.length != 1) {
         throw new NoSingleMethodException("Class " + clazz.getName()
                  + " must have only one constructor (found: " + constructors.length + ")");
      }
      Route route = clazz.getAnnotation(Route.class);
      String routePath = route != null ? route.value()
               : buildRouteFromParams(getResourceName(clazz), constructors[0].getParameterTypes());
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
      List<ITemplatePart> result = RoutePatternParser.parse(routePath, types);
      if (route != null) {
         validateRoute(result, constructors[0].getParameterTypes());
      }
      return new RoutePattern(result);
   }

   static RoutePattern build(RoutePattern resource, Method method) throws ParseException {
      Route route = method.getAnnotation(Route.class);
      String routePath = route != null ? route.value()
               : buildRouteFromParams(null, method.getParameterTypes());
      if (!routePath.startsWith(SEPARATOR)) {
         routePath = SEPARATOR + routePath;
      }
      List<ITemplatePart> result = RoutePatternParser.parse(routePath, types);
      if (route != null) {
         validateRoute(result, method.getParameterTypes());
      }
      List<ITemplatePart> pattern = new ArrayList<>();
      if (resource != null) {
         pattern.addAll(resource.parts);
      }
      result.forEach(pattern::add);
      return new RoutePattern(pattern);
   }

   private static String buildRouteFromParams(String prefix, Class<?>[] parameterTypes)
      throws TypeMismatchException {
      StringBuilder result = new StringBuilder();
      if (prefix != null) {
         result.append('/');
         result.append(prefix);
      }
      for (Class<?> paramClass : parameterTypes) {
         if (paramClass != String.class && !hasValueOfMethod(paramClass)) {
            throw new TypeMismatchException(
                     paramClass.getName() + " is not an acceptable route part");
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

   private static void validateRoute(List<ITemplatePart> route, Class<?>[] parameterTypes)
      throws ParseException {
      long count = route.stream().filter(p -> p instanceof AbstractRouteType).count();
      if (count != parameterTypes.length) {
         throw new ParseException("Wrong parameter count in route (found: " + count + ", expected: "
                  + parameterTypes.length + ")", 0);
      }
      List<Class<?>> parameters = new ArrayList<>();
      parameters.addAll(Arrays.asList(parameterTypes));
      for (ITemplatePart part : route) {
         if (part instanceof AbstractRouteType) {
            AbstractRouteType var = (AbstractRouteType) part;
            Class<?> param = parameters.remove(0);
            if (param != String.class && !var.isAssignableTo(param)) {
               throw new TypeMismatchException(
                        "Cannot cast '" + var.getName() + "' to " + param.getName());
            }
         }
      }
   }

   RoutePattern(List<ITemplatePart> parts) {
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