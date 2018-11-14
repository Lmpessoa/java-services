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
package com.lmpessoa.services.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lmpessoa.util.ConnectionInfo;
import com.lmpessoa.util.parsing.AbstractParser;
import com.lmpessoa.util.parsing.ITemplatePart;

final class LogFormatParser extends AbstractParser<LogVariable> {

   private static final Pattern VARIABLE_DEF = Pattern
            .compile("^([A-Z][a-zA-Z0-9]*(?:\\.[A-Z0-9][a-zA-Z0-9]*)*)(?:\\:([<>])(\\d+))?$");
   private final Map<String, Function<LogEntry, String>> variables;
   private static String hostname;

   @Override
   protected LogVariable parseVariable(int pos, String variablePart) throws ParseException {
      Matcher matcher = VARIABLE_DEF.matcher(variablePart);
      if (!matcher.find()) {
         throw new ParseException("Not a valid variable reference: " + variablePart, pos);
      }
      final String varName = matcher.group(1);
      Function<LogEntry, String> func = variables.get(varName);
      if (func == null && !"Class.Name".equals(varName)) {
         throw new ParseException("Unknown log variable: " + varName, pos);
      }
      boolean rightAlign = ">".equals(matcher.group(2));
      int length = matcher.group(3) != null ? Integer.valueOf(matcher.group(3)) : -1;
      if ("Class.Name".equals(varName)) {
         return new ClassNameLogVariable(varName, rightAlign, length, func);
      }
      return new LogVariable(varName, rightAlign, length, func);
   }

   static List<ITemplatePart> parse(String template, Map<String, Function<LogEntry, String>> variables)
      throws ParseException {
      return new LogFormatParser(template, variables).parse();
   }

   static void registerVariables(Map<String, Function<LogEntry, String>> variables) {
      variables.put("Time", LogFormatParser::getTime);
      variables.put("Time.Web", LogFormatParser::getTimeWeb);
      variables.put("Time.AmPm", LogFormatParser::getTimeAmPm);
      variables.put("Time.Day", LogFormatParser::getTimeDay);
      variables.put("Time.Hour", LogFormatParser::getTimeHour);
      variables.put("Time.12Hour", LogFormatParser::getTime12Hour);
      variables.put("Time.Millis", LogFormatParser::getTimeMillis);
      variables.put("Time.Minutes", LogFormatParser::getTimeMinutes);
      variables.put("Time.Month", LogFormatParser::getTimeMonth);
      variables.put("Time.Month.Name", LogFormatParser::getTimeMonthName);
      variables.put("Time.Month.Short", LogFormatParser::getTimeMonthShort);
      variables.put("Time.Offset", LogFormatParser::getTimeOffset);
      variables.put("Time.Seconds", LogFormatParser::getTimeSeconds);
      variables.put("Time.WeekDay", LogFormatParser::getTimeWeekDay);
      variables.put("Time.WeekDay.Short", LogFormatParser::getTimeWeekDayShort);
      variables.put("Time.Year", LogFormatParser::getTimeYear);
      variables.put("Time.Zone", LogFormatParser::getTimeZone);
      variables.put("Severity", LogFormatParser::getSeverity);
      variables.put("Message", LogFormatParser::getMessage);
      variables.put("Thread", LogFormatParser::getThread);
      variables.put("Thread.ID", LogFormatParser::getThreadId);
      variables.put("Thread.Name", LogFormatParser::getThreadName);
      variables.put("Remote.Addr", LogFormatParser::getRemoteAddress);
      variables.put("Remote.Host", LogFormatParser::getRemoteHost);
      variables.put("Local.Host", LogFormatParser::getLocalHost);
   }

   private LogFormatParser(String template, Map<String, Function<LogEntry, String>> variables) {
      super(template, false, null);
      this.variables = variables;
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
         return "127.0.0.1";
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

   private static String getLocalHost(LogEntry entry) {
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
}
