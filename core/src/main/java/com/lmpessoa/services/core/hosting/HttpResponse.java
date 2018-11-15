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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.lmpessoa.services.util.logging.ILogger;

final class HttpResponse implements HttpServletResponse {

   private static final String CONTENT_TYPE = "Content-Type";
   private static final DateTimeFormatter RFC_1123_US = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US);
   private static final Map<Integer, String> STATUSES = new HashMap<>();
   private static final String CRLF = "\r\n";

   static {
      try {
         final URI uri = HttpResponse.class.getResource("/status.codes").toURI();
         List<String> statuses = Files.readAllLines(Paths.get(uri));
         for (String line : statuses) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
               try { // NOSONAR
                  int code = Integer.parseInt(parts[0]);
                  STATUSES.put(code, parts[1]);
               } catch (NumberFormatException e) {
                  // Ignore
               }
            }
         }
      } catch (IOException | URISyntaxException e) {
         // Any error here is treated as fatal
         e.printStackTrace(); // NOSONAR
         System.exit(1);
      }
   }

   private final ByteArrayOutputStream body = new ByteArrayOutputStream();
   private final Map<String, List<String>> headers = new HashMap<>();
   private final OutputStream output;

   private Charset encoding = Charset.forName("UTF-8");
   private List<Cookie> cookies = new ArrayList<>();
   private ServletOutputStream bodyStream = null;
   private boolean commited = false;
   private int status = 200;

   @Override
   public int getStatus() {
      return status;
   }

   @Override
   public void setStatus(int status) {
      this.status = status;
   }

   @Override
   public String getContentType() {
      return getHeader(CONTENT_TYPE);
   }

   @Override
   public void setContentType(String type) {
      String[] parts = type.split(";");
      setHeader(CONTENT_TYPE, parts[0]);
      for (int i = 1; i < parts.length; ++i) {
         if (parts[i].trim().startsWith("charset=")) {
            String[] charset = parts[i].trim().split("=", 2);
            setCharacterEncoding(charset[1]);
         }
      }
   }

   @Override
   public void setContentLength(int length) {
      // Ignore; correct content length comes from body
   }

   @Override
   public void setContentLengthLong(long length) {
      // Ignore; correct content length comes from body
   }

   @Override
   public String getCharacterEncoding() {
      return encoding.name();
   }

   @Override
   public void setCharacterEncoding(String encoding) {
      this.encoding = Charset.forName(encoding);
   }

   @Override
   public String getHeader(String name) {
      if (headers.containsKey(name)) {
         return headers.get(name).get(0);
      }
      return null;
   }

   @Override
   public Collection<String> getHeaders(String name) {
      if (headers.containsKey(name)) {
         return Collections.unmodifiableCollection(headers.get(name));
      }
      return null;
   }

   @Override
   public void addHeader(String name, String value) {
      if (name == null || name.length() == 0 || value == null) {
         return;
      }
      if (!headers.containsKey(name)) {
         headers.put(name, new ArrayList<>());
      }
      headers.get(name).add(value);
   }

   @Override
   public void setHeader(String name, String value) {
      if (name == null || name.length() == 0 || value == null) {
         return;
      }
      if (!headers.containsKey(name)) {
         addHeader(name, value);
      } else {
         headers.get(name).set(0, value);
      }
   }

   @Override
   public boolean containsHeader(String name) {
      return headers.containsKey(name);
   }

   @Override
   public void addIntHeader(String name, int value) {
      addHeader(name, String.valueOf(value));
   }

   @Override
   public void setIntHeader(String name, int value) {
      setHeader(name, String.valueOf(value));
   }

   @Override
   public void addDateHeader(String name, long value) {
      ZonedDateTime time = Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC);
      addHeader(name, RFC_1123_US.format(time));
   }

   @Override
   public void setDateHeader(String name, long value) {
      ZonedDateTime time = Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC);
      setHeader(name, RFC_1123_US.format(time));
   }

   @Override
   public Collection<String> getHeaderNames() {
      return headers.keySet();
   }

   @Override
   public void addCookie(Cookie cookie) {
      cookies.add(cookie);
   }

   @Override
   public ServletOutputStream getOutputStream() throws IOException {
      if (bodyStream == null) {
         bodyStream = new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
               body.write(b);
            }

            @Override
            public void setWriteListener(WriteListener listener) {
               throw new UnsupportedOperationException();
            }

            @Override
            public boolean isReady() {
               return true;
            }
         };
      }
      return bodyStream;
   }

   @Override
   public void reset() {
      status = 200;
      headers.clear();
      bodyStream = null;
      body.reset();
   }

   @Override
   public boolean isCommitted() {
      return commited;
   }

   // NOTE: All public inherited methods below are marked as deprecated only to ensure they are not
   // being called by any methods without prior notice and implementation.

   @Override
   public PrintWriter getWriter() throws IOException {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setStatus(int status, String message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sendRedirect(String location) throws IOException {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sendError(int status) throws IOException {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sendError(int status, String message) throws IOException {
      throw new UnsupportedOperationException();
   }

   @Override
   public String encodeURL(String location) {
      throw new UnsupportedOperationException();
   }

   @Override
   public String encodeUrl(String location) {
      throw new UnsupportedOperationException();
   }

   @Override
   public String encodeRedirectURL(String location) {
      throw new UnsupportedOperationException();
   }

   @Override
   public String encodeRedirectUrl(String location) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Locale getLocale() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setLocale(Locale locale) {
      throw new UnsupportedOperationException();
   }

   @Override
   public int getBufferSize() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setBufferSize(int size) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void flushBuffer() throws IOException {
      throw new UnsupportedOperationException();
   }

   @Override
   public void resetBuffer() {
      throw new UnsupportedOperationException();
   }

   HttpResponse(OutputStream output) {
      this.output = Objects.requireNonNull(output);
   }

   void commit(ILogger log) {
      checkIfCommited();
      StringBuilder client = new StringBuilder();
      client.append("HTTP/1.1 ");
      client.append(getStatus());
      client.append(' ');
      client.append(STATUSES.get(getStatus()));
      client.append(CRLF);
      byte[] data = null;
      if (body.size() > 0 && headers.containsKey(CONTENT_TYPE)) {
         data = body.toByteArray();
         setIntHeader("Content-Length", data.length);
      }
      for (Entry<String, List<String>> header : headers.entrySet()) {
         if (!header.getKey().startsWith("Content-") || data != null) {
            for (String value : header.getValue()) {
               if (value.startsWith("text/")) {
                  value += "; charset=" + encoding.name().toLowerCase();
               }
               client.append(String.format("%s: %s", header.getKey(), value));
               client.append(CRLF);
            }
         }
      }

      // TODO: Output cookies
      client.append(CRLF);
      try {
         output.write(client.toString().getBytes());
         if (data != null) {
            output.write(data);
         }
         output.flush();
         commited = true;
      } catch (IOException e) {
         log.error(e);
      }
   }

   private void checkIfCommited() {
      if (isCommitted()) {
         throw new IllegalStateException("This response has already been commited");
      }
   }
}
