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
package com.lmpessoa.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;

import org.junit.Test;

public class LocalizationTest {

   private Localized.MessageSet source;
   private Localized.Message message;

   static {
      Locale.setDefault(Locale.ROOT);
   }

   @Test
   public void testBaseLanguage() {
      source = Localized.messagesAt("messages/ValidationMessages.properties");
      message = source.get("javax.validation.constraints.AssertFalse.message");
      assertEquals("must be false", message.get());

   }

   @Test
   public void testOneMoreLanguage() {
      source = Localized.messagesAt("messages/ValidationMessages.properties", Locale.GERMAN);
      message = source.get("javax.validation.constraints.AssertFalse.message");
      assertEquals("muss falsch sein", message.get());
   }

   @Test
   public void testTwoLanguagesFirstMissing() {
      source = Localized.messagesAt("messages/ValidationMessages.properties",
               Locale.forLanguageTag("zz"), Locale.forLanguageTag("zz"),
               Locale.forLanguageTag("nl"));
      message = source.get("javax.validation.constraints.AssertFalse.message");
      assertEquals("moet onwaar zijn", message.get());
      message = source.get("javax.validation.constraints.AssertTrue.message");
      assertEquals("zzzzzzzzzz", message.get());
   }

   @Test
   public void testLanguageMissing() {
      source = Localized.messagesAt("messages/ValidationMessages.properties",
               Locale.forLanguageTag("zn"));
      message = source.get("javax.validation.constraints.AssertFalse.message");
      assertEquals("must be false", message.get());
   }

   @Test
   public void testFormattedMessage() {
      source = Localized.messagesAt("messages/ExceptionMessages.properties", Locale.getDefault());
      message = source.get("com.lmpessoa.services.hosting.Headers.illegalname");
      assertEquals("Illegal HTTP header name: Sample", message.with("Sample"));
   }

   @Test
   public void testDefaultContents() {
      String text = Localized.resourceContentsAt("/localization/sample.txt");
      assertEquals("This is a simple example text", text);
   }

   @Test
   public void testMissingLanguageContents() {
      String text = Localized.resourceContentsAt("/localization/sample.txt", Locale.FRENCH,
               Locale.CHINESE);
      assertEquals("This is a simple example text", text);
   }

   @Test
   public void testLanguageContents() {
      String text = Localized.resourceContentsAt("/localization/sample.txt", Locale.FRENCH,
               Locale.forLanguageTag("nl"));
      assertEquals("Dit is een eenvoudig voorbeeld tekst", text);
   }

   @Test
   public void testMissingContents() {
      assertNull(Localized.resourceContentsAt("example.txt", Locale.GERMAN));
   }
}
