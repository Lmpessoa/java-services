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
package com.lmpessoa.services.views.liquid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.views.liquid.parsing.LiquidParseContext;
import com.lmpessoa.services.views.liquid.parsing.NumberLiteral;
import com.lmpessoa.services.views.liquid.parsing.TagElement;
import com.lmpessoa.services.views.liquid.parsing.TagElementBuilder;
import com.lmpessoa.services.views.templating.RenderizationContext;
import com.lmpessoa.services.views.templating.TemplateParseException;
import com.lmpessoa.services.views.templating.TemplateParseException.ParseError;

public class LiquidTemplateTest {

   private static final String FILENAME = "file.lhtm";
   private RenderizationContext context;
   private LiquidTemplate template;
   private LiquidEngine engine;
   private String result;

   @Before
   public void setup() {
      context = new RenderizationContext();
      engine = new LiquidEngine();
   }

   @Test
   public void testSimpleTemplate() throws TemplateParseException {
      template = engine.parse(FILENAME, "Hi {{ name }}!");
      context.set("name", "Lmpessoa");
      result = template.render(context);
      assertEquals("Hi Lmpessoa!", result);
   }

   @Test
   public void testMissingVariableTemplate() throws TemplateParseException {
      template = engine.parse(FILENAME, "Hi {{ name }}!");
      result = template.render(context);
      assertEquals("Hi !", result);
   }

   @Test
   public void testUnknownTagTemplate() {
      try {
         engine.parse(FILENAME, "{% random 7 %}");
         fail();
      } catch (TemplateParseException e) {
         ParseError[] errors = e.getErrors();
         assertEquals(1, errors.length);
         ParseError error = errors[0];
         assertEquals(3, error.position());
         assertEquals(6, error.length());
         assertEquals("This tag was not recognised", error.message());
      }
   }

   @Test
   public void testCustomTagTemplate() throws TemplateParseException {
      engine.registerTagBuilder("random", RandomTagElementBuilder.class);
      template = engine.parse(FILENAME, "{% random 7 %}");
      result = template.render(context);
      assertEquals("RAND(7)", result);
   }

   @Test
   public void testConditionalMissingTemplate() throws TemplateParseException {
      template = engine.parse(FILENAME, "{% if a %}Test{% endif %}");
      result = template.render(context);
      assertEquals("", result);
   }

   @Test
   public void testConditionalTemplate() throws TemplateParseException {
      context.set("a", true);
      template = engine.parse(FILENAME, "{% if a %}Test{% endif %}");
      result = template.render(context);
      assertEquals("Test", result);
   }

   @Test
   public void testConditionalExpression() throws TemplateParseException {
      context.set("user", new Object() {

         @SuppressWarnings("unused")
         public String getName() {
            return "Tobi";
         }
      });
      template = engine.parse(FILENAME, "{% if user.name == 'Tobi' %}Hello, Tobi!{% endif %}");
      result = template.render(context);
      assertEquals("Hello, Tobi!", result);
   }

   @Test
   public void testConditionalExpressionDiff() throws TemplateParseException {
      context.set("user", new Object() {

         @SuppressWarnings("unused")
         public String getName() {
            return "Bob";
         }
      });
      template = engine.parse(FILENAME, "{% if user.name == 'Tobi' %}Hello, Tobi!{% endif %}");
      result = template.render(context);
      assertEquals("", result);
   }

   @Test
   public void testIfElseTemplateFalse() throws TemplateParseException {
      context.set("a", false);
      template = engine.parse(FILENAME, "{% if a %}True{% else %}False{% endif %}");
      result = template.render(context);
      assertEquals("False", result);
   }

   @Test
   public void testIfElseTemplateTrue() throws TemplateParseException {
      context.set("a", true);
      template = engine.parse(FILENAME, "{% if a %}True{% else %}False{% endif %}");
      result = template.render(context);
      assertEquals("True", result);
   }

   @Test
   public void testSimpleAssign() throws TemplateParseException {
      assertNull(context.get("a"));
      template = engine.parse(FILENAME, "{% assign a = 'Test' %}");
      result = template.render(context);
      assertEquals("", result);
      assertEquals("Test", context.get("a"));
   }

   @Test
   public void testCaptureAssign() throws TemplateParseException {
      assertNull(context.get("designation"));
      context.set("serial", 1701);
      context.set("name", "Enterprise");
      template = engine.parse(FILENAME,
               "{% capture designation %}NCC {{ serial }}, USS {{ name }}{% endcapture %}");
      result = template.render(context);
      assertEquals("", result);
      assertEquals("NCC 1701, USS Enterprise", context.get("designation"));
   }

   @Test
   public void testSingleCycle() throws TemplateParseException {
      template = engine.parse(FILENAME, "{% cycle 'one', 'two', 'three' %}");
      result = template.render(context);
      assertEquals("one", result);
   }

   @Test
   public void testLoopCycles() throws TemplateParseException {
      template = engine.parse(FILENAME,
               "{% cycle 'one', 'two', 'three' %} {% cycle 'one', 'two', 'three' %} "
                        + "{% cycle 'one', 'two', 'three' %} {% cycle 'one', 'two', 'three' %}");
      result = template.render(context);
      assertEquals("one two three one", result);
   }

   @Test
   public void testNamedCycles() throws TemplateParseException {
      template = engine.parse(FILENAME,
               "{% cycle 'group1':'one', 'two', 'three' %} {% cycle 'group1':'one', 'two', 'three' %} "
                        + "{% cycle 'group2':'one', 'two', 'three' %} {% cycle 'group2':'one', 'two', 'three' %}");
      result = template.render(context);
      assertEquals("one two one two", result);
   }

   @Test
   public void testUnlessMissingVariable() throws TemplateParseException {
      assertNull(context.get("a"));
      template = engine.parse(FILENAME, "{% unless a %}Test{% endunless %}");
      result = template.render(context);
      assertEquals("Test", result);
   }

   @Test
   public void testUnlessSatisfied() throws TemplateParseException {
      context.set("a", true);
      template = engine.parse(FILENAME, "{% unless a %}Test{% endunless %}");
      result = template.render(context);
      assertEquals("", result);
   }

   @Test
   public void testSimpleCase1() throws TemplateParseException {
      context.set("a", 1);
      template = engine.parse(FILENAME, "{% case a %}{% when 1 %}One{% when 2 %}Two{% endcase %}");
      result = template.render(context);
      assertEquals("One", result);
   }

   @Test
   public void testSimpleCase2() throws TemplateParseException {
      context.set("a", 2);
      template = engine.parse(FILENAME, "{% case a %}{% when 1 %}One{% when 2 %}Two{% endcase %}");
      result = template.render(context);
      assertEquals("Two", result);
   }

   @Test
   public void testCaseWithMultipleValues2() throws TemplateParseException {
      context.set("a", 2);
      template = engine.parse(FILENAME,
               "{% case a %}{% when 1 %}One{% when 2 or 3 %}Two{% endcase %}");
      result = template.render(context);
      assertEquals("Two", result);
   }

   @Test
   public void testCaseWithMultipleValues3() throws TemplateParseException {
      context.set("a", 3);
      template = engine.parse(FILENAME,
               "{% case a %}{% when 1 %}One{% when 2 or 3 %}Two{% endcase %}");
      result = template.render(context);
      assertEquals("Two", result);
   }

   @Test
   public void testCaseWithElse2() throws TemplateParseException {
      context.set("a", 2);
      template = engine.parse(FILENAME,
               "{% case a %}{% when 1 %}One{% when 2 %}Two{% else %}More{% endcase %}");
      result = template.render(context);
      assertEquals("Two", result);
   }

   @Test
   public void testCaseWithElse4() throws TemplateParseException {
      context.set("a", 4);
      template = engine.parse(FILENAME,
               "{% case a %}{% when 1 %}One{% when 2 or 3 %}Two{% else %}More{% endcase %}");
      result = template.render(context);
      assertEquals("More", result);
   }

   @Test
   public void testSimpleForLoop() throws TemplateParseException {
      context.set("a", new String[] { "one", "two", "three" });
      template = engine.parse(FILENAME, "{% for i in a %}{{ i }} {% endfor %}");
      result = template.render(context);
      assertEquals("one two three ", result);
   }

   @Test
   public void testMapForLoop() throws TemplateParseException {
      Map<String, Integer> values = new LinkedHashMap<>();
      values.put("a", 1);
      values.put("b", 2);
      values.put("c", 3);
      context.set("map", values);
      template = engine.parse(FILENAME, "{% for e in map %}{{ e[0] }}-{{ e[1] }} {% endfor %}");
      result = template.render(context);
      assertEquals("a-1 b-2 c-3 ", result);
   }

   @Test
   public void testRangeForLoop() throws TemplateParseException {
      template = engine.parse(FILENAME, "{% for i in (1..5) %}{{ i }} {% endfor %}");
      result = template.render(context);
      assertEquals("1 2 3 4 5 ", result);
   }

   @Test
   public void testLoopElse1() throws TemplateParseException {
      context.set("a", new String[0]);
      template = engine.parse(FILENAME, "{% for i in a %}{{ i }}{% else %}Nothing{% endfor %}");
      result = template.render(context);
      assertEquals("Nothing", result);
   }

   @Test
   public void testLoopElse2() throws TemplateParseException {
      context.set("a", 1);
      template = engine.parse(FILENAME, "{% for i in a %}{{ i }}{% else %}Nothing{% endfor %}");
      result = template.render(context);
      assertEquals("Nothing", result);
   }

   @Test
   public void testLoopElse3() throws TemplateParseException {
      template = engine.parse(FILENAME, "{% for i in a %}{{ i }}{% else %}Nothing{% endfor %}");
      result = template.render(context);
      assertEquals("Nothing", result);
   }

   @Test
   public void testLoopBreak() throws TemplateParseException {
      template = engine.parse(FILENAME,
               "{% for i in (1..5) %}{{ i }}{% if i == 3 %}{% break %}{% endif %}{% endfor %}");
      result = template.render(context);
      assertEquals("123", result);
   }

   @Test
   public void testLoopContinue() throws TemplateParseException {
      template = engine.parse(FILENAME,
               "{% for i in (1..5) %}{% if i == 3 %}{% continue %}{% endif %}{{ i }}{% endfor %}");
      result = template.render(context);
      assertEquals("1245", result);
   }

   @Test
   public void testRawElement() throws TemplateParseException {
      context.set("var", "variable value");
      template = engine.parse(FILENAME, "{% raw %}The {{ var }} will not be rendered{% endraw %}");
      result = template.render(context);
      assertEquals("The {{ var }} will not be rendered", result);
   }

   @Test
   public void testCommentElement() throws TemplateParseException {
      context.set("var", "variable value");
      template = engine.parse(FILENAME,
               "{% comment %}The {{ var }} will not be rendered{% endcomment %}");
      result = template.render(context);
      assertEquals("", result);
   }

   public static class RandomTagElement extends TagElement {

      private final BigDecimal value;

      protected RandomTagElement(NumberLiteral value) {
         super("random");
         this.value = value.get();
      }

      @Override
      public String render(LiquidRenderizationContext context) {
         return String.format("RAND(%s)", value.toString());
      }
   }

   public static class RandomTagElementBuilder implements TagElementBuilder {

      @Override
      public TagElement build(String tagName, LiquidParseContext context) {
         return new RandomTagElement(context.readNumber(false));
      }
   }
}
