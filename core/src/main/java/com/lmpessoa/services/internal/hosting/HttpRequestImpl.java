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
package com.lmpessoa.services.internal.hosting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import com.lmpessoa.services.BadRequestException;
import com.lmpessoa.services.ContentType;
import com.lmpessoa.services.hosting.Headers;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.hosting.ValuesMap;
import com.lmpessoa.services.internal.CoreMessage;
import com.lmpessoa.services.internal.ValuesMapBuilder;
import com.lmpessoa.services.internal.serializing.Serializer;
import com.lmpessoa.services.routing.HttpMethod;

final class HttpRequestImpl implements HttpRequest {

   private final String queryString;
   private final HttpMethod method;
   private final ValuesMap headers;
   private final String protocol;
   private final byte[] content;
   private final long timeout;
   private final String path;

   private ValuesMap query;
   private ValuesMap form;

   @Override
   public HttpMethod getMethod() {
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
   public ValuesMap getQuery() {
      if (query == null) {
         query = Serializer.parseHttpForm(this.queryString);
         if (query == null) {
            query = new ValuesMapBuilder().build();
         }
      }
      return query;
   }

   @Override
   public InputStream getContentBody() {
      if (content != null) {
         return new ByteArrayInputStream(content);
      }
      return null;
   }

   @Override
   public ValuesMap getForm() {
      if (form == null) {
         if (ContentType.FORM.equals(getContentType())) {
            form = Serializer.parseHttpForm(new String(this.content));
         }
         if (form == null) {
            form = new ValuesMapBuilder().build();
         }
      }
      return form;
   }

   @Override
   public ValuesMap getHeaders() {
      return headers;
   }

   @Override
   public String toString() {
      return String.format("%s %s %s", method,
               path + (queryString != null ? "?" + queryString : ""), protocol);
   }

   HttpRequestImpl(InputStream clientStream, int timeout) throws IOException {
      this.timeout = timeout * 1000L;
      String requestLine = readLine(clientStream);
      if (requestLine == null) {
         throw new SocketTimeoutException();
      }
      String[] parts = requestLine.split(" ");
      this.method = HttpMethod.valueOf(parts[0].toUpperCase());
      this.protocol = parts[2];
      parts = parts[1].split("\\?", 2);
      String thePath = parts[0];
      ValuesMapBuilder headerBuilder = new ValuesMapBuilder();
      if (thePath.startsWith("http://") || thePath.startsWith("https://")) {
         int index = thePath.indexOf('/', thePath.indexOf("://") + 3);
         headerBuilder.add(Headers.HOST, thePath.substring(0, index));
         thePath = thePath.substring(index);
      }
      this.path = thePath;
      this.queryString = parts.length > 1 ? parts[1] : null;
      this.headers = extractHeaders(clientStream, headerBuilder);
      if (headers.contains(Headers.CONTENT_TYPE)) {
         if (!headers.contains(Headers.CONTENT_LENGTH)) {
            throw new LengthRequiredException();
         }
         long contentLength = getContentLength();
         if (contentLength < 0 || contentLength > Integer.MAX_VALUE) {
            throw new PayloadTooLargeException();
         }
         byte[] data = new byte[(int) getContentLength()];
         while (clientStream.available() < data.length) {
            // Do nothing, just sit and wait
         }
         int read = clientStream.read(data);
         if (read != data.length) {
            throw new AssertionError("Error reading from client (expected: " + data.length
                     + " bytes, found: " + read + " bytes)");
         }
         this.content = data;
      } else {
         this.content = new byte[0];
      }
   }

   private ValuesMap extractHeaders(InputStream clientStream, ValuesMapBuilder builder)
      throws IOException {
      String headerLine;
      while ((headerLine = readLine(clientStream)) != null && !headerLine.isEmpty()) {
         String[] header = headerLine.split(":", 2);
         if (header.length != 2) {
            throw new BadRequestException(null, null,
                     CoreMessage.ILLEGAL_HEADER_LINE.with(headerLine));
         }
         String headerName = Headers.normalise(header[0]);
         builder.add(headerName, header[1].trim());
      }
      return builder.build();
   }

   private String readLine(InputStream input) throws IOException {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      int[] b = new int[] { 0, 0 };
      if (input.available() == 0) {
         return null;
      }
      while (!Arrays.equals(b, new int[] { '\r', '\n' })) {
         Instant time = Instant.now();
         while (input.available() == 0) {
            if (Duration.between(time, Instant.now()).toMillis() > timeout) {
               throw new SocketTimeoutException();
            }
         }
         b[0] = b[1];
         b[1] = input.read();
         result.write(b[1]);
      }
      return result.toString(StandardCharsets.UTF_8.name()).trim();
   }
}
