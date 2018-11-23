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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

import com.lmpessoa.services.core.internal.hosting.ApplicationServerImpl;

/**
 * Represents the Application Server.
 *
 * <p>
 * The static methods in this class are used to start and stop the embedded application server
 * applications. Applications must call {@link #start()} from their <code>main</code> method to
 * start the application server.
 * </p>
 *
 * <p>
 * The application server can only be configured through a file named either
 * <code>settings.yml</code>, <code>settings.json</code> or <code>application.properties</code>
 * placed next to the application itself (refer to the extended documentation for the available
 * parameters). Files are searched in this exact order; once the first is found, any other files
 * present are ignored.
 * </p>
 */
public final class ApplicationServer {

   private static ApplicationServerImpl instance;

   /**
    * Starts the Application Server.
    * <p>
    * This method should be called only once from a <code>main</code> method under the desired
    * startup class.
    * </p>
    */
   public static void start() {
      if (instance == null) {
         instance = new ApplicationServerImpl();
         instance.start();
      }
   }

   /**
    * Requests that the Application Server be shutdown.
    * <p>
    * Calling this method does not immediately shuts down the server. Any requests in course will be
    * allowed to be completed and any log entries will be written out before the server effectively
    * shuts down.
    * </p>
    */
   public static void shutdown() {
      if (instance != null) {
         instance.stop(() -> instance = null);
      }
   }

   /**
    * Returns the version number of the Application Server.
    * <p>
    * The version number of the server coincides with the version number for the engine itself, and
    * thus may be used interchangeably.
    * </p>
    *
    * @return the version number of the Application Server.
    */
   public static String getVersion() {
      String version = ApplicationServer.class.getPackage().getImplementationVersion();
      if (version == null) {
         URL versionFile = ApplicationServer.class.getResource("/application.version");
         Collection<String> versionLines;
         try {
            versionLines = Files.readAllLines(Paths.get(versionFile.toURI()));
            version = versionLines.toArray()[0].toString();
         } catch (IOException | URISyntaxException e) {
            // Should never get here, but...
            version = null;
         }
      }
      if (version != null) {
         return version.split("-")[0];
      }
      return "";
   }

   /**
    * Returns whether the server is running or not.
    *
    * @return {@code true} if the server is running, {@code false} otherwise.
    */
   public static boolean isRunning() {
      return instance != null;
   }

   public static Class<?> getStartupClass() {
      return instance != null ? instance.getStartupClass() : null;
   }
}
