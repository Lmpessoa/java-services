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

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import com.lmpessoa.services.util.ConnectionInfo;

final class HttpRequestJob implements Runnable {

   private final Application app;
   private final Socket client;

   public HttpRequestJob(Application app, Socket client) {
      this.client = client;
      this.app = app;
   }

   @Override
   public void run() {
      try {
         try {
            HttpRequest request = new HttpRequestImpl(client.getInputStream());
            ConnectionInfo connInfo = new ConnectionInfo(client);
            OutputStream output = client.getOutputStream();
            app.respondTo(request, connInfo, output);
            output.flush();
         } finally {
            client.close();
         }
      } catch (IOException e) {
         app.getLogger().fatal(e);
      }
   }
}
