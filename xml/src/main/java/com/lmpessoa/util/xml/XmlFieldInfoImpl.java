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
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlInlineBinaryData;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

final class XmlFieldInfoImpl implements XmlFieldInfo {

   private static final String DEFAULT = "##default";

   private final Field field;

   @Override
   public boolean isRequired() {
      if (field.isAnnotationPresent(XmlAttribute.class)) {
         return field.getAnnotation(XmlAttribute.class).required();
      } else if (field.isAnnotationPresent(XmlElement.class)) {
         return field.getAnnotation(XmlElement.class).required();
      } else if (field.isAnnotationPresent(XmlElementRef.class)) {
         return field.getAnnotation(XmlElementRef.class).required();
      }
      return false;
   }

   @Override
   public Object getValueIn(Object src) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getNameFor(Object value) {
      String result = DEFAULT;
      if (field.isAnnotationPresent(XmlAttribute.class)) {
         result = field.getAnnotation(XmlAttribute.class).name();
      } else if (field.isAnnotationPresent(XmlElement.class)) {
         result = field.getAnnotation(XmlElement.class).name();
      } else if (field.isAnnotationPresent(XmlElementRef.class)) {
         result = field.getType().getAnnotation(XmlRootElement.class).name();
         result = DEFAULT.equals(result) ? field.getType().getSimpleName() : result;
      } else if (field.isAnnotationPresent(XmlElements.class)) {
         XmlElement[] xelems = field.getAnnotation(XmlElements.class).value();
         for (XmlElement xelem : xelems) {
            if (xelem.type() == XmlElement.DEFAULT.class || xelem.type().isInstance(value)) {
               result = xelem.name();
               break;
            }
         }
      }
      return DEFAULT.equals(result) ? field.getName() : result;
   }

   static boolean nonTransient(Field field) {
      return !Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())
               && !field.isAnnotationPresent(XmlTransient.class);
   }

   XmlFieldInfoImpl(Field field) {
      this.field = Objects.requireNonNull(field);
      Set<Class<? extends Annotation>> annotations = Arrays
               .asList(XmlAttribute.class, XmlAnyAttribute.class, XmlAnyElement.class, XmlElement.class,
                        XmlElements.class, XmlElementRef.class, XmlElementWrapper.class, XmlEnumValue.class,
                        XmlList.class, XmlValue.class)
               .stream()
               .filter(field::isAnnotationPresent)
               .collect(Collectors.toSet());
      // @XmlAttribute
      XmlTypeInfo.validateCompatibleAnnotations(annotations, XmlAttribute.class, XmlID.class, XmlIDREF.class,
               XmlList.class, XmlSchemaType.class, XmlValue.class, XmlAttachmentRef.class, XmlMimeType.class,
               XmlInlineBinaryData.class);
      // @XmlAnyAttribute
      if (XmlTypeInfo.validateCompatibleAnnotations(annotations, XmlAnyAttribute.class)) {
         if (!Map.class.isAssignableFrom(field.getType())) {
            throw new XmlSerializeException("{XmlFieldInfo.2}");
         }
      }
      // @XmlAnyElement
      XmlTypeInfo.validateCompatibleAnnotations(annotations, XmlAnyElement.class, XmlElementRef.class,
               XmlElementRefs.class, XmlElementWrapper.class, XmlList.class, XmlMixed.class);
      // @XmlElement
      XmlTypeInfo.validateCompatibleAnnotations(annotations, XmlElement.class, XmlID.class, XmlIDREF.class,
               XmlList.class, XmlSchemaType.class, XmlValue.class, XmlAttachmentRef.class, XmlMimeType.class,
               XmlInlineBinaryData.class, XmlElementWrapper.class);
      // @XmlElements
      XmlTypeInfo.validateCompatibleAnnotations(annotations, XmlElements.class, XmlIDREF.class,
               XmlElementWrapper.class);
      // @XmlElementRef
      if (XmlTypeInfo.validateCompatibleAnnotations(annotations, XmlElementRef.class, XmlElementWrapper.class)) {
         if (field.getType() == JAXBElement.class) {
            throw new XmlSerializeException("{XmlFieldInfo.3}");
         } else if (!field.getType().isAnnotationPresent(XmlRootElement.class)) {
            throw new XmlSerializeException("{XmlFieldInfo.4}");
         }
      }
      // @XmlElementWrapper
      if (XmlTypeInfo.validateCompatibleAnnotations(annotations, XmlElementWrapper.class, XmlElement.class,
               XmlElements.class, XmlElementRef.class, XmlElementRefs.class)) {
         if (!Collection.class.isAssignableFrom(field.getType())) {
            throw new XmlSerializeException("{XmlFieldInfo.5}");
         }
      }
      // @XmlList
      XmlTypeInfo.validateCompatibleAnnotations(annotations, XmlList.class, XmlElement.class, XmlAttribute.class,
               XmlValue.class, XmlIDREF.class);
      // @XmlValue
      XmlTypeInfo.validateCompatibleAnnotations(annotations, XmlValue.class, XmlList.class);
      // @XmlEnumValue
      XmlTypeInfo.validateCompatibleAnnotations(annotations, XmlEnumValue.class);
   }

   String getWrapperName() {
      if (isWrapped()) {
         return field.getAnnotation(XmlElementWrapper.class).name();
      }
      return null;
   }

   boolean isAttribute() {
      return field.isAnnotationPresent(XmlAttribute.class);
   }

   boolean isElement() {
      return !isAttribute() && !isAnyAttribute() && !isAnyElement() && !isValue();
   }

   boolean isWrapped() {
      return field.isAnnotationPresent(XmlElementWrapper.class);
   }

   boolean isAnyAttribute() {
      return field.isAnnotationPresent(XmlAnyAttribute.class);
   }

   boolean isAnyElement() {
      return field.isAnnotationPresent(XmlAnyElement.class);
   }

   boolean isValue() {
      return field.isAnnotationPresent(XmlValue.class);
   }

   boolean isList() {
      return field.isAnnotationPresent(XmlList.class);
   }

   // XmlAttribute xattr = field.getAnnotation(XmlAttribute.class);
   // javax.xml.bind.annotation.XmlElement xelem =
   // field.getAnnotation(javax.xml.bind.annotation.XmlElement.class);
   // if (xattr != null && xelem != null) {
   // throw new XmlSerializeException("{XmlFieldInfo.1}");
   // }
   // if (xattr != null) {
   // name = coalesce(xattr.name(), field.getName());
   // namespace = coalesce(xattr.namespace(), null);
   // required = xattr.required();
   // nillable = true;
   // attribute = true;
   // any = field.isAnnotationPresent(XmlAnyAttribute.class) &&
   // Map.class.isAssignableFrom(field.getType());
   // } else {
   // if (field.isAnnotationPresent(XmlElementRef.class)) {
   // XmlRootElement xroot = field.getType().getAnnotation(XmlRootElement.class);
   // if (xroot == null) {
   // throw new XmlSerializeException("{XmlFieldInfo.2}");
   // }
   // name = coalesce(xroot.name(), field.getType().getSimpleName());
   // } else {
   // name = coalesce(xelem.name(), field.getName());
   // }
   // namespace = coalesce(xelem.namespace(), null);
   // required = xelem.required();
   // nillable = xelem.nillable();
   // attribute = false;
   // any = field.isAnnotationPresent(XmlAnyElement.class);
   // }
   //
   // }
   // String getName() {
   // String result = null;
   // if (field.isAnnotationPresent(XmlAttribute.class)) {
   // result = field.getAnnotation(XmlAttribute.class).name();
   // } else if (field.isAnnotationPresent(XmlElement.class)) {
   // result = field.getAnnotation(XmlElement.class).name();
   // } else if (field.isAnnotationPresent(XmlElements.class)) {
   // XmlElement[] elems = field.getAnnotation(XmlElements.class).value();
   // for (XmlElement elem : elems) {
   // if (elem.type() == XmlElement.DEFAULT.class || elem.type().isAssignableFrom(value)) {
   // result = elem.name();
   // break;
   // }
   // }
   // } else if (field.isAnnotationPresent(XmlElementRef.class)) {
   // result = field.getType().getAnnotation(XmlRootElement.class).name();
   // String className = field.getType().getSimpleName();
   // className = Character.toLowerCase(className.charAt(0)) + className.substring(1);
   // result = coalesce(result, className);
   // }
   // return coalesce(result, field.getName());
   // }
   //
   // String getNamespace() {
   // String result = null;
   // if (field.isAnnotationPresent(XmlAttribute.class)) {
   // result = field.getAnnotation(XmlAttribute.class).namespace();
   // } else if (field.isAnnotationPresent(XmlElement.class)) {
   // result = field.getAnnotation(XmlElement.class).namespace();
   // } else if (field.isAnnotationPresent(XmlElements.class)) {
   // XmlElement[] elems = field.getAnnotation(XmlElements.class).value();
   // for (XmlElement elem : elems) {
   // if (elem.type() == XmlElement.DEFAULT.class || elem.type().isAssignableFrom(value)) {
   // result = elem.namespace();
   // break;
   // }
   // }
   // } else if (field.isAnnotationPresent(XmlElementRef.class)) {
   // result = field.getType().getAnnotation(XmlRootElement.class).namespace();
   // }
   // return coalesce(result, null);
   // }
   //
   // String getQualifiedName() {
   // String namespace = getNamespace();
   // String name = getName();
   // if (namespace == null) {
   // return name;
   // }
   // return String.format("%s:%s", namespace, name);
   // }
   //
   // boolean isAttribute() {
   // return field.isAnnotationPresent(XmlAttribute.class);
   // }
   //
   // boolean isRequired() {
   // if (field.isAnnotationPresent(XmlAttribute.class)) {
   // return field.getAnnotation(XmlAttribute.class).required();
   // } else if (field.isAnnotationPresent(XmlElement.class)) {
   // return field.getAnnotation(XmlElement.class).required();
   // } else if (field.isAnnotationPresent(XmlElements.class)) {
   //
   // } else if (field.isAnnotationPresent(XmlElementRef.class)) {
   // return field.getAnnotation(XmlElementRef.class).required();
   // }
   // return false;
   // }
   //
   // boolean isNillable() {
   // return nillable;
   // }
   //
   // boolean isAny() {
   // return any;
   // }
   //
   // <T> T getAnnotationValue(Supplier<T> operation) {
   //
   // }
   //
   // String coalesce(String value, String defaultValue) {
   // return value == null || "##default".equals(value) ? defaultValue : value;
   // }
}
