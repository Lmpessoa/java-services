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

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;

final class DefaultXmlTypeAdapter implements XmlSerializer<Object>, XmlDeserializer<Object> {

   private static final DefaultXmlTypeAdapter DEFAULT = new DefaultXmlTypeAdapter();
   private final Object adapter;

   public static DefaultXmlTypeAdapter getDefault() {
      return DEFAULT;
   }

   public DefaultXmlTypeAdapter(Object adapter) {
      Objects.requireNonNull(adapter);
      if (!(adapter instanceof XmlSerializer<?> || adapter instanceof XmlDeserializer<?>)) {
         throw new IllegalArgumentException("Not a valid type adapter");
      }
      this.adapter = adapter;
   }

   @Override
   @SuppressWarnings("unchecked")
   public XmlElement serialize(Object src, Class<?> typeOfSrc, XmlSerializationContext context) {
      if (adapter instanceof XmlSerializer<?>) {
         return ((XmlSerializer<Object>) adapter).serialize(src, typeOfSrc, context);
      }
      return defaultSerialize(src, typeOfSrc, context);
   }

   @Override
   @SuppressWarnings("unchecked")
   public Object deserialize(XmlElement xml, Class<?> typeOfT, XmlDeserializationContext context) {
      if (adapter instanceof XmlDeserializer<?>) {
         return ((XmlDeserializer<Object>) adapter).deserialize(xml, typeOfT, context);
      }
      return defaultDeserialize(xml, typeOfT, context);
   }

   private DefaultXmlTypeAdapter() {
      this.adapter = null;
   }

   // @XmlAccessorOrder
   // @XmlAccessorType
   // @XmlEnum
   // @XmlEnumValue
   // @XmlJavaTypeAdapters
   // @XmlSchemaTypes
   // @XmlSeeAlso
   // @XmlType
   private XmlElement defaultSerialize(Object src, Class<?> typeOfSrc, XmlSerializationContext context) {
      if (src instanceof Boolean) {
         return new XmlPrimitive((Boolean) src);
      } else if (src instanceof Character) {
         return new XmlPrimitive((Character) src);
      } else if (src instanceof Number) {
         return new XmlPrimitive((Number) src);
      } else if (src instanceof CharSequence) {
         return new XmlPrimitive((CharSequence) src);
      }

      XmlFieldInfo field = context.getFieldInfo();
      if (src instanceof Iterable) {
         XmlArray result = new XmlArray();
         Iterator<?> iterator = ((Iterable<?>) src).iterator();
         while (iterator.hasNext()) {
            Object value = iterator.next();
            XmlElement element = context.serialize(value, field);
            if (element instanceof XmlArray || element instanceof XmlPrimitive) {
               XmlObject tmp = new XmlObject(field.getNameFor(value));
               tmp.setValue(element);
               element = tmp;
            }
            result.add(element);
         }
      }

      XmlObject result = new XmlObject(field.getNameFor(src));

      XmlTypeInfo childType = new XmlTypeInfo(typeOfSrc);
      for (XmlFieldInfo childAttrField : childType.getAttributeFields()) {
         Object value = childAttrField.getValueIn(src);
         XmlElement element = context.serialize(value, childAttrField);
         if (element instanceof XmlArray) {
            if (!((XmlArray) element).stream().allMatch(e -> e instanceof XmlPrimitive)) {
               throw new XmlSerializeException("{DefaultXmlTypeAdapter.1}");
            }
            String concat = ((XmlArray) element).stream() //
                     .map(e -> ((XmlPrimitive) e).getAsString())
                     .collect(Collectors.joining(" "));
            element = new XmlPrimitive(concat);
         }
         if (!(element instanceof XmlPrimitive)) {
            throw new XmlSerializeException("{DefaultXmlTypeAdapter.2}");
         }
         result.addAttribute(childAttrField.getNameFor(value), (XmlPrimitive) element);
      }
      if (childType.hasValueField()) {
         Object value = childType.getValueField().getValueIn(src);
         result.setValue(context.serialize(value, childType.getValueField()));
      } else {
         XmlArray fields = new XmlArray();
         result.setValue(fields);
         for (XmlFieldInfo childField : childType.getFields()) {
            Object value = childField.getValueIn(src);
            XmlElement element = context.serialize(value, childField);
            if (element instanceof XmlPrimitive) {
               XmlObject tmp = new XmlObject(childField.getNameFor(value));
               tmp.setValue(element);
               element = tmp;
            } else if (element instanceof XmlArray) {
               if (((XmlFieldInfoImpl) childField).isWrapped()) {
                  XmlObject tmp = new XmlObject(((XmlFieldInfoImpl) childField).getWrapperName());
                  tmp.setValue(element);
                  element = tmp;
               } else {
                  fields.addAll((XmlArray) element);
               }
            }
         }
      }

      return result;
   }

   private Object defaultDeserialize(XmlElement xml, Class<?> typeOfT, XmlDeserializationContext context) {
      // TODO Auto-generated method stub
      return null;
   }

}
