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
package com.lmpessoa.services.hosting;

import static com.lmpessoa.services.services.Reuse.ALWAYS;

import java.util.Map;

import com.lmpessoa.services.services.HealthStatus;
import com.lmpessoa.services.services.Service;

/**
 * Provides information about the application at runtime.
 *
 * <p>
 * Instances of this interface provide real time information about the status of the current
 * application and its dependent resources.
 * </p>
 */
@Service(reuse = ALWAYS)
public interface IApplicationInfo {

   /**
    * Returns the class used to start up the application.
    *
    * @return the class used to start up the application.
    */
   Class<?> getStartupClass();

   /**
    * Returns the name of the application.
    *
    * @return the name of the application.
    */
   String getName();

   /**
    * Returns the health status of the application.
    *
    * @return the health status of the application.
    */
   HealthStatus getHealth();

   /**
    * Returns the health status of services used by the application.
    *
    * @return the health status of services used by the application.
    */
   Map<Class<?>, HealthStatus> getServiceHealth();

   /**
    * Returns the amount of time the application is running in milliseconds.
    *
    * @return the amount of time the application is running in milliseconds.
    */
   long getUptime();

   /**
    * Returns the amount of memory used by the application in bytes.
    *
    * @return the amount of memory used by the application in bytes.
    */
   long getUsedMemory();
}
