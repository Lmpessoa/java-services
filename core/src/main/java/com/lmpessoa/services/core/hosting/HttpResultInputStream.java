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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
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
public final class HttpResultInputStream extends InputStream implements AutoCloseable {

   private final InputStream contentStream;
   private final String contentType;
   private final int size;

   private String downloadName = null;
   private Charset encoding = null;

   /**
    * Creates a new <code>HttpResultInputStream</code> with the given content type and content.
    *
    * @param contentType the content type of this result input stream.
    * @param data the actual content of this result input stream.
    * @throws IOException
    */
   public HttpResultInputStream(String contentType, byte[] content) {
      this(contentType, new ByteArrayInputStream(content));
   }

   /**
    * Creates a new <code>HttpResultInputStream</code> with the given content type and content.
    *
    * @param contentType the content type of this result input stream.
    * @param content the actual content of this result input stream.
    * @throws IOException
    */
   public HttpResultInputStream(String contentType, InputStream contentStream) {
      this.contentType = Objects.requireNonNull(contentType);
      this.contentStream = Objects.requireNonNull(contentStream);
      int contentSize = 0;
      try {
         contentSize = contentStream.available();
      } catch (IOException e) {
         contentSize = 0;
      }
      this.size = contentSize;
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

   /**
    * Sets the name to use to download this content as a file.
    *
    * <p>
    * Setting a download name for a result stream automatically forces this content to be downloaded
    * (attached) instead of displayed inline.
    * </p>
    *
    * @param downloadName the download name for this content as a file.
    */
   public void setDownloadName(String downloadName) {
      this.downloadName = downloadName;
   }

   /**
    * Returns the content encoding for this content.
    *
    * <p>
    * This class does not enforce the content to be encoded with the returned encoding, as it is
    * provided by the creator of instances of this class. However, this information is ignored for
    * non-textual content types.
    * </p>
    *
    * @return the content encoding for this content.
    */
   public Charset getContentEncoding() {
      return encoding;
   }

   /**
    * Sets the content encoding for this content.
    *
    * <p>
    * This class does not enforce the content to be encoded with the given encoding, as it is provided
    * by the creator of instances of this class. However, this information is ignored for non-textual
    * content types.
    * </p>
    *
    * @param encoding the content encoding of this content.
    */
   public void setContentEncoding(Charset encoding) {
      this.encoding = encoding;
   }

   @Override
   public int available() throws IOException {
      return contentStream.available();
   }

   @Override
   public void close() throws IOException {
      contentStream.close();
   }

   @Override
   public int read() throws IOException {
      return contentStream.read();
   }

   /**
    * Returns the size of this content.
    *
    * @return the size of this content.
    * @throws IOException if an I/O error occurs.
    */
   public int size() {
      return size;
   }

   /**
    * Copies the contents of this input stream into a given output.
    *
    * @param out the output stream to copy this content to.
    * @throws IOException if an I/O error happened during the execution of this method.
    */
   public void copyTo(OutputStream out) throws IOException {
      copyStream(contentStream, out);
   }

   private void copyStream(InputStream in, OutputStream out) throws IOException {
      byte[] buffer = new byte[8192];
      int len;
      while ((len = in.read(buffer)) != -1) {
         out.write(buffer, 0, len);
      }
   }
}
