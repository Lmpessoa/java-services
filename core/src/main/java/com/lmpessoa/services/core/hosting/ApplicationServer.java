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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import com.lmpessoa.services.core.NonResource;
import com.lmpessoa.services.core.Resource;
import com.lmpessoa.services.util.ClassUtils;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.NonTraced;

@NonTraced
public class ApplicationServer {

   private static ApplicationServer instance;

   private final Class<?> startupClass;
   private final ApplicationInfo info;

   private Collection<Class<?>> resources = null;
   private IHostEnvironment env = null;
   private HttpServerJob server;

   static {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         if (instance != null) {
            ILogger log = instance.getLogger();
            final BigDecimal uptime = toSeconds(instance.info.getUpTime());
            log.info("Stopped application after %s seconds", uptime);
         }
      }));
   }

   public static void start() {
      if (instance == null) {
         instance = new ApplicationServer();
         instance.run();
      }
   }

   public static void shutdown() {
      if (instance != null) {
         ILogger log = instance.getLogger();
         log.info("Application was requested to shutdown");
         instance.info.getThreadPool().shutdown();
         instance.server.stop();
      }
   }

   public Class<?> getStartupClass() {
      return startupClass;
   }

   public ApplicationInfo getApplicationInfo() {
      return info;
   }

   public ILogger getLogger() {
      return info.getLogger();
   }

   public ExecutorService getThreadPool() {
      return info.getThreadPool();
   }

   public IHostEnvironment getEnvironment() {
      if (env == null) {
         String name = getApplicationInfo().getProperty("environment")
                  .orElse(Optional.ofNullable(System.getenv("SERVICES_ENVIRONMENT_NAME"))
                           .orElse("Development"));
         final String envName = Character.toUpperCase(name.charAt(0))
                  + name.substring(1).toLowerCase();
         env = () -> envName;
      }
      return env;
   }

   public static String getVersion() {
      URL versionFile = ApplicationServer.class.getResource("/version.txt");
      Collection<String> versionLines;
      try {
         versionLines = Files.readAllLines(Paths.get(versionFile.toURI()));
         return versionLines.toArray(new String[0])[0];
      } catch (IOException | URISyntaxException e) {
         // Should never get here, but...
         return null;
      }
   }

   public Collection<Class<?>> getResourceClasses() {
      if (resources == null) {
         Collection<String> classes;
         try {
            classes = ClassUtils.scanInProjectOf(getStartupClass());
         } catch (IOException e) {
            getLogger().error(e);
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
               getLogger().debug(e);
            }
         }
         resources = Collections.unmodifiableCollection(result);
      }
      return resources;
   }

   private ApplicationServer() {
      this.startupClass = findStartupClass();
      File location = findLocation();
      this.info = new ApplicationInfo(startupClass, location);
      outputBanner(startupClass);
      logStartupMessage(info);
   }

   private void logStartupMessage(ApplicationInfo info) {
      StringBuilder message = new StringBuilder();
      message.append("Starting application");
      if (info.getName() != null) {
         message.append(' ');
         message.append(info.getName());
      }
      String packVersion = startupClass.getPackage().getImplementationVersion();
      if (packVersion != null && !packVersion.isEmpty()) {
         message.append(" v");
         message.append(packVersion);
      }
      message.append(" on '");
      message.append(getEnvironment().getName());
      message.append("' environment");
      getLogger().info(message);
   }

   private static void outputBanner(Class<?> startupClass) {
      URL bannerUrl = startupClass.getResource("/banner.txt");
      if (bannerUrl == null) {
         bannerUrl = Application.class.getResource("/banner.txt");
      }
      if (bannerUrl == null) {
         return;
      }
      try {
         Collection<String> banner = Files.readAllLines(Paths.get(bannerUrl.toURI()));
         banner.stream() //
                  .map(s -> s.replaceAll("\\$\\{project.version\\}", getVersion())) //
                  .forEach(System.out::println);
      } catch (IOException | URISyntaxException e) {
         // Should never happen but may just ignore
      }
   }

   private static BigDecimal toSeconds(long millis) {
      return new BigDecimal(millis).divide(new BigDecimal(1000));
   }

   private Class<?> findStartupClass() {
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

   private File findLocation() {
      String pathOfClass = File.separator + startupClass.getName().replaceAll("\\.", File.separator)
               + ".class";
      String fullPathOfClass = startupClass.getResource(startupClass.getSimpleName() + ".class")
               .toString();
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

   private void run() {
      Application app = new Application(this, startupClass);
      server = new HttpServerJob(this, app);
      Thread job = new Thread(server);
      job.start();
      getLogger().info("Server is listening on port %d (http)", server.getPort());
      getLogger().info("Application started in %s seconds (JVM running for %s seconds)",
               toSeconds(info.getUpTime()), toSeconds(info.getVMUpTime()));
      try {
         job.join();
      } catch (InterruptedException e) {
         getLogger().debug(e);
         job.interrupt();
      }
   }
}
