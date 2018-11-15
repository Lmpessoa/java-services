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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.lmpessoa.services.core.routing.AbstractRouteType;
import com.lmpessoa.services.core.routing.IRouteOptions;
import com.lmpessoa.services.core.routing.RouteTable;
import com.lmpessoa.services.core.security.IIdentityOptions;
import com.lmpessoa.services.core.security.IIdentityProvider;
import com.lmpessoa.services.core.services.ServiceMap;
import com.lmpessoa.services.util.ClassUtils;

final class ApplicationOptions implements IApplicationOptions {

   private final List<Class<?>> responders = new ArrayList<>();
   private final ServiceMap services = new ServiceMap();
   private final RouteTable routes = new RouteTable(services);

   private IIdentityProvider identityProvider = null;
   private boolean configured = false;

   // Responder

   @Override
   public void useResponder(Class<?> responderClass) {
      lockConfiguration();
      Objects.requireNonNull(responderClass);
      if (responders.contains(responderClass)) {
         throw new IllegalArgumentException("Responder is already registered");
      }
      if (!ClassUtils.isConcreteClass(responderClass)) {
         throw new IllegalArgumentException("Responder must be a concrete class");
      }
      if (ClassUtils.getConstructor(responderClass, NextResponder.class) == null) {
         throw new IllegalArgumentException("Responder must implement a required constructor");
      }
      Method[] invokes = ClassUtils.findMethods(responderClass, m -> "invoke".equals(m.getName()));
      if (invokes.length != 1) {
         throw new IllegalArgumentException("Responder must have exaclty one method named 'invoke'");
      }
      responders.add(responderClass);
   }

   // Routes

   @Override
   public void useRoutes(Consumer<IRouteOptions> options) {
      lockConfiguration();
      if (options != null) {
         RouteOptionsImpl impl = new RouteOptionsImpl(routes.getOptions());
         options.accept(impl);
         impl.options = null;
      }
   }

   // Services

   @Override
   public <T> void useService(Class<T> serviceClass) {
      lockConfiguration();
      services.put(serviceClass);
   }

   @Override
   public <T> void useService(Class<T> serviceClass, Class<? extends T> implementationClass) {
      lockConfiguration();
      services.put(serviceClass, implementationClass);
   }

   @Override
   public <T> void useService(Class<T> serviceClass, Supplier<T> supplier) {
      lockConfiguration();
      services.put(serviceClass, supplier);
   }

   @Override
   public <T> void useService(Class<T> serviceClass, T instance) {
      lockConfiguration();
      services.put(serviceClass, instance);
   }

   // Async

   @Override
   public void useAsync() {
      useAsyncWithFeedbackPath("/feedback/");
   }

   @Override
   public void useAsyncWithFeedbackPath(String feedbackPath) {
      lockConfiguration();
      Objects.requireNonNull(feedbackPath);
      if (AsyncResponder.getFeedbackPath() != null) {
         throw new IllegalStateException("Async is already configured");
      }
      if (!feedbackPath.startsWith("/")) {
         feedbackPath = "/" + feedbackPath; // NOSONAR
      }
      if (!feedbackPath.endsWith("/")) {
         feedbackPath += "/";
      }
      if (!feedbackPath.matches("/([a-zA-Z0-9.-_]+/)+")) {
         throw new IllegalArgumentException("Given path is not valid");
      }
      AsyncResponder.setFeedbackPath(feedbackPath);
   }

   // Static files

   @Override
   public void useStaticFiles() {
      useStaticFilesAtPath("/static");
   }

   @Override
   public void useStaticFilesAtPath(String staticPath) {
      lockConfiguration();
      Objects.requireNonNull(staticPath);
      if (StaticResponder.getStaticPath() != null) {
         throw new IllegalStateException("Static files is already configured");
      }
      if (!staticPath.startsWith("/")) {
         staticPath = "/" + staticPath;// NOSONAR
      }
      while (staticPath.endsWith("/")) {
         staticPath = staticPath.substring(0, staticPath.length() - 1);
      }
      if (!staticPath.matches("(/[a-zA-Z0-9.-_]+)+")) {
         throw new IllegalArgumentException("Given path is not valid");
      }
      StaticResponder.setStaticPath(staticPath);
   }

   // Identity

   @Override
   public void useIdentity(IIdentityProvider identityProvider) {
      useIdentity(identityProvider, null);
   }

   @Override
   public void useIdentity(IIdentityProvider identityProvider, Consumer<IIdentityOptions> options) {
      lockConfiguration();
      Objects.requireNonNull(identityProvider);
      if (this.identityProvider != null) {
         throw new IllegalStateException("Identity is already configured");
      }
      this.identityProvider = identityProvider;
      if (options != null) {
         options.accept((policyName, policyMethod) -> {
            lockConfiguration();
            if (IdentityResponder.hasPolicy(policyName)) {
               throw new IllegalArgumentException("A policy with that name is already defined: " + policyName);
            }
            IdentityResponder.addPolicy(policyName, policyMethod);
         });
      }
   }

   ApplicationOptions(Consumer<ServiceMap> configuration) {
      if (configuration != null) {
         configuration.accept(services);
      }
   }

   void configurationEnded() {
      configured = true;
   }

   NextResponder getFirstResponder() {
      List<Class<?>> result = new ArrayList<>();
      result.add(SerializerResponder.class);
      result.add(FaviconResponder.class);
      if (StaticResponder.getStaticPath() != null) {
         result.add(StaticResponder.class);
      }

      result.addAll(this.responders);

      if (identityProvider != null) {
         result.add(IdentityResponder.class);
      }
      if (AsyncResponder.getFeedbackPath() != null) {
         result.add(AsyncResponder.class);
      }
      result.add(InvokeResponder.class);
      return new NextResponderImpl(services, result);
   }

   ServiceMap getServices() {
      return services;
   }

   RouteTable getRoutes() {
      return routes;
   }

   IIdentityProvider getIdentityProvider() {
      return identityProvider;
   }

   private void lockConfiguration() {
      if (configured) {
         throw new IllegalStateException("Application is already configured");
      }
   }

   private static class RouteOptionsImpl implements IRouteOptions {

      private IRouteOptions options;

      public RouteOptionsImpl(IRouteOptions options) {
         this.options = options;
      }

      @Override
      public void addArea(String areaPath, String packageExpr) {
         options.addArea(areaPath, packageExpr);
      }

      @Override
      public void addArea(String areaPath, String packageExpr, String defaultResource) {
         options.addArea(areaPath, packageExpr, defaultResource);
      }

      @Override
      public void addType(String typeLabel, Class<? extends AbstractRouteType> typeClass) {
         options.addType(typeLabel, typeClass);
      }
   }
}
