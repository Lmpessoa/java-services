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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.views.templating.RenderizationContext;
import com.lmpessoa.services.views.templating.TemplateParseException;
import com.lmpessoa.services.views.templating.TemplateParseException.ParseError;

public class LiquidFilterTest {

   private RenderizationContext context;
   private LiquidEngine engine;

   @Before
   public void setup() {
      context = new RenderizationContext();
      engine = new LiquidEngine();
   }

   @Test
   public void testUnknownFilterTemplate() throws TemplateParseException {
      try {
         render("{{ 'test' | custom }}");
         fail();
      } catch (TemplateParseException e) {
         ParseError[] errors = e.getErrors();
         assertEquals(1, errors.length);
         ParseError error = errors[0];
         assertEquals(12, error.position());
         assertEquals(6, error.length());
         assertEquals("A filter with this name is not known", error.message());
      }
   }

   @Test
   public void testCustomFilterTemplate() throws TemplateParseException {
      engine.registerFilter("custom", (v, a) -> "Custom(" + v.toString() + ")");
      assertEquals("Custom(test)", render("{{ 'test' | custom }}"));
   }

   @Test
   public void testFiltersSplitJoin() throws TemplateParseException {
      assertEquals("one - two - three", render("{{ 'one,two,three' | split: ',' | join: ' - ' }}"));
   }

   @Test
   public void testFilterAbs() throws TemplateParseException {
      assertEquals("10.9", render("{{ -10.9 | abs }}"));
      //
      assertEquals("7", render("{{ 7 | abs }}"));
      //
      assertEquals("9", render("{{ '-9' | abs }}"));
   }

   @Test
   public void testFilterAppend() throws TemplateParseException {
      assertEquals("/my/fancy/url.html", render("{{ \"/my/fancy/url\" | append: \".html\" }}"));
      //
      assertEquals("website.com/index.html", render(
               "{% assign filename = \"/index.html\" %}{{ \"website.com\" | append: filename }}"));
   }

   @Test
   public void testFilterAtLeast() throws TemplateParseException {
      assertEquals("5", render("{{ 4 | at_least: 5 }}"));
      //
      assertEquals("4", render("{{ 4 | at_least: 3 }}"));
   }

   @Test
   public void testFilterAtMost() throws TemplateParseException {
      assertEquals("4", render("{{ 4 | at_most: 5 }}"));
      //
      assertEquals("3", render("{{ 4 | at_most: 3 }}"));
   }

   @Test
   public void testFilterCapitalize() throws TemplateParseException {
      assertEquals("My great title", render("{{ \"my great title\" | capitalize }}"));
   }

   @Test
   public void testFilterCeil() throws TemplateParseException {
      assertEquals("2", render("{{ 1.2 | ceil }}"));
      //
      assertEquals("2", render("{{ 2.0 | ceil }}"));
      //
      assertEquals("4", render("{{ '3.5' | ceil }}"));
   }

   @Test
   public void testFilterCompact() throws TemplateParseException {
      context.set("a", new String[] { "business", "celebrities", null, "lifestyle", "sports", null,
               "technology" });
      assertEquals("business, celebrities, lifestyle, sports, technology",
               render("{{ a | compact | join: ', ' }}"));
   }

   @Test
   public void testFilterConcat() throws TemplateParseException {
      assertEquals("apples,oranges,peaches,carrots,turnips,potatoes",
               render("{% assign fruits = \"apples, oranges, peaches\" | split: \", \" %}"
                        + "{% assign vegetables = \"carrots, turnips, potatoes\" | split: \", \" %}"
                        + "{{ fruits | concat: vegetables | join: ',' }}"));
   }

   @Test
   public void testFilterDefault() throws TemplateParseException {
      final String TEMPLATE = "{{ ammount | default: 1 }}";
      assertEquals("1", render(TEMPLATE));
      //
      context.set("ammount", 7);
      assertEquals("7", render(TEMPLATE));
      //
      context.set("ammount", "");
      assertEquals("1", render(TEMPLATE));
   }

   @Test
   public void testFilterDividedBy() throws TemplateParseException {
      final String TEMPLATE = "{{ a | divided_by: b }}";
      context.set("a", 16);
      context.set("b", 4);
      assertEquals("4", render(TEMPLATE));
      //
      context.set("b", 4.0);
      assertEquals("4.0", render(TEMPLATE));
      //
      context.set("a", 20);
      context.set("b", 7);
      assertEquals("2", render(TEMPLATE));
      //
      context.set("b", 7.0);
      assertEquals("2.857142857142857", render(TEMPLATE));
      //
      context.set("a", 5);
      context.set("b", 4.0);
      assertEquals("1.25", render(TEMPLATE));
   }

   @Test
   public void testFilterDowncase() throws TemplateParseException {
      assertEquals("parker moore", render("{{ \"Parker Moore\" | downcase }}"));
   }

   @Test
   public void testFilterEscape() throws TemplateParseException {
      assertEquals("Have you read &#39;James &amp; the Giant Peach&#39;?",
               render("{{ \"Have you read 'James & the Giant Peach'?\" | escape }}"));
   }

   @Test
   public void testFilterEscapeOnce() throws TemplateParseException {
      assertEquals("1 &lt; 2 &amp; 3", render("{{ \"1 < 2 & 3\" | escape_once }}"));
      //
      assertEquals("1 &lt; 2 &amp; 3", render("{{ \"1 &lt; 2 &amp; 3\" | escape_once }}"));
      //
      assertEquals("1 &amp;lt; 2 &amp;amp; 3", render("{{ \"1 &lt; 2 &amp; 3\" | escape }}"));
   }

   @Test
   public void testFilterFirst() throws TemplateParseException {
      assertEquals("apples",
               render("{% assign my_array = \"apples, oranges, peaches, plums\" | split: \", \" %}"
                        + "{{ my_array | first }}"));
      //
      assertEquals("apples",
               render("{% assign my_array = \"apples, oranges, peaches, plums\" | split: \", \" %}"
                        + "{{ my_array.first }}"));
   }

   @Test
   public void testFilterFloor() throws TemplateParseException {
      assertEquals("1", render("{{ 1.2 | floor }}"));
      //
      assertEquals("2", render("{{ 2.0 | floor }}"));
      //
      assertEquals("3", render("{{ '3.5' | floor }}"));
   }

   @Test
   public void testFilterLast() throws TemplateParseException {
      assertEquals("plums", render(
               "{% assign my_array = \"apples, oranges, peaches, plums\" | split: \", \" %}{{ my_array | last }}"));
      //
      assertEquals("plums", render(
               "{% assign my_array = \"apples, oranges, peaches, plums\" | split: \", \" %}{{ my_array.last }}"));
   }

   @Test
   public void testFilterLstrip() throws TemplateParseException {
      assertEquals("So much room for activities!          ",
               render("{{ \"          So much room for activities!          \" | lstrip }}"));
   }

   @Test
   public void testFilterRstrip() throws TemplateParseException {
      assertEquals("          So much room for activities!",
               render("{{ \"          So much room for activities!          \" | rstrip }}"));
   }

   @Test
   public void testFilterStrip() throws TemplateParseException {
      assertEquals("So much room for activities!",
               render("{{ \"          So much room for activities!          \" | strip }}"));
   }

   @Test
   public void testFilterMinus() throws TemplateParseException {
      assertEquals("2", render("{{ 4 | minus: 2 }}"));
      //
      assertEquals("12", render("{{ 16 | minus: 4 }}"));
      //
      assertEquals("171.357", render("{{ 183.357 | minus: 12 }}"));
   }

   @Test
   public void testFilterModulo() throws TemplateParseException {
      assertEquals("1", render("{{ 3 | modulo: 2 }}"));
      //
      assertEquals("3", render("{{ 24 | modulo: 7 }}"));
      //
      assertEquals("3.357", render("{{ 183.357 | modulo: 12 }}"));
   }

   @Test
   public void testFilterNewlineToBr() throws TemplateParseException {
      assertEquals("<br/>Hello<br/>there<br/>",
               render("{% capture string_with_newlines %}\nHello\nthere\n"
                        + "{% endcapture %}{{ string_with_newlines | newline_to_br }}"));
   }

   @Test
   public void testFilterPlus() throws TemplateParseException {
      assertEquals("5", render("{{ 3 | plus: 2 }}"));
      //
      assertEquals("31", render("{{ 24 | plus: 7 }}"));
      //
      assertEquals("195.357", render("{{ 183.357 | plus: 12 }}"));
   }

   @Test
   public void testFilterPrepend() throws TemplateParseException {
      assertEquals("Some fruit: apples, oranges, and bananas",
               render("{{ \"apples, oranges, and bananas\" | prepend: \"Some fruit: \" }}"));
      //
      assertEquals("liquidmarkup.com/index.html", render(
               "{% assign url = \"liquidmarkup.com\" %}{{ \"/index.html\" | prepend: url }}"));
   }

   @Test
   public void testFilterRemove() throws TemplateParseException {
      assertEquals("I sted to see the t through the ",
               render("{{ \"I strained to see the train through the rain\" | remove: \"rain\" }}"));
   }

   @Test
   public void testFilterRemoveFirst() throws TemplateParseException {
      assertEquals("I sted to see the train through the rain", render(
               "{{ \"I strained to see the train through the rain\" | remove_first: \"rain\" }}"));
   }

   @Test
   public void testFilterReplace() throws TemplateParseException {
      assertEquals("Take your protein pills and put your helmet on", render(
               "{{ \"Take my protein pills and put my helmet on\" | replace: \"my\", \"your\" }}"));
   }

   @Test
   public void testFilterReplaceFirst() throws TemplateParseException {
      assertEquals("Take your protein pills and put my helmet on", render(
               "{{ \"Take my protein pills and put my helmet on\" | replace_first: \"my\", \"your\" }}"));
   }

   @Test
   public void testFilterReverse() throws TemplateParseException {
      assertEquals("plums, peaches, oranges, apples", render(
               "{% assign a = \"apples, oranges, peaches, plums\" | split: \", \" %}{{ a | reverse | join: \", \" }}"));
      //
      assertEquals(".moT rojaM ot lortnoc dnuorG", render(
               "{{ \"Ground control to Major Tom.\" | split: \"\" | reverse | join: \"\" }}"));
   }

   @Test
   public void testFilterRound() throws TemplateParseException {
      assertEquals("1", render("{{ 1.2 | round }}"));
      //
      assertEquals("3", render("{{ '2.7' | round }}"));
      //
      assertEquals("183.36", render("{{ 183.357 | round: 2 }}"));
   }

   @Test
   public void testFilterSize() throws TemplateParseException {
      assertEquals("28", render("{{ \"Ground control to Major Tom.\" | size }}"));
      //
      assertEquals("4", render(
               "{% assign a = \"apples, oranges, peaches, plums\" | split: \", \" %}{{ a.size }}"));
   }

   @Test
   public void testFilterSlice() throws TemplateParseException {
      assertEquals("L", render("{{ 'Liquid' | slice: 0 }}"));
      //
      assertEquals("q", render("{{ 'Liquid' | slice: 2 }}"));
      //
      assertEquals("quid", render("{{ 'Liquid' | slice: 2, 5 }}"));
      //
      assertEquals("ui", render("{{ 'Liquid' | slice: -3, 2 }}"));
   }

   @Test
   public void testFilterSort() throws TemplateParseException {
      assertEquals("Snake, giraffe, octopus, zebra", render(
               "{% assign a = \"zebra, octopus, giraffe, Snake\" | split: \", \" %}{{ a | sort | join: \", \" }}"));
   }

   @Test
   public void testFilterSortNatural() throws TemplateParseException {
      assertEquals("giraffe, octopus, Snake, zebra", render(
               "{% assign a = \"zebra, octopus, giraffe, Snake\" | split: \", \" %}{{ a | sort_natural | join: \", \" }}"));
   }

   @Test
   public void testFilterStripHtml() throws TemplateParseException {
      assertEquals("Have you read Ulysses?",
               render("{{ \"Have <em>you</em> read <strong>Ulysses</strong>?\" | strip_html }}"));
   }

   @Test
   public void testFilterStripNewlines() throws TemplateParseException {
      assertEquals("Hellothere", render("{% capture string_with_newlines %}\nHello\nthere\n"
               + "{% endcapture %}{{ string_with_newlines | strip_newlines }}"));
   }

   @Test
   public void testFilterTimes() throws TemplateParseException {
      assertEquals("6", render("{{ 3 | times: 2 }}"));
      //
      assertEquals("168", render("{{ 24 | times: 7 }}"));
      //
      assertEquals("2200.284", render("{{ 183.357 | times: 12 }}"));
   }

   @Test
   public void testFilterTruncate() throws TemplateParseException {
      assertEquals("Ground control to...",
               render("{{ \"Ground control to Major Tom.\" | truncate: 20 }}"));
      //
      assertEquals("Ground control, and so on",
               render("{{ \"Ground control to Major Tom.\" | truncate: 25, \", and so on\" }}"));
      //
      assertEquals("Ground control to Ma",
               render("{{ \"Ground control to Major Tom.\" | truncate: 20, \"\" }}"));
   }

   @Test
   public void testFilterTruncateWords() throws TemplateParseException {
      assertEquals("Ground control to...",
               render("{{ \"Ground control to Major Tom.\" | truncatewords: 3 }}"));
      //
      assertEquals("Ground control to--",
               render("{{ \"Ground control to Major Tom.\" | truncatewords: 3, \"--\" }}"));
      //
      assertEquals("Ground control to",
               render("{{ \"Ground control to Major Tom.\" | truncatewords: 3, \"\" }}"));
   }

   @Test
   public void testFilterUniq() throws TemplateParseException {
      assertEquals("ants, bugs, bees", render(
               "{% assign a = \"ants, bugs, bees, bugs, ants\" | split: \", \" %}{{ a | uniq | join: \", \" }}"));
   }

   @Test
   public void testFilterUpcase() throws TemplateParseException {
      assertEquals("PARKER MOORE", render("{{ \"Parker Moore\" | upcase }}"));
   }

   @Test
   public void testFilterUrlDecode() throws TemplateParseException {
      assertEquals("'Stop!' said Fred", render("{{ \"%27Stop%21%27+said+Fred\" | url_decode }}"));
   }

   @Test
   public void testFilterUrlEncode() throws TemplateParseException {
      assertEquals("%27Stop%21%27+said+Fred", render("{{ \"'Stop!' said Fred\" | url_encode }}"));
   }

   @Test
   public void testFilterMap() throws TemplateParseException {
      context.set("pages", Arrays.asList(new Page("business"), new Page("celebrities"),
               new Page("lifestyle"), new Page("sports"), new Page("technology")));

      assertEquals("business, celebrities, lifestyle, sports, technology",
               render("{{ pages | map: \"category\" | join: \", \" }}"));
   }

   @Test
   public void testFilterDate() throws TemplateParseException {
      context.set("published_at", LocalDate.of(2017, 6, 5));
      assertEquals("Mon, Jun 05, 17", render("{{ published_at | date: \"%a, %b %d, %y\" }}"));
      //
      assertEquals("2017", render("{{ published_at | date: \"%Y\" }}"));
      //
      String today = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now());
      assertEquals(today, render("{{ \"today\" | date: \"%Y-%m-%d\" }}"));
      //
      assertTrue(render("{{ \"now\" | date: \"%Y-%m-%d %H:%M\" }}").startsWith(today));
      //
      assertEquals("Mar 14, 16", render("{{ \"2016-03-14\" | date: \"%b %d, %y\" }}"));
   }

   private String render(String template) throws TemplateParseException {
      LiquidTemplate lt = engine.parse("file.lhtm", template);
      return lt.render(context);
   }

   public static final class Page {

      private final String category;

      public Page(String category) {
         this.category = category;
      }

      public String getCategory() {
         return category;
      }
   }
}
