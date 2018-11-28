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
package com.lmpessoa.services.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Random;

import com.lmpessoa.services.hosting.Headers;
import com.lmpessoa.services.hosting.HttpRequest;

/**
 * Provides means for the engine to retrieve user information.
 *
 * <p>
 * Classes implementing this interface are responsible for returning the identity or the user in a
 * given request.
 * </p>
 *
 * <p>
 * By default, users are identified by a token sent through the {@code Authorization} HTTP header.
 * This header must be composed by one or more parts describing the authentication mechanism used
 * and its arguments. Developers deciding to implement {@link #getIdentity(String, String)} must
 * evaluate the {@code method} to check whether or not the authentication mechanism is to be
 * supported while the {@code token} argument contains the remaining arguments used to identify the
 * user.
 * </p>
 *
 * <p>
 * Developers may opt to implement {@link #getIdentity(HttpRequest)} instead. This method will
 * enable full access to the request and allow applications to retrieve information about the user's
 * identity from anywhere in the request.
 * </p>
 */
public interface IIdentityProvider {

   /**
    * Returns a pseudo random token for the user represented by the given ID.
    *
    * <p>
    * Applications using the engine were designed to work as backend services. Thus, instead of
    * being identified by the classic username/password pair, services expect you to provide an
    * information (like a token) that can be verified as belonging to a certain user.
    * </p>
    *
    * <p>
    * This method can be used to generate pseudo random tokens that can be used by other
    * applications to identify themselves with the current application.
    * </p>
    *
    * <p>
    * Note that this method only produces a token, and is not responsible for associating it with a
    * specific user, retrieving specific credentials, or or even testing for collision with previous
    * tokens.
    * </p>
    *
    * @param userId the ID of the user for which the token will be generated.
    * @return the pseudo random token for the user represented by the given ID.
    */
   static String generateTokenFor(String userId) {
      // Computes the string hash using long instead of int
      // This in separate to ensure no conflicts with other users
      long hashl = 0;
      byte[] s = userId.getBytes(StandardCharsets.UTF_8);
      for (int n = 0; n < s.length; ++n) {
         hashl += s[n] * Math.pow(31, (double) n - 1);
      }

      // Create random long using bytes since Random cannot
      // produce all values on the long range
      BigInteger randi = BigInteger.ZERO;
      Random rand = new Random();
      for (int i = 0; i < 42; ++i) {
         randi = randi.multiply(BigInteger.TEN) //
                  .add(BigInteger.valueOf(rand.nextInt(10)));
      }
      randi = randi.multiply(BigInteger.TEN.pow(19)) //
               .add(BigInteger.valueOf(Instant.now().toEpochMilli()));

      // Convert values to base62
      final String RADIX = "0qQaAzZ1wWsSxX2eEdDcC3rRfFvV4tTgGbB5yYhHnN6uUjJmM7iIkK8oOlL9pP";
      StringBuilder hash = new StringBuilder();
      while (hashl != 0) {
         hash.append(RADIX.charAt((int) (hashl % RADIX.length())));
         hashl /= RADIX.length();
      }
      while (hash.length() < 11) {
         hash.insert(0, '0');
      }
      StringBuilder token = new StringBuilder();
      BigInteger radix = BigInteger.valueOf(RADIX.length());
      while (!randi.equals(BigInteger.ZERO)) {
         BigInteger[] div = randi.divideAndRemainder(radix);
         token.append(RADIX.charAt(div[1].intValue()));
         randi = div[0];
      }
      while (token.length() > 34) {
         token.delete(0, 1);
      }
      while (token.length() < 34) {
         token.insert(0, '0');
      }
      hash.append(token.toString());
      return hash.toString();
   }

   /**
    * Returns the identity of the user of the given request.
    *
    * <p>
    * Applications that wish to have more control over how the identity of the user is retrieved
    * from the request must subclass this method. Most applications however will be fine
    * implementing only {@link #getIdentity(String, String)}.
    * </p>
    *
    * @param request the request from which to retrieve the identity of the user.
    * @return the identity of the user of the given request.
    */
   default IIdentity getIdentity(HttpRequest request) {
      String auth = request.getHeaders().get(Headers.AUTHORIZATION);
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
    *
    * <p>
    * By default, users are identified by a token sent through the {@code Authorization} HTTP
    * header. The format of this token is not bound by the engine and may bear any format. It is,
    * however, recommended that developers check the {@code format} argument to verify that the
    * authentication mechanism is one supported by the identity provider.
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
