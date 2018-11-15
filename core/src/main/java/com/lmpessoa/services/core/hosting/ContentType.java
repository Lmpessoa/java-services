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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Identifies the content type returned by a method.
 *
 * <p>
 * Methods returning objects are usually automatically cast into an REST representation according to
 * the <code>Accept</code> header sent by the client. In methods that return input streams and byte
 * arrays the result is assumed to be in raw format and should be sent straight to the client
 * without further transformation.
 * </p>
 *
 * <p>
 * Methods which return input streams and byte arrays may choose to annotate the method with
 * <code>ContentType</code> to indicate all content returned from this method should be returned
 * with the given content type.
 * </p>
 *
 * <p>
 * Optionally, methods may return an {@link HttpInputStream} if the content type of the return may
 * vary.
 * </p>
 *
 * @see HttpInputStream
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface ContentType {

   public static final String ATOM = "application/atom+xml";
   public static final String BINARY = "application/octet-stream";
   public static final String CSS = "text/css";
   public static final String CSV = "text/csv";
   public static final String GIF = "image/gif";
   public static final String HTML = "text/html";
   public static final String ICO = "image/x-icon";
   public static final String JS = "application/javascript";
   public static final String JSON = "application/json";
   public static final String JPEG = "image/jpeg";
   public static final String PDF = "application/pdf";
   public static final String PNG = "image/png";
   public static final String RSS = "application/rss+xml";
   public static final String SVG = "image/svg+xml";
   public static final String SGML = "text/sgml";
   public static final String TEXT = "text/plain";
   public static final String WEBP = "image/webp";
   public static final String WSDL = "application/wdsl+xml";
   public static final String XHTML = "application/xhtml+xml";
   public static final String XML = "application/xml";
   public static final String XML_TEXT = "text/xml";
   public static final String YAML = "text/yaml";

   public static final String FORM = "application/x-www-form-urlencoded";
   public static final String MULTIPART_FORM = "multipart/form-data";

   String value();
}
