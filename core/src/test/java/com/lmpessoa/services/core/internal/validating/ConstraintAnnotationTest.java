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
package com.lmpessoa.services.core.internal.validating;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.junit.Test;

import com.lmpessoa.services.core.internal.validating.ConstraintAnnotation;

public final class ConstraintAnnotationTest {

   @NotNull
   private String value;

   @Test
   public void testMultipleAnnotations() throws NoSuchMethodException {
      Method method = ConstraintAnnotationTest.class.getMethod("duplicateConstraints");
      Set<ConstraintAnnotation> result = ConstraintAnnotation.of(method);
      ConstraintAnnotation[] values = result.toArray(new ConstraintAnnotation[0]);
      assertEquals(1, values.length);
      assertTrue(values[0].getAnnotation() instanceof NotNull);
   }

   @Test
   public void testMultipleDifferentAnnotations() throws NoSuchMethodException {
      Method method = ConstraintAnnotationTest.class.getMethod("differentConstraints");
      Set<ConstraintAnnotation> result = ConstraintAnnotation.of(method);
      ConstraintAnnotation[] values = result.toArray(new ConstraintAnnotation[0]);
      assertEquals(2, values.length);
      assertTrue(values[0].getAnnotation() instanceof Min);
      assertEquals(9L, values[0].get("value"));
      assertTrue(values[1].getAnnotation() instanceof Min);
      assertEquals(4L, values[1].get("value"));
   }

   @Test
   public void testParamWithNoConstraint() throws NoSuchMethodException {
      Method method = ConstraintAnnotationTest.class.getMethod("paramWithConstraints", int.class,
               String.class);
      Set<ConstraintAnnotation> result = ConstraintAnnotation.of(method.getParameters()[0]);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testParamWithConstraint() throws NoSuchMethodException {
      Method method = ConstraintAnnotationTest.class.getMethod("paramWithConstraints", int.class,
               String.class);
      Set<ConstraintAnnotation> result = ConstraintAnnotation.of(method.getParameters()[1]);
      ConstraintAnnotation[] values = result.toArray(new ConstraintAnnotation[0]);
      assertEquals(1, values.length);
      assertTrue(values[0].getAnnotation() instanceof NotNull);
   }

   @Test
   public void testConstrainedField() throws NoSuchFieldException {
      Field field = ConstraintAnnotationTest.class.getDeclaredField("value");
      Set<ConstraintAnnotation> result = ConstraintAnnotation.of(field);
      ConstraintAnnotation[] values = result.toArray(new ConstraintAnnotation[0]);
      assertEquals(1, values.length);
      assertTrue(values[0].getAnnotation() instanceof NotNull);
   }

   @Test
   public void testConstrainedResultElement() throws NoSuchMethodException {
      Method method = ConstraintAnnotationTest.class.getMethod("listElement", List.class);
      AnnotatedType type = method.getAnnotatedReturnType();
      assertTrue(type instanceof AnnotatedParameterizedType);
      AnnotatedParameterizedType ptype = (AnnotatedParameterizedType) type;
      Set<ConstraintAnnotation> result = ConstraintAnnotation
               .of(ptype.getAnnotatedActualTypeArguments()[0]);
      ConstraintAnnotation[] values = result.toArray(new ConstraintAnnotation[0]);
      assertEquals(1, values.length);
      assertTrue(values[0].getAnnotation() instanceof NotEmpty);
   }

   @Test
   public void testConstrainedParamElement() {
      // Nothing to do here
   }

   @NotNull
   @NotNull
   public String duplicateConstraints() {
      return null;
   }

   @Min(4)
   @Min(9)
   public int differentConstraints() {
      return 0;
   }

   public String paramWithConstraints(int id, @NotNull String value) {
      return null;
   }

   public Set<@NotEmpty String> listElement(List<@NotEmpty String> values) {
      return new HashSet<>(values);
   }
}
