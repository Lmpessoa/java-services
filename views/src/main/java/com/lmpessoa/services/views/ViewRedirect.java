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
package com.lmpessoa.services.views;

import java.time.ZonedDateTime;

import com.lmpessoa.services.DateHeader;
import com.lmpessoa.services.hosting.ConnectionInfo;
import com.lmpessoa.services.hosting.Headers;
import com.lmpessoa.services.hosting.HttpResponse;
import com.lmpessoa.services.hosting.ValuesMap;
import com.lmpessoa.services.internal.ValuesMapBuilder;

final class ViewRedirect implements HttpResponse {

   private final String userSessionToken;
   private final String url;

   private ConnectionInfo connection;

   public ViewRedirect(String url, String userSessionToken) {
      this.userSessionToken = userSessionToken;
      this.url = url == null ? "/" : url;
   }

   @Override
   public int getStatusCode() {
      return 302;
   }

   @Override
   public ValuesMap getHeaders() {
      String result = url;
      if (result.startsWith("/") && connection != null) {
         int port = connection.getServerPort();
         boolean secure = connection.isSecure();
         int defaultPort = secure ? 443 : 80;
         StringBuilder resultUrl = new StringBuilder();
         resultUrl.append("http");
         if (secure) {
            resultUrl.append('s');
         }
         resultUrl.append("://");
         resultUrl.append(connection.getServerName());
         if (port != defaultPort) {
            resultUrl.append(':');
            resultUrl.append(port);
         }
         resultUrl.append(result);
         result = resultUrl.toString();
      }
      ValuesMapBuilder builder = new ValuesMapBuilder();
      builder.add(Headers.LOCATION, result);
      StringBuilder cookie = new StringBuilder();
      cookie.append(ViewResponder.SESSION_COOKIE);
      cookie.append('=');
      if (userSessionToken != null) {
         cookie.append(userSessionToken);
         if (ViewResponder.expiration != null) {
            cookie.append("; Expires=");
            ZonedDateTime expires = ZonedDateTime.now().plus(ViewResponder.expiration);
            cookie.append(DateHeader.RFC_7231_DATE_TIME.format(expires));
            cookie.append("; Max-Age=");
            cookie.append((int) ViewResponder.expiration.toMillis() / 1000);
         }
         cookie.append("; Same-Site=Strict");
      } else {
         cookie.append("deleted; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
      }
      builder.add(Headers.SET_COOKIE, cookie.toString());
      return builder.build();
   }

   @Override
   public void setConnectionInfo(ConnectionInfo connection) {
      this.connection = connection;
   }
}
