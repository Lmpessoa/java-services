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

import java.util.Objects;

import com.lmpessoa.services.internal.CoreMessage;

/**
 * Represents an information about a certain user.
 *
 * <p>
 * Claims are usually a pair formed by a type and a value. This pair represents some information
 * about an user in an identity, like a name, e-mail or roles assigned to that user. An user's
 * identity may contain as many claims it requires and each claim type can be present multiple times
 * in one single identity.
 * </p>
 *
 * <p>
 * One created, the value of a {@code Claim} instance cannot be changed. Note, however, that this
 * does not mean the value cannot be changed in the underlying system where it is stored since it's
 * dependent only on the application that provides the identity claim.
 * </p>
 *
 * <p>
 * It is not mandatory but claims may be associated with the source or issuer of that claim. In
 * identities formed with information from more than one source can use this information about a
 * claim.
 * </p>
 *
 * @see IIdentity
 */
public final class Claim {

   private final String issuer;
   private final Object value;
   private final String type;

   /**
    * Creates a new {@code Claim} with the given type and value. The issuer of this new
    * {@code Claim} is {@code null}.
    *
    * @param type the type of the claim to be created.
    * @param value the value to be associated with this claim type.
    */
   public Claim(String type, Object value) {
      this(null, type, value);
   }

   /**
    * Creates a new {@code Claim} with the given type, value and issuer.
    *
    * <p>
    * It is recommended that the issuer of a claim be identified by the base URL of the
    * application's main site. For example, the recommended issuer for a Facebook claim should be
    * {@code "https://www.facebook.com"}, while Twitter should use {@code "https://twitter.com"}.
    * Either format with or without the {@code "www."} part is acceptable, as long as it is
    * consistent among all claims by the same issuer.
    * </p>
    *
    * @param type the type of the claim to be created.
    * @param value the value to be associated with this claim type.
    * @param issuer an identifier of the issuer of this claim, or {@code null} if it cannot be
    *           identified.
    *
    * @see ClaimType
    */
   public Claim(String issuer, String type, Object value) {
      this.value = Objects.requireNonNull(value);
      this.type = Objects.requireNonNull(type);
      if (!type.matches("([a-z][a-z0-9]*:)+claim:[a-z][a-z0-9]*")) {
         throw new IllegalArgumentException(CoreMessage.INVALID_CLAIM_TYPE.with(type));
      }
      this.issuer = issuer;
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof Claim)) {
         return false;
      }
      Claim other = (Claim) o;
      return type.equals(other.type) && value.equals(other.value)
               && (issuer == null && other.issuer == null
                        || issuer != null && issuer.equals(other.issuer));
   }

   @Override
   public int hashCode() {
      return (issuer != null ? issuer.hashCode() : 0) + type.hashCode() + value.hashCode();
   }

   /**
    * Returns the type of the data of this claim.
    *
    * <p>
    * The type of a claim identifies what information that claim has associated with it. For a list
    * of examples claim types, see {@link ClaimType}. For custom claims, it is strongly recommended
    * to avoid the prefix {@code "identity:"} since these are used to identify common claims by the
    * engine.
    * </p>
    *
    * @return the type of the data of this claim.
    *
    * @see ClaimType
    */
   public String getType() {
      return type;
   }

   /**
    * Returns the value of this claim.
    *
    * <p>
    * The value of a claim can be an object of any type. Developers are advised to check the type of
    * data expected from any given source/issuer before trying to cast the value into another type.
    * </p>
    *
    * @return the value of this claim.
    */
   public Object getValue() {
      return value;
   }

   /**
    * Returns the identifier of the issuer of this claim.
    *
    * <p>
    * It is recommended this identifier be the main URL used by the issuer of this claim, but it is
    * not mandatory and issuers or identity providers may use other values as long as this value is
    * consistent with all claims from the same issuer.
    * </p>
    *
    * @return the identifier of the issuer of this claim.
    */
   public String getIssuer() {
      return issuer;
   }

   static boolean equalIssuers(Claim c1, Claim c2) {
      Objects.requireNonNull(c1);
      Objects.requireNonNull(c2);
      if (c1.getIssuer() == null != (c2.getIssuer() == null)) {
         return false;
      } else if (c1.getIssuer() != null) {
         return c1.getIssuer().equals(c2.getIssuer());
      }
      return true;
   }
}
