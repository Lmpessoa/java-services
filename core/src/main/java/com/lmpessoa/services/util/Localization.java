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
package com.lmpessoa.services.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import com.lmpessoa.services.core.hosting.ApplicationServer;

/**
 * Facilitates the development of localised applications.
 *
 * <p>
 * Localisation simplifies finding resources that belong to families whose members share a common
 * base name, but whose names also have an additional component that identify their locales. This
 * allows developers to isolate locale specific content and retrieve them for the adequate locale
 * using only the common base name.
 * </p>
 *
 * This allows you to write programs that can:
 * <ul>
 * <li>be easily localised, or translated, into different languages</li>
 * <li>handle multiple locales at once</li>
 * <li>be easily modified later to support even more locales</li>
 * </ul>
 *
 * <p>
 * A localised resource must be in the same path and share the same base name of the default
 * resource added by the locale identifier (usually a two-letter code for the language, optionally
 * followed by another two-letter code to designate a country specific language). For example, for a
 * resource requested with the name "Picture.png", this file will be returned if no file for the
 * other requested locales is found. A family may provide as many locale-specific resources as
 * needed. The "Picture.png" resource for the Dutch locale would bear the name "Picture_nl.png"
 * while a resource specific for Dutch spoken in Suriname would bear the name "Picture_nl_SR.png".
 * </p>
 *
 * <p>
 * The {@code Localization} shares some resemblance with the {@link ResourceBundle} class but with a
 * few differences. {@code Localization} allows you to provide multiple locale at once and returns
 * the requested resource found for the first locale passed thus it is not necessary to perform
 * multiple tests per locale. Also, the location where the resources are searched for is dependent
 * may include defaults defined by the engine itself or in external accessory projects (like
 * plug-ins, for example).
 * </p>
 */
public final class Localization {

   private static final String SEPARATOR = "/";

   /**
    * Finds a resource with the given name best suited to the given preferred locale list.
    *
    * @param resourcePath the name of the desired resource.
    * @param locales the list of preferred locales for the resource.
    * @return an {link InputStream} object or {@code null} if no resource with this name is found.
    */
   public static InputStream streamOf(String resourcePath, Locale... locales) {
      if (!resourcePath.startsWith(SEPARATOR)) {
         resourcePath = SEPARATOR + resourcePath;
      }
      List<Locale> localeList = normalize(locales);
      String[] resourceParts = resourcePath.split("\\.", 2);
      if (resourceParts.length == 1) {
         resourceParts = new String[] { resourcePath, "" };
      }
      for (Locale locale : localeList) {
         Class<?>[] searchRefs = getReferenceClasses();
         for (Class<?> ref : searchRefs) {
            String fileName = String.format("%s_%s.%s", resourceParts[0], locale.toString(),
                     resourceParts[1]);
            if (locale == Locale.ROOT) {
               fileName = resourcePath;
            }
            InputStream result = ref.getResourceAsStream(fileName);
            if (result != null) {
               return result;
            }
         }
      }
      return null;
   }

   /**
    * Finds a resource with the given name best suited to the given preferred loacle list and
    * returns its contents as a string.
    *
    * @param resourcePath the name of the desired resource.
    * @param locales the list of preferred locales for the resource.
    * @return the contents of the resource or {@code null} if no resource with this name is found.
    */
   public static String contentsOf(String resourcePath, Locale... locales) {
      return contentsOf(resourcePath, StandardCharsets.UTF_8, locales);
   }

   /**
    * Finds a resource with the given name best suited to the given preferred loacle list and
    * returns its contents as a string.
    *
    * @param resourcePath the name of the desired resource.
    * @param encoding the encoding used by the resource.
    * @param locales the list of preferred locales for the resource.
    * @return the contents of the resource or {@code null} if no resource with this name is found.
    */
   public static String contentsOf(String resourcePath, Charset encoding, Locale... locales) {
      try (InputStream is = streamOf(resourcePath, locales)) {
         if (is == null) {
            return null;
         }
         ByteArrayOutputStream contents = new ByteArrayOutputStream();
         int read;
         byte[] buffer = new byte[1024];
         while ((read = is.read(buffer)) > 0) {
            contents.write(buffer, 0, read);
         }
         return new String(contents.toByteArray(), encoding);
      } catch (IOException e) {
         // Ignore
      }
      return null;
   }

   /**
    * Returns an object from which localised messages can be retrieved.
    * <p>
    * It is important to notice this method and the object it return will not check if the desired
    * resource family has any members at all. Instead, if no members of the family are found, any
    * calls to {@link Messages#get(String)} will always return {@code null}
    * </p>
    *
    * @param resourcePath the name of the resource containing messages.
    * @param locales the list of preferred locales for the resource.
    * @return an object from which localised messages can be retrieved.
    */
   public static Messages messagesOf(String resourcePath, Locale... locales) {
      final String absolutePath = (!resourcePath.startsWith(SEPARATOR) ? SEPARATOR : "")
               + resourcePath;
      List<Locale> localeList = normalize(locales);
      return message -> {
         Class<?>[] searchRefs = getReferenceClasses();
         for (Locale locale : localeList) {
            for (Class<?> ref : searchRefs) {
               String fileName = String.format("%s_%s.properties", absolutePath, locale.toString());
               if (locale == Locale.ROOT) {
                  fileName = String.format("%s.properties", absolutePath);
               }
               try (InputStream is = ref.getResourceAsStream(fileName)) {
                  if (is != null) {
                     Properties props = new Properties();
                     props.load(is);
                     if (props.containsKey(message)) {
                        return props.getProperty(message);
                     }
                  }
               } catch (IOException e) {
                  // Ignore and move to next language/file
               }
            }
         }
         return null;
      };
   }

   private static List<Locale> normalize(Locale[] locales) {
      List<Locale> localeList = new ArrayList<>(Arrays.asList(locales));
      Locale defaultLocale = Locale.getDefault();
      if (!localeList.contains(defaultLocale)) {
         localeList.add(defaultLocale);
      }
      localeList.add(Locale.ROOT);
      return localeList;
   }

   private static Class<?>[] getReferenceClasses() {
      List<Class<?>> result = new ArrayList<>();
      Class<?>[] stackClasses = new SecurityManager() {

         public Class<?>[] getStack() {
            return getClassContext();
         }
      }.getStack();
      // Finds the class that called the #get() method
      Class<?> callerClass = Arrays.stream(stackClasses)
               .filter(c -> !c.getName().matches(Localization.class.getName() + "(\\$.+)?"))
               .findFirst()
               .orElse(null);
      String callerClassPath = ClassUtils.findLocation(callerClass);

      // Finds the startup class (again, I know)
      Class<?> startupClass = ApplicationServer.getStartupClass();
      String startupClassPath = startupClass == null ? null : ClassUtils.findLocation(startupClass);

      // Adds an engine class to the list
      String thisClassPath = ClassUtils.findLocation(Localization.class);

      if (!callerClassPath.equals(thisClassPath)) {
         result.add(callerClass);
      }

      if (startupClass != null
               && (!result.contains(callerClass) || !startupClassPath.equals(callerClassPath))) {
         result.add(startupClass);
      }

      if ((!result.contains(callerClass) || !thisClassPath.equals(callerClassPath))
               && (startupClass == null || !result.contains(startupClass)
                        || !thisClassPath.equals(startupClassPath))) {
         result.add(Localization.class);
      }

      return result.toArray(new Class<?>[0]);
   }

   /**
    * Represents a set of localised messages.
    */
   public static interface Messages {

      /**
       * Returns the localised version of the given message.
       *
       * @param message the message whose localised version is desired.
       * @return the localised version of the given message, or {@code null} if no such message
       *         localisation exists.
       */
      String get(String message);
   }
}
