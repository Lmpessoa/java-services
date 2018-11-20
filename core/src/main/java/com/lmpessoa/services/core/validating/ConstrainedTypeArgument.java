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

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Map;

final class ConstrainedTypeArgument extends ConstrainedContainer<AnnotatedType>
   implements Comparable<ConstrainedTypeArgument> {

   private final ConstrainedContainer<?> owner;
   private final int index;

   @Override
   public int compareTo(ConstrainedTypeArgument o) {
      return getIndex() - o.getIndex();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (!(obj instanceof ConstrainedTypeArgument)) {
         return false;
      }
      return compareTo((ConstrainedTypeArgument) obj) == 0;
   }

   @Override
   public int hashCode() {
      return getElement().hashCode();
   }

   ConstrainedTypeArgument(ConstrainedContainer<?> owner, AnnotatedType type, int index) {
      super(type);
      this.owner = owner;
      this.index = index;
   }

   @Override
   boolean mustBeValid() {
      Class<?> type = owner.getContainerType();
      return super.mustBeValid() || (type != Map.class || index != 0) && owner.mustBeValid();
   }

   @Override
   String getName() {
      Class<?> type = owner.getContainerType();
      if (type == List.class) {
         return "<list element>";
      } else if (type == Iterable.class) {
         return "<iterable element>";
      } else if (type == Map.class) {
         return index == 0 ? "<map key>" : "<map value>";
      }
      return null;
   }

   @Override
   AnnotatedType getAnnotatedType() {
      return getElement();
   }

   @Override
   Class<?> getElementType() {
      String typeName = getElement().getType().getTypeName();
      if (typeName.contains("<")) {
         typeName = typeName.substring(0, typeName.indexOf('<'));
      }
      try {
         return Class.forName(typeName);
      } catch (ClassNotFoundException e) {
         return null;
      }
   }

   @Override
   Class<?> getEnclosingType() {
      return owner.getEnclosingType();
   }

   int getIndex() {
      return index;
   }
}
