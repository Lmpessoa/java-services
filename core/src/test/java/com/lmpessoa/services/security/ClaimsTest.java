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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ClaimsTest {

   @Test(expected = IllegalArgumentException.class)
   public void testInvalidClaimType() {
      new Claim("claim.type", 12);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTooShortClaimType() {
      new Claim("claim:type", 12);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testMissingMandatoryClaimTypeSegment() {
      new Claim("com:lmpessoa:type", 12);
   }

   @Test
   public void testValidClaimType() {
      new Claim("com:lmpessoa:claim:type", 12);
   }

   @Test
   public void testCompareClaimsDifferent() {
      Claim c1 = new Claim(ClaimType.ACCOUNT_NAME, "JDoe");
      Claim c2 = new Claim(ClaimType.ACCOUNT_NAME, "jdoe");
      assertFalse(c1.equals(c2));
      assertFalse(c2.equals(c1));
   }

   @Test
   public void testCompareClaimsEqual() {
      Claim c1 = new Claim(ClaimType.ACCOUNT_NAME, "JDoe");
      Claim c2 = new Claim(ClaimType.ACCOUNT_NAME, "JDoe");
      assertNotSame(c1, c2);
      assertTrue(c1.equals(c2));
      assertTrue(c2.equals(c1));
   }

   @Test
   public void testEqualIssuersBothNull() {
      Claim c1 = new Claim(ClaimType.ACCOUNT_NAME, "JDoe");
      Claim c2 = new Claim(ClaimType.ACCOUNT_NAME, "jdoe");
      assertTrue(Claim.equalIssuers(c1, c2));
   }

   @Test
   public void testEqualIssuersOneNull() {
      Claim c1 = new Claim("lmpessoa", ClaimType.ACCOUNT_NAME, "JDoe");
      Claim c2 = new Claim(ClaimType.ACCOUNT_NAME, "jdoe");
      assertFalse(Claim.equalIssuers(c1, c2));
   }

   @Test
   public void testEqualIssuersBothDifferent() {
      Claim c1 = new Claim("lmpessoa", ClaimType.ACCOUNT_NAME, "JDoe");
      Claim c2 = new Claim("sh_rt", ClaimType.ACCOUNT_NAME, "jdoe");
      assertFalse(Claim.equalIssuers(c1, c2));
   }

   @Test
   public void testEqualIssuersBothEquals() {
      Claim c1 = new Claim("lmpessoa", ClaimType.ACCOUNT_NAME, "JDoe");
      Claim c2 = new Claim("lmpessoa", ClaimType.ACCOUNT_NAME, "jdoe");
      assertTrue(Claim.equalIssuers(c1, c2));
   }
}
