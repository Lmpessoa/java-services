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
package com.lmpessoa.services.core.internal.concurrent;

import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.lmpessoa.services.core.concurrent.AsyncReject;
import com.lmpessoa.services.core.concurrent.AsyncRequest;
import com.lmpessoa.services.core.concurrent.IAsyncRequestMatcher;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.security.IIdentity;

public final class RejectRequestMatcher implements IAsyncRequestMatcher {

   private final AsyncReject rule;

   public RejectRequestMatcher(AsyncReject rule) {
      this.rule = rule;
   }

   @Override
   public UUID match(AsyncRequest request, Map<UUID, AsyncRequest> queued) {
      if (rule != AsyncReject.NEVER) {
         for (Entry<UUID, AsyncRequest> entry : queued.entrySet()) {
            RouteMatch r = entry.getValue().getRoute();
            RouteMatch route = request.getRoute();
            if (route.equals(r)) {
               switch (rule) {
                  case SAME_CONTENT:
                     if (!matchContents(request, entry.getValue())) {
                        continue;
                     }
                     break;
                  case SAME_IDENTITY:
                     if (!matchIdentities(request, entry.getValue())) {
                        continue;
                     }
                     break;
                  case SAME_REQUEST:
                     if (!matchContents(request, entry.getValue())
                              || !matchIdentities(request, entry.getValue())) {
                        continue;
                     }
                     break;
                  default:
                     break;
               }
               return entry.getKey();
            }
         }
      }
      return null;
   }

   private boolean matchContents(AsyncRequest request, AsyncRequest queued) {
      Object thisContent = request.getRoute().getContentObject();
      Object otherContent = queued.getRoute().getContentObject();
      return thisContent == null == (otherContent == null)
               && (thisContent == null || thisContent.equals(otherContent));
   }

   private boolean matchIdentities(AsyncRequest request, AsyncRequest queued) {
      IIdentity thisIdentity = request.getIdentity();
      IIdentity otherIdentity = queued.getIdentity();
      return otherIdentity == null || thisIdentity != null && thisIdentity.equals(otherIdentity);
   }
}
