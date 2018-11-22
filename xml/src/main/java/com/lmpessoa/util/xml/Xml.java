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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.annotation.XmlRootElement;

public final class Xml {

   private static final String HEADER = "<?xml version=\"1.0\"?>\n";

   private final Map<Class<?>, Object> adapters = new HashMap<>();

   public <T> void registerAdapter(Class<T> baseType, Object typeAdapter) {
      if (!(typeAdapter instanceof XmlSerializer || typeAdapter instanceof XmlDeserializer)) {
         throw new XmlSerializeException("{Xml.1}");
      }
      adapters.put(baseType, typeAdapter);
   }

   public String toXml(Object object) throws XmlSerializeException {
      XmlFieldInfo field = new XmlFieldInfo() {

         @Override
         public boolean isRequired() {
            return false;
         }

         @Override
         public Object getValueIn(Object src) {
            return object;
         }

         @Override
         public String getNameFor(Object value) {
            XmlRootElement xroot = object.getClass().getAnnotation(XmlRootElement.class);
            if (xroot != null && !"##default".equals(xroot.name())) {
               return xroot.name();
            }
            return object.getClass().getSimpleName();
         }
      };
      XmlSerializationContext context = new XmlSerializationContextImpl(field);
      return HEADER + context.serialize(object, field).toString();
   }

   public <T> T fromXml(String xml, Class<T> classOfT) {
      return null;
   }

   @SuppressWarnings("unchecked")
   private <T> XmlSerializer<T> getSerializer(Class<? extends T> baseType) {
      List<Class<?>> types = getAllSuperTypes(baseType);
      return (XmlSerializer<T>) types.stream() //
               .map(adapters::get)
               .filter(Objects::nonNull)
               .filter(a -> a instanceof XmlSerializer)
               .findFirst()
               .orElse(DefaultXmlTypeAdapter.getDefault());
   }

   @SuppressWarnings("unchecked")
   private <T> XmlDeserializer<T> getDeserializer(Class<? extends T> baseType) {
      List<Class<?>> types = getAllSuperTypes(baseType);
      return (XmlDeserializer<T>) types.stream() //
               .map(adapters::get)
               .filter(Objects::nonNull)
               .filter(a -> a instanceof XmlDeserializer)
               .findFirst()
               .orElse(DefaultXmlTypeAdapter.getDefault());
   }

   private List<Class<?>> getAllSuperTypes(Class<?> baseType) {
      List<Class<?>> types = new ArrayList<>();
      Class<?> type = baseType;
      while (type != null) {
         types.add(type);
         if (type.getSuperclass() != Object.class) {
            types.add(type.getSuperclass());
         }
         for (Class<?> intf : type.getInterfaces()) {
            types.add(intf);
         }
         if (type.getSuperclass() != Object.class) {
            type = type.getSuperclass();
         } else {
            type = null;
         }
      }
      return Collections.unmodifiableList(types);
   }

   private class XmlSerializationContextImpl implements XmlSerializationContext {

      private final XmlFieldInfo field;

      XmlSerializationContextImpl(XmlFieldInfo field) {
         this.field = Objects.requireNonNull(field);
      }

      @Override
      public <T> XmlElement serialize(T src, Class<? extends T> classOfSrc, XmlFieldInfo field) {
         XmlSerializer<T> adapter = getSerializer(classOfSrc);
         try {
            return adapter.serialize(src, classOfSrc, new XmlSerializationContextImpl(field));
         } catch (Exception e) {
            throw new XmlSerializeException(e);
         }
      }

      @Override
      public XmlFieldInfo getFieldInfo() {
         return field;
      }
   }

   @SuppressWarnings("unused")
   private class XmlDeserializationContextImpl implements XmlDeserializationContext {

      @Override
      public <T> T deserialize(XmlElement src, Class<? extends T> classOfT) {
         XmlDeserializer<T> adapter = getDeserializer(classOfT);
         try {
            return adapter.deserialize(src, classOfT, this);
         } catch (Exception e) {
            throw new XmlSerializeException(e);
         }
      }
   }
}
