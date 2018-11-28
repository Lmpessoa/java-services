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

import static com.lmpessoa.services.services.Reuse.REQUEST;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import com.lmpessoa.services.services.Service;

/**
 * Represents a user's identity on the system.
 *
 * <p>
 * Users may come in a variety of formats and support different sets of informations. However, in
 * order for identities to be used with the engine, they must provide a set of methods to allow
 * querying who the user is through this interface.
 * </p>
 *
 * <p>
 * Applications may choose to implement their own subclass, work with a third-party implementation,
 * or even work with the {@link IdentityBuilder} class to create a generic, whatever suits their
 * needs best. However, developer must be aware that, while implementing their own identity
 * subclass, although not enforced by the compiler, these subclasses must provide their own
 * implementation of {@link #equals(Object)} if they plan on supporting asynchronous calls (or, more
 * precisely, the abortion of asynchronous tasks by the requester).
 * </p>
 *
 * <p>
 * To support the format required by the engine, any information about an identity must be possible
 * to be retrieved using claims, no matter if the implementor of this interface provides further
 * convenience methods to access the same information. For example, the default implementation for
 * the methods {@link #getName()} and {@link #hasRole(String)} all rely on claims.
 * </p>
 *
 * @see IdentityBuilder
 */
@Service(reuse = REQUEST)
public interface IIdentity {

   /**
    * Returns a collection of all the claims an identity has.
    *
    * <p>
    * A claim is any information about a certain user. The means used by a system to identify which
    * claims an user has are dependent on the given application. Subclasses implementing this method
    * are free to retrieve this information from wherever the see best suited.
    * </p>
    *
    * @return a collection of all the claims an identity has.
    */
   Collection<Claim> claims();

   /**
    * Returns the first claim of the given type from this identity.
    *
    * <p>
    * A claim is any information about a certain user. The means used by a system to identify which
    * claims an user has are dependent on the given application. Subclasses implementing this method
    * are free to retrieve this information from wherever the see best suited.
    * </p>
    *
    * @param claimType the type of the claim to be retrieved.
    * @return the first claim of the given type from this identity.
    */
   default Collection<Claim> claims(String claimType) {
      return claims().stream().filter(c -> c.getType().equals(claimType)).collect(
               Collectors.toList());
   }

   /**
    * Returns whether the user of this identity has a certain information claim.
    *
    * <p>
    * A claim is any information about a certain user. The means used by a system to identify which
    * claims an user has are dependent on the given application. Subclasses implementing this method
    * are free to retrieve this information from wherever the see best suited.
    * </p>
    *
    * @param claimType the type of the claim to be tested.
    * @return {@code true} if the user has a claim of the given type, {@code false} otherwise.
    */
   default boolean hasClaim(String claimType) {
      return claims().stream().anyMatch(c -> c.getType().equals(claimType));
   }

   /**
    * Returns the name of the user for display.
    *
    * <p>
    * The name information is usually just a display string that allows for visually asserting the
    * identity of the user and not used to distinguish the user uniquely.
    * </p>
    * <p>
    * This is a convenience method since the user name to be displayed can come from a variety of
    * sources (claims).
    * </p>
    *
    * @return the name of the user
    */
   default String getDisplayName() {
      Collection<Claim> c = claims(ClaimType.DISPLAY_NAME);
      if (c.isEmpty()) {
         c = claims(ClaimType.NAME);
      }
      if (c.isEmpty()) {
         c = claims(ClaimType.GIVEN_NAME);
         if (!c.isEmpty()) {
            Claim c1 = c.stream().findFirst().orElse(null);
            if (c1 != null) {
               String c2 = claims(ClaimType.SURNAME).stream() //
                        .filter(cs -> Claim.equalIssuers(cs, c1))
                        .map(Claim::getValue)
                        .findFirst()
                        .orElse("")
                        .toString();
               return String.format("%s %s", c1.getValue().toString(), c2).trim();
            }
         }
      }
      if (c.isEmpty()) {
         c = claims(ClaimType.ACCOUNT_NAME);
      }
      if (c.isEmpty()) {
         c = claims(ClaimType.EMAIL);
      }
      Claim result = c.stream().findFirst().orElse(null);
      return result == null ? null : result.getValue().toString();
   }

   /**
    * Returns whether the user of this identity was given a certain role.
    *
    * <p>
    * Roles can be used to determine what an user is entitled to do in a given system. The means
    * used by a system to identify which roles an user has are dependent on the given application.
    * Subclasses implementing this method are free to retrieve this information from wherever the
    * see best suited.
    * </p>
    *
    * @param roleName the name of the role to check if the user was given.
    * @return {@code true} if the user is authorised with the given role, {@code false} otherwise.
    */
   default boolean hasRole(String roleName) {
      Objects.requireNonNull(roleName);
      return claims().stream() //
               .filter(c -> c.getType() == ClaimType.ROLE)
               .map(c -> c.getValue().toString())
               .anyMatch(roleName::equals);
   }
}
