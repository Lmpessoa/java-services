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
package com.lmpessoa.services.views.liquid.internal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.lmpessoa.services.views.liquid.LiquidEngine;
import com.lmpessoa.services.views.liquid.LiquidFilterFunction;
import com.lmpessoa.services.views.liquid.LiquidRenderizationContext;
import com.lmpessoa.services.views.templating.RenderizationContext;

public class RenderizationContextImpl implements LiquidRenderizationContext {

   private static final DateTimeFormatter[] DATE_PARSERS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_INSTANT, DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.RFC_1123_DATE_TIME, DateTimeFormatter.BASIC_ISO_DATE,
            DateTimeFormatter.ISO_DATE };
   private final Map<String, Integer> steps = new HashMap<>();

   private final LiquidEngine engine;

   private RenderizationContext context;
   private State state = State.NONE;

   @Override
   public RenderizationContext data() {
      return context;
   }

   @Override
   public String doBlock(Supplier<String> block) {
      context = new RenderizationContext(context);
      try {
         return block.get();
      } finally {
         context = context.getParent();
      }
   }

   public RenderizationContextImpl(LiquidEngine engine, RenderizationContext context) {
      this.context = context;
      this.engine = engine;
   }

   LiquidFilterFunction getFilter(String name) {
      if ("date".equals(name)) {
         return this::date;
      }
      return engine.getFilter(name);
   }

   boolean isInterrupted() {
      return state != State.NONE;
   }

   boolean shouldContinue() {
      return state != State.BREAK;
   }

   void resetLoop() {
      state = State.NONE;
   }

   void interrupt(boolean shouldContinue) {
      state = shouldContinue ? State.CONTINUE : State.BREAK;
   }

   int getSequence(String name) {
      int result = steps.getOrDefault(name, 0);
      steps.put(name, result + 1);
      return result;
   }

   private Object date(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      if (value instanceof String) {
         value = tryParseDate((String) value);
      } else if (value instanceof Date) {
         value = ((Date) value).toInstant();
      }
      if (value instanceof TemporalAccessor) {
         LiquidDateFormatter fmt = new LiquidDateFormatter(context.getLocale());
         return fmt.format((TemporalAccessor) value, args[0].toString());
      }
      return value;
   }

   private TemporalAccessor tryParseDate(String value) {
      if ("now".equals(value)) {
         return LocalDateTime.now();
      } else if ("today".equals(value)) {
         return LocalDate.now();
      }
      for (DateTimeFormatter fmt : DATE_PARSERS) {
         try {
            return fmt.parse(value);
         } catch (Exception e) {
            // Ignore and try the next
         }
      }
      return null;
   }

   private enum State {
      NONE, BREAK, CONTINUE;
   }

}
