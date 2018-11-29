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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class IdentityBuilderTest {

   private IdentityBuilder builder;
   private IIdentity identity;

   @Before
   public void setup() {
      builder = new IdentityBuilder();
   }

   @Test
   public void testNameFromName() {
      identity = builder //
               .addName("John Doe")
               .addDisplayName("Jane Doe")
               .build();
      assertEquals("Jane Doe", identity.getDisplayName());
      assertTrue(identity.hasClaim(ClaimType.DISPLAY_NAME));
   }

   @Test
   public void testNameFromDisplayName() {
      identity = builder //
               .addDisplayName("Jane Doe")
               .addGivenName("John")
               .addSurname("Doe")
               .build();
      assertEquals("Jane Doe", identity.getDisplayName());
      assertTrue(identity.hasClaim(ClaimType.GIVEN_NAME));
      assertTrue(identity.hasClaim(ClaimType.SURNAME));
   }

   @Test
   public void testNameFromGivenName() {
      identity = builder //
               .addGivenName("Jane")
               .addSurname("Doe")
               .addAccountName("JDoe")
               .build();
      assertEquals("Jane Doe", identity.getDisplayName());
      assertTrue(identity.hasClaim(ClaimType.ACCOUNT_NAME));
   }

   @Test
   public void testNameFromAccountName() {
      identity = builder //
               .addAccountName("JaneDoe")
               .addEmail("jane.doe@example.org")
               .build();
      assertEquals("JaneDoe", identity.getDisplayName());
      assertTrue(identity.hasClaim(ClaimType.EMAIL));
   }

   @Test
   public void testRoleFromRole() {
      identity = builder.addRole("test").build();
      assertTrue(identity.hasRole("test"));
   }

   @Test
   public void testRoleFromClaim() {
      identity = builder.addClaim(ClaimType.ROLE, "test").build();
      assertTrue(identity.hasRole("test"));
   }

   @Test
   public void testGetRoles() {
      identity = builder //
               .addRole("foo")
               .addRole("bar")
               .build();
      assertTrue(identity.hasRole("foo"));
      assertTrue(identity.hasRole("bar"));
      String[] roles = identity.claims()
               .stream()
               .filter(c -> c.getType().equals(ClaimType.ROLE))
               .map(c -> c.getValue().toString())
               .toArray(String[]::new);
      assertArrayEquals(new String[] { "foo", "bar" }, roles);
      roles = identity.claims(ClaimType.ROLE) //
               .stream()
               .map(c -> c.getValue().toString())
               .toArray(String[]::new);
      assertArrayEquals(new String[] { "foo", "bar" }, roles);
   }

   @Test
   public void testCompareIdentityDefaultMatch() {
      identity = builder //
               .addAccountName("jdoe")
               .addRoles(new String[] { "admin", "manager" })
               .build();
      IIdentity id2 = builder //
               .addRoles(new String[] { "manager", "admin" })
               .addAccountName("jdoe")
               .build();
      assertNotSame(identity, id2);
      assertEquals(identity, id2);
   }

   @Test
   public void testCompareIdentityDefaultFailedMissingOne() {
      identity = builder //
               .addRoles(new String[] { "admin", "manager" })
               .build();
      IIdentity id2 = builder //
               .addRoles(new String[] { "manager", "admin" })
               .addAccountName("jdoe")
               .build();
      assertNotSame(identity, id2);
      assertNotEquals(identity, id2);
   }

   @Test
   public void testCompareIdentityDefaultFailedMissingOther() {
      identity = builder //
               .addAccountName("jdoe")
               .addRoles(new String[] { "admin", "manager" })
               .build();
      IIdentity id2 = builder //
               .addRoles(new String[] { "manager", "admin" })
               .build();
      assertNotSame(identity, id2);
      assertNotEquals(identity, id2);
   }

   @Test
   public void testCompareIdentityDefaultFailedDifferentValues() {
      identity = builder //
               .addAccountName("jdoe")
               .addRoles(new String[] { "admin", "manager" })
               .build();
      IIdentity id2 = builder //
               .addRoles(new String[] { "manager", "admin" })
               .addAccountName("Jdoe")
               .build();
      assertNotSame(identity, id2);
      assertNotEquals(identity, id2);
   }

   @Test
   public void testCompareIdentityClaimMatch() {
      identity = builder //
               .compareUsing(ClaimType.ACCOUNT_NAME)
               .addAccountName("jdoe")
               .addRoles(new String[] { "admin", "editor" })
               .build();
      IIdentity id2 = builder //
               .compareUsing(ClaimType.ACCOUNT_NAME)
               .addRoles(new String[] { "manager", "admin" })
               .addAccountName("jdoe")
               .build();
      assertNotSame(identity, id2);
      assertEquals(identity, id2);
   }

   @Test
   public void testCompareIdentityClaimFailedMissing() {
      identity = builder //
               .compareUsing(ClaimType.ACCOUNT_NAME)
               .addAccountName("jdoe")
               .addRoles(new String[] { "admin", "editor" })
               .build();
      IIdentity id2 = builder //
               .compareUsing(ClaimType.ACCOUNT_NAME)
               .addRoles(new String[] { "manager", "admin" })
               .build();
      assertNotSame(identity, id2);
      assertNotEquals(identity, id2);
   }

   @Test
   public void testCompareIdentityClaimFailedDifferentValues() {
      identity = builder //
               .compareUsing(ClaimType.ACCOUNT_NAME)
               .addAccountName("jdoe")
               .addRoles(new String[] { "admin", "manager" })
               .build();
      IIdentity id2 = builder //
               .compareUsing(ClaimType.ACCOUNT_NAME)
               .addRoles(new String[] { "manager", "admin" })
               .addAccountName("Jdoe")
               .build();
      assertNotSame(identity, id2);
      assertNotEquals(identity, id2);
   }

   @Test
   public void testCompareIdentityCustomMatch() {
      identity = builder //
               .compareUsing((i1, i2) -> {
                  Claim c1 = i1.claims(ClaimType.ACCOUNT_NAME).stream().findFirst().orElse(null);
                  Claim c2 = i2.claims(ClaimType.ACCOUNT_NAME).stream().findFirst().orElse(null);
                  return c1 != null && c2 != null
                           && c1.getValue().toString().equalsIgnoreCase(c2.getValue().toString());
               })
               .addAccountName("jdoe")
               .addRoles(new String[] { "admin", "manager" })
               .build();
      IIdentity id2 = builder //
               .compareUsing(ClaimType.ACCOUNT_NAME)
               .addRoles(new String[] { "manager", "admin" })
               .addAccountName("Jdoe")
               .build();
      assertNotSame(identity, id2);
      assertEquals(identity, id2);
   }
}
