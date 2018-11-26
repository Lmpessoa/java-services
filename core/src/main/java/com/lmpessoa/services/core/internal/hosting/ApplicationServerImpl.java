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
package com.lmpessoa.services.core.internal.hosting;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.lmpessoa.services.core.NotPublished;
import com.lmpessoa.services.core.concurrent.IExecutionService;
import com.lmpessoa.services.core.hosting.ApplicationServer;
import com.lmpessoa.services.core.hosting.ConnectionInfo;
import com.lmpessoa.services.core.hosting.HttpRequest;
import com.lmpessoa.services.core.hosting.IApplicationInfo;
import com.lmpessoa.services.core.hosting.IApplicationOptions;
import com.lmpessoa.services.core.hosting.IHostEnvironment;
import com.lmpessoa.services.core.internal.Wrapper;
import com.lmpessoa.services.core.internal.routing.RouteEntry;
import com.lmpessoa.services.core.internal.routing.RouteTable;
import com.lmpessoa.services.core.internal.serializing.Serializer;
import com.lmpessoa.services.core.routing.IRouteTable;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.security.IIdentity;
import com.lmpessoa.services.core.validating.IValidationService;
import com.lmpessoa.services.util.ClassUtils;
import com.lmpessoa.services.util.logging.ILogger;

public final class ApplicationServerImpl {

   private static final String CONFIGURE = "configure";

   private ApplicationSettings settings;
   private ApplicationOptions options;
   private ApplicationContext context;
   private Runnable postAction;

   public ApplicationServerImpl() {
      // Nothing to do here
   }

   public void start() {
      Class<?> startupClass = findStartupClass();
      outputBanner(startupClass);
      settings = new ApplicationSettings(this, startupClass);
      initServer();
      run();
   }

   public void stop(Runnable postAction) {
      this.postAction = postAction;
      getSettings().getLogger().info("Requested application server to shut down");
      getContext().stop();
   }

   public Class<?> getStartupClass() {
      return settings.getStartupClass();
   }

   ApplicationServerImpl(ApplicationSettings settings) {
      this.settings = settings;
      initServer();
   }

   ApplicationSettings getSettings() {
      return settings;
   }

   ApplicationOptions getOptions() {
      return options;
   }

   synchronized ApplicationContext getContext() {
      if (context == null) {
         RouteTable routes = options.getRoutes();
         Collection<RouteEntry> result = routes.putAll(getResources());
         for (RouteEntry entry : result) {
            if (entry.getError() != null) {
               settings.getLogger().warning(entry.getError());
            } else if (entry.getDuplicateOf() != null) {
               settings.getLogger().info(
                        "Route '%s' is already assigned to another method; ignored",
                        entry.getRoute());
            } else {
               Method method = entry.getMethod();
               String paramTypes = Arrays.stream(method.getParameterTypes())
                        .map(Class::getName)
                        .collect(Collectors.joining(", "));
               settings.getLogger().info("Mapped route '%s' to method %s.%s(%s)", entry.getRoute(),
                        method.getDeclaringClass().getName(), method.getName(), paramTypes);
            }
         }
         context = new ApplicationContext(this, settings.getHttpPort(), "http", routes);
      }
      return context;
   }

   ConnectionInfo getConnectionInfo() {
      return options.getServices().get(ConnectionInfo.class);
   }

   Collection<Class<?>> getResources() {
      Collection<String> classes = null;
      try {
         classes = ClassUtils.scanInProjectOf(settings.getStartupClass());
      } catch (IOException e) {
         settings.getLogger().error(e);
         System.exit(1);
      }
      Collection<Class<?>> result = new ArrayList<>();
      final Pattern endsInResource = Pattern.compile("[a-zA-Z0-9]Resource$");
      for (String className : classes) {
         try {
            Class<?> clazz = Class.forName(className);
            if (ClassUtils.isConcreteClass(clazz) && Modifier.isPublic(clazz.getModifiers())
                     && endsInResource.matcher(clazz.getSimpleName()).find()
                     && !clazz.isAnnotationPresent(NotPublished.class)) {
               result.add(clazz);
            }
         } catch (ClassNotFoundException e) {
            // Should never get here since we fetched existing class names
            settings.getLogger().debug(e);
         }
      }
      return Collections.unmodifiableCollection(result);
   }

   void configureServices() {
      IHostEnvironment env = settings.getEnvironment();
      Class<?> startupClass = settings.getStartupClass();
      String envSpecific = CONFIGURE + env.getName();
      Method configMethod = ClassUtils.getMethod(startupClass, envSpecific,
               IApplicationOptions.class);
      Object[] args = new Object[] { options };
      if (configMethod == null || !Modifier.isStatic(configMethod.getModifiers())) {
         configMethod = ClassUtils.getMethod(startupClass, CONFIGURE, IApplicationOptions.class,
                  IHostEnvironment.class);
         args = new Object[] { options, env };
      }
      if (configMethod == null || !Modifier.isStatic(configMethod.getModifiers())) {
         configMethod = ClassUtils.getMethod(startupClass, CONFIGURE, IApplicationOptions.class);
         args = new Object[] { options };
      }
      if (configMethod == null || !Modifier.isStatic(configMethod.getModifiers())) {
         settings.getLogger().info("Application has no service configuration method");
         return;
      } else if (!CONFIGURE.equals(configMethod.getName())) {
         settings.getLogger().info("Using service configuration specific for the environment");
      }
      try {
         configMethod.invoke(null, args);
      } catch (IllegalAccessException | InvocationTargetException e) {
         settings.getLogger().debug(e);
      }
      Serializer.enableXml(options.isXmlEnabled());
   }

   private static Class<?> findStartupClass() {
      Class<?>[] stackClasses = new SecurityManager() {

         public Class<?>[] getStack() {
            return getClassContext();
         }
      }.getStack();
      return Arrays.stream(stackClasses)
               .filter(c -> c != ApplicationServerImpl.class && c != ApplicationServer.class)
               .skip(1)
               .findFirst()
               .orElse(null);
   }

   private static void outputBanner(Class<?> startupClass) {
      Objects.requireNonNull(startupClass);
      URL bannerUrl = startupClass.getResource("/banner.txt");
      if (bannerUrl == null) {
         bannerUrl = ApplicationServer.class.getResource("/banner.txt");
      }
      if (bannerUrl == null) {
         return;
      }
      try {
         String version = ApplicationServer.getVersion();
         Collection<String> banner = Files.readAllLines(Paths.get(bannerUrl.toURI()));
         banner.stream() //
                  .map(s -> s.replaceAll("\\$\\{services.version\\}", version)) //
                  .forEach(System.out::println);
      } catch (IOException | URISyntaxException e) {
         // Should never happen but may just ignore
      }
   }

   private void initServer() {
      AsyncResponder.setExecutor(settings.getJobExecutor());
      options = new ApplicationOptions(services -> {
         // Registers Singleton services
         services.put(ILogger.class, Wrapper.wrap(settings.getLogger()));
         services.put(IHostEnvironment.class, settings.getEnvironment());
         services.put(IExecutionService.class, Wrapper.wrap(settings.getJobExecutor()));
         services.put(IValidationService.class, Wrapper.wrap(settings.getValidationService()));
         services.put(IApplicationInfo.class, Wrapper.wrap(new ApplicationInfo(settings, options)));

         // Registers PerRequest services
         services.putSupplier(IRouteTable.class, () -> null);
         services.putSupplier(ConnectionInfo.class, () -> null);
         services.putSupplier(HttpRequest.class, () -> null);
         services.putSupplier(RouteMatch.class, () -> null);
         services.putSupplier(IIdentity.class, () -> null);
      });
      logStartupMessage(settings.getStartupClass(), settings.getApplicationName());
   }

   private void run() {
      configureServices();
      Thread ct = new Thread(getContext());
      ct.start();
      logCreatedContext(getContext());
      logStartedMessage();
      try {
         ct.join();
      } catch (InterruptedException e) {
         settings.getLogger().warning(e);
         Thread.currentThread().interrupt();
      }
      settings.getMainExecutor().shutdown();
      settings.getJobExecutor().shutdown();
      try {
         settings.getLogger().join();
      } catch (InterruptedException e) {
         settings.getLogger().warning(e);
         Thread.currentThread().interrupt();
      }
      logShutdownMessage();
      if (postAction != null) {
         postAction.run();
      }
   }

   private void logStartupMessage(Class<?> startupClass, String appName) {
      StringBuilder message = new StringBuilder();
      message.append("Starting application");
      if (appName != null) {
         message.append(' ');
         message.append(appName);
      }
      String packVersion = startupClass.getPackage().getImplementationVersion();
      if (packVersion != null && !packVersion.isEmpty()) {
         message.append(" v");
         message.append(packVersion);
      }
      message.append(" on '");
      message.append(settings.getEnvironment().getName());
      message.append("' environment");
      settings.getLogger().info(message);
   }

   private void logCreatedContext(ApplicationContext context) {
      settings.getLogger().info("Application is now listening on ports: %d [%s]", context.getPort(),
               context.getName());
   }

   private void logStartedMessage() {
      BigDecimal thousand = new BigDecimal(1000);
      Duration duration = Duration.between(settings.getStartupTime(), Instant.now());
      BigDecimal uptime = new BigDecimal(duration.toMillis()).divide(thousand);
      BigDecimal vmUptime = new BigDecimal(ManagementFactory.getRuntimeMXBean().getUptime())
               .divide(thousand);
      settings.getLogger().info("Started application in %s seconds (VM running for %s seconds)",
               uptime, vmUptime);
   }

   private void logShutdownMessage() {
      Duration duration = Duration.between(settings.getStartupTime(), Instant.now());
      long millis = duration.toMillis();
      long seconds = millis / 1000;
      long minutes = seconds / 60;
      seconds -= minutes * 60;
      long hours = minutes / 60;
      minutes -= hours * 60;
      StringBuilder durationStr = new StringBuilder();
      if (hours > 0) {
         durationStr.append(hours);
         durationStr.append(" hour");
         if (hours > 1) {
            durationStr.append('s');
         }
      }
      if (minutes > 0) {
         if (durationStr.length() > 0) {
            durationStr.append(", ");
         }
         durationStr.append(minutes);
         durationStr.append(" minute");
         if (minutes > 1) {
            durationStr.append('s');
         }
      }
      if (seconds > 0) {
         if (durationStr.length() > 0) {
            durationStr.append(", ");
         }
         durationStr.append(seconds);
         durationStr.append(" second");
         if (seconds > 1) {
            durationStr.append('s');
         }
      }
      if (durationStr.length() > 0) {
         durationStr.insert(0, " after ");
      }
      settings.getLogger().info("Application stopped%s", durationStr);
   }
}
