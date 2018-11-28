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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;

import org.junit.Test;

import com.lmpessoa.services.hosting.Headers;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.internal.hosting.HttpRequestBuilder;
import com.lmpessoa.services.security.Claim;
import com.lmpessoa.services.security.IIdentity;
import com.lmpessoa.services.security.IIdentityProvider;
import com.lmpessoa.services.security.IdentityBuilder;

public class IdentityProviderTest {

   @Test
   public void testTokensSameUser() {
      String t1 = IIdentityProvider.generateTokenFor("lmpessoa");
      String t2 = IIdentityProvider.generateTokenFor("lmpessoa");
      assertEquals(45, t1.length());
      assertEquals(45, t2.length());
      assertNotEquals(t1, t2);
      assertEquals(t1.substring(0, 8), t2.substring(0, 8));
   }

   @Test
   public void testTokensDifferentUsers() {
      String t1 = IIdentityProvider.generateTokenFor("lmpessoa");
      String t2 = IIdentityProvider.generateTokenFor("sh_rt");
      assertEquals(45, t1.length());
      assertEquals(45, t2.length());
      assertNotEquals(t1, t2);
      assertNotEquals(t1.substring(0, 8), t2.substring(0, 8));
   }

   @Test
   public void testIdentityProvider() throws IOException {
      IIdentityProvider provider = new TestIdentityProvider();
      String token = IIdentityProvider.generateTokenFor("lmpessoa");
      HttpRequest request = new HttpRequestBuilder() //
               .addHeader(Headers.AUTHORIZATION, token)
               .build();
      IIdentity identity = provider.getIdentity(request);
      assertEquals("Token", identity.claims("test:claim:type") //
               .stream()
               .map(Claim::getValue)
               .findFirst()
               .orElse(null));
      assertEquals(token, identity.claims("test:claim:token") //
               .stream()
               .map(Claim::getValue)
               .findFirst()
               .orElse(null));
   }

   @Test
   public void testIdentityProviderOtherName() throws IOException {
      IIdentityProvider provider = new TestIdentityProvider();
      String token = IIdentityProvider.generateTokenFor("lmpessoa");
      HttpRequest request = new HttpRequestBuilder() //
               .addHeader(Headers.AUTHORIZATION, "Key " + token)
               .build();
      IIdentity identity = provider.getIdentity(request);
      assertEquals("Key", identity.claims("test:claim:type") //
               .stream()
               .map(Claim::getValue)
               .findFirst()
               .orElse(null));
      assertEquals(token, identity.claims("test:claim:token") //
               .stream()
               .map(Claim::getValue)
               .findFirst()
               .orElse(null));
   }

   public static class TestIdentityProvider implements IIdentityProvider {

      @Override
      public IIdentity getIdentity(String format, String token) {
         return new IdentityBuilder() //
                  .addClaim("test:claim:type", format)
                  .addClaim("test:claim:token", token)
                  .build();
      }
   }
}
