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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.lmpessoa.services.core.MediaType;

final class HttpRequestImpl implements HttpRequest {

   private static final String UTF8 = "UTF-8";

   private Map<String, String> headers = new HashMap<>();
   private Map<String, String> cookies;
   private Map<String, String> query;
   private Map<String, String> form;
   private String queryString;
   private String protocol;
   private String method;
   private byte[] body;
   private String path;

   HttpRequestImpl(InputStream clientStream) throws IOException {
      StringBuilder request = new StringBuilder();
      String line;
      line = readLine(clientStream);
      String[] parts = line.split(" ");
      this.method = parts[0];
      this.protocol = parts[2];
      parts = parts[1].split("\\?", 2);
      this.path = parts[0];
      if (path.startsWith("http://") || path.startsWith("https://")) {
         int index = path.indexOf('/', path.indexOf("://") + 3);
         headers.put("Host", path.substring(0, index));
         path = path.substring(index);
      }
      this.queryString = parts.length > 1 ? parts[1] : null;
      request.append(line + "\n");
      while ((line = readLine(clientStream)) != null) {
         if (line.isEmpty()) {
            break;
         }
         String[] head = line.split(":", 2);
         if (head.length != 2) {
            throw new IllegalStateException("Unrecognised header value: " + line);
         }
         headers.put(head[0].trim(), head[1].trim());
         request.append(line + "\n");
      }
      headers = Collections.unmodifiableMap(headers);
      if (getContentLength() > Integer.MAX_VALUE) {
         throw new UnsupportedOperationException("Body content too large");
      }
      if (clientStream.available() > 0) {
         body = new byte[clientStream.available()];
         clientStream.read(body);
      } else {
         body = new byte[0];
      }
   }

   @Override
   public String getMethod() {
      return method;
   }

   @Override
   public String getPath() {
      return path;
   }

   @Override
   public String getProtocol() {
      return protocol;
   }

   @Override
   public String getQueryString() {
      return queryString;
   }

   @Override
   public long getContentLength() {
      String length = headers.get("Content-Length");
      return length != null ? Long.parseLong(length) : 0;
   }

   @Override
   public String getContentType() {
      return headers.get("Content-Type");
   }

   @Override
   public String getHost() {
      return headers.get("Host");
   }

   @Override
   public InputStream getBody() {
      return new ByteArrayInputStream(body);
   }

   @Override
   public Map<String, String> getHeaders() {
      return headers;
   }

   @Override
   public Map<String, String> getQuery() {
      if (query == null && queryString != null) {
         final Map<String, String> result = new HashMap<>();
         Arrays.asList(queryString.split("&"))
                  .stream() //
                  .map(s -> s.split("=", 2)) //
                  .forEach(s -> {
                     try {
                        result.put(s[0], URLDecoder.decode(s[1], UTF8));
                     } catch (UnsupportedEncodingException e) {
                        throw new AssertionError(e);
                     }
                  });
         query = Collections.unmodifiableMap(result);
      }
      return query;
   }

   @Override
   public Map<String, String> getForm() {
      if (form == null && MediaType.FORM.equals(getContentType())) {
         Map<String, String> result = new HashMap<>();
         Arrays.asList(new String(body).split("&"))
                  .stream() //
                  .map(s -> s.split("=", 2)) //
                  .forEach(s -> {
                     try {
                        result.put(URLDecoder.decode(s[0], UTF8), URLDecoder.decode(s[1], UTF8));
                     } catch (UnsupportedEncodingException e) {
                        throw new AssertionError(e);
                     }
                  });
         form = Collections.unmodifiableMap(result);
      }
      return form;
   }

   @Override
   public Map<String, String> getCookies() {
      if (cookies == null && headers.containsKey("Cookie")) {
         Map<String, String> result = new HashMap<>();
         String tmp = headers.get("Cookie");
         Arrays.asList(tmp.split(";"))
                  .stream() //
                  .map(s -> s.split("=", 2)) //
                  .forEach(s -> result.put(s[0].trim(), s[1].trim()));
         this.cookies = Collections.unmodifiableMap(result);
      }
      return cookies;
   }

   private String readLine(InputStream input) throws IOException {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      int b;
      while ((b = input.read()) > -1) {
         if (b == '\n') {
            break;
         } else if (b != '\r') {
            result.write(new byte[] { (byte) b });
         }
      }
      return new String(result.toByteArray()).trim();
   }
}
