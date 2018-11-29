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
package com.lmpessoa.services.hosting;

import static com.lmpessoa.services.services.Reuse.ALWAYS;

import com.lmpessoa.services.services.Service;

/**
 * Represents the hosting environment information.
 * <p>
 * The hosting environment information is used by the application to define if the application is
 * being executed as development, staging or production environment. It is possible to create other
 * different environments as required but it tests for these environments will have to be made
 * manually.
 * </p>
 *
 * <p>
 * The name of the host environment is defined as the first of the following:
 * <ul>
 * <li>the Java system property {@code service.environment};</li>
 * <li>the variable {@code environment} on the configuration file of the application;</li>
 * <li>the system environment variable {@code SERVICES_ENVIRONMENT_NAME}; or</li>
 * <li>the default value '{@code Development}'.
 * </ul>
 * </p>
 *
 * <p>
 * Note that when running from another application server the configuration file of the application
 * is not used since the Application Server is not started.
 * </p>
 */
@Service(reuse = ALWAYS)
public interface IHostEnvironment {

   /**
    * Returns the name of the running development environment.
    *
    * @return the name of the running development environment.
    */
   String getName();

   /**
    * Returns whether this application is running in a development environment.
    *
    * @return {@code true} if this application is running in a development environment,
    *         {@code false} otherwise.
    */
   default boolean isDevelopment() {
      return "Development".equalsIgnoreCase(getName());
   }

   /**
    * Returns whether this application is running in a staging/testing environment.
    *
    * @return {@code true} if this application is running in a stating/testing environment,
    *         {@code false} otherwise.
    */
   default boolean isStaging() {
      return "Staging".equalsIgnoreCase(getName());
   }

   /**
    * Returns whether this application is running in a production environment.
    *
    * @return {@code true} if this application is running in a production environment, {@code false}
    *         otherwise.
    */
   default boolean isProduction() {
      return "Production".equalsIgnoreCase(getName());
   }
}
