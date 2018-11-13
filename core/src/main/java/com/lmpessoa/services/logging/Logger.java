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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

final class Logger implements ILogger {

   private final Queue<LogEntry> entries = new ArrayDeque<>();
   private final LoggerOptions options = new LoggerOptions();

   private Thread loggerJob = null;

   @Override
   public void fatal(Object message) {
      log(Severity.FATAL, message);
   }

   @Override
   public void error(Object message) {
      log(Severity.ERROR, message);
   }

   @Override
   public void warning(Object message) {
      log(Severity.WARNING, message);
   }

   @Override
   public void info(Object message) {
      log(Severity.INFO, message);
   }

   @Override
   public void debug(Object message) {
      log(Severity.DEBUG, message);
   }

   LoggerOptions getOptions() {
      return options;
   }

   private void log(Severity level, Object message) {
      List<LogEntry> entriesToAdd = new ArrayList<>();
      if (message instanceof Throwable) {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         ((Throwable) message).printStackTrace(new PrintStream(out));
         String[] lines = new String(out.toByteArray()).split("\n");
         entriesToAdd.add(new LogEntry(level, lines[0], 2, options.getLogZone()));
         for (int i = 1; i < lines.length; ++i) {
            entriesToAdd.add(new LogEntry(Severity.DEBUG, lines[i], 2, options.getLogZone()));
         }
      } else {
         entriesToAdd.add(new LogEntry(level, message.toString(), 2, options.getLogZone()));
      }
      synchronized (entries) {
         entries.addAll(entriesToAdd);
         if (loggerJob == null) {
            loggerJob = new Thread(new LoggerJob());
            loggerJob.setName("logger");
            loggerJob.start();
         }
      }
   }

   private class LoggerJob implements Runnable {

      @Override
      public void run() {
         while (!entries.isEmpty()) {
            LogEntry entry;
            synchronized (entries) {
               entry = entries.poll();
            }
            options.process(entry);
         }
         loggerJob = null;
      }
   }

}
