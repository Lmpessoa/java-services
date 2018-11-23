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
package com.lmpessoa.services.core.services;

/**
 * Represents the possible reuse levels for services.
 *
 * <p>
 * Each service is required to identify how its instances are to be reused by a service map upon
 * requests. Available values are:
 * </p>
 * <ul>
 * <li><b>{@code NEVER}</b> means whenever an instance of the service is required, a new instance of
 * the providing class will be used.</li>
 * <li><b>{@code REQUEST}</b> means a different instance of the service will be created the first
 * time the service is required and will reuse that instance for subsequent calls within the same
 * request for each request received by the application; and</li>
 * <li><b>{@code ALWAYS}</b> means a single instance of the service will be created the first time
 * the service is required and that instance will be reused for any subsequent calls for the same
 * service.</li>
 * </ul>
 */
public enum Reuse {
   /**
    * Service reuse level which means whenever an instance of the service is required, a new
    * instance of the providing class will be used.
    */
   NEVER,
   /**
    * Service reuse level which means a different instance of the service will be created the first
    * time the service is required and will reuse that instance for subsequent calls within the same
    * request for each request received by the application.
    */
   REQUEST,
   /**
    * Service reuse level which means a single instance of the service will be created the first
    * time the service is required and that instance will be reused for any subsequent calls for the
    * same service.
    */
   ALWAYS;
}
