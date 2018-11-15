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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lmpessoa.services.Internal;
import com.lmpessoa.services.core.NonResource;
import com.lmpessoa.services.core.Resource;
import com.lmpessoa.services.core.routing.IRouteTable;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.core.routing.content.Serializer;
import com.lmpessoa.services.core.services.IConfigurable;
import com.lmpessoa.services.core.services.IConfigurationLifecycle;
import com.lmpessoa.services.core.services.IServiceMap;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.ClassUtils;
import com.lmpessoa.services.util.ConnectionInfo;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.LogEntry;
import com.lmpessoa.services.util.logging.LogWriter;
import com.lmpessoa.services.util.logging.Logger;
import com.lmpessoa.services.util.logging.NonTraced;

/**
 * Defines the servlet for microservice applications. This servlet acts as an interface between the
 * microservice engine and popular web application servers (i.e. Tomcat, Jboss).
 *
 * <p>
 * Applications prepared to run in such containers should not try to subclass this servlet class.
 * Instead register this class as the servlet for your application on your web.xml file. Apart from
 * that, this class is meant to be considered internal to the engine.
 * </p>
 *
 * <p>
 * Also include in the configuration of the servlet an initial parameter named
 * <code>service.startup.classname</code> with the full name of the class which should be used to
 * configure your application (this would be the same class where you call
 * {@link ApplicationServer#start()}).
 * </p>
 */
@Internal
@NonTraced
public final class ApplicationServlet extends GenericServlet {

   private static final long serialVersionUID = 1L;

   private static final String CONFIGURE_SERVICES = "configureServices";
   private static final String CONFIGURE = "configure";

   private final ApplicationOptions options = new ApplicationOptions();

   private Collection<Class<?>> resources;
   private Class<?> startupClass;
   private IHostEnvironment env;
   private ServiceMap services;
   private ILogger log;

   public ApplicationServlet() {
      // Empty just to ensure we have one
   }

   @Override
   public void service(ServletRequest req, ServletResponse res)
      throws ServletException, IOException {
      if (req instanceof HttpServletRequest && res instanceof HttpServletResponse) {
         HttpRequest request = new HttpRequestWrapper((HttpServletRequest) req);
         ConnectionInfo connection = new ServletConnectionInfo(req);
         service(request, connection, (HttpServletResponse) res);
      }
   }

   @Override
   public void init() throws ServletException {
      loadInfoFromContext();
      createServiceMap();
   }

   ApplicationServlet(Class<?>... resources) {
      this.resources = Arrays.asList(resources);
   }

   void service(HttpRequest request, ConnectionInfo connection, HttpServletResponse response) {
      HttpResult result = resolveRequest(request, connection);
      response.setStatus(result.getStatusCode());
      try (HttpResultInputStream is = result.getInputStream()) {
         if (is != null) {
            response.setContentType(is.getContentType());
            response.setContentLength(is.available());
            if (is.getContentEncoding() != null) {
               response.setCharacterEncoding(is.getContentEncoding().name());
            }
            if (is.isForceDownload()) {
               response.setHeader("Content-Disposition",
                        String.format("attachment; filename=\"%s\"", is.getDownloadName()));
            }
            is.copyTo(response.getOutputStream());
         }
      } catch (IOException e) {
         getLogger().fatal(e);
      }
      HeaderMap headers = services.get(HeaderMap.class);
      headers.stream().forEach(s -> response.addHeader(s.getKey(), s.getValue()));
   }

   IServiceMap getServices() {
      return services;
   }

   ILogger getLogger() {
      return log;
   }

   Collection<Class<?>> getResources() {
      if (resources == null) {
         Collection<String> classes;
         try {
            classes = ClassUtils.scanInProjectOf(startupClass);
         } catch (IOException e) {
            log.error(e);
            return null;
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

   private void loadInfoFromContext() throws ServletException {
      ServletContext context = getServletContext();
      if (context instanceof ApplicationContext) {
         ApplicationContext app = (ApplicationContext) context;
         this.startupClass = (Class<?>) app.getAttribute("service.startup.class");
         this.env = app.getEnvironment();
         this.log = app.getLogger();
      } else {
         String className = getServletConfig().getInitParameter("service.startup.classname");
         try {
            this.startupClass = Class.forName(className);
         } catch (ClassNotFoundException e) {
            throw new ServletException(e);
         }
         this.log = createLogger(startupClass);
         this.env = createEnvironment();
         String enableXml = getServletConfig().getInitParameter("service.enable.xml");
         Serializer.enableXml("true".equals(enableXml));
      }
   }

   private IHostEnvironment createEnvironment() {
      String name = System.getProperty("service.environment");
      if (name == null) {
         name = System.getenv("SERVICES_ENVIRONMENT_NAME");
      }
      if (name == null) {
         name = "Development";
      }
      final String envName = Character.toUpperCase(name.charAt(0))
               + name.substring(1).toLowerCase();
      return () -> envName;
   }

   private ILogger createLogger(Class<?> startupClass) {
      return new Logger(startupClass, new LogWriter() {

         @Override
         protected void append(LogEntry entry) {
            // Due to servlet specification, entry severity is lost here
            if (entry.getObjectMessage() instanceof Throwable) {
               log(entry.getMessage(), (Throwable) entry.getObjectMessage());
            } else {
               log(entry.getMessage());
            }
         }
      });
   }

   private void createServiceMap() {
      services = new ServiceMap();

      // Registers Singleton services
      services.useSingleton(IApplicationInfo.class, new ApplicationInfo(startupClass));
      services.useSingleton(IServiceMap.class, services);
      services.useSingleton(ILogger.class, log);
      services.useSingleton(IHostEnvironment.class, env);
      services.useSingleton(IRouteTable.class, RouteTable.class);

      // Registers PerRequest services
      services.usePerRequest(ConnectionInfo.class, () -> null);
      services.usePerRequest(HttpRequest.class, () -> null);
      services.usePerRequest(RouteMatch.class, () -> null);
      services.usePerRequest(HeaderMap.class);

      // Runs used defined service registration
      configureServices(services);
      final ServiceMap configMap = getConfigServiceMap(services);
      configureApp(configMap);
      endConfiguration(configMap);
      registerResources(services);

   }

   private void configureServices(IServiceMap services) {
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

   private void configureApp(IServiceMap configMap) {
      String envSpecific = CONFIGURE + env.getName();
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
      routeTable.putAll(getResources());
   }

   private HttpResult resolveRequest(HttpRequest request, ConnectionInfo info) {
      services.putRequestValue(ConnectionInfo.class, Objects.requireNonNull(info));
      services.putRequestValue(HttpRequest.class, Objects.requireNonNull(request));
      HttpResult result;
      try {
         RouteTable routeTable = (RouteTable) services.get(IRouteTable.class);
         RouteMatch route = routeTable.matches(request);
         services.putRequestValue(RouteMatch.class, route);
         NextHandler chain = options.getFirstHandler(services);
         Object resultObj = chain.invoke();
         if (resultObj instanceof HttpResult) {
            result = (HttpResult) resultObj;
         } else {
            throw new InternalServerError("Unrecognised result type");
         }
      } catch (InternalServerError | HttpException e) {
         result = new HttpResult(request, e.getStatusCode(), e, null);
      }
      log.info(result);
      if (result.getObject() instanceof InternalServerError) {
         log.error(((InternalServerError) result.getObject()).getCause());
      }
      return result;
   }
}
