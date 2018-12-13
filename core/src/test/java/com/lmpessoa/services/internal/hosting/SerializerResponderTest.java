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
package com.lmpessoa.services.internal.hosting;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.ContentType;
import com.lmpessoa.services.NotFoundException;
import com.lmpessoa.services.Redirect;
import com.lmpessoa.services.hosting.ConnectionInfo;
import com.lmpessoa.services.hosting.Headers;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.hosting.HttpResponse;
import com.lmpessoa.services.hosting.NextResponder;
import com.lmpessoa.services.hosting.ValuesMap;
import com.lmpessoa.services.internal.ValuesMapBuilder;
import com.lmpessoa.services.internal.logging.Logger;
import com.lmpessoa.services.logging.ILogger;
import com.lmpessoa.services.logging.NullHandler;
import com.lmpessoa.services.routing.RouteMatch;

public class SerializerResponderTest {

   private static final ConnectionInfo connect = new ConnectionInfo(mock(Socket.class),
            "https://lmpessoa.com/");
   private static final String TEST_URL = "https://lmpessoa.com/test";
   private static final ILogger log = new Logger(new NullHandler());

   private HttpRequest request;

   private SerializerResponder handler;
   private NextResponder next;

   @Before
   public void setup() {
      request = mock(HttpRequest.class);
      when(request.getProtocol()).thenReturn("HTTP/1.1");
      when(request.getAcceptedLanguages()).thenReturn(new Locale[0]);
   }

   @Test
   public void testStringResult() throws IOException {
      next = () -> "success";
      handler = new SerializerResponder(next);
      HttpResponse result = handler.invoke(request, null, connect, log);
      assertEquals(200, result.getStatusCode());
      assertEquals(ContentType.TEXT, result.getContentBody().getType());
      assertEquals("UTF-8", result.getContentBody().getEncoding().name());
      String str = new String(readStreamContent(result.getContentBody()),
               result.getContentBody().getEncoding());
      assertEquals("success", str);
   }

   @Test
   public void testIntResult() throws IOException {
      next = () -> 7;
      handler = new SerializerResponder(next);
      ValuesMap headers = new ValuesMapBuilder().add(Headers.ACCEPT, ContentType.JSON).build();
      when(request.getHeaders()).thenReturn(headers);
      HttpResponse result = handler.invoke(request, null, connect, log);
      assertEquals(200, result.getStatusCode());
      assertEquals(ContentType.JSON, result.getContentBody().getType());
      String str = new String(readStreamContent(result.getContentBody()));
      assertEquals("7", str);
   }

   @Test
   public void testBinaryResult() throws IOException {
      next = () -> new byte[] { 115, 117, 99, 99, 101, 115, 115 };
      handler = new SerializerResponder(next);
      HttpResponse result = handler.invoke(request, null, connect, log);
      assertEquals(200, result.getStatusCode());
      assertEquals(ContentType.BINARY, result.getContentBody().getType());
      String str = new String(readStreamContent(result.getContentBody()));
      assertEquals("success", str);
   }

   @Test
   public void testBinaryResultWithContentType() throws IOException, NoSuchMethodException {
      next = this::getResultWithContentType;
      handler = new SerializerResponder(next);
      RouteMatch route = mock(RouteMatch.class);
      when(route.getMethod())
               .thenReturn(SerializerResponderTest.class.getMethod("getResultWithContentType"));
      HttpResponse result = handler.invoke(request, route, connect, log);
      assertEquals(200, result.getStatusCode());
      assertEquals(ContentType.TEXT, result.getContentBody().getType());
      String str = new String(readStreamContent(result.getContentBody()));
      assertEquals("success", str);
   }

   @ContentType(ContentType.TEXT)
   public byte[] getResultWithContentType() {
      return new byte[] { 115, 117, 99, 99, 101, 115, 115 };
   }

   @Test
   public void testErrorProducingContent() throws IOException {
      handler = new SerializerResponder(null);
      ValuesMap headers = new ValuesMapBuilder().add(Headers.ACCEPT, ContentType.JSON).build();
      when(request.getHeaders()).thenReturn(headers);
      HttpResponse result = handler.invoke(request, null, connect, log);
      assertEquals(500, result.getStatusCode());
      assertEquals(ContentType.TEXT, result.getContentBody().getType());
      String str = new String(readStreamContent(result.getContentBody()));
      assertEquals("java.lang.NullPointerException", str);
   }

   @Test
   public void testRedirectWithUrl() throws IOException {
      next = () -> {
         try {
            return new URL(TEST_URL);
         } catch (MalformedURLException e) {
            // Test, never happen
            return null;
         }
      };
      handler = new SerializerResponder(next);
      HttpResponse result = handler.invoke(request, null, connect, log);
      assertEquals(302, result.getStatusCode());
      assertEquals(TEST_URL, result.getHeaders().get(Headers.LOCATION));
   }

   @Test
   public void testRedirectWithPath() throws IOException {
      next = () -> Redirect.createdAt("/test");
      handler = new SerializerResponder(next);
      HttpResponse result = handler.invoke(request, null, connect, log);
      result.setConnectionInfo(connect);
      assertEquals(201, result.getStatusCode());
      assertEquals(TEST_URL, result.getHeaders().get(Headers.LOCATION));
   }

   @Test
   public void testNotFoundException() throws IOException {
      next = () -> {
         throw new NotFoundException();
      };
      handler = new SerializerResponder(next);
      HttpResponse result = handler.invoke(request, null, connect, log);
      assertEquals(404, result.getStatusCode());
   }

   @Test
   public void testNullPointerException() throws IOException {
      next = () -> {
         throw new NullPointerException();
      };
      handler = new SerializerResponder(next);
      HttpResponse result = handler.invoke(request, null, connect, log);
      assertEquals(500, result.getStatusCode());
      assertEquals(ContentType.TEXT, result.getContentBody().getType());
      String str = new String(readStreamContent(result.getContentBody()));
      assertEquals("java.lang.NullPointerException", str);
   }

   @Test
   public void testNotAcceptableContent() throws IOException {
      next = () -> 7;
      handler = new SerializerResponder(next);
      ValuesMap headers = new ValuesMapBuilder().add(Headers.ACCEPT, ContentType.HTML).build();
      when(request.getHeaders()).thenReturn(headers);
      HttpResponse result = handler.invoke(request, null, connect, log);
      assertEquals(406, result.getStatusCode());
   }

   @Test
   public void testVersionNotSupported() throws IOException {
      when(request.getProtocol()).thenReturn("HTTP/1.7");
      next = () -> 7;
      handler = new SerializerResponder(next);
      HttpResponse result = handler.invoke(request, null, connect, log);
      assertEquals(505, result.getStatusCode());
   }

   private byte[] readStreamContent(InputStream input) throws IOException {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buff = new byte[1024];
      int len;
      while ((len = input.read(buff)) > 0) {
         output.write(buff, 0, len);
      }
      return output.toByteArray();
   }
}
