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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.function.Predicate;

import com.lmpessoa.services.logging.FormattedHandler;
import com.lmpessoa.services.logging.LogEntry;

/**
 * A Handler that stores log messages in a file.
 * <p>
 * </p>
 * <p>
 * Use an instance of this class to indicate a file to which all log will be output to. This file
 * will be open and closed periodically to update when log files are rotated by the file system.
 * </p>
 * <p>
 * <b>Configuration:</b><br/>
 * File handlers must specify the name of the file into which the log messages are to be stored. The
 * {@code filename} property can point to any file in the underlying operating system, including
 * special files in *nix systems.
 * </p>
 * <p>
 * Note that the {@code FileHandler} is not capable of rotating log files on its own. You must
 * configure the underlying operating system to do so.
 * </p>
 * <p>
 * Since a {@code FileHandler} is a {@link FormattedHandler}, a different template can be defined in
 * the application settings file under the parameter {@code template}. A template will recognise
 * values in the format {@code ${VAR}} as variables to be replaced with information from the log
 * message entry.
 * </p>
 * <p>
 * In the following example, a logger is defined to send to the file {@code /var/log/service.log}
 * only a timestamp, severity and message text of messages with severity {@code WARNING} or above:
 * </p>
 *
 * <pre>
 * log:
 * - type: file
 *   above: warning
 *   filename: /var/log/service.log
 *   template: ${Time} [${Severity}] ${Message}
 * </pre>
 */
public final class FileHandler extends FormattedHandler {

   java.util.logging.FileHandler x;
   private final File file;

   private PrintWriter out;

   /**
    * Creates a new log writer with the given file.
    *
    * @param file the file where log entries should be written to.
    */
   public FileHandler(File file, Predicate<LogEntry> filter) {
      super(filter);
      this.file = file;
   }

   /**
    * Creates a new log writer with the given file name.
    *
    * @param filename the name of the file where log entries should be written to.
    */
   public FileHandler(String filename, Predicate<LogEntry> filter) {
      this(new File(filename), filter);
   }

   @Override
   public void prepare() {
      try {
         out = new PrintWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8, //
                  StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE),
                  true);
      } catch (IOException e) {
         // Ignore this exception for now
      }
   }

   @Override
   public void append(String entry) {
      if (out != null) {
         out.println(entry);
         out.flush();
      }
   }

   @Override
   public void finished() {
      out.close();
      out = null;
   }
}
