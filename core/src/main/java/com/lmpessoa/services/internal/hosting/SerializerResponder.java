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
package com.lmpessoa.services.internal.hosting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.lmpessoa.services.BadRequestException;
import com.lmpessoa.services.ContentType;
import com.lmpessoa.services.DateHeader;
import com.lmpessoa.services.HttpInputStream;
import com.lmpessoa.services.Redirect;
import com.lmpessoa.services.hosting.ConnectionInfo;
import com.lmpessoa.services.hosting.Headers;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.hosting.NextResponder;
import com.lmpessoa.services.internal.ClassUtils;
import com.lmpessoa.services.internal.CoreMessage;
import com.lmpessoa.services.internal.serializing.Serializer;
import com.lmpessoa.services.logging.ILogger;
import com.lmpessoa.services.routing.RouteMatch;

final class SerializerResponder {

   private NextResponder next;

   public SerializerResponder(NextResponder next) {
      this.next = next;
   }

   public HttpResult invoke(HttpRequest request, RouteMatch route, ConnectionInfo connect,
      ILogger log) throws IOException {
      Localization.setLocales(request.getAcceptedLanguages());
      Object obj = getResultObject(request);
      int statusCode = getStatusCode(obj);
      HttpInputStream is;
      try {
         is = getContentBody(obj, request, route != null ? route.getMethod() : null);
      } catch (Throwable e) {
         obj = e instanceof HttpException ? e : new InternalServerError(e);
         statusCode = getStatusCode(obj);
         is = getContentBody(obj, request, null);
      }
      Collection<HeaderEntry> headers = getExtraHeaders(obj, is, connect, log);
      if (obj instanceof Throwable && !(obj instanceof Redirect)) {
         Throwable t = (Throwable) obj;
         if (t instanceof InternalServerError) {
            t = t.getCause();
         }
         log.error(t);
      }
      return new HttpResult(statusCode, headers, is);
   }

   static boolean isTextual(String contentType) {
      List<String> extraTextTypes = Arrays.asList(ContentType.ATOM, ContentType.JS,
               ContentType.JSON, ContentType.RSS, ContentType.SVG, ContentType.WSDL,
               ContentType.XHTML, ContentType.XML);
      return contentType.startsWith("text/") || extraTextTypes.contains(contentType);
   }

   private static boolean isDateHeaderMethod(Method method) {
      Class<?> retType = method.getReturnType();
      int mod = method.getModifiers();
      return method.isAnnotationPresent(DateHeader.class) && method.getName().matches("get[A-Z].*")
               && !Modifier.isStatic(mod) && !Modifier.isAbstract(mod)
               && method.getParameterCount() == 0
               && TemporalAccessor.class.isAssignableFrom(retType);
   }

   private Object getResultObject(HttpRequest request) {
      if (!Arrays.asList("HTTP/1.0", "HTTP/1.1").contains(request.getProtocol())) {
         return new VersionNotSupportedError(request.getProtocol());
      }
      try {
         Object result = next.invoke();
         if (result instanceof URL) {
            return Redirect.to((URL) result);
         }
         return result;
      } catch (Throwable t) {
         // This is correct; we capture every kind of exception here
         // We also know that Sonar wants us to have each type of exception treated in its own catch
         // block but that would cause too many code duplications we'd rather ignore these warnings
         while ((t instanceof InvocationTargetException || t instanceof InternalServerError)
                  && t.getCause() != null) {
            t = t.getCause();
         }
         if (t instanceof HttpException || t instanceof Error) {
            return t;
         }
         return new InternalServerError(t);
      }
   }

   private int getStatusCode(Object obj) {
      if (obj == null) {
         return 204;
      } else if (obj instanceof IHttpStatusCodeProvider) {
         return ((IHttpStatusCodeProvider) obj).getStatusCode();
      }
      return 200;
   }

   private Collection<HeaderEntry> getExtraHeaders(Object obj, HttpInputStream content,
      ConnectionInfo connect, ILogger log) throws IOException {
      List<HeaderEntry> result = new ArrayList<>();
      if (content != null) {
         String contentType = content.getType();
         if (isTextual(contentType) && content.getEncoding() != null) {
            contentType += String.format("; charset=\"%s\"",
                     content.getEncoding().name().toLowerCase());
         }
         result.add(new HeaderEntry(Headers.CONTENT_TYPE, contentType));
         result.add(new HeaderEntry(Headers.CONTENT_LENGTH, String.valueOf(content.available())));
         if (content.getFilename() != null) {
            String disposition = content.isDownloadable() ? "attachment" : "inline";
            result.add(new HeaderEntry(Headers.CONTENT_DISPOSITION,
                     String.format("%s; filename=\"%s\"", disposition, content.getFilename())));
         }
      }
      if (obj instanceof RedirectImpl) {
         RedirectImpl redirect = (RedirectImpl) obj;
         result.add(new HeaderEntry(Headers.LOCATION, redirect.getUrl(connect).toExternalForm()));
      }
      if (obj != null) {
         String date = getDateHeaderFromContent(obj, log);
         if (date == null) {
            // Date header is nearly mandatory according to RFC 7231
            date = DateHeader.RFC_7231_DATE_TIME.format(ZonedDateTime.now());
         }
         result.add(new HeaderEntry(Headers.DATE, date));
      }
      return Collections.unmodifiableCollection(result);
   }

   private String getDateHeaderFromContent(Object obj, ILogger log) {
      // Find the field or getter with @DateInfo
      TemporalAccessor result = null;
      Class<?> clazz = obj.getClass();
      Method[] methods = ClassUtils.findMethods(clazz, SerializerResponder::isDateHeaderMethod);
      if (methods.length > 1) {
         log.debug("Too many methods providing date header: "
                  + Arrays.stream(methods).map(Method::getName).collect(Collectors.joining(", ")));
         return null;
      } else if (methods.length == 1) {
         try {
            result = (TemporalAccessor) methods[0].invoke(obj);
         } catch (InvocationTargetException e) {
            log.debug(e.getCause());
            return null;
         } catch (IllegalAccessException | IllegalArgumentException e) {
            log.debug(e);
            return null;
         }
         if (!(result instanceof OffsetDateTime || result instanceof ZonedDateTime
                  || result instanceof LocalDateTime || result instanceof Instant)) {
            return null;
         }
      } else {
         return null;
      }

      // Convert LocalDateTime, ZonedDateTime or Instant to OffsetDateTime
      ZonedDateTime dt;
      if (result instanceof LocalDateTime) {
         dt = ((LocalDateTime) result).atZone(ZoneId.systemDefault());
      } else if (result instanceof Instant) {
         dt = ((Instant) result).atZone(ZoneId.systemDefault());
      } else if (result instanceof OffsetDateTime) {
         dt = ((OffsetDateTime) result).toZonedDateTime();
      } else {
         dt = (ZonedDateTime) result;
      }
      dt = dt.withZoneSameInstant(ZoneId.of("GMT"));
      return DateHeader.RFC_7231_DATE_TIME.format(dt);
   }

   private HttpInputStream getContentBody(Object obj, HttpRequest request, Method method) {
      Object object = obj;
      if (object instanceof HttpInputStream) {
         return (HttpInputStream) object;
      }
      if (object == null || object instanceof Redirect) {
         return null;
      }
      if (object instanceof InternalServerError
               && ((InternalServerError) object).getCause() != null) {
         object = ((InternalServerError) obj).getCause();
      }
      if (object instanceof BadRequestException) {
         BadRequestException e = (BadRequestException) object;
         if (e.getErrors() != null) {
            object = e.getErrors();
         }
      }
      if (object instanceof Throwable) {
         object = ((Throwable) obj).getMessage();
         if (object == null) {
            return null;
         }
      }
      return convertToInputStream(obj, object, request, method);
   }

   private HttpInputStream convertToInputStream(Object original, Object serialised,
      HttpRequest request, Method method) {
      String contentType;
      if (!(original instanceof Throwable) && method != null
               && method.isAnnotationPresent(ContentType.class)) {
         String[] types = method.getAnnotation(ContentType.class).value();
         if (types.length != 1) {
            throw new IllegalStateException(CoreMessage.SERIALIZE_CONTENT_TYPE.get());
         }
         contentType = types[0];
      } else if (serialised instanceof String) {
         contentType = ContentType.TEXT + "; charset=\"utf-8\"";
      } else {
         contentType = ContentType.BINARY;
      }
      if (serialised instanceof String) {
         Charset charset = getCharsetFromMethodOrUTF8(method);
         serialised = ((String) serialised).getBytes(charset);
      } else if (serialised instanceof ByteArrayOutputStream) {
         serialised = ((ByteArrayOutputStream) serialised).toByteArray();
      }
      if (serialised instanceof byte[]) {
         serialised = new ByteArrayInputStream((byte[]) serialised);
      }
      if (serialised instanceof InputStream) {
         return new HttpInputStream((InputStream) serialised, contentType);
      }
      return serialize(serialised, request);
   }

   private HttpInputStream serialize(Object object, HttpRequest request) {
      final List<String> accepts = new ArrayList<>();
      if (request.getHeaders().contains(Headers.ACCEPT)) {
         Arrays.stream(request.getHeaders().getAll(Headers.ACCEPT)).map(s -> s.split(",")).forEach(
                  s -> Arrays.stream(s).map(ss -> ss.split(";")[0].trim()).forEach(accepts::add));
         if (!accepts.contains(ContentType.JSON) && accepts.contains("*/*")) {
            accepts.set(accepts.indexOf("*/*"), ContentType.JSON);
         }
      } else {
         accepts.add(ContentType.JSON);
      }
      return Serializer.fromObject(object, accepts.toArray(new String[0]),
               request.getAcceptedLanguages());
   }

   private Charset getCharsetFromMethodOrUTF8(Method method) {
      if (method != null && method.isAnnotationPresent(ContentType.class)) {
         String[] types = method.getAnnotation(ContentType.class).value();
         if (types.length != 1) {
            throw new IllegalStateException(CoreMessage.SERIALIZE_CONTENT_TYPE.get());
         }
         String contentType = types[0];
         Map<String, String> ctypeMap = Headers.split(contentType);
         if (ctypeMap.containsKey("charset")) {
            return Charset.forName(ctypeMap.get("charset"));
         }
      }
      return StandardCharsets.UTF_8;
   }
}
