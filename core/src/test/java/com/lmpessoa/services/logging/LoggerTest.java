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
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public final class LoggerTest {

   private TestLogWriter output;
   private Logger log;

   @Before
   public void setup() {
      log = new Logger();
      log.getOptions().setDefaultLevel(Severity.WARNING);
      output = new TestLogWriter();
      log.getOptions().useWriter(output);
   }

   @Test
   public void testLastWarningMessage() throws InterruptedException {
      log.warning("Test");
      Thread.sleep(100);
      assertEquals(Severity.WARNING, output.getLevel());
      assertEquals("Test", output.getMessage());
   }

   @Test
   public void testLastErrorMessage() throws InterruptedException {
      log.error("Test");
      Thread.sleep(100);
      assertEquals(Severity.ERROR, output.getLevel());
      assertEquals("Test", output.getMessage());
   }

   @Test
   public void testLastInfoMessage() throws InterruptedException {
      log.info("Test");
      Thread.sleep(100);
      assertNull(output.getLevel());
      assertNull(output.getMessage());
   }

   @Test
   public void testExceptionNoDebug() throws InterruptedException {
      RuntimeException e = new RuntimeException("Test");
      e.setStackTrace(Arrays.copyOf(e.getStackTrace(), 2));
      log.error(e);
      Thread.sleep(100);
      assertEquals(Severity.ERROR, output.getLevel());
      assertEquals("java.lang.RuntimeException: Test", output.getMessage());
   }

   @Test
   public void testExceptionWithDebug() throws InterruptedException {
      log.getOptions().setDefaultLevel(Severity.DEBUG);
      RuntimeException e = new RuntimeException("Test");
      e.setStackTrace(Arrays.copyOf(e.getStackTrace(), 1));
      log.error(e);
      Thread.sleep(100);
      assertEquals(Severity.DEBUG, output.getLevel());
      assertEquals(
               "\tat com.lmpessoa.services.logging.LoggerTest.testExceptionWithDebug(LoggerTest.java:83)",
               output.getMessage());
   }

   final class TestLogWriter implements LogWriter {

      private String lastMessage = null;
      private Severity lastLevel = null;

      @Override
      public void append(LogEntry entry) {
         lastMessage = entry.getMessage();
         lastLevel = entry.getSeverity();
      }

      String getMessage() {
         return lastMessage;
      }

      Severity getLevel() {
         return lastLevel;
      }
   }
}
