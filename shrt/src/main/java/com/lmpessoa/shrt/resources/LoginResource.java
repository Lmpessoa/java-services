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
package com.lmpessoa.shrt.resources;

import com.lmpessoa.services.security.IIdentity;
import com.lmpessoa.services.security.IdentityBuilder;
import com.lmpessoa.services.views.FailedLoginException;
import com.lmpessoa.services.views.ILoginResource;
import com.lmpessoa.services.views.UserPasswordRequest;
import com.lmpessoa.shrt.model.ILinksManager;

public class LoginResource implements ILoginResource {

   private final ILinksManager links;

   public LoginResource(ILinksManager links) {
      this.links = links;
   }

   @Override
   public IIdentity post(UserPasswordRequest login) {
      if (links.isValidUser(login.getUsername(), login.getPassword())) {
         return new IdentityBuilder().addAccountName(login.getUsername()).build();
      }
      throw new FailedLoginException();
   }
}
