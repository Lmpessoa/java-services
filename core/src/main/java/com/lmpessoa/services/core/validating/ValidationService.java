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
package com.lmpessoa.services.core.validating;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.validation.GroupSequence;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.groups.Default;

import com.lmpessoa.services.core.validating.PathNode.ContainerElementPathNode;
import com.lmpessoa.services.core.validating.PathNode.ContainerElementProviderPathNode;
import com.lmpessoa.services.core.validating.PathNode.CrossParameterPathNode;
import com.lmpessoa.services.core.validating.PathNode.ExecutablePathNode;
import com.lmpessoa.services.core.validating.PathNode.MethodPathNode;
import com.lmpessoa.services.core.validating.PathNode.ParameterPathNode;
import com.lmpessoa.services.core.validating.PathNode.PropertyPathNode;
import com.lmpessoa.services.core.validating.PathNode.ReturnValuePathNode;
import com.lmpessoa.services.core.validating.PathNode.TypePathNode;
import com.lmpessoa.services.core.validating.PathNode.TypeProviderPathNode;

final class ValidationService implements IValidationService {

   static IValidationService instance;

   @Override
   public ErrorSet validate(Object object) {
      ConstrainedType ctype = ConstrainedElement.of(object.getClass());
      TypePathNode path = PathNode.ofObject(object);
      for (Class<?> group : getGroups(object.getClass())) {
         Set<Violation> result = validate(path, ctype, object, group);
         if (!result.isEmpty()) {
            return new ErrorSet(result);
         }
      }
      return ErrorSet.EMPTY;
   }

   @Override
   public ErrorSet validateParameters(Object object, Method method, Object[] paramValues) {
      if (!method.getDeclaringClass().isAssignableFrom(object.getClass())) {
         throw new ValidationException("Object does not have a " + method.toString());
      }
      ConstrainedMethod cmethod = ConstrainedElement.of(method);
      MethodPathNode path = PathNode.ofMethod(method.getName(), method.getParameters());
      path.setValue(paramValues);
      for (Class<?> group : getGroups(object.getClass())) {
         Set<Violation> result = validateParameters(path, cmethod, group);
         if (!result.isEmpty()) {
            return new ErrorSet(result);
         }
      }
      return ErrorSet.EMPTY;
   }

   @Override
   public ErrorSet validateReturnValue(Object object, Method method, Object returnValue) {
      if (!method.getDeclaringClass().isAssignableFrom(object.getClass())) {
         throw new ValidationException("Object does not have a " + method.toString());
      }
      ConstrainedMethod cmethod = ConstrainedElement.of(method);
      ReturnValuePathNode path = PathNode.ofMethod(method.getName(), method.getParameters()).addReturnValue();
      path.setValue(returnValue);
      ConstrainedReturnValue crv = cmethod.getReturnValue();
      for (Class<?> group : getGroups(object.getClass())) {
         Set<Violation> result = validate(path, crv, group);
         if (!result.isEmpty()) {
            return new ErrorSet(result);
         }
      }
      return ErrorSet.EMPTY;
   }

   Set<Violation> validateParameters(ExecutablePathNode path, ConstrainedExecutable<?> exec, Class<?> group) {
      Set<Violation> result = new HashSet<>();
      ConstrainedParameter[] params = exec.getParameters().toArray(new ConstrainedParameter[0]);
      ConstrainedCrossParameter crossParams = exec.getCrossParameters();
      Object[] paramValues = (Object[]) path.getValue();
      CrossParameterPathNode crossEntry = path.addCrossParameter();
      crossEntry.setValue(paramValues);
      validate(crossEntry, crossParams, group).forEach(result::add);
      for (int i = 0; i < params.length; ++i) {
         ParameterPathNode param = path.addParameter(i);
         param.setValue(paramValues[i]);
         validate(param, params[i], group).forEach(result::add);
      }
      return Collections.unmodifiableSet(result);
   }

   Set<Violation> validate(PathNode path, ConstrainedElement<?> element, Class<?> group) {
      Set<Violation> result = new HashSet<>();
      Set<ConstraintAnnotation> constraints = element.getConstraints().get(group);
      if (constraints != null) {
         validateConstraints(constraints, path).forEach(result::add);
      }
      return Collections.unmodifiableSet(result);
   }

   Set<Violation> validate(ContainerElementProviderPathNode path, ConstrainedContainer<?> container, Class<?> group) {
      Set<Violation> result = new HashSet<>();
      validate(path, (ConstrainedElement<?>) container, group).forEach(result::add);
      Object value = path.getValue();
      for (ConstrainedTypeArgument typearg : container.getTypeArguments()) {
         if (value instanceof Map) {
            validate(path, typearg, (Map<?, ?>) value, group).forEach(result::add);
         } else if (value instanceof List) {
            validate(path, typearg, (List<?>) value, group).forEach(result::add);
         } else if (value instanceof Iterable) {
            validate(path, typearg, (Iterable<?>) value, group).forEach(result::add);
         }
      }
      if (value != null && container.mustBeValid() && path instanceof TypeProviderPathNode) {
         ConstrainedType type = ConstrainedElement.of(value.getClass());
         validate(((TypeProviderPathNode) path).addType(), type, value, group).forEach(result::add);
      }
      return Collections.unmodifiableSet(result);
   }

   Set<Violation> validate(TypePathNode path, ConstrainedType type, Object object, Class<?> group) {
      Set<Violation> result = new HashSet<>();
      Set<ConstraintAnnotation> constraints = type.getConstraints().get(group);
      if (constraints != null) {
         validateConstraints(constraints, path).forEach(result::add);
      }
      for (ConstrainedField field : type.getFields()) {
         try {
            field.getElement().setAccessible(true);
            PropertyPathNode child = path.addProperty(field.getName());
            child.setValue(field.getElement().get(object));
            validate(child, field, group).forEach(result::add);
         } catch (IllegalAccessException e) {
            throw new ValidationException(e);
         }
      }
      for (ConstrainedMethod getter : type.getGetterMethods()) {
         try {
            getter.getElement().setAccessible(true);
            String propertyName = getter.getPropertyName();
            PropertyPathNode child = path.addProperty(propertyName);
            child.setValue(getter.getElement().invoke(object));
            validate(child, getter.getReturnValue(), group).forEach(result::add);
         } catch (IllegalAccessException e) {
            throw new ValidationException(e);
         } catch (InvocationTargetException e) {
            throw new ValidationException(e.getCause());
         }
      }
      return Collections.unmodifiableSet(result);
   }

   Set<Violation> validate(ContainerElementProviderPathNode path, ConstrainedTypeArgument typearg, Iterable<?> iterable,
      Class<?> group) {
      Set<Violation> result = new HashSet<>();
      for (Object value : iterable) {
         ContainerElementPathNode child = path.addContainerElement("<iterable element>", Iterable.class, 0).iterable();
         child.setValue(value);
         validate(child, typearg, group).forEach(result::add);
      }
      return Collections.unmodifiableSet(result);
   }

   Set<Violation> validate(ContainerElementProviderPathNode path, ConstrainedTypeArgument typearg, List<?> list,
      Class<?> group) {
      Set<Violation> result = new HashSet<>();
      int i = 0;
      for (Object value : list) {
         ContainerElementPathNode child = path.addContainerElement("<list element>", List.class, 0)
                  .iterableWithIndex(i);
         child.setValue(value);
         validate(child, typearg, group).forEach(result::add);
         i += 1;
      }
      return Collections.unmodifiableSet(result);
   }

   Set<Violation> validate(ContainerElementProviderPathNode path, ConstrainedTypeArgument typearg, Map<?, ?> map,
      Class<?> group) {
      Set<Violation> result = new HashSet<>();
      for (Entry<?, ?> mapentry : map.entrySet()) {
         int typeArgIndex = typearg.getIndex();
         ContainerElementPathNode child = path
                  .addContainerElement(typeArgIndex == 0 ? "<map key>" : "<map value>", Map.class, typeArgIndex)
                  .iterableWithKey(mapentry.getKey());
         child.setValue(typeArgIndex == 0 ? mapentry.getKey() : mapentry.getValue());
         validate(child, typearg, group).forEach(result::add);
      }
      return Collections.unmodifiableSet(result);
   }

   Set<Violation> validateConstraints(Set<ConstraintAnnotation> constraints, PathNode path) {
      Set<Violation> result = new HashSet<>();
      for (ConstraintAnnotation constraint : constraints) {
         constraint.validate(path).forEach(result::add);
      }
      return Collections.unmodifiableSet(result);
   }

   Set<Class<?>> getGroups(Class<?> clazz) {
      GroupSequence sequence = clazz.getAnnotation(GroupSequence.class);
      if (sequence != null && sequence.value().length > 0) {
         return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(sequence.value())));
      }
      return Collections.singleton(Default.class);
   }

   static class ValidatorWrapper implements IValidationService {

      private final Validator validator;

      @Override
      public ErrorSet validate(Object object) {
         return new ErrorSet(validator.validate(object, Default.class));
      }

      @Override
      public ErrorSet validateParameters(Object object, Method method, Object[] paramValues) {
         return new ErrorSet(validator.forExecutables().validateParameters(object, method, paramValues, Default.class));
      }

      @Override
      public ErrorSet validateReturnValue(Object object, Method method, Object returnValue) {
         return new ErrorSet(
                  validator.forExecutables().validateReturnValue(object, method, returnValue, Default.class));
      }

      ValidatorWrapper(Validator validator) {
         this.validator = validator;
      }
   }
}
