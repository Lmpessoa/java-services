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
package com.lmpessoa.services.internal.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;
import java.util.UUID;

import javax.validation.constraints.Min;
import javax.validation.constraints.Negative;
import javax.validation.constraints.Null;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.Query;
import com.lmpessoa.services.internal.parsing.ParseException;
import com.lmpessoa.services.internal.routing.RoutePatternParser;
import com.lmpessoa.services.internal.routing.VariableRoutePart;

public final class RoutePatternParserTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private static VariableRoutePart parseVariable(String variable) {
      Method method;
      try {
         method = RoutePatternParserTest.class.getMethod("methodToTest", int.class, String.class,
                  UUID.class, String.class, long.class, String.class, String.class);
      } catch (NoSuchMethodException | SecurityException e) {
         throw new ParseException(e.getMessage(), 0);
      }
      return new RoutePatternParser(RoutePatternParserTest.class, method, "").parseVariable(0,
               variable);
   }

   public void methodToTest(@Min(7) int number,
      @Pattern(regexp = "[0-9a-f]+") @Size(max = 12) String hexNumber, UUID uuid,
      @Negative String numString, @Size(min = 7) long strNumber, @Null String nullValue,
      @Null @Query String nullQueryParam) {
      // Nothing to do here
   }

   @Test
   public void testIntVariableWithMin() {
      VariableRoutePart var = parseVariable("number");
      assertEquals(0, var.getParameterIndex());
      assertEquals("number", var.getParameterName());
      assertEquals("(\\d+)", var.getRegexPattern());
      assertEquals(7, var.getMinValue().intValue());
      assertNull(var.getMaxValue());
   }

   @Test
   public void testStringVariableWithPatternSize() {
      VariableRoutePart var = parseVariable("1");
      assertEquals(1, var.getParameterIndex());
      assertEquals("hexNumber", var.getParameterName());
      assertEquals("((?=[0-9a-f]+)[^\\/]{,12})", var.getRegexPattern());
      assertNull(var.getMinValue());
      assertNull(var.getMaxValue());
   }

   @Test
   public void testUuidVariable() {
      VariableRoutePart var = parseVariable("uuid");
      assertEquals(2, var.getParameterIndex());
      assertEquals("uuid", var.getParameterName());
      assertEquals("([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})",
               var.getRegexPattern());
      assertNull(var.getMinValue());
      assertNull(var.getMaxValue());
   }

   @Test
   public void testStringVariableWithNegative() {
      thrown.expect(ParseException.class);
      thrown.expectMessage("Expected number type, was java.lang.String");
      parseVariable("3");
   }

   @Test
   public void testLongVariableWithSize() {
      thrown.expect(ParseException.class);
      thrown.expectMessage("Expected string type, was long");
      parseVariable("4");
   }

   @Test
   public void testNullVariable() {
      thrown.expect(ParseException.class);
      thrown.expectMessage("Path argument cannot be null");
      parseVariable("5");
   }

   @Test
   public void testNullVariableWithQueryParam() {
      thrown.expect(ParseException.class);
      thrown.expectMessage("Query parameters cannot be in path");
      parseVariable("6");
   }
}
