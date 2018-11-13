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

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

public final class LogFormatterTest {

   private LogEntry entry = new LogEntry(Severity.ERROR, "Test", 0, ZonedDateTime.now().getZone());

   private String formatWith(String template) throws ParseException {
      LogFormatter formatter = LogFormatter.parse(template);
      return formatter.format(entry);
   }

   @Test
   public void testFormatTimeWeb() throws ParseException {
      String result = formatWith("{Time.Web}");
      ZonedDateTime utc = ZonedDateTime.ofInstant(entry.getTime().toInstant(), ZoneOffset.UTC);
      assertEquals(DateTimeFormatter.RFC_1123_DATE_TIME.format(utc), result);
   }

   @Test
   public void testFormatDateValue() throws ParseException {
      String result = formatWith("{Time.Year}-{Time.Month}-{Time.Day}");
      assertEquals(DateTimeFormatter.ISO_LOCAL_DATE.format(entry.getTime()), result);
   }

   @Test
   public void testFormatTimeValue() throws ParseException {
      String result = formatWith("{Time.Hour}:{Time.Minutes}:{Time.Seconds}");
      assertEquals(DateTimeFormatter.ISO_LOCAL_TIME.format(entry.getTime()).substring(0, 8),
               result);
   }

   @Test
   public void testFormatLevelLeft() throws ParseException {
      String result = formatWith("{Severity(<10)}");
      assertEquals("ERROR     ", result);
   }

   @Test
   public void testFormatLevelRight() throws ParseException {
      String result = formatWith("{Severity(>10)}");
      assertEquals("     ERROR", result);
   }

   @Test
   public void testFormatClassNameAbbreviated() throws ParseException {
      String result = formatWith("{Class.Name(<30)}");
      assertEquals("c.l.s.logging.LogFormatterTest", result);
   }

   @Test
   public void testFormatClassNameAbbreviated2() throws ParseException {
      String result = formatWith("{Class.Name(<25)}");
      assertEquals("c.l.s.l.LogFormatterTest ", result);
   }

   @Test
   public void testCompleteFormatter() throws ParseException {
      String result = formatWith(ILoggerOptions.DEFAULT);
      String date = result.substring(0, 23);
      result = result.substring(24);
      String expected = DateTimeFormatter.ISO_DATE_TIME.format(entry.getTime().toLocalDateTime());
      if (expected.length() == 19) {
         expected += ".000";
      }
      while (expected.length() < 23) {
         expected = expected.substring(0, 20) + '0' + expected.substring(20);
      }
      assertEquals(expected, date);
      assertEquals("ERROR   -- [main        ] c.l.services.logging.LogFormatterTest    : Test",
               result);
   }
}
