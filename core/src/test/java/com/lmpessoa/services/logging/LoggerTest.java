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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.internal.logging.Logger;

public final class LoggerTest {

   private TestHandler output;
   private Logger log;

   @Before
   public void setup() {
      setupWith(Severity.WARNING);
   }

   public void setupWith(Severity defaultLevel) {
      output = new TestHandler(Severity.atOrAbove(defaultLevel));
      log = new Logger();
      log.addHandler(output);
   }

   @Test
   public void testLastWarningMessage() throws InterruptedException {
      log.warning("Test");
      log.join();
      assertEquals(Severity.WARNING, output.getLastEntry().getSeverity());
      assertEquals("Test", output.getLastEntry().getMessage());
   }

   @Test
   public void testLastErrorMessage() throws InterruptedException {
      log.error("Test");
      log.join();
      assertEquals(Severity.ERROR, output.getLastEntry().getSeverity());
      assertEquals("Test", output.getLastEntry().getMessage());
   }

   @Test
   public void testLastInfoMessage() throws InterruptedException {
      log.info("Test");
      log.join();
      assertNull(output.getLastEntry());
   }

   @Test
   public void testLastNoneMessage() throws InterruptedException {
      setupWith(Severity.NONE);
      log.fatal("Test");
      log.join();
      assertNull(output.getLastEntry());
   }

   @Test
   public void testLastErrorMessageWithTrace() throws InterruptedException {
      log.enableTracing(true);
      log.error("Test");
      log.join();
      assertEquals(Severity.ERROR, output.getLastEntry().getSeverity());
      String[] message = output.getLastEntry().getMessage().split("\n");
      assertTrue(message.length > 2);
      assertEquals("Test", message[0]);
      assertEquals(
               "...at com.lmpessoa.services.logging.LoggerTest.testLastErrorMessageWithTrace(LoggerTest.java:85)",
               message[1]);
   }

   @Test
   public void testExceptionNoDebug() throws InterruptedException {
      RuntimeException e = new RuntimeException("Test");
      log.error(e);
      log.join();
      assertEquals(Severity.ERROR, output.getLastEntry().getSeverity());
      String[] message = output.getLastEntry().getMessage().split("\n");
      assertTrue(message.length > 2);
      assertEquals("java.lang.RuntimeException: Test", message[0]);
      assertEquals(
               "...at com.lmpessoa.services.logging.LoggerTest.testExceptionNoDebug(LoggerTest.java:98)",
               message[1]);
   }

   @Test
   public void testArrayMessage() throws InterruptedException {
      log.warning(new String[] { "Line #1", "Line #2" });
      log.join();
      assertEquals(Severity.WARNING, output.getLastEntry().getSeverity());
      String[] message = output.getLastEntry().getMessage().split("\n");
      assertArrayEquals(new String[] { "Line #1", "Line #2" }, message);
   }
}
