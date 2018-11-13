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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.hosting.Application;
import com.lmpessoa.services.routing.IRouteOptions;

public final class AutoLoaderTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private Application app;

   @Test
   public void testScanDefault() throws IOException, IllegalAccessException, NoSuchMethodException {
      app = new Application(AutoLoaderTest.class, new String[0]);
      ApplicationBridge.useNullLogWriter(app);
      Collection<Class<?>> result = app.scanResourcesFromStartup();
      assertTrue(result.contains(com.lmpessoa.services.test.resources.IndexResource.class));
      assertTrue(result.contains(com.lmpessoa.services.test.resources.TestResource.class));
      assertFalse(result.contains(com.lmpessoa.services.test.resources.AbstractResource.class));
      assertFalse(result.contains(com.lmpessoa.services.test.resources.api.TestResource.class));
   }

   @Test
   public void testScanUserDefined() throws IOException, IllegalAccessException, NoSuchMethodException {
      app = new Application(MainWithApi.class, new String[0]);
      ApplicationBridge.useNullLogWriter(app);
      app.doConfigure();
      Collection<Class<?>> result = app.scanResourcesFromStartup();
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
}
