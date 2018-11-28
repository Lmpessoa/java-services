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
package com.lmpessoa.services;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Indicates the source for the HTTP "Date" header.
 *
 * <p>
 * According to RFC 7231, <i>the "Date" header field represents the date and time at which the
 * message was originated. In theory, the date ought to represent the moment just before the payload
 * is generated. In practice, the date can be generated at any time during message origination. An
 * origin server MUST send a Date header field in all cases.</i>
 * </p>
 *
 * <p>
 * In respect to this definition, the engine will always sent the current date and time as response
 * to a request. Developers may indicate a method, using this annotation, that will provide a
 * different value for the "Date" header. For example:
 * </p>
 *
 * <pre>
 *
 * &#64;DateHeader
 * public LocalDateTime getLastModified() {
 *    LocalDateTime result;
 *    // some expensive processing here
 *    return result;
 * }
 * </pre>
 *
 * <p>
 * The method must be public, not static, and follow the naming convention for property getter
 * methods (must start with "get" and have no parameters) although the actual name of the property
 * is ignored. The return type on the method signature is also of little importance but the method
 * must return either a {@link ZonedDateTime}, {@link OffsetDateTime}, {@link LocalDateTime}, or
 * {@link Instant}. The engine will ensure the value of the header is in the correct time zone.
 * </p>
 *
 * <p>
 * At most one method can be marked with this annotation on a content object. If more than one
 * method is found for an object, none is used and the current date and time are returned in the
 * header. Methods from superclasses are also considered for the source of the "Date" header.
 * </p>
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface DateHeader {

   // RFC 7231 also estabilishes (https://tools.ietf.org/html/rfc7231#section-7.1.1.1) days must
   // have 2 digits while RFC 1123 produces days with one digit only
   public static final DateTimeFormatter RFC_7231_DATE_TIME = DateTimeFormatter
            .ofPattern("EEE, dd MMM yyyy HH:mm:ss O");

}
