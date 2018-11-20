/*
 * Copyright (c) 2018 Leonardo Pessoa
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

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.validation.GroupSequence;
import javax.validation.UnexpectedTypeException;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Negative;
import javax.validation.constraints.NegativeOrZero;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Null;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;

import com.lmpessoa.services.util.ClassUtils;
import com.lmpessoa.services.util.parsing.IVariablePart;

final class VariableRoutePart implements IVariablePart, Comparable<VariableRoutePart> {

   private final Collection<Class<?>> groups;
   private final BigDecimal minValue;
   private final BigDecimal maxValue;
   private final Class<?> paramType;
   private final String paramName;
   private final boolean catchall;
   private final String pattern;
   private final int paramIndex;

   @Override
   public String toString() {
      return pattern;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof VariableRoutePart) {
         return compareTo((VariableRoutePart) obj) == 0;
      }
      return false;
   }

   boolean isSimilarTo(Parameter param) {
      Parameter[] params = param.getDeclaringExecutable().getParameters();
      int index = -1;
      for (int i = 0; i < params.length; ++i) {
         if (params[i] == param) {
            index = i;
            break;
         }
      }
      return index == paramIndex && param.getName().equals(paramName)
               && param.getType() == paramType;

   }

   @Override
   public int compareTo(VariableRoutePart otherVar) {
      if (getParameterIndex() != otherVar.getParameterIndex()) {
         return otherVar.getParameterIndex() - getParameterIndex();
      }
      int typeNum = typeNumberOf(paramType);
      int otherTypeNum = typeNumberOf(otherVar.paramType);
      int cmp = otherTypeNum - typeNum;
      if (cmp != 0) {
         return cmp;
      }
      cmp = innerCompare(otherVar.getRegexPattern(), getRegexPattern());
      if (cmp != 0) {
         return cmp;
      }
      cmp = innerCompare(getMaxValue(), otherVar.getMaxValue());
      if (cmp != 0) {
         return cmp;
      }
      return innerCompare(otherVar.getMinValue(), getMinValue());
   }

   @Override
   public int hashCode() {
      return pattern.hashCode();
   }

   VariableRoutePart(Class<?> resourceClass, Executable exec, int paramIndex) {
      this.paramIndex = paramIndex;
      if (paramIndex >= exec.getParameterCount()) {
         throw new ArrayIndexOutOfBoundsException(
                  String.format("Wrong parameter count in route (found: %d, expected: %d)",
                           paramIndex + 1, exec.getParameterCount()));
      }
      Parameter param = exec.getParameters()[paramIndex];
      this.paramName = param.getName();
      this.paramType = param.getType();
      this.catchall = param.isVarArgs();
      GroupSequence groupSeq = resourceClass.getAnnotation(GroupSequence.class);
      if (groupSeq != null) {
         this.groups = Arrays.asList(groupSeq.value());
      } else {
         this.groups = Collections.singleton(Default.class);
      }
      if (param.isAnnotationPresent(QueryParam.class)) {
         throw new IllegalStateException("Query params cannot be in path");
      }
      if (!getConstraints(param, Null.class).isEmpty()) {
         throw new IllegalStateException("Path argument cannot be null");
      }
      this.minValue = getMinValue(param);
      this.maxValue = getMaxValue(param);

      String sizePattern = getSizePattern(param);
      String typePattern = getTypePattern(param.getType());
      this.pattern = getPattern(param, sizePattern, typePattern);
   }

   BigDecimal getMinValue() {
      return minValue;
   }

   BigDecimal getMaxValue() {
      return maxValue;
   }

   String getRegexPattern() {
      return pattern;
   }

   int getParameterIndex() {
      return paramIndex;
   }

   String getParameterName() {
      return paramName;
   }

   boolean isAssignableTo(Class<?> otherType) {
      return otherType.isAssignableFrom(paramType);
   }

   boolean isCatchAll() {
      return catchall;
   }

   private <T extends Comparable<T>> int innerCompare(T o1, T o2) {
      if (o1 != null && o2 != null) {
         return o1.compareTo(o2);
      } else if (o1 != null) {
         return -1;
      } else if (o2 != null) {
         return 1;
      }
      return 0;
   }

   private int typeNumberOf(Class<?> type) {
      if (type == String.class) {
         return 0;
      } else if (type == byte.class || type == Byte.class) {
         return 1;
      } else if (type == short.class || type == Short.class) {
         return 2;
      } else if (type == int.class || type == Integer.class) {
         return 3;
      } else if (type == long.class || type == Long.class) {
         return 4;
      }
      return 5;
   }

   private BigDecimal getMinValue(Parameter source) {
      BigDecimal result = null;
      BigDecimal min = getConstraints(source, Min.class).stream() //
               .map(m -> BigDecimal.valueOf(m.value()))
               .min(BigDecimal::compareTo)
               .orElse(result);
      if (min != null) {
         result = min;
      }
      min = getConstraints(source, DecimalMin.class).stream() //
               .map(m -> new BigDecimal(m.value()))
               .min(BigDecimal::compareTo)
               .orElse(result);
      if (min != null && (result == null || min.compareTo(result) < 0)) {
         result = min;
      }
      if (!getConstraints(source, Positive.class).isEmpty()
               && (result == null || BigDecimal.ONE.compareTo(result) < 0)) {
         result = BigDecimal.ONE;
      }
      if (!getConstraints(source, PositiveOrZero.class).isEmpty()
               && (result == null || BigDecimal.ZERO.compareTo(result) < 0)) {
         result = BigDecimal.ZERO;
      }
      if (result != null && !isNumberType(source.getType())) {
         throw new UnexpectedTypeException(
                  "Expected number type, was " + source.getType().getName());
      }
      return result;
   }

   private BigDecimal getMaxValue(Parameter source) {
      BigDecimal result = null;
      BigDecimal max = getConstraints(source, Max.class).stream() //
               .map(m -> BigDecimal.valueOf(m.value()))
               .min(BigDecimal::compareTo)
               .orElse(result);
      if (max != null) {
         result = max;
      }
      max = getConstraints(source, DecimalMax.class).stream() //
               .map(m -> new BigDecimal(m.value()))
               .min(BigDecimal::compareTo)
               .orElse(result);
      if (max != null && (result == null || max.compareTo(result) > 0)) {
         result = max;
      }
      if (!getConstraints(source, Negative.class).isEmpty()
               && (result == null || BigDecimal.ONE.compareTo(result) > 0)) {
         result = BigDecimal.ONE;
      }
      if (!getConstraints(source, NegativeOrZero.class).isEmpty()
               && (result == null || BigDecimal.ZERO.compareTo(result) > 0)) {
         result = BigDecimal.ZERO;
      }
      if (result != null && !isNumberType(source.getType())) {
         throw new UnexpectedTypeException(
                  "Expected number type, was " + source.getType().getName());
      }
      return result;
   }

   private boolean isNumberType(Class<?> type) {
      return type == byte.class || type == short.class || type == int.class || type == long.class
               || type == Byte.class || type == Short.class || type == Integer.class
               || type == Long.class || type == BigInteger.class || type == BigDecimal.class;
   }

   private String getSizePattern(Parameter source) {
      Collection<Size> sizes = getConstraints(source, Size.class);
      Integer min = sizes.stream().map(Size::min).min(Integer::compareTo).orElse(null);
      if (min != null && min <= 0) {
         min = null;
      }
      Integer max = sizes.stream().map(Size::max).max(Integer::compareTo).orElse(null);
      if (max != null && max == Integer.MAX_VALUE) {
         max = null;
      }
      if (min == null && max == null) {
         return null;
      } else if (source.getType() != String.class) {
         throw new UnexpectedTypeException(
                  "Expected string type, was " + source.getType().getName());
      }
      StringBuilder result = new StringBuilder("[^\\/]{");
      if (min != null) {
         result.append(min);
      }
      result.append(',');
      if (max != null) {
         result.append(max);
      }
      result.append('}');
      return result.toString();
   }

   private String getTypePattern(Class<?> type) {
      if (isNumberType(type)) {
         return "\\d+";
      }
      if (type == UUID.class) {
         return "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
      }
      return null;
   }

   private String getPattern(Parameter source, String sizePattern, String typePattern) {
      List<String> result = new ArrayList<>();
      getConstraints(source, Pattern.class).stream() //
               .map(Pattern::regexp)
               .forEach(result::add);
      if (sizePattern != null) {
         result.add(sizePattern);
      }
      if (!result.isEmpty() && source.getType() != String.class) {
         throw new UnexpectedTypeException(
                  "Expected string type, was " + source.getType().getName());
      }
      if (typePattern != null) {
         result.add(typePattern);
      }
      if (result.isEmpty()) {
         result.add("[^\\/]+");
      }
      for (int i = 0; i < result.size() - 1; ++i) {
         result.set(i, "(?=" + result.get(i) + ")");
      }
      String resultStr = String.join("", result);
      if (catchall) {
         resultStr = "(?:\\/" + resultStr + ")";
         resultStr += getConstraints(source, NotEmpty.class).isEmpty() ? "*" : "+";
      }
      return "(" + resultStr + ")";
   }

   @SuppressWarnings("unchecked")
   private <T extends Annotation> Collection<T> getConstraints(Parameter source,
      Class<T> annotationClass) {
      Method groupsMethod = ClassUtils.getMethod(annotationClass, "groups");
      List<T> result = new ArrayList<>();
      T ann = source.getAnnotation(annotationClass);
      if (ann != null && isInGroup(ann, groupsMethod)) {
         result.add(ann);
      }
      Repeatable repeat = annotationClass.getAnnotation(Repeatable.class);
      if (repeat != null) {
         Method value = ClassUtils.getMethod(repeat.value(), "value");
         Annotation listAnn = source.getAnnotation(repeat.value());
         if (listAnn != null) {
            try {
               for (T annt : (T[]) value.invoke(listAnn)) {
                  if (isInGroup(annt, groupsMethod)) {
                     result.add(annt);
                  }
               }
            } catch (IllegalAccessException | IllegalArgumentException
                     | InvocationTargetException e) {
               // Should not happen but...
            }
         }
      }
      return result;
   }

   private boolean isInGroup(Annotation ann, Method groupsMethod) {
      if (groupsMethod == null) {
         return true;
      }
      try {
         Class<?>[] annGroups = (Class<?>[]) groupsMethod.invoke(ann);
         if (annGroups.length == 0) {
            annGroups = new Class<?>[] { Default.class };
         }
         for (Class<?> annGroup : annGroups) {
            if (groups.contains(annGroup)) {
               return true;
            }
         }
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
         // Should not happen but...
      }
      return false;
   }
}
