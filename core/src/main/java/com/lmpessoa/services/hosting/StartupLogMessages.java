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

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import com.lmpessoa.services.logging.ILogger;

final class StartupLogMessages {

   private final Instant started = Instant.now();
   private final Class<?> startupClass;
   private final ILogger log;

   StartupLogMessages(ILogger log, Class<?> startupClass) {
      this.log = log;
      this.startupClass = startupClass;
   }

   void logStartingMessage(IHostEnvironment env) {
      StringBuilder message = new StringBuilder();
      message.append("Starting application on '");
      message.append(env.getName());
      message.append("' environment");
      String packVersion = startupClass.getPackage().getImplementationVersion();
      if (packVersion != null && !packVersion.isEmpty()) {
         message.append(" (v");
         message.append(packVersion);
         message.append(')');
      }
      log.info(message);
   }

   void logServicesConfiguration(Method configMethod) {
      if (configMethod == null) {
         log.info("Startup class has no application configuration method");
      } else if (!"configureServices".equals(configMethod.getName())) {
         log.info("Using service configuration specific for the environment");
      }
   }

   void logConfiguration(Method[] configMethod) {
      if (configMethod.length != 1) {
         log.info("Startup class has no service configuration method");
      } else if (!"configure".equals(configMethod[0].getName())) {
         log.info("Using application configuration specific for the environment");
      }
   }

   void logServerUp(int port) {
      StringBuilder message = new StringBuilder();
      message.append("Server is listening on port ");
      message.append(port);
      message.append(" (http)");
      log.info(message);
   }

   void logStartedMessage() {
      BigDecimal duration = new BigDecimal(Duration.between(started, Instant.now()).toMillis())
               .divide(new BigDecimal(1000));
      double vmduration = ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0;
      log.info("Application started in " + duration + " seconds (JVM running for " + vmduration + " seconds)");
   }
}
