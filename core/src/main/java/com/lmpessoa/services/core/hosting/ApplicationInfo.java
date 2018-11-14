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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lmpessoa.services.util.ClassUtils;
import com.lmpessoa.services.util.logging.ConsoleLogWriter;
import com.lmpessoa.services.util.logging.FileLogWriter;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.ILoggerOptions;
import com.lmpessoa.services.util.logging.LogWriter;
import com.lmpessoa.services.util.logging.Logger;
import com.lmpessoa.services.util.logging.Severity;

final class ApplicationInfo implements IApplicationInfo {

   private static final String PROPERTY_PATTERN = "[a-z][a-zA-Z0-9_-]*(?:\\.(?:(?:[a-z][a-zA-Z0-9_-]*)|(?:[1-9][0-9]*)))*";

   private final Instant startupTime = Instant.now();
   private final Map<String, String> settings;
   private final Class<?> startupClass;

   private ExecutorService pool = null;
   private InetAddress addr = null;
   private String appName = null;
   private ILogger log = null;
   private int maxJobs = -1;
   private int port = -1;

   @Override
   public Optional<String> getProperty(String key) {
      if (!key.matches(PROPERTY_PATTERN)) {
         return Optional.empty();
      }
      if (settings.containsKey(key)) {
         return Optional.ofNullable(settings.get(key));
      }
      Object obj = settings;
      String[] keyParts = key.split("\\.");
      for (String keyPart : keyParts) {
         if (!(obj instanceof Map)) {
            return Optional.empty();
         }
         Map<?, ?> map = (Map<?, ?>) obj;
         if (!map.containsKey(keyPart)) {
            return Optional.empty();
         }
         obj = map.get(keyPart);
      }
      return Optional.ofNullable(obj.toString());
   }

   @Override
   public Map<String, String> getProperties(String parent) {
      Map<String, String> result = new HashMap<>();
      for (Entry<String, String> entry : settings.entrySet()) {
         if (entry.getKey().startsWith(parent + '.')) {
            result.put(entry.getKey().substring(parent.length() + 1), entry.getValue());
         }
      }
      return result;
   }

   @Override
   public String getName() {
      if (appName == null) {
         appName = startupClass.getSimpleName().replaceAll("Application$", "");
         appName = getProperty("appname").orElse(appName);
      }
      return appName;
   }

   @Override
   public int getPort() {
      if (this.port == -1) {
         try {
            String portStr = ManagementFactory.getRuntimeMXBean()
                     .getInputArguments()
                     .stream() //
                     .filter(s -> s.startsWith("-Dserver.port=")) //
                     .map(s -> s.split("=", 2)[1]) //
                     .findFirst() //
                     .orElse(getProperty("server.port").orElse(null));
            this.port = Integer.parseInt(portStr);
         } catch (NumberFormatException | NoSuchElementException e) {
            this.port = 5617;
         }
      }
      return this.port;
   }

   @Override
   public InetAddress getBindAddress() {
      if (addr == null) {
         try {
            addr = InetAddress.getByName(getProperty("server.address").orElse(null));
         } catch (UnknownHostException | NoSuchElementException e) {
            addr = InetAddress.getLoopbackAddress();
         }
      }
      return addr;
   }

   @Override
   public int getMaxConcurrentJobs() {
      if (maxJobs == -1) {
         maxJobs = Integer.parseInt(getProperty("maxJobs").orElse("0"));
      }
      return maxJobs;
   }

   @Override
   public long getVMUpTime() {
      return ManagementFactory.getRuntimeMXBean().getUptime();
   }

   @Override
   public long getUpTime() {
      return Duration.between(startupTime, Instant.now()).toMillis();
   }

   @Override
   public boolean isXmlEnabled() {
      return "true".equals(getProperty("useXml").orElse("false"));
   }

   public ILogger getLogger() {
      if (log == null) {
         try {
            LogWriter writer;
            Map<String, String> logParams = getProperties("logging.writer");
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
               String methodName = String.format("set%s%s",
                        Character.toUpperCase(param.getKey().charAt(0)),
                        param.getKey().substring(1));
               Method[] methods = ClassUtils.findMethods(writerClass,
                        m -> m.getName().equals(methodName));
               if (methods.length == 1 && methods[0].getParameterCount() == 1
                        && methods[0].getParameterTypes()[0]
                                 .isAssignableFrom(param.getValue().getClass())) {
                  methods[0].invoke(writer, param.getValue());
               }
            }
            log = new Logger(startupClass, writer);
            ILoggerOptions logOpt = (ILoggerOptions) log;
            Severity level = Severity.valueOf(getProperty("logging.default").orElse("INFO"));
            logOpt.setDefaultLevel(level);
            Map<String, String> packages = getProperties("logging.packages");
            for (int i = 0; i < packages.size() / 2; ++i) {
               String packageName = packages.get(String.format("%d.name", i));
               level = Severity.valueOf(packages.get(String.format("%d.level", i)));
               logOpt.setPackageLevel(packageName, level);
            }
         } catch (Exception e) {
            log = new Logger(startupClass);
            log.error(e);
         }
      }
      return log;
   }

   public ExecutorService getThreadPool() {
      if (pool == null) {
         ThreadFactory factory = r -> {
            Thread result = new Thread(r);
            result.setUncaughtExceptionHandler((t, e) -> log.fatal(e));
            return result;
         };
         int maxJobCount = getMaxConcurrentJobs();
         if (maxJobCount > 0) {
            pool = Executors.newFixedThreadPool(maxJobCount, factory);
         } else {
            pool = Executors.newCachedThreadPool(factory);
         }
      }
      return pool;
   }

   ApplicationInfo(Class<?> startupClass, File appRoot) {
      this(startupClass, getConfigInfo(appRoot));
   }

   ApplicationInfo(Class<?> startupClass, Map<String, String> settings) {
      this.settings = Collections.unmodifiableMap(settings);
      this.startupClass = startupClass;
   }

   static Map<String, String> parseConfigMapFile(String type, Reader reader) throws IOException {
      switch (type) {
         case "YML":
            YamlReader yamlReader = new YamlReader(reader);
            return flattenMap(null, (Map<?, ?>) yamlReader.read());
         case "JSON":
            Type typeToken = new TypeToken<Map<Object, Object>>() {}.getType();
            return flattenMap(null, new Gson().fromJson(reader, typeToken));
         case "PROPERTIES":
            Properties props = new Properties();
            props.load(reader);
            Map<String, String> result = new HashMap<>();
            for (Entry<?, ?> entry : props.entrySet()) {
               result.put(entry.getKey().toString(), entry.getValue().toString());
            }
            return result;
         default:
            throw new IllegalArgumentException("Unknown configuration file type: " + type);
      }
   }

   private static Map<String, String> getConfigInfo(File appRoot) {
      try {
         File result = new File(appRoot, "settings.yml");
         if (!result.exists()) {
            result = new File(appRoot, "settings.json");
         }
         if (!result.exists()) {
            result = new File(appRoot, "application.properties");
         }
         if (result.exists()) {
            String type = result.getName()
                     .substring(result.getName().lastIndexOf('.') + 1)
                     .toUpperCase();
            try (FileReader reader = new FileReader(result)) {
               return parseConfigMapFile(type, reader);
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
      return Collections.emptyMap();
   }

   private static Map<String, String> flattenMap(String parentName, Map<?, ?> map) {
      Map<String, String> result = new HashMap<>();
      for (Entry<?, ?> entry : map.entrySet()) {
         String thisName;
         if (parentName != null && !parentName.isEmpty()) {
            thisName = String.format("%s.%s", parentName, entry.getKey());
         } else {
            thisName = entry.getKey().toString();
         }
         if (entry.getValue() instanceof Map<?, ?>) {
            Map<String, String> submap = flattenMap(thisName, (Map<?, ?>) entry.getValue());
            result.putAll(submap);
         } else if (entry.getValue() instanceof List<?>) {
            Map<String, String> submap = flattenList(thisName, (List<?>) entry.getValue());
            result.putAll(submap);
         } else {
            result.put(thisName, entry.getValue().toString());
         }
      }
      return result;
   }

   private static Map<String, String> flattenList(String parentName, List<?> list) {
      Map<String, String> result = new HashMap<>();
      for (int i = 0; i < list.size(); ++i) {
         String thisName = String.format("%s.%d", parentName, i);
         Object entry = list.get(i);
         if (entry instanceof Map<?, ?>) {
            Map<String, String> submap = flattenMap(thisName, (Map<?, ?>) entry);
            result.putAll(submap);
         } else if (entry instanceof List<?>) {
            Map<String, String> submap = flattenList(thisName, (List<?>) entry);
            result.putAll(submap);
         } else {
            result.put(thisName, entry.toString());
         }
      }
      return result;
   }
}
