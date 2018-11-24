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
package com.lmpessoa.services.core.internal.validating;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import javax.validation.UnexpectedTypeException;
import javax.validation.constraints.*;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.validating.ErrorSet;
import com.lmpessoa.services.core.validating.IValidationService;
import com.lmpessoa.services.core.validating.ErrorSet.Entry;

public class ContraintValidationTest {

   private IValidationService validator = new ValidationService();

   static {
      Locale.setDefault(Locale.forLanguageTag("en-GB"));
   }

   @Before
   public void setup() {
      ZoneOffset bsa = ZoneOffset.ofHours(-3);
      ConstraintAnnotation.setClock(
               Clock.fixed(OffsetDateTime.of(2017, 6, 5, 5, 42, 0, 0, bsa).toInstant(), bsa));
   }

   @Test
   public void testNullWithNull() {
      Object object = new Object() {

         @Null
         private Object value = null;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testNullWithObject() {
      Object innerValue = new Object();
      Object object = new Object() {

         @Null
         public Object value = innerValue;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Null.message}", error.getMessageTemplate());
      assertEquals("must be null", error.getMessage());
      assertEquals("<java.lang.Object>", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testNullWithOptionalEmpty() {
      Object object = new Object() {

         @Null
         private OptionalInt value = OptionalInt.empty();
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testNullWithOptionalPresent() {
      Object object = new Object() {

         @Null
         private OptionalDouble value = OptionalDouble.of(1);
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Null.message}", error.getMessageTemplate());
      assertEquals("must be null", error.getMessage());
      assertEquals("1.0", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testNotNullWithObject() {
      Object object = new Object() {

         @NotNull
         private Object value = new Object();
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testNotNullWithNull() {
      Object object = new Object() {

         @NotNull
         private Object value = null;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.NotNull.message}", error.getMessageTemplate());
      assertEquals("must not be null", error.getMessage());
      assertSame("null", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testNotNullWithOptionalPresent() {
      Object object = new Object() {

         @NotNull
         private Optional<String> value = Optional.of("test");
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testNotNullWithOptionalEmpty() {
      Object object = new Object() {

         @NotNull
         private OptionalLong value = OptionalLong.empty();
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.NotNull.message}", error.getMessageTemplate());
      assertEquals("must not be null", error.getMessage());
      assertSame("null", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testAssertTrueWithTrue() {
      Object object = new Object() {

         @AssertTrue
         private boolean value = true;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testAssertTrueWithFalse() {
      Object object = new Object() {

         @AssertTrue
         private boolean value = false;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.AssertTrue.message}", error.getMessageTemplate());
      assertEquals("must be true", error.getMessage());
      assertEquals("false", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testAssertTrueWithString() {
      Object object = new Object() {

         @AssertTrue
         private String value = "true";
      };
      validator.validate(object);
   }

   @Test
   public void testAssertFalseWithFalse() {
      Object object = new Object() {

         @AssertFalse
         private boolean value = false;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testAssertFalseWithTrue() {
      Object object = new Object() {

         @AssertFalse
         private boolean value = true;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.AssertFalse.message}",
               error.getMessageTemplate());
      assertEquals("must be false", error.getMessage());
      assertEquals("true", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testAssertTalseWithString() {
      Object object = new Object() {

         @AssertFalse
         private String value = "false";
      };
      validator.validate(object);
   }

   @Test
   public void testMinValid() {
      Object object = new Object() {

         @Min(7)
         private int value = 8;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testMinInvalid() {
      Object object = new Object() {

         @Min(7)
         private int value = 6;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Min.message}", error.getMessageTemplate());
      assertEquals("must be greater than or equal to 7", error.getMessage());
      assertEquals("6", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testMinWithLong() {
      Object object = new Object() {

         @Min(7)
         private long value = 7;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testMinWithBigInteger() {
      Object object = new Object() {

         @Min(7)
         private BigDecimal value = BigDecimal.TEN;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testMinWithOptionalInt() {
      Object object = new Object() {

         @Min(7)
         private OptionalInt value = OptionalInt.of(8);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testMinWithDouble() {
      Object object = new Object() {

         @Min(7)
         private double value = 8;
      };
      validator.validate(object);
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testMinWithOptionalDouble() {
      Object object = new Object() {

         @Min(7)
         private OptionalDouble value = OptionalDouble.of(8);
      };
      validator.validate(object);
   }

   @Test
   public void testMaxValid() {
      Object object = new Object() {

         @Max(7)
         private int value = 6;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testMaxInvalid() {
      Object object = new Object() {

         @Max(7)
         private int value = 8;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Max.message}", error.getMessageTemplate());
      assertEquals("must be less than or equal to 7", error.getMessage());
      assertEquals("8", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testMaxWithLong() {
      Object object = new Object() {

         @Max(7)
         private long value = 7;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testMaxWithBigInteger() {
      Object object = new Object() {

         @Max(7)
         private BigDecimal value = BigDecimal.ONE;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testMaxWithOptionalLong() {
      Object object = new Object() {

         @Max(7)
         private OptionalLong value = OptionalLong.of(1);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testMaxWithDouble() {
      Object object = new Object() {

         @Max(7)
         private double value = 6;
      };
      validator.validate(object);
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testMaxWithOptionalDouble() {
      Object object = new Object() {

         @Max(7)
         private OptionalDouble value = OptionalDouble.of(6);
      };
      validator.validate(object);
   }

   @Test
   public void testDecimalMinValid() {
      Object object = new Object() {

         @DecimalMin("7")
         private int value = 8;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testDecimalMinInvalid() {
      Object object = new Object() {

         @DecimalMin("7")
         private int value = 6;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.DecimalMin.message}", error.getMessageTemplate());
      assertEquals("must be greater than or equal to 7", error.getMessage());
      assertEquals("6", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testDecimalMinInvalidNotInclusive() {
      Object object = new Object() {

         @DecimalMin(value = "7", inclusive = false)
         private int value = 7;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.DecimalMin.message}", error.getMessageTemplate());
      assertEquals("must be greater than or equal to 7", error.getMessage());
      assertEquals("7", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testDecimalMinWithLong() {
      Object object = new Object() {

         @DecimalMin("7")
         private long value = 7;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testDecimalMinWithBigInteger() {
      Object object = new Object() {

         @DecimalMin("7")
         private BigInteger value = BigInteger.TEN;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testDecimalMinWithDouble() {
      Object object = new Object() {

         @DecimalMin("7")
         private double value = 8;
      };
      validator.validate(object);
   }

   @Test
   public void testDecimalMaxValid() {
      Object object = new Object() {

         @DecimalMax("7")
         private int value = 6;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testDecimalMaxInvalid() {
      Object object = new Object() {

         @DecimalMax("7")
         private int value = 8;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.DecimalMax.message}", error.getMessageTemplate());
      assertEquals("must be less than or equal to 7", error.getMessage());
      assertEquals("8", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testDecimalMaxInvalidNotInclusive() {
      Object object = new Object() {

         @DecimalMax(value = "7", inclusive = false)
         private int value = 7;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.DecimalMax.message}", error.getMessageTemplate());
      assertEquals("must be less than or equal to 7", error.getMessage());
      assertEquals("7", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testDecimalMaxWithLong() {
      Object object = new Object() {

         @DecimalMax("7")
         private long value = 7;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testDecimalMaxWithBigInteger() {
      Object object = new Object() {

         @DecimalMax("7")
         private BigInteger value = BigInteger.ONE;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testDecimalMaxWithDouble() {
      Object object = new Object() {

         @DecimalMax("7")
         private double value = 6;
      };
      validator.validate(object);
   }

   @Test
   public void testNegativeWithNegative() {
      Object object = new Object() {

         @Negative
         private int value = -1;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testNegativeWithPositive() {
      Object object = new Object() {

         @Negative
         private int value = 1;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Negative.message}", error.getMessageTemplate());
      assertEquals("must be less than 0", error.getMessage());
      assertEquals("1", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testNegativeWithZero() {
      Object object = new Object() {

         @Negative
         private int value = 0;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Negative.message}", error.getMessageTemplate());
      assertEquals("must be less than 0", error.getMessage());
      assertEquals("0", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testNegativeWithLong() {
      Object object = new Object() {

         @Negative
         private long value = -1;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testNegativeWithBigInteger() {
      Object object = new Object() {

         @Negative
         private BigInteger value = BigInteger.valueOf(-1);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testNegativeWithDouble() {
      Object object = new Object() {

         @Negative
         private double value = -1;
      };
      validator.validate(object);
   }

   @Test
   public void testNegativeOrZeroWithNegative() {
      Object object = new Object() {

         @NegativeOrZero
         private int value = -1;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testNegativeOrZeroWithPositive() {
      Object object = new Object() {

         @NegativeOrZero
         private int value = 1;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.NegativeOrZero.message}",
               error.getMessageTemplate());
      assertEquals("must be less than or equal to 0", error.getMessage());
      assertEquals("1", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testNegativeOrZeroWithZero() {
      Object object = new Object() {

         @NegativeOrZero
         private int value = 0;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testNegativeOrZeroWithLong() {
      Object object = new Object() {

         @NegativeOrZero
         private long value = -1;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testNegativeOrZeroWithBigInteger() {
      Object object = new Object() {

         @NegativeOrZero
         private BigInteger value = BigInteger.ZERO;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testNegativeOrZeroWithDouble() {
      Object object = new Object() {

         @NegativeOrZero
         private double value = -1;
      };
      validator.validate(object);
   }

   @Test
   public void testPositiveWithPositive() {
      Object object = new Object() {

         @Positive
         private int value = 1;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testPositiveWithNegative() {
      Object object = new Object() {

         @Positive
         private int value = -1;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Positive.message}", error.getMessageTemplate());
      assertEquals("must be greater than 0", error.getMessage());
      assertEquals("-1", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testPositiveWithZero() {
      Object object = new Object() {

         @Positive
         private int value = 0;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Positive.message}", error.getMessageTemplate());
      assertEquals("must be greater than 0", error.getMessage());
      assertEquals("0", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testPositiveWithLong() {
      Object object = new Object() {

         @Positive
         private long value = 1;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testPositiveWithBigInteger() {
      Object object = new Object() {

         @Positive
         private BigInteger value = BigInteger.ONE;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testPositiveWithDouble() {
      Object object = new Object() {

         @Positive
         private double value = 1;
      };
      validator.validate(object);
   }

   @Test
   public void testPositiveOrZeroWithPositive() {
      Object object = new Object() {

         @PositiveOrZero
         private int value = 1;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testPositiveOrZeroWithNegative() {
      Object object = new Object() {

         @PositiveOrZero
         private int value = -1;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.PositiveOrZero.message}",
               error.getMessageTemplate());
      assertEquals("must be greater than or equal to 0", error.getMessage());
      assertEquals("-1", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testPositiveOrZeroWithZero() {
      Object object = new Object() {

         @PositiveOrZero
         private int value = 0;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testPositiveOrZeroWithLong() {
      Object object = new Object() {

         @PositiveOrZero
         private long value = 1;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testPositiveOrZeroWithBigInteger() {
      Object object = new Object() {

         @PositiveOrZero
         private BigInteger value = BigInteger.ZERO;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testPositiveOrZeroWithDouble() {
      Object object = new Object() {

         @PositiveOrZero
         private double value = 1;
      };
      validator.validate(object);
   }

   @Test
   public void testSizeMinValid() {
      Object object = new Object() {

         @Size(min = 7)
         private String value = "1234567";
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testSizeMinInvalid() {
      Object object = new Object() {

         @Size(min = 7)
         private String value = "123456";
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Size.message}", error.getMessageTemplate());
      assertEquals("size must be between 7 and " + Integer.MAX_VALUE, error.getMessage());
      assertEquals("123456", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testSizeMaxValid() {
      Object object = new Object() {

         @Size(max = 7)
         private String value = "1234567";
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testSizeMaxInvalid() {
      Object object = new Object() {

         @Size(max = 7)
         private String value = "12345678";
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Size.message}", error.getMessageTemplate());
      assertEquals("size must be between 0 and 7", error.getMessage());
      assertEquals("12345678", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testSizeWithCollection() {
      Object object = new Object() {

         @Size(min = 2)
         private Collection<String> value = Arrays.asList("123", "456");
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testSizeWithMap() {
      Map<String, String> innerValue = new HashMap<>();
      innerValue.put("123", "abc");
      innerValue.put("456", "def");
      Object object = new Object() {

         @Size(min = 2)
         private Map<String, String> value = innerValue;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testSizeWithArray() {
      Object object = new Object() {

         @Size(min = 2)
         private String[] value = new String[] { "123", "456" };
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testSizeWithBigInteger() {
      Object object = new Object() {

         @Size(min = 7)
         private BigInteger value = new BigInteger("1234567");
      };
      validator.validate(object);
   }

   @Test
   public void testDigitsIntegerValid() {
      Object object = new Object() {

         @Digits(integer = 3, fraction = 0)
         private int value = 100;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testDigitsIntegerInvalid() {
      Object object = new Object() {

         @Digits(integer = 3, fraction = 0)
         private int value = 1000;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Digits.message}", error.getMessageTemplate());
      assertEquals("numeric value out of bounds (<3 digits>.<0 digits> expected)",
               error.getMessage());
      assertEquals("1000", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testDigitsFractionValid() {
      Object object = new Object() {

         @Digits(integer = 3, fraction = 3)
         private BigDecimal value = new BigDecimal("123.456");
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testDigitsFractionInvalid() {
      Object object = new Object() {

         @Digits(integer = 3, fraction = 3)
         private BigDecimal value = new BigDecimal("123.4567");
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Digits.message}", error.getMessageTemplate());
      assertEquals("numeric value out of bounds (<3 digits>.<3 digits> expected)",
               error.getMessage());
      assertEquals("123.4567", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testDigitsWithString() {
      Object object = new Object() {

         @Digits(integer = 3, fraction = 3)
         private String value = "123.456";
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testDigitsWithDouble() {
      Object object = new Object() {

         @Digits(integer = 3, fraction = 3)
         private double value = 123.456;
      };
      validator.validate(object);
   }

   @Test
   public void testPastWithPast() {
      Object object = new Object() {

         @Past
         private LocalDate value = LocalDate.of(2016, 6, 5);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testPastWithFuture() {
      LocalDate innerValue = LocalDate.of(2018, 6, 5);
      Object object = new Object() {

         @Past
         private LocalDate value = innerValue;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Past.message}", error.getMessageTemplate());
      assertEquals("must be a past date", error.getMessage());
      assertEquals(innerValue.toString(), error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testPastWithPresent() {
      LocalDate innerValue = LocalDate.of(2017, 6, 5);
      Object object = new Object() {

         @Past
         private LocalDate value = innerValue;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Past.message}", error.getMessageTemplate());
      assertEquals("must be a past date", error.getMessage());
      assertEquals(innerValue.toString(), error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testPastWithDate() {
      Object object = new Object() {

         @Past
         private Date value = Date
                  .from(LocalDateTime.of(2016, 6, 5, 5, 42).toInstant(ZoneOffset.ofHours(-3)));
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testPastWithLocalTime() {
      Object object = new Object() {

         @Past
         private LocalTime value = LocalTime.of(5, 41);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testPastWithYearMonth() {
      Object object = new Object() {

         @Past
         private YearMonth value = YearMonth.of(2016, 6);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testPastWithString() {
      Object object = new Object() {

         @Past
         private String value = "2016-06-05";
      };
      validator.validate(object);
   }

   @Test
   public void testPastOrPresentWithPast() {
      Object object = new Object() {

         @PastOrPresent
         private LocalDate value = LocalDate.of(2016, 6, 5);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testPastOrPresentWithFuture() {
      LocalDate innerValue = LocalDate.of(2018, 6, 5);
      Object object = new Object() {

         @PastOrPresent
         private Object value = innerValue;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.PastOrPresent.message}",
               error.getMessageTemplate());
      assertEquals("must be a date in the past or in the present", error.getMessage());
      assertEquals(innerValue.toString(), error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testPastOrPresentWithPresent() {
      Object object = new Object() {

         @PastOrPresent
         private LocalDate value = LocalDate.of(2017, 6, 5);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testPastOrPresentWithDate() {
      Object object = new Object() {

         @PastOrPresent
         private Date value = Date
                  .from(LocalDateTime.of(2017, 6, 5, 5, 42).toInstant(ZoneOffset.ofHours(-3)));
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testPastOrPresentWithLocalTime() {
      Object object = new Object() {

         @PastOrPresent
         private LocalTime value = LocalTime.of(5, 42);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testPastOrPresentWithYearMonth() {
      Object object = new Object() {

         @PastOrPresent
         private YearMonth value = YearMonth.of(2017, 6);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testPastOrPresentWithString() {
      Object object = new Object() {

         @PastOrPresent
         private String value = "2017-06-05";
      };
      validator.validate(object);
   }

   @Test
   public void testFutureWithFuture() {
      Object object = new Object() {

         @Future
         private LocalDate value = LocalDate.of(2018, 6, 5);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testFutureWithPast() {
      LocalDate innerValue = LocalDate.of(2016, 6, 5);
      Object object = new Object() {

         @Future
         private LocalDate value = innerValue;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Future.message}", error.getMessageTemplate());
      assertEquals("must be a future date", error.getMessage());
      assertEquals(innerValue.toString(), error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testFutureWithPresent() {
      LocalDate innerValue = LocalDate.of(2017, 6, 5);
      Object object = new Object() {

         @Future
         private LocalDate value = innerValue;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Future.message}", error.getMessageTemplate());
      assertEquals("must be a future date", error.getMessage());
      assertEquals(innerValue.toString(), error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testFutureWithDate() {
      Object object = new Object() {

         @Future
         private Date value = Date
                  .from(LocalDateTime.of(2018, 6, 5, 5, 42).toInstant(ZoneOffset.ofHours(-3)));
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testFutureWithLocalTime() {
      Object object = new Object() {

         @Future
         private LocalTime value = LocalTime.of(5, 43);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testFutureWithYearMonth() {
      Object object = new Object() {

         @Future
         private YearMonth value = YearMonth.of(2018, 6);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testFutureWithString() {
      Object object = new Object() {

         @Future
         private String value = "2018-06-05";
      };
      validator.validate(object);
   }

   @Test
   public void testFutureOrPresentWithFuture() {
      Object object = new Object() {

         @FutureOrPresent
         private LocalDate value = LocalDate.of(2018, 6, 5);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testFutureOrPresentWithPast() {
      LocalDate innerValue = LocalDate.of(2016, 6, 5);
      Object object = new Object() {

         @FutureOrPresent
         private LocalDate value = innerValue;
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.FutureOrPresent.message}",
               error.getMessageTemplate());
      assertEquals("must be a date in the present or in the future", error.getMessage());
      assertEquals(innerValue.toString(), error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testFutureOrPresentWithPresent() {
      Object object = new Object() {

         @FutureOrPresent
         private LocalDate value = LocalDate.of(2017, 6, 5);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testFutureOrPresentWithDate() {
      Object object = new Object() {

         @FutureOrPresent
         private Date value = Date
                  .from(LocalDateTime.of(2017, 6, 5, 5, 42).toInstant(ZoneOffset.ofHours(-3)));
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testFutureOrPresentWithLocalTime() {
      Object object = new Object() {

         @FutureOrPresent
         private LocalTime value = LocalTime.of(5, 42);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testFutureOrPresentWithYearMonth() {
      Object object = new Object() {

         @FutureOrPresent
         private YearMonth value = YearMonth.of(2017, 6);
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testFutureOrPresentWithString() {
      Object object = new Object() {

         @FutureOrPresent
         private String value = "2017-06-05";
      };
      validator.validate(object);
   }

   @Test
   public void testPatternValid() {
      Object object = new Object() {

         @Pattern(regexp = "[A-Z]{2}[0-9]{9}[A-Z]{2}")
         private String value = "AL123456789UK";
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testPatternInvalid() {
      Object object = new Object() {

         @Pattern(regexp = "[A-Z]{2}[0-9]{9}[A-Z]{2}")
         private String value = "Al1376CA";
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Pattern.message}", error.getMessageTemplate());
      assertEquals("must match \"[A-Z]{2}[0-9]{9}[A-Z]{2}\"", error.getMessage());
      assertEquals("Al1376CA", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testPatternWithOptional() {
      Object object = new Object() {

         @Pattern(regexp = "[A-Z]{2}[0-9]{9}[A-Z]{2}")
         private Optional<String> value = Optional.of("AL123456789UK");
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testPatternWithLong() {
      Object object = new Object() {

         @Pattern(regexp = "[0-7]+")
         private long value = 170;
      };
      validator.validate(object);
   }

   @Test
   public void testNotEmptyWithEmpty() {
      Object object = new Object() {

         @NotEmpty
         private String value = "";
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.NotEmpty.message}", error.getMessageTemplate());
      assertEquals("must not be empty", error.getMessage());
      assertEquals("", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testNotEmptyWithString() {
      Object object = new Object() {

         @NotEmpty
         private String value = "123";
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testNotEmptyWithCollection() {
      Object object = new Object() {

         @NotEmpty
         private Collection<String> value = Arrays.asList("123");
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testNotEmptyWithMap() {
      Map<String, String> innerValue = new HashMap<>();
      innerValue.put("123", "abc");
      Object object = new Object() {

         @NotEmpty
         private Object value = innerValue;
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testNotEmptyWithArray() {
      Object object = new Object() {

         @NotEmpty
         private String[] value = new String[] { "123" };
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testNotEmptyWithBigInteger() {
      Object object = new Object() {

         @NotEmpty
         private BigInteger value = BigInteger.valueOf(123);
      };
      validator.validate(object);
   }

   @Test
   public void testNotBlankWithBlank() {
      Object object = new Object() {

         @NotBlank
         private String value = "    ";
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.NotBlank.message}", error.getMessageTemplate());
      assertEquals("must not be blank", error.getMessage());
      assertEquals("    ", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testNotBlankWithString() {
      Object object = new Object() {

         @NotBlank
         private String value = "       123  ";
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testNotBlankWithBigInteger() {
      Object object = new Object() {

         @NotBlank
         private BigInteger value = BigInteger.valueOf(123);
      };
      validator.validate(object);
   }

   @Test
   public void testEmailValid() {
      Object object = new Object() {

         @Email
         private String value = "info@lmpessoa.com";
      };
      ErrorSet result = validator.validate(object);
      assertTrue(result.isEmpty());
   }

   @Test
   public void testEmailInvalid() {
      Object object = new Object() {

         @Email
         private String value = "@Lmpessoa";
      };
      ErrorSet result = validator.validate(object);
      assertFalse(result.isEmpty());
      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Email.message}", error.getMessageTemplate());
      assertEquals("must be a well-formed email address", error.getMessage());
      assertEquals("@Lmpessoa", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test(expected = UnexpectedTypeException.class)
   public void testEmailWithBigInteger() {
      Object object = new Object() {

         @Email
         private BigInteger value = BigInteger.ONE;
      };
      validator.validate(object);
   }
}
