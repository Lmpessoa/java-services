/*
 * Copyright (c) 2018 Leonardo Pessoa
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
package com.lmpessoa.services.views.templating;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * Indicates one or more errors were found during the parsing of a template.
 *
 * <p>
 * Different from most other exceptions, this exception may contain multiple messages which can be
 * used to identify errors in the structure of a template.
 * </p>
 *
 * <p>
 * Classes implementing {@link RenderizationEngine} must validate the template according to their
 * recognised syntaxes and list any errors found by throwing this exception. They are also
 * responsible for the actual message to be shown and pointing the correct location of the error in
 * the template.
 * </p>
 */
public final class TemplateParseException extends Exception {

   private static final long serialVersionUID = 1L;

   private final transient String filename;
   private final transient String template;
   private final transient ParseError[] entries;

   /**
    * Creates a new {@code TemplateParseException} for the given template.
    *
    * @param filename the name of the file that was parsed and produced this exception.
    * @param template the template that contained errors.
    * @param entries a list of errors found on the given template.
    */
   public TemplateParseException(String filename, String template, ParseError[] entries) {
      this.filename = filename;
      this.template = template;
      this.entries = entries;
      if (entries.length == 0) {
         throw new IllegalArgumentException("entries");
      }
   }

   /**
    * Creates a new {@code TemplateParseException} for the given template.
    *
    * @param filename the name of the file that was parsed and produced this exception.
    * @param template the template that contained errors.
    * @param entries a list of errors found on the given template.
    */
   public TemplateParseException(String filename, String template, Collection<ParseError> entries) {
      this(filename, template, entries.toArray(new ParseError[0]));
   }

   /**
    * Returns the list of errors in this {@code TemplateParseException}.
    *
    * @return the list of errors in this exception.
    */
   public ParseError[] getErrors() {
      return Arrays.copyOf(entries, entries.length);
   }

   @Override
   public String getMessage() {
      String[] lines = template.split("\n");
      StringBuilder result = new StringBuilder();
      for (ParseError entry : entries) {
         int[] pos = positionOf(entry.position());
         int len = entry.length();
         result.append(String.format("%s at %d:%d: error: %s", filename, pos[0], pos[1],
                  entry.message()));
         result.append('\n');
         result.append(lines[pos[0] - 1]);
         result.append('\n');
         for (int i = 0; i < pos[1] - 1; ++i) {
            result.append(' ');
         }
         result.append('^');
         for (int i = 0; i < len - 1; ++i) {
            result.append('~');
         }
         result.append('\n');
      }
      return result.toString();
   }

   private int[] positionOf(int pos) {
      int lineStart = 0;
      int line = 1;
      int n = 0;
      while (pos > n && n > -1 && n < template.length()) {
         n = template.indexOf('\n', lineStart);
         if (n > -1 && pos > n) {
            lineStart = n + 1;
            line += 1;
         }
      }
      return new int[] { line, pos - lineStart + 1 };
   }

   /**
    * Represents a single parse error.
    *
    * <p>
    * Classes throwing a {@link TemplateParseException} must provide as many instances of this class
    * to help identify each error in the parsed template. At least one error must be provided while
    * creating a {@code TemplateParseException}.
    * </p>
    */
   public static class ParseError {

      private final String message;
      private final int length;
      private final int pos;

      /**
       * Creates a new {@code ParseError} with the given arguments.
       *
       * @param pos the position in the template where the error occurred.
       * @param len the length of the element that caused the error.
       * @param message a message describing the error found.
       */
      public ParseError(int pos, int len, String message) {
         this.message = Objects.requireNonNull(message);
         this.length = len < 1 ? 1 : len;
         this.pos = pos < 0 ? 0 : pos;
      }

      /**
       * Returns the linear position where this error occurred.
       *
       * <p>
       * The linear position is the position where the error occurred considering the template is in
       * a single line (i.e. ignoring that '\n' creates a new line). For this matter, '\r\n' is
       * accounted as a single character.
       * </p>
       *
       * @return the linear position where this error occurred.
       */
      public int position() {
         return pos;
      }

      /**
       * Returns the length of the element that caused this error.
       *
       * @return the length of the element that caused this error.
       */
      public int length() {
         return length;
      }

      /**
       * Returns the message describing this error.
       *
       * @return the message describing this error.
       */
      public String message() {
         return message;
      }
   }
}
