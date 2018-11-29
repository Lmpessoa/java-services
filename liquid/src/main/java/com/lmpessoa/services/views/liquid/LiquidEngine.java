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
package com.lmpessoa.services.views.liquid;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.lmpessoa.services.views.liquid.internal.AssignTagElement;
import com.lmpessoa.services.views.liquid.internal.CaptureTagElement;
import com.lmpessoa.services.views.liquid.internal.CaseTagElement;
import com.lmpessoa.services.views.liquid.internal.CommentTagElement;
import com.lmpessoa.services.views.liquid.internal.CycleTagElement;
import com.lmpessoa.services.views.liquid.internal.ForTagElement;
import com.lmpessoa.services.views.liquid.internal.IfTagElement;
import com.lmpessoa.services.views.liquid.internal.InterruptTagElement;
import com.lmpessoa.services.views.liquid.internal.LiquidParser;
import com.lmpessoa.services.views.liquid.internal.MarkerTagElement;
import com.lmpessoa.services.views.liquid.internal.RawTagElement;
import com.lmpessoa.services.views.liquid.internal.TablerowTagElement;
import com.lmpessoa.services.views.liquid.internal.UnlessTagElement;
import com.lmpessoa.services.views.liquid.internal.WhenTagElement;
import com.lmpessoa.services.views.liquid.parsing.TagElementBuilder;
import com.lmpessoa.services.views.templating.RenderizationEngine;
import com.lmpessoa.services.views.templating.TemplateParseException;

public final class LiquidEngine implements RenderizationEngine<LiquidTemplate> {

   private final Map<String, Class<? extends TagElementBuilder>> builders = new HashMap<>();
   private final Map<String, LiquidFilterFunction> filters = new HashMap<>();

   public LiquidEngine() {
      builders.put("assign", AssignTagElement.Builder.class);
      builders.put("capture", CaptureTagElement.Builder.class);
      builders.put("endcapture", MarkerTagElement.Builder.class);
      builders.put("case", CaseTagElement.Builder.class);
      builders.put("comment", CommentTagElement.Builder.class);
      builders.put("endcomment", MarkerTagElement.Builder.class);
      builders.put("raw", RawTagElement.Builder.class);
      builders.put("endraw", MarkerTagElement.Builder.class);
      builders.put("when", WhenTagElement.Builder.class);
      builders.put("endcase", MarkerTagElement.Builder.class);
      builders.put("cycle", CycleTagElement.Builder.class);
      builders.put("for", ForTagElement.Builder.class);
      builders.put("break", InterruptTagElement.Builder.class);
      builders.put("continue", InterruptTagElement.Builder.class);
      builders.put("endfor", MarkerTagElement.Builder.class);
      builders.put("if", IfTagElement.Builder.class);
      builders.put("elsif", IfTagElement.Builder.class);
      builders.put("else", MarkerTagElement.Builder.class);
      builders.put("endif", MarkerTagElement.Builder.class);
      builders.put("unless", UnlessTagElement.Builder.class);
      builders.put("endunless", MarkerTagElement.Builder.class);
      builders.put("tablerow", TablerowTagElement.Builder.class);
      builders.put("endtablerow", MarkerTagElement.Builder.class);

      filters.put("abs", LiquidEngine::abs);
      filters.put("append", LiquidEngine::append);
      filters.put("at_least", LiquidEngine::atLeast);
      filters.put("at_most", LiquidEngine::atMost);
      filters.put("capitalize", LiquidEngine::capitalize);
      filters.put("ceil", LiquidEngine::ceil);
      filters.put("compact", LiquidEngine::compact);
      filters.put("concat", LiquidEngine::concat);
      filters.put("date", LiquidEngine::date);
      filters.put("default", LiquidEngine::coalesce);
      filters.put("divided_by", LiquidEngine::dividedBy);
      filters.put("downcase", LiquidEngine::downcase);
      filters.put("escape", LiquidEngine::escape);
      filters.put("escape_once", LiquidEngine::escapeOnce);
      filters.put("first", LiquidEngine::first);
      filters.put("floor", LiquidEngine::floor);
      filters.put("join", LiquidEngine::join);
      filters.put("last", LiquidEngine::last);
      filters.put("lstrip", LiquidEngine::lstrip);
      filters.put("map", LiquidEngine::map);
      filters.put("minus", LiquidEngine::minus);
      filters.put("modulo", LiquidEngine::modulo);
      filters.put("newline_to_br", LiquidEngine::newlineToBr);
      filters.put("plus", LiquidEngine::plus);
      filters.put("prepend", LiquidEngine::prepend);
      filters.put("remove", LiquidEngine::remove);
      filters.put("remove_first", LiquidEngine::removeFirst);
      filters.put("replace", LiquidEngine::replace);
      filters.put("replace_first", LiquidEngine::replaceFirst);
      filters.put("reverse", LiquidEngine::reverse);
      filters.put("round", LiquidEngine::round);
      filters.put("rstrip", LiquidEngine::rstrip);
      filters.put("size", LiquidEngine::size);
      filters.put("slice", LiquidEngine::slice);
      filters.put("sort", LiquidEngine::sort);
      filters.put("sort_natural", LiquidEngine::sortNatural);
      filters.put("split", LiquidEngine::split);
      filters.put("strip", LiquidEngine::strip);
      filters.put("strip_html", LiquidEngine::stripHtml);
      filters.put("strip_newlines", LiquidEngine::stripNewlines);
      filters.put("times", LiquidEngine::times);
      filters.put("truncate", LiquidEngine::truncate);
      filters.put("truncatewords", LiquidEngine::truncateWords);
      filters.put("uniq", LiquidEngine::uniq);
      filters.put("upcase", LiquidEngine::upcase);
      filters.put("url_decode", LiquidEngine::urlDecode);
      filters.put("url_encode", LiquidEngine::urlEncode);
   }

   @Override
   public LiquidTemplate parse(String filename, String template) throws TemplateParseException {
      return new LiquidTemplate(this, LiquidParser.parse(this, filename, template));
   }

   public void registerTagBuilder(String name, Class<? extends TagElementBuilder> builder) {
      builders.put(name, builder);
   }

   public TagElementBuilder getTagBuilder(String name) {
      Class<? extends TagElementBuilder> builder = builders.get(name);
      try {
         return builder.newInstance();
      } catch (Exception e) {
         return null;
      }
   }

   public void registerFilter(String name, LiquidFilterFunction function) {
      filters.put(name, function);
   }

   public LiquidFilterFunction getFilter(String name) {
      return filters.get(name);
   }

   private static Object[] arrayOf(Object value) {
      if (value != null) {
         if (value instanceof Map<?, ?>) {
            return ((Map<?, ?>) value).values().toArray();
         }
         if (!value.getClass().isArray() && !(value instanceof Collection)) {
            return null;
         }
         return LiquidParser.arrayOf(value);
      }
      return null;
   }

   private static Object numberOf(Object value) {
      if (!(value instanceof BigDecimal)) {
         try {
            value = new BigDecimal(value.toString());
         } catch (Exception e) {
            // Ignore and value will remain the same
         }
      }
      return value;
   }

   private static Object getProperty(Object value, String name) {
      if (value instanceof Map) {
         return ((Map<?, ?>) value).get(name);
      } else {
         Method[] methods = Arrays.stream(value.getClass().getMethods()) //
                  .filter(m -> m.getName().equalsIgnoreCase("get" + name)
                           && m.getParameterCount() == 0 && m.getReturnType() != void.class)
                  .toArray(Method[]::new);
         if (methods.length == 1) {
            try {
               return methods[0].invoke(value);
            } catch (IllegalAccessException | IllegalArgumentException
                     | InvocationTargetException e) {
               e.printStackTrace();
            }
         }
      }
      return null;
   }

   private static Object abs(Object value, Object[] args) {
      value = numberOf(value);
      if (value instanceof BigDecimal) {
         return ((BigDecimal) value).abs();
      }
      return value;
   }

   private static Object append(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      return value.toString() + (args.length > 0 && args[0] != null ? args[0].toString() : "");
   }

   private static Object atLeast(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      value = numberOf(value);
      Object arg0 = numberOf(args[0]);
      if (value instanceof BigDecimal && arg0 instanceof BigDecimal) {
         return ((BigDecimal) value).max((BigDecimal) arg0);
      }
      return value;
   }

   private static Object atMost(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      value = numberOf(value);
      Object arg0 = numberOf(args[0]);
      if (value instanceof BigDecimal && arg0 instanceof BigDecimal) {
         return ((BigDecimal) value).min((BigDecimal) arg0);
      }
      return value;
   }

   private static Object capitalize(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      String valueStr = value.toString();
      if (valueStr.isEmpty()) {
         return valueStr;
      }
      return Character.toUpperCase(valueStr.charAt(0)) + valueStr.substring(1);
   }

   private static Object ceil(Object value, Object[] args) {
      value = numberOf(value);
      if (value instanceof BigDecimal) {
         return ((BigDecimal) value).setScale(0, RoundingMode.CEILING);
      }
      return value;
   }

   private static Object compact(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      if (value instanceof Map) {
         return ((Map<?, ?>) value).entrySet().stream().filter(e -> e.getValue() != null).collect(
                  Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
      } else {
         Object[] values = LiquidParser.arrayOf(value);
         if (values != null) {
            return Arrays.stream(values).filter(Objects::nonNull).toArray();
         }
      }
      return value;
   }

   private static Object concat(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }

      if (value instanceof Map && args[0] instanceof Map) {
         Map<Object, Object> result = new HashMap<>();
         result.putAll((Map<?, ?>) value);
         result.putAll((Map<?, ?>) args[0]);
         return Collections.unmodifiableMap(result);
      }

      List<Object> result = new ArrayList<>();
      if (value instanceof Collection) {
         result.addAll((Collection<?>) value);
      } else if (value.getClass().isArray()) {
         result.addAll(Arrays.asList((Object[]) value));
      } else {
         return value;
      }

      if (args[0] instanceof Collection) {
         result.addAll((Collection<?>) args[0]);
      } else if (value.getClass().isArray()) {
         result.addAll(Arrays.asList((Object[]) args[0]));
      } else {
         throw new TypeMismatchException(value.getClass(), args[0].getClass());
      }

      return result.toArray();
   }

   private static Object coalesce(Object value, Object[] args) {
      if (args != null && args.length > 0) {
         if (value == null || value instanceof String && ((String) value).isEmpty()) {
            return args[0];
         }
      }
      return value;
   }

   private static Object date(Object value, Object[] args) {
      // This method is actually never used for the 'date' filter
      // The real method is in the RenderizationContextImpl class
      return null;
   }

   private static Object dividedBy(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      value = numberOf(value);
      Object arg0 = numberOf(args[0]);
      if (value instanceof BigDecimal && arg0 instanceof BigDecimal) {
         BigDecimal d1 = (BigDecimal) value;
         BigDecimal d2 = (BigDecimal) arg0;
         if (d2.scale() == 0) {
            return d1.divide(d2, 0, RoundingMode.FLOOR);
         } else {
            BigDecimal result = d1.divide(d2, 15, RoundingMode.HALF_UP).stripTrailingZeros();
            if (result.scale() == 0) {
               result = result.setScale(1);
            }
            return result;
         }
      }
      return value;
   }

   private static Object downcase(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      return value.toString().toLowerCase();
   }

   private static Object escape(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      return value.toString()
               .replace("&", "&amp;")
               .replace("<", "&lt;")
               .replace(">", "&gt;")
               .replace("\"", "&quot;")
               .replace("'", "&#39;");
   }

   private static Object escapeOnce(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      return value.toString()
               .replaceAll("&(?!(#[0-9]+|#x[0-9a-fA-F]+|[a-zA-Z]+);)", "&amp;")
               .replace("<", "&lt;")
               .replace(">", "&gt;")
               .replace("\"", "&quot;")
               .replace("'", "&#39;");
   }

   private static Object first(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      Object[] values = arrayOf(value);
      if (values != null && values.length > 0) {
         value = values[0];
         return value.getClass().isArray() ? ((Object[]) value)[1] : value;
      }
      return value;
   }

   private static Object floor(Object value, Object[] args) {
      value = numberOf(value);
      if (value instanceof BigDecimal) {
         return ((BigDecimal) value).setScale(0, RoundingMode.FLOOR);
      }
      return value;
   }

   private static Object join(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      Object[] values = arrayOf(value);
      if (values != null) {
         String glue = args[0].toString();
         return Arrays.stream(values).map(Object::toString).collect(Collectors.joining(glue));
      }
      return value;
   }

   private static Object last(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      Object[] values = arrayOf(value);
      if (values != null && values.length > 0) {
         value = values[values.length - 1];
         return value.getClass().isArray() ? ((Object[]) value)[1] : value;
      }
      return value;
   }

   private static Object lstrip(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      return value.toString().replaceAll("^\\s+", "");
   }

   private static Object map(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      Object[] values = arrayOf(value);
      if (values != null) {
         String propName = args[0].toString();
         return Arrays.stream(values).map(o -> getProperty(o, propName)).toArray();
      }
      return value;
   }

   private static Object minus(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      value = numberOf(value);
      Object arg0 = numberOf(args[0]);
      if (value instanceof BigDecimal && arg0 instanceof BigDecimal) {
         return ((BigDecimal) value).subtract((BigDecimal) arg0);
      }
      return value;
   }

   private static Object modulo(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      value = numberOf(value);
      Object arg0 = numberOf(args[0]);
      if (value instanceof BigDecimal && arg0 instanceof BigDecimal) {
         return ((BigDecimal) value).remainder((BigDecimal) arg0);
      }
      return value;
   }

   private static Object newlineToBr(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      return value.toString().replaceAll("\\r?\\n", "<br/>");
   }

   private static Object plus(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      value = numberOf(value);
      Object arg0 = numberOf(args[0]);
      if (value instanceof BigDecimal && arg0 instanceof BigDecimal) {
         return ((BigDecimal) value).add((BigDecimal) arg0);
      }
      return value;
   }

   private static Object prepend(Object value, Object[] args) {
      if (args == null || args.length == 0) {
         return value;
      }
      return (args.length > 0 && args[0] != null ? args[0].toString() : "") + value.toString();
   }

   private static Object remove(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      String needle = args[0].toString();
      return value.toString().replace(needle, "");
   }

   private static Object removeFirst(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      String needle = args[0].toString();
      return value.toString().replaceFirst(Pattern.quote(needle), "");
   }

   private static Object replace(Object value, Object[] args) {
      if (value == null || args == null || args.length < 2) {
         return value;
      }
      String needle = args[0].toString();
      String replacement = args[1].toString();
      return value.toString().replace(needle, replacement);
   }

   private static Object replaceFirst(Object value, Object[] args) {
      if (value == null || args == null || args.length < 2) {
         return value;
      }
      String needle = args[0].toString();
      String replacement = args[1].toString();
      return value.toString().replaceFirst(Pattern.quote(needle),
               Matcher.quoteReplacement(replacement));
   }

   private static Object reverse(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      Object[] values = arrayOf(value);
      if (values != null) {
         List<Object> list = new ArrayList<>(Arrays.asList(values));
         Collections.reverse(list);
         return list.toArray();
      }
      return value;
   }

   private static Object round(Object value, Object[] args) {
      value = numberOf(value);
      if (value instanceof BigDecimal) {
         int scale = 0;
         if (args != null && args.length > 0) {
            if (args[0] instanceof Number) {
               scale = ((Number) args[0]).intValue();
            } else {
               try {
                  scale = Integer.parseInt(args[0].toString());
               } catch (Exception e) {
                  scale = 0;
               }
            }
         }
         return ((BigDecimal) value).setScale(scale, RoundingMode.HALF_DOWN);
      }
      return value;
   }

   private static Object rstrip(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      return value.toString().replaceAll("\\s+$", "");
   }

   private static Object size(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      Object[] values = arrayOf(value);
      if (values != null) {
         return values.length;
      }
      return value.toString().length();
   }

   private static Object slice(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      BigDecimal indexD = args[0] instanceof BigDecimal ? (BigDecimal) args[0]
               : new BigDecimal(args[0].toString());
      BigDecimal countD = BigDecimal.ONE;
      if (args.length > 1) {
         countD = args[1] instanceof BigDecimal ? (BigDecimal) args[1]
                  : new BigDecimal(args[1].toString());
      }
      int start = indexD.intValueExact();
      int end = countD.intValueExact();
      Object[] values = arrayOf(value);
      if (values != null) {
         if (start < 0) {
            start = values.length + start;
         }
         end = Math.min(start + end, values.length);
         return Arrays.copyOfRange(values, start, end);
      }
      String str = value.toString();
      if (start < 0) {
         start = str.length() + start;
      }
      end = Math.min(start + end, str.length());
      return str.substring(start, end);
   }

   private static Object sort(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      Object[] values = arrayOf(value);
      if (values != null) {
         Arrays.sort(values);
         return values;
      }
      return value;
   }

   private static Object sortNatural(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      Object[] values = arrayOf(value);
      if (values != null) {
         Arrays.sort(values, (c1, c2) -> c1.toString().compareToIgnoreCase(c2.toString()));
         return values;
      }
      return value;
   }

   private static Object split(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      return value.toString().split("(?<!^)" + Pattern.quote(args[0].toString()));
   }

   private static Object strip(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      return value.toString().trim();
   }

   private static Object stripHtml(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      return value.toString().replaceAll("<[^>]+>", "");
   }

   private static Object stripNewlines(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      return value.toString().replaceAll("\\r?\\n", "");
   }

   private static Object times(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      value = numberOf(value);
      Object arg0 = numberOf(args[0]);
      if (value instanceof BigDecimal && arg0 instanceof BigDecimal) {
         return ((BigDecimal) value).multiply((BigDecimal) arg0);
      }
      return value;
   }

   private static Object truncate(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      BigDecimal lengthD = args[0] instanceof BigDecimal ? (BigDecimal) args[0]
               : new BigDecimal(args[0].toString());
      String ellipsis = "...";
      if (args.length > 1 && args[1] != null) {
         ellipsis = args[1].toString();
      }
      int length = lengthD.intValueExact() - ellipsis.length();
      String str = value.toString();
      String result = str.substring(0, Math.min(length, str.length()));
      if (result.length() < str.length()) {
         result += ellipsis;
      }
      return result;
   }

   private static Object truncateWords(Object value, Object[] args) {
      if (value == null || args == null || args.length == 0) {
         return value;
      }
      BigDecimal lengthD = args[0] instanceof BigDecimal ? (BigDecimal) args[0]
               : new BigDecimal(args[0].toString());
      int length = lengthD.intValueExact();
      String ellipsis = "...";
      if (args.length > 1 && args[1] != null) {
         ellipsis = args[1].toString();
      }
      String str = value.toString();
      final Matcher NON_SPACES = Pattern.compile("\\S+").matcher(str);
      int pos = 0;
      for (int i = 0; i < length; ++i) {
         if (!NON_SPACES.find(pos)) {
            pos = str.length();
            break;
         }
         pos = NON_SPACES.end();
      }
      String result = str.substring(0, pos);
      if (result.length() < str.length()) {
         result += ellipsis;
      }
      return result;
   }

   private static Object uniq(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      Object[] values = arrayOf(value);
      if (values != null) {
         Set<Object> result = new LinkedHashSet<>();
         result.addAll(Arrays.asList(values));
         return result.toArray();
      }
      return value;
   }

   private static Object upcase(Object value, Object[] args) {
      if (value == null) {
         return null;
      }
      return value.toString().toUpperCase();
   }

   private static Object urlDecode(Object value, Object[] args) {
      try {
         return URLDecoder.decode(value.toString(), StandardCharsets.UTF_8.name());
      } catch (Exception e) {
         return value;
      }
   }

   private static Object urlEncode(Object value, Object[] args) {
      try {
         return URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.name());
      } catch (Exception e) {
         return value;
      }
   }
}
