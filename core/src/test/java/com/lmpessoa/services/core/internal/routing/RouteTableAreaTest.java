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
package com.lmpessoa.services.core.internal.routing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.internal.services.ServiceMap;

public final class RouteTableAreaTest {

   private ServiceMap services;
   private RouteTable table;

   @Before
   public void setup() {
      services = new ServiceMap();
      table = new RouteTable(services);
   }

   @Test
   public void testDefaultResource() {
      table.put(com.lmpessoa.services.test.resources.IndexResource.class);
      assertTrue(table.hasRoute("/"));
      assertTrue(table.hasRoute("/test"));
   }

   @Test
   public void testDefaultArea() {
      table.put("", com.lmpessoa.services.test.resources.api.TestResource.class);
      assertTrue(table.hasRoute("/test"));
      assertFalse(table.hasRoute("/api/test"));
   }

   @Test
   public void testApiArea() {
      table.getOptions().addArea("api/v1", ".resources.api$");
      table.put(com.lmpessoa.services.test.resources.api.TestResource.class);
      assertTrue(table.hasRoute("/api/v1/test"));
      assertFalse(table.hasRoute("/test"));
   }

   @Test
   public void testMultipleResourcesInArea() throws ParseException {
      table.getOptions().addArea("api/v1", ".resources.api$");
      table.putAll(Arrays.asList( //
               com.lmpessoa.services.test.resources.api.TestResource.class, //
               com.lmpessoa.services.test.resources.TestResource.class, //
               com.lmpessoa.services.test.resources.AbstractResource.class));
      assertTrue(table.hasRoute("/api/v1/test"));
      assertTrue(table.hasRoute("/test"));
      assertFalse(table.hasRoute("/abstract"));
   }

   @Test
   public void testApiAreaOverride() {
      table.getOptions().addArea("api/v1", "\\.resources\\.api$");
      table.put("api/v2", com.lmpessoa.services.test.resources.api.TestResource.class);
      assertFalse(table.hasRoute("/api/v1/test"));
      assertTrue(table.hasRoute("/api/v2/test"));
   }
}
