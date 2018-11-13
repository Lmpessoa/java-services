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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.lmpessoa.services.core.MediaType;

/**
 * A <code>HttpResultInputStream</code> wraps content to be sent to a client in response to an HTTP
 * request along with the actual type of this content (as defined by the HTTP protocol).
 *
 * Applications that need to return dynamically generated content in response to certain requests
 * may return instances of this class in order to allow these content to be sent with the correct
 * content type.
 *
 * @see MediaType
 */
public final class HttpResultInputStream extends InputStream {

   private final InputStream content;
   private final String contentType;
   private final String downloadName;

   /**
    * Creates a new <code>HttpResultInputStream</code> with the given content type and content.
    *
    * @param contentType the content type of this result input stream.
    * @param data the actual content of this result input stream.
    */
   public HttpResultInputStream(String contentType, byte[] content) {
      this(contentType, content, null);
   }

   /**
    * Creates a new <code>HttpResultInputStream</code> with the given content type and content.
    *
    * @param contentType the content type of this result input stream.
    * @param content the actual content of this result input stream.
    */
   public HttpResultInputStream(String contentType, InputStream content) {
      this(contentType, content, null);
   }

   /**
    * Creates a new <code>HttpResultInputStream</code> with the given content type and content.
    *
    * @param contentType the content type of this result input stream.
    * @param content the actual content of this result input stream.
    * @param downloadName the suggested name for the file to contain this result input stream.
    */
   public HttpResultInputStream(String contentType, byte[] content, String downloadName) {
      this(contentType, new ByteArrayInputStream(content), downloadName);
   }

   /**
    * Creates a new <code>HttpResultInputStream</code> with the given content type and content.
    *
    * @param contentType the content type of this result input stream.
    * @param content the actual content of this result input stream.
    * @param downloadName the suggested name for the file to contain this result input stream.
    */
   public HttpResultInputStream(String contentType, InputStream content, String downloadName) {
      this.contentType = contentType;
      this.content = Objects.requireNonNull(content);
      this.downloadName = downloadName;
   }

   /**
    * Returns the content type of this input stream.
    *
    * @return the content type of this input stream.
    */
   public String getContentType() {
      return contentType;
   }

   /**
    * Returns whether the contents of this input stream should be downloaded by clients.
    *
    * @return <code>true</code> is the contents of this input stream should be downloaded by clients,
    * <code>false</code> otherwise.
    */
   public boolean isForceDownload() {
      return downloadName != null;
   }

   /**
    * Returns the name to use with the download file.
    *
    * @return the name to use with the download file, or <code>null</code> if the content is not meant
    * to be downloaded.
    */
   public String getDownloadName() {
      return downloadName;
   }

   @Override
   public int available() throws IOException {
      return content.available();
   }

   @Override
   public void close() throws IOException {
      content.close();
   }

   @Override
   public int read() throws IOException {
      return content.read();
   }

   @Override
   public int read(byte[] b) throws IOException {
      return content.read(b);
   }

   @Override
   public void reset() throws IOException {
      content.reset();
   }
}
