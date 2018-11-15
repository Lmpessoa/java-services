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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.hosting.ContentType;
import com.lmpessoa.services.core.hosting.HttpInputStream;
import com.lmpessoa.services.core.hosting.HttpRequest;
import com.lmpessoa.services.core.hosting.IApplicationSettings;
import com.lmpessoa.services.core.hosting.MethodNotAllowedException;
import com.lmpessoa.services.core.hosting.NextResponder;
import com.lmpessoa.services.core.hosting.NotFoundException;
import com.lmpessoa.services.core.hosting.StaticResponder;
import com.lmpessoa.services.core.routing.RouteMatch;

public class StaticResponderTest {

   private IApplicationSettings app;
   private StaticResponder responder;

   static {
      StaticResponder.setStaticPath("/static");
   }

   @Before
   public void setup() {
      app = mock(IApplicationSettings.class);
      when(app.getStartupClass()).thenAnswer(inv -> StaticResponderTest.class);

      NextResponder next = mock(NextResponder.class);
      when(next.invoke()).thenReturn("Tested");
      responder = new StaticResponder(next);
   }

   @Test
   public void testGetMissingAll() {
      HttpRequest request = mockRequest("GET", "/missing.png");
      RouteMatch route = new NotFoundException();

      Object result = responder.invoke(app, request, route);
      assertEquals("Tested", result);
   }

   @Test
   public void testGetMissingRoute() throws IOException {
      HttpRequest request = mockRequest("GET", "/sample.png");
      RouteMatch route = new NotFoundException();

      Object result = responder.invoke(app, request, route);
      assertTrue(result instanceof HttpInputStream);
      try (HttpInputStream stream = (HttpInputStream) result) {
         assertEquals(ContentType.PNG, stream.getType());
      }
   }

   @Test
   public void testGetMissingStatic() {
      HttpRequest request = mockRequest("GET", "/missing.png");
      RouteMatch route = mock(RouteMatch.class);

      Object result = responder.invoke(app, request, route);
      assertEquals("Tested", result);
   }

   @Test
   public void testGetBothPresent() {
      HttpRequest request = mockRequest("GET", "/sample.png");
      RouteMatch route = mock(RouteMatch.class);

      Object result = responder.invoke(app, request, route);
      assertEquals("Tested", result);
   }

   @Test
   public void testPostMissingRoute() {
      HttpRequest request = mockRequest("POST", "/missing.png");
      RouteMatch route = new MethodNotAllowedException();

      Object result = responder.invoke(app, request, route);
      assertEquals("Tested", result);
   }

   @Test
   public void testFileWithCustomMimeType() throws IOException {
      HttpRequest request = mockRequest("GET", "/sample.random");
      RouteMatch route = new MethodNotAllowedException();

      Object result = responder.invoke(app, request, route);
      assertTrue(result instanceof HttpInputStream);
      try (HttpInputStream stream = (HttpInputStream) result) {
         assertEquals("random/test", stream.getType());
      }
   }

   private HttpRequest mockRequest(String method, String path) {
      HttpRequest result = mock(HttpRequest.class);
      when(result.getMethod()).thenReturn(method);
      when(result.getPath()).thenReturn(path);
      return result;
   }
}