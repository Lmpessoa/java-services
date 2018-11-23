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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a generic identity that can be reused by different identity providers.
 * <p>
 * A generic identity is a simple collection of claims that represent an user. Since it is generic,
 * any information about this identity can only be retrieved through claims as it does not provide
 * any convenience methods for any claim.
 * </p>
 * <p>
 * Identity providers may choose to return instances of this class instead of implementing their own
 * identity classes.
 * </p>
 */
public final class GenericIdentity implements IIdentity {

   private final List<Claim> claims = Collections.synchronizedList(new ArrayList<>());

   /**
    * Creates a new empty {@code GenericIdentity}.
    */
   public GenericIdentity() {
      // Nothing to do, just keep the constructor available
   }

   /**
    * Creates a new {@code GenericIdentity} with the given collection of claims.
    *
    * @param claims the initial list of claims of this identity.
    */
   public GenericIdentity(Collection<Claim> claims) {
      addClaims(claims);
   }

   @Override
   public String getName() {
      Claim c = getFirstClaim(ClaimType.NAME);
      if (c == null) {
         c = getFirstClaim(ClaimType.DISPLAY_NAME);
         if (c == null) {
            c = getFirstClaim(ClaimType.GIVEN_NAME);
            Claim c2 = getFirstClaim(ClaimType.SURNAME);
            if (c != null && c2 != null) {
               return String.format("%s %s", c.getValue(), c2.getValue());
            }
            c = getFirstClaim(ClaimType.ACCOUNT_NAME);
            if (c == null) {
               c = getFirstClaim(ClaimType.EMAIL);
            }
         }
      }
      return c == null ? null : c.getValue().toString();
   }

   @Override
   public boolean hasRole(String roleName) {
      Objects.requireNonNull(roleName);
      return claims.stream() //
               .filter(c -> c.getType() == ClaimType.ROLE)
               .map(c -> c.getValue().toString())
               .anyMatch(roleName::equals);
   }

   @Override
   public boolean hasClaim(String type) {
      return claims.stream().anyMatch(c -> c.getType().equals(type));
   }

   @Override
   public Collection<Claim> claims() {
      return Collections.unmodifiableCollection(claims);
   }

   @Override
   public Claim getFirstClaim(String claimType) {
      return claims.stream().filter(c -> c.getType().equals(claimType)).findFirst().orElse(null);
   }

   @Override
   public Collection<Claim> getAllClaims(String claimType) {
      return Collections.unmodifiableCollection(
               claims.stream().filter(c -> c.getType().equals(claimType)).collect(
                        Collectors.toList()));
   }

   /**
    * Adds the role with the given name to this identity.
    *
    * @param roleName the name of the role to be added to this identity.
    */
   public void addRole(String roleName) {
      addClaim(new Claim(ClaimType.ROLE, roleName));
   }

   /**
    * Adds the roles with the given names to this identity.
    *
    * @param roleNames the names of the roles to be added to this identity.
    */
   public void addRoles(String[] roleNames) {
      Arrays.stream(roleNames).forEach(this::addRole);
   }

   /**
    * Adds the roles with the given names to this identity.
    *
    * @param roleNames the names of the roles to be added to this identity.
    */
   public void addRoles(Collection<String> roleNames) {
      roleNames.stream().forEach(this::addRole);
   }

   /**
    * Adds the given claim to this identity.
    *
    * @param claim the claim to be added to this identity.
    */
   public void addClaim(Claim claim) {
      if (!claims.contains(claim)) {
         claims.add(claim);
      }
   }

   /**
    * Adds a claim with the given name and value to this identity.
    *
    * @param claimType the type of the claim to be added.
    * @param value the value of the claim to be added.
    */
   public void addClaim(String claimType, String value) {
      addClaim(new Claim(claimType, value));
   }

   /**
    * Adds the given claims to this identity.
    *
    * @param claims the list of claims to be added to this identity
    */
   public void addClaims(Claim[] claims) {
      Arrays.stream(claims).forEach(this::addClaim);
   }

   /**
    * Adds the given claims to this identity.
    *
    * @param claims the list of claims to be added to this identity
    */
   public void addClaims(Collection<Claim> claims) {
      claims.stream().forEach(this::addClaim);
   }
}
