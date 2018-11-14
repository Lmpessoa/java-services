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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.text.ParseException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.test.resources.TestResource;

public final class LoggerTest {

   private TestLogWriter output;
   private TestResource res;
   private Logger log;

   @Before
   public void setup() throws ParseException {
      setupWith(Severity.WARNING);
   }

   public void setupWith(Severity defaultLevel) throws ParseException {
      setupWith(defaultLevel, null);
   }

   public void setupWith(Severity defaultLevel, Severity packageLevel) throws ParseException {
      output = new TestLogWriter();
      res = new TestResource();
      log = new Logger(LoggerTest.class, output);
      log.setDefaultLevel(defaultLevel);
      if (packageLevel != null) {
         log.setPackageLevel("com.lmpessoa.services.test.resources", packageLevel);
      }
   }

   @Test
   public void testLastWarningMessage() throws InterruptedException {
      log.warning("Test");
      log.join();
      assertEquals(Severity.WARNING, output.getLastSeverity());
      assertEquals("Test", output.getLastMessage());
      assertArrayEquals(new String[0], output.getLastAdditionalMessages());
   }

   @Test
   public void testLastErrorMessage() throws InterruptedException {
      log.error("Test");
      log.join();
      assertEquals(Severity.ERROR, output.getLastSeverity());
      assertEquals("Test", output.getLastMessage());
      assertArrayEquals(new String[0], output.getLastAdditionalMessages());
   }

   @Test
   public void testLastInfoMessage() throws InterruptedException {
      log.info("Test");
      log.join();
      assertNull(output.getLastSeverity());
      assertNull(output.getLastMessage());
      assertArrayEquals(new String[0], output.getLastAdditionalMessages());
   }

   @Test
   public void testLastNoneMessage() throws InterruptedException, ParseException {
      setupWith(Severity.NONE);
      log.fatal("Test");
      log.join();
      assertNull(output.getLastSeverity());
      assertNull(output.getLastMessage());
      assertArrayEquals(new String[0], output.getLastAdditionalMessages());
   }

   @Test
   public void testTraceEntry() throws InterruptedException {
      log.error("Test");
      log.join();
      LogEntry trace = output.getLastTrace();
      assertNotNull(trace);
      assertEquals(Severity.TRACE, trace.getSeverity());
      assertEquals(
               "...at com.lmpessoa.services.util.logging.LoggerTest.testTraceEntry(LoggerTest.java:102)",
               trace.getMessage());
      assertArrayEquals(new String[0], output.getLastAdditionalMessages());
   }

   @Test
   public void testExceptionNoDebug() throws InterruptedException {
      RuntimeException e = new RuntimeException("Test");
      e.setStackTrace(Arrays.copyOf(e.getStackTrace(), 2));
      log.error(e);
      log.join();
      assertEquals(Severity.ERROR, output.getLastSeverity());
      assertEquals("java.lang.RuntimeException: Test", output.getLastMessage());
      assertArrayEquals(new String[] {
               "...at com.lmpessoa.services.util.logging.LoggerTest.testExceptionNoDebug(LoggerTest.java:115)" },
               output.getLastAdditionalMessages());
      assertNull(output.getLastTrace());
   }

   @Test
   public void testPackageLevelDefault() throws InterruptedException {
      res.log(log);
      log.join();
      assertNull(output.getLastSeverity());
      assertNull(output.getLastMessage());
      assertArrayEquals(new String[0], output.getLastAdditionalMessages());
   }

   @Test
   public void testPackageLevelOff() throws InterruptedException, ParseException {
      setupWith(Severity.WARNING, Severity.NONE);
      res.log(log);
      log.join();
      assertNull(output.getLastSeverity());
      assertNull(output.getLastMessage());
      assertArrayEquals(new String[0], output.getLastAdditionalMessages());
   }

   @Test
   public void testPackageLevelInfo() throws InterruptedException, ParseException {
      setupWith(Severity.WARNING, Severity.INFO);
      res.log(log);
      log.join();
      assertEquals(Severity.INFO, output.getLastSeverity());
      assertEquals("Test", output.getLastMessage());
      assertArrayEquals(new String[0], output.getLastAdditionalMessages());
   }
}
