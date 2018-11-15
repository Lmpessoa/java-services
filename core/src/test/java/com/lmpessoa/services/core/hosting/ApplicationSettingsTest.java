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

import java.util.Collection;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.hosting.ApplicationConfig;
import com.lmpessoa.services.core.hosting.ApplicationContext;
import com.lmpessoa.services.core.hosting.ApplicationServlet;
import com.lmpessoa.services.core.hosting.IHostEnvironment;
import com.lmpessoa.services.core.routing.IRouteOptions;
import com.lmpessoa.services.core.services.IServiceMap;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.NullLogger;

public final class ApplicationSettingsTest {

   private static ILogger log = new NullLogger();
   private static String servicesResult;
   private static String configResult;

   private ApplicationContext context;
   private ApplicationConfig config;
   private ApplicationServlet app;

   @Before
   public void setup() {
      context = mock(ApplicationContext.class);
      when(context.getEnvironment()).thenReturn(() -> "Development");
      when(context.getLogger()).thenReturn(log);
      config = new ApplicationConfig(context, "test");
      app = new ApplicationServlet();
   }

   @Test
   public void testConfigMultipleEnvironments() throws ServletException {
      when(context.getAttribute("service.startup.class")).thenReturn(CommonEnv.class);
      app.init(config);
      assertEquals("common", servicesResult);
      assertEquals("common", configResult);
   }

   @Test
   public void testConfigDefaultEnvironment() throws ServletException {
      when(context.getAttribute("service.startup.class")).thenReturn(DefaultEnv.class);
      app.init(config);
      assertEquals("common", servicesResult);
      assertEquals("dev", configResult);
   }

   @Test
   public void testConfigSpecificEnvironment() throws ServletException {
      when(context.getEnvironment()).thenReturn(() -> "Staging");
      when(context.getAttribute("service.startup.class")).thenReturn(SpecificEnv.class);
      app.init(config);
      assertEquals("staging", servicesResult);
      assertEquals("staging", configResult);
   }

   @Test
   public void testScanDefault() throws ServletException {
      when(context.getAttribute("service.startup.class")).thenReturn(ApplicationInfoTest.class);
      app.init(config);
      Collection<Class<?>> result = app.getResources();
      assertTrue(result.contains(com.lmpessoa.services.test.resources.IndexResource.class));
      assertTrue(result.contains(com.lmpessoa.services.test.resources.TestResource.class));
      assertTrue(result.contains(com.lmpessoa.services.test.resources.api.TestResource.class));
      assertFalse(result.contains(com.lmpessoa.services.test.resources.AbstractResource.class));
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
