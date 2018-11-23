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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import javax.validation.ClockProvider;
import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.ContainerElementNodeBuilderCustomizableContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.ContainerElementNodeBuilderDefinedContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.ContainerElementNodeContextBuilder;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderCustomizableContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderDefinedContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeContextBuilder;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeContextBuilder;

import com.lmpessoa.services.core.internal.validating.PathNode.CrossParameterPathNode;
import com.lmpessoa.services.core.internal.validating.PathNode.ParameterProviderPathNode;
import com.lmpessoa.services.core.internal.validating.PathNode.PropertyProviderPathNode;
import com.lmpessoa.services.core.internal.validating.PathNode.TypePathNode;
import com.lmpessoa.services.core.internal.validating.PathNode.TypeProviderPathNode;
import com.lmpessoa.services.util.ClassUtils;

final class ValidationContext implements ConstraintValidatorContext {

   private final Set<Violation> violations = new LinkedHashSet<>();
   private final ConstraintAnnotation constraint;
   private final PathNode path;
   private final Clock clock;

   private boolean defaultViolation = true;

   @Override
   public void disableDefaultConstraintViolation() {
      defaultViolation = false;
   }

   @Override
   public String getDefaultConstraintMessageTemplate() {
      return constraint.getMessage();
   }

   @Override
   public ClockProvider getClockProvider() {
      return () -> clock;
   }

   @Override
   public ConstraintViolationBuilder buildConstraintViolationWithTemplate(String messageTemplate) {
      return new NodeBuilder(path, Objects.requireNonNull(messageTemplate));
   }

   @Override
   public <T> T unwrap(Class<T> type) {
      throw new UnsupportedOperationException();
   }

   ValidationContext(ConstraintAnnotation constraint, PathNode entry, Clock clock) {
      this.constraint = constraint;
      this.path = entry;
      this.clock = clock;
   }

   Set<Violation> getViolations() {
      if (defaultViolation || violations.isEmpty()) {
         return Collections.singleton(new Violation(constraint, path));
      }
      return violations;
   }

   class NodeBuilder implements ConstraintViolationBuilder, NodeBuilderDefinedContext,
      LeafNodeBuilderDefinedContext, ContainerElementNodeBuilderDefinedContext,
      NodeBuilderCustomizableContext, LeafNodeBuilderCustomizableContext,
      ContainerElementNodeBuilderCustomizableContext, NodeContextBuilder, LeafNodeContextBuilder,
      ContainerElementNodeContextBuilder {

      private final String messageTemplate;
      private PathNode path;

      @Override
      public NodeBuilder atKey(Object key) {
         ifViolationalreadyAdded();
         return null;
      }

      @Override
      public NodeBuilder atIndex(Integer index) {
         ifViolationalreadyAdded();
         return null;
      }

      @Override
      public NodeBuilder inIterable() {
         ifViolationalreadyAdded();
         return null;
      }

      @Override
      public NodeBuilder inContainer(Class<?> containerClass, Integer typeArgumentIndex) {
         ifViolationalreadyAdded();
         return null;
      }

      @Override
      public NodeBuilder addNode(String name) {
         ifViolationalreadyAdded();
         if (path instanceof PropertyProviderPathNode) {
            Object value = path.getValue();
            if (value != null) {
               Field field = ClassUtils.getField(value.getClass(), name);
               Method method = ClassUtils.getMethod(value.getClass(),
                        "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
               try {
                  if (field != null) {
                     value = field.get(value);
                  } else if (method != null) {
                     value = method.invoke(value);
                  } else {
                     value = null;
                  }
               } catch (Exception e) {
                  // Just ignore; value will be null
               }
               if (value != null) {
                  path = ((PropertyProviderPathNode) path).addProperty(name);
                  path.setValue(value);
               }
            }
         }
         return this;
      }

      @Override
      public NodeBuilderCustomizableContext addPropertyNode(String name) {
         return addNode(name);
      }

      @Override
      public LeafNodeBuilderCustomizableContext addBeanNode() {
         ifViolationalreadyAdded();
         if (path instanceof TypeProviderPathNode) {
            Object value = path.getValue();
            path = ((TypeProviderPathNode) path).addType();
            path.setValue(value);
            return this;
         }
         return null;
      }

      @Override
      public NodeBuilderDefinedContext addParameterNode(int index) {
         ifViolationalreadyAdded();
         if (path instanceof ParameterProviderPathNode) {
            Object[] params = (Object[]) path.getValue();
            path = ((ParameterProviderPathNode) path).addParameter(index);
            if (params != null) {
               path.setValue(params[index]);
            }
         }
         return this;
      }

      @Override
      public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name,
         Class<?> containerType, Integer typeArgumentIndex) {
         ifViolationalreadyAdded();
         return this;
      }

      @Override
      public ConstraintValidatorContext addConstraintViolation() {
         ifViolationalreadyAdded();
         if (path instanceof CrossParameterPathNode || path instanceof TypePathNode) {
            path = path.getParent();
         }
         violations.add(new Violation(constraint, path, messageTemplate));
         defaultViolation = false;
         path = null;
         return ValidationContext.this;
      }

      NodeBuilder(PathNode entry, String messageTemplate) {
         this.messageTemplate = messageTemplate;
         this.path = entry;
      }

      private void ifViolationalreadyAdded() {
         if (path == null) {
            throw new ConstraintDefinitionException(new IllegalStateException());
         }
      }
   }
}
