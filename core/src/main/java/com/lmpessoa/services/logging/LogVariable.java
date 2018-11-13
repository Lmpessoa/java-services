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
package com.lmpessoa.services.logging;

import java.util.function.Function;

import com.lmpessoa.util.parsing.IVariablePart;

class LogVariable implements IVariablePart {

   private final String name;
   private final boolean rightAlign;
   protected final int length;
   private final Function<LogEntry, String> func;

   protected LogVariable(String name, boolean rightAlign, int length, Function<LogEntry, String> func) {
      this.name = name;
      this.rightAlign = rightAlign;
      this.length = length;
      this.func = func;
   }

   protected final String format(Object value) {
      String result = value.toString();
      if (length > 0) {
         if (result.length() > length) {
            if (rightAlign) {
               result = result.substring(result.length() - length);
            } else {
               result = result.substring(0, length);
            }
         } else {
            while (result.length() < length) {
               if (rightAlign) {
                  result = " " + result;
               } else {
                  result += " ";
               }
            }
         }
      }
      return result;
   }

   public String getValueOf(LogEntry entry) {
      return format(func.apply(entry));
   }

   @Override
   public final String toString() {
      StringBuilder result = new StringBuilder();
      result.append('{');
      result.append(name);
      if (length != -1) {
         result.append('(');
         result.append(rightAlign ? '>' : '<');
         result.append(length);
         result.append(')');
      }
      result.append('}');
      return result.toString();
   }
}
