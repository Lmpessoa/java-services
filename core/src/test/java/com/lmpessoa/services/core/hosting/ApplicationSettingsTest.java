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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.lmpessoa.services.core.services.IServiceMap;
import com.lmpessoa.services.util.logging.LogEntry;
import com.lmpessoa.services.util.logging.LogWriter;
import com.lmpessoa.services.util.logging.Logger;
import com.lmpessoa.services.util.logging.NullLogWriter;

public final class ApplicationSettingsTest {

   private static Logger log = new Logger(ApplicationSettingsTest.class, new NullLogWriter());
   private static String servicesResult;
   private static String configResult;
   private static String logResult;

   private ApplicationServer server;

   private void setup(Class<?> startupClass) {
      setup(startupClass, "Development");
   }

   private void setup(Class<?> startupClass, String envName) {
      ApplicationServerInfo info = mock(ApplicationServerInfo.class);
      server = new ApplicationServer(startupClass, info, envName, log);
      server.getServices();
   }

   @Test
   public void testScanDefault() {
      setup(ApplicationInfoTest.class);
      Collection<Class<?>> result = server.getResources();
      assertTrue(result.contains(com.lmpessoa.services.test.resources.IndexResource.class));
      assertTrue(result.contains(com.lmpessoa.services.test.resources.TestResource.class));
      assertTrue(result.contains(com.lmpessoa.services.test.resources.api.TestResource.class));
      assertFalse(result.contains(com.lmpessoa.services.test.resources.AbstractResource.class));
   }

   @Test
   public void testConfigMultipleEnvironments() {
      setup(CommonEnv.class);
      assertEquals("common", servicesResult);
      assertEquals("common", configResult);
   }

   @Test
   public void testConfigDefaultEnvironment() {
      setup(DefaultEnv.class);
      assertEquals("common", servicesResult);
      assertEquals("dev", configResult);
   }

   @Test
   public void testConfigSpecificEnvironment() {
      setup(SpecificEnv.class, "Staging");
      assertEquals("staging", servicesResult);
      assertEquals("staging", configResult);
   }

   @Test
   public void testLoggerCreation() {
      ApplicationServerInfo info = mock(ApplicationServerInfo.class);
      Map<String, String> settings = new HashMap<>();
      settings.put("type", TestLogWriter.class.getName());
      when(info.getProperties("logging.writer")).thenReturn(settings);

      Map<String, String> packages = new HashMap<>();
      packages.put("0.name", "com.lmpessoa.services.core.hosting");
      packages.put("0.level", "ERROR");
      when(info.getProperties("logging.packages")).thenReturn(packages);
      Logger log = ApplicationServer.createLogger(info, ApplicationSettingsTest.class);

      log.info("Test");
      log.join();
      assertNull(logResult);

      log.error("Test");
      log.join();
      assertEquals("[ERROR] com.lmpessoa.services.core.hosting.ApplicationSettingsTest: Test",
               logResult);
   }

   public static class CommonEnv {

      public static void configureServices(IServiceMap services, IHostEnvironment env) {
         servicesResult = "common";
      }

      public static void configure() {
         configResult = "common";
      }
   }

   public static class DefaultEnv extends CommonEnv {

      public static void configureDevelopment() {
         configResult = "dev";
      }
   }

   public static class SpecificEnv extends CommonEnv {

      public static void configureStagingServices(IServiceMap services) {
         servicesResult = "staging";
      }

      public static void configureStaging() {
         configResult = "staging";
      }
   }

   public static class TestLogWriter extends LogWriter {

      @Override
      protected void append(LogEntry entry) {
         logResult = entry.toString();
      }
   }
}