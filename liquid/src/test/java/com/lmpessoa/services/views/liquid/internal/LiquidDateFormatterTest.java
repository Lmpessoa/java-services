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
package com.lmpessoa.services.views.liquid.internal;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import org.junit.Test;

public class LiquidDateFormatterTest {

   private static final ZonedDateTime DATE = ZonedDateTime.of(2017, 6, 5, 5, 42, 7, 0,
            ZoneId.of("America/Sao_Paulo"));
   private static final LiquidDateFormatter FORMATTER = new LiquidDateFormatter(
            Locale.forLanguageTag("en-GB"));

   @Test
   public void testLiteralPercent() {
      String result = FORMATTER.format(DATE, "%%");
      assertEquals("%", result);
   }

   @Test
   public void testShortWeekdayName() {
      String result = FORMATTER.format(DATE, "%a");
      assertEquals("Mon", result);
   }

   @Test
   public void testFullWeekdayName() {
      String result = FORMATTER.format(DATE, "%A");
      assertEquals("Monday", result);
   }

   @Test
   public void testShortMonthName_b() {
      String result = FORMATTER.format(DATE, "%b");
      assertEquals("Jun", result);
   }

   @Test
   public void testShortMonthName_h() {
      String result = FORMATTER.format(DATE, "%h");
      assertEquals("Jun", result);
   }

   @Test
   public void testFullMonthName() {
      String result = FORMATTER.format(DATE, "%B");
      assertEquals("June", result);
   }

   @Test
   public void testDayOfTheMonthWithZeroes() {
      String result = FORMATTER.format(DATE, "%d");
      assertEquals("05", result);
   }

   @Test
   public void testDayOfTheMonth() {
      String result = FORMATTER.format(DATE, "%e");
      assertEquals(" 5", result);
   }

   @Test
   public void testHourWith24HoursAndZeroes() {
      String result = FORMATTER.format(DATE.plusHours(12), "%H");
      assertEquals("17", result);
   }

   @Test
   public void testHourWith12HoursAndZeroes() {
      String result = FORMATTER.format(DATE.plusHours(12), "%I");
      assertEquals("05", result);
   }

   @Test
   public void testDayOfTheYear() {
      String result = FORMATTER.format(DATE, "%j");
      assertEquals("156", result);
   }

   @Test
   public void testHourWith24Hours() {
      String result = FORMATTER.format(DATE.plusHours(12), "%k");
      assertEquals("17", result);
   }

   @Test
   public void testHourWith12Hours() {
      String result = FORMATTER.format(DATE.plusHours(12), "%l");
      assertEquals(" 5", result);
   }

   @Test
   public void testMonthWithZeroes() {
      String result = FORMATTER.format(DATE, "%m");
      assertEquals("06", result);
   }

   @Test
   public void testMinutesWithZeroes() {
      String result = FORMATTER.format(DATE, "%M");
      assertEquals("42", result);
   }

   @Test
   public void testMeridianIndicator() {
      String result = FORMATTER.format(DATE, "%p");
      assertEquals("AM", result);
   }

   @Test
   public void testSecondsWithZeroes() {
      String result = FORMATTER.format(DATE, "%S");
      assertEquals("07", result);
   }

   @Test
   public void testWeekNumberStartingSunday() {
      String result = FORMATTER.format(DATE, "%U");
      assertEquals("23", result);
   }

   @Test
   public void testWeekNumberStartingMonday() {
      String result = FORMATTER.format(DATE, "%W");
      assertEquals("23", result);
   }

   @Test
   public void testWeekNumberStartingSunday_2() {
      String result = FORMATTER.format(LocalDate.of(2018, 8, 3), "%U");
      assertEquals("30", result);
   }

   @Test
   public void testWeekNumberStartingMonday_2() {
      String result = FORMATTER.format(LocalDate.of(2018, 8, 3), "%W");
      assertEquals("31", result);
   }

   @Test
   public void testWeekNumberStartingSunday_3() {
      String result = FORMATTER.format(LocalDate.of(2015, 11, 22), "%U");
      assertEquals("47", result);
   }

   @Test
   public void testWeekNumberStartingMonday_3() {
      String result = FORMATTER.format(LocalDate.of(2015, 11, 22), "%W");
      assertEquals("46", result);
   }

   @Test
   public void testDayOfTheWeek() {
      String result = FORMATTER.format(DATE, "%w");
      assertEquals("1", result);
   }

   @Test
   public void testYearWithoutCentury() {
      String result = FORMATTER.format(DATE, "%y");
      assertEquals("17", result);
   }

   @Test
   public void testYearWithCentury() {
      String result = FORMATTER.format(DATE, "%Y");
      assertEquals("2017", result);
   }

   @Test
   public void testCentury() {
      String result = FORMATTER.format(DATE, "%C");
      assertEquals("21", result);
   }

   @Test
   public void testTimeZoneName() {
      String result = FORMATTER.format(DATE, "%Z");
      assertEquals("BRT", result);
   }

   @Test
   public void testTimeZoneOffset() {
      String result = FORMATTER.format(DATE, "%z");
      assertEquals("-0300", result);
   }

   @Test
   public void testTimeZoneOffset_2() {
      String result = FORMATTER.format(DATE, "%:z");
      assertEquals("-03:00", result);
   }

   @Test
   public void testTimeZoneOffset_3() {
      String result = FORMATTER.format(DATE, "%::z");
      assertEquals("-03:00:00", result);
   }

   @Test
   public void testTimeZoneOffset_4() {
      String result = FORMATTER.format(DATE, "%:::z");
      assertEquals("-03", result);
   }

   @Test
   public void testFormatDateTime() {
      String result = FORMATTER.format(DATE, "%c");
      assertEquals("Mon Jun  5 05:42:07 2017", result);
   }

   @Test
   public void testSimpleDate() {
      String result = FORMATTER.format(DATE, "%D");
      assertEquals("06/05/17", result);
   }

   @Test
   public void testIso8601Date() {
      String result = FORMATTER.format(DATE, "%F");
      assertEquals("2017-06-05", result);
   }

   @Test
   public void testVmsDate() {
      String result = FORMATTER.format(DATE, "%v");
      assertEquals(" 5-JUN-2017", result);
   }

   @Test
   public void test12HourTime() {
      String result = FORMATTER.format(DATE, "%r");
      assertEquals("05:42:07 AM", result);
   }

   @Test
   public void testFullDateTime() {
      String result = FORMATTER.format(DATE, "%+");
      assertEquals("Mon Jun  5 05:42:07 BRT 2017", result);
   }

   @Test
   public void testDayOfTheYearWithUpcase() {
      String result = FORMATTER.format(DATE, "%^j");
      assertEquals("156", result);
   }

   @Test
   public void testDayOfTheYearWithMoreZeroes() {
      String result = FORMATTER.format(DATE, "%5j");
      assertEquals("00156", result);
   }

   @Test
   public void testDayOfTheYearStrippingMoreZeroes() {
      String result = FORMATTER.format(DATE, "%-5j");
      assertEquals("156", result);
   }

   @Test
   public void testDayOfTheYearWithBlanks() {
      String result = FORMATTER.format(DATE, "%_5j");
      assertEquals("  156", result);
   }

   @Test
   public void testDayOfTheMonthWithoutZeroes() {
      String result = FORMATTER.format(DATE.minusMonths(5), "%-j");
      assertEquals("5", result);
   }

   @Test
   public void testDayOfTheMonthWithLength() {
      String result = FORMATTER.format(DATE.minusMonths(5), "%2j");
      assertEquals("05", result);
   }

   @Test
   public void testMonthUpcase() {
      String result = FORMATTER.format(DATE, "%^b");
      assertEquals("JUN", result);
   }

   @Test
   public void testMonthAligned() {
      String result = FORMATTER.format(DATE, "%^9B");
      assertEquals("     JUNE", result);
   }
}
