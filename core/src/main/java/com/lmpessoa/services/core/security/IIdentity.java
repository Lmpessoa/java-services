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

import static com.lmpessoa.services.core.services.Reuse.REQUEST;

import java.util.Collection;

import com.lmpessoa.services.core.services.Service;

/**
 * Represents a user's identity on the system.
 * <p>
 * Users may come in a variety of formats and support different sets of informations. However, in
 * order for identities to be used with the engine, they must provide a set of methods to allow
 * querying who the user is through this interface.
 * </p>
 * <p>
 * Applications may choose to implement their own subclass, work with a third-party implementation,
 * or even work with the {@link GenericIdentity} class, however suits their needs.
 * </p>
 * <p>
 * Any information about an identity must be possible to be retrieved using claims, no matter if the
 * implementor of this interface provides further convenience methods to access the same
 * information.
 * </p>
 */
@Service(reuse = REQUEST)
public interface IIdentity {

   /**
    * Returns the name of the user.
    * <p>
    * The name information is usually just a display string that allows for visually asserting the
    * identity of the user.
    * </p>
    * <p>
    * This is a convenience method since the user name to be displayed can come from a variety of
    * sources (claims).
    * </p>
    *
    * @return
    */
   String getName();

   /**
    * Returns whether the user of this identity was given a certain role.
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
   boolean hasRole(String roleName);

   /**
    * Returns whether the user of this identity has a certain information claim.
    * <p>
    * A claim is any information about a certain user. The means used by a system to identify which
    * claims an user has are dependent on the given application. Subclasses implementing this method
    * are free to retrieve this information from wherever the see best suited.
    * </p>
    *
    * @param claimType the type of the claim to be tested.
    * @return {@code true} if the user has a claim of the given type, {@code false} otherwise.
    */
   boolean hasClaim(String claimType);

   /**
    * Returns a collection of all the claims an identity has.
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
    * <p>
    * A claim is any information about a certain user. The means used by a system to identify which
    * claims an user has are dependent on the given application. Subclasses implementing this method
    * are free to retrieve this information from wherever the see best suited.
    * </p>
    *
    * @param claimType the type of the claim to be retrieved.
    * @return the first claim of the given type from this identity.
    */
   Claim getFirstClaim(String claimType);

   /**
    * Returns a collection of all the claims of the given type from this identity.
    * <p>
    * A claim is any information about a certain user. The means used by a system to identify which
    * claims an user has are dependent on the given application. Subclasses implementing this method
    * are free to retrieve this information from wherever the see best suited.
    * </p>
    *
    * @param claimType the type of the claim to be retrieved.
    * @return a collection of all the claims of the given type from this identity.
    */
   Collection<Claim> getAllClaims(String claimType);
}
