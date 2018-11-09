/*
 * Copyright (c) 2017 Leonardo Pessoa
 * http://github.com/lmpessoa/java-services
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
package com.lmpessoa.services.routing;

import com.lmpessoa.util.parsing.IVariablePart;

/**
 * An <code>AbstractRouteType</code> describes the common ground for all route types used with the
 * routing engine.
 *
 * <p>
 * A route type shall describe an unique rule to match an URI against. These may describe any static
 * kind of content that might be matched from an URI, i.e. a segment all composed by numbers. This
 * rule should be immutable except for the optional constraint of minimum and maximum length the
 * segment may have to match the route type instance.
 * </p>
 *
 * <p>
 * Subclasses inheriting from this class must be registered to be recognised by the engine.
 * </p>
 *
 * @see AlphaRouteType
 * @see AnyRouteType
 * @see IntRouteType
 * @see HexRouteType
 */
public abstract class AbstractRouteType implements IVariablePart {

   private final int minLength;
   private final int maxLength;

   /**
    * Creates a new route type with the given length constraints.
    *
    * <p>
    * Subclasses must call this constructor providing both values; the minimum length cannot be less
    * than one (as there is no reason to expect a zero-length URI segment) and the maximum length
    * cannot be less than the minimum length. Both however can have the same value meaning the
    * matching route must have exactly that length. If the route type to be created does not wish
    * specify a maximum length, the value <code>-1</code> can be provided instead.
    * </p>
    *
    * @param minLength the minimum length of the matching route value.
    * @param maxLength the maximum length of the matching route value.
    */
   protected AbstractRouteType(int minLength, int maxLength) {
      if (minLength < 1) {
         throw new IllegalArgumentException("Minimum length cannot be less than one");
      }
      if (maxLength < -1 || maxLength > 0 && maxLength < minLength) {
         throw new IllegalArgumentException("Maximum length cannot be less than minimum");
      }
      this.minLength = minLength;
      this.maxLength = maxLength;
   }

   /**
    * Returns the expected regular expression modifier for the length requirements of this route
    * type.
    *
    * @return the expected regular expression modifier for the length requirements of this route
    * type.
    */
   protected final String getRegexLength() {
      if (minLength == 1 && maxLength == -1) {
         return "+";
      } else if (minLength == maxLength) {
         return "{" + minLength + "}";
      } else if (minLength > 1 && maxLength == -1) {
         return "{" + minLength + ",}";
      } else if (minLength == 1 && maxLength > 0) {
         return "{," + maxLength + "}";
      }
      return "{" + minLength + "," + maxLength + "}";
   }

   /**
    * Returns the name used to identify occurrences of this type in routes.
    *
    * @return the name used to identify occurrences of this type in routes.
    */
   protected abstract String getName();

   /**
    * Returns the regular expression used to validate this type in routes.
    *
    * <p>
    * Subclasses must provide an implementation of this method which returns a valid regular
    * expression to match only the part of this route type. The returned value must not use
    * parenthesis.
    * </p>
    *
    * @return the regular expression used to validate this type in routes.
    */
   protected abstract String getRegex();

   /**
    * Returns whether values matched by this route type can be assigned to variables of the given
    * type.
    *
    * <p>
    * It is possible many classes are capable of accepting values matched by a route type.
    * Subclasses implementing this method should test only for the prefered known types. Note that
    * any route type will always be assignable to a <code>String</code> variable so there is no need
    * to test for this type.
    * </p>
    *
    * @param clazz the type to check if it is compatible with this route type.
    * @return <code>true</code> if values matched by thos route type can be assigned to variables of
    * the given type, <code>false</code> otherwise.
    */
   protected abstract boolean isAssignableTo(Class<?> clazz);

   /**
    * Returns the minimum length this route type instance will recognise.
    *
    * @return the minimum length this route type instance will recognise.
    */
   public final int getMinLength() {
      return minLength;
   }

   /**
    * Returns the maximum length this route type instance will recognise.
    *
    * <p>
    * If the value returned by this method is negative (<code>-1</code>) means this route type does
    * not have a constraint on the maximum length a URL segment might have to match it.
    * </p>
    *
    * @return the maximum length this route type instance will recognise.
    */
   public final int getMaxLength() {
      return maxLength;
   }

   @Override
   public final String toString() {
      StringBuilder result = new StringBuilder();
      result.append('{');
      result.append(getName().toLowerCase());
      if (minLength != 1 || maxLength != -1) {
         result.append('(');
         if (minLength != 1) {
            result.append(minLength);
         }
         if (minLength != maxLength) {
            result.append("..");
            if (maxLength != -1) {
               result.append(maxLength);
            }
         }
         result.append(')');
      }
      result.append('}');
      return result.toString();
   }

}
