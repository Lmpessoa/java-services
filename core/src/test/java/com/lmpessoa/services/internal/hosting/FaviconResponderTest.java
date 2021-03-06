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
package com.lmpessoa.services.internal.hosting;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.ContentType;
import com.lmpessoa.services.HttpInputStream;
import com.lmpessoa.services.NotFoundException;
import com.lmpessoa.services.Route;
import com.lmpessoa.services.hosting.ConnectionInfo;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.hosting.HttpResponse;
import com.lmpessoa.services.hosting.IApplicationInfo;
import com.lmpessoa.services.internal.logging.Logger;
import com.lmpessoa.services.internal.services.ServiceMap;
import com.lmpessoa.services.internal.validating.ValidationService;
import com.lmpessoa.services.logging.ILogger;
import com.lmpessoa.services.logging.NullHandler;
import com.lmpessoa.services.routing.RouteMatch;
import com.lmpessoa.services.validating.IValidationService;

public class FaviconResponderTest {

   private final Logger log = new Logger(new NullHandler());
   private final ConnectionInfo connect;
   private ApplicationOptions app;
   private ServiceMap services;

   private HttpRequest request;
   private RouteMatch route;

   public FaviconResponderTest() {
      Socket socket = mock(Socket.class);
      connect = new ConnectionInfo(socket, "https://lmpessoa.com/");
   }

   @Before
   public void setup() throws IOException {
      ApplicationSettings settings = new ApplicationSettings(FaviconResponderTest.class, null,
               null);
      app = new ApplicationOptions(null);
      services = app.getServices();
      services.put(ILogger.class, log);
      services.putSupplier(ConnectionInfo.class, () -> connect);
      services.putSupplier(RouteMatch.class, () -> route);
      services.put(IValidationService.class, ValidationService.instance());
      services.put(IApplicationInfo.class, new ApplicationInfo(settings, app));
      request = new HttpRequestBuilder().setPath("/favicon.ico").build();
      services.putSupplier(HttpRequest.class, () -> request);
   }

   @Test
   public void testDefaultFavicon() throws IOException {
      route = new NotFoundException();
      HttpResponse result = (HttpResponse) app.getFirstResponder().invoke();
      assertContent(result.getContentBody(),
               FaviconResponder.class.getResourceAsStream("/favicon.ico"));
   }

   @Test
   public void testStaticFavicon() throws IOException {
      app.useStaticFiles();
      route = new NotFoundException();
      HttpResponse result = (HttpResponse) app.getFirstResponder().invoke();
      assertContent(result.getContentBody(),
               FaviconResponderTest.class.getResourceAsStream("/static/favicon.ico"));
   }

   @Test
   public void testResourceFavicon() throws IOException {
      route = () -> new TestResource().get(false);
      HttpResponse result = (HttpResponse) app.getFirstResponder().invoke();
      assertContent(result.getContentBody(),
               FaviconResponderTest.class.getResourceAsStream("/http/resource-favicon.ico"));
   }

   @Test
   public void testResourceFaviconMissing() throws IOException {
      app.useStaticFiles();
      route = () -> new TestResource().get(true);
      HttpResponse result = (HttpResponse) app.getFirstResponder().invoke();
      assertContent(result.getContentBody(),
               FaviconResponder.class.getResourceAsStream("/favicon.ico"));
   }

   private void assertContent(HttpInputStream result, InputStream content) throws IOException {
      assertNotNull(result);
      assertEquals(ContentType.ICO, result.getType());
      byte[] inputBytes = readAllBytes(result);
      byte[] contentBytes = readAllBytes(content);
      assertArrayEquals(contentBytes, inputBytes);
   }

   private byte[] readAllBytes(InputStream is) throws IOException {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int read;
      while ((read = is.read(buffer)) > 0) {
         result.write(buffer, 0, read);
      }
      return result.toByteArray();
   }

   public static class TestResource {

      @Route("favicon.ico")
      public HttpInputStream get(boolean throwing) {
         if (throwing) {
            throw new NotFoundException();
         }
         return new HttpInputStream(
                  FaviconResponderTest.class.getResourceAsStream("/http/resource-favicon.ico"),
                  ContentType.ICO);
      }
   }
}
