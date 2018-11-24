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
package com.lmpessoa.services.core.internal.serializing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.lmpessoa.services.util.ClassUtils;

final class TemporalAccessorAdapter
   implements JsonSerializer<TemporalAccessor>, JsonDeserializer<TemporalAccessor> {

   @Override
   public JsonElement serialize(TemporalAccessor src, Type typeOfSrc,
      JsonSerializationContext context) {
      return new JsonPrimitive(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(src));
   }

   @Override
   public TemporalAccessor deserialize(JsonElement json, Type typeOfT,
      JsonDeserializationContext context) {
      Method parser = ClassUtils.getMethod((Class<?>) typeOfT, "parse", CharSequence.class);
      if (parser == null || !Modifier.isStatic(parser.getModifiers())) {
         throw new JsonParseException("Cannot parse value to " + typeOfT.getTypeName());
      }
      try {
         return (TemporalAccessor) parser.invoke(null, json.getAsString());
      } catch (InvocationTargetException e) {
         throw new JsonParseException(e.getCause());
      } catch (IllegalAccessException | IllegalArgumentException e) {
         throw new JsonParseException(e);
      }
   }
}
