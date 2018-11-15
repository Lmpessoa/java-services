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
package com.lmpessoa.services.util.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.text.ParseException;
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

import com.lmpessoa.services.util.ConnectionInfo;
import com.lmpessoa.services.util.parsing.ITemplatePart;
import com.lmpessoa.services.util.parsing.LiteralPart;

final class LogFormatter {

   private static final Map<String, Function<LogEntry, String>> variables = new HashMap<>();
   private static String hostname = null;

   private final List<ITemplatePart> parts;

   static LogFormatter parse(String template) throws ParseException {
      return new LogFormatter(LogFormatParser.parse(template, variables));
   }

   static {
      variables.put("Time", LogFormatter::getTime);
      variables.put("Time.Web", LogFormatter::getTimeWeb);
      variables.put("Time.AmPm", LogFormatter::getTimeAmPm);
      variables.put("Time.Day", LogFormatter::getTimeDay);
      variables.put("Time.Hour", LogFormatter::getTimeHour);
      variables.put("Time.12Hour", LogFormatter::getTime12Hour);
      variables.put("Time.Millis", LogFormatter::getTimeMillis);
      variables.put("Time.Minutes", LogFormatter::getTimeMinutes);
      variables.put("Time.Month", LogFormatter::getTimeMonth);
      variables.put("Time.Month.Name", LogFormatter::getTimeMonthName);
      variables.put("Time.Month.Short", LogFormatter::getTimeMonthShort);
      variables.put("Time.Offset", LogFormatter::getTimeOffset);
      variables.put("Time.Seconds", LogFormatter::getTimeSeconds);
      variables.put("Time.WeekDay", LogFormatter::getTimeWeekDay);
      variables.put("Time.WeekDay.Short", LogFormatter::getTimeWeekDayShort);
      variables.put("Time.Year", LogFormatter::getTimeYear);
      variables.put("Time.Zone", LogFormatter::getTimeZone);
      variables.put("Severity", LogFormatter::getSeverity);
      variables.put("Message", LogFormatter::getMessage);
      variables.put("Thread", LogFormatter::getThread);
      variables.put("Thread.Id", LogFormatter::getThreadId);
      variables.put("Thread.Name", LogFormatter::getThreadName);
      variables.put("Remote.Addr", LogFormatter::getRemoteAddress);
      variables.put("Remote.Host", LogFormatter::getRemoteHost);
      variables.put("Local.Host", LogFormatter::getLocalHost);
      variables.put("Class.SimpleName", LogFormatter::getSimpleClassName);
   }

   String format(LogEntry entry) {
      return format(entry, null);
   }

   String format(LogEntry entry, String message) {
      StringBuilder result = new StringBuilder();
      for (ITemplatePart part : parts) {
         if (part instanceof LogVariable) {
            LogVariable var = (LogVariable) part;
            if (var.isMessage() && message != null) {
               result.append(message);
            } else {
               result.append(var.getValueOf(entry));
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
      String result = formatterOf(ChronoField.MILLI_OF_SECOND, TextStyle.FULL).format(entry.getTime());
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

   private static String getMessage(LogEntry entry) {
      return entry.getMessage();
   }

   private static String getThread(LogEntry entry) {
      return getThreadName(entry) + "/" + getThreadId(entry);
   }

   private static String getThreadId(LogEntry entry) {
      return String.valueOf(entry.getThreadId());
   }

   private static String getThreadName(LogEntry entry) {
      return entry.getThreadName();
   }

   private static String getRemoteAddress(LogEntry entry) {
      ConnectionInfo conn = entry.getConnection();
      if (conn == null) {
         return InetAddress.getLoopbackAddress().getHostAddress();
      }
      return conn.getRemoteAddress().getHostAddress();
   }

   private static String getRemoteHost(LogEntry entry) {
      ConnectionInfo conn = entry.getConnection();
      if (conn == null) {
         return "localhost";
      }
      return conn.getRemoteAddress().getHostName();
   }

   private static String getLocalHost(LogEntry entry) { // NOSONAR
      if (hostname == null) {
         if (System.getProperty("os.name").startsWith("Windows")) {
            hostname = System.getenv("COMPUTERNAME");
         } else {
            hostname = System.getenv("HOSTNAME");
            if (hostname == null) {
               try {
                  Process proc = Runtime.getRuntime().exec("hostname");
                  BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                  StringBuilder result = new StringBuilder();
                  String s;
                  while ((s = stdInput.readLine()) != null) {
                     result.append(s);
                  }
                  hostname = result.toString().trim();
               } catch (IOException e) {
                  // Ignore, should never get here
                  hostname = "localhost";
               }
            }
         }
      }
      return hostname;
   }

   private static String getSimpleClassName(LogEntry entry) {
      String[] className = entry.getClassName().split("\\.");
      return className[className.length - 1];
   }

   private LogFormatter(List<ITemplatePart> parts) {
      this.parts = parts;
   }
}