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

import java.net.MalformedURLException;
import java.net.URL;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Helps creating an identity instance.
 *
 * <p>
 * Most methods in this class are convenience methods to add proper claims without requiring the
 * direct use of the constants defined by {@link ClaimType} class or individually instantiating the
 * {@link Claim} class. The parameters of these methods reflect the expected type of the value of
 * that claim; methods where this parameter is {@code Object} do not have an expected type defined.
 * </p>
 *
 * <p>
 * After defining the claims of the identity, use the method {@link #build()} to return the identity
 * instance with the given set of claims. Although unlikely, an instance of {@code IdentityBuilder}
 * may be reused to create another identity after this call. Note, however, that the builder will
 * not retain any of the claims assigned to the previously created identity and will behave as if
 * just created.
 *
 * <p>
 * Identity instances created using an IdentityBuilder are immutable.
 * </p>
 *
 * @see IIdentity
 * @see Claim
 * @see ClaimType
 */
public final class IdentityBuilder {

   private BiPredicate<IIdentity, IIdentity> comparer = IdentityBuilder::fullEquals;
   private Collection<Claim> claims = new ArrayList<>();

   /**
    * Creates the identity instance using the previously defined set of claims.
    *
    * <p>
    * An {@code IdentityBuilder} may be reused to create another identity instance after the call to
    * this method.
    * </p>
    *
    * @return the identity built with the previously set of claims.
    */
   public IIdentity build() {
      final Collection<Claim> resultClaims = Collections.unmodifiableCollection(this.claims);
      final BiPredicate<IIdentity, IIdentity> resultComparer = this.comparer;
      this.comparer = IdentityBuilder::fullEquals;
      this.claims = new ArrayList<>();
      return new IIdentity() {

         @Override
         public Collection<Claim> claims() {
            return resultClaims;
         }

         @Override
         public boolean equals(Object obj) {
            if (!(obj instanceof IIdentity)) {
               return false;
            }
            return resultComparer.test(this, (IIdentity) obj);
         }

         @Override
         public int hashCode() {
            return resultClaims.hashCode();
         }
      };
   }

   public IdentityBuilder compareUsing(String claimType) {
      compareUsing(null, claimType);
      return this;
   }

   public IdentityBuilder compareUsing(String issuer, String claimType) {
      if (!claimType.matches("([a-z][a-z0-9]*:)+claim:[a-z][a-z0-9]*")) {
         throw new IllegalArgumentException("Not a valid claim type identifier: " + claimType);
      }
      this.comparer = (i1, i2) -> claimEquals(i1, i2, issuer, claimType);
      return this;
   }

   public IdentityBuilder compareUsing(BiPredicate<IIdentity, IIdentity> comparer) {
      this.comparer = Objects.requireNonNull(comparer);
      return this;
   }

   /**
    * Adds the given claim to this identity.
    *
    * @param claim the claim to be added to this identity.
    */
   public IdentityBuilder addClaim(Claim claim) {
      claims.add(claim);
      return this;
   }

   /**
    * Adds the given claim to this identity.
    *
    * @param claimType the type of the claim to be added.
    * @param value the value of the claim to be added.
    * @param issuer the issuer of the claim to be added.
    */
   public IdentityBuilder addClaim(String issuer, String claimType, Object value) {
      return addClaim(new Claim(issuer, claimType, value));
   }

   /**
    * Adds the given claim to this identity
    *
    * @param claimType the type of the claim to be added.
    * @param value the value of the claim to be added.
    */
   public IdentityBuilder addClaim(String claimType, Object value) {
      return addClaim(null, claimType, value);
   }

   /**
    * Adds a collection of claims to this identity.
    *
    * @param claims the collection of claims to be added.
    */
   public IdentityBuilder addClaims(Collection<Claim> claims) {
      this.claims.addAll(claims);
      return this;
   }

   /**
    * Adds the given role to this identity.
    *
    * @param issuer the issuer of the role to be added.
    * @param roleName the identifier of the role to be added.
    */
   public IdentityBuilder addRole(String issuer, String roleName) {
      return addClaim(issuer, ClaimType.ROLE, roleName);
   }

   /**
    * Adds the given role to this identity.
    *
    * @param roleName the identifier of the role to be added.
    */
   public IdentityBuilder addRole(String roleName) {
      return addRole(null, roleName);
   }

   /**
    * Adds the given roles to this identity.
    *
    * @param issuer the issuer of the role to be added.
    * @param roleNames the identifiers of the roles to be added.
    */
   public IdentityBuilder addRoles(String issuer, String[] roleNames) {
      return addRoles(issuer, Arrays.asList(roleNames));
   }

   /**
    * Adds the given roles to this identity.
    *
    * @param roleNames the identifier of the roles to be added.
    */
   public IdentityBuilder addRoles(String[] roleNames) {
      return addRoles(null, roleNames);
   }

   /**
    * Adds the given roles to this identity.
    *
    * @param issuer the issuer of the role to be added.
    * @param roleNames the identifiers of the roles to be added.
    */
   public IdentityBuilder addRoles(String issuer, Collection<String> roleNames) {
      for (String roleName : roleNames) {
         addRole(issuer, roleName);
      }
      return this;
   }

   /**
    * Adds the given roles to this identity.
    *
    * @param roleNames the names of the roles to be added.
    */
   public IdentityBuilder addRoles(Collection<String> roleNames) {
      return addRoles(null, roleNames);
   }

   /**
    * Adds the given account name to this identity.
    *
    * @param issuer the issuer of the account name to be added.
    * @param accountName the name of the account to be added.
    */
   public IdentityBuilder addAccountName(String issuer, String accountName) {
      return addClaim(issuer, ClaimType.ACCOUNT_NAME, accountName);
   }

   /**
    * Adds the given account name to this identity.
    *
    * @param accountName the name of the account to be added.
    */
   public IdentityBuilder addAccountName(String accountName) {
      return addAccountName(null, accountName);
   }

   /**
    * Adds the given URL as the avatar of this identity.
    *
    * @param issuer the issuer of the avatar URL to be added.
    * @param url the URL of the avatar to be added.
    */
   public IdentityBuilder addAvatarUrl(String issuer, URL url) {
      return addClaim(issuer, ClaimType.AVATAR_URL, url);
   }

   /**
    * Adds the given URL as the avatar of this identity.
    *
    * @param url the URL of the avatar to be added.
    */
   public IdentityBuilder addAvatarUrl(URL url) {
      return addAvatarUrl(null, url);
   }

   /**
    * Adds the given URL as the avatar of this identity.
    *
    * @param issuer the issuer of the avatar URL to be added.
    * @param url the URL of the avatar to be added.
    */
   public IdentityBuilder addAvatarUrl(String issuer, String url) throws MalformedURLException {
      return addAvatarUrl(issuer, new URL(url));
   }

   /**
    * Adds the given URL as the avatar of this identity.
    *
    * @param url the URL of the avatar to be added.
    */
   public IdentityBuilder addAvatarUrl(String url) throws MalformedURLException {
      return addAvatarUrl(null, url);
   }

   /**
    * Adds the given country to this identity.
    *
    * @param issuer the issuer of the country to be added.
    * @param country the country value to be added.
    */
   public IdentityBuilder addCountry(String issuer, Object country) {
      return addClaim(issuer, ClaimType.COUNTRY, country);
   }

   /**
    * Adds the given country to this identity.
    *
    * @param country the country value to be added.
    */
   public IdentityBuilder addCountry(Object country) {
      return addCountry(null, country);
   }

   /**
    * Adds the given date of birth to this identity.
    *
    * @param issuer the issuer of the date of birth to be added.
    * @param date the date of birth to be added.
    */
   public IdentityBuilder addDateOfBirth(String issuer, TemporalAccessor date) {
      return addClaim(issuer, ClaimType.DATE_OF_BIRTH, date);
   }

   /**
    * Adds the given date of birth to this identity.
    *
    * @param date the date of birth to be added.
    */
   public IdentityBuilder addDateOfBirth(TemporalAccessor date) {
      return addDateOfBirth(null, date);
   }

   /**
    * Adds the given display name to this identity.
    *
    * @param issuer the issuer of the display name to be added.
    * @param displayName the display name to be added.
    */
   public IdentityBuilder addDisplayName(String issuer, String displayName) {
      return addClaim(issuer, ClaimType.DISPLAY_NAME, displayName);
   }

   /**
    * Adds the given display name to this identity.
    *
    * @param displayName the display name to be added.
    */
   public IdentityBuilder addDisplayName(String displayName) {
      return addDisplayName(null, displayName);
   }

   /**
    * Adds the given e-mail address to this identity.
    *
    * @param issuer the issuer of the e-mail address to be added.
    * @param email the e-mail address to be added.
    */
   public IdentityBuilder addEmail(String issuer, String email) {
      return addClaim(issuer, ClaimType.EMAIL, email);
   }

   /**
    * Adds the given e-mail address to this identity.
    *
    * @param email the e-mail address to be added.
    */
   public IdentityBuilder addEmail(String email) {
      return addEmail(null, email);
   }

   /**
    * Adds the given expiration date to this identity.
    *
    * @param issuer the issuer of the expiration date to be added.
    * @param expiration the expiration date to be added.
    */
   public IdentityBuilder addExpiration(String issuer, TemporalAccessor expiration) {
      return addClaim(issuer, ClaimType.EXPIRATION, expiration);
   }

   /**
    * Adds the given expiration date to this identity.
    *
    * @param expiration the expiration date to be added.
    */
   public IdentityBuilder addExpiration(TemporalAccessor expiration) {
      return addExpiration(null, expiration);
   }

   /**
    * Adds the given expired date to this identity.
    *
    * @param issuer the issuer of the expired date to be added.
    * @param expired the expired date to be added.
    */
   public IdentityBuilder addExpired(String issuer, TemporalAccessor expired) {
      return addClaim(issuer, ClaimType.EXPIRED, expired);
   }

   /**
    * Adds the given expired date to this identity.
    *
    * @param expired the expired date to be added.
    */
   public IdentityBuilder addExpired(TemporalAccessor expired) {
      return addExpired(null, expired);
   }

   /**
    * Adds the given gender to this identity.
    *
    * @param issuer the issuer of the gender to be added.
    * @param gender the value of the gender to be added.
    */
   public IdentityBuilder addGender(String issuer, Object gender) {
      return addClaim(issuer, ClaimType.GENDER, gender);
   }

   /**
    * Adds the given gender to this identity.
    *
    * @param gender the value of the gender to be added.
    */
   public IdentityBuilder addGender(Object gender) {
      return addGender(null, gender);
   }

   /**
    * Adds the given first name to this identity.
    *
    * @param issuer the issuer of the first name to be added.
    * @param name the name to be added.
    */
   public IdentityBuilder addGivenName(String issuer, String name) {
      return addClaim(issuer, ClaimType.GIVEN_NAME, name);
   }

   /**
    * Adds the given first name to this identity.
    *
    * @param name the name to be added.
    */
   public IdentityBuilder addGivenName(String name) {
      return addGivenName(null, name);
   }

   /**
    * Adds the given home phone number to this identity.
    *
    * @param issuer the issuer of the phone number to be added.
    * @param phoneNumber the phone number to be added.
    */
   public IdentityBuilder addHomePhone(String issuer, String phoneNumber) {
      return addClaim(issuer, ClaimType.HOME_PHONE, phoneNumber);
   }

   /**
    * Adds the given home phone number to this identity.
    *
    * @param phoneNumber the phone number to be added.
    */
   public IdentityBuilder addHomePhone(String phoneNumber) {
      return addHomePhone(null, phoneNumber);
   }

   /**
    * Adds the given locale to this identity.
    *
    * @param issuer the issuer of the locale to be added.
    * @param locale the value of the locale to be added.
    */
   public IdentityBuilder addLocale(String issuer, Locale locale) {
      return addClaim(issuer, ClaimType.LOCALE, locale);
   }

   /**
    * Adds the given locale to this identity.
    *
    * @param locale the value of the locale to be added.
    */
   public IdentityBuilder addLocale(Locale locale) {
      return addLocale(null, locale);
   }

   /**
    * Adds the given locale to this identity.
    *
    * @param issuer the issuer of the locale to be added.
    * @param locale the tag of the locale to be added.
    */
   public IdentityBuilder addLocale(String issuer, String locale) {
      return addLocale(issuer, Locale.forLanguageTag(locale));
   }

   /**
    * Adds the given locale to this identity.
    *
    * @param locale the tag of the locale to be added.
    */
   public IdentityBuilder addLocale(String locale) {
      return addLocale(null, locale);
   }

   /**
    * Adds the given mobile phone number to this identity.
    *
    * @param issuer the issuer of the phone number to be added.
    * @param phoneNumber the phone number to be added.
    */
   public IdentityBuilder addMobilePhone(String issuer, String phoneNumber) {
      return addClaim(issuer, ClaimType.MOBILE_PHONE, phoneNumber);
   }

   /**
    * Adds the given mobile phone number to this identity.
    *
    * @param phoneNumber the phone number to be added.
    */
   public IdentityBuilder addMobilePhone(String phoneNumber) {
      return addMobilePhone(null, phoneNumber);
   }

   /**
    * Adds the given name to this identity.
    *
    * @param issuer the issuer of the name to be added.
    * @param name the name to be added.
    */
   public IdentityBuilder addName(String issuer, String name) {
      return addClaim(issuer, ClaimType.NAME, name);
   }

   /**
    * Adds the given name to this identity.
    *
    * @param name the name to be added.
    */
   public IdentityBuilder addName(String name) {
      return addName(null, name);
   }

   /**
    * Adds the given phone number to this identity.
    *
    * @param issuer the issuer of the phone number to be added.
    * @param phoneNumber the phone number to be added.
    */
   public IdentityBuilder addOtherPhone(String issuer, String phoneNumber) {
      return addClaim(issuer, ClaimType.OTHER_PHONE, phoneNumber);
   }

   /**
    * Adds the given phone number to this identity.
    *
    * @param phoneNumber the phone number to be added.
    */
   public IdentityBuilder addOtherPhone(String phoneNumber) {
      return addOtherPhone(null, phoneNumber);
   }

   /**
    * Adds the given postal code to this identity.
    *
    * @param issuer the issuer of the postal code to be added.
    * @param postalCode the value of the postal code to be added.
    */
   public IdentityBuilder addPostalCode(String issuer, String postalCode) {
      return addClaim(issuer, ClaimType.POSTAL_CODE, postalCode);
   }

   /**
    * Adds the given postal code to this identity.
    *
    * @param postalCode the value of the postal code to be added.
    */
   public IdentityBuilder addPostalCode(String postalCode) {
      return addPostalCode(null, postalCode);
   }

   /**
    * Adds the given serial number to this identity.
    *
    * @param issuer the issuer of the serial number to be added.
    * @param number the serial number to be added.
    */
   public IdentityBuilder addSerialNumber(String issuer, Object number) {
      return addClaim(issuer, ClaimType.SERIAL_NUMBER, number);
   }

   /**
    * Adds the given serial number to this identity.
    *
    * @param number the serial number to be added.
    */
   public IdentityBuilder addSerialNumber(Object number) {
      return addSerialNumber(null, number);
   }

   /**
    * Adds the given state/province to this identity.
    *
    * @param issuer the issuer of the state/province to be added.
    * @param province the value of the state/province to be added.
    */
   public IdentityBuilder addStateOrProvince(String issuer, Object province) {
      return addClaim(issuer, ClaimType.STATE_OR_PROVINCE, province);
   }

   /**
    * Adds the given state/province to this identity.
    *
    * @param province the value of the state/province to be added.
    */
   public IdentityBuilder addStateOrProvince(Object province) {
      return addStateOrProvince(null, province);
   }

   /**
    * Adds the given street address to this identity.
    *
    * @param issuer the issuer of the street address to be added.
    * @param address the value of the street address to be added.
    */
   public IdentityBuilder addStreetAddress(String issuer, String address) {
      return addClaim(issuer, ClaimType.STREET_ADDRESS, address);
   }

   /**
    * Adds the given street address to this identity.
    *
    * @param address the value of the street address to be added.
    */
   public IdentityBuilder addStreetAddress(String address) {
      return addStreetAddress(null, address);
   }

   /**
    * Adds the given surname to this identity.
    *
    * @param issuer the issuer of the surname to be added.
    * @param surname the surname to be added.
    */
   public IdentityBuilder addSurname(String issuer, String surname) {
      return addClaim(issuer, ClaimType.SURNAME, surname);
   }

   /**
    * Adds the given surname to this identity.
    *
    * @param surname the surname to be added.
    */
   public IdentityBuilder addSurname(String surname) {
      return addSurname(null, surname);
   }

   /**
    * Adds the given thumb print to this identity.
    *
    * @param issuer the issuer of the thumb print to be added.
    * @param thumbPrint the value of the thumb print to be added.
    */
   public IdentityBuilder addThumbPrint(String issuer, Object thumbPrint) {
      return addClaim(issuer, ClaimType.THUMBPRINT, thumbPrint);
   }

   /**
    * Adds the given thumb print to this identity.
    *
    * @param thumbPrint the value of the thumb print to be added.
    */
   public IdentityBuilder addThumbPrint(Object thumbPrint) {
      return addThumbPrint(null, thumbPrint);
   }

   /**
    * Adds the given version to this identity.
    *
    * @param issuer the issuer of the version to be added.
    * @param version the value of the version to be added.
    */
   public IdentityBuilder addVersion(String issuer, Object version) {
      return addClaim(issuer, ClaimType.VERSION, version);
   }

   /**
    * Adds the given version to this identity.
    *
    * @param version the value of the version to be added.
    */
   public IdentityBuilder addVersion(Object version) {
      return addVersion(null, version);
   }

   /**
    * Adds the given web page to this identity.
    *
    * @param issuer the issuer of the web page to be added.
    * @param url the URL of the web page to be added.
    */
   public IdentityBuilder addWebPage(String issuer, URL url) {
      return addClaim(issuer, ClaimType.WEBPAGE, url);
   }

   /**
    * Adds the given web page to this identity.
    *
    * @param url the URL of the web page to be added.
    */
   public IdentityBuilder addWebPage(URL url) {
      return addWebPage(null, url);
   }

   /**
    * Adds the given web page to this identity.
    *
    * @param issuer the issuer of the country to be added.
    * @param url the URL of the web page to be added.
    */
   public IdentityBuilder addWebPage(String issuer, String url) throws MalformedURLException {
      return addWebPage(issuer, new URL(url));
   }

   /**
    * Adds the given web page to this identity.
    *
    * @param url the URL of the web page to be added.
    */
   public IdentityBuilder addWebPage(String url) throws MalformedURLException {
      return addWebPage(null, url);
   }

   /**
    * Adds the given work phone number to this identity.
    *
    * @param issuer the issuer of the phone number to be added.
    * @param phoneNumber the phone number to be added.
    */
   public IdentityBuilder addWorkPhone(String issuer, String phoneNumber) {
      return addClaim(issuer, ClaimType.WORK_PHONE, phoneNumber);
   }

   /**
    * Adds the given work phone number to this identity.
    *
    * @param phoneNumber the phone number to be added.
    */
   public IdentityBuilder addWorkPhone(String phoneNumber) {
      return addWorkPhone(null, phoneNumber);
   }

   private static boolean fullEquals(IIdentity i1, IIdentity i2) {
      return equalCollections(i1.claims(), i2.claims());
   }

   private static boolean claimEquals(IIdentity i1, IIdentity i2, String issuer, String claimType) {
      Objects.requireNonNull(claimType);
      Collection<Claim> c1 = filtered(i1.claims(), issuer, claimType);
      Collection<Claim> c2 = filtered(i2.claims(), issuer, claimType);
      return equalCollections(c1, c2);
   }

   private static Collection<Claim> filtered(Collection<Claim> source, String issuer,
      String claimType) {
      return source.stream()
               .filter(c -> claimType.equals(c.getType())
                        && (issuer == null || issuer.equals(c.getIssuer())))
               .collect(Collectors.toList());
   }

   private static boolean equalCollections(Collection<Claim> c1, Collection<Claim> c2) {
      return c1.stream().filter(c -> !c2.contains(c)).count() == 0
               && c2.stream().filter(c -> !c1.contains(c)).count() == 0;
   }
}
