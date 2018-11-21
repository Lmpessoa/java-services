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
package com.lmpessoa.services.core.hosting;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.lmpessoa.services.core.services.HealthStatus;
import com.lmpessoa.services.core.services.IHealthProvider;
import com.lmpessoa.services.core.services.ServiceMap;

final class ApplicationInfo implements IApplicationInfo {

   private final ApplicationSettings settings;
   private final ApplicationOptions options;

   ApplicationInfo(ApplicationSettings settings, ApplicationOptions options) {
      this.settings = settings;
      this.options = options;
   }

   @Override
   public Class<?> getStartupClass() {
      return settings.getStartupClass();
   }

   @Override
   public String getName() {
      return settings.getApplicationName();
   }

   @Override
   public HealthStatus getHealth() {
      if (!getServiceHealth().values().stream().allMatch(s -> s == HealthStatus.OK)) {
         return HealthStatus.PARTIAL;
      }
      return HealthStatus.OK;
   }

   @Override
   public Map<Class<?>, HealthStatus> getServiceHealth() {
      ServiceMap services = options.getServices();
      Map<Class<?>, HealthStatus> result = new HashMap<>();
      for (Class<?> service : services.getServices()) {
         if (IHealthProvider.class.isAssignableFrom(service)) {
            HealthStatus status = ((IHealthProvider) services.get(service)).getHealth();
            result.put(service, status);
         }
      }
      return Collections.unmodifiableMap(result);
   }

   @Override
   public long getUptime() {
      return Duration.between(settings.getStartupTime(), Instant.now()).toMillis();
   }

   @Override
   public long getUsedMemory() {
      Runtime runtime = Runtime.getRuntime();
      return runtime.totalMemory() - runtime.freeMemory();
   }
}
