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
package com.lmpessoa.services.core.security;

import com.lmpessoa.services.core.hosting.Headers;
import com.lmpessoa.services.core.hosting.HttpRequest;

/**
 * Provides means for the engine to retrieve user information.
 * <p>
 * Classes implementing this interface are responsible for returning the identity or the user in a
 * given request.
 * </p>
 * <p>
 * By default, users are identified by a token sent through the {@code Authorization} HTTP header.
 * This header must be composed by one or more parts describing the authentication mechanism used
 * and its arguments. Developers deciding to implement {@link #getIdentity(String, String)} must
 * evaluate the {@code method} to check whether or not the authentication mechanism is to be
 * supported while the {@code token} argument contains the remaining arguments used to identify the
 * user.
 * </p>
 * <p>
 * Developers may opt to implement {@link #getIdentity(HttpRequest)} instead. This method will
 * enable full access to the request and allow applications to retrieve information about the user's
 * identity from anywhere in the request.
 * </p>
 */
public interface IIdentityProvider {

   /**
    * Returns the identity of the user of the given request.
    * <p>
    * Applications that wish to have more control over how the identity of the user is retrieved from
    * the request must subclass this method. Most applications however will be fine implementing only
    * {@link #getIdentity(String, String)}.
    * </p>
    *
    * @param request the request from which to retrieve the identity of the user.
    * @return the identity of the user of the given request.
    */
   default IIdentity getIdentity(HttpRequest request) {
      String auth = request.getHeader(Headers.AUTHORIZATION);
      if (auth != null) {
         String[] parts = auth.split(" ", 2);
         if (parts.length == 2) {
            return getIdentity(parts[0], parts[1]);
         } else {
            return getIdentity("Token", auth);
         }
      }
      return null;
   }

   /**
    * Returns the identity of the user with the given authentication credentials.
    * <p>
    * By default, users are identified by a token sent through the {@code Authorization} HTTP header.
    * The format of this token is not bound by the engine and may bear any format. It is, however,
    * recommended that developers check the {@code format} argument to verify that the authentication
    * mechanism is one supported by the identity provider.
    * </p>
    *
    * @param format the name of the mechanism used for the user's identification.
    * @param token a token that identifies the current user.
    * @return the identity of the user with the given authentication credentials.
    */
   default IIdentity getIdentity(String format, String token) {
      return null;
   }
}
