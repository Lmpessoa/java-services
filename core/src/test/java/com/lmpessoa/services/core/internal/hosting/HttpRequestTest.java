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
package com.lmpessoa.services.core.internal.hosting;

import static com.lmpessoa.services.core.routing.HttpMethod.GET;
import static com.lmpessoa.services.core.routing.HttpMethod.PUT;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Locale;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.core.ContentType;
import com.lmpessoa.services.core.hosting.Headers;
import com.lmpessoa.services.core.hosting.HttpRequest;

public final class HttpRequestTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private HttpRequest getRequest(String filename) throws IOException {
      return new HttpRequestImpl(this.getClass().getResourceAsStream("/http/" + filename));
   }

   @Test
   public void testSimpleGetRequest() throws IOException {
      HttpRequest request = getRequest("simple_get_request.txt");
      assertEquals(GET, request.getMethod());
      assertEquals("/path/file.html", request.getPath());
      assertEquals("someuser@jmarshall.com", request.getHeaders().get("From"));
      assertEquals("HTTPTool/1.0", request.getHeaders().get(Headers.USER_AGENT));
   }

   @Test
   public void testHostedGetRequest() throws IOException {
      HttpRequest request = getRequest("hosted_get_request.txt");
      assertEquals(GET, request.getMethod());
      assertEquals("/path/file.html", request.getPath());
      assertEquals("https://lmpessoa.com", request.getHeaders().get(Headers.HOST));
      assertEquals("HTTPTool/1.0", request.getHeaders().get(Headers.USER_AGENT));
   }

   @Test
   public void testJsonPutRequest() throws IOException {
      HttpRequest request = getRequest("json_put_request.txt");
      assertEquals(PUT, request.getMethod());
      assertEquals("/api/2.2/auth/signin", request.getPath());
      assertEquals(ContentType.JSON, request.getContentType());
      assertEquals("my-server", request.getHeaders().get(Headers.HOST));
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

   @Test
   public void testAcceptLanguageRequest() throws IOException {
      HttpRequest request = getRequest("accept_lang_request.txt");
      assertEquals(GET, request.getMethod());
      assertEquals("/path/file.html", request.getPath());
      assertEquals("da, de-at; q=0.9, en-gb; q=0.8, en; g=0.7",
               request.getHeaders().get(Headers.ACCEPT_LANGUAGE));
      Locale[] langs = request.getAcceptedLanguages();
      assertEquals(4, langs.length);

      assertEquals("da", langs[0].getLanguage());
      assertEquals("", langs[0].getCountry());

      assertEquals("de", langs[1].getLanguage());
      assertEquals("AT", langs[1].getCountry());

      assertEquals("en", langs[2].getLanguage());
      assertEquals("GB", langs[2].getCountry());

      assertEquals("en", langs[3].getLanguage());
      assertEquals("", langs[3].getCountry());
   }
}
