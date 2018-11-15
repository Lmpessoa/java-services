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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.core.hosting.ForbiddenException;
import com.lmpessoa.services.core.hosting.IdentityResponder;
import com.lmpessoa.services.core.hosting.NextResponder;
import com.lmpessoa.services.core.hosting.NextResponderImpl;
import com.lmpessoa.services.core.hosting.UnauthorizedException;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.security.AllowAnonymous;
import com.lmpessoa.services.core.security.Authorize;
import com.lmpessoa.services.core.security.Claim;
import com.lmpessoa.services.core.security.ClaimType;
import com.lmpessoa.services.core.security.GenericIdentity;
import com.lmpessoa.services.core.security.IIdentity;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.ClassUtils;

public class IdentityResponderTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private static IIdentity identity;

   private IdentityResponder responder;
   private ServiceMap services;
   private NextResponder next;
   private RouteMatch match;

   static {
      Collection<Claim> claims = new ArrayList<>();
      claims.add(new Claim(ClaimType.ROLE, "foo"));
      claims.add(new Claim(ClaimType.ROLE, "bar"));
      identity = new GenericIdentity(claims);
   }

   @Before
   public void setup() {
      services = new ServiceMap();
      services.put(IIdentity.class, (Supplier<IIdentity>) () -> identity);
      next = new NextResponderImpl(services, Arrays.asList(TestResponder.class));
      responder = new IdentityResponder(next);
   }

   @Test
   public void testNonMarkedMethod() {
      setMatched(NonMarkedType.class, "nonMarked");
      Object result = responder.invoke(match, identity);
      assertEquals("Tested", result);
   }

   @Test
   public void testNonMarkedAnonymous() {
      setMatched(NonMarkedType.class, "nonMarked");
      Object result = responder.invoke(match, null);
      assertEquals("Tested", result);
   }

   @Test
   public void testMarkedMethod() {
      setMatched(NonMarkedType.class, "marked");
      Object result = responder.invoke(match, identity);
      assertEquals("Tested", result);
   }

   @Test
   public void testMarkedAnonymous() {
      thrown.expect(UnauthorizedException.class);
      setMatched(NonMarkedType.class, "marked");
      responder.invoke(match, null);
   }

   @Test
   public void testMarkedWithRole() {
      setMatched(NonMarkedType.class, "markedWithRole");
      Object result = responder.invoke(match, identity);
      assertEquals("Tested", result);
   }

   @Test
   public void testMarkedWithoutRole() {
      thrown.expect(ForbiddenException.class);
      setMatched(NonMarkedType.class, "markedWithMissingRole");
      responder.invoke(match, identity);
   }

   @Test
   public void testMarkedWithPolicy() {
      setMatched(NonMarkedType.class, "markedWithPolicy");
      ((GenericIdentity) identity).addClaim(ClaimType.DISPLAY_NAME, "Jane Doe");
      IdentityResponder.addPolicy("named", IdentityResponderTest::testPolicy);
      Object result = responder.invoke(match, identity);
      assertEquals("Tested", result);
   }

   @Test
   public void testMarkedWithoutPolicy() {
      thrown.expect(ForbiddenException.class);
      setMatched(NonMarkedType.class, "markedWithPolicy");
      IdentityResponder.addPolicy("named", IdentityResponderTest::testPolicy);
      responder.invoke(match, identity);
   }

   @Test
   public void testMarkedClassNonMarkedMethod() {
      setMatched(MarkedType.class, "nonMarked");
      Object result = responder.invoke(match, identity);
      assertEquals("Tested", result);
   }

   @Test
   public void testMarkedClassAnonymous() {
      thrown.expect(UnauthorizedException.class);
      setMatched(MarkedType.class, "nonMarked");
      responder.invoke(match, null);
   }

   @Test
   public void testMarkedClassDualRoles() {
      setMatched(MarkedType.class, "markedWithRole");
      Object result = responder.invoke(match, identity);
      assertEquals("Tested", result);
   }

   @Test
   public void testMarkedClassMissingSecondRole() {
      thrown.expect(ForbiddenException.class);
      setMatched(MarkedType.class, "markedWithMissingRole");
      responder.invoke(match, identity);
   }

   @Test
   public void testMarkedClassAllowAnonymous() {
      setMatched(MarkedType.class, "markedAnonymous");
      Object result = responder.invoke(match, null);
      assertEquals("Tested", result);
   }

   @Test
   public void testAnonymousWithMarkedMethod() {
      setMatched(AnonymousType.class, "marked");
      Object result = responder.invoke(match, null);
      assertEquals("Tested", result);
   }

   @Test
   public void testAnonymousWithMarkedMethodAndIdentity() {
      setMatched(AnonymousType.class, "marked");
      Object result = responder.invoke(match, identity);
      assertEquals("Tested", result);
   }

   private static boolean testPolicy(IIdentity identity) {
      return identity.hasClaim(ClaimType.DISPLAY_NAME);
   }

   private void setMatched(Class<?> clazz, String methodName) {
      final Method method = ClassUtils.getMethod(clazz, methodName);
      match = new RouteMatch() {

         @Override
         public Class<?> getResourceClass() {
            return clazz;
         }

         @Override
         public Method getMethod() {
            return method;
         }

         @Override
         public Object invoke() {
            return "Tested";
         }
      };
      services.put(RouteMatch.class, (Supplier<RouteMatch>) () -> match);
   }

   public static class NonMarkedType {

      public void nonMarked() {
      }

      @Authorize
      public void marked() {
      }

      @Authorize(roles = "foo")
      public void markedWithRole() {
      }

      @Authorize(roles = "baz")
      public void markedWithMissingRole() {
      }

      @Authorize(policy = "named")
      public void markedWithPolicy() {
      }
   }

   @Authorize(roles = "foo")
   public static class MarkedType {

      public void nonMarked() {
      }

      @Authorize(roles = "bar")
      public void markedWithRole() {
      }

      @Authorize(roles = "baz")
      public void markedWithMissingRole() {
      }

      @AllowAnonymous
      public void markedAnonymous() {
      }
   }

   @AllowAnonymous
   public static class AnonymousType {

      @Authorize(roles = "baz")
      public void marked() {
      }
   }

   public static class TestResponder {

      public TestResponder(NextResponder next) {
         // Ignore
      }

      public Object invoke(RouteMatch route) {
         return route.invoke();
      }
   }
}
