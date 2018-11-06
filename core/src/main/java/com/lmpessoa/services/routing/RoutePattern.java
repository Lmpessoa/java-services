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

import com.lmpessoa.utils.parsing.ITemplatePart;
import com.lmpessoa.utils.parsing.LiteralPart;

final class RoutePattern {

   private static final Map<String, Class<? extends AbstractRouteType>> types = new HashMap<>();

   private final List<ITemplatePart> parts;
   private final int classArgCount;

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
      StringBuilder classPart = new StringBuilder();
      if (area != null && !area.isEmpty()) {
         classPart.append('/');
         classPart.append(area.replaceAll("^/", "").replaceAll("/$", "").replaceAll("//", "/"));
      }
      if (classPart.length() == 0 || classPart.charAt(classPart.length() - 1) != '/') {
         classPart.append('/');
      }
      classPart.append(getResourceName(clazz));
      for (Class<?> paramClass : constructors[0].getParameterTypes()) {
         if (!hasValueOfMethod(paramClass)) {
            throw new IllegalArgumentException(
                     "Cannot convert route to " + paramClass.getSimpleName());
         }
         classPart.append("/{");
         classPart.append(getRouteTypeOf(paramClass));
         classPart.append('}');
      }
      List<ITemplatePart> pattern = Arrays
               .asList(RoutePatternParser.parse(classPart.toString(), types));
      int argCount = (int) pattern.stream().filter(p -> p instanceof AbstractRouteType).count();
      return new RoutePattern(pattern, argCount);
   }

   static RoutePattern build(RoutePattern resource, Method method) throws ParseException {
      StringBuilder methodPart = new StringBuilder();
      for (Class<?> paramClass : method.getParameterTypes()) {
         if (!hasValueOfMethod(paramClass)) {
            throw new IllegalArgumentException(
                     "Cannot convert route to " + paramClass.getSimpleName());
         }
         methodPart.append("/{");
         methodPart.append(getRouteTypeOf(paramClass));
         methodPart.append('}');
      }
      List<ITemplatePart> pattern = new ArrayList<>();
      if (resource != null) {
         pattern.addAll(resource.parts);
      }
      Arrays.stream(RoutePatternParser.parse(methodPart.toString(), types)).forEach(pattern::add);
      return new RoutePattern(pattern, resource != null ? resource.classArgCount : 0);
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

   private static boolean hasValueOfMethod(Class<?> clazz) {
      if (clazz == String.class || clazz == long.class || clazz == int.class || clazz == short.class
               || clazz == byte.class) {
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
      if (clazz == long.class || clazz == int.class || clazz == short.class || clazz == byte.class
               || clazz == Long.class || clazz == Integer.class || clazz == Short.class
               || clazz == Byte.class) {
         return "int";
      }
      return "any";
   }

   private RoutePattern(List<ITemplatePart> parts, int argCount) {
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
         if (litValue.endsWith("/") && !"/".equals(litValue)) {
            result.remove(lit);
            lit = new LiteralPart(litValue.replaceAll("/$", ""));
            result.add(lit);
         }
      }
      this.parts = Collections.unmodifiableList(result);
      this.classArgCount = argCount;
   }

   @Override
   public String toString() {
      if (parts.isEmpty()) {
         return "/";
      }
      StringBuilder result = new StringBuilder();
      for (ITemplatePart part : parts) {
         result.append(part.toString());
      }
      return result.toString();
   }

   List<ITemplatePart> getParts() {
      return parts;
   }

   int getResourceArgsCount() {
      return classArgCount;
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
