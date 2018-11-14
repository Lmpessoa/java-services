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
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.lmpessoa.services.core.routing.IRouteTable;
import com.lmpessoa.services.core.routing.MatchedRoute;
import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.core.services.IConfigurable;
import com.lmpessoa.services.core.services.IConfigurationLifecycle;
import com.lmpessoa.services.core.services.IServiceMap;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.ClassUtils;
import com.lmpessoa.services.util.ConnectionInfo;
import com.lmpessoa.services.util.logging.ILogger;

public final class Application {

   private static final String CONFIGURE_SERVICES = "configureServices";
   private static final Map<Integer, String> statusCodeTexts = new HashMap<>();
   private static final String CONFIGURE = "configure";
   private static final String CRLF = "\r\n";

   static {
      // These should be better maintained in a separate file but it didn't work during tests.
      statusCodeTexts.put(200, "OK");
      statusCodeTexts.put(201, "Created");
      statusCodeTexts.put(202, "Accepted");
      statusCodeTexts.put(204, "No Content");
      statusCodeTexts.put(301, "Moved Permanently");
      statusCodeTexts.put(302, "Found");
      statusCodeTexts.put(303, "See Other");
      statusCodeTexts.put(304, "Not Modified");
      statusCodeTexts.put(307, "Temporary Redirect");
      statusCodeTexts.put(308, "Permanent Redirect");
      statusCodeTexts.put(400, "Bad Request");
      statusCodeTexts.put(401, "Unauthorized");
      statusCodeTexts.put(403, "Forbidden");
      statusCodeTexts.put(404, "Not Found");
      statusCodeTexts.put(405, "Method Not Allowed");
      statusCodeTexts.put(406, "Not Acceptable");
      statusCodeTexts.put(415, "Unsupported Media Type");
      statusCodeTexts.put(500, "Internal Server Error");
      statusCodeTexts.put(501, "Not Implemented");
   }

   private final ApplicationOptions options = new ApplicationOptions();
   private final ApplicationServer server;
   private final Class<?> startupClass;

   private ServiceMap services;

   public Class<?> getStartupClass() {
      return startupClass;
   }

   public ILogger getLogger() {
      return server.getLogger();
   }

   public IHostEnvironment getEnvironment() {
      return server.getEnvironment();
   }

   public Collection<Class<?>> getResources() {
      return getResources(getServiceMap());
   }

   Application(ApplicationServer server, Class<?> startupClass) {
      this.startupClass = startupClass;
      this.server = server;

      getServiceMap();
   }

   void respondTo(HttpRequest request, ConnectionInfo info, OutputStream output) {
      HttpResult result = resolveRequest(request, info);
      StringBuilder client = new StringBuilder();
      try {
         client.append("HTTP/1.1 ");
         client.append(result.getStatusCode());
         client.append(' ');
         client.append(statusCodeTexts.get(result.getStatusCode()));
         client.append(CRLF);
         byte[] data = null;
         if (result.getInputStream() != null) {
            HttpResultInputStream is = result.getInputStream();
            data = new byte[is.available()];
            int read = is.read(data);
            if (read != data.length) {
               getLogger()
                        .warning("Different lengths (expected: " + data.length + " bytes, found: " + read + " bytes)");
            }
            client.append("Content-Type: ");
            client.append(is.getContentType());
            client.append(CRLF);
            client.append("Content-Length: ");
            client.append(data.length);
            client.append(CRLF);
            if (is.isForceDownload()) {
               client.append("Content-Disposition: attachment; filename=\"");
               client.append(is.getDownloadName());
               client.append('"');
               client.append(CRLF);
            }
            client.append(CRLF);
         }
         output.write(client.toString().getBytes());
         if (data != null) {
            output.write(data);
         }
         output.flush();
      } catch (IOException e) {
         throw new InternalServerError(e);
      }
   }

   private synchronized ServiceMap getServiceMap() {
      if (services == null) {
         services = new ServiceMap();

         // Registers Singleton services
         services.useSingleton(IServiceMap.class, services);
         services.useSingleton(ILogger.class, server.getLogger());
         services.useSingleton(IRouteTable.class, RouteTable.class);
         services.useSingleton(IApplicationInfo.class, server.getApplicationInfo());
         services.useSingleton(IHostEnvironment.class, server.getEnvironment());

         // Registers PerRequest services
         services.usePerRequest(ConnectionInfo.class, () -> null);
         services.usePerRequest(HttpRequest.class, () -> null);
         services.usePerRequest(MatchedRoute.class, () -> null);

         // Runs used defined service registration
         configureServices(services);
         final ServiceMap configMap = getConfigServiceMap(services);
         configureApp(configMap);
         endConfiguration(configMap);
         registerResources(services);
      }
      return services;
   }

   private void configureServices(IServiceMap services) {
      IHostEnvironment env = getEnvironment();
      String envSpecific = CONFIGURE + env.getName() + "Services";
      final Class<?> clazz = getStartupClass();
      Method configMethod = ClassUtils.getMethod(clazz, envSpecific, IServiceMap.class);
      Object[] args = new Object[] { services };
      if (configMethod == null || !Modifier.isStatic(configMethod.getModifiers())) {
         configMethod = ClassUtils.getMethod(clazz, CONFIGURE_SERVICES, IServiceMap.class, IHostEnvironment.class);
         args = new Object[] { services, env };
      }
      if (configMethod == null || !Modifier.isStatic(configMethod.getModifiers())) {
         configMethod = ClassUtils.getMethod(clazz, CONFIGURE_SERVICES, IServiceMap.class);
         args = new Object[] { services };
      }
      if (configMethod == null) {
         getLogger().info("Application has no service configuration method");
      } else if (!CONFIGURE_SERVICES.equals(configMethod.getName())) {
         getLogger().info("Using service configuration specific for the environment");
      }
      if (configMethod == null || !Modifier.isStatic(configMethod.getModifiers())) {
         return;
      }
      try {
         configMethod.invoke(null, args);
      } catch (IllegalAccessException | InvocationTargetException e) {
         getLogger().debug(e);
      }
   }

   private void configureApp(IServiceMap configMap) {
      IHostEnvironment env = getEnvironment();
      String envSpecific = CONFIGURE + env.getName();
      final Class<?> clazz = getStartupClass();
      Method[] methods = ClassUtils.findMethods(clazz,
               m -> envSpecific.equals(m.getName()) && Modifier.isStatic(m.getModifiers()));
      if (methods.length != 1) {
         methods = ClassUtils.findMethods(clazz,
                  m -> CONFIGURE.equals(m.getName()) && Modifier.isStatic(m.getModifiers()));
      }
      if (methods.length != 1) {
         getLogger().info("Application has no configuration method");
      } else if (!"configure".equals(methods[0].getName())) {
         getLogger().info("Using application configuration specific for the environment");
      }
      if (methods.length != 1) {
         return;
      }
      try {
         configMap.invoke(clazz, methods[0]);
      } catch (IllegalAccessException | InvocationTargetException e) {
         getLogger().debug(e);
      }
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   private ServiceMap getConfigServiceMap(ServiceMap services) {
      ServiceMap configMap = new ServiceMap();
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

   private void endConfiguration(IServiceMap configMap) {
      for (Class<?> config : configMap.getServices()) {
         Object obj = configMap.get(config);
         if (obj != null && obj instanceof IConfigurationLifecycle) {
            ((IConfigurationLifecycle) obj).configurationEnded();
         }
      }
   }

   private void registerResources(ServiceMap services) {
      IRouteTable routeTable = services.get(IRouteTable.class);
      routeTable.putAll(getResources(services));
   }

   private Collection<Class<?>> getResources(ServiceMap services) {
      IRouteTable routeTable = services.get(IRouteTable.class);
      return server.getResourceClasses()
               .stream()
               .filter(c -> routeTable.findArea(c.getPackage().getName()) != null)
               .collect(Collectors.toSet());
   }

   private HttpResult resolveRequest(HttpRequest request, ConnectionInfo info) {
      ServiceMap serviceMap = getServiceMap();
      serviceMap.putRequestValue(ConnectionInfo.class, Objects.requireNonNull(info));
      serviceMap.putRequestValue(HttpRequest.class, Objects.requireNonNull(request));
      HttpResult result;
      try {
         RouteTable routeTable = (RouteTable) serviceMap.get(IRouteTable.class);
         MatchedRoute route = routeTable.matches(request);
         serviceMap.putRequestValue(MatchedRoute.class, route);
         NextHandler chain = options.getFirstHandler(serviceMap);
         Object resultObj = chain.invoke();
         if (resultObj instanceof HttpResult) {
            result = (HttpResult) resultObj;
         } else {
            throw new InternalServerError("Unrecognised result type");
         }
      } catch (InternalServerError | HttpException e) {
         result = new HttpResult(request, e.getStatusCode(), e, null);
      }
      getLogger().info(result);
      if (result.getObject() instanceof InternalServerError) {
         getLogger().error(((InternalServerError) result.getObject()).getCause());
      }
      return result;
   }
}
