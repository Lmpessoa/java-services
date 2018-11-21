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
package com.lmpessoa.services.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;

import org.junit.Test;

import com.lmpessoa.services.util.Localization.Messages;

public class LocalizationTest {

   private Messages source;

   static {
      Locale.setDefault(Locale.forLanguageTag("en-gb"));
   }

   @Test
   public void testBaseLanguage() {
      source = Localization.messagesOf("/validation/ValidationMessages");
      String message = source.get("javax.validation.constraints.AssertFalse.message");
      assertEquals("must be false", message);
   }

   @Test
   public void testOneMoreLanguage() {
      source = Localization.messagesOf("/validation/ValidationMessages", Locale.GERMAN);
      String message = source.get("javax.validation.constraints.AssertFalse.message");
      assertEquals("muss falsch sein", message);
   }

   @Test
   public void testTwoLanguagesFirstMissing() {
      source = Localization.messagesOf("/validation/ValidationMessages",
               Locale.forLanguageTag("zz"), Locale.forLanguageTag("nl"));
      String message = source.get("javax.validation.constraints.AssertFalse.message");
      assertEquals("moet onwaar zijn", message);
      message = source.get("javax.validation.constraints.AssertTrue.message");
      assertEquals("zzzzzzzzzz", message);
   }

   @Test
   public void testLanguageMissing() {
      source = Localization.messagesOf("/validation/ValidationMessages",
               Locale.forLanguageTag("zn"));
      String message = source.get("javax.validation.constraints.AssertFalse.message");
      assertEquals("must be false", message);
   }

   @Test
   public void testDefaultContents() {
      String text = Localization.contentsOf("/localization/sample.txt");
      assertEquals("This is a simple example text", text);
   }

   @Test
   public void testMissingLanguageContents() {
      String text = Localization.contentsOf("/localization/sample.txt", Locale.FRENCH,
               Locale.CHINESE);
      assertEquals("This is a simple example text", text);
   }

   @Test
   public void testLanguageContents() {
      String text = Localization.contentsOf("/localization/sample.txt", Locale.FRENCH,
               Locale.forLanguageTag("nl"));
      assertEquals("Dit is een eenvoudig voorbeeld tekst", text);
   }

   @Test
   public void testMissingContents() {
      assertNull(Localization.contentsOf("example.txt", Locale.GERMAN));
   }
}
