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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Indicates a file to be used to output log entries.
 * <p>
 * Use an instance of this class to indicate a file to which all log will be output to. This file
 * will be open and closed periodically to update when log files are rotated by the file system.
 * </p>
 */
public final class FileLogWriter extends FormattedLogWriter {

   private final File file;

   private PrintWriter out;

   /**
    * Creates a new log writer with the given file.
    *
    * @param file the file where log entries should be written to.
    */
   public FileLogWriter(File file) {
      this.file = file;
   }

   /**
    * Creates a new log writer with the given file name.
    *
    * @param filename the name of the file where log entries should be written to.
    */
   public FileLogWriter(String filename) {
      this(new File(filename));
   }

   @Override
   public void prepare() {
      try {
         file.createNewFile();
         out = new PrintWriter(file, "UTF-8");
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void append(Severity severity, String entry) {
      if (out != null) {
         out.println(entry);
      }
   }

   @Override
   public void finished() {
      out.flush();
      out.close();
      out = null;
   }
}
