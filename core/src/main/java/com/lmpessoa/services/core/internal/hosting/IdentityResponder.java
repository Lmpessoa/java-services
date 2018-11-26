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

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.lmpessoa.services.core.ForbiddenException;
import com.lmpessoa.services.core.UnauthorizedException;
import com.lmpessoa.services.core.hosting.NextResponder;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.security.AllowAnonymous;
import com.lmpessoa.services.core.security.Authorize;
import com.lmpessoa.services.core.security.IIdentity;

final class IdentityResponder {

   private final ApplicationOptions options;
   private final NextResponder next;

   public IdentityResponder(NextResponder next, ApplicationOptions options) {
      this.options = options;
      this.next = next;
   }

   public Object invoke(RouteMatch route, IIdentity identity) {
      if (!(route instanceof HttpException)
               && !route.getMethod().isAnnotationPresent(AllowAnonymous.class)) {
         Authorize[] classAuth = getAuthorizations(route.getResourceClass());
         Authorize[] methodAuth = getAuthorizations(route.getMethod());
         if (classAuth.length > 0 || methodAuth.length > 0) {
            if (identity == null) {
               throw new UnauthorizedException();
            }
            isAuthorized(classAuth, identity);
            isAuthorized(methodAuth, identity);
         }
      }
      return next.invoke();
   }

   private Authorize[] getAuthorizations(AnnotatedElement element) {
      List<Authorize> result = new ArrayList<>();
      Authorize.List list = element.getAnnotation(Authorize.List.class);
      if (list != null) {
         result.addAll(Arrays.asList(list.value()));
      }
      Authorize auth = element.getAnnotation(Authorize.class);
      if (auth != null) {
         result.add(auth);
      }
      return result.toArray(new Authorize[0]);
   }

   private void isAuthorized(Authorize[] auths, IIdentity identity) {
      if (auths.length > 0) {
         for (Authorize auth : auths) {
            if (isAuthorized(auth, identity)) {
               return;
            }
         }
         throw new ForbiddenException();
      }
   }

   private boolean isAuthorized(Authorize auth, IIdentity identity) {
      for (String role : auth.roles()) {
         if (!identity.hasRole(role)) {
            return false;
         }
      }
      if (!"##default".equals(auth.policy())) {
         Predicate<IIdentity> policy = options.getPolicy(auth.policy());
         if (policy == null || !policy.test(identity)) {
            return false;
         }
      }
      return true;
   }
}
