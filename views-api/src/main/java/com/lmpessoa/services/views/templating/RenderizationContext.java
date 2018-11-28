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
package com.lmpessoa.services.views.templating;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents the context to render a template.
 *
 * <p>
 * A renderisation context is a collection of variables that can be used dunring the renderisation
 * of a template. The context also hold the locale that is to be used, if required, during this
 * renderisation.
 * </p>
 *
 * <p>
 * Note that the context, prior to being passed to the renderisation of the template, may be
 * initialised with any given number of variables that may be used or not by the template. During
 * its renderisation, the template may also update or add new variables to this context.
 * </p>
 */
public final class RenderizationContext {

   private final Map<String, Object> values = new HashMap<>();
   private final RenderizationContext parent;
   private final Locale locale;

   /**
    * Creates a new {@code RenderizationContext}.
    */
   public RenderizationContext() {
      this.locale = Locale.getDefault();
      this.parent = null;
   }

   /**
    * Creates a new {@code RenderizationContext} using the given locale.
    *
    * <p>
    * Locales may be used during the renderisation to enable a template to use information about a
    * specific geographical, political, or cultural region. This locale may differ from the one used
    * by the application.
    * </p>
    *
    * @param locale the locale to be used with this context.
    */
   public RenderizationContext(Locale locale) {
      this.locale = locale;
      this.parent = null;
   }

   /**
    * Creates a new {@code RenderizationContext} using another context as parent.
    *
    * <p>
    * Using another context as a parent to a new context enables this context to define variables
    * that belong only to the new context while using and updating values of the parent context. If
    * the current context is later discarded, changes to variables in the parent context propagate
    * while variables created in the new context do not.
    * </p>
    *
    * @param parent the parent context.
    */
   public RenderizationContext(RenderizationContext parent) {
      this.locale = parent.locale;
      this.parent = parent;
   }

   /**
    * Returns the value of the given variable.
    *
    * <p>
    * Variables may be a composed of multiple level values, thus indicating the desire for a
    * specific sub-value. This can be done in two ways:
    * </p>
    *
    * <ul>
    * <li>By indicating the name of a sub-variable separated by a dot ('.'); or
    * <li>By indicating an array index around square brackets (i.e. [7] or ['seven'])
    * </ul>
    *
    * @param name the name of the variable to resolve.
    * @return the value of the variable or {@code null} if the variable, or any intermetiate values,
    *         are not defined in this context.
    */
   public Object get(String name) {
      String[] parts = name.replace("[", ".[").split("\\.");
      Map<String, Object> map = new HashMap<>();
      if (parent != null) {
         map.putAll(parent.values);
      }
      map.putAll(this.values);
      Object value = map;
      for (String part : parts) {
         if (value == null) {
            break;
         } else if (part.matches("\\[.*\\]")) {
            String index = part.substring(1, part.length() - 1);
            if ("'\"".indexOf(index.charAt(0)) >= 0) {
               char ch = index.charAt(0);
               index = index.substring(1);
               if (index.charAt(index.length() - 1) == ch) {
                  index = index.substring(0, index.length() - 1);
               }
            }
            value = getIndex(value, index);
         } else {
            value = getProperty(value, part);
         }
      }
      return value;
   }

   /**
    * Changes the value of a single variable.
    *
    * <p>
    * Different from the {@code #get(String)} method, this method can only change a single value
    * directly in the current context and thus cannot change values in composite variables or
    * arrays.
    * </p>
    *
    * @param name the name of the variable to set.
    * @param value the new value of variable.
    */
   public void set(String name, Object value) {
      if (name.contains(".")) {
         throw new IllegalArgumentException(name);
      }
      if (parent != null && parent.values.containsKey(name)) {
         parent.values.put(name, value);
      } else {
         values.put(name, value);
      }
   }

   /**
    * Returns the parent context of the current context, if any.
    *
    * @return the parent context of the current context or {@code null} if the current context has
    *         no parent context.
    */
   public RenderizationContext getParent() {
      return parent;
   }

   /**
    * Returns the locale of the current context.
    *
    * @return the locale of the current context.
    */
   public Locale getLocale() {
      return locale;
   }

   private Object getIndex(Object value, String index) {
      if (value.getClass().isArray() && index.matches("\\d+")) {
         int i = Integer.parseInt(index);
         if (i >= 0 && i < Array.getLength(value)) {
            return Array.get(value, i);
         }
      } else if (value instanceof List && index.matches("\\d+")) {
         int i = Integer.parseInt(index);
         if (i >= 0 && i < ((List<?>) value).size()) {
            return ((List<?>) value).get(i);
         }
      } else if (value instanceof Collection && index.matches("\\d+")) {
         Object[] list = ((Collection<?>) value).toArray();
         int i = Integer.parseInt(index);
         if (i >= 0 && i < list.length) {
            return list[i];
         }
      } else if (value instanceof Map) {
         return ((Map<?, ?>) value).get(index);
      }
      return null;
   }

   private Object getProperty(Object value, String name) {
      if (value instanceof Map) {
         return ((Map<?, ?>) value).get(name);
      } else {
         String methodName = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
         Method[] methods = Arrays.stream(value.getClass().getMethods()) //
                  .filter(m -> m.getName().equalsIgnoreCase(methodName)
                           && m.getParameterCount() == 0 && m.getReturnType() != void.class)
                  .toArray(Method[]::new);
         if (methods.length == 1) {
            try {
               methods[0].setAccessible(true);
               return methods[0].invoke(value);
            } catch (InvocationTargetException e) {
               throw new IllegalStateException(e.getCause());
            } catch (IllegalAccessException | IllegalArgumentException e) {
               throw new IllegalStateException(e);
            }
         }
      }
      return null;
   }
}
