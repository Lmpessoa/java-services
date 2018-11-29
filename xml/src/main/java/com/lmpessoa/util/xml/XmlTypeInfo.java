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
package com.lmpessoa.util.xml;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSchemaTypes;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

final class XmlTypeInfo {

   private final Collection<XmlFieldInfoImpl> fields;
   private final Class<?> type;

   @SafeVarargs
   static boolean validateCompatibleAnnotations(Set<Class<? extends Annotation>> annotations,
      Class<? extends Annotation>... compatibleAnnotations) {
      if (compatibleAnnotations.length > 0 && annotations.contains(compatibleAnnotations[0])) {
         List<Class<? extends Annotation>> ann = Arrays.asList(compatibleAnnotations);
         if (annotations.stream().anyMatch(t -> !ann.contains(t))) {
            throw new XmlSerializeException("{XmlAnyInfo.1}");
         }
         return true;
      }
      return false;
   }

   public XmlTypeInfo(Class<?> type) {
      this.type = Objects.requireNonNull(type);
      Set<Class<? extends Annotation>> annotations = Arrays
               .asList(XmlAccessorOrder.class, XmlEnum.class, XmlRootElement.class,
                        XmlSeeAlso.class, XmlTransient.class, XmlType.class)
               .stream()
               .filter(type::isAnnotationPresent)
               .collect(Collectors.toSet());
      // @XmlTransient
      validateCompatibleAnnotations(annotations, XmlTransient.class);
      // @XmlAccessorOrder
      validateCompatibleAnnotations(annotations, XmlAccessorOrder.class, XmlType.class,
               XmlRootElement.class, XmlAccessorType.class, XmlSchema.class, XmlSchemaType.class,
               XmlSchemaTypes.class);
      // @XmlEnum
      if (validateCompatibleAnnotations(annotations, XmlEnum.class, XmlType.class,
               XmlRootElement.class)) {
         if (!type.isEnum()) {
            throw new XmlSerializeException("{XmlTypeInfo.1}");
         }
      }
      // @XmlRootElement
      validateCompatibleAnnotations(annotations, XmlRootElement.class, XmlType.class, XmlEnum.class,
               XmlAccessorType.class, XmlAccessorOrder.class);
      // @XmlSeeAlso
      validateCompatibleAnnotations(annotations, XmlSeeAlso.class);
      // @XmlType
      validateCompatibleAnnotations(annotations, XmlType.class, XmlRootElement.class,
               XmlAccessorOrder.class, XmlAccessorType.class, XmlEnum.class);

      this.fields = getAllDeclaredFields(type).stream() //
               .filter(XmlFieldInfoImpl::nonTransient)
               .map(XmlFieldInfoImpl::new)
               .collect(Collectors.toList());
      // @XmlAnyAttribute
      if (fields.stream().filter(XmlFieldInfoImpl::isAnyAttribute).count() > 1) {
         throw new XmlSerializeException("{XmlTypeInfo.2}");
      }
      // @XmlAnyElement
      if (fields.stream().filter(XmlFieldInfoImpl::isAnyElement).count() > 1) {
         throw new XmlSerializeException("{XmlTypeInfo.3}");
      }
      // @XmlValue
      long valueCount = fields.stream().filter(XmlFieldInfoImpl::isValue).count();
      if (valueCount > 1) {
         throw new XmlSerializeException("{XmlTypeInfo.4}");
      } else if (valueCount == 1
               && fields.stream().filter(XmlFieldInfoImpl::isElement).count() > 0) {
         throw new XmlSerializeException("{XmlTypeInfo.5}");
      }
   }

   public Class<?> getType() {
      return type;
   }

   public Collection<XmlFieldInfo> getFields() {
      return fields.stream().filter(XmlFieldInfoImpl::isElement).collect(Collectors.toSet());
   }

   public Collection<XmlFieldInfo> getAttributeFields() {
      return fields.stream().filter(XmlFieldInfoImpl::isAttribute).collect(Collectors.toSet());
   }

   public boolean hasValueField() {
      return getValueField() != null;
   }

   public XmlFieldInfo getValueField() {
      return fields.stream().filter(XmlFieldInfoImpl::isValue).findFirst().orElse(null);
   }

   private Collection<Field> getAllDeclaredFields(Class<?> type) {
      List<Field> result = new ArrayList<>();
      while (type != Object.class) {
         Arrays.stream(type.getDeclaredFields()).filter(XmlFieldInfoImpl::nonTransient).forEach(
                  result::add);
         type = type.getSuperclass();
      }
      return result;
   }
}
