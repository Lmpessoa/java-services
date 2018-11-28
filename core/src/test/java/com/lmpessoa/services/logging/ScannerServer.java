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
package com.lmpessoa.services.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ScannerServer implements AutoCloseable {

   private final Queue<Byte> buffer = new ConcurrentLinkedQueue<>();
   private final String delimiter;
   private final Thread thread;
   private final int port;

   private boolean active = false;

   public ScannerServer(int port, String delimiter) {
      this.delimiter = delimiter;
      this.port = port;
      this.thread = new Thread(this::run);
      this.thread.start();
   }

   public String next() {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      String result = "";
      while (active && !result.endsWith(delimiter)) {
         Byte b = buffer.poll();
         if (b != null) {
            out.write(b);
            result = new String(out.toByteArray());
         }
      }
      return result.substring(0, result.length() - delimiter.length());
   }

   @Override
   public void close() throws IOException {
      active = false;
      while (thread.isAlive()) {
         try {
            thread.join();
         } catch (InterruptedException e) {
            // Ignore
         }
      }
   }

   private void write(byte[] buffer, int size) {
      for (int i = 0; i < size; ++i) {
         this.buffer.add(buffer[i]);
      }
   }

   private void run() {
      try (ServerSocket server = new ServerSocket(port)) {
         byte[] buffer = new byte[1024];
         server.setSoTimeout(100);
         active = true;
         int read;
         while (active) {
            try (Socket client = server.accept()) {
               InputStream in = client.getInputStream();
               while ((read = in.read(buffer)) >= 0) {
                  write(buffer, read);
               }
               in.close();
            } catch (SocketTimeoutException e) {
               // Do nothing; default behaviour
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
