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
package com.lmpessoa.util.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.util.parsing.AbstractParser;
import com.lmpessoa.util.parsing.ITemplatePart;
import com.lmpessoa.util.parsing.IVariablePart;
import com.lmpessoa.util.parsing.LiteralPart;

public final class AbstractParserTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void testParseOneVariableNoLiteral() throws ParseException {
      ITemplatePart[] result = TestParser.parse("{one}", false);
      assertEquals(1, result.length);
      assertTrue(result[0] instanceof TestVariablePart);
      assertEquals("one", ((TestVariablePart) result[0]).value);
   }

   @Test
   public void testParseLiteralNoVariables() throws ParseException {
      ITemplatePart[] result = TestParser.parse("literal", false);
      assertEquals(1, result.length);
      assertTrue(result[0] instanceof LiteralPart);
      assertEquals("literal", ((LiteralPart) result[0]).getValue());
   }

   @Test
   public void testParseTwoVariablesOneLiteral() throws ParseException {
      ITemplatePart[] result = TestParser.parse("{one}literal{two}", false);
      assertEquals(3, result.length);
      assertTrue(result[0] instanceof TestVariablePart);
      assertEquals("one", ((TestVariablePart) result[0]).value);
      assertTrue(result[1] instanceof LiteralPart);
      assertEquals("literal", ((LiteralPart) result[1]).getValue());
      assertTrue(result[2] instanceof TestVariablePart);
      assertEquals("two", ((TestVariablePart) result[2]).value);
   }

   @Test
   public void testParseOneVariableTwoLiterals() throws ParseException {
      ITemplatePart[] result = TestParser.parse("literal{one}literal", false);
      assertEquals(3, result.length);
      assertTrue(result[0] instanceof LiteralPart);
      assertEquals("literal", ((LiteralPart) result[0]).getValue());
      assertTrue(result[1] instanceof TestVariablePart);
      assertEquals("one", ((TestVariablePart) result[1]).value);
      assertTrue(result[2] instanceof LiteralPart);
      assertEquals("literal", ((LiteralPart) result[2]).getValue());

   }

   @Test
   public void testParseVariableError() throws ParseException {
      thrown.expect(ParseException.class);
      thrown.expectMessage("error");
      TestParser.parse("literal{error}", false);
   }

   @Test
   public void testParseNoLiteralBetweenVariables() throws ParseException {
      thrown.expect(ParseException.class);
      thrown.expectMessage("A literal must separate two variables");
      TestParser.parse("{one}{two}", true);
   }

   @Test
   public void testParseNoLiteralBetweenVariablesAllowed() throws ParseException {
      ITemplatePart[] result = TestParser.parse("{one}{two}", false);
      assertEquals(2, result.length);
      assertTrue(result[0] instanceof TestVariablePart);
      assertEquals("one", ((TestVariablePart) result[0]).value);
      assertTrue(result[1] instanceof TestVariablePart);
      assertEquals("two", ((TestVariablePart) result[1]).value);
   }

   @Test
   public void testParseUnfinishedVariable() throws ParseException {
      thrown.expect(ParseException.class);
      thrown.expectMessage("Unexpected end of the template");
      TestParser.parse("{one", false);
   }

   @Test
   public void testParseUnopennedVariable() throws ParseException {
      ITemplatePart[] result = TestParser.parse("one}", false);
      assertEquals(1, result.length);
      assertTrue(result[0] instanceof LiteralPart);
      assertEquals("one}", ((LiteralPart) result[0]).getValue());
   }

   @Test
   public void testParseEscapedVariable() throws ParseException {
      ITemplatePart[] result = TestParser.parse("\\{literal}", false);
      assertEquals(1, result.length);
      assertTrue(result[0] instanceof LiteralPart);
      assertEquals("{literal}", ((LiteralPart) result[0]).getValue());
   }

   @Test
   public void testParseNestedBlocks() throws ParseException {
      ITemplatePart[] result = TestParser.parse("{one{2}}", false);
      assertEquals(1, result.length);
      assertTrue(result[0] instanceof TestVariablePart);
      assertEquals("one{2}", ((TestVariablePart) result[0]).value);
   }

   public static class TestParser extends AbstractParser<TestVariablePart> {

      public static ITemplatePart[] parse(String template, boolean force) throws ParseException {
         return new TestParser(template, force).parse().toArray(new ITemplatePart[0]);
      }

      private TestParser(String template, boolean force) {
         super(template, force);
      }

      @Override
      protected TestVariablePart parseVariable(int pos, String variablePart) throws ParseException {
         if ("error".equals(variablePart)) {
            throw new ParseException(variablePart, pos);
         }
         return new TestVariablePart(variablePart);
      }
   }

   public static class TestVariablePart implements IVariablePart {

      public final String value;

      public TestVariablePart(String value) {
         this.value = value;
      }
   }
}
