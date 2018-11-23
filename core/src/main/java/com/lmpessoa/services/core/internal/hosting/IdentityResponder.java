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
package com.lmpessoa.services.core.internal.hosting;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import com.lmpessoa.services.core.hosting.ForbiddenException;
import com.lmpessoa.services.core.hosting.NextResponder;
import com.lmpessoa.services.core.hosting.UnauthorizedException;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.security.AllowAnonymous;
import com.lmpessoa.services.core.security.Authorize;
import com.lmpessoa.services.core.security.IIdentity;

final class IdentityResponder {

   private static Map<String, Predicate<IIdentity>> policies = new HashMap<>();
   private final NextResponder next;

   public IdentityResponder(NextResponder next) {
      this.next = next;
   }

   public Object invoke(RouteMatch route, IIdentity identity) {
      if (!(route instanceof HttpException)) {
         AllowAnonymous anon = route.getMethod().getAnnotation(AllowAnonymous.class);
         if (anon == null) {
            anon = route.getResourceClass().getAnnotation(AllowAnonymous.class);
         }
         if (anon == null) {
            Authorize methodAuth = route.getMethod().getAnnotation(Authorize.class);
            Authorize classAuth = route.getResourceClass().getAnnotation(Authorize.class);
            if (methodAuth != null || classAuth != null) {
               if (identity == null) {
                  throw new UnauthorizedException();
               }
               isAuthorized(classAuth, identity);
               isAuthorized(methodAuth, identity);
            }
         }
      }
      return next.invoke();
   }

   static void addPolicy(String policyName, Predicate<IIdentity> policyRule) {
      policies.put(policyName, policyRule);
   }

   static boolean hasPolicy(String policyName) {
      return policies.containsKey(policyName);
   }

   private void isAuthorized(Authorize auth, IIdentity identity) {
      if (auth != null) {
         for (String role : auth.roles()) {
            if (!identity.hasRole(role)) {
               throw new ForbiddenException(String.format("User failed role: %s", role));
            }
         }
         if (!auth.policy().isEmpty()) {
            Predicate<IIdentity> policy = policies.get(auth.policy());
            if (policy == null || !policy.test(identity)) {
               throw new ForbiddenException("User failed policy: " + auth.policy());
            }
         }
      }
   }
}
