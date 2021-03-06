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
package com.lmpessoa.services.views;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lmpessoa.services.ContentType;
import com.lmpessoa.services.HttpInputStream;
import com.lmpessoa.services.NotFoundException;
import com.lmpessoa.services.hosting.ConnectionInfo;
import com.lmpessoa.services.hosting.Headers;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.hosting.ValuesMap;
import com.lmpessoa.services.routing.HttpMethod;
import com.lmpessoa.services.routing.RouteMatch;

public class ViewResponderTest {

   private static ConnectionInfo connection;

   private ViewResponder responder;
   private HttpRequest request;
   private ValuesMap headers;
   private RouteMatch route;

   @BeforeClass
   public static void setupClass() {
      ViewResponder.useEngine("txt", new TestRenderizationEngine());
      Socket socket = mock(Socket.class);
      connection = new ConnectionInfo(socket, "https://lmpessoa.com");
   }

   @Before
   public void setup() throws NoSuchMethodException {
      headers = mock(ValuesMap.class);
      when(headers.get(Headers.ACCEPT)).thenReturn("text/html; */*");

      request = mock(HttpRequest.class);
      when(request.getMethod()).thenReturn(HttpMethod.GET);
      when(request.getHeaders()).thenReturn(headers);
      when(request.getForm()).thenReturn(ValuesMap.empty());
      when(request.getQuery()).thenReturn(ValuesMap.empty());

      route = mock(RouteMatch.class);
      when(route.getMethod()).thenReturn(ViewResponderTest.class.getMethod("doTest", int.class));
   }

   @Test
   public void testNoRenderization() throws IOException, NoSuchMethodException {
      when(headers.get(Headers.ACCEPT)).thenReturn(null);
      responder = new ViewResponder(() -> doTest(0));
      Object result = responder.invoke(request, route, null, connection, null, null);
      assertTrue(result instanceof String);
      assertEquals("the method #doTest()", result);
   }

   @Test
   public void testRenderization() throws IOException, NoSuchMethodException {
      responder = new ViewResponder(() -> doTest(0));
      Object result = responder.invoke(request, route, null, connection, null, null);
      assertHttpInputStream("This is a test template file from the method #doTest()", result);
   }

   @Test
   public void testViewAndModelRenderization() throws IOException, NoSuchMethodException {
      responder = new ViewResponder(() -> doTest(1));
      Object result = responder.invoke(request, route, null, connection, null, null);
      assertHttpInputStream("This is a test template file from the method #doTest()", result);
   }

   @Test(expected = NotFoundException.class)
   public void test404NotRendered() throws IOException {
      when(headers.get(Headers.ACCEPT)).thenReturn(null);
      route = new NotFoundException();
      responder = new ViewResponder(NotFoundException::new);
      responder.invoke(request, route, null, connection, null, null);
   }

   @Test
   public void test404Rendered() throws IOException {
      route = new NotFoundException();
      responder = new ViewResponder(NotFoundException::new);
      Object result = responder.invoke(request, route, null, connection, null, null);
      assertHttpInputStream("The file was not found", result);
   }

   @Test
   public void testOverrideViewAnnotation() throws IOException {
      responder = new ViewResponder(() -> doTest(2));
      Object result = responder.invoke(request, route, null, connection, null, null);
      assertHttpInputStream("The file was not found", result);
   }

   @View("template")
   public Object doTest(int scenario) {
      switch (scenario) {
         case 0:
            return "the method #doTest()";
         case 1:
            return new ViewAndModel("template", "the method #doTest()");
         case 2:
            return new ViewAndModel("404", "from the method #doTest()");
      }
      return null;
   }

   private void assertHttpInputStream(String value, Object obj) throws IOException {
      assertTrue(obj instanceof HttpInputStream);
      try (HttpInputStream his = (HttpInputStream) obj) {
         assertEquals(ContentType.HTML, his.getType());
         byte[] b = new byte[1024];
         his.read(b);
         String hist = new String(b, StandardCharsets.UTF_8).trim();
         assertEquals(value, hist);
      }
   }
}
