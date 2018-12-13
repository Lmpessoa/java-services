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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.HttpInputStream;
import com.lmpessoa.services.Redirect;
import com.lmpessoa.services.Route;
import com.lmpessoa.services.UnauthorizedException;
import com.lmpessoa.services.hosting.ConnectionInfo;
import com.lmpessoa.services.hosting.Headers;
import com.lmpessoa.services.hosting.HttpRequestBuilder;
import com.lmpessoa.services.internal.ClassUtils;
import com.lmpessoa.services.internal.hosting.RedirectImpl;
import com.lmpessoa.services.internal.logging.Logger;
import com.lmpessoa.services.internal.routing.RouteTable;
import com.lmpessoa.services.internal.services.ServiceMap;
import com.lmpessoa.services.logging.ILogger;
import com.lmpessoa.services.logging.NullHandler;
import com.lmpessoa.services.routing.HttpMethod;
import com.lmpessoa.services.routing.RouteMatch;
import com.lmpessoa.services.security.Authorize;
import com.lmpessoa.services.security.IIdentity;
import com.lmpessoa.services.security.ITokenManager;
import com.lmpessoa.services.security.IdentityBuilder;

public class LoginViewResponderTest {

   private static final IIdentity IDENTITY = new IdentityBuilder().build();
   private static final Method METHOD = getAuthMethod();

   private static Method getAuthMethod() {
      return ClassUtils.getMethod(LoginViewResponderTest.class, "auth");
   }

   private ILogger log = new Logger(new NullHandler());
   private ViewResponder responder;
   private HttpRequestBuilder request;
   private ConnectionInfo connection;
   private ITokenManager tokens;
   private IIdentity identity;
   private RouteMatch route;

   @Before
   public void setup() {
      identity = null;
      Socket socket = mock(Socket.class);
      connection = new ConnectionInfo(socket, "https://lmpessoa.com");
      tokens = new TestTokenManager();

      request = new HttpRequestBuilder();
      route = mock(RouteMatch.class);

      ServiceMap services = new ServiceMap();
      RouteTable routes = new RouteTable(services);
      routes.put("", TestLoginResource.class);
      RedirectImpl.setRoutes(routes);

      ViewResponder.useEngine("txt", new TestRenderizationEngine());
      ViewResponder.useLogin(TestLoginResource.class);
   }

   @Test(expected = UnauthorizedException.class)
   public void testUnauthenticatedClient() throws IOException {
      when(route.getResourceClass()).then(i -> LoginViewResponderTest.class);
      when(route.getMethod()).thenReturn(METHOD);
      request.setPath("/auth");
      responder = new ViewResponder(() -> {
         throw new UnauthorizedException();
      });

      responder.invoke(request.build(), route, identity, connection, tokens, log);
   }

   @Test
   public void testUnauthenticatedBrowser() throws IOException {
      when(route.getResourceClass()).then(i -> LoginViewResponderTest.class);
      when(route.getMethod()).thenReturn(METHOD);
      request.setPath("/auth");
      request.addHeader(Headers.ACCEPT, "text/html; */*");
      responder = new ViewResponder(() -> {
         throw new UnauthorizedException();
      });

      Object result = responder.invoke(request.build(), route, identity, connection, tokens, log);
      assertTrue(result instanceof Redirect);
      Redirect redirect = (Redirect) result;
      assertEquals(302, redirect.getStatusCode());
      redirect.setConnectionInfo(connection);
      String location = redirect.getHeaders().get(Headers.LOCATION);
      assertEquals("https://lmpessoa.com/login?returnUrl=%2Fauth", location);
   }

   @Test
   public void testLoginGetClean() throws IOException {
      when(route.getResourceClass()).then(i -> TestLoginResource.class);
      when(route.getMethod())
               .thenReturn(ClassUtils.getMethod(TestLoginResource.class, "get", String.class));
      request.setPath("/login?returnUrl=%2Fauth");
      request.addHeader(Headers.ACCEPT, "text/html; */*");
      final ViewAndModel login = new ViewAndModel("login");
      login.set("returnUrl", "/auth");
      responder = new ViewResponder(() -> login);

      Object result = responder.invoke(request.build(), route, identity, connection, tokens, log);
      assertTrue(result instanceof HttpInputStream);
      String content = readInputStream((HttpInputStream) result);
      assertEquals("Return URL: /auth\nMessage: \nUsername: ", content);
   }

   @Test
   public void testLoginGetAuthenticated() throws IOException {
      identity = IDENTITY;
      when(route.getResourceClass()).then(i -> TestLoginResource.class);
      when(route.getMethod())
               .thenReturn(ClassUtils.getMethod(TestLoginResource.class, "get", String.class));
      request.setPath("/login?returnUrl=%2Fauth");
      request.addHeader(Headers.ACCEPT, "text/html; */*");
      final ViewAndModel login = new ViewAndModel("login");
      login.set("returnUrl", "/auth");
      responder = new ViewResponder(() -> login);

      Object result = responder.invoke(request.build(), route, identity, connection, tokens, log);
      assertTrue(result instanceof Redirect);
      Redirect redirect = (Redirect) result;
      assertEquals(302, redirect.getStatusCode());
      redirect.setConnectionInfo(connection);
      String location = redirect.getHeaders().get(Headers.LOCATION);
      assertEquals("https://lmpessoa.com/auth", location);
   }

   @Test
   public void testLoginPostCorrect() throws IOException {
      when(route.getResourceClass()).then(i -> TestLoginResource.class);
      when(route.getMethod()).thenReturn(
               ClassUtils.getMethod(TestLoginResource.class, "post", UserPasswordRequest.class));
      request.setPath("/login?returnUrl=%2Fauth");
      request.setMethod(HttpMethod.POST);
      request.addHeader(Headers.ACCEPT, "text/html; */*");
      responder = new ViewResponder(() -> IDENTITY);

      Object result = responder.invoke(request.build(), route, identity, connection, tokens, log);
      assertTrue(result instanceof ViewRedirect);
      ViewRedirect redirect = (ViewRedirect) result;
      assertEquals(302, redirect.getStatusCode());
      redirect.setConnectionInfo(connection);
      String location = redirect.getHeaders().get(Headers.LOCATION);
      assertEquals("https://lmpessoa.com/auth", location);
      String cookie = redirect.getHeaders().get(Headers.SET_COOKIE);
      assertEquals("__user_session=<token>; Same-Site=Strict", cookie);
   }

   @Test
   public void testLoginPostFailed() throws IOException {
      when(route.getResourceClass()).then(i -> TestLoginResource.class);
      when(route.getMethod()).thenReturn(
               ClassUtils.getMethod(TestLoginResource.class, "post", UserPasswordRequest.class));
      when(route.getContentObject()).thenReturn(new UserPasswordRequest() {

         @Override
         public String getUsername() {
            return "test";
         }
      });
      request.setPath("/login?returnUrl=%2Fauth");
      request.setMethod(HttpMethod.POST);
      request.addHeader(Headers.ACCEPT, "text/html; */*");
      responder = new ViewResponder(() -> {
         throw new FailedLoginException();
      });

      Object result = responder.invoke(request.build(), route, identity, connection, tokens, log);
      assertTrue(result instanceof HttpInputStream);
      String content = readInputStream((HttpInputStream) result);
      assertEquals("Return URL: /auth\nMessage: Invalid user name or password\nUsername: test",
               content);
   }

   @Test
   public void testLoginPostAuthenticated() throws IOException {
      identity = IDENTITY;
      when(route.getResourceClass()).then(i -> TestLoginResource.class);
      when(route.getMethod()).thenReturn(
               ClassUtils.getMethod(TestLoginResource.class, "post", UserPasswordRequest.class));
      request.setPath("/login?returnUrl=%2Fauth");
      request.setMethod(HttpMethod.POST);
      request.addHeader(Headers.ACCEPT, "text/html; */*");
      responder = new ViewResponder(() -> null);

      Object result = responder.invoke(request.build(), route, identity, connection, tokens, log);
      assertTrue(result instanceof Redirect);
      Redirect redirect = (Redirect) result;
      assertEquals(302, redirect.getStatusCode());
      redirect.setConnectionInfo(connection);
      String location = redirect.getHeaders().get(Headers.LOCATION);
      assertEquals("https://lmpessoa.com/auth", location);
   }

   @Authorize
   public String auth() {
      return "Success!";
   }

   private String readInputStream(InputStream input) {
      if (input != null) {
         try (InputStream is = input) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) > 0) {
               output.write(buffer, 0, len);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      return null;
   }

   public static class TestTokenManager implements IViewTokenManager {

      @Override
      public IIdentity get(String token) {
         if ("<token>".equals(token)) {
            return IDENTITY;
         }
         return null;
      }

      @Override
      public String add(IIdentity identity, Duration expiration) {
         if (identity == IDENTITY) {
            return "<token>";
         }
         return null;
      }
   }

   @Route("login")
   public static class TestLoginResource implements ILoginResource {

      @Override
      public IIdentity post(UserPasswordRequest data) {
         if ("user".equals(data.getUsername()) && "pass".equals(data.getPassword())) {
            return IDENTITY;
         }
         throw new FailedLoginException();
      }
   }
}
