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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.routing.AbstractRouteType;
import com.lmpessoa.services.routing.AlphaRouteType;
import com.lmpessoa.services.routing.AnyRouteType;
import com.lmpessoa.services.routing.HexRouteType;
import com.lmpessoa.services.routing.IntRouteType;
import com.lmpessoa.services.routing.RoutePatternParser;

public final class RoutePatternParserTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private static final Map<String, Class<? extends AbstractRouteType>> types = new HashMap<>();

   static {
      types.put("hex", HexRouteType.class);
      types.put("int", IntRouteType.class);
      types.put("alpha", AlphaRouteType.class);
      types.put("any", AnyRouteType.class);
   }

   private static AbstractRouteType parseVariable(String variable) throws ParseException {
      return new RoutePatternParser("", types).parseVariable(0, variable);
   }

   @Test
   public void testVariableNoArgs() throws ParseException {
      AbstractRouteType var = parseVariable("alpha");
      assertTrue(var instanceof AlphaRouteType);
      assertEquals(1, var.getMinLength());
      assertEquals(-1, var.getMaxLength());
   }

   @Test
   public void testVariableOneArg() throws ParseException {
      AbstractRouteType var = parseVariable("alpha(7)");
      assertTrue(var instanceof AlphaRouteType);
      assertEquals(7, var.getMinLength());
      assertEquals(7, var.getMaxLength());
   }

   @Test
   public void testVariableTwoArgs() throws ParseException {
      AbstractRouteType var = parseVariable("alpha(4..7)");
      assertTrue(var instanceof AlphaRouteType);
      assertEquals(4, var.getMinLength());
      assertEquals(7, var.getMaxLength());
   }

   @Test
   public void testVariableOpenStart() throws ParseException {
      AbstractRouteType var = parseVariable("alpha(..7)");
      assertTrue(var instanceof AlphaRouteType);
      assertEquals(1, var.getMinLength());
      assertEquals(7, var.getMaxLength());
   }

   @Test
   public void testVariableOpenEnd() throws ParseException {
      AbstractRouteType var = parseVariable("alpha(4..)");
      assertTrue(var instanceof AlphaRouteType);
      assertEquals(4, var.getMinLength());
      assertEquals(-1, var.getMaxLength());
   }

   @Test
   public void testVariableEmptyArgs() throws ParseException {
      thrown.expect(ParseException.class);
      thrown.expectMessage("Length range expected");
      parseVariable("alpha()");
   }

   @Test
   public void testVariableIncompleteEmptyArgs() throws ParseException {
      thrown.expect(ParseException.class);
      thrown.expectMessage("Length range expected");
      parseVariable("alpha(..)");
   }

   @Test
   public void testVariableUnknownType() throws ParseException {
      thrown.expect(ParseException.class);
      thrown.expectMessage("Unknown route type: route");
      parseVariable("route");
   }
}
