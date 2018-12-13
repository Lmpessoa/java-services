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

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;

import com.lmpessoa.services.HttpInputStream;
import com.lmpessoa.services.hosting.ConnectionInfo;
import com.lmpessoa.services.hosting.Headers;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.hosting.HttpResponse;
import com.lmpessoa.services.hosting.NextResponder;
import com.lmpessoa.services.hosting.ValuesMap;
import com.lmpessoa.services.internal.Wrapper;
import com.lmpessoa.services.internal.routing.RouteTable;
import com.lmpessoa.services.internal.services.ServiceMap;
import com.lmpessoa.services.logging.ILogger;
import com.lmpessoa.services.routing.IRouteTable;
import com.lmpessoa.services.routing.RouteMatch;
import com.lmpessoa.services.security.IIdentity;
import com.lmpessoa.services.security.ITokenManager;

final class ApplicationRequestJob implements Runnable {

   private static final String CRLF = "\r\n";

   private final ApplicationContext context;
   private final Socket client;
   private final ILogger log;

   @Override
   public void run() {
      try (Socket socket = this.client) {
         HttpRequest request = new HttpRequestImpl(socket.getInputStream(), context.getTimeout());

         String host = request.getHeaders().get(Headers.HOST);
         if (host == null) {
            host = String.format("localhost:%d", context.getPort());
         }
         ConnectionInfo connection = new ConnectionInfo(socket, host);

         HttpResponse result = resolveRequest(request, connection);
         result.setConnectionInfo(connection);

         log.info("\"%s\" %d %d \"%s\"", request, result.getStatusCode(),
                  result.getContentBody() != null ? result.getContentBody().available() : 0,
                  request.getHeaders().get(Headers.USER_AGENT));

         try (HttpInputStream contentStream = result.getContentBody()) {
            StringBuilder response = new StringBuilder();
            response.append("HTTP/1.1 ");
            response.append(result.getStatusCode());
            response.append(' ');
            response.append(result.getStatusLabel());
            response.append(CRLF);
            processHeaders(result, contentStream, response);

            OutputStream output = socket.getOutputStream();
            output.write(response.toString().getBytes());
            if (contentStream != null && contentStream.available() > 0) {
               output.write(CRLF.getBytes());
               contentStream.sendTo(output);
            }
            output.flush();
         }
      } catch (Exception e) {
         log.debug(e);
      }
   }

   ApplicationRequestJob(ApplicationContext context, Socket client) {
      this.log = context.getLogger();
      this.context = context;
      this.client = client;
   }

   private HttpResponse resolveRequest(HttpRequest request, ConnectionInfo connection) {
      final ServiceMap services = context.getServices();
      services.putRequestValue(ConnectionInfo.class, Objects.requireNonNull(connection));
      services.putRequestValue(HttpRequest.class, Wrapper.wrap(request));
      RouteTable routes = context.getRouteTable();
      services.putRequestValue(IRouteTable.class, Wrapper.wrap(routes));
      RouteMatch route = routes.matches(request);
      services.putRequestValue(RouteMatch.class, route);
      IIdentity identity = null;
      ITokenManager tokenManager = services.get(ITokenManager.class);
      if (tokenManager != null) {
         identity = tokenManager.get(request);
      }
      services.putRequestValue(IIdentity.class, identity);
      NextResponder chain = context.getFirstResponder();
      Object result = chain.invoke();
      if (result instanceof HttpResponse) {
         return (HttpResponse) result;
      } else {
         InternalServerError e = new InternalServerError("Unrecognised result type");
         e.setConnectionInfo(connection);
         throw e;
      }
   }

   private void processHeaders(HttpResponse result, HttpInputStream content, StringBuilder response)
      throws IOException {
      final ValuesMap headers = result.getHeaders();
      if (headers != null) {
         for (String headerName : headers.keySet()) {
            switch (headerName) {
               case Headers.CONTENT_TYPE:
               case Headers.CONTENT_DISPOSITION:
               case Headers.CONTENT_ENCODING:
               case Headers.CONTENT_LENGTH:
                  continue;
               default:
                  String[] headerValues = headers.getAll(headerName);
                  for (String value : headerValues) {
                     response.append(String.format("%s: %s%s", headerName, value, CRLF));
                  }
                  break;
            }
         }
      }
      if (content != null) {
         response.append(Headers.CONTENT_TYPE);
         response.append(": ");
         response.append(content.getType());
         if (content.getEncoding() != null) {
            response.append("; charset=\"");
            response.append(content.getEncoding().name().toLowerCase());
            response.append('"');
         }
         response.append(CRLF);

         response.append(Headers.CONTENT_LENGTH);
         response.append(": ");
         response.append(content.available());
         response.append(CRLF);

         if (content.getFilename() != null) {
            response.append(Headers.CONTENT_DISPOSITION);
            response.append(": ");
            response.append(content.isDownloadable() ? "attachment" : "inline");
            response.append("; filename=\"");
            response.append(content.getFilename());
            response.append('"');
            response.append(CRLF);
         }
      }
   }
}
