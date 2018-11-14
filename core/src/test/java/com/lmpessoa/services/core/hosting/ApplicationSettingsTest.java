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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.hosting.Application;
import com.lmpessoa.services.core.hosting.ApplicationServer;
import com.lmpessoa.services.core.hosting.IHostEnvironment;
import com.lmpessoa.services.core.routing.IRouteOptions;
import com.lmpessoa.services.core.services.IServiceMap;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.NullLogger;

public final class ApplicationSettingsTest {

   private static ILogger log = new NullLogger();
   private static String servicesResult;
   private static String configResult;

   private ApplicationServer server;
   private Application app;

   @Before
   public void setup() {
      server = mock(ApplicationServer.class);
      when(server.getResourceClasses()).thenCallRealMethod();
      when(server.getStartupClass()).then(o -> ApplicationSettingsTest.class);
      when(server.getEnvironment()).thenReturn(() -> "Development");
      when(server.getLogger()).thenReturn(log);
      servicesResult = configResult = null;
   }

   @Test
   public void testConfigMultipleEnvironments() throws NoSuchMethodException, IllegalAccessException, IOException {
      new Application(server, CommonEnv.class).getResources();
      assertEquals("common", servicesResult);
      assertEquals("common", configResult);
   }

   @Test
   public void testConfigDefaultEnvironment() throws NoSuchMethodException, IllegalAccessException, IOException {
      new Application(server, DefaultEnv.class).getResources();
      assertEquals("common", servicesResult);
      assertEquals("dev", configResult);
   }

   @Test
   public void testConfigSpecificEnvironment() throws NoSuchMethodException, IllegalAccessException, IOException {
      when(server.getEnvironment()).thenReturn(() -> "Staging");
      new Application(server, SpecificEnv.class).getResources();
      assertEquals("staging", servicesResult);
      assertEquals("staging", configResult);
   }

   @Test
   public void testScanDefault() throws IOException, IllegalAccessException, NoSuchMethodException {
      app = new Application(server, ApplicationInfoTest.class);
      Collection<Class<?>> result = app.getResources();
      assertTrue(result.contains(com.lmpessoa.services.test.resources.IndexResource.class));
      assertTrue(result.contains(com.lmpessoa.services.test.resources.TestResource.class));
      assertFalse(result.contains(com.lmpessoa.services.test.resources.AbstractResource.class));
      assertFalse(result.contains(com.lmpessoa.services.test.resources.api.TestResource.class));
   }

   @Test
   public void testScanUserDefined() throws IOException, IllegalAccessException, NoSuchMethodException {
      app = new Application(server, MainWithApi.class);
      Collection<Class<?>> result = app.getResources();
      assertTrue(result.contains(com.lmpessoa.services.test.resources.IndexResource.class));
      assertTrue(result.contains(com.lmpessoa.services.test.resources.TestResource.class));
      assertFalse(result.contains(com.lmpessoa.services.test.resources.AbstractResource.class));
      assertTrue(result.contains(com.lmpessoa.services.test.resources.api.TestResource.class));
   }

   public static class MainWithApi {

      public static void configure(IRouteOptions routes) {
         routes.addArea("api/v1", ".resources.api$");
      }
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
}
