/*
 * Copyright (c) 2018 Leonardo Pessoa
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
import static org.mockito.Mockito.mock;

import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.ConnectionInfo;
import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.Logger;
import com.lmpessoa.services.util.logging.NullLogWriter;

public final class RedirectTest {

   private ILogger log = new Logger(RedirectTest.class, new NullLogWriter());

   private ConnectionInfo connect;
   private ServiceMap services;
   private RouteTable routes;

   @Before
   public void setup() {
      Socket socket = mock(Socket.class);
      connect = new ConnectionInfo(socket, "https://lmpessoa.com/");

      services = new ServiceMap();

      routes = new RouteTable(services, log);
      Redirect.setRoutes(routes);
   }

   @Test
   public void testRedirectString() throws MalformedURLException {
      Redirect redirect = Redirect.to("/test/7");
      URL result = redirect.getUrl(connect);
      assertEquals("https://lmpessoa.com/test/7", result.toExternalForm());
   }

   @Test
   public void testRedirectMethod() throws MalformedURLException {
      Class<?> testClass = com.lmpessoa.services.test.resources.TestResource.class;
      routes.put("", testClass);
      Redirect redirect = Redirect.to(testClass, "get");
      URL result = redirect.getUrl(connect);
      assertEquals("https://lmpessoa.com/test", result.toExternalForm());
   }

   @Test
   public void testRedirectMethodWithArgs() throws MalformedURLException {
      Class<?> testClass = com.lmpessoa.services.core.routing.RouteTableTest.TestResource.class;
      routes.put("", testClass);
      Redirect redirect = Redirect.to(testClass, "patch", 7);
      URL result = redirect.getUrl(connect);
      assertEquals("https://lmpessoa.com/test/7", result.toExternalForm());
   }

   @Test
   public void testRedirectMethodWithRoute() throws MalformedURLException {
      Class<?> testClass = com.lmpessoa.services.core.hosting.NextHandlerFullTest.TestResource.class;
      routes.put("", testClass);
      Redirect redirect = Redirect.to(testClass, "empty");
      URL result = redirect.getUrl(connect);
      assertEquals("https://lmpessoa.com/test/empty", result.toExternalForm());
   }
}