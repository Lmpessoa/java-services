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
package com.lmpessoa.services.hosting;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.hosting.Application;
import com.lmpessoa.services.hosting.IHostEnvironment;
import com.lmpessoa.services.services.IServiceMap;

public final class ApplicationConfigurationTest {

   private static String servicesResult;
   private static String configResult;

   private Application app;

   @Before
   public void setup() {
      servicesResult = configResult = null;
   }

   @Test
   public void testConfigMultipleEnvironments() throws NoSuchMethodException, IllegalAccessException, IOException {
      app = new Application(CommonEnv.class, new String[0]);
      app.doConfigure();
      assertEquals("common", servicesResult);
      assertEquals("common", configResult);
   }

   @Test
   public void testConfigDefaultEnvironment() throws NoSuchMethodException, IllegalAccessException, IOException {
      app = new Application(DefaultEnv.class, new String[0]);
      app.doConfigure();
      assertEquals("common", servicesResult);
      assertEquals("dev", configResult);
   }

   @Test
   public void testConfigSpecificEnvironment() throws NoSuchMethodException, IllegalAccessException, IOException {
      app = new Application(SpecificEnv.class, new String[] { "-e", "Staging" });
      app.doConfigure();
      assertEquals("staging", servicesResult);
      assertEquals("staging", configResult);
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
