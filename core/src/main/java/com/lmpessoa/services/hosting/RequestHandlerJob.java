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
package com.lmpessoa.services.hosting;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.lmpessoa.services.logging.ILogger;
import com.lmpessoa.services.logging.NonTraced;
import com.lmpessoa.services.routing.MatchedRoute;
import com.lmpessoa.services.services.IServiceMap;
import com.lmpessoa.util.ConnectionInfo;

@NonTraced
final class RequestHandlerJob implements Runnable {

   private static final Map<Integer, String> statusCodeTexts = new HashMap<>();
   private static final String CRLF = "\r\n";

   private final Application app;
   private final HttpRequest request;
   private final OutputStream out;
   private final ILogger log;

   private Socket clientSocket = null;

   static {
      // These should be better maintained in a separate file but it didn't work during tests.
      statusCodeTexts.put(200, "OK");
      statusCodeTexts.put(201, "Created");
      statusCodeTexts.put(202, "Accepted");
      statusCodeTexts.put(204, "No Content");
      statusCodeTexts.put(301, "Moved Permanently");
      statusCodeTexts.put(302, "Found");
      statusCodeTexts.put(303, "See Other");
      statusCodeTexts.put(304, "Not Modified");
      statusCodeTexts.put(307, "Temporary Redirect");
      statusCodeTexts.put(308, "Permanent Redirect");
      statusCodeTexts.put(400, "Bad Request");
      statusCodeTexts.put(401, "Unauthorized");
      statusCodeTexts.put(403, "Forbidden");
      statusCodeTexts.put(404, "Not Found");
      statusCodeTexts.put(405, "Method Not Allowed");
      statusCodeTexts.put(406, "Not Acceptable");
      statusCodeTexts.put(415, "Unsupported Media Type");
      statusCodeTexts.put(500, "Internal Server Error");
      statusCodeTexts.put(501, "Not Implemented");
   }

   RequestHandlerJob(Application app, Socket socket) throws IOException {
      this(app, new HttpRequestImpl(socket.getInputStream()), socket.getOutputStream());
      this.clientSocket = socket;
   }

   RequestHandlerJob(Application app, HttpRequest request, OutputStream out) {
      this.app = Objects.requireNonNull(app);
      this.log = app.getLogger();
      this.request = Objects.requireNonNull(request);
      this.out = Objects.requireNonNull(out);
   }

   @Override
   public void run() {
      initializeJob();
      HttpResult result = resolveRequest();

      StringBuilder client = new StringBuilder();
      try {
         try {
            client.append("HTTP/1.1 ");
            client.append(result.getStatusCode());
            client.append(' ');
            client.append(statusCodeTexts.get(result.getStatusCode()));
            client.append(CRLF);
            byte[] data = null;
            if (result.getInputStream() != null) {
               HttpResultInputStream is = result.getInputStream();
               data = new byte[is.available()];
               int read = is.read(data);
               if (read != data.length) {
                  log.warning("Different lengths (expected: " + data.length + " bytes, found: " + read + " bytes)");
               }
               client.append("Content-Type: ");
               client.append(is.getContentType());
               client.append(CRLF);
               client.append("Content-Length: ");
               client.append(data.length);
               client.append(CRLF);
               if (is.isForceDownload()) {
                  client.append("Content-Disposition: attachment; filename=\"");
                  client.append(is.getDownloadName());
                  client.append('"');
                  client.append(CRLF);
               }
               client.append(CRLF);
            }
            out.write(client.toString().getBytes());
            if (data != null) {
               out.write(data);
            }
            out.flush();
         } finally {
            clientSocket.close();
         }
      } catch (IOException e) {
         throw new InternalServerError(e);
      }
   }

   private void initializeJob() {
      Thread.currentThread().setName("request");
      final IServiceMap serviceMap = app.getServiceMap();
      serviceMap.putRequestValue(ConnectionInfo.class, new ConnectionInfo(clientSocket));
      serviceMap.putRequestValue(HttpRequest.class, request);
   }

   private HttpResult resolveRequest() {
      HttpResult result;
      try {
         MatchedRoute route = app.matches(request);
         app.getServiceMap().putRequestValue(MatchedRoute.class, route);
         result = app.getMediator().invoke();
      } catch (InternalServerError | HttpException e) {
         result = new HttpResult(request, e.getStatusCode(), e, null);
      }
      log.info(result);
      if (result.getObject() instanceof InternalServerError) {
         log.error(((InternalServerError) result.getObject()).getCause());
      }
      return result;

   }
}
