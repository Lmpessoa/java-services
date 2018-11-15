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

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class LogFormatterTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   static {
      Thread.currentThread().setName("test");
   }

   private final LogEntry entry = new LogEntry(
            ZonedDateTime.of(LocalDateTime.of(2017, 6, 5, 5, 42, 7), ZoneId.of("America/Sao_Paulo")), Severity.ERROR,
            "Test", null, LogFormatterTest.class);

   @Test
   public void testFormatTimeWeb() throws ParseException {
      String result = formatWith("{Time.Web}");
      assertEquals("Mon, 5 Jun 2017 08:42:07 GMT", result);
   }

   @Test
   public void testFormatDateValue() throws ParseException {
      String result = formatWith("{Time.Year}-{Time.Month}-{Time.Day}");
      assertEquals("2017-06-05", result);
   }

   @Test
   public void testFormatTimeValue() throws ParseException {
      String result = formatWith("{Time.Hour}:{Time.Minutes}:{Time.Seconds}");
      assertEquals("05:42:07", result);
   }

   @Test
   public void testFormatMonthValue() throws ParseException {
      String result = formatWith("{Time.Month.Name} {Time.Month.Short}");
      assertEquals("June Jun", result);
   }

   @Test
   public void testFormatWeekdayValue() throws ParseException {
      String result = formatWith("{Time.WeekDay} {Time.WeekDay.Short}");
      assertEquals("Monday Mon", result);
   }

   @Test
   public void testFormatAmPmValue() throws ParseException {
      String result = formatWith("{Time.12Hour} {Time.AmPm}");
      assertEquals("05 AM", result);
   }

   @Test
   public void testFormatLevelLeft() throws ParseException {
      String result = formatWith("{Severity:<10}");
      assertEquals("ERROR     ", result);
   }

   @Test
   public void testFormatLevelRight() throws ParseException {
      String result = formatWith("{Severity:>10}");
      assertEquals("     ERROR", result);
   }

   @Test
   public void testFormatLevelShort() throws ParseException {
      String result = formatWith("{Severity:<3}");
      assertEquals("ERR", result);
   }

   @Test
   public void testFormatClassNameAbbreviated() throws ParseException {
      String result = formatWith("{Class.Name:<32}");
      assertEquals("c.l.s.u.logging.LogFormatterTest", result);
   }

   @Test
   public void testFormatClassNameAbbreviated2() throws ParseException {
      String result = formatWith("{Class.Name:<27}");
      assertEquals("c.l.s.u.l.LogFormatterTest ", result);
   }

   @Test
   public void testSimpleClassName() throws ParseException {
      String result = formatWith("{Class.SimpleName}");
      assertEquals("LogFormatterTest", result);
   }

   @Test
   public void testFormatRemoteValue() throws ParseException {
      String result = formatWith("{Remote.Host} {Remote.Addr}");
      assertEquals("localhost 127.0.0.1", result);
   }

   @Test
   public void testInvalidVariableReference() throws ParseException {
      thrown.expect(ParseException.class);
      formatWith("{invalid$-var}");
   }

   @Test
   public void testNonDefinedVariable() throws ParseException {
      thrown.expect(ParseException.class);
      formatWith("{Undefined}");
   }

   @Test
   public void testThreadReference() throws ParseException {
      String result = formatWith("{Thread.Name}/{Thread.Id}");
      assertEquals("test/1", result);
   }

   @Test
   public void testCompleteFormatter() throws ParseException {
      String result = formatWith(FormattedLogWriter.DEFAULT);
      assertEquals(
               "2017-06-05T05:42:07.000   ERROR -- [localhost      ] c.l.s.util.logging.LogFormatterTest  : Test",
               result);
   }

   private String formatWith(String template) throws ParseException {
      LogFormatter formatter = LogFormatter.parse(template);
      return formatter.format(entry);
   }
}