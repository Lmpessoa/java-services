/*
 * Copyright (c) 2017 Leonardo Pessoa
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
package com.lmpessoa.services.core.hosting;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.core.hosting.HttpRequest;
import com.lmpessoa.services.core.hosting.HttpRequestImpl;
import com.lmpessoa.services.core.hosting.LengthRequiredException;
import com.lmpessoa.services.core.hosting.PayloadTooLargeException;

public final class HttpRequestTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private HttpRequest getRequest(String filename) throws IOException {
      return new HttpRequestImpl(this.getClass().getResourceAsStream("/http/" + filename));
   }

   @Test
   public void testSimpleGetRequest() throws IOException {
      HttpRequest request = getRequest("simple_get_request.txt");
      assertEquals("GET", request.getMethod());
      assertEquals("/path/file.html", request.getPath());
      assertEquals("someuser@jmarshall.com", request.getHeader("From"));
   }

   @Test
   public void testQueryGetRequest() throws IOException {
      HttpRequest request = getRequest("query_get_request.txt");
      assertEquals("GET", request.getMethod());
      assertEquals("/b/ss/rsid/0", request.getPath());
      assertEquals(Arrays.asList("apps.sillystring.com/summary.do"), request.getQuery().get("g"));
      assertEquals(Arrays.asList("http://apps.sillystring.com/summary.do"), request.getQuery().get("r"));
      assertEquals(Arrays.asList("2009-03-05T01:00:01-05"), request.getQuery().get("ts"));
   }

   @Test
   public void testHostedGetRequest() throws IOException {
      HttpRequest request = getRequest("hosted_get_request.txt");
      assertEquals("GET", request.getMethod());
      assertEquals("/path/file.html", request.getPath());
      assertEquals("https://lmpessoa.com", request.getHeader("Host"));
   }

   @Test
   public void testJsonPutRequest() throws IOException {
      HttpRequest request = getRequest("json_put_request.txt");
      assertEquals("PUT", request.getMethod());
      assertEquals("/api/2.2/auth/signin", request.getPath());
      assertEquals("application/json", request.getContentType());
      assertEquals(129, request.getBody().available());
   }

   @Test
   public void testWrongPutRequest() throws IOException {
      thrown.expect(LengthRequiredException.class);
      getRequest("wrong_put_request.txt");
   }

   @Test
   public void testPayloadTooLargeRequest() throws IOException {
      thrown.expect(PayloadTooLargeException.class);
      getRequest("large_payload_request.txt");
   }
}
