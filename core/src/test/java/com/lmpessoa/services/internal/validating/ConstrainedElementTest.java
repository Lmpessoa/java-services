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
package com.lmpessoa.services.internal.validating;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;

import org.junit.Test;

import com.lmpessoa.services.test.validating.Author;
import com.lmpessoa.services.test.validating.Book;
import com.lmpessoa.services.test.validating.ForceStrongPassword;
import com.lmpessoa.services.test.validating.Library;
import com.lmpessoa.services.test.validating.OldAndNewPasswordsDifferent;

public final class ConstrainedElementTest {

   @Test
   public void testAuthorTypeConstraints() {
      ConstrainedType result = ConstrainedElement.of(Author.class);
      assertTrue(result.hasConstraints());

      Map<Class<?>, Set<ConstraintAnnotation>> constraints = result.getConstraints();
      assertEquals(0, constraints.size());

      assertEquals(3, result.getFields().size());
      Iterator<ConstrainedField> fields = result.getFields()
               .stream()
               .filter(ConstrainedField::hasConstraints)
               .iterator();

      ConstrainedField field = fields.next();
      assertEquals("lastName", field.getName());
      assertTrue(field.hasConstraints());
      constraints = field.getConstraints();
      assertEquals(1, constraints.size());
      assertTrue(constraints.containsKey(Default.class));
      ConstraintAnnotation[] carray = constraints.get(Default.class)
               .toArray(new ConstraintAnnotation[0]);
      assertEquals(1, carray.length);
      assertTrue(carray[0].getAnnotation() instanceof NotEmpty);
      assertEquals(0, field.getTypeArguments().size());

      assertFalse(fields.hasNext());

      assertEquals(2, result.getGetterMethods().size());
      Iterator<ConstrainedMethod> methods = result.getGetterMethods()
               .stream()
               .filter(ConstrainedMethod::hasConstraints)
               .sorted((m1, m2) -> m1.getName().compareTo(m2.getName()))
               .iterator();

      ConstrainedMethod method = methods.next();
      assertEquals("getCompany", method.getName());
      assertEquals("company", method.getPropertyName());
      assertTrue(method.isGetter());
      assertTrue(method.hasConstraints());
      assertTrue(method.getConstraints().isEmpty());
      assertEquals(0, method.getParameters().size());
      assertFalse(method.getCrossParameters().hasConstraints());
      assertTrue(method.getReturnValue().hasConstraints());
      constraints = method.getReturnValue().getConstraints();
      assertEquals(1, constraints.size());
      assertTrue(constraints.containsKey(Default.class));
      carray = constraints.get(Default.class).toArray(new ConstraintAnnotation[0]);
      assertEquals(1, carray.length);
      assertTrue(carray[0].getAnnotation() instanceof Size);
      assertEquals(0, carray[0].get("min"));
      assertEquals(25, carray[0].get("max"));
      assertTrue(field.getTypeArguments().isEmpty());

      method = methods.next();
      assertEquals("isFirstName", method.getName());
      assertEquals("firstName", method.getPropertyName());
      assertTrue(method.isGetter());
      assertTrue(method.hasConstraints());
      assertTrue(method.getConstraints().isEmpty());
      assertEquals(0, method.getParameters().size());
      assertFalse(method.getCrossParameters().hasConstraints());
      assertTrue(method.getReturnValue().hasConstraints());
      constraints = method.getReturnValue().getConstraints();
      assertEquals(1, constraints.size());
      assertTrue(constraints.containsKey(Default.class));
      carray = constraints.get(Default.class).toArray(new ConstraintAnnotation[0]);
      assertEquals(1, carray.length);
      assertTrue(carray[0].getAnnotation() instanceof AssertTrue);
      assertTrue(field.getTypeArguments().isEmpty());

      assertFalse(methods.hasNext());
   }

   @Test
   public void testCrossParameterConstraints() throws NoSuchMethodException {
      Method method = Author.class.getDeclaredMethod("renewPassword", String.class, String.class,
               String.class);
      ConstrainedMethod result = ConstrainedElement.of(method);
      assertTrue(result.hasConstraints());
      assertEquals(0,
               result.getParameters()
                        .stream()
                        .filter(ConstrainedParameter::hasConstraints)
                        .count());
      assertFalse(result.getReturnValue().hasConstraints());
      assertTrue(result.getCrossParameters().hasConstraints());
      Map<Class<?>, Set<ConstraintAnnotation>> constraints = result.getCrossParameters()
               .getConstraints();
      assertEquals(1, constraints.keySet().size());
      assertTrue(constraints.containsKey(Default.class));
      ConstraintAnnotation[] carray = constraints.get(Default.class)
               .toArray(new ConstraintAnnotation[0]);
      Arrays.sort(carray, ConstrainedElementTest::compareConstraints);
      assertEquals(2, carray.length);
      assertTrue(carray[0].getAnnotation() instanceof ForceStrongPassword);
      assertEquals("{com.lmpessoa.services.test.validating.ForceStrongPassword.message}",
               carray[0].getMessage());
      assertTrue(carray[1].getAnnotation() instanceof OldAndNewPasswordsDifferent);
      assertEquals("{com.lmpessoa.services.test.validating.OldAndNewPasswordsDifferent.message}",
               carray[1].getMessage());
   }

   @Test
   public void testBookTypeConstraints() {
      ConstrainedType result = ConstrainedElement.of(Book.class);
      assertTrue(result.hasConstraints());
      assertTrue(result.getConstraints().isEmpty());
      assertTrue(result.getGetterMethods().isEmpty());

      assertEquals(8, result.getFields().size());
      ConstraintAnnotation[] carray;
      ConstrainedTypeArgument typearg;
      ConstrainedField[] fields = result.getFields().toArray(new ConstrainedField[0]);
      Arrays.sort(fields, (f1, f2) -> f1.getName().compareTo(f2.getName()));
      Iterator<ConstrainedField> iterator = Arrays.asList(fields).iterator();

      ConstrainedField field = iterator.next();
      Map<Class<?>, Set<ConstraintAnnotation>> constraints = field.getConstraints();

      assertEquals("authors", field.getName());
      assertEquals(1, constraints.size());
      assertTrue(constraints.containsKey(Default.class));
      carray = constraints.get(Default.class).toArray(new ConstraintAnnotation[0]);
      assertEquals(1, carray.length);
      assertTrue(carray[0].getAnnotation() instanceof NotEmpty);
      assertTrue(field.mustBeValid());
      assertFalse(field.getTypeArguments().isEmpty());

      field = iterator.next();
      constraints = field.getConstraints();
      assertEquals("authorsByChapter", field.getName());
      assertTrue(constraints.isEmpty());
      assertEquals(1, field.getTypeArguments().size());
      typearg = field.getTypeArguments().toArray(new ConstrainedTypeArgument[0])[0];
      assertTrue(typearg.getConstraints().isEmpty());
      assertFalse(typearg.mustBeValid());
      assertEquals(1, typearg.getTypeArguments().size());
      typearg = typearg.getTypeArguments().toArray(new ConstrainedTypeArgument[0])[0];
      assertTrue(typearg.getConstraints().isEmpty());
      assertTrue(typearg.mustBeValid());
      assertTrue(typearg.getTypeArguments().isEmpty());

      field = iterator.next();
      constraints = field.getConstraints();
      assertEquals("categories", field.getName());
      assertTrue(constraints.isEmpty());
      assertEquals(1, field.getTypeArguments().size());
      typearg = field.getTypeArguments().toArray(new ConstrainedTypeArgument[0])[0];
      assertTrue(typearg.getConstraints().isEmpty());
      assertTrue(typearg.mustBeValid());
      assertTrue(typearg.getTypeArguments().isEmpty());

      field = iterator.next();
      constraints = field.getConstraints();
      assertEquals("pickedReview", field.getName());
      assertTrue(constraints.isEmpty());
      assertTrue(field.mustBeValid());
      assertTrue(field.getTypeArguments().isEmpty());

      field = iterator.next();
      constraints = field.getConstraints();
      assertEquals("reviewsPerSource", field.getName());
      assertTrue(constraints.isEmpty());
      assertTrue(field.mustBeValid());
      assertFalse(field.getTypeArguments().isEmpty());

      field = iterator.next();
      constraints = field.getConstraints();
      assertEquals("tags", field.getName());
      assertTrue(constraints.isEmpty());
      assertEquals(1, field.getTypeArguments().size());
      typearg = field.getTypeArguments().toArray(new ConstrainedTypeArgument[0])[0];
      constraints = typearg.getConstraints();
      assertEquals(1, constraints.size());
      assertTrue(constraints.containsKey(Default.class));
      carray = constraints.get(Default.class).toArray(new ConstraintAnnotation[0]);
      assertTrue(carray[0].getAnnotation() instanceof NotBlank);
      assertFalse(typearg.mustBeValid());
      assertTrue(typearg.getTypeArguments().isEmpty());

      field = iterator.next();
      constraints = field.getConstraints();
      assertEquals("tagsByChapter", field.getName());
      assertTrue(constraints.isEmpty());
      assertEquals(1, field.getTypeArguments().size());
      typearg = field.getTypeArguments().toArray(new ConstrainedTypeArgument[0])[0];
      assertTrue(typearg.getConstraints().isEmpty());
      assertFalse(typearg.mustBeValid());
      assertEquals(1, typearg.getTypeArguments().size());
      typearg = typearg.getTypeArguments().toArray(new ConstrainedTypeArgument[0])[0];
      constraints = typearg.getConstraints();
      assertEquals(1, constraints.size());
      assertTrue(constraints.containsKey(Default.class));
      carray = constraints.get(Default.class).toArray(new ConstraintAnnotation[0]);
      assertEquals(1, carray.length);
      assertTrue(carray[0].getAnnotation() instanceof NotBlank);
      assertFalse(typearg.mustBeValid());
      assertTrue(typearg.getTypeArguments().isEmpty());

      field = iterator.next();
      constraints = field.getConstraints();
      assertEquals("title", field.getName());
      assertEquals(1, constraints.size());
      assertTrue(constraints.containsKey(Default.class));
      carray = constraints.get(Default.class).toArray(new ConstraintAnnotation[0]);
      assertEquals(1, carray.length);
      assertTrue(carray[0].getAnnotation() instanceof NotEmpty);
      assertFalse(field.mustBeValid());
      assertTrue(field.getTypeArguments().isEmpty());

      assertFalse(iterator.hasNext());
   }

   @Test
   public void testConstructorParameterConstraints() throws NoSuchMethodException {
      Constructor<?> constructor = Library.class.getConstructor(List.class);
      ConstrainedConstructor result = ConstrainedElement.of(constructor);

      assertTrue(result.hasConstraints());
      assertFalse(result.getReturnValue().hasConstraints());
      assertFalse(result.getCrossParameters().hasConstraints());
      assertEquals(1, result.getParameters().size());
      ConstrainedParameter param = result.getParameters().toArray(new ConstrainedParameter[0])[0];

      assertTrue(param.hasConstraints());
      Map<Class<?>, Set<ConstraintAnnotation>> constraints = param.getConstraints();
      assertEquals(1, constraints.size());
      assertTrue(constraints.containsKey(Default.class));
      ConstraintAnnotation[] carray = constraints.get(Default.class)
               .toArray(new ConstraintAnnotation[0]);
      assertEquals(1, carray.length);
      assertTrue(carray[0].getAnnotation() instanceof NotNull);
      assertFalse(param.mustBeValid());
      assertEquals(1, param.getTypeArguments().size());

      ConstrainedTypeArgument typearg = param.getTypeArguments()
               .toArray(new ConstrainedTypeArgument[0])[0];
      assertTrue(typearg.hasConstraints());
      assertTrue(typearg.getConstraints().isEmpty());
      assertTrue(typearg.mustBeValid());
   }

   private static int compareConstraints(ConstraintAnnotation c1, ConstraintAnnotation c2) {
      return c1.getAnnotation().annotationType().getSimpleName().compareTo(
               c2.getAnnotation().annotationType().getSimpleName());
   }
}
