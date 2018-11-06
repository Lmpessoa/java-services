/*
 * A light and easy engine for developing web APIs and microservices.
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

import com.lmpessoa.utils.parsing.IVariablePart;

/**
 * An <code>AbstractRouteType</code> describes the common ground for all route types used with the
 * routing engine.
 *
 * <p>
 * A route type shall describe an unique rule to match an URI against. These may describe any static
 * kind of content that might be matched from an URI, i.e. a segment all composed by numbers. This
 * rule should be immuteble except for the optional constraint of minimum and maximum length the
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

   /**
    * Returns a standardised representation of this route type using the given name.
    *
    * @param name the representation name of the route type
    * @return a standardised representation of this route type using the given name.
    */
   protected final String toString(String name) {
      StringBuilder result = new StringBuilder();
      result.append('{');
      result.append(name);
      if (minLength != 1 || maxLength != -1) {
         if (minLength != 1) {
            result.append(minLength);
         }
         if (minLength != maxLength) {
            result.append("..");
            if (maxLength != -1) {
               result.append(maxLength);
            }
         }
      }
      result.append('}');
      return result.toString();
   }
}
