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
package com.lmpessoa.services.core.internal.serializing;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.lmpessoa.services.core.hosting.IApplicationInfo;
import com.lmpessoa.services.core.services.HealthStatus;
import com.lmpessoa.services.core.validating.ErrorSet;
import com.lmpessoa.services.util.ClassUtils;

final class XmlSerializer extends Serializer {

   private static final Map<Class<?>, String> types = new HashMap<>();
   private static final String XML_HEAD = "<?xml version=\"1.0\"?>";

   private final Map<Class<?>, Function<Object, Object>> adapters = new HashMap<>();
   private final Locale[] locales;

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

   public XmlSerializer(Locale[] locales) {
      this.locales = locales;
      adapters.put(ErrorSet.class, this::adaptErrorSet);
      adapters.put(IApplicationInfo.class, this::adaptAppInfo);
   }

   @Override
   @SuppressWarnings("unchecked")
   protected <T> T read(String content, Class<T> type) {
      try {
         JAXBContext context = JAXBContext.newInstance(type);
         Unmarshaller unmarshaller = context.createUnmarshaller();
         return (T) unmarshaller.unmarshal(new StringReader(content));
      } catch (Exception e) {
         throw new SerializationException(e);
      }
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

   private XmlErrorSet adaptErrorSet(Object obj) {
      if (!(obj instanceof ErrorSet)) {
         return null;
      }
      ErrorSet errors = (ErrorSet) obj;
      XmlErrorSet result = new XmlErrorSet();
      errors.forEach(m -> {
         XmlErrorSet.Entry entry = new XmlErrorSet.Entry();
         entry.invalidValue = m.getInvalidValue();
         entry.message = m.getMessage(locales);
         entry.path = m.getPathEntry();
         result.error.add(entry);
      });
      return result;
   }

   private XmlAppInfo adaptAppInfo(Object obj) {
      if (!(obj instanceof IApplicationInfo)) {
         return null;
      }
      IApplicationInfo info = (IApplicationInfo) obj;
      XmlAppInfo result = new XmlAppInfo();
      result.name = info.getName();
      result.status = HealthStatus.OK;
      for (Entry<Class<?>, HealthStatus> entry : info.getServiceHealth().entrySet()) {
         XmlAppInfo.ServiceStatus ss = new XmlAppInfo.ServiceStatus();
         ss.name = JsonSerializer.getServiceName(entry.getKey());
         ss.status = entry.getValue();
         result.service.add(ss);
         if (ss.status != HealthStatus.OK) {
            result.status = HealthStatus.PARTIAL;
         }
      }
      result.uptime = info.getUptime();
      result.memory = info.getUsedMemory();
      return result;
   }

   private String producePrimitive(String name, String value) {
      return XML_HEAD + "<" + name + " value=\"" + value + "\"/>";
   }

   private String produceException(Throwable t) {
      StringBuilder result = new StringBuilder();
      result.append(XML_HEAD);
      result.append("<error type=\"");
      result.append(t.getClass().getSimpleName());
      if (t.getMessage() != null && !t.getLocalizedMessage().isEmpty()) {
         result.append("\" message=\"");
         result.append(t.getLocalizedMessage());
      }
      result.append("\"/>");
      return result.toString();
   }

   private String produceObject(Object obj) throws JAXBException {
      for (Entry<Class<?>, Function<Object, Object>> entry : adapters.entrySet()) {
         if (entry.getKey().isInstance(obj)) {
            obj = entry.getValue().apply(obj);
         }
      }
      JAXBContext context = JAXBContext.newInstance(obj.getClass());
      Marshaller marshaller = context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      marshaller.setProperty("com.sun.xml.internal.bind.indentString", "  ");
      StringWriter result = new StringWriter();
      marshaller.marshal(obj, result);
      return result.toString();
   }

   @XmlRootElement(name = "appInfo")
   static final class XmlAppInfo {

      public String name;
      public HealthStatus status;
      @XmlElementWrapper(name = "services")
      public List<ServiceStatus> service = new ArrayList<>();
      public long uptime;
      public long memory;

      static final class ServiceStatus {

         @XmlAttribute
         public String name;
         @XmlAttribute
         public HealthStatus status;
      }
   }

   @XmlRootElement(name = "errors")
   static final class XmlErrorSet {

      public List<Entry> error = new ArrayList<>();

      static final class Entry {

         @XmlAttribute
         public String path;
         @XmlAttribute
         public String message;
         @XmlAttribute
         public String invalidValue;
      }
   }
}
