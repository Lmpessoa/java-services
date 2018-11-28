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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;

import com.lmpessoa.services.internal.ClassUtils;
import com.lmpessoa.services.internal.hosting.Localization;
import com.lmpessoa.services.internal.parsing.ITemplatePart;
import com.lmpessoa.services.internal.parsing.LiteralPart;
import com.lmpessoa.services.internal.parsing.SimpleParser;
import com.lmpessoa.services.internal.parsing.SimpleVariablePart;

/**
 * Facilitates the development of localised applications.
 *
 * <p>
 * This class simplifies finding resources that belong to families whose members share a common base
 * name, but whose names also have an additional component that identify their locales. This allows
 * developers to isolate locale specific content and retrieve them for the adequate locale using
 * only the common base name.
 * </p>
 *
 * This allows you to write programs that can:
 * <ul>
 * <li>be easily localized, or translated, into different languages</li>
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
 * The {@code Localizd} class shares some resemblance with the {@link ResourceBundle} class but with
 * a few differences. {@code Localized} allows you to provide multiple locale at once and returns
 * the requested resource found for the first locale passed thus it is not necessary to perform
 * multiple tests per locale. Also, the location where the resources are searched for is dependent
 * may include defaults defined by the engine itself or in external accessory projects (like
 * plug-ins, for example).
 * </p>
 */
public final class Localized {

   private static final String VARIABLE_FORMAT = "\\d+|(?:[a-zA-Z][a-zA-Z0-9_]*\\.)*[a-zA-Z][a-zA-Z0-9_]*";
   private static final String SEPARATOR = "/";

   /**
    * Finds the URL to a resource with the given name best suited to the given prefered locale list.
    *
    * <p>
    * Different from a direct call of {@link Class#getResource(String)}, all names used as arguments
    * to this method are absolute, prefixed or not with a {@code '/'} (<tt>'&#92;u002f'</tt>).
    * </p>
    *
    * *
    * <p>
    * the languages accepted by the client request (or the default language in the absence of any).
    * However, this method can be given a list of different {@link Locale}s from which localisation
    * of messages should be based on.
    * </p>
    *
    * @param name the name of the desired resource.
    * @param locales the list of prefered locales for the resource.
    * @return A {@link java.net.URL} object representing the path to the resource or {@code null} if
    *         no resource with this name is found.
    */
   public static URL resourceAt(String name, Locale... locales) {
      return getResourceUriFrom(name, locales);
   }

   /**
    * Finds a resource with the given name best suited to the given prefered locale list.
    *
    * <p>
    * By default, this method returns the contents of a resource that corresponds to the first match
    * using the languages accepted by the client request (or the default language in the absence of
    * any). However, this method can be given a list of different {@link Locale}s from which
    * localisation of messages should be based on.
    * </p>
    *
    * @param name the name of the desired resource.
    * @param locales the list of prefered locales for the resource.
    * @return an {link InputStream} object or {@code null} if no resource with this name is found.
    */
   public static InputStream resourceStreamAt(String name, Locale... locales) {
      URL resource = getResourceUriFrom(name, locales);
      try {
         return resource.openStream();
      } catch (Exception e) {
         // Any error is irrelevant, just return null
      }
      return null;
   }

   /**
    * Finds a resource with the given name best suited to the given prefered loacle list and returns
    * its contents as a string.
    *
    * <p>
    * By default, this method returns the contents of a resource that corresponds to the first match
    * using the languages accepted by the client request (or the default language in the absence of
    * any). However, this method can be given a list of different {@link Locale}s from which
    * localisation of messages should be based on.
    * </p>
    *
    * @param name the name of the desired resource.
    * @param locales the list of prefered locales for the resource.
    * @return the contents of the resource or {@code null} if no resource with this name is found.
    */
   public static String resourceContentsAt(String name, Locale... locales) {
      return getResourceContents(name, StandardCharsets.UTF_8, locales);
   }

   /**
    * Finds a resource with the given name best suited to the given prefered loacle list and returns
    * its contents as a string.
    *
    * <p>
    * By default, this method returns the contents of a resource that corresponds to the first match
    * using the languages accepted by the client request (or the default language in the absence of
    * any). However, this method can be given a list of different {@link Locale}s from which
    * localisation of messages should be based on.
    * </p>
    *
    * @param resourcePath the name of the desired resource.
    * @param encoding the encoding used by the resource.
    * @param locales the list of prefered locales for the resource.
    * @return the contents of the resource or {@code null} if no resource with this name is found.
    */
   public static String resourceContentsAt(String name, Charset encoding, Locale... locales) {
      return getResourceContents(name, encoding, locales);
   }

   /**
    * Returns an object from which localised messages can be retrieved.
    *
    * <p>
    * It is important to notice this method and the object it return will not check if the desired
    * resource family has any members at all. Instead, if no members of the family are found, any
    * calls to {@link MessageSet#get(String)} will always return {@code null}
    * </p>
    *
    * <p>
    * By default, this method returns a {@code MessageSet} that responds using the languages
    * accepted by the client request (or the default language in the absence of any). However, this
    * method can be given a list of different {@link Locale}s from which localisation of messages
    * should be based on.
    * </p>
    *
    * @param resourcePath the name of the resource containing messages.
    * @param locales the list of prefered locales for the resource.
    * @return an object from which localised messages can be retrieved.
    */
   public static MessageSet messagesAt(String name, Locale... locales) {
      return new MessageSet(Objects.requireNonNull(name), locales);
   }

   private Localized() {
      // Nothing to be done here
   }

   private static Class<?> getApplicationClass() {
      if (ApplicationServer.isRunning()) {
         return ApplicationServer.getStartupClass();
      }
      return ClassUtils
               .findCaller(c -> c != Localized.class && c.getDeclaringClass() != Localized.class);
   }

   private static String[] getLocaleSufixes(Locale[] locales) {
      List<Locale> localeList = new ArrayList<>();
      Arrays.asList(locales).forEach(localeList::add);
      Locale[] defaultLocales = Localization.getLocales();
      if (localeList.isEmpty() && defaultLocales != null) {
         Arrays.asList(defaultLocales).forEach(localeList::add);
      }
      if (!localeList.contains(Locale.getDefault())) {
         localeList.add(Locale.getDefault());
      }
      localeList.add(Locale.ROOT);
      String[] result = localeList.stream().map(l -> '_' + l.toString()).toArray(String[]::new);
      result[result.length - 1] = "";
      return result;
   }

   private static URL getResourceUriFrom(String name, Locale[] locales) {
      name = name.replace('\\', '/');
      if (!name.startsWith(SEPARATOR)) {
         name = SEPARATOR + name;
      }
      Iterator<String> sufixes = Arrays.asList(getLocaleSufixes(locales)).iterator();
      String[] path = splitResourcePath(name);
      URL result = null;
      while (result == null && sufixes.hasNext()) {
         String fullPath = String.format("%s%s.%s", path[0], sufixes.next(), path[1]);
         result = getApplicationClass().getResource(fullPath);
         if (result == null) {
            result = Localized.class.getResource(fullPath);
         }
      }
      return result;
   }

   private static String getResourceContents(String name, Charset encoding, Locale... locales) {
      URL resource = getResourceUriFrom(name, locales);
      try (InputStream is = resource.openStream()) {
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
      } catch (Exception e) {
         // Ignore
      }
      return null;
   }

   private static String[] splitResourcePath(String resourcePath) {
      resourcePath = resourcePath.replace('\\', '/');
      int dot = resourcePath.indexOf('.', resourcePath.lastIndexOf('/'));
      if (dot > -1) {
         return new String[] { resourcePath.substring(0, dot), resourcePath.substring(dot + 1) };
      }
      return new String[] { resourcePath, "" };
   }

   /**
    * Represents a set of message localisations.
    *
    * <p>
    * Message sets represents a family of localisation resources from which localised messages may
    * be retrieved. However, message sets are unable to ensure that localisations to either
    * particular language are available. In fact, a message set may represent an empty family of
    * localisations if no files with localisation messages exist at the location used to create its
    * instance.
    * </p>
    */
   public static final class MessageSet {

      private final String resourcePath;
      private final Locale[] locales;

      /**
       * Returns a {@link Message} represing the given message value.
       *
       * <p>
       * The {@code Message} instance returned by this method is only a representation of the
       * message whose localization is desired and not the localised message itself. The message can
       * be further formatted with arguments in order to provide the actual message.
       * </p>
       *
       * <p>
       * Also note that this method will return a valid {@code Message} instance whether or not
       * there is a suitable localisation of the message available in either of the available
       * languages. If the given message has no valid representation in any language of this message
       * set, this given value is returned after formatting.
       * </p>
       *
       * <p>
       * It is highly recommended that messages are refered to using a package style notation (the
       * fully qualified name of the class using the message followed by a dot ('.') and a simple
       * value identifying the particular message). This format is language neutral and provides a
       * clear reference as to where the message is being used. It is also a special kind of
       * variable accepted by the message formatter which enables messages to be composed by other
       * messages in order to enable better message reuse. Developers must ensure that there are no
       * loops in message reuse using this notation.
       * </p>
       *
       * @param value the message to be localised.
       * @return a {@code Message} instance representing the given message to be localised.
       */
      public Message get(String value) {
         return new Message(this, value);
      }

      private MessageSet(String resourcePath, Locale[] locales) {
         if (!resourcePath.startsWith(SEPARATOR)) {
            resourcePath = SEPARATOR + resourcePath;
         }
         this.resourcePath = resourcePath;
         this.locales = locales;

      }
   }

   /**
    * Represents a particular message in a {@link MessageSet}.
    *
    * <p>
    * Actual messages in any language may contain variables that may be replaced by arguments
    * provided before getting the final localised message. Variables are referenced by either
    * indices or names around curly braces (depending on the method called).
    * </p>
    *
    * <p>
    * It is highly recommended that messages are refered to using a package style notation (the
    * fully qualified name of the class using the message followed by a dot ('.') and a simple value
    * identifying the particular message). This format is language neutral and provides a clear
    * reference as to where the message is being used. It is also a special kind of variable
    * accepted by the message formatter which enables messages to be composed by other messages in
    * order to enable better message reuse. Developers must ensure that there are no loops in
    * message reuse using this notation.
    * </p>
    *
    * <p>
    * Any argument referenced by a message that is not available is left unchanged in the message
    * after formatting.
    * </p>
    */
   public static final class Message {

      private final MessageSet source;
      private final String message;

      /**
       * Returns the localised message without formatting.
       *
       * <p>
       * Messages that do not use arguments may use this method to retrieve the localised message
       * without the impression that an argument is missing
       *
       * @return
       */
      public String get() {
         return with(new HashMap<>());
      }

      /**
       * Formats the message using the given arguments.
       *
       * <p>
       * Messages may use arguments which refer to a positional index of the arguments given when
       * this method is called.
       * </p>
       *
       * @param args the arguments to be used to format the message.
       * @return the localised message formatted with the given arguments.
       */
      public String with(Object arg0, Object... args) {
         Map<String, Object> map = new HashMap<>();
         map.put("0", Objects.requireNonNull(arg0));
         for (int i = 0; i < args.length; ++i) {
            map.put(String.valueOf(i + 1), Objects.requireNonNull(args[i]));
         }
         return with(map);
      }

      /**
       * Formats the message using the values in the given collection.
       *
       * <p>
       * Messages may use arguments which refer to a positional index of the values in the
       * collection given when this method is called.
       * </p>
       *
       * @param args the values to be used to format the message.
       * @return the localised message formatted with the given arguments.
       */
      public String with(Collection<?> args) {
         Objects.requireNonNull(args);
         Map<String, Object> map = new HashMap<>();
         int i = 0;
         for (Object arg : args) {
            map.put(String.valueOf(i), Objects.requireNonNull(arg));
            i += 1;
         }
         return with(map);
      }

      /**
       * Formats the message using the values in the given map.
       *
       * <p>
       * Messages formatted using this method may only use named arguments. Positional arguments,
       * however, are not simply ignored since the argument map may refer to variable names that are
       * simply numbers.
       * </p>
       *
       * <p>
       * Developers, however, must refrain from using dots ('.') in variable names since these are
       * treated in a special way by the formatter.
       * </p>
       *
       * @param args the values to be used to format the message.
       * @return the localised message formatted with the given arguments.
       */
      public String with(Map<String, ?> args) {
         Objects.requireNonNull(args);
         String template = translate(message);
         if (template == null) {
            template = message;
         }
         return format(template, args);
      }

      private Message(MessageSet source, String message) {
         this.message = message;
         this.source = source;
      }

      private String format(String template, Map<String, ?> args) {
         List<ITemplatePart> parts = SimpleParser.parse(template, VARIABLE_FORMAT);
         StringBuilder result = new StringBuilder();
         for (ITemplatePart part : parts) {
            if (part instanceof LiteralPart) {
               result.append(((LiteralPart) part).getValue());
            } else if (part instanceof SimpleVariablePart) {
               String var = ((SimpleVariablePart) part).getName();
               String value = null;
               if (var.contains(".")) {
                  value = translate(var);
                  if (value != null) {
                     value = format(value, args);
                  }
               } else if (args.containsKey(var)) {
                  value = args.get(var).toString();
               }
               if (value == null) {
                  result.append('{');
                  result.append(var);
                  result.append('}');
               } else {
                  result.append(value);
               }
            }
         }
         return result.toString();
      }

      private String translate(String value) {
         String[] locales = getLocaleSufixes(source.locales);
         String[] path = splitResourcePath(source.resourcePath);
         for (String locale : locales) {
            String fileName = String.format("%s%s.%s", path[0], locale, path[1]);
            String result = translate(getApplicationClass(), fileName, value);
            if (result != null) {
               return result;
            }
            result = translate(Localized.class, fileName, value);
            if (result != null) {
               return result;
            }
         }
         return null;
      }

      private String translate(Class<?> reference, String fileName, String value) {
         Properties props = new Properties();
         try (InputStream is = reference.getResourceAsStream(fileName)) {
            if (is != null) {
               props.load(is);
               String result = props.getProperty(value);
               if (result != null) {
                  return result;
               }
            }
         } catch (IOException e) {
            // Should not happen; anyway just ignore
         }
         return null;
      }
   }
}
