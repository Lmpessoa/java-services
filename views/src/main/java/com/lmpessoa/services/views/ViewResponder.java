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
package com.lmpessoa.services.views;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.lmpessoa.services.ContentType;
import com.lmpessoa.services.HttpInputStream;
import com.lmpessoa.services.Localized;
import com.lmpessoa.services.Redirect;
import com.lmpessoa.services.UnauthorizedException;
import com.lmpessoa.services.hosting.ConnectionInfo;
import com.lmpessoa.services.hosting.Headers;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.hosting.HttpResponse;
import com.lmpessoa.services.hosting.NextResponder;
import com.lmpessoa.services.internal.hosting.HttpException;
import com.lmpessoa.services.logging.ILogger;
import com.lmpessoa.services.routing.HttpMethod;
import com.lmpessoa.services.routing.RouteMatch;
import com.lmpessoa.services.security.IIdentity;
import com.lmpessoa.services.security.ITokenManager;
import com.lmpessoa.services.views.templating.RenderizationContext;
import com.lmpessoa.services.views.templating.RenderizationEngine;
import com.lmpessoa.services.views.templating.Template;
import com.lmpessoa.services.views.templating.TemplateParseException;

public final class ViewResponder {

   private static final Map<String, RenderizationEngine<?>> engines = new LinkedHashMap<>();
   private static final Map<String, CachedTemplate> cache = new HashMap<>();
   private static final Map<String, Object> variables = new HashMap<>();
   private static Class<? extends ILoginResource> loginClass;
   private static String viewsPath = "/views/";

   static final String SESSION_COOKIE = "__user_session";
   static Duration expiration;

   private final NextResponder next;

   public ViewResponder(NextResponder next) {
      this.next = next;
   }

   public static void useEngine(String extension, RenderizationEngine<?> engine) {
      engines.put(extension, engine);
   }

   public static void useLogin(Class<? extends ILoginResource> loginResource) {
      useLogin(loginResource, null);
   }

   public static void useLogin(Class<? extends ILoginResource> loginResource, Duration expiration) {
      ViewResponder.loginClass = Objects.requireNonNull(loginResource);
      ViewResponder.expiration = expiration;
   }

   public static void useViewsPath(String path) {
      viewsPath = String.format("/%s/", path).replace("//", "/");
   }

   public static void addVariable(String name, Object value) {
      if (!name.matches("[_a-zA-Z][_a-zA-Z0-9]*")) {
         throw new IllegalArgumentException(ViewsMessage.INVALID_VARIABLE_NAME.with(name));
      }
      variables.put(name, value);
   }

   public Object invoke(HttpRequest request, RouteMatch route, IIdentity identity,
      ConnectionInfo connection, ITokenManager tokens, ILogger log) throws IOException {
      Object result;
      try {
         result = next.invoke();
      } catch (Exception e) {
         result = e;
      }
      if (isTextHtmlRequest(request)) {
         result = processResultForLogin(result, request, route, identity, connection, tokens);
         if (result instanceof ViewAndModel) {
            ViewAndModel view = (ViewAndModel) result;
            if (view.getContext().get("now") == null) {
               view.set("now", ZonedDateTime.now());
            }
            result = produceViewPage((ViewAndModel) result, log);
         }
      }
      if (result instanceof ViewAndModel) {
         return ((ViewAndModel) result).getContext().get("model");
      } else if (result instanceof HttpException) {
         throw (HttpException) result;
      } else if (result instanceof Error) {
         throw (Error) result;
      }
      return result;
   }

   static String getTokenFromCookies(HttpRequest request) {
      String[] cookies = request.getHeaders().getAll(Headers.COOKIE);
      if (cookies != null) {
         for (String cookieSet : cookies) {
            for (String cookie : cookieSet.split(";")) {
               String[] cookiePair = cookie.split("=", 2);
               try {
                  String cookieName = URLDecoder.decode(cookiePair[0].trim(), UTF_8.name());
                  if (SESSION_COOKIE.equals(cookieName)) {
                     return URLDecoder.decode(cookiePair[1].trim(), UTF_8.name());
                  }
               } catch (UnsupportedEncodingException e) {
                  // Should never happen since we use only UTF-8
               }
            }
         }
      }
      return null;
   }

   private boolean isTextHtmlRequest(HttpRequest request) {
      HttpMethod method = request.getMethod();
      String accepts = request.getHeaders().get(Headers.ACCEPT);
      if (accepts != null) {
         accepts = accepts.split(",")[0];
         accepts = accepts.split(";")[0];
         accepts = accepts.toLowerCase();
      }
      return (method == HttpMethod.GET || method == HttpMethod.POST)
               && ContentType.HTML.equals(accepts);
   }

   private static Object processResultForLogin(Object result, HttpRequest request, RouteMatch route,
      IIdentity identity, ConnectionInfo connection, ITokenManager tokens) {
      if (loginClass != null) {
         if (result instanceof UnauthorizedException) {
            String returnUrl = request.getPath();
            if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
               returnUrl += "?" + request.getQueryString();
            }
            return redirectToLogin(returnUrl, connection);
         } else if (route.getResourceClass() == loginClass) {
            String returnUrl = request.getQuery().get("returnUrl");
            if (returnUrl == null) {
               returnUrl = request.getForm().get("returnUrl");
            }
            if (identity != null) {
               //
               // User is already logged in; just redirect
               //
               return Redirect.to(returnUrl);
            } else if ("get".equals(route.getMethod().getName())
                     && "logout".equals(request.getQueryString())) {
               //
               // User logout; remove token from server and browser
               //
               String token = getTokenFromCookies(request);
               if (token != null) {
                  tokens.remove(token);
               }
               return new ViewRedirect(returnUrl, null);
            } else if ("post".equals(route.getMethod().getName())
                     && result instanceof FailedLoginException) {
               //
               // User name and/or password invalid; re-present login form
               //
               ViewAndModel view = new ViewAndModel("login", route.getContentObject());
               view.set("message", ((Throwable) result).getMessage());
               view.set("returnUrl", returnUrl);
               return view;
            } else if ("post".equals(route.getMethod().getName()) && result instanceof IIdentity) {
               //
               // User name and password ok; redirect to URL with a token
               //
               String sessionToken = tokens.add((IIdentity) result);
               return new ViewRedirect(returnUrl, sessionToken);
            }
         }
      }
      if (result instanceof Throwable) {
         String viewName = result instanceof HttpResponse
                  ? String.valueOf(((HttpResponse) result).getStatusCode())
                  : "500";
         result = new ViewAndModel(viewName);
         ((ViewAndModel) result).set("error", result);
      } else if (!(result instanceof ViewAndModel || result instanceof HttpResponse
               || result instanceof InputStream)) {
         String viewName = getViewResourceName(route);
         result = new ViewAndModel(viewName, result);
      }
      return result;
   }

   private static Redirect redirectToLogin(String returnUrl, ConnectionInfo connection) {
      Matcher m = Pattern.compile("http(s)?://[^/](/.*)").matcher(returnUrl);
      if (m.find()) {
         returnUrl = m.group(2);
      }
      if ("/".equals(returnUrl)) {
         returnUrl = null;
      }
      try {
         returnUrl = URLEncoder.encode(returnUrl, UTF_8.name());
      } catch (UnsupportedEncodingException e) {
         // Not happening since its UTF-8
      }
      return Redirect.to(loginClass, "get", returnUrl);
   }

   private static Object produceViewPage(ViewAndModel view, ILogger log) {
      for (Entry<String, Object> entry : variables.entrySet()) {
         if (view.getContext().get(entry.getKey()) == null) {
            Object value = entry.getValue();
            if (value instanceof Supplier<?>) {
               value = ((Supplier<?>) value).get();
            }
            view.getContext().set(entry.getKey(), value);
         }
      }
      Template template = null;
      try {
         template = getTemplate(view.getViewName(), log);
      } catch (IOException | TemplateParseException | URISyntaxException e) {
         log.error(e);
         try {
            template = getTemplate("500", log);
         } catch (IOException | TemplateParseException | URISyntaxException e1) {
            log.error(e1);
         }
      }
      if (template != null) {
         String page = template.render(view.getContext());
         return new HttpInputStream(page.getBytes(UTF_8), ContentType.HTML, UTF_8);
      } else if (view.getContext().get("error") != null) {
         return view.getContext().get("error");
      }
      return view.getContext().get("model");
   }

   private static Template getTemplate(String viewName, ILogger log)
      throws IOException, TemplateParseException, URISyntaxException {
      URL url = getTemplateFile(viewName);
      if (url != null) {
         return getTemplate(url, file -> {
            String fileName = file.getName();
            String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
            RenderizationEngine<?> engine = engines.get(ext);
            try {
               return engine.parse(file);
            } catch (IOException | TemplateParseException e) {
               log.error(e);
               return null;
            }
         });
      } else {
         url = Localized.resourceAt(String.format("%s%s.html", viewsPath, viewName));
         if (url != null) {
            return getTemplate(url, file -> {
               try {
                  final String contents = Files.lines(file.toPath(), UTF_8)
                           .collect(Collectors.joining("\n"));
                  return context -> contents;
               } catch (IOException e) {
                  log.error(e);
                  return null;
               }
            });
         }
      }
      return null;
   }

   private static Template getTemplate(URL url, Function<File, Template> builder) {
      String fileName = url.getPath();
      synchronized (cache) {
         File file = new File(fileName);
         if (cache.containsKey(fileName)) {
            CachedTemplate cached = cache.get(fileName);
            if (cached.created >= file.lastModified()) {
               return cached;
            }
         }
         Template template = builder.apply(file);
         if (template == null) {
            return null;
         }
         CachedTemplate result = new CachedTemplate(template);
         cache.put(fileName, result);
         return result;
      }
   }

   private static URL getTemplateFile(String viewName) {
      URL url = null;
      for (Entry<String, RenderizationEngine<?>> e : engines.entrySet()) {
         url = Localized.resourceAt(String.format("%s%s.html.%s", viewsPath, viewName, e.getKey()));
         if (url == null) {
            url = Localized.resourceAt(String.format("%s%s.%s", viewsPath, viewName, e.getKey()));
         }
         if (url != null) {
            return url;
         }
      }
      return null;
   }

   private static String getViewResourceName(RouteMatch route) {
      View ann = route.getMethod().getAnnotation(View.class);
      if (ann == null) {
         ann = route.getResourceClass().getAnnotation(View.class);
      }
      if (ann == null || "##default".equals(ann.value())) {
         String className = getResourceName(route.getResourceClass());
         return String.format("%s/%s", className, route.getMethod().getName());
      }
      return ann.value();
   }

   private static String getResourceName(Class<?> clazz) {
      String[] nameParts = clazz.getName().replaceAll("\\$", ".").split("\\.");
      String name = nameParts[nameParts.length - 1].replaceAll("([A-Z])", "_$1")
               .toLowerCase()
               .replaceAll("^_", "");
      if (name.endsWith("_resource")) {
         name = name.substring(0, name.length() - 8).replaceAll("_$", "");
      }
      return name;
   }

   private static final class CachedTemplate implements Template {

      private final long created = System.currentTimeMillis();
      private final Template template;

      CachedTemplate(Template template) {
         this.template = template;
      }

      @Override
      public String render(RenderizationContext context) {
         return template.render(context);
      }
   }
}
