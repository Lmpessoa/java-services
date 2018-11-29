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
package com.lmpessoa.services.internal.logging;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import com.lmpessoa.services.internal.CoreMessage;
import com.lmpessoa.services.internal.parsing.ITemplatePart;
import com.lmpessoa.services.internal.parsing.LiteralPart;
import com.lmpessoa.services.logging.LogEntry;

public final class LogFormatter {

   private static final Map<String, Function<LogEntry, String>> sharedVariables = new HashMap<>();

   private final List<ITemplatePart> parts;

   static {
      sharedVariables.put("Time", LogFormatter::getTime);
      sharedVariables.put("Time.Web", LogFormatter::getTimeWeb);
      sharedVariables.put("Time.AmPm", LogFormatter::getTimeAmPm);
      sharedVariables.put("Time.Day", LogFormatter::getTimeDay);
      sharedVariables.put("Time.Hour", LogFormatter::getTimeHour);
      sharedVariables.put("Time.12Hour", LogFormatter::getTime12Hour);
      sharedVariables.put("Time.Millis", LogFormatter::getTimeMillis);
      sharedVariables.put("Time.Minutes", LogFormatter::getTimeMinutes);
      sharedVariables.put("Time.Month", LogFormatter::getTimeMonth);
      sharedVariables.put("Time.Month.Name", LogFormatter::getTimeMonthName);
      sharedVariables.put("Time.Month.Short", LogFormatter::getTimeMonthShort);
      sharedVariables.put("Time.Offset", LogFormatter::getTimeOffset);
      sharedVariables.put("Time.Seconds", LogFormatter::getTimeSeconds);
      sharedVariables.put("Time.WeekDay", LogFormatter::getTimeWeekDay);
      sharedVariables.put("Time.WeekDay.Short", LogFormatter::getTimeWeekDayShort);
      sharedVariables.put("Time.Year", LogFormatter::getTimeYear);
      sharedVariables.put("Time.Zone", LogFormatter::getTimeZone);
      sharedVariables.put("Severity", LogFormatter::getSeverity);
      sharedVariables.put("Message", LogEntry::getMessage);
      sharedVariables.put("Thread", LogFormatter::getThread);
      sharedVariables.put("Thread.Id", LogFormatter::getThreadId);
      sharedVariables.put("Thread.Name", LogEntry::getThreadName);
      sharedVariables.put("Class.Name", LogEntry::getClassName);
      sharedVariables.put("Class.SimpleName", LogFormatter::getSimpleClassName);
   }

   public static LogFormatter parse(String template) {
      return new LogFormatter(LogFormatParser.parse(template));
   }

   public String format(LogEntry entry, String message) {
      StringBuilder result = new StringBuilder();
      for (ITemplatePart part : parts) {
         if (part instanceof LogVariable) {
            LogVariable var = (LogVariable) part;
            if (var.isMessage()) {
               result.append(message);
            } else {
               Function<LogEntry, String> func;
               if (sharedVariables.containsKey(var.getName())) {
                  func = sharedVariables.get(var.getName());
               } else {
                  func = ((LogEntryImpl) entry).getLogger().getVariable(var.getName());
                  if (func == null) {
                     throw new IllegalArgumentException(
                              CoreMessage.UNKNOWN_VARIABLE.with(var.getName()));
                  }
               }
               result.append(var.format(func.apply(entry)));
            }
         } else {
            result.append(((LiteralPart) part).getValue());
         }
      }
      return result.toString();
   }

   private static DateTimeFormatter formatterOf(TemporalField field, TextStyle style) {
      return new DateTimeFormatterBuilder().appendText(field, style).toFormatter();
   }

   private static String getTime(LogEntry entry) {
      DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
      String result = formatter.format(entry.getTime().toLocalDateTime());
      if (result.length() == 19) {
         result += ".000";
      }
      while (result.length() < 23) {
         result = result.substring(0, 20) + '0' + result.substring(20);
      }
      return result;
   }

   private static String getTimeWeb(LogEntry entry) {
      DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
      ZonedDateTime utc = ZonedDateTime.ofInstant(entry.getTime().toInstant(), ZoneOffset.UTC);
      return formatter.format(utc);
   }

   private static String getTimeAmPm(LogEntry entry) {
      return formatterOf(ChronoField.AMPM_OF_DAY, TextStyle.SHORT).format(entry.getTime());
   }

   private static String getTimeDay(LogEntry entry) {
      String result = String.valueOf(entry.getTime().getDayOfMonth());
      if (result.length() == 1) {
         result = "0" + result;
      }
      return result;
   }

   private static String getTimeHour(LogEntry entry) {
      String result = String.valueOf(entry.getTime().getHour());
      if (result.length() == 1) {
         result = "0" + result;
      }
      return result;
   }

   private static String getTime12Hour(LogEntry entry) {
      String result = formatterOf(ChronoField.HOUR_OF_AMPM, TextStyle.FULL).format(entry.getTime());
      if (result.length() == 1) {
         result = "0" + result;
      }
      return result;
   }

   private static String getTimeMillis(LogEntry entry) {
      String result = formatterOf(ChronoField.MILLI_OF_SECOND, TextStyle.FULL)
               .format(entry.getTime());
      if (result.length() == 2) {
         result = "0" + result;
      }
      return result;
   }

   private static String getTimeMinutes(LogEntry entry) {
      String result = String.valueOf(entry.getTime().getMinute());
      if (result.length() == 1) {
         result = "0" + result;
      }
      return result;
   }

   private static String getTimeMonth(LogEntry entry) {
      String result = String.valueOf(entry.getTime().getMonthValue());
      if (result.length() == 1) {
         result = "0" + result;
      }
      return result;
   }

   private static String getTimeMonthName(LogEntry entry) {
      return formatterOf(ChronoField.MONTH_OF_YEAR, TextStyle.FULL).format(entry.getTime());
   }

   private static String getTimeMonthShort(LogEntry entry) {
      return formatterOf(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT).format(entry.getTime());
   }

   private static String getTimeOffset(LogEntry entry) {
      return entry.getTime().getOffset().getDisplayName(TextStyle.SHORT, Locale.getDefault());
   }

   private static String getTimeSeconds(LogEntry entry) {
      String result = String.valueOf(entry.getTime().getSecond());
      if (result.length() == 1) {
         result = "0" + result;
      }
      return result;
   }

   private static String getTimeWeekDay(LogEntry entry) {
      return formatterOf(ChronoField.DAY_OF_WEEK, TextStyle.FULL).format(entry.getTime());
   }

   private static String getTimeWeekDayShort(LogEntry entry) {
      return formatterOf(ChronoField.DAY_OF_WEEK, TextStyle.SHORT).format(entry.getTime());
   }

   private static String getTimeYear(LogEntry entry) {
      return String.valueOf(entry.getTime().getYear());
   }

   private static String getTimeZone(LogEntry entry) {
      return entry.getTime().getZone().getDisplayName(TextStyle.SHORT, Locale.getDefault());
   }

   private static String getSeverity(LogEntry entry) {
      return entry.getSeverity().toString();
   }

   private static String getThread(LogEntry entry) {
      return entry.getThreadName() + "/" + getThreadId(entry);
   }

   private static String getThreadId(LogEntry entry) {
      return String.valueOf(entry.getThreadId());
   }

   private static String getSimpleClassName(LogEntry entry) {
      String[] className = entry.getClassName().split("\\.");
      return className[className.length - 1];
   }

   private LogFormatter(List<ITemplatePart> parts) {
      this.parts = parts;
   }
}
