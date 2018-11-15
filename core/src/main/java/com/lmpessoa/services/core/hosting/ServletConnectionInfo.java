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

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletRequest;

import com.lmpessoa.services.util.ConnectionInfo;

final class ServletConnectionInfo implements ConnectionInfo {

   private final ServletRequest request;
   private final InetAddress remoteAddr;

   @Override
   public int getLocalPort() {
      return request.getLocalPort();
   }

   @Override
   public synchronized InetAddress getRemoteAddress() {
      return remoteAddr;
   }

   @Override
   public int getRemotePort() {
      return request.getRemotePort();
   }

   @Override
   public String getServerName() {
      return request.getServerName();
   }

   @Override
   public int getServerPort() {
      return request.getServerPort();
   }

   ServletConnectionInfo(ServletRequest request) {
      this.request = request;
      InetAddress remote = null;
      try {
         byte[] addrBytes = InetAddress.getByName(request.getRemoteAddr()).getAddress();
         remote = InetAddress.getByAddress(request.getRemoteHost(), addrBytes);
      } catch (UnknownHostException e) {
         // Should not throw given IP address and API description
      }
      this.remoteAddr = remote;
   }

   @Override
   public boolean isSecure() {
      return request.isSecure();
   }
}
