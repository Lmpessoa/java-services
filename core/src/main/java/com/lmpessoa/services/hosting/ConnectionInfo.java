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

import java.net.InetAddress;
import java.net.Socket;

/**
 * Represents connection information about the source of an HTTP request.
 */
public final class ConnectionInfo {

   private final Socket socket;

   ConnectionInfo(Socket socket) {
      this.socket = socket;
   }

   /**
    * Returns the local port this connection is bound to.
    *
    * @return the local port this connection is bound to.
    */
   public int getLocalPort() {
      return socket.getLocalPort();
   }

   /**
    * Returns the remote address this connection is bound to.
    *
    * @return the remote address this connection is bound to.
    */
   public InetAddress getRemoteAddress() {
      return socket.getInetAddress();
   }

   /**
    * Returns the remote port this connection is bound to.
    *
    * @return the remote port this connection is bound to.
    */
   public int getRemotePort() {
      return socket.getPort();
   }

   /**
    * Returns whether this connection is closed.
    *
    * @return <code>true</code> if this connection is closed, <code>false</code> otherwise.
    */
   public boolean isClosed() {
      return socket.isClosed();
   }

   /**
    * Returns whether this connection is still connected to its remote part.
    *
    * @return <code>true</code> if this connection is still open, <code>false</code> otherwise.
    */
   public boolean isConnected() {
      return socket.isConnected();
   }
}
