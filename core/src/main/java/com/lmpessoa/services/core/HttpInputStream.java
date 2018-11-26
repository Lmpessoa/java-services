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
package com.lmpessoa.services.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import com.lmpessoa.services.core.hosting.ApplicationServer;
import com.lmpessoa.services.core.hosting.Headers;
import com.lmpessoa.services.core.internal.serializing.Serializer;

/**
 * Represents an input stream with HTTP context information.
 *
 * <p>
 * Methods may choose to return content as fully formatted streams instead of objects, which can
 * only be encoded in REST supported formats. This may be accomplished in two ways:
 * </p>
 *
 * <ul>
 * <li>by returning a byte array or any {@link InputStream} and having the method annotated with
 * {@link ContentType}, which is useful only when the returned contents are always of the same type;
 * or</li>
 * <li>by returning an {@code HttpInputStream}, which can vary content type per request.</li>
 * </ul>
 *
 * <p>
 * An {@code HttpInputStream} may also be used instead of any other object class to receive content
 * from a request. In this case, contents received in a request will be passed to the method as they
 * were received by the application server.
 * </p>
 *
 * <p>
 * If no charset is given during the initialisation of the {@code HttpInputStream} it is assumed to
 * be UTF-8. The charset is ignored for content types that are not textual.
 * </p>
 */
public final class HttpInputStream extends InputStream implements AutoCloseable {

   private final InputStream stream;
   private final String contentType;
   private final Charset charset;
   private final String filename;

   private boolean downloadable = false;
   private ZonedDateTime date = null;

   /**
    * Creates a new {@code HttpInputStream} with the given context information.
    *
    * @param content the actual contents of this stream.
    * @param contentType the type of content of this stream.
    */
   public HttpInputStream(byte[] content, String contentType) {
      this(content, contentType, null, null);
   }

   /**
    * Creates a new {@code HttpInputStream} with the given context information.
    *
    * @param content the actual contents of this stream.
    * @param contentType the type of content of this stream.
    * @param filename a suggested file name for when saving this stream to a file.
    */
   public HttpInputStream(byte[] content, String contentType, String filename) {
      this(content, contentType, null, filename);
   }

   /**
    * Creates a new {@code HttpInputStream} with the given context information.
    *
    * @param content the actual contents of this stream.
    * @param contentType the type of content of this stream.
    * @param charset the charset used to encode the contents of this stream.
    */
   public HttpInputStream(byte[] content, String contentType, Charset charset) {
      this(content, contentType, charset, null);
   }

   /**
    * Creates a new {@code HttpInputStream} with the given context information.
    *
    * @param content the actual contents of this stream.
    * @param contentType the type of content of this stream.
    * @param charset the charset used to encode the contents of this stream.
    * @param filename a suggested file name for when saving this stream to a file.
    */
   public HttpInputStream(byte[] content, String contentType, Charset charset, String filename) {
      this(new ByteArrayInputStream(content), contentType, charset, filename);
   }

   /**
    * Creates a new {@code HttpInputStream} with the given context information.
    *
    * @param stream an input stream with the actual contents of this stream.
    * @param contentType the type of content of this stream.
    */
   public HttpInputStream(InputStream stream, String contentType) {
      this(stream, contentType, null, null);
   }

   /**
    * Creates a new {@code HttpInputStream} with the given context information.
    *
    * @param stream an input stream with the actual contents of this stream.
    * @param contentType the type of content of this stream.
    * @param filename a suggested file name for when saving this stream to a file.
    */
   public HttpInputStream(InputStream stream, String contentType, String filename) {
      this(stream, contentType, null, filename);
   }

   /**
    * Creates a new {@code HttpInputStream} with the given context information.
    *
    * @param stream an input stream with the actual contents of this stream.
    * @param contentType the type of content of this stream.
    * @param charset the charset used to encode the contents of this stream.
    */
   public HttpInputStream(InputStream stream, String contentType, Charset charset) {
      this(stream, contentType, charset, null);
   }

   /**
    * Creates a new {@code HttpInputStream} with the given context information.
    *
    * @param stream an input stream with the actual contents of this stream.
    * @param contentType the type of content of this stream.
    * @param charset the charset used to encode the contents of this stream.
    * @param filename a suggested file name for when saving this stream to a file.
    */
   public HttpInputStream(InputStream stream, String contentType, Charset charset,
      String filename) {
      Map<String, String> values = Headers.split(contentType);
      contentType = values.get("");
      if (charset == null && values.containsKey("charset")) {
         charset = Charset.forName(values.get("charset"));
      }
      if (!contentType.matches("[a-z]+/[a-z0-9-]+")) {
         throw new IllegalArgumentException("Invalid format for content type");
      }
      this.contentType = Objects.requireNonNull(contentType);
      this.stream = Objects.requireNonNull(stream);
      this.filename = filename;
      this.charset = charset;
   }

   /**
    * Creates a new {@code HttpInputStream} for the given file.
    *
    * @param file the file that contains the contents of this stream.
    * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular
    *            file, or for some other reason cannot be opened for reading.
    */
   public HttpInputStream(File file) throws FileNotFoundException {
      this(file, null);
   }

   /**
    * Creates a new {@code HttpInputStream} for the given file.
    *
    * @param file the file that contains the contents of this stream.
    * @param charset the charset used to encode the contents of this stream.
    * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular
    *            file, or for some other reason cannot be opened for reading.
    */
   public HttpInputStream(File file, Charset charset) throws FileNotFoundException {
      this(new FileInputStream(file), contentTypeOf(file), charset, file.getName()); // NOSONAR
      setDate(Instant.ofEpochMilli(file.lastModified()));
   }

   /**
    * Provides the date for the HTTP header "Date".
    *
    * @return the date to be returned in the HTTP header "Date", or {@code null} if no date is set.
    *
    * @see DateHeader
    */
   @DateHeader
   public ZonedDateTime getDate() {
      return date;
   }

   /**
    * Sets the date and time for the current {@code HttpInputStream} to the given value.
    *
    * @param date the new date and time for this {@code HttpInputStream}.
    */
   public void setDate(ZonedDateTime date) {
      this.date = date;
   }

   /**
    * Sets the date and time for the current {@code HttpInputStream} to the given value.
    *
    * @param date the new date and time for this {@code HttpInputStream}.
    */
   public void setDate(OffsetDateTime date) {
      this.date = date != null ? date.atZoneSameInstant(ZoneId.systemDefault()) : null;
   }

   /**
    * Sets the date and time for the current {@code HttpInputStream} to the given value.
    *
    * @param date the new date and time for this {@code HttpInputStream}.
    */
   public void setDate(LocalDateTime date) {
      this.date = date != null ? date.atZone(ZoneId.systemDefault()) : null;
   }

   /**
    * Sets the date and time for the current {@code HttpInputStream} to the given value.
    *
    * @param date the new date and time for this {@code HttpInputStream}.
    */
   public void setDate(Instant instant) {
      this.date = instant != null ? ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
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

   /**
    * Returns the type of the content of this stream.
    *
    * @return the type of the content of this stream.
    */
   public String getType() {
      return contentType;
   }

   /**
    * Returns the encoding of the content of this stream.
    *
    * @return the encoding of the content of this stream.
    */
   public Charset getEncoding() {
      return charset;
   }

   /**
    * Returns the file name associated with this stream.
    *
    * <p>
    * For streams created in method calls, this method will return a suggested file name to be used
    * when the user agent saves its contents to disk. For streams received from requests, this
    * method returns the given file name of the uploaded file.
    * </p>
    *
    * @return the file name associated with this stream.
    */
   public String getFilename() {
      return filename;
   }

   /**
    * Returns whether the contents of this stream should be forced to be saved by the user agent.
    *
    * @return {@code true} if the user agent should be forced to save this content to a file,
    *         {@code false} otherwise.
    */
   public boolean isDownloadable() {
      return downloadable;
   }

   /**
    * Sets whether the contents of this stream should be forced to be saved by the used agent.
    *
    * @param downloadable {@code true} if the user agent should be forced to save this content to a
    *           file, {@code false} otherwise.
    */
   public void setDownloadable(boolean downloadable) {
      this.downloadable = downloadable;
   }

   /**
    * Copies the contents of this stream into the given output stream.
    *
    * <p>
    * Note that this method will copy contents from the current position of the input stream up
    * until there are no more contents available. If the stream does not support {@link #reset()}
    * this operation may be executed only once.
    * </p>
    *
    * @param output the output stream to copy the contents to.
    * @throws IOException if an I/O error occurs. In particular, an {@code IOException} is thrown if
    *            either the input or output streams are closed or if the source input stream cannot
    *            be read.
    */
   public void sendTo(OutputStream output) throws IOException {
      Objects.requireNonNull(output);
      byte[] buffer = new byte[4096];
      int len;
      while ((len = stream.read(buffer)) != -1) {
         output.write(buffer, 0, len);
      }
   }

   private static String contentTypeOf(File file) {
      Class<?> startupClass = ApplicationServer.getStartupClass();
      if (startupClass == null) {
         Class<?>[] stackClasses = new SecurityManager() {

            public Class<?>[] getStack() {
               return getClassContext();
            }
         }.getStack();
         startupClass = Arrays.stream(stackClasses) //
                  .filter(c -> c != HttpInputStream.class)
                  .skip(1)
                  .findFirst()
                  .orElse(null);
      }
      String[] fileParts = file.getName().split("\\.", 2);
      if (fileParts.length == 1) {
         return ContentType.BINARY;
      }
      return Serializer.getContentTypeFromExtension(fileParts[1], startupClass);
   }

}
