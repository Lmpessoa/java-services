/*
 * Copyright (c) 2017 Leonardo Pessoa
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
package com.lmpessoa.services.core.serializing;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.lmpessoa.services.util.ClassUtils;

final class XmlSerializer extends Serializer {

   private static final String XML_HEAD = "<?xml version=\"1.0\"?>";
   private static final Map<Class<?>, String> types = new HashMap<>();
   static {
      types.put(String.class, "string");
      types.put(Long.class, "long");
      types.put(Integer.class, "int");
      types.put(Short.class, "short");
      types.put(Byte.class, "byte");
      types.put(Double.class, "double");
      types.put(Float.class, "float");
      types.put(Boolean.class, "boolean");
   }

   @Override
   @SuppressWarnings("unchecked")
   protected <T> T read(String content, Class<T> type) throws Exception {
      JAXBContext context = JAXBContext.newInstance(type);
      Unmarshaller unmarshaller = context.createUnmarshaller();
      return (T) unmarshaller.unmarshal(new StringReader(content));
   }

   @Override
   protected String write(Object object) {
      String result;
      Class<?> type = ClassUtils.box(object.getClass());
      if (types.containsKey(type)) {
         result = producePrimitive(types.get(type), object.toString());
      } else if (object instanceof Throwable) {
         result = produceException((Throwable) object);
      } else {
         try {
            result = produceObject(object);
         } catch (JAXBException e) {
            result = null;
         }
      }
      return result;
   }

   private String producePrimitive(String name, String value) {
      return XML_HEAD + "<" + name + " value=\"" + value + "\"/>";
   }

   private String produceException(Throwable t) {
      StringBuilder result = new StringBuilder();
      result.append(XML_HEAD);
      result.append("<exception type=\"");
      result.append(t.getClass().getSimpleName());
      if (t.getMessage() != null && !t.getMessage().isEmpty()) {
         result.append("\" message=\"");
         result.append(t.getMessage());
      }
      result.append("\"/>");
      return result.toString();
   }

   private String produceObject(Object obj) throws JAXBException {
      JAXBContext context = JAXBContext.newInstance(obj.getClass());
      Marshaller marshaller = context.createMarshaller();
      StringWriter result = new StringWriter();
      marshaller.marshal(obj, result);
      return result.toString();
   }
}
