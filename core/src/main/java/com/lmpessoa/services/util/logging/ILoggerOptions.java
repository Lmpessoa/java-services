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

import java.util.function.Supplier;

import com.lmpessoa.services.util.ConnectionInfo;

/**
 * Enables configuration of a {@link Logger}
 *
 * <p>
 * Methods in this interface represent options that can be used to configure the {@code Logger} for
 * an application. Some of these options, however, can be overridden by a configuration file.
 * </p>
 */
public interface ILoggerOptions {

   /**
    * Sets the default log level for this {@code Logger}.
    *
    * <p>
    * After calling this method, the Logger will ignore any messages whose level is below that of the
    * argument of this method. For example, if this method is called with {@link Severity#WARNING}, any
    * messages logged using {@code info(...)} or {@code debug(...)} will not be registered by the
    * Logger.
    * </p>
    *
    * @param level the default log level for this Logger.
    */
   void setDefaultLevel(Severity level);

   /**
    * Sets a specific log level for classes of the given package.
    *
    * <p>
    * The behaviour of this method is similar to {@link #setDefaultLevel(Severity)} except that it
    * applies only to classes of the given package. After a call to this method, messages to be logged
    * coming from classes in the given package will be filtered using the specified severity as filter
    * while all other classes will still be subject to the default level.
    * </p>
    *
    * <p>
    * This method can be called several times with as many different packages as desired. Calling this
    * method a second time for a previously registered package will change that the log level for that
    * package only. Calling this method with a null level will reset log level for the package will
    * reset it to use the default log level.
    * </p>
    *
    * @param packageName
    * @param level
    */
   void setPackageLevel(String packageName, Severity level);

   /**
    * Sets the a supplier of {@link ConnectionInfo} to be used with this logger.
    *
    * <p>
    * Logged messages may be comprised of information from the connection/request the message is
    * associated with. But in order to obtain this information it is required that the Logger has means
    * to obtain this information.
    * </p>
    *
    * <p>
    * If the logger has no supplier for {@code ConnectionInfo}, then no information about a connection
    * will be available.
    * </p>
    *
    * @param supplier
    */
   void setConnectionSupplier(Supplier<ConnectionInfo> supplier);
}
