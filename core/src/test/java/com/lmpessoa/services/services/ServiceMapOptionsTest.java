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
package com.lmpessoa.services.services;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.hosting.InternalServerError;
import com.lmpessoa.services.services.ConfiguresWith;
import com.lmpessoa.services.services.IServiceMap;
import com.lmpessoa.services.services.ServiceMap;

public final class ServiceMapOptionsTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private IServiceMap serviceMap;

   @Before
   public void setup() {
      serviceMap = new ServiceMap();
   }

   @Test
   public void testValidOptions() {
      serviceMap.putSingleton(ITestService.class, TestService.class);
      ServiceMap configMap = (ServiceMap) serviceMap.getConfigMap();
      assertTrue(configMap.contains(ITestOptions.class));
      ITestOptions options = configMap.get(ITestOptions.class);
      assertNotNull(options);
   }

   @Test
   public void testWrongOptions() {
      thrown.expect(ClassCastException.class);
      serviceMap.putSingleton(ITestService.class, WrongOptionsService.class);
      ServiceMap configMap = (ServiceMap) serviceMap.getConfigMap();
      assertTrue(configMap.contains(ITestOptions.class));
      configMap.get(ITestOptions.class);
   }

   @Test
   public void testNoOptions() throws Throwable {
      thrown.expect(NoSuchMethodException.class);
      try {
         serviceMap.putSingleton(ITestService.class, NoOptionsService.class);
         ServiceMap configMap = (ServiceMap) serviceMap.getConfigMap();
         assertTrue(configMap.contains(ITestOptions.class));
         configMap.get(ITestOptions.class);
      } catch (InternalServerError e) {
         throw e.getCause();
      }
   }

   public static interface ITestOptions {
      // Test type, has nothing
   }

   public static class TestOptions implements ITestOptions {
      // Test type, has nothing
   }

   @ConfiguresWith(ITestOptions.class)
   public static interface ITestService {
      // Test type, has nothing
   }

   public static class TestService implements ITestService {

      public ITestOptions getOptions() {
         return new TestOptions();
      }
   }

   public static class WrongOptionsService implements ITestService {

      public ITestService getOptions() {
         return this;
      }
   }

   public static class NoOptionsService implements ITestService {
      // Test method, has nothing
   }
}
