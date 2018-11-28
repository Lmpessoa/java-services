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
package com.lmpessoa.services.views.templating;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public final class RenderizationContextTest {

   private RenderizationContext context;

   @Before
   public void setup() {
      context = new RenderizationContext();
   }

   @Test
   public void testSimpleVariable() {
      context.set("name", "Leeow");
      assertEquals("Leeow", context.get("name"));
   }

   @Test
   public void testPropertyInMap() {
      Map<String, String> map = new HashMap<>();
      map.put("name", "Leeow");
      context.set("site", map);
      assertEquals("Leeow", context.get("site.name"));
   }

   @Test
   public void testPropertyInObject() {
      context.set("site", new Object() {

         @SuppressWarnings("unused")
         public String getName() {
            return "Leeow";
         }
      });
      assertEquals("Leeow", context.get("site.name"));
   }

   @Test
   public void testValueInArray() {
      context.set("names", new String[] { "", "Leeow" });
      assertEquals("Leeow", context.get("names[1]"));
   }

   @Test
   public void testValueInCollection() {
      Collection<String> list = new HashSet<>();
      list.add("");
      list.add("Leeow");
      context.set("names", list);
      assertEquals("Leeow", context.get("names[1]"));
   }

   @Test
   public void testValueInList() {
      context.set("names", Arrays.asList("", "Leeow"));
      assertEquals("Leeow", context.get("names[1]"));
   }

   @Test
   public void testMissingValueInMap() {
      context.set("site", new HashMap<>());
      assertNull(context.get("site.name"));
   }

   @Test
   public void testMissingValueInArray() {
      context.set("names", new String[] { "", "Leeow" });
      assertNull(context.get("names[10]"));
   }

   @Test(expected = IllegalArgumentException.class)
   public void testIllegalNameInContext() {
      context.set("site.name", "Leeow");
   }

   @Test
   public void testValueInMap() {
      Map<String, String> map = new HashMap<>();
      map.put("name", "Leeow");
      context.set("site", map);
      assertEquals("Leeow", context.get("site['name']"));
   }

   @Test
   public void testNullInTheMiddle() {
      assertNull(context.get("site.name"));
   }

   @Test
   public void testValueInArryaWithInvalidIndex() {
      context.set("names", new String[] { "", "Leeow" });
      assertNull(context.get("names['name']"));
   }

   @Test
   public void testGetValueFromParentContext() {
      context.set("name", "Leeow");
      RenderizationContext newContext = new RenderizationContext(context);
      assertEquals("Leeow", newContext.get("name"));
   }

   @Test
   public void textSetValueInParentContext() {
      context.set("name", "Leeuw");
      RenderizationContext newContext = new RenderizationContext(context);
      newContext.set("name", "Leeow");
      assertEquals("Leeow", context.get("name"));
   }
}
