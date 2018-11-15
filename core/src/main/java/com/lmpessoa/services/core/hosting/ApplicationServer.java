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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import com.lmpessoa.services.core.NonResource;
import com.lmpessoa.services.core.Resource;
import com.lmpessoa.services.core.hosting.content.Serializer;
import com.lmpessoa.services.core.routing.IRouteTable;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.core.services.IConfigurable;
import com.lmpessoa.services.core.services.IConfigurationLifecycle;
import com.lmpessoa.services.core.services.IServiceMap;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.ClassUtils;
import com.lmpessoa.services.util.ConnectionInfo;
import com.lmpessoa.services.util.logging.ConsoleLogWriter;
import com.lmpessoa.services.util.logging.FileLogWriter;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.LogWriter;
import com.lmpessoa.services.util.logging.Logger;
import com.lmpessoa.services.util.logging.Severity;

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
public final class ApplicationServer implements IApplicationInfo {

   private static final String CONFIGURE_SERVICES = "configureServices";
   private static final String CONFIGURE = "configure";

   private static ApplicationServer instance;

   private final ApplicationOptions options = new ApplicationOptions();
   private final Instant startupTime = Instant.now();
   private final int port;

   private Collection<Class<?>> resources;
   private IHostEnvironment environment;
   private ApplicationContext context;
   private AsyncJobQueue jobQueue;
   private Class<?> startupClass;
   private ServiceMap services;
   private Logger log;

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
         instance.getLogger().info("Requested application server to shut down");
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

   @Override
   public Class<?> getStartupClass() {
      return startupClass;
   }

   ApplicationServer(Class<?> startupClass) {
      this(startupClass, new ApplicationServerInfo(startupClass));
   }

   ApplicationServer(Class<?> startupClass, ApplicationServerInfo info) {
      this(startupClass, info, getEnvironmentName(info), createLogger(info, startupClass));
   }

   ApplicationServer(Class<?> startupClass, ApplicationServerInfo info, String envName, Logger log) {
      this.startupClass = startupClass;
      this.log = log;
   
      Collection<String> enable = info.getProperties("enable").values();
      Serializer.enableXml(enable.contains("xml"));
      this.log.enableTracing(enable.contains("trace"));
      final String name = Character.toUpperCase(envName.charAt(0)) + envName.substring(1).toLowerCase();
      this.environment = () -> name;
      logStartupMessage(startupClass, info);
   
      this.port = Integer.parseInt(info.getProperty("server.port").orElse("5617"));
      this.jobQueue = new AsyncJobQueue(info, log);
   }

   static Logger createLogger(ApplicationServerInfo info, Class<?> startupClass) {
      Logger result;
      try {
         LogWriter writer;
         Map<String, String> logParams = info.getProperties("logging.writer");
         String writerType = logParams.getOrDefault("type", "console");
         logParams.remove("type");
         switch (writerType) {
            case "console":
               writer = new ConsoleLogWriter();
               break;
            case "file":
               String filename = logParams.getOrDefault("filename", null);
               logParams.remove("filename");
               writer = new FileLogWriter(filename);
               break;
            default:
               Class<?> writerClass = Class.forName(writerType);
               writer = (LogWriter) writerClass.newInstance();
               break;
         }
         Class<?> writerClass = writer.getClass();
         for (Entry<String, String> param : logParams.entrySet()) {
            String methodName = String.format("set%s%s", Character.toUpperCase(param.getKey().charAt(0)),
                     param.getKey().substring(1));
            Method[] methods = ClassUtils.findMethods(writerClass, m -> m.getName().equals(methodName));
            if (methods.length == 1 && methods[0].getParameterCount() == 1
                     && methods[0].getParameterTypes()[0].isAssignableFrom(param.getValue().getClass())) {
               methods[0].invoke(writer, param.getValue());
            }
         }
         result = new Logger(startupClass, writer);
         Severity level = Severity.valueOf(info.getProperty("logging.default").orElse("INFO"));
         result.setDefaultLevel(level);
         Map<String, String> packages = info.getProperties("logging.packages");
         for (int i = 0; i < packages.size() / 2; ++i) {
            String packageName = packages.get(String.format("%d.name", i));
            level = Severity.valueOf(packages.get(String.format("%d.level", i)));
            result.setPackageLevel(packageName, level);
         }
      } catch (Exception e) {
         result = new Logger(startupClass);
         result.error(e);
      }
      return result;
   }

   Logger getLogger() {
      return log;
   }

   IHostEnvironment getEnvironment() {
      return environment;
   }

   AsyncJobQueue getJobQueue() {
      return jobQueue;
   }

   NextHandler getFirstResponder() {
      return options.getFirstResponder(services);
   }

   synchronized Collection<Class<?>> getResources() {
      if (resources == null) {
         Collection<String> classes = null;
         try {
            classes = ClassUtils.scanInProjectOf(startupClass);
         } catch (IOException e) {
            log.error(e);
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
               log.debug(e);
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
         services.useSingleton(IServiceMap.class, services);
         services.useSingleton(ILogger.class, log);
         services.useSingleton(IApplicationInfo.class, this);
         services.useSingleton(IHostEnvironment.class, environment);
         services.useSingleton(AsyncJobQueue.class, jobQueue);
   
         // Registers PerRequest services
         services.usePerRequest(IRouteTable.class, () -> null);
         services.usePerRequest(ConnectionInfo.class, () -> null);
         services.usePerRequest(HttpRequest.class, () -> null);
         services.usePerRequest(RouteMatch.class, () -> null);
         services.usePerRequest(HeaderMap.class);
   
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
         RouteTable routes = new RouteTable(services, log);
         routes.putAll(getResources());
         context = new ApplicationContext(this, port, "http", routes);
      }
      return context;
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

   private static String getEnvironmentName(ApplicationServerInfo info) {
      String name = System.getProperty("service.environment");
      if (name == null) {
         name = info.getProperty("environment").orElse(null);
      }
      if (name == null) {
         name = System.getenv("SERVICES_ENVIRONMENT_NAME");
      }
      if (name == null) {
         name = "Development";
      }
      return Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
   }

   private void configureServices(IServiceMap services) {
      String envSpecific = CONFIGURE + environment.getName() + "Services";
      Method configMethod = ClassUtils.getMethod(startupClass, envSpecific, IServiceMap.class);
      Object[] args = new Object[] { services };
      if (configMethod == null || !Modifier.isStatic(configMethod.getModifiers())) {
         configMethod = ClassUtils.getMethod(startupClass, CONFIGURE_SERVICES, IServiceMap.class,
                  IHostEnvironment.class);
         args = new Object[] { services, environment };
      }
      if (configMethod == null || !Modifier.isStatic(configMethod.getModifiers())) {
         configMethod = ClassUtils.getMethod(startupClass, CONFIGURE_SERVICES, IServiceMap.class);
         args = new Object[] { services };
      }
      if (configMethod == null) {
         log.info("Application has no service configuration method");
      } else if (!CONFIGURE_SERVICES.equals(configMethod.getName())) {
         log.info("Using service configuration specific for the environment");
      }
      if (configMethod == null || !Modifier.isStatic(configMethod.getModifiers())) {
         return;
      }
      try {
         configMethod.invoke(null, args);
      } catch (IllegalAccessException | InvocationTargetException e) {
         log.debug(e);
      }
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   private ServiceMap getConfigServiceMap(ServiceMap services) {
      ServiceMap configMap = new ServiceMap();
      configMap.useSingleton(IApplicationOptions.class, options);
      for (Class<?> c : services.getServices()) {
         Object o = services.get(c);
         if (o != null && o instanceof IConfigurable<?>) {
            Method m = ClassUtils.getMethod(o.getClass(), "getOptions");
            if (m != null) {
               Class<?> configOptions = m.getReturnType();
               if (configOptions != Object.class) {
                  configMap.useSingleton(configOptions, new LazyGetOptions(c, services));
               }
            }
         }
      }
      return configMap;
   }

   private void configureApp(ServiceMap configMap) {
      String envSpecific = CONFIGURE + environment.getName();
      Method[] methods = ClassUtils.findMethods(startupClass,
               m -> envSpecific.equals(m.getName()) && Modifier.isStatic(m.getModifiers()));
      if (methods.length != 1) {
         methods = ClassUtils.findMethods(startupClass,
                  m -> CONFIGURE.equals(m.getName()) && Modifier.isStatic(m.getModifiers()));
      }
      if (methods.length != 1) {
         log.info("Application has no configuration method");
      } else if (!CONFIGURE.equals(methods[0].getName())) {
         log.info("Using application configuration specific for the environment");
      }
      if (methods.length != 1) {
         return;
      }
      try {
         configMap.invoke(startupClass, methods[0]);
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         log.debug(e);
      }
   }

   private void endConfiguration(ServiceMap configMap) {
      for (Class<?> config : configMap.getServices()) {
         Object obj = configMap.get(config);
         if (obj != null && obj instanceof IConfigurationLifecycle) {
            ((IConfigurationLifecycle) obj).configurationEnded();
         }
      }
   }

   private void run() {
      getServices();
      Thread ct = new Thread(getContext());
      ct.start();
      logStartedMessage();
      try {
         ct.join();
      } catch (InterruptedException e) {
         log.warning(e);
         ct.interrupt();
      }
      jobQueue.shutdown();
      logShutdownMessage();
      log.join();
   }

   private void logStartupMessage(Class<?> startupClass, ApplicationServerInfo info) {
      StringBuilder message = new StringBuilder();
      message.append("Starting application");
      Optional<String> name = info.getProperty("application.name");
      if (name.isPresent()) {
         message.append(' ');
         message.append(name.get());
      }
      String packVersion = startupClass.getPackage().getImplementationVersion();
      if (packVersion != null && !packVersion.isEmpty()) {
         message.append(" v");
         message.append(packVersion);
      }
      message.append(" on '");
      message.append(getEnvironment().getName());
      message.append("' environment");
      log.info(message);
   }

   private void logStartedMessage() {
      BigDecimal thousand = new BigDecimal(1000);
      Duration duration = Duration.between(this.startupTime, Instant.now());
      BigDecimal uptime = new BigDecimal(duration.toMillis()).divide(thousand);
      BigDecimal vmUptime = new BigDecimal(ManagementFactory.getRuntimeMXBean().getUptime()).divide(thousand);
      log.info("Started application in %s seconds (VM running for %s seconds)", uptime, vmUptime);
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
      log.info("Application stopped%s", durationStr);
   }
}
