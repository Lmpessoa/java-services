/*
 * Copyright (c) 2017 Leonardo Pessoa
 * http://github.com/lmpessoa/java-services
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
package com.lmpessoa.services.hosting;

import java.io.IOException;

import org.junit.Test;

import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.hosting.HttpRequestImpl;

public final class HttpRequestTest {

   private HttpRequest getRequest(String filename) throws IOException {
      return new HttpRequestImpl(this.getClass().getResourceAsStream("/http/" + filename));
   }

   @Test
   public void testSimpleGetRequest() throws IOException {
      HttpRequest request = getRequest("simple_get_request.txt");
      assert "GET".equals(request.getMethod());
      assert "/path/file.html".equals(request.getPath());
      assert "someuser@jmarshall.com".equals(request.getHeaders().get("From"));
   }

   @Test
   public void testQueryGetRequest() throws IOException {
      HttpRequest request = getRequest("query_get_request.txt");
      assert "GET".equals(request.getMethod());
      assert "/b/ss/rsid/0".equals(request.getPath());
      assert "apps.sillystring.com/summary.do".equals(request.getQuery().get("g"));
   }

   @Test
   public void testHostedGetRequest() throws IOException {
      HttpRequest request = getRequest("hosted_get_request.txt");
      assert "GET".equals(request.getMethod());
      assert "/path/file.html".equals(request.getPath());
      assert "https://lmpessoa.com".equals(request.getHost());
   }

   @Test
   public void testFormPostRequest() throws IOException {
      HttpRequest request = getRequest("form_post_request.txt");
      assert "POST".equals(request.getMethod());
      assert "/path/script.cgi".equals(request.getPath());
      assert "flies".equals(request.getForm().get("favorite flavor"));
   }

   @Test
   public void testJsonPostRequest() throws IOException {
      HttpRequest request = getRequest("json_put_request.txt");
      assert "PUT".equals(request.getMethod());
      assert "/api/2.2/auth/signin".equals(request.getPath());
      assert "application/json".equals(request.getContentType());
      assert request.getBody().available() == 122;
   }

   @Test
   public void testCookiesGetRequest() throws IOException {
      HttpRequest request = getRequest("cookies_get_request.txt");
      assert "GET".equals(request.getMethod());
      assert "/".equals(request.getPath());
      assert "false".equals(request.getCookies().get("toggle"));

   }
}
