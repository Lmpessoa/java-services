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

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

interface PathNode {

   static ConstructorPathNode ofConstructor(Class<?> clazz, Parameter[] params) {
      return new ConstructorPathNode(clazz, params);
   }

   static MethodPathNode ofMethod(String name, Parameter[] params) {
      return new MethodPathNode(name, params);
   }

   static TypePathNode ofObject(Object object) {
      TypePathNode result = new TypePathNode(null);
      result.setValue(object);
      return result;
   }

   PathNode getParent();

   String getName();

   Object getValue();

   void setValue(Object value);

   abstract static class BasePathNode implements PathNode {

      private final PathNode parent;
      private final String name;

      private Object value;

      BasePathNode(PathNode parent, String name) {
         this.parent = parent;
         this.name = name;
      }

      @Override
      public final PathNode getParent() {
         return parent;
      }

      @Override
      public final String getName() {
         return name;
      }

      @Override
      public final Object getValue() {
         return value;
      }

      @Override
      public final void setValue(Object value) {
         this.value = value;
      }

      @Override
      public String toString() {
         StringBuilder result = new StringBuilder();
         if (getParent() != null) {
            result.append(getParent());
         }
         String realName = this.name;
         if (this instanceof ContainerElementPathNode) {
            if (parent instanceof ContainerElementPathNode) {
               realName = parent.getName();
            } else {
               realName = null;
            }
         }
         if (realName != null) {
            if (result.length() > 0) {
               result.append('.');
            }
            result.append(realName);
         }
         return result.toString();
      }
   }

   static interface ContainerElementProviderPathNode extends PathNode {

      default ContainerElementPathNode addContainerElement(String name, Class<?> containerClass,
         Integer typeArgIndex) {
         return new ContainerElementPathNode(this, name, false, containerClass, typeArgIndex, null,
                  null);
      }
   }

   static interface ParameterProviderPathNode extends PathNode {

      default ParameterPathNode addParameter(int paramIndex) {
         return new ParameterPathNode(this, paramIndex);
      }

      List<Parameter> getParameters();
   }

   static interface PropertyProviderPathNode extends PathNode {

      default PropertyPathNode addProperty(String name) {
         return new PropertyPathNode(this, name);
      }
   }

   static interface TypeProviderPathNode extends PathNode {

      default TypePathNode addType() {
         return new TypePathNode(this);
      }
   }

   static class ConstructorPathNode extends ExecutablePathNode {

      ConstructorPathNode(Class<?> clazz, Parameter[] params) {
         super(clazz.getSimpleName(), params);
      }
   }

   static class ContainerElementPathNode extends BasePathNode
      implements ContainerElementProviderPathNode, PropertyProviderPathNode, TypeProviderPathNode {

      protected final Class<?> containerClass;
      protected final Integer typeArgIndex;
      protected final boolean iterable;
      protected final Integer index;
      protected final Object key;

      ContainerElementPathNode(PathNode parent, String name, boolean iterable,
         Class<?> containerClass, Integer typeArgIndex, Integer index, Object key) {
         super(parent, name);
         this.containerClass = containerClass;
         this.typeArgIndex = typeArgIndex;
         this.iterable = iterable;
         this.index = index;
         this.key = key;
      }

      ContainerElementPathNode iterable() {
         return new ContainerElementPathNode(this.getParent(), this.getName(), true, containerClass,
                  typeArgIndex, index, key);
      }

      ContainerElementPathNode iterableWithIndex(int index) {
         return new ContainerElementPathNode(this.getParent(), this.getName(), true, containerClass,
                  typeArgIndex, index, null);
      }

      ContainerElementPathNode iterableWithKey(Object key) {
         return new ContainerElementPathNode(this.getParent(), this.getName(), true, containerClass,
                  typeArgIndex, null, key);
      }

      @Override
      public String toString() {
         StringBuilder result = new StringBuilder();
         result.append(super.toString());
         if (containerClass == Map.class && typeArgIndex == 0) {
            result.append("<K>");
         }
         if (iterable || index != null || key != null) {
            result.append('[');
            if (index != null) {
               result.append(index);
            } else if (key != null) {
               result.append(key);
            }
            result.append(']');
         }
         return result.toString();
      }
   }

   static class CrossParameterPathNode extends BasePathNode implements ParameterProviderPathNode {

      CrossParameterPathNode(ExecutablePathNode parent) {
         super(parent, "<cross-parameter>");
      }

      @Override
      public ParameterPathNode addParameter(int paramIndex) {
         return new ParameterPathNode((ExecutablePathNode) this.getParent(), paramIndex);
      }

      @Override
      public List<Parameter> getParameters() {
         return ((ExecutablePathNode) this.getParent()).getParameters();
      }
   }

   static class ExecutablePathNode extends BasePathNode implements ParameterProviderPathNode {

      private final List<Parameter> params;

      ExecutablePathNode(String name, Parameter[] params) {
         super(null, name);
         this.params = Arrays.asList(Objects.requireNonNull(params));
      }

      CrossParameterPathNode addCrossParameter() {
         return new CrossParameterPathNode(this);
      }

      @Override
      public List<Parameter> getParameters() {
         return params;
      }
   }

   static class MethodPathNode extends ExecutablePathNode {

      MethodPathNode(String name, Parameter[] params) {
         super(name, params);
      }

      ReturnValuePathNode addReturnValue() {
         return new ReturnValuePathNode(this);
      }
   }

   static class ParameterPathNode extends BasePathNode
      implements ContainerElementProviderPathNode, PropertyProviderPathNode, TypeProviderPathNode {

      ParameterPathNode(ParameterProviderPathNode parent, int paramIndex) {
         super(parent, parent.getParameters().get(paramIndex).getName());
      }
   }

   static class PropertyPathNode extends BasePathNode
      implements ContainerElementProviderPathNode, PropertyProviderPathNode, TypeProviderPathNode {

      PropertyPathNode(PropertyProviderPathNode parent, String name) {
         super(parent, name);
      }
   }

   static class ReturnValuePathNode extends BasePathNode
      implements ContainerElementProviderPathNode, PropertyProviderPathNode, TypeProviderPathNode {

      ReturnValuePathNode(MethodPathNode parent) {
         super(parent, "<return value>");
      }
   }

   static class TypePathNode extends BasePathNode implements PropertyProviderPathNode {

      TypePathNode(PathNode parent) {
         super(parent, null);
      }
   }
}
