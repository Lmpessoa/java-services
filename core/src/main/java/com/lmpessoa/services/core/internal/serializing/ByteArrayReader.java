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
package com.lmpessoa.services.core.internal.serializing;

final class ByteArrayReader {

   private static final byte[] CRLF = new byte[] { '\r', '\n' };

   private final byte[] data;

   private int pos = 0;

   ByteArrayReader(byte[] data) {
      this.data = data;
   }

   int available() {
      return this.data.length - pos;
   }

   String readLine() {
      return new String(readUntil(CRLF));
   }

   synchronized byte[] read(int len) {
      if (len > data.length - pos) {
         len = data.length - pos;
      }
      byte[] result = new byte[len];
      for (int i = 0; i < result.length; ++i) {
         result[i] = data[pos + i];
      }
      pos += len;
      return result;
   }

   synchronized byte[] readUntil(byte[] delimiter) {
      int len = 0;
      while (pos + len < data.length && !matchesDelimiter(pos + len, delimiter)) {
         len += 1;
      }
      byte[] result = read(len);
      pos += delimiter.length;
      return result;
   }

   private boolean matchesDelimiter(int pos, byte[] delimiter) {
      for (int i = 0; i < delimiter.length; ++i) {
         if (data[pos + i] != delimiter[i]) {
            return false;
         }
      }
      return true;
   }
}
