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
package com.lmpessoa.services.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Objects;

public final class HttpInputStream extends InputStream {

   private final InputStream stream;
   private final String contentType;
   private final Charset charset;
   private final String filename;

   private boolean downloadable = false;

   public HttpInputStream(String contentType, byte[] content) {
      this(contentType, content, null, null);
   }

   public HttpInputStream(String contentType, byte[] content, String filename) {
      this(contentType, content, null, filename);
   }

   public HttpInputStream(String contentType, byte[] content, Charset charset) {
      this(contentType, content, charset, null);
   }

   public HttpInputStream(String contentType, byte[] content, Charset charset, String filename) {
      this(contentType, new ByteArrayInputStream(content), charset, filename);
   }

   public HttpInputStream(String contentType, InputStream stream) {
      this(contentType, stream, null, null);
   }

   public HttpInputStream(String contentType, InputStream stream, String filename) {
      this(contentType, stream, null, filename);
   }

   public HttpInputStream(String contentType, InputStream stream, Charset charset) {
      this(contentType, stream, charset, null);
   }

   public HttpInputStream(String contentType, InputStream stream, Charset charset, String filename) {
      this.contentType = Objects.requireNonNull(contentType);
      if (!contentType.matches("[a-z]+/[a-z0-9-]+")) {
         throw new IllegalArgumentException("Invalid format for content type");
      }
      this.stream = Objects.requireNonNull(stream);
      this.filename = filename;
      this.charset = charset;
   }

   @Override
   public int available() throws IOException {
      return stream.available();
   }

   @Override
   public void close() throws IOException {
      stream.close();
   }

   @Override
   public synchronized void mark(int readlimit) {
      stream.mark(readlimit);
   }

   @Override
   public boolean markSupported() {
      return stream.markSupported();
   }

   @Override
   public int read() throws IOException {
      return stream.read();
   }

   @Override
   public synchronized void reset() throws IOException {
      stream.reset();
   }

   @Override
   public long skip(long n) throws IOException {
      return stream.skip(n);
   }

   public String getContentType() {
      return contentType;
   }

   public Charset getContentEncoding() {
      return charset;
   }

   public String getFilename() {
      return filename;
   }

   public boolean isDownloadable() {
      return downloadable;
   }

   public void setDownloadable(boolean downloadable) {
      this.downloadable = downloadable;
   }

   public void copyTo(OutputStream output) throws IOException {
      byte[] buffer = new byte[4096];
      int len;
      while ((len = stream.read(buffer)) != -1) {
         output.write(buffer, 0, len);
      }
   }
}
