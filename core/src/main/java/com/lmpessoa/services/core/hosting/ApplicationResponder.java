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
package com.lmpessoa.services.core.hosting;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.lmpessoa.services.core.routing.IRouteTable;
import com.lmpessoa.services.core.routing.RouteMatch;
import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.ConnectionInfo;
import com.lmpessoa.services.util.logging.ILogger;

final class ApplicationResponder implements Runnable {

   private static final Map<Integer, String> STATUSES = new HashMap<>();
   private static final String CRLF = "\r\n";

   private final ApplicationContext context;
   private final Socket client;
   private final ILogger log;

   @Override
   public void run() {
      try (Socket socket = this.client) {
         HttpRequest request = new HttpRequestImpl(socket.getInputStream());

         String host = request.getHeader(HeaderMap.HOST);
         if (host == null) {
            host = String.format("localhost:%d", context.getPort());
         }
         ConnectionInfo connection = new ConnectionInfo(socket, host);

         HttpResult result = resolveRequest(request, connection);

         log.info("\"%s\" %s \"%s\"", request, result, request.getHeader(HeaderMap.USER_AGENT));

         int statusCode = result.getStatusCode();
         StringBuilder response = new StringBuilder();
         response.append("HTTP/1.1 ");
         response.append(statusCode);
         response.append(' ');
         response.append(STATUSES.get(statusCode));
         response.append(CRLF);
         result.getHeaders().stream().forEach(
                  e -> response.append(String.format("%s: %s%s", e.getKey(), e.getValue(), CRLF)));

         HttpInputStream contentStream = result.getInputStream();

         response.append(CRLF);
         OutputStream output = socket.getOutputStream();
         output.write(response.toString().getBytes());
         if (contentStream != null && contentStream.available() > 0) {
            contentStream.sendTo(output);
         }
         output.flush();
      } catch (Exception e) {
         log.error(e);
      }
   }

   ApplicationResponder(ApplicationContext context, Socket client) {
      this.log = context.getLogger();
      this.context = context;
      this.client = client;
   }

   static {
      try {
         final URI uri = ApplicationResponder.class.getResource("/status.codes").toURI();
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

   private HttpResult resolveRequest(HttpRequest request, ConnectionInfo connection) {
      final ServiceMap services = context.getServices();
      services.putRequestValue(ConnectionInfo.class, Objects.requireNonNull(connection));
      services.putRequestValue(HttpRequest.class, Wrapper.wrap(request));
      RouteTable routes = context.getRouteTable();
      services.putRequestValue(IRouteTable.class, Wrapper.wrap(routes));
      RouteMatch route = routes.matches(request);
      services.putRequestValue(RouteMatch.class, route);
      NextHandler chain = context.getFirstResponder();
      Object result = chain.invoke();
      if (result instanceof HttpResult) {
         return (HttpResult) result;
      } else {
         throw new InternalServerError("Unrecognised result type");
      }
   }
}
