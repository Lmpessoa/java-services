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
package com.lmpessoa.services.internal.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class AbstractParserTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void testParseOneVariableNoLiteral() {
      ITemplatePart[] result = TestParser.parse("{one}", false, null);
      assertEquals(1, result.length);
      assertTrue(result[0] instanceof TestVariablePart);
      assertEquals("one", ((TestVariablePart) result[0]).value);
   }

   @Test
   public void testParseLiteralNoVariables() {
      ITemplatePart[] result = TestParser.parse("literal", false, null);
      assertEquals(1, result.length);
      assertTrue(result[0] instanceof LiteralPart);
      assertEquals("literal", ((LiteralPart) result[0]).getValue());
   }

   @Test
   public void testParseTwoVariablesOneLiteral() {
      ITemplatePart[] result = TestParser.parse("{one}literal{two}", false, null);
      assertEquals(3, result.length);
      assertTrue(result[0] instanceof TestVariablePart);
      assertEquals("one", ((TestVariablePart) result[0]).value);
      assertTrue(result[1] instanceof LiteralPart);
      assertEquals("literal", ((LiteralPart) result[1]).getValue());
      assertTrue(result[2] instanceof TestVariablePart);
      assertEquals("two", ((TestVariablePart) result[2]).value);
   }

   @Test
   public void testParseOneVariableTwoLiterals() {
      ITemplatePart[] result = TestParser.parse("literal{one}literal", false, null);
      assertEquals(3, result.length);
      assertTrue(result[0] instanceof LiteralPart);
      assertEquals("literal", ((LiteralPart) result[0]).getValue());
      assertTrue(result[1] instanceof TestVariablePart);
      assertEquals("one", ((TestVariablePart) result[1]).value);
      assertTrue(result[2] instanceof LiteralPart);
      assertEquals("literal", ((LiteralPart) result[2]).getValue());

   }

   @Test
   public void testParseVariableError() {
      thrown.expect(ParseException.class);
      thrown.expectMessage("error");
      TestParser.parse("literal{error}", false, null);
   }

   @Test
   public void testParseNoLiteralBetweenVariables() {
      thrown.expect(ParseException.class);
      thrown.expectMessage("A literal must separate two variables");
      TestParser.parse("{one}{two}", true, null);
   }

   @Test
   public void testParseNoLiteralBetweenVariablesAllowed() {
      ITemplatePart[] result = TestParser.parse("{one}{two}", false, null);
      assertEquals(2, result.length);
      assertTrue(result[0] instanceof TestVariablePart);
      assertEquals("one", ((TestVariablePart) result[0]).value);
      assertTrue(result[1] instanceof TestVariablePart);
      assertEquals("two", ((TestVariablePart) result[1]).value);
   }

   @Test
   public void testParseUnfinishedVariable() {
      thrown.expect(ParseException.class);
      thrown.expectMessage("Unexpected end of the template");
      TestParser.parse("{one", false, null);
   }

   @Test
   public void testParseUnopennedVariable() {
      ITemplatePart[] result = TestParser.parse("one}", false, null);
      assertEquals(1, result.length);
      assertTrue(result[0] instanceof LiteralPart);
      assertEquals("one}", ((LiteralPart) result[0]).getValue());
   }

   @Test
   public void testParseEscapedVariable() {
      ITemplatePart[] result = TestParser.parse("\\{literal}", false, null);
      assertEquals(1, result.length);
      assertTrue(result[0] instanceof LiteralPart);
      assertEquals("{literal}", ((LiteralPart) result[0]).getValue());
   }

   @Test
   public void testParseNestedBlocks() {
      ITemplatePart[] result = TestParser.parse("{one{2}}", false, null);
      assertEquals(1, result.length);
      assertTrue(result[0] instanceof TestVariablePart);
      assertEquals("one{2}", ((TestVariablePart) result[0]).value);
   }

   @Test
   public void testParseWithVariablePrefix() {
      ITemplatePart[] result = TestParser.parse("${one}{two}", false, '$');
      assertEquals(2, result.length);
      assertTrue(result[0] instanceof TestVariablePart);
      assertTrue(result[1] instanceof LiteralPart);
      assertEquals("one", ((TestVariablePart) result[0]).value);
      assertEquals("{two}", ((LiteralPart) result[1]).getValue());
   }

   public static class TestParser extends AbstractParser<TestVariablePart> {

      public static ITemplatePart[] parse(String template, boolean force, Character prefix) {
         return new TestParser(template, force, prefix).parse().toArray(new ITemplatePart[0]);
      }

      private TestParser(String template, boolean force, Character prefix) {
         super(template, force, prefix);
      }

      @Override
      protected TestVariablePart parseVariable(int pos, String variablePart) {
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
