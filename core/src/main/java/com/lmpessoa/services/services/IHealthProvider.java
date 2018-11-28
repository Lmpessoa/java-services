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
package com.lmpessoa.services.services;

/**
 * Identifies services that provide their health status.
 *
 * <p>
 * In order to ensure an application is fully working, it is important that the services they depend
 * on can provide information about their status. Services implementing this interface automatically
 * contribute their health to the overall health of the application.
 * </p>
 *
 * <p>
 * Not all services must implement this interface. Services that are always available (e.g. those
 * that are built into the application) do not need to implement this interface since their status
 * is not subject to change.
 * </p>
 *
 * @see HealthStatus
 */
public interface IHealthProvider {

   /**
    * Returns the current status of this service.
    *
    * <p>
    * The status of a service can be one of the following:
    * </p>
    * <ul>
    * <li><b>OK</b> when the service is available and fully operational;</li>
    * <li><b>PARTIAL</b> when the service is operating at partial capacity or if another
    * non-essential service it depends on is not available or operational; or</li>
    * <li><b> FAILED</b> when the service or another essential service it depends on is not
    * available or not operational at all.</li>
    * </ul>
    *
    * @return the current status of this service.
    */
   HealthStatus getHealth();
}
