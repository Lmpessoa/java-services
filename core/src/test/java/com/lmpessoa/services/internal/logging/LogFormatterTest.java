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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.hosting.ConnectionInfo;
import com.lmpessoa.services.internal.parsing.ParseException;
import com.lmpessoa.services.logging.FormattedHandler;
import com.lmpessoa.services.logging.LogEntry;
import com.lmpessoa.services.logging.Severity;

public final class LogFormatterTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private static final Logger log = new Logger();
   private static final LogEntry entry;

   static {
      Thread.currentThread().setName("test");
      Socket socket = mock(Socket.class);
      when(socket.getInetAddress()).thenReturn(InetAddress.getLoopbackAddress());
      log.addVariable("Remote.Host", ConnectionInfo.class, c -> c.getRemoteAddress().getHostName());
      log.addVariable("Remote.Addr", ConnectionInfo.class,
               c -> c.getRemoteAddress().getHostAddress());
      log.addVariable("Null", String.class, s -> null);
      Map<Class<?>, Object> extra = new HashMap<>();
      extra.put(ConnectionInfo.class, new ConnectionInfo(socket, "https://lmpessoa.com/"));
      entry = new LogEntryImpl(ZonedDateTime.of(LocalDateTime.of(2017, 6, 5, 5, 42, 7),
               ZoneId.of("America/Sao_Paulo")), Severity.ERROR, "Test", extra, log);
   }

   @Test
   public void testFormatTimeWeb() {
      String result = formatWith("{Time.Web}");
      assertEquals("Mon, 5 Jun 2017 08:42:07 GMT", result);
   }

   @Test
   public void testFormatDateValue() {
      String result = formatWith("{Time.Year}-{Time.Month}-{Time.Day}");
      assertEquals("2017-06-05", result);
   }

   @Test
   public void testFormatTimeValue() {
      String result = formatWith("{Time.Hour}:{Time.Minutes}:{Time.Seconds}");
      assertEquals("05:42:07", result);
   }

   @Test
   public void testFormatMonthValue() {
      String result = formatWith("{Time.Month.Name} {Time.Month.Short}");
      assertEquals("June Jun", result);
   }

   @Test
   public void testFormatWeekdayValue() {
      String result = formatWith("{Time.WeekDay} {Time.WeekDay.Short}");
      assertEquals("Monday Mon", result);
   }

   @Test
   public void testFormatAmPmValue() {
      String result = formatWith("{Time.12Hour} {Time.AmPm}");
      assertEquals("05 AM", result);
   }

   @Test
   public void testFormatLevelLeft() {
      String result = formatWith("{Severity:<10}");
      assertEquals("ERROR     ", result);
   }

   @Test
   public void testFormatLevelRight() {
      String result = formatWith("{Severity:>10}");
      assertEquals("     ERROR", result);
   }

   @Test
   public void testFormatLevelShort() {
      String result = formatWith("{Severity:<3}");
      assertEquals("ERR", result);
   }

   @Test
   public void testFormatClassNameAbbreviated() {
      String result = formatWith("{Class.Name:<34}");
      assertEquals("c.l.s.i.logging.LogFormatterTest  ", result);
   }

   @Test
   public void testFormatClassNameAbbreviated2() {
      String result = formatWith("{Class.Name:<27}");
      assertEquals("c.l.s.i.l.LogFormatterTest ", result);
   }

   @Test
   public void testSimpleClassName() {
      String result = formatWith("{Class.SimpleName}");
      assertEquals("LogFormatterTest", result);
   }

   @Test
   public void testFormatRemoteValue() {
      String result = formatWith("{Remote.Host} {Remote.Addr}");
      assertEquals("localhost 127.0.0.1", result);
   }

   @Test
   public void testInvalidVariableReference() {
      thrown.expect(ParseException.class);
      formatWith("{invalid$-var}");
   }

   @Test
   public void testNonDefinedVariable() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Unknown variable: Undefined");
      formatWith("{Undefined}");
   }

   @Test
   public void testThreadReference() {
      String result = formatWith("{Thread.Name}/{Thread.Id}");
      assertEquals("test/1", result);
   }

   @Test
   public void testNullVariable() {
      String result = formatWith("{Null:<7}");
      assertEquals("       ", result);
   }

   @Test
   public void testCompleteFormatter() {
      String result = formatWith(FormattedHandler.DEFAULT);
      assertEquals(
               "2017-06-05T05:42:07.000   ERROR -- [localhost      ] c.l.s.i.logging.LogFormatterTest     : Test",
               result);
   }

   private String formatWith(String template) {
      LogFormatter formatter = LogFormatter.parse(template);
      return formatter.format(entry, entry.getMessage());
   }
}
