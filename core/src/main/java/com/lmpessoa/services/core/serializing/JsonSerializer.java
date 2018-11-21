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

import java.lang.reflect.Type;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.lmpessoa.services.core.hosting.IApplicationInfo;
import com.lmpessoa.services.core.services.HealthStatus;
import com.lmpessoa.services.core.services.Service;

final class JsonSerializer extends Serializer {

   private final Gson gson = new GsonBuilder() //
            .registerTypeHierarchyAdapter(IApplicationInfo.class, new AppInfoSerialiser())
            .disableHtmlEscaping()
            .create();

   @Override
   protected <T> T read(String content, Class<T> type) {
      return gson.fromJson(content, type);
   }

   @Override
   protected String write(Object object) {
      try {
         return gson.toJson(object);
      } catch (Exception e) {
         return null;
      }
   }

   static class AppInfoSerialiser implements com.google.gson.JsonSerializer<IApplicationInfo> {

      @Override
      public JsonElement serialize(IApplicationInfo src, Type typeOfSrc,
         JsonSerializationContext context) {
         JsonObject result = new JsonObject();
         result.addProperty("app", src.getName());
         HealthStatus status = HealthStatus.OK;

         JsonObject services = new JsonObject();
         for (Entry<Class<?>, HealthStatus> entry : src.getServiceHealth().entrySet()) {
            final String serviceName = getServiceName(entry.getKey());
            services.addProperty(serviceName, entry.getValue().name());
            if (entry.getValue() != HealthStatus.OK) {
               status = HealthStatus.PARTIAL;
            }
         }

         result.addProperty("status", status.name());
         if (services.size() > 0) {
            result.add("services", services);
         }

         result.addProperty("uptime", src.getUptime());
         result.addProperty("memory", src.getUsedMemory());
         return result;
      }

      static String getServiceName(Class<?> serviceClass) {
         Service ann = serviceClass.getAnnotation(Service.class);
         if (!ann.name().isEmpty()) {
            return ann.name();
         }
         String result = serviceClass.getSimpleName();
         if (result.matches("I[A-Z][a-z].+")) {
            result = result.substring(1);
         }
         if (result.endsWith("Service")) {
            result = result.substring(0, result.length() - 7);
         }
         return Character.toLowerCase(result.charAt(0)) + result.substring(1);
      }
   }
}
