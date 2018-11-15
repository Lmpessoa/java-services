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
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.lmpessoa.services.util.ClassUtils;
import com.lmpessoa.services.util.logging.ConsoleLogWriter;
import com.lmpessoa.services.util.logging.FileLogWriter;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.ILoggerOptions;
import com.lmpessoa.services.util.logging.LogWriter;
import com.lmpessoa.services.util.logging.Logger;
import com.lmpessoa.services.util.logging.NonTraced;
import com.lmpessoa.services.util.logging.Severity;

/**
 * Represents the Application Server.
 *
 * <p>
 * The static methods in this class are used to start and stop the embedded application server for
 * applications. Applications must call {@link #start()} from their <code>main</code> method
 * to start the application server.
 * </p>
 *
 * <p>
 * The application server can only be configured through a file named either
 * <code>settings.yml</code>, <code>settings.json</code> or <code>application.properties</code>
 * placed next to the application itself (refer to the extended documentation for the available
 * parameters). Files are searched in this exact order; once the first is found, any other files
 * present are ignored.
 *
 * </p>
 */
@NonTraced
public final class ApplicationServer {

   private static ApplicationServer instance;

   private final Instant startupTime = Instant.now();
   private final ApplicationContext context;
   private final ExecutorService threadPool;
   private final IHostEnvironment env;
   private final ILogger log;

   /**
    * Starts the Application Server.
    *
    * <p>
    * This method has no effect if the application is running under a different application server.
    * </p>
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
    *
    * <p>
    * This method has no effect if the application is running under a different application server.
    * </p>
    */
   public static void shutdown() {
      if (instance != null) {
         instance.log.info("Requested application server to shut down");
         instance.context.stop();
      }
   }

   /**
    * Returns the version number of the Application Server.
    * <p>
    * The version number of the server coincides with the version number for the microservice engine itself,
    * and thus may be used interchangeably.
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
      ApplicationServerInfo info = new ApplicationServerInfo(startupClass);
      this.log = createLogger(info, startupClass);
      this.env = createEnvironment(info);
      logStartupMessage(startupClass, info);
      this.threadPool = createJobQueue(info);
      this.context = createApplicationContext(startupClass);
   }

   IHostEnvironment getEnvironment() {
      return env;
   }

   ILogger getLogger() {
      return log;
   }

   Future<?> queueJob(Runnable job) {
      return threadPool.submit(job);
   }

   <T> Future<T> queueJob(Callable<T> job) {
      return threadPool.submit(job);
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

   private void run() {
      Thread ct = new Thread(this.context);
      ct.start();
      logStartedMessage();
      try {
         ct.join();
      } catch (InterruptedException e) {
         log.error(e);
         ct.interrupt();
      }
      threadPool.shutdown();
      try {
         while (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
            // Just sit and wait
         }
      } catch (InterruptedException e) {
         log.error(e);
      }
      logShutdownMessage();
      log.join();
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
                  .forEach(System.out::println);
      } catch (IOException | URISyntaxException e) {
         // Should never happen but may just ignore
      }
   }

   private ApplicationContext createApplicationContext(Class<?> startupClass) {
      ApplicationContext result = new ApplicationContext(this, "http", null, 5617);
      result.setAttribute("service.startup.class", startupClass);
      result.addServlet("service", new ApplicationServlet());
      return result;
   }

   private IHostEnvironment createEnvironment(ApplicationServerInfo info) {
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
      final String envName = Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
      return () -> envName;
   }

   private ExecutorService createJobQueue(ApplicationServerInfo info) {
      ThreadFactory factory = r -> {
         Thread result = new Thread(r);
         result.setUncaughtExceptionHandler((t, e) -> log.fatal(e));
         return result;
      };
      int maxJobCount = info.getIntProperty("requests.limit").orElse(0);
      if (maxJobCount > 0) {
         return Executors.newFixedThreadPool(maxJobCount, factory);
      } else {
         return Executors.newCachedThreadPool(factory);
      }
   }

   private ILogger createLogger(ApplicationServerInfo info, Class<?> startupClass) {
      ILogger result;
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
         ILoggerOptions logOpt = (ILoggerOptions) result;
         Severity level = Severity.valueOf(info.getProperty("logging.default").orElse("INFO"));
         logOpt.setDefaultLevel(level);
         Map<String, String> packages = info.getProperties("logging.packages");
         for (int i = 0; i < packages.size() / 2; ++i) {
            String packageName = packages.get(String.format("%d.name", i));
            level = Severity.valueOf(packages.get(String.format("%d.level", i)));
            logOpt.setPackageLevel(packageName, level);
         }
      } catch (

      Exception e) {
         result = new Logger(startupClass);
         result.error(e);
      }
      return result;
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
      log.info("Stopped application after %s", durationStr);
   }
}
