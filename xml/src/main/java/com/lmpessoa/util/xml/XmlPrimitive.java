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
package com.lmpessoa.util.xml;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import com.google.gson.internal.LazilyParsedNumber;

public final class XmlPrimitive extends XmlElement {

   private final Object value;

   public XmlPrimitive(Boolean value) {
      this.value = Objects.requireNonNull(value);
   }

   public XmlPrimitive(Character value) {
      this.value = Objects.requireNonNull(value).toString();
   }

   public XmlPrimitive(Number value) {
      this.value = Objects.requireNonNull(value);
   }

   public XmlPrimitive(CharSequence value) {
      this.value = Objects.requireNonNull(value).toString();
   }

   /**
    * Check whether this primitive contains a boolean value.
    *
    * @return true if this primitive contains a boolean value, false otherwise.
    */
   public boolean isBoolean() {
      return value instanceof Boolean;
   }

   /**
    * Convenience method to get this element as a boolean value.
    *
    * @return get this element as a primitive boolean value.
    */
   public boolean getAsBoolean() {
      if (isBoolean()) {
         return getAsBooleanWrapper().booleanValue();
      } else {
         // Check to see if the value as a String is "true" in any case.
         return Boolean.parseBoolean(getAsString());
      }
   }

   /**
    * Check whether this primitive contains a Number.
    *
    * @return true if this primitive contains a Number, false otherwise.
    */
   public boolean isNumber() {
      return value instanceof Number;
   }

   /**
    * Convenience method to get this element as a Number.
    *
    * @return get this element as a Number.
    * @throws NumberFormatException if the value contained is not a valid Number.
    */
   public Number getAsNumber() {
      return value instanceof String ? new LazilyParsedNumber((String) value) : (Number) value;
   }

   /**
    * Check whether this primitive contains a String value.
    *
    * @return true if this primitive contains a String value, false otherwise.
    */
   public boolean isString() {
      return value instanceof String;
   }

   /**
    * Convenience method to get this element as a String.
    *
    * @return get this element as a String.
    */
   public String getAsString() {
      if (isNumber()) {
         return getAsNumber().toString();
      } else if (isBoolean()) {
         return getAsBooleanWrapper().toString();
      } else {
         return (String) value;
      }
   }

   /**
    * Convenience method to get this element as a primitive double.
    *
    * @return get this element as a primitive double.
    * @throws NumberFormatException if the value contained is not a valid double.
    */
   public double getAsDouble() {
      return isNumber() ? getAsNumber().doubleValue() : Double.parseDouble(getAsString());
   }

   /**
    * Convenience method to get this element as a {@link BigDecimal}.
    *
    * @return get this element as a {@link BigDecimal}.
    * @throws NumberFormatException if the value contained is not a valid {@link BigDecimal}.
    */
   public BigDecimal getAsBigDecimal() {
      return value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
   }

   /**
    * Convenience method to get this element as a {@link BigInteger}.
    *
    * @return get this element as a {@link BigInteger}.
    * @throws NumberFormatException if the value contained is not a valid {@link BigInteger}.
    */
   public BigInteger getAsBigInteger() {
      return value instanceof BigInteger ? (BigInteger) value : new BigInteger(value.toString());
   }

   /**
    * Convenience method to get this element as a float.
    *
    * @return get this element as a float.
    * @throws NumberFormatException if the value contained is not a valid float.
    */
   public float getAsFloat() {
      return isNumber() ? getAsNumber().floatValue() : Float.parseFloat(getAsString());
   }

   /**
    * Convenience method to get this element as a primitive long.
    *
    * @return get this element as a primitive long.
    * @throws NumberFormatException if the value contained is not a valid long.
    */
   public long getAsLong() {
      return isNumber() ? getAsNumber().longValue() : Long.parseLong(getAsString());
   }

   /**
    * Convenience method to get this element as a primitive short.
    *
    * @return get this element as a primitive short.
    * @throws NumberFormatException if the value contained is not a valid short value.
    */
   public short getAsShort() {
      return isNumber() ? getAsNumber().shortValue() : Short.parseShort(getAsString());
   }

   /**
    * Convenience method to get this element as a primitive integer.
    *
    * @return get this element as a primitive integer.
    * @throws NumberFormatException if the value contained is not a valid integer.
    */
   public int getAsInt() {
      return isNumber() ? getAsNumber().intValue() : Integer.parseInt(getAsString());
   }

   public byte getAsByte() {
      return isNumber() ? getAsNumber().byteValue() : Byte.parseByte(getAsString());
   }

   public char getAsCharacter() {
      return getAsString().charAt(0);
   }

   @Override
   public int hashCode() {
      if (value == null) {
         return 31;
      }
      // Using recommended hashing algorithm from Effective Java for longs and doubles
      if (isIntegral(this)) {
         long value = getAsNumber().longValue();
         return (int) (value ^ value >>> 32);
      }
      if (value instanceof Number) {
         long value = Double.doubleToLongBits(getAsNumber().doubleValue());
         return (int) (value ^ value >>> 32);
      }
      return value.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null || !(obj instanceof XmlPrimitive)) {
         return false;
      }
      XmlPrimitive other = (XmlPrimitive) obj;
      if (value == null) {
         return other.value == null;
      }
      if (isIntegral(this) && isIntegral(other)) {
         return getAsNumber().longValue() == other.getAsNumber().longValue();
      }
      if (value instanceof Number && other.value instanceof Number) {
         double a = getAsNumber().doubleValue();
         // Java standard types other than double return true for two NaN. So, need
         // special handling for double.
         double b = other.getAsNumber().doubleValue();
         return a == b || Double.isNaN(a) && Double.isNaN(b);
      }
      return value.equals(other.value);
   }

   @Override
   protected String buildXmlAtLevel(int indentLevel) {
      return value.toString();
   }

   private Boolean getAsBooleanWrapper() {
      return (Boolean) value;
   }

   private static boolean isIntegral(XmlPrimitive primitive) {
      if (primitive.value instanceof Number) {
         Number number = (Number) primitive.value;
         return number instanceof BigInteger || number instanceof Long || number instanceof Integer
                  || number instanceof Short || number instanceof Byte;
      }
      return false;
   }
}
