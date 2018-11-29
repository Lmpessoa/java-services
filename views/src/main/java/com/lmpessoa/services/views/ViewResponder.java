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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.lmpessoa.services.ContentType;
import com.lmpessoa.services.HttpInputStream;
import com.lmpessoa.services.Localized;
import com.lmpessoa.services.Redirect;
import com.lmpessoa.services.hosting.Headers;
import com.lmpessoa.services.hosting.HttpRequest;
import com.lmpessoa.services.hosting.NextResponder;
import com.lmpessoa.services.internal.hosting.HttpException;
import com.lmpessoa.services.internal.hosting.IHttpStatusCodeProvider;
import com.lmpessoa.services.internal.hosting.InternalServerError;
import com.lmpessoa.services.logging.ILogger;
import com.lmpessoa.services.routing.RouteMatch;
import com.lmpessoa.services.views.templating.RenderizationContext;
import com.lmpessoa.services.views.templating.RenderizationEngine;
import com.lmpessoa.services.views.templating.Template;
import com.lmpessoa.services.views.templating.TemplateParseException;

public final class ViewResponder {

   private static final Map<String, RenderizationEngine<?>> engines = new LinkedHashMap<>();
   private static final Map<String, CachedTemplate> cache = new HashMap<>();
   private static final Map<String, Object> variables = new HashMap<>();
   private static String viewsPath = "/views/";

   private final NextResponder next;

   public ViewResponder(NextResponder next) {
      this.next = next;
   }

   public static void useEngine(String extension, RenderizationEngine<?> engine) {
      engines.put(extension, engine);
   }

   public static void useViewsPath(String path) {
      viewsPath = String.format("/%s/", path).replace("//", "/");
   }

   public static void addVariable(String name, Object value) {
      if (!name.matches("[_a-zA-Z][_a-zA-Z0-9]*")) {
         throw new IllegalArgumentException(ViewsMessage.INVALID_VARIABLE_NAME.get());
      }
      variables.put(name, value);
   }

   public Object invoke(HttpRequest request, RouteMatch route, ILogger log) throws IOException {
      Object result = getRequestedObject(request, route);
      if (result instanceof ViewAndModel) {
         ViewAndModel view = (ViewAndModel) result;
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
         } catch (TemplateParseException | URISyntaxException e) {
            log.error(e);
         }
         if (template != null) {
            String page = template.render(view.getContext());
            result = new HttpInputStream(page.getBytes(UTF_8), ContentType.HTML, UTF_8);
         } else if (view.getContext().get("error") != null) {
            result = view.getContext().get("error");
         } else {
            result = view.getContext().get("model");
         }
      }
      return result;
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

   private Object getRequestedObject(HttpRequest request, RouteMatch route) {
      Object result;
      try {
         if (route instanceof HttpException) {
            throw (HttpException) route;
         }
         result = next.invoke();
         if (result instanceof ViewAndModel || result instanceof Redirect
                  || result instanceof InputStream || result instanceof byte[]) {
            return result;
         }
      } catch (Throwable t) {
         result = t;
      }
      String accepts = request.getHeaders().get(Headers.ACCEPT);
      if (accepts != null && accepts.startsWith("text/html")) {
         if (result instanceof Throwable) {
            Throwable t = (Throwable) result;
            String viewName = result instanceof IHttpStatusCodeProvider
                     ? String.valueOf(((HttpException) result).getStatusCode())
                     : "500";
            result = new ViewAndModel(viewName);
            ((ViewAndModel) result).set("error", t);
         } else if (route.getMethod() != null) {
            String viewName = getViewResourceName(route);
            result = new ViewAndModel(viewName, result);
         }
      } else if (result instanceof HttpException) {
         throw (HttpException) result;
      } else if (result instanceof InternalServerError) {
         throw (InternalServerError) result;
      }
      return result;
   }

   private String getViewResourceName(RouteMatch route) {
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

   private String getResourceName(Class<?> clazz) {
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
