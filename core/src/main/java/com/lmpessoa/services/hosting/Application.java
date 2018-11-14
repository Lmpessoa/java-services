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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import com.lmpessoa.services.Internal;
import com.lmpessoa.services.core.NonResource;
import com.lmpessoa.services.core.Resource;
import com.lmpessoa.services.logging.ILogger;
import com.lmpessoa.services.logging.NonTraced;
import com.lmpessoa.services.logging.Severity;
import com.lmpessoa.services.routing.IRouteTable;
import com.lmpessoa.services.routing.MatchedRoute;
import com.lmpessoa.services.services.IConfigurationLifecycle;
import com.lmpessoa.services.services.IServiceMap;
import com.lmpessoa.util.ArgumentReader;
import com.lmpessoa.util.ClassUtils;

/**
 * Represents the entry point of the application.
 *
 * <p>
 * Service applications must only call one of the static methods in this class for the application
 * to be started.
 * </p>
 *
 * <p>
 * Applications can be configured through the implementation of two methods:
 * <code>configureServices</code> and <code>configure</code>. The former will enable the application
 * to describe services which can be provided to other classes while the latter will enable to
 * configure how these services will work differently from the default configuration settings. These
 * method must have very specific signatures:
 * </p>
 *
 * <pre>
 * public static void configureService({@link IServiceMap} services, {@link IHostEnvironment} env) { ... }
 *
 * public static void configure({@link IApplicationOptions} app, {@link IHostEnvironment} env) { ... }
 * </pre>
 * <p>
 * It is also possible to declare environment specific versions of this method. If defined, these
 * methods will be called instead of the generic ones shown above. The environment specific versions
 * however do not receive the host environment variable since the environment is already known and
 * are defined by appending the name of the environment after <code>configure</code>. For example,
 * the following would be method specific for configuration of the <em>Staging</em> environment:
 *
 * <pre>
 * public static void configureStagingServices({@link IServiceMap} services) { ... }
 *                             ^^^^^^^
 *
 * public static void configureStaging({@link IApplicationOptions} app) { ... }
 *                             ^^^^^^^
 * </pre>
 * <p>
 * These configuration methods are only called once when the application is started and it is
 * important to note that the configuration objects returned by the <code>IApplicationOptions</code>
 * may enable configurations to be further changed after these methods are called.
 * </p>
 */
@NonTraced
public final class Application {

   private static final String ACCEPT_XML = "accept-xml";
   private static final String MAX_JOBS = "max-jobs";
   private static final String CONFIGURE = "configure";

   private static Application currentApp;

   private final ApplicationOptions options = new ApplicationOptions(this);
   private final Map<String, Object> argMap;
   private final StartupLogMessages startup;
   private final HandlerMediator mediator;
   private final IServiceMap serviceMap;
   private final Class<?> startupClass;
   private final IHostEnvironment env;
   private final ILogger log;

   private ServerSocket serverSocket;
   private InetAddress addr;
   private int port;

   /**
    * Starts the application with the given list of arguments.
    *
    * <p>
    * The list of arguments should be passed from the main method to this unmodified. If a developer
    * wishes to set any of the arguments available from the command line, use the methods in the
    * IApplicationOptions used with the <code>configure</code> method.
    * </p>
    *
    * @param args the arguments received by the application
    * @throws IOException
    * @throws IllegalAccessException
    * @throws NoSuchMethodException
    */
   public static synchronized void startWith(String[] args) throws IOException {
      Thread.currentThread().setName("main");
      Class<?> startupClass = findStartupClass();
      ILogger log = ILogger.newInstance();
      outputBanner(startupClass);
      currentApp = new Application(startupClass, args, log);
      currentApp.run();
      currentApp = null;
   }

   /**
    * Shuts down the application gracefully.
    *
    * <p>
    * Applications wishing to enable a graceful shutdown should implement a resource that calls
    * this method. Instead of providing a ready-made resource to support this feature, by requiring
    * developers to implement their own resources to call this method ensures there is no default
    * route for shutdown and enables other security requirements to be implemented.
    * </p>
    */
   public static void shutdown() {
      if (currentApp != null) {
         currentApp.stop(true);
      }
   }

   /**
    * Forces the application to shut down immediately.
    *
    * <p>
    * Applications wishing to enable a forceful shutdown should implement a resource that calls this
    * method. Instead of providing a ready-made resource to support this feature, by requiring
    * developers to implement their own resources to call this method ensures there is no default
    * route for shutdown and enables other security requirements to be implemented.
    * </p>
    */
   public static void shutdownNow() {
      if (currentApp != null) {
         currentApp.stop(false);
      }
   }

   public static String getVersion() {
      try {
         Properties properties = new Properties();
         properties.load(Application.class.getResourceAsStream("/project.properties"));
         return properties.getProperty("project.version");
      } catch (IOException e) {
         throw new IllegalStateException(e);
      }
   }

   @Internal
   public static Application currentApp() {
      ClassUtils.checkInternalAccess();
      return currentApp;
   }

   @Internal
   public ILogger getLogger() {
      ClassUtils.checkInternalAccess();
      return log;
   }

   public Class<?> getStartupClass() {
      return startupClass;
   }

   Application(Class<?> startupClass, String[] args, ILogger logger) {
      this.log = logger;
      startup = new StartupLogMessages(log, startupClass);
      this.argMap = readArguments(args);
      String envName = (String) argMap.getOrDefault("env",
               System.getenv().getOrDefault("SERVICES_ENVIRONMENT_NAME", "Development"));
      this.env = () -> formatEnvName(envName);
      startup.logStartingMessage(env);
      this.startupClass = startupClass;
      this.serviceMap = IServiceMap.newInstance();
      this.mediator = new HandlerMediator(getServiceMap());
      registerInitialServices();
   }

   static Class<?> findStartupClass() {
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      try {
         return Class.forName(trace[3].getClassName());
      } catch (Exception e) {
         throw new IllegalStateException("Could not find a startup class", e);
      }
   }

   static InetAddress parseIpAddress(String addr) {
      final String ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
      final String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";
      if (!"localhost".equals(addr) && !Pattern.matches(ipv4Pattern, addr) && !Pattern.matches(ipv6Pattern, addr)) {
         throw new IllegalArgumentException("Not a valid IP address: '" + addr + "'");
      }
      try {
         return InetAddress.getByName(addr);
      } catch (UnknownHostException e) {
         throw new IllegalArgumentException("Not a valid IP address: '" + addr + "'", e);
      }
   }

   void run() throws IOException {
      doConfigure();
      getRouteTable().putAll(scanResourcesFromStartup());
      listen();
   }

   void stop(boolean graceful) {
      if (graceful) {
         options.getThreadPool().shutdown();
      } else {
         options.getThreadPool().shutdownNow();
      }
      try {
         serverSocket.close();
      } catch (IOException e) {
         log.debug(e);
      }
   }

   IServiceMap getServiceMap() {
      return serviceMap;
   }

   IRouteTable getRouteTable() {
      return serviceMap.get(IRouteTable.class);
   }

   HandlerMediator getMediator() {
      return mediator;
   }

   MatchedRoute matches(HttpRequest request) {
      return getRouteTable().matches(request);
   }

   Collection<Class<?>> scanResourcesFromStartup() throws IOException {
      final Pattern endsInResource = Pattern.compile("[a-zA-Z0-9]Resource$");
      Collection<String> classes = ClassUtils.scanInProjectOf(startupClass);
      Collection<Class<?>> result = new ArrayList<>();
      for (String className : classes) {
         String[] pckgs = className.split("\\.");
         String pckg = String.join(".", Arrays.copyOf(pckgs, pckgs.length - 1));
         if (getRouteTable().findArea(pckg) == null) {
            continue;
         }
         try {
            Class<?> clazz = Class.forName(className);
            if (ClassUtils.isConcreteClass(clazz) && Modifier.isPublic(clazz.getModifiers())
                     && (endsInResource.matcher(clazz.getSimpleName()).find()
                              || clazz.isAnnotationPresent(Resource.class))
                     && !clazz.isAnnotationPresent(NonResource.class)) {
               result.add(clazz);
            }
         } catch (ClassNotFoundException e) {
            return null;
         }
      }
      return result;
   }

   Future<?> submitJob(Runnable target) {
      return options.getThreadPool().submit(target);
   }

   void doConfigure() {
      configureServices();
      IServiceMap configMap = serviceMap.getConfigMap();
      configMap.putSingleton(IApplicationOptions.class, options);
      configMap.putSingleton(IHostEnvironment.class, env);
      configure(configMap);
      endConfiguration(configMap);

      addr = (InetAddress) argMap.getOrDefault("bind", options.getBindAddress());
      port = (int) argMap.getOrDefault("port", options.getPort());
      if (argMap.containsKey(MAX_JOBS)) {
         options.limitConcurrentJobs((int) argMap.get(MAX_JOBS));
      }
      if (argMap.containsKey(ACCEPT_XML)) {
         options.acceptXmlRequests();
      }
      if (argMap.containsKey("trace")) {
         log.getOptions().setDefaultLevel(Severity.TRACE);
      } else if (argMap.containsKey("debug")) {
         log.getOptions().setDefaultLevel(Severity.DEBUG);
      }
   }

   void registerInitialServices() {
      // Singleton
      serviceMap.putSingleton(ILogger.class, this.log);
      serviceMap.putSingleton(IServiceMap.class, this.serviceMap);
      serviceMap.putSingleton(IRouteTable.class, IRouteTable.getServiceClass());
      serviceMap.putSingleton(IHostEnvironment.class, env);

      // Per Request
      serviceMap.putPerRequest(HttpRequest.class, () -> null);
      serviceMap.putPerRequest(MatchedRoute.class, () -> null);
      serviceMap.putPerRequest(ConnectionInfo.class, () -> null);
   }

   private static void outputBanner(Class<?> startupClass) {
      InputStream bannerIS = startupClass.getResourceAsStream("/banner.txt");
      if (bannerIS == null) {
         bannerIS = Application.class.getResourceAsStream("/banner.txt");
      }
      if (bannerIS == null) {
         return;
      }
      BufferedReader banner = new BufferedReader(new InputStreamReader(bannerIS));
      banner.lines() //
               .map(s -> s.replaceAll("\\$\\{project.version\\}", Application.getVersion())) //
               .forEach(System.out::println);
   }

   private static String formatEnvName(String envName) {
      return envName == null ? null : Character.toUpperCase(envName.charAt(0)) + envName.substring(1).toLowerCase();
   }

   private Map<String, Object> readArguments(String[] args) {
      ArgumentReader reader = new ArgumentReader();
      reader.setFlag('X', ACCEPT_XML);
      reader.setOption('b', "bind", Application::parseIpAddress);
      reader.setFlag('d', "debug");
      reader.setOption('e', "env", s -> s);
      reader.setOption('j', MAX_JOBS, Integer::valueOf);
      reader.setOption('p', "port", Integer::valueOf);
      reader.setFlag('t', "trace");
      try {
         return reader.parse(args);
      } catch (Exception e) {
         System.out.println(e.getMessage());
         System.exit(1);
      }
      return null;
   }

   private void configure(IServiceMap configMap) {
      mediator.addHandler(ResultHandler.class);
      try {
         String envSpecific = CONFIGURE + env.getName();
         Method[] methods = ClassUtils.findMethods(startupClass,
                  m -> envSpecific.equals(m.getName()) && Modifier.isStatic(m.getModifiers()));
         if (methods.length != 1) {
            methods = ClassUtils.findMethods(startupClass,
                     m -> CONFIGURE.equals(m.getName()) && Modifier.isStatic(m.getModifiers()));
         }
         startup.logConfiguration(methods);
         if (methods.length != 1) {
            return;
         }
         try {
            configMap.invoke(startupClass, methods[0]);
         } catch (IllegalAccessException | InvocationTargetException e) {
            log.debug(e);
         }
      } finally {
         mediator.addHandler(InvokeHandler.class);
      }
   }

   private void configureServices() {
      String envSpecific = CONFIGURE + env.getName() + "Services";
      Method configServices = ClassUtils.getMethod(startupClass, envSpecific, IServiceMap.class);
      Object[] args = new Object[] { serviceMap };
      if (configServices == null || !Modifier.isStatic(configServices.getModifiers())) {
         configServices = ClassUtils.getMethod(startupClass, "configureServices", IServiceMap.class,
                  IHostEnvironment.class);
         args = new Object[] { serviceMap, env };
      }
      if (configServices == null || !Modifier.isStatic(configServices.getModifiers())) {
         configServices = ClassUtils.getMethod(startupClass, "configureServices", IServiceMap.class);
         args = new Object[] { serviceMap };
      }
      startup.logServicesConfiguration(configServices);
      if (configServices == null || !Modifier.isStatic(configServices.getModifiers())) {
         return;
      }
      try {
         configServices.invoke(null, args);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
         log.debug(e);
      }
   }

   private void endConfiguration(IServiceMap configMap) {
      for (Class<?> config : configMap.getServices()) {
         Object obj = configMap.get(config);
         if (obj != null && obj instanceof IConfigurationLifecycle) {
            ((IConfigurationLifecycle) obj).configurationEnded();
         }
      }
   }

   private void listen() throws IOException {
      try (ServerSocket server = new ServerSocket(port, 0, addr)) {
         startup.logServerUp(port);
         this.serverSocket = server;
         server.setSoTimeout(1);
         startup.logStartedMessage();
         while (!server.isClosed()) {
            try {
               Socket socket = server.accept();
               options.getThreadPool().execute(new RequestHandlerJob(this, socket));
            } catch (SocketTimeoutException e) {
               // just ignore
            } catch (IOException e) {
               log.error(e);
            }
         }
      }
      while (!options.getThreadPool().isTerminated()) {
         // Do nothing, just wait
      }
   }
}
