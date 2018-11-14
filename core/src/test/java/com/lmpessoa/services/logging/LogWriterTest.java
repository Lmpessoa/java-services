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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.logging.ConsoleLogWriter;
import com.lmpessoa.services.logging.FileLogWriter;
import com.lmpessoa.services.logging.FormattedLogWriter;
import com.lmpessoa.services.logging.LogWriter;
import com.lmpessoa.services.logging.Logger;
import com.lmpessoa.services.logging.Severity;
import com.lmpessoa.services.services.IConfigurationLifecycle;

public final class LogWriterTest {

   private Logger log;

   @Before
   public void setup() throws ParseException {
      log = new Logger();
      log.getOptions().setDefaultLevel(Severity.INFO);
      log.getOptions().useTemplate("[{Severity}] {Message}");
   }

   private void useWriter(LogWriter writer) {
      log.getOptions().useWriter(writer);
      ((IConfigurationLifecycle) log.getOptions()).configurationEnded();
   }

   @Test
   public void testConsoleWriterError() throws InterruptedException {
      PrintStream originalOut = System.out;
      PrintStream originalErr = System.err;
      ByteArrayOutputStream replaceOut = new ByteArrayOutputStream();
      ByteArrayOutputStream replaceErr = new ByteArrayOutputStream();
      System.setOut(new PrintStream(replaceOut));
      System.setErr(new PrintStream(replaceErr));

      useWriter(new ConsoleLogWriter());
      log.error("Test");
      Thread.sleep(100);
      assertEquals(0, replaceOut.toByteArray().length);
      assertEquals("[ERROR] Test\n", new String(replaceErr.toByteArray()));

      System.setOut(originalOut);
      System.setErr(originalErr);
   }

   @Test
   public void testConsoleWriterWarning() throws InterruptedException {
      PrintStream originalOut = System.out;
      PrintStream originalErr = System.err;
      ByteArrayOutputStream replaceOut = new ByteArrayOutputStream();
      ByteArrayOutputStream replaceErr = new ByteArrayOutputStream();
      System.setOut(new PrintStream(replaceOut));
      System.setErr(new PrintStream(replaceErr));

      useWriter(new ConsoleLogWriter());
      log.warning("Test");
      Thread.sleep(100);
      assertEquals(0, replaceErr.toByteArray().length);
      assertEquals("[WARNING] Test\n", new String(replaceOut.toByteArray()));

      System.setOut(originalOut);
      System.setErr(originalErr);
   }

   @Test
   public void testFileWriter() throws InterruptedException, IOException {
      File tmp = File.createTempFile("test", ".log");
      tmp.deleteOnExit();

      useWriter(new FileLogWriter(tmp));
      log.warning("Test");
      Thread.sleep(100);
      try (BufferedReader buffer = new BufferedReader(new FileReader(tmp))) {
         assertEquals("[WARNING] Test", buffer.readLine());
      }
   }

   final class TestFormattedLogWriter extends FormattedLogWriter {

      private String entry;

      @Override
      protected void append(Severity severity, String entry) {
         this.entry = entry;
      }

      String getLastEntry() {
         return entry;
      }
   }
}
