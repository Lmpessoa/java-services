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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import com.lmpessoa.services.util.logging.ILogger;

class ApplicationContext implements ServletContext, Runnable {

   private final Map<String, Servlet> servlets = new HashMap<>();
   private final Map<String, String> params = new HashMap<>();
   private final Map<String, Object> attrs = new HashMap<>();
   private final ApplicationServer server;
   private final InetAddress addr;
   private final String name;
   private final int port;

   private ServerSocket socket;

   @Override
   public void run() {
      Thread.currentThread().setName(name);
      try (ServerSocket serverSocket = new ServerSocket(port, 0, addr)) {
         socket = serverSocket;
         serverSocket.setSoTimeout(1);
         while (socket != null && !socket.isClosed()) {
            acceptClient(socket);
         }
      } catch (IOException e) {
         getLogger().error(e);
      }
   }

   @Override
   public String getContextPath() {
      return "/";
   }

   @Override
   public ServletContext getContext(String uripath) {
      return this;
   }

   @Override
   public int getMajorVersion() {
      return 3;
   }

   @Override
   public int getMinorVersion() {
      return 0;
   }

   @Override
   public int getEffectiveMajorVersion() {
      return 3;
   }

   @Override
   public int getEffectiveMinorVersion() {
      return 0;
   }

   @Override
   public String getMimeType(String file) {
      return null;
   }

   @Override
   public Set<String> getResourcePaths(String path) {
      return null;
   }

   @Override
   public URL getResource(String path) throws MalformedURLException {
      return null;
   }

   @Override
   public InputStream getResourceAsStream(String path) {
      return null;
   }

   @Override
   public RequestDispatcher getRequestDispatcher(String path) {
      return null;
   }

   @Override
   public RequestDispatcher getNamedDispatcher(String name) {
      return null;
   }

   @Override
   public Servlet getServlet(String name) throws ServletException {
      return null;
   }

   @Override
   public Enumeration<Servlet> getServlets() {
      return null;
   }

   @Override
   public Enumeration<String> getServletNames() {
      return null;
   }

   @Override
   public void log(String msg) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void log(Exception exception, String msg) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void log(String message, Throwable throwable) {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getRealPath(String path) {
      return null;
   }

   @Override
   public String getServerInfo() {
      return "Leeow/" + ApplicationServer.getVersion();
   }

   @Override
   public String getInitParameter(String name) {
      return params.get(name);
   }

   @Override
   public Enumeration<String> getInitParameterNames() {
      return Collections.enumeration(params.keySet());
   }

   @Override
   public boolean setInitParameter(String name, String value) {
      if (params.containsKey(name)) {
         return false;
      }
      params.put(name, value);
      return true;
   }

   @Override
   public Object getAttribute(String name) {
      return attrs.get(name);
   }

   @Override
   public Enumeration<String> getAttributeNames() {
      return Collections.enumeration(attrs.keySet());
   }

   @Override
   public void setAttribute(String name, Object object) {
      attrs.put(name, object);
   }

   @Override
   public void removeAttribute(String name) {
      attrs.remove(name);
   }

   @Override
   public String getServletContextName() {
      return name;
   }

   @Override
   public Dynamic addServlet(String servletName, String className) {
      return null;
   }

   @Override
   public Dynamic addServlet(String servletName, Servlet servlet) {
      try {
         servlet.init(new ApplicationConfig(this, servletName));
         servlets.put(servletName, servlet);
      } catch (ServletException e) {
         getLogger().error(e);
      }
      return null;
   }

   @Override
   public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
      try {
         Servlet servlet = createServlet(servletClass);
         return addServlet(servletName, servlet);
      } catch (ServletException e) {
         getLogger().error(e);
      }
      return null;
   }

   @Override
   public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
      return null;
   }

   @Override
   public ServletRegistration getServletRegistration(String servletName) {
      return null;
   }

   @Override
   public Map<String, ? extends ServletRegistration> getServletRegistrations() {
      return null;
   }

   @Override
   public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
      return null;
   }

   @Override
   public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
      return null;
   }

   @Override
   public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
      return null;
   }

   @Override
   public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
      return null;
   }

   @Override
   public FilterRegistration getFilterRegistration(String filterName) {
      return null;
   }

   @Override
   public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
      return null;
   }

   @Override
   public SessionCookieConfig getSessionCookieConfig() {
      return null;
   }

   @Override
   public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
      return null;
   }

   @Override
   public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
      return null;
   }

   @Override
   public void addListener(String className) {
      throw new UnsupportedOperationException();
   }

   @Override
   public <T extends EventListener> void addListener(T t) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void addListener(Class<? extends EventListener> listenerClass) {
      throw new UnsupportedOperationException();
   }

   @Override
   public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
      return null;
   }

   @Override
   public JspConfigDescriptor getJspConfigDescriptor() {
      return null;
   }

   @Override
   public ClassLoader getClassLoader() {
      return ClassLoader.getSystemClassLoader();
   }

   @Override
   public void declareRoles(String... roleNames) {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getVirtualServerName() {
      return null;
   }

   ApplicationContext(ApplicationServer applicationServer, String name, InetAddress addr, int port) {
      this.server = Objects.requireNonNull(applicationServer);
      this.name = name;
      this.addr = addr;
      this.port = port;
   }

   IHostEnvironment getEnvironment() {
      return server.getEnvironment();
   }

   ILogger getLogger() {
      return server.getLogger();
   }

   void stop() {
      socket = null;
   }

   private void acceptClient(ServerSocket socket) {
      try {
         Socket client = socket.accept();
         ApplicationServlet servlet = (ApplicationServlet) servlets.get("leeow");
         Runnable job = new HttpRequestJob(servlet, client);
         server.queueJob(job);
      } catch (SocketTimeoutException e) {
         // just ignore
      } catch (IOException e) {
         getLogger().error(e);
      }
   }
}
