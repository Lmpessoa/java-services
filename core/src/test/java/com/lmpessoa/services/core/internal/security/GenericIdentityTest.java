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
package com.lmpessoa.services.core.internal.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.security.ClaimType;
import com.lmpessoa.services.core.security.GenericIdentity;

public class GenericIdentityTest {

   private GenericIdentity identity;

   @Before
   public void setup() {
      identity = new GenericIdentity();
   }

   @Test
   public void testNameFromName() {
      identity.addClaim(ClaimType.NAME, "Jane Doe");
      identity.addClaim(ClaimType.DISPLAY_NAME, "John Doe");
      assertEquals("Jane Doe", identity.getName());
      assertTrue(identity.hasClaim(ClaimType.DISPLAY_NAME));
   }

   @Test
   public void testNameFromDisplayName() {
      identity.addClaim(ClaimType.DISPLAY_NAME, "Jane Doe");
      identity.addClaim(ClaimType.GIVEN_NAME, "John");
      identity.addClaim(ClaimType.SURNAME, "Doe");
      assertEquals("Jane Doe", identity.getName());
      assertTrue(identity.hasClaim(ClaimType.GIVEN_NAME));
      assertTrue(identity.hasClaim(ClaimType.SURNAME));
   }

   @Test
   public void testNameFromGivenName() {
      identity.addClaim(ClaimType.GIVEN_NAME, "Jane");
      identity.addClaim(ClaimType.SURNAME, "Doe");
      identity.addClaim(ClaimType.ACCOUNT_NAME, "JDoe");
      assertEquals("Jane Doe", identity.getName());
      assertTrue(identity.hasClaim(ClaimType.ACCOUNT_NAME));
   }

   @Test
   public void testNameFromAccountName() {
      identity.addClaim(ClaimType.ACCOUNT_NAME, "JaneDoe");
      identity.addClaim(ClaimType.EMAIL, "jane.doe@example.org");
      assertEquals("JaneDoe", identity.getName());
      assertTrue(identity.hasClaim(ClaimType.EMAIL));
   }

   @Test
   public void testRoleThroughRole() {
      identity.addRole("test");
      assertTrue(identity.hasRole("test"));
   }

   @Test
   public void testRoleThroughClaim() {
      identity.addClaim(ClaimType.ROLE, "test");
      assertTrue(identity.hasRole("test"));
   }

   @Test
   public void testGetRoles() {
      identity.addRole("foo");
      identity.addRole("bar");
      String[] roles = identity.getAllClaims(ClaimType.ROLE)
               .stream()
               .map(c -> c.getValue().toString())
               .toArray(String[]::new);
      assertArrayEquals(new String[] { "foo", "bar" }, roles);
   }
}
