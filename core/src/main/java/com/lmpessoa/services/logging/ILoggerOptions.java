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

import java.text.ParseException;
import java.util.function.Function;

/**
 * Describes the configuration options for logging.
 *
 * <p>
 * Objects of this class are injected during the configuration phase of the application to allow it
 * to be configured before the application is running and resource classes are loaded.
 * </p>
 */
public interface ILoggerOptions {

   void addVariable(String label, Function<LogEntry, String> func);

   /**
    * Sets the default logging level for the application.
    *
    * <p>
    * The default level is the minimum level of a log entry which will be saved to the log. Entries
    * with lower severity are ignored. Packages with their own levels override this setting.
    * </p>
    *
    * @param level the default logging level for the application.
    */
   void setDefaultLevel(Severity level);

   /**
    * Sets the logging level for entries from the given package.
    *
    * <p>
    * This method enables setting different logging levels for different packages, overriding the
    * default log level. Entries from the given package with lower severity are ignored. Note that
    * child packages can also override the settings for a parent package by calling this method.
    * </p>
    *
    * @param packageName the name of the package to be observed.
    * @param level the desired log level for the given package.
    */
   void setPackageLevel(String packageName, Severity level);

   /**
    * Sets the template to use to format log entries.
    *
    * <p>
    * When log entries are logged as pure text, they must first be formatted before being effectively
    * stored. This method allows developers to set the format t use for such loggers.
    * </p>
    *
    * <p>
    * Templates may be defined by using one of the constants defines in this interface or using your
    * own defined with a string where variable parts are replaces with the name of an existing variable
    * wrapped in curly braces. The following variables may be used when defining a log template:
    * </p>
    *
    * <ul>
    * <li>Time</li>
    * <li>Time.Web</li>
    * <li>Time.AmPm</li>
    * <li>Time.Day</li>
    * <li>Time.Hour</li>
    * <li>Time.12Hour</li>
    * <li>Time.Millis</li>
    * <li>Time.Minutes</li>
    * <li>Time.Month</li>
    * <li>Time.Month.Name</li>
    * <li>Time.Month.Short</li>
    * <li>Time.Offset</li>
    * <li>Time.Seconds</li>
    * <li>Time.WeekDay</li>
    * <li>Time.WeekDay.Short</li>
    * <li>Time.Year</li>
    * <li>Time.Zone</li>
    * <li>Level</li>
    * <li>Message</li>
    * <li>Thread.ID</li>
    * <li>Thread.Name</li>
    * <li>Remote.Addr</li>
    * <li>Remote.Host</li>
    * <li>Class.Name</li>
    * </ul>
    *
    * <p>
    * Optionally the variable name can be followed by an alignment information (either '<' or '>') and
    * a size around parenthesis to ensure the variable space will be constant in text.
    * </p>
    *
    * @param template the template to be used to format log entries as plain text.
    * @throws ParseException if the given template is not valid.
    */
   void useTemplate(String template) throws ParseException;

   /**
    * Sets the writer which will be used to store log entries.
    *
    * @param writer the writer which will be used to store log entries.
    */
   void useWriter(LogWriter writer);

   /**
    * Sets the writer which will be used to store log entries.
    *
    * @param writerClass the class of the writer which will be used to store log entries.
    * @throws InstantiationException
    * @throws IllegalAccessException
    */
   default void useWriter(Class<? extends LogWriter> writerClass)
      throws InstantiationException, IllegalAccessException {
      useWriter(writerClass.newInstance());
   }
}
