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
package com.lmpessoa.services.core.hosting;

/**
 * <p>
 * Configuration classes might be interested in freezing certain options or even performing
 * additional tasks after configuration of the application has finished. Options classes may choose
 * to extend this class in order to be notified when the configuration of the application has
 * finished.
 * </p>
 *
 * <p>
 * Subclasses may use two protected methods:
 * </p>
 *
 * <ul>
 * <li>The {@link #configurationEnded()} method is called once after the configuration of the
 * application is done and may be used to perform any operations exactly after the configuration has
 * finished; and</li>
 * <li>The {@link #protectConfiguration()} method can be used as the first call of configuration
 * methods that should not be allowed to change after the configuration stage has finished.</li>
 * </ul>
 */
public abstract class AbstractOptions {

   private boolean configEnded = false;

   void doConfigurationEnded() {
      configEnded = true;
      configurationEnded();
   }

   protected void configurationEnded() {
      // Does nothing, may be overridden in subclasses
   }

   protected final void protectConfiguration() {
      if (configEnded) {
         throw new IllegalStateException("Configuration cannot be changed here");
      }
   }
}
