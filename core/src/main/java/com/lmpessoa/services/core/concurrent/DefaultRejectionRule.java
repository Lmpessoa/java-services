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
package com.lmpessoa.services.core.concurrent;

import java.util.Collection;

import com.lmpessoa.services.core.routing.RouteMatch;

final class DefaultRejectionRule implements IAsyncRejectionRule {

   private final AsyncReject rule;

   public DefaultRejectionRule(Async async) {
      this.rule = async.reject();
   }

   @Override
   public boolean shouldReject(RouteMatch route, Collection<RouteMatch> routes) {
      if (rule != AsyncReject.NEVER) {
         for (RouteMatch r : routes) {
            if (route.equals(r)) {
               if (rule == AsyncReject.SAME_PATH) {
                  return true;
               }
               Object thisContent = route.getContentObject();
               Object otherContent = r.getContentObject();
               if (thisContent == null == (otherContent == null)
                        && (thisContent == null || thisContent.equals(otherContent))) {
                  return true;
               }
            }
         }
      }
      return false;
   }

}
