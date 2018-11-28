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

import java.util.Objects;
import java.util.function.Predicate;

import com.lmpessoa.services.logging.LogEntry;

/**
 * Defines the level of severity for event logs.
 * <p>
 * There are 5 different levels of severity, each with a different expectation when they occur:
 * </p>
 * <ul>
 * <li><b>FATAL</b> indicates a severe error that cause the application (or a thread) to terminate
 * prematurely;</li>
 * <li><b>ERROR</b> indicates an unexpected error or exception occurred but the application managed
 * to handle it and can continue;</li>
 * <li><b>WARNING</b> indicate an unexpected situation (not mandatorily an exception) occurred and
 * the application handled it but the situation was worth registering;</li>
 * <li><b>INFO</b> are used to register interesting runtime events (i.e. startup/shutdown);</li>
 * <li><b>DEBUG</b> are used to register information about the application execution that can be
 * used to understand if the application behaviour is correct;</li>
 * <li><b>NONE</b> indicates nothing should be logged at all.</li>
 * </ul>
 * <p>
 * Try to attain to these guidelines when defining which level of severity you should use when
 * logging in order to maintain consistency throughout the application.
 * </p>
 */
public enum Severity {
   NONE, FATAL, ERROR, WARNING, INFO, DEBUG;

   public static Predicate<LogEntry> atOrAbove(final Severity level) {
      Objects.requireNonNull(level);
      return entry -> level.compareTo(entry.getSeverity()) >= 0;
   }

   public static Predicate<LogEntry> atOrBelow(final Severity level) {
      Objects.requireNonNull(level);
      return entry -> level.compareTo(entry.getSeverity()) <= 0;
   }

   public static Predicate<LogEntry> between(Severity above, Severity below) {
      Objects.requireNonNull(above);
      Objects.requireNonNull(below);
      if (above.compareTo(below) < 0) {
         Severity tmp = above;
         above = below;
         below = tmp;
      }
      final Severity aboveParam = above;
      final Severity belowParam = below;
      return entry -> aboveParam.compareTo(entry.getSeverity()) >= 0 //
               && belowParam.compareTo(entry.getSeverity()) <= 0;
   }
}
