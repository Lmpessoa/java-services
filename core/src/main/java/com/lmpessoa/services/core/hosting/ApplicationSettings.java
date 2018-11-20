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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.lmpessoa.services.core.concurrent.ExecutionService;
import com.lmpessoa.services.core.validating.IValidationService;
import com.lmpessoa.services.util.ClassUtils;
import com.lmpessoa.services.util.Property;
import com.lmpessoa.services.util.logging.ConsoleHandler;
import com.lmpessoa.services.util.logging.FileHandler;
import com.lmpessoa.services.util.logging.Handler;
import com.lmpessoa.services.util.logging.LogEntry;
import com.lmpessoa.services.util.logging.Logger;
import com.lmpessoa.services.util.logging.Severity;
import com.lmpessoa.services.util.logging.SyslogHandler;

class ApplicationSettings implements IApplicationSettings {

   private static final String LOCATION = ClassUtils.findLocation(ApplicationSettings.class);
   private static final String ABOVE = "above";
   private static final String BELOW = "below";

   private final Supplier<ConnectionInfo> connectionInfoSupplier;
   private final Class<?> startupClass;
   private final Property settings;

   private IValidationService validator;
   private ExecutionService mainExec;
   private ExecutionService jobExec;
   private IHostEnvironment env;
   private Logger log;

   @Override
   public String getApplicationName() {
      return settings.get("application.name").getValue();
   }

   @Override
   public Class<?> getStartupClass() {
      return startupClass;
   }

   @Override
   public boolean isXmlEnabled() {
      Property xml = settings.get("enable.xml");
      if (!xml.isEmpty() && !xml.hasChildren()) {
         return xml.getBoolValueOrDefault(false);
      }
      return false;
   }

   ApplicationSettings(ApplicationServer server, Class<?> startupClass) {
      this(startupClass, getPropertiesNear(startupClass), server::getConnectionInfo);
   }

   ApplicationSettings(Class<?> startupClass, Property settings, Supplier<ConnectionInfo> connectionInfoSupplier) {
      this.connectionInfoSupplier = connectionInfoSupplier;
      this.startupClass = startupClass;
      this.settings = settings;
   }

   IHostEnvironment getEnvironment() {
      if (env == null) {
         String envName = System.getenv("SERVICES_ENVIRONMENT_NAME");
         if (envName == null) {
            envName = "Development";
         }
         final String name = Character.toUpperCase(envName.charAt(0)) + envName.substring(1).toLowerCase();
         env = () -> name;

      }
      return env;
   }

   ExecutionService getMainExecutor() {
      if (mainExec == null) {
         int limit = settings.get("limits.requests").getIntValueOrDefault(0);
         mainExec = new ExecutionService(limit, getLogger());
      }
      return mainExec;
   }

   ExecutionService getJobExecutor() {
      if (jobExec == null) {
         Property prop = settings.get("limits.jobs");
         if (!prop.isEmpty() && prop.getIntValueOrDefault(0) > 0) {
            jobExec = new ExecutionService(prop.getIntValue(), getLogger());
         } else {
            jobExec = getMainExecutor();
         }
      }
      return jobExec;
   }

   IValidationService getValidationService() {
      if (validator == null) {
         validator = IValidationService.newInstance();
      }
      return validator;
   }

   Logger getLogger() {
      if (log == null) {
         Collection<Handler> handlers = getHandlers();
         log = new Logger(startupClass, new StackFilter());
         log.addSupplier(ConnectionInfo.class, connectionInfoSupplier);
         log.addVariable("Remote.Host", ConnectionInfo.class, c -> c.getRemoteAddress().getHostName());
         log.addVariable("Remote.Addr", ConnectionInfo.class, c -> c.getRemoteAddress().getHostAddress());
         log.addVariable("Local.Host", ConnectionInfo.class, c -> c.getLocalAddress().getHostName());
         Property tracing = settings.get("enable.tracing");
         if (!tracing.isEmpty() && !tracing.hasChildren()) {
            log.enableTracing(tracing.getBoolValueOrDefault(false));
         }
         handlers.stream().forEach(log::addHandler);
      }
      return log;
   }

   int getHttpPort() {
      return settings.get("server.port").getIntValueOrDefault(5617);
   }

   private static Property getPropertiesNear(Class<?> startupClass) {
      File location = findLocation(startupClass);
      File file = new File(location, "settings.yml");
      if (!file.exists()) {
         file = new File(location, "settings.yaml");
         if (!file.exists()) {
            file = new File(location, "settings.json");
         }
      }
      try {
         return Property.fromFile(file);
      } catch (Exception e) {
         return Property.EMPTY;
      }
   }

   private static File findLocation(Class<?> startupClass) {
      String pathOfClass = File.separator + startupClass.getName().replaceAll("\\.", File.separator) + ".class";
      String fullPathOfClass = startupClass.getResource(startupClass.getSimpleName() + ".class").toString();
      String result = fullPathOfClass.substring(0, fullPathOfClass.length() - pathOfClass.length());
      if (result.startsWith("jar:")) {
         int lastSep = result.lastIndexOf(File.separator);
         result = result.substring(4, lastSep);
      }
      if (result.startsWith("file:")) {
         result = result.substring(5);
         while (result.startsWith(File.separator + File.separator)) {
            result = result.substring(1);
         }
         String sep = File.separator;
         if (sep.equals("\\")) {
            sep = "\\\\";
         }
         if (result.matches(String.format("%s[a-zA-Z]:%s.*", sep, sep))) {
            result = result.substring(1).replaceAll("/", "\\");
         }
      }
      if (result.matches("[a-zA-Z0-9]+:" + File.separator + ".*")) {
         return null;
      }
      return new File(result);
   }

   private Collection<Handler> getHandlers() {
      Property logSettings = settings.get("log");
      if (logSettings.isEmpty() || !logSettings.hasChildren()) {
         return Arrays.asList(new ConsoleHandler(Severity.atOrAbove(Severity.INFO)));
      }
      List<Handler> result = new ArrayList<>();
      for (Property handlerSettings : logSettings.values()) {
         Predicate<LogEntry> filter = createFilter(handlerSettings);
         String type = handlerSettings.get("type").getValueOrDefault("console");
         Handler handler;
         try {
            switch (type) {
               case "console":
                  handler = new ConsoleHandler(filter);
                  break;
               case "file":
                  Property filename = handlerSettings.get("filename");
                  handler = new FileHandler(filename.getValue(), filter);
                  break;
               case "syslog":
                  Property host = handlerSettings.get("hostname");
                  Property port = handlerSettings.get("port");
                  handler = new SyslogHandler(getApplicationName(), host.getValue(),
                           port.getIntValueOrDefault(SyslogHandler.DEFAULT_PORT), filter);
                  break;
               default:
                  Class<?> clazz = Class.forName(type);
                  Constructor<?> construct = clazz.getConstructor(Predicate.class);
                  handler = (Handler) construct.newInstance(filter);
                  break;
            }
         } catch (Exception e) {
            handler = new ConsoleHandler(filter);
         }
         setHandlerProperties(handlerSettings, handler);
         if (handler != null) {
            result.add(handler);
         }
      }
      return result;
   }

   private void setHandlerProperties(Property handlerSettings, Handler handler) {
      Collection<String> consumed = Arrays.asList("type", ABOVE, BELOW, "packages");
      for (Property sub : handlerSettings.values()) {
         String subName = sub.getName();
         if (!consumed.contains(subName)) {
            final String methodName = "set" + Character.toUpperCase(subName.charAt(0)) + subName.substring(1);
            Method[] methods = ClassUtils.findMethods(handler.getClass(), m -> methodName.equals(m.getName())
                     && m.getParameterTypes().length == 1 && m.getReturnType() == void.class);
            if (methods.length == 1) {
               try {
                  methods[0].invoke(handler, ClassUtils.cast(sub.getValue(), methods[0].getParameterTypes()[0]));
               } catch (Exception e) {
                  // Ignore since we still do not have the handler fully set
               }
            }
         }
      }
   }

   private Predicate<LogEntry> createFilter(Property handlerSettings) {
      Severity above = getSeverityFor(handlerSettings, ABOVE);
      Severity below = getSeverityFor(handlerSettings, BELOW);
      LogFilter result = new LogFilter(above, below);
      Property pkgSettings = handlerSettings.get("packages");
      if (!pkgSettings.isEmpty() && pkgSettings.hasChildren()) {
         for (Property pkg : pkgSettings.values()) {
            String pkgName = pkg.get("name").getValue();
            if (pkgName != null) {
               above = getSeverityFor(pkg, ABOVE);
               below = getSeverityFor(pkg, BELOW);
               result.addPackage(pkgName, above, below);
            }
         }
      }
      return result;
   }

   private Severity getSeverityFor(Property prop, String propName) {
      Severity result;
      try {
         result = Severity.valueOf(prop.get(propName).getValue().toUpperCase());
      } catch (Exception e) {
         result = Severity.NONE;
      }
      return result;
   }

   private static class LogFilter implements Predicate<LogEntry> {

      private final Map<String, Predicate<LogEntry>> packageLevels = new HashMap<>();

      LogFilter(Severity above, Severity below) {
         addPackage(null, above, below);
      }

      void addPackage(String packageName, Severity above, Severity below) {
         Predicate<LogEntry> pred;
         if (above == Severity.NONE && below == Severity.NONE) {
            pred = Severity.atOrAbove(Severity.INFO);
         } else if (below == Severity.NONE) {
            pred = Severity.atOrAbove(above);
         } else if (above == Severity.NONE) {
            pred = Severity.atOrBelow(below);
         } else {
            pred = Severity.between(above, below);
         }
         packageLevels.put(packageName, pred);
      }

      @Override
      public boolean test(LogEntry t) {
         String clazz = t.getClassName();
         String pkg = clazz.substring(0, clazz.lastIndexOf('.'));
         if (!packageLevels.containsKey(pkg)) {
            pkg = null;
         }
         return packageLevels.get(pkg).test(t);
      }
   }

   private static class StackFilter implements Predicate<StackTraceElement> {

      @Override
      public boolean test(StackTraceElement element) {
         if (element.isNativeMethod()) {
            return false;
         }
         String className = element.getClassName();
         if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("sun.")) {
            return false;
         }
         Class<?> clazz;
         try {
            clazz = Class.forName(className);
         } catch (ClassNotFoundException e) {
            return false;
         }
         if (!Modifier.isPublic(clazz.getModifiers())) {
            return false;
         }
         if (LOCATION.equals(ClassUtils.findLocation(clazz))) {
            return false;
         }
         final String methodName = element.getMethodName();
         if ("<cinit>".equals(methodName)) {
            return true;
         }
         Object[] methods;
         if ("<init>".equals(methodName)) {
            methods = ClassUtils.findConstructor(clazz, m -> !m.isSynthetic());
         } else {
            methods = ClassUtils.findMethods(clazz, m -> m.getName().equals(methodName) && !m.isSynthetic());
         }
         return methods.length != 0;
      }
   }
}
