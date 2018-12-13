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
package com.lmpessoa.services.views;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lmpessoa.services.views.templating.RenderizationContext;
import com.lmpessoa.services.views.templating.Template;

public class TestTemplate implements Template {

   private final String template;

   public TestTemplate(String template) {
      this.template = template;
   }

   @Override
   public String render(RenderizationContext context) {
      StringBuilder result = new StringBuilder();

      int pos = 0;
      Matcher m = Pattern.compile("\\{([a-zA-Z0-9._]*)\\}").matcher(template);
      while (m.find()) {
         result.append(template.substring(pos, m.start()));
         Object value = context.get(m.group(1));
         result.append(value == null ? "" : value);
         pos = m.end();
      }
      if (pos < template.length()) {
         result.append(template.substring(pos));
      }
      return result.toString();
   }
}
