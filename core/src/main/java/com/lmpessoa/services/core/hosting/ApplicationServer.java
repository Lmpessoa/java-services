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
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.lmpessoa.services.core.concurrent.IExecutionService;
import com.lmpessoa.services.core.routing.IRouteTable;
import com.lmpessoa.services.core.routing.NonResource;
import com.lmpessoa.services.core.routing.Resource;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.core.serializing.Serializer;
import com.lmpessoa.services.core.services.IServiceMap;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.ClassUtils;
import com.lmpessoa.services.util.logging.ILogger;

/**
 * Represents the Application Server.
 *
 * <p>
 * The static methods in this class are used to start and stop the embedded application server for
 * applications. Applications must call {@link #start()} from their <code>main</code> method to
 * start the application server.
 * </p>
 *
 * <p>
 * The application server can only be configured through a file named either
 * <code>settings.yml</code>, <code>settings.json</code> or <code>application.properties</code>
 * placed next to the application itself (refer to the extended documentation for the available
 * parameters). Files are searched in this exact order; once the first is found, any other files
 * present are ignored.
 * </p>
 */
public final class ApplicationServer {

   private static final String CONFIGURE_SERVICES = "configureServices";
   private static final String CONFIGURE = "configure";

   private static ApplicationServer instance;

   private final ApplicationOptions options = new ApplicationOptions();
   private final Instant startupTime = Instant.now();
   private final ApplicationSettings settings;

   private Collection<Class<?>> resources;
   private ApplicationContext context;
   private ServiceMap services;

   /**
    * Starts the Application Server.
    *
    * <p>
    * This method should be called only once from a <code>main</code> method under the desired startup
    * class.
    */
   public static void start() {
      if (instance == null) {
         Class<?> startupClass = findStartupClass();
         outputBanner(startupClass);
         instance = new ApplicationServer(startupClass);
         instance.run();
      }
   }

   /**
    * Requests that the Application Server be shutdown.
    *
    * <p>
    * Calling this method does not immediately shuts down the server. Any requests in course will be
    * allowed to be completed and any log entries will be written out before the server effectively
    * shuts down.
    * </p>
    */
   public static void shutdown() {
      if (instance != null) {
         instance.settings.getLogger().info("Requested application server to shut down");
         instance.getContext().stop();
      }
   }

   /**
    * Returns the version number of the Application Server.
    * <p>
    * The version number of the server coincides with the version number for the engine itself, and
    * thus may be used interchangeably.
    * </p>
    *
    * @return the version number of the Application Server.
    */
   public static String getVersion() {
      String version = ApplicationServer.class.getPackage().getImplementationVersion();
      if (version == null) {
         URL versionFile = ApplicationServer.class.getResource("/application.version");
         Collection<String> versionLines;
         try {
            versionLines = Files.readAllLines(Paths.get(versionFile.toURI()));
            version = versionLines.toArray()[0].toString();
         } catch (IOException | URISyntaxException e) {
            // Should never get here, but...
            version = null;
         }
      }
      if (version != null) {
         return version.split("-")[0];
      }
      return "";
   }

   ApplicationServer(Class<?> startupClass) {
      this.settings = new ApplicationSettings(this, startupClass);
      initServer();
   }

   ApplicationServer(ApplicationSettings settings) {
      this.settings = settings;
      initServer();
   }

   static ApplicationServer instance() {
      return instance;
   }

   ApplicationSettings getSettings() {
      return settings;
   }

   NextHandler getFirstResponder() {
      return options.getFirstResponder(services);
   }

   synchronized Collection<Class<?>> getResources() {
      if (resources == null) {
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
                        && (endsInResource.matcher(clazz.getSimpleName()).find()
                                 || clazz.isAnnotationPresent(Resource.class))
                        && !clazz.isAnnotationPresent(NonResource.class)) {
                  result.add(clazz);
               }
            } catch (ClassNotFoundException e) {
               // Should never get here since we fetched existing class names
               settings.getLogger().debug(e);
            }
         }
         resources = Collections.unmodifiableCollection(result);
      }
      return resources;
   }

   synchronized ServiceMap getServices() {
      if (services == null) {
         services = new ServiceMap();

         // Registers Singleton services
         services.put(IServiceMap.class, Wrapper.wrap(services));
         services.put(ILogger.class, Wrapper.wrap(settings.getLogger()));
         services.put(IApplicationSettings.class, Wrapper.wrap(settings));
         services.put(IApplicationOptions.class, Wrapper.wrap(options));
         services.put(IHostEnvironment.class, settings.getEnvironment());
         services.put(IExecutionService.class, Wrapper.wrap(settings.getJobExecutor()));

         // Registers PerRequest services
         services.put(IRouteTable.class, (Supplier<IRouteTable>) () -> null);
         services.put(ConnectionInfo.class, (Supplier<ConnectionInfo>) () -> null);
         services.put(HttpRequest.class, (Supplier<HttpRequest>) () -> null);
         services.put(RouteMatch.class, (Supplier<RouteMatch>) () -> null);
         services.put(HeaderMap.class);

         // Runs used defined service registration
         configureServices(services);
         final ServiceMap configMap = getConfigServiceMap(services);
         configureApp(configMap);
         endConfiguration(configMap);
      }
      return services;
   }

   synchronized ApplicationContext getContext() {
      if (context == null) {
         RouteTable routes = new RouteTable(services, settings.getLogger());
         routes.putAll(getResources());
         context = new ApplicationContext(this, settings.getHttpPort(), "http", routes);
      }
      return context;
   }

   ConnectionInfo getConnectionInfo() {
      return getServices().get(ConnectionInfo.class);
   }

   private static Class<?> findStartupClass() {
      Class<?>[] stackClasses = new SecurityManager() {

         public Class<?>[] getStack() {
            return getClassContext();
         }
      }.getStack();
      for (int i = 1; i < stackClasses.length; ++i) {
         if (ApplicationServer.class != stackClasses[i]) {
            return stackClasses[i];
         }
      }
      return null;
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
         String version = getVersion();
         Collection<String> banner = Files.readAllLines(Paths.get(bannerUrl.toURI()));
         banner.stream() //
                  .map(s -> s.replaceAll("\\$\\{project.version\\}", version)) //
                  .forEach(System.out::println); // NOSONAR
      } catch (IOException | URISyntaxException e) {
         // Should never happen but may just ignore
      }
   }

   private void initServer() {
      logStartupMessage(settings.getStartupClass(), settings.getApplicationName());
      Serializer.enableXml(settings.isXmlEnabled());
      AsyncHandler.setExecutor(settings.getJobExecutor());
   }

   private void configureServices(IServiceMap services) {
      IHostEnvironment env = settings.getEnvironment();
      Class<?> startupClass = settings.getStartupClass();
      String envSpecific = CONFIGURE + env.getName() + "Services";
      Method configMethod = ClassUtils.getMethod(startupClass, envSpecific, IServiceMap.class);
      Object[] args = new Object[] { services };
      if (configMethod == null || !Modifier.isStatic(configMethod.getModifiers())) {
         configMethod = ClassUtils.getMethod(startupClass, CONFIGURE_SERVICES, IServiceMap.class,
                  IHostEnvironment.class);
         args = new Object[] { services, env };
      }
      if (configMethod == null || !Modifier.isStatic(configMethod.getModifiers())) {
         configMethod = ClassUtils.getMethod(startupClass, CONFIGURE_SERVICES, IServiceMap.class);
         args = new Object[] { services };
      }
      if (configMethod == null) {
         settings.getLogger().info("Application has no service configuration method");
      } else if (!CONFIGURE_SERVICES.equals(configMethod.getName())) {
         settings.getLogger().info("Using service configuration specific for the environment");
      }
      if (configMethod == null || !Modifier.isStatic(configMethod.getModifiers())) {
         return;
      }
      try {
         configMethod.invoke(null, args);
      } catch (IllegalAccessException | InvocationTargetException e) {
         settings.getLogger().debug(e);
      }
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   private ServiceMap getConfigServiceMap(ServiceMap services) {
      ServiceMap configMap = new ServiceMap();
      configMap.put(IApplicationOptions.class, Wrapper.wrap(options));
      for (Class<?> c : services.getServices()) {
         Object o = services.get(c);
         if (o != null && o instanceof Configurable<?>) {
            Method m = ClassUtils.getMethod(o.getClass(), "getOptions");
            if (m != null) {
               Class<?> configOptions = m.getReturnType();
               if (configOptions != Object.class) {
                  configMap.put(configOptions, new LazyGetOptions(c, services));
               }
            }
         }
      }
      return configMap;
   }

   private void configureApp(ServiceMap configMap) {
      IHostEnvironment env = settings.getEnvironment();
      Class<?> startupClass = settings.getStartupClass();
      String envSpecific = CONFIGURE + env.getName();
      Method[] methods = ClassUtils.findMethods(startupClass,
               m -> envSpecific.equals(m.getName()) && Modifier.isStatic(m.getModifiers()));
      if (methods.length != 1) {
         methods = ClassUtils.findMethods(startupClass,
                  m -> CONFIGURE.equals(m.getName()) && Modifier.isStatic(m.getModifiers()));
      }
      if (methods.length != 1) {
         settings.getLogger().info("Application has no configuration method");
      } else if (!CONFIGURE.equals(methods[0].getName())) {
         settings.getLogger().info("Using application configuration specific for the environment");
      }
      if (methods.length != 1) {
         return;
      }
      try {
         configMap.invoke(startupClass, methods[0]);
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         settings.getLogger().debug(e);
      }
   }

   private void endConfiguration(ServiceMap configMap) {
      for (Class<?> config : configMap.getServices()) {
         Object obj = configMap.get(config);
         if (obj != null && obj instanceof AbstractOptions) {
            ((AbstractOptions) obj).doConfigurationEnded();
         }
      }
   }

   private void run() {
      getServices();
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
      settings.getLogger().info("Application is now listening on ports: %d [%s]", context.getPort(), context.getName());
   }

   private void logStartedMessage() {
      BigDecimal thousand = new BigDecimal(1000);
      Duration duration = Duration.between(this.startupTime, Instant.now());
      BigDecimal uptime = new BigDecimal(duration.toMillis()).divide(thousand);
      BigDecimal vmUptime = new BigDecimal(ManagementFactory.getRuntimeMXBean().getUptime()).divide(thousand);
      settings.getLogger().info("Started application in %s seconds (VM running for %s seconds)", uptime, vmUptime);
   }

   private void logShutdownMessage() {
      Duration duration = Duration.between(this.startupTime, Instant.now());
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
