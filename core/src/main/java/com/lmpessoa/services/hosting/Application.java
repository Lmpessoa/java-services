/*
 * Copyright (c) 2017 Leonardo Pessoa
 * http://github.com/lmpessoa/java-services
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import com.lmpessoa.services.core.NonResource;
import com.lmpessoa.services.core.Resource;
import com.lmpessoa.services.routing.IRouteTable;
import com.lmpessoa.services.routing.MatchedRoute;
import com.lmpessoa.services.services.IConfigurationLifecycle;
import com.lmpessoa.services.services.IServiceMap;
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
public final class Application {

   private static final String CONFIGURE = "configure";
   private static Application currentApp;

   final List<Thread> threads = new ArrayList<>();

   private final ApplicationOptions options = new ApplicationOptions(this);
   private final HandlerMediator mediator;
   private final IServiceMap serviceMap;
   private final IRouteTable routeTable;
   private final Class<?> startupClass;
   private final IHostEnvironment env;
   private final InetAddress iface;
   private final int port;

   private ServerSocket serverSocket;
   private Method tableMatches;

   /**
    * Starts the application listening on the default port (port 5617).
    * <p>
    * The application will be available in every network address served by the computer.
    * </p>
    */
   public static void start() throws NoSuchMethodException, IOException {
      startAt(5617, null);
   }

   /**
    * Starts the application listening on the given port.
    * <p>
    * The application will be available in every network address served by the computer.
    * </p>
    *
    * @param port the number of the port the application will be listening to.
    */
   public static void startAt(int port) throws NoSuchMethodException, IOException {
      startAt(port, null);
   }

   /**
    * Starts the application listening on the given port and on the given network address.
    *
    * @param addr the network address the application will be listening to.
    * @param port the number of the port the application will be listening to.
    */
   public static void startAt(int port, InetAddress iface) throws NoSuchMethodException, IOException {
      Thread.currentThread().setName("main");
      if (currentApp != null) {
         throw new IllegalStateException("Application is already running");
      }
      currentApp = new Application(findStartupClass(), System.getenv("LEEOW_ENVIRONMENT_NAME"), port, iface);
      currentApp.run();
      currentApp = null;
   }

   /**
    * Shuts down the application gracefully.
    *
    * <p>
    * Applications wishing to enable a graceful shutdown should implement a resource that calls this
    * method. Instead of providing a ready-made resource to support this feature, by requiring
    * developers to implement their own resources to call this method ensures there is no default
    * route for shutdown and enables other security requirements to be implemented.
    * </p>
    */
   public static void shutdown() {
      if (currentApp != null) {
         currentApp.stop();
      }
   }

   Application(Class<?> startupClass, String envName, int port, InetAddress iface) throws NoSuchMethodException {
      this.env = () -> {
         String name = formatEnvName(envName);
         return name != null ? name : "Development";
      };
      this.serviceMap = IServiceMap.newInstance();
      this.routeTable = IRouteTable.newInstance(serviceMap);
      this.serviceMap.putSingleton(IServiceMap.class, this.serviceMap);
      this.serviceMap.putSingleton(IRouteTable.class, this.routeTable);
      this.mediator = new HandlerMediator(serviceMap);
      this.startupClass = startupClass;
      this.port = port;
      this.iface = iface;
   }

   void stop() {
      try {
         serverSocket.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   void run() throws IOException {
      doConfiguration();
      routeTable.putAll(scanResourcesFromStartup());
      listening();
   }

   IServiceMap getServiceMap() {
      return serviceMap;
   }

   IRouteTable getRouteTable() {
      return routeTable;
   }

   HandlerMediator getMediator() {
      return mediator;
   }

   MatchedRoute matches(HttpRequest request) {
      if (tableMatches == null) {
         tableMatches = ClassUtils.getDeclaredMethod(routeTable.getClass(), "matches", HttpRequest.class);
      }
      if (tableMatches == null) {
         return null;
      }
      tableMatches.setAccessible(true);
      try {
         return (MatchedRoute) tableMatches.invoke(routeTable, request);
      } catch (InvocationTargetException e) {
         if (e.getCause() instanceof RuntimeException) {
            throw (RuntimeException) e.getCause();
         } else if (e.getCause() instanceof Error) {
            throw (Error) e.getCause();
         }
         e.printStackTrace();
      } catch (IllegalAccessException | IllegalArgumentException e) {
         e.printStackTrace();
      }
      return null;
   }

   void doConfiguration() {
      configureServices();
      IServiceMap configMap = serviceMap.getConfigMap();
      configMap.putSingleton(IApplicationOptions.class, options);
      configMap.putSingleton(IHostEnvironment.class, env);
      configure(configMap);
      endConfiguration(configMap);
   }

   Collection<Class<?>> scanResourcesFromStartup() throws IOException {
      final Pattern endsInResource = Pattern.compile("[a-zA-Z0-9]Resource$");
      Collection<String> classes = ClassUtils.scanInProjectOf(startupClass);
      Collection<Class<?>> result = new ArrayList<>();
      for (String className : classes) {
         String[] pckgs = className.split("\\.");
         String pckg = String.join(".", Arrays.copyOf(pckgs, pckgs.length - 1));
         if (routeTable.findArea(pckg) == null) {
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

   private static Class<?> findStartupClass() {
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      String appClassName = Application.class.getName();
      for (int i = 1; i < trace.length; ++i) {
         if (!trace[i].getClassName().equals(appClassName)) {
            try {
               return Class.forName(trace[i].getClassName());
            } catch (ClassNotFoundException e) {
               throw new Error(e.getMessage());
            }
         }
      }
      throw new Error("Could not find startup class");
   }

   private static String formatEnvName(String envName) {
      return envName == null ? null : Character.toUpperCase(envName.charAt(0)) + envName.substring(1).toLowerCase();
   }

   private void configureServices() {
      serviceMap.putSingleton(IHostEnvironment.class, env);
      serviceMap.putPerRequest(HttpRequest.class, () -> null);
      serviceMap.putPerRequest(MatchedRoute.class, () -> null);
      String envSpecific = CONFIGURE + env.getName() + "Services";
      Method configServices = ClassUtils.getMethod(startupClass, envSpecific, IServiceMap.class);
      Object[] args = new Object[] { serviceMap };
      if (configServices == null) {
         configServices = ClassUtils.getMethod(startupClass, "configureServices", IServiceMap.class,
                  IHostEnvironment.class);
         args = new Object[] { serviceMap, env };
         if (configServices == null) {
            configServices = ClassUtils.getMethod(startupClass, "configureServices", IServiceMap.class);
            args = new Object[] { serviceMap };
         }
      }
      if (configServices != null && Modifier.isStatic(configServices.getModifiers())) {
         try {
            configServices.invoke(null, args);
         } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
         }
      } else {
         System.err.println("Could not find a service configuration method");
      }
   }

   private void configure(IServiceMap configMap) {
      mediator.addHandler(ResultHandler.class);
      try {
         String envSpecific = CONFIGURE + env.getName();
         Method[] methods = ClassUtils.findMethods(startupClass, m -> envSpecific.equals(m.getName()));
         if (methods.length != 1) {
            methods = ClassUtils.findMethods(startupClass, m -> CONFIGURE.equals(m.getName()));
            if (methods.length != 1) {
               System.err.println("Could not find a configuration method");
               return;
            }
         }

         Method mapInvoke = ClassUtils.getDeclaredMethod(configMap.getClass(), "invoke", Object.class, Method.class);
         mapInvoke.setAccessible(true);
         try {
            mapInvoke.invoke(configMap, startupClass, methods[0]);
         } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
         }
      } finally {
         mediator.addHandler(InvokeHandler.class);
      }
   }

   private void endConfiguration(IServiceMap configMap) {
      Method getter = ClassUtils.getDeclaredMethod(configMap.getClass(), "get", Class.class);
      if (getter == null) {
         return;
      }
      getter.setAccessible(true);
      for (Class<?> service : configMap.getServices()) {
         if (IConfigurationLifecycle.class.isAssignableFrom(service)) {
            try {
               Object obj = getter.invoke(configMap, service);
               ((IConfigurationLifecycle) obj).configurationEnded();
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
               e.printStackTrace();
            }
         }
      }
   }

   private void listening() throws IOException {
      try (ServerSocket server = new ServerSocket(port, 0, iface)) {
         this.serverSocket = server;
         server.setSoTimeout(1);
         while (!server.isClosed()) {
            try {
               Socket socket = server.accept();
               Thread t = new RequestHandlerJob(this, socket);
               t.start();
            } catch (SocketTimeoutException e) {
               // just ignore
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
      while (!threads.isEmpty()) {
         // Do nothing, just wait
      }
   }
}
