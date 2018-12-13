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
package com.lmpessoa.shrt.identity;

import java.time.Duration;

import com.lmpessoa.services.security.ClaimType;
import com.lmpessoa.services.security.IIdentity;
import com.lmpessoa.services.security.ITokenManager;
import com.lmpessoa.services.views.IViewTokenManager;
import com.lmpessoa.shrt.model.ILinksManager;

public final class ShrtTokenManager implements IViewTokenManager {

   private final ILinksManager links;

   public ShrtTokenManager(ILinksManager links) {
      this.links = links;
   }

   @Override
   public IIdentity get(String token) {
      return links.getUserOfToken(token);
   }

   @Override
   public String add(IIdentity identity, Duration expires) {
      String user = identity.claims(ClaimType.ACCOUNT_NAME)
               .stream()
               .map(c -> c.getValue().toString())
               .findFirst()
               .orElse(null);
      if (user == null) {
         return null;
      }
      String token = ITokenManager.generateTokenFor(user);
      links.setTokenForUser(user, token, expires);
      return token;
   }

   @Override
   public void remove(String token) {
      links.removeToken(token);
   }
}
