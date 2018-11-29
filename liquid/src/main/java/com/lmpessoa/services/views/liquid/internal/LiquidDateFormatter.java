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

import java.text.DateFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LiquidDateFormatter {

   private static final Pattern VARIABLE = Pattern.compile("%([\\^_-])*(\\d*)([a-yA-Z%+]|:*z)");
   private static final Map<String, TimeVariableInfo> VALUES = new HashMap<>();

   private final Locale locale;

   static {
      VALUES.put("Y", new TimeVariableInfo(LiquidDateFormatter::yearWithCentury, 4, true));
      VALUES.put("C", new TimeVariableInfo(LiquidDateFormatter::centuryOfYear, 2));
      VALUES.put("y", new TimeVariableInfo(LiquidDateFormatter::yearWithoutCentury, 2));

      VALUES.put("m", new TimeVariableInfo(LiquidDateFormatter::month, 2));
      VALUES.put("B", new TimeVariableInfo(LiquidDateFormatter::fullMonthName, 0));
      VALUES.put("b", new TimeVariableInfo(LiquidDateFormatter::shortMonthName, 0));
      VALUES.put("h", new TimeVariableInfo(LiquidDateFormatter::shortMonthName, 0));

      VALUES.put("d", new TimeVariableInfo(LiquidDateFormatter::dayOfTheMonthWithZeroes, 2));
      VALUES.put("e", new TimeVariableInfo(LiquidDateFormatter::dayOfTheMonthWithBlanks, 2));
      VALUES.put("j", new TimeVariableInfo(LiquidDateFormatter::dayOfTheYear, 3));

      VALUES.put("H", new TimeVariableInfo(LiquidDateFormatter::hourWith24Hours, 2));
      VALUES.put("k", new TimeVariableInfo(LiquidDateFormatter::hourWith24Hours, 2));
      VALUES.put("I", new TimeVariableInfo(LiquidDateFormatter::hourWith12Hours, 2));
      VALUES.put("l", new TimeVariableInfo(LiquidDateFormatter::hourWith12Hours, 2));
      VALUES.put("P", new TimeVariableInfo((t, l) -> meridianIndicator(t, l).toLowerCase(), 0));
      VALUES.put("p", new TimeVariableInfo(LiquidDateFormatter::meridianIndicator, 0));

      VALUES.put("M", new TimeVariableInfo(LiquidDateFormatter::minutes, 2));

      VALUES.put("S", new TimeVariableInfo(LiquidDateFormatter::seconds, 2));

      VALUES.put("L", new TimeVariableInfo(LiquidDateFormatter::milliseconds, 3));

      VALUES.put("z", new TimeVariableInfo(LiquidDateFormatter::timeZoneOffset, 4));
      VALUES.put(":z", new TimeVariableInfo(LiquidDateFormatter::timeZoneOffsetWithColon, 5));
      VALUES.put("::z", new TimeVariableInfo(LiquidDateFormatter::timeZoneOffsetWithSeconds, 8));
      VALUES.put(":::z", new TimeVariableInfo(LiquidDateFormatter::timeZoneOffsetDynamic, 0));
      VALUES.put("Z", new TimeVariableInfo(LiquidDateFormatter::timeZoneName, 0));

      VALUES.put("A", new TimeVariableInfo(LiquidDateFormatter::fullWeekdayName, 0));
      VALUES.put("a", new TimeVariableInfo(LiquidDateFormatter::shortWeekdayName, 0));
      VALUES.put("u", new TimeVariableInfo(LiquidDateFormatter::dayOfTheWeekMonday, 1));
      VALUES.put("w", new TimeVariableInfo(LiquidDateFormatter::dayOfTheWeekSunday, 1));

      VALUES.put("U", new TimeVariableInfo(LiquidDateFormatter::weekNumberSunday, 2));
      VALUES.put("W", new TimeVariableInfo(LiquidDateFormatter::weekNumberMonday, 2));

      VALUES.put("s", new TimeVariableInfo(LiquidDateFormatter::secondsSince1970, 1));

      VALUES.put("n", new TimeVariableInfo((t, l) -> "\n", 1, false));
      VALUES.put("t", new TimeVariableInfo((t, l) -> "\t", 1, false));
      VALUES.put("%", new TimeVariableInfo((t, l) -> "%", 1, false));

      VALUES.put("c", new TimeVariableInfo(LiquidDateFormatter::dateAndTime, 0));
      VALUES.put("D", new TimeVariableInfo(LiquidDateFormatter::date, 0));
      VALUES.put("F", new TimeVariableInfo(LiquidDateFormatter::iso8601Date, 0));
      VALUES.put("v", new TimeVariableInfo(LiquidDateFormatter::vmsDate, 0));
      VALUES.put("x", new TimeVariableInfo(LiquidDateFormatter::date, 0));
      VALUES.put("X", new TimeVariableInfo(LiquidDateFormatter::time24HoursWithSeconds, 0));
      VALUES.put("r", new TimeVariableInfo(LiquidDateFormatter::time12Hours, 0));
      VALUES.put("R", new TimeVariableInfo(LiquidDateFormatter::time24Hours, 0));
      VALUES.put("T", new TimeVariableInfo(LiquidDateFormatter::time24HoursWithSeconds, 0));
      VALUES.put("+", new TimeVariableInfo(LiquidDateFormatter::date1, 0));
   }

   public String format(Date date, String template) {
      return format(date.toInstant(), template);
   }

   public String format(TemporalAccessor date, String template) {
      StringBuilder result = new StringBuilder();
      Matcher m = VARIABLE.matcher(template);
      FormatSymbols symbols = new FormatSymbols(locale);
      int pos = 0;
      while (m.find(pos)) {
         if (pos < m.start()) {
            result.append(template.substring(pos, m.start()));
            pos = m.start();
         }
         TimeVariableInfo info = VALUES.get(m.group(3));
         if (info != null) {
            Modifiers mod = new Modifiers(m.group(1), m.group(2), m.group(3));
            String variable = info.func.apply(date, symbols);
            variable = align(variable, mod.length != null ? mod.length : info.length, info.signal,
                     mod.pad, mod.upcase);
            result.append(variable);
         } else {
            result.append(m.group());
         }
         pos = m.end();
      }
      if (pos < template.length()) {
         result.append(template.substring(pos));
      }
      return result.toString();
   }

   public LiquidDateFormatter(Locale locale) {
      this.locale = locale;
   }

   private static String align(String value, int length, boolean signal, char pad, boolean upcase) {
      if (upcase) {
         value = value.toUpperCase();
      }
      StringBuilder result = new StringBuilder(value);
      int vlen = value.length();
      int i = 0;
      if (value.charAt(0) == '+' || value.charAt(0) == '-') {
         if (!signal) {
            vlen -= 1;
         }
         i = 1;
      }
      if (pad != '\0' && length > vlen) {
         char[] padchars = new char[length - vlen];
         Arrays.fill(padchars, pad);
         result.insert(i, padchars);
      }
      return result.toString();
   }

   private static String yearWithCentury(TemporalAccessor date, FormatSymbols symbols) {
      return String.valueOf(date.get(ChronoField.YEAR));
   }

   private static String centuryOfYear(TemporalAccessor date, FormatSymbols symbols) {
      int year = date.get(ChronoField.YEAR);
      int cent = year / 100 + (year > 0 && year % 100 != 0 ? 1 : 0);
      return String.valueOf(cent);
   }

   private static String yearWithoutCentury(TemporalAccessor date, FormatSymbols symbols) {
      return String.valueOf(date.get(ChronoField.YEAR) % 100);
   }

   private static String month(TemporalAccessor date, FormatSymbols symbols) {
      return String.valueOf(date.get(ChronoField.MONTH_OF_YEAR));
   }

   private static String fullMonthName(TemporalAccessor date, FormatSymbols symbols) {
      return symbols.getMonths()[date.get(ChronoField.MONTH_OF_YEAR) - 1];
   }

   private static String shortMonthName(TemporalAccessor date, FormatSymbols symbols) {
      return symbols.getShortMonths()[date.get(ChronoField.MONTH_OF_YEAR) - 1];
   }

   private static String dayOfTheMonthWithZeroes(TemporalAccessor date, FormatSymbols symbols) {
      return String.valueOf(date.get(ChronoField.DAY_OF_MONTH));
   }

   private static String dayOfTheMonthWithBlanks(TemporalAccessor date, FormatSymbols symbols) {
      return String.valueOf(date.get(ChronoField.DAY_OF_MONTH));
   }

   private static String dayOfTheYear(TemporalAccessor date, FormatSymbols symbols) {
      return String.valueOf(date.get(ChronoField.DAY_OF_YEAR));
   }

   private static String hourWith24Hours(TemporalAccessor date, FormatSymbols symbols) {
      return String.valueOf(date.get(ChronoField.HOUR_OF_DAY));
   }

   private static String hourWith12Hours(TemporalAccessor date, FormatSymbols symbols) {
      int hour = date.get(ChronoField.HOUR_OF_AMPM);
      return String.valueOf(hour == 0 ? 12 : hour);
   }

   private static String meridianIndicator(TemporalAccessor date, FormatSymbols symbols) {
      return symbols.getAmPmStrings()[date.get(ChronoField.AMPM_OF_DAY)];
   }

   private static String minutes(TemporalAccessor date, FormatSymbols symbols) {
      return String.valueOf(date.get(ChronoField.MINUTE_OF_HOUR));
   }

   private static String seconds(TemporalAccessor date, FormatSymbols symbols) {
      return String.valueOf(date.get(ChronoField.SECOND_OF_MINUTE));
   }

   private static String milliseconds(TemporalAccessor date, FormatSymbols symbols) {
      return String.valueOf(date.get(ChronoField.MILLI_OF_SECOND));
   }

   private static int[] offsetOf(TemporalAccessor date) {
      int offset = date.get(ChronoField.OFFSET_SECONDS);
      int signal = offset < 0 ? -1 : 1;
      int osec = offset % 60;
      offset = (Math.abs(offset) - osec) / 60;
      int omin = offset % 60;
      int ohour = (offset - omin) / 60;
      return new int[] { signal, ohour, omin, osec };
   }

   private static String timeZoneOffset(TemporalAccessor date, FormatSymbols symbols) {
      int[] offset = offsetOf(date);
      return (offset[0] > 0 ? "+" : "-") + String.valueOf(offset[1]) + (offset[2] < 10 ? "0" : "")
               + String.valueOf(offset[2]);
   }

   private static String timeZoneOffsetWithColon(TemporalAccessor date, FormatSymbols symbols) {
      int[] offset = offsetOf(date);
      return (offset[0] > 0 ? "+" : "-") + String.valueOf(offset[1]) + ":"
               + (offset[2] < 10 ? "0" : "") + String.valueOf(offset[2]);
   }

   private static String timeZoneOffsetWithSeconds(TemporalAccessor date, FormatSymbols symbols) {
      int[] offset = offsetOf(date);
      return (offset[0] > 0 ? "+" : "-") + String.valueOf(offset[1]) + ":"
               + (offset[2] < 10 ? "0" : "") + String.valueOf(offset[2]) + ":"
               + (offset[3] < 10 ? "0" : "") + String.valueOf(offset[3]);
   }

   private static String timeZoneOffsetDynamic(TemporalAccessor date, FormatSymbols symbols) {
      int[] offset = offsetOf(date);
      StringBuilder result = new StringBuilder();
      result.append(offset[0] > 0 ? '+' : '-');
      result.append(offset[1] < 10 ? "0" : "");
      result.append(offset[1]);
      if (offset[2] != 0 || offset[3] != 0) {
         result.append(':');
         result.append(offset[2] < 10 ? "0" : "");
         result.append(offset[2]);
         if (offset[3] != 0) {
            result.append(':');
            result.append(offset[3] < 10 ? "0" : "");
            result.append(offset[3]);
         }
      }
      return result.toString();
   }

   private static String timeZoneName(TemporalAccessor date, FormatSymbols symbols) {
      if (!(date instanceof ZonedDateTime)) {
         return timeZoneOffsetWithColon(date, symbols);
      }
      ZoneId zone = ((ZonedDateTime) date).getZone();
      return zone.getDisplayName(TextStyle.SHORT, symbols.getLocale());
   }

   private static String fullWeekdayName(TemporalAccessor date, FormatSymbols symbols) {
      return symbols.getWeekdays()[fixSunday(date.get(ChronoField.DAY_OF_WEEK)) + 1];
   }

   private static String shortWeekdayName(TemporalAccessor date, FormatSymbols symbols) {
      return symbols.getShortWeekdays()[fixSunday(date.get(ChronoField.DAY_OF_WEEK)) + 1];
   }

   private static String dayOfTheWeekMonday(TemporalAccessor date, FormatSymbols symbols) {
      return String.valueOf(date.get(ChronoField.DAY_OF_WEEK));
   }

   private static String dayOfTheWeekSunday(TemporalAccessor date, FormatSymbols symbols) {
      int wday = date.get(ChronoField.DAY_OF_WEEK);
      return String.valueOf(fixSunday(wday));
   }

   private static String weekNumberSunday(TemporalAccessor date, FormatSymbols symbols) {
      return String.valueOf(weekNumberOf(date, 0));
   }

   private static String weekNumberMonday(TemporalAccessor date, FormatSymbols symbols) {
      return String.valueOf(weekNumberOf(date, 1));
   }

   private static int weekNumberOf(TemporalAccessor date, int s0) {
      int d1 = date.get(ChronoField.DAY_OF_YEAR);
      int w0 = fixSunday(
               LocalDate.of(date.get(ChronoField.YEAR), 1, 1).get(ChronoField.DAY_OF_WEEK));
      int d0 = (w0 <= s0 ? 1 : 8) + s0 - w0;
      return (d1 - d1 % 7) / 7 + (d1 % 7 < d0 ? 0 : 1);
   }

   private static int fixSunday(int wday) {
      return wday == 7 ? 0 : wday;
   }

   private static String secondsSince1970(TemporalAccessor date, FormatSymbols symbols) {
      long millis = Instant.from(date).toEpochMilli();
      long secs = millis - millis % 1000 / 1000;
      return String.valueOf(secs);
   }

   private static String dateAndTime(TemporalAccessor date, FormatSymbols symbols) {
      return new LiquidDateFormatter(symbols.locale).format(date, "%a %b %e %H:%M:%S %Y");
   }

   private static String date(TemporalAccessor date, FormatSymbols symbols) {
      return new LiquidDateFormatter(symbols.locale).format(date, "%m/%d/%y");
   }

   private static String iso8601Date(TemporalAccessor date, FormatSymbols symbols) {
      return new LiquidDateFormatter(symbols.locale).format(date, "%Y-%m-%d");
   }

   private static String vmsDate(TemporalAccessor date, FormatSymbols symbols) {
      return new LiquidDateFormatter(symbols.locale).format(date, "%e-%^b-%Y");
   }

   private static String time12Hours(TemporalAccessor date, FormatSymbols symbols) {
      return new LiquidDateFormatter(symbols.locale).format(date, "%I:%M:%S %p");
   }

   private static String time24Hours(TemporalAccessor date, FormatSymbols symbols) {
      return new LiquidDateFormatter(symbols.locale).format(date, "%H:%M");
   }

   private static String time24HoursWithSeconds(TemporalAccessor date, FormatSymbols symbols) {
      return new LiquidDateFormatter(symbols.locale).format(date, "%H:%M:%S");
   }

   private static String date1(TemporalAccessor date, FormatSymbols symbols) {
      return new LiquidDateFormatter(symbols.locale).format(date, "%a %b %e %H:%M:%S %Z %Y");
   }

   private static interface TimeFunction {

      String apply(TemporalAccessor time, FormatSymbols symbols);
   }

   private static class TimeVariableInfo {

      public final TimeFunction func;
      public final boolean signal;
      public final int length;

      TimeVariableInfo(TimeFunction func, int length) {
         this(func, length, false);
      }

      TimeVariableInfo(TimeFunction func, int length, boolean signal) {
         this.length = length;
         this.signal = signal;
         this.func = func;
      }
   }

   private static class FormatSymbols extends DateFormatSymbols {

      private static final long serialVersionUID = 1L;

      private final Locale locale;

      FormatSymbols(Locale locale) {
         super(locale);
         this.locale = locale;
      }

      Locale getLocale() {
         return locale;
      }
   }

   private static class Modifiers {

      public final boolean upcase;
      public final Integer length;
      public final char pad;

      Modifiers(String padding, String length, String field) {
         this.length = length != null && !length.isEmpty() ? Integer.valueOf(length) : null;
         this.upcase = padding != null && padding.indexOf('^') >= 0;
         char ch = field.charAt(field.length() - 1);
         char pad = "YCymdjHIMSLzuwUWs".indexOf(ch) >= 0 ? '0' : ' ';
         if (padding != null) {
            if (padding.indexOf('_') >= 0) {
               pad = ' ';
            }
            if (ch != 'z' && padding.indexOf('-') >= 0) {
               pad = '\0';
            }
         }
         this.pad = pad;
      }
   }
}
