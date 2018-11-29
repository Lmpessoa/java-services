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
package com.lmpessoa.services.internal.validating;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import com.lmpessoa.services.internal.hosting.Localization;
import com.lmpessoa.services.test.validating.Author;
import com.lmpessoa.services.test.validating.Book;
import com.lmpessoa.services.test.validating.Category;
import com.lmpessoa.services.test.validating.Library;
import com.lmpessoa.services.validating.ErrorSet;
import com.lmpessoa.services.validating.ErrorSet.Entry;
import com.lmpessoa.services.validating.IValidationService;

public abstract class AbstractValidationServiceTest {

   private final IValidationService validator;

   public AbstractValidationServiceTest(IValidationService validator) {
      this.validator = validator;
   }

   @Test
   public void testAuthorSecurityCheck() {
      Author author = new Author("John", "Constantine");
      author.setCompany("Marvel");
      ErrorSet violations = validator.validate(author);
      assertTrue(violations.isEmpty());
   }

   @Test
   public void testAuthorSecurityCheckFail() {
      Author author = new Author(null, "Constantine");
      author.setCompany("Marvel");
      ErrorSet result = validator.validate(author);
      assertEquals(1, result.size());

      Iterator<Entry> iterator = result.iterator();
      Entry error = iterator.next();
      assertEquals("{com.lmpessoa.services.test.validating.SecurityChecking.message}",
               error.getMessageTemplate());
      assertEquals("firstName", error.getPathEntry());
      assertEquals("false", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testLibraryAddBook() throws NoSuchMethodException {
      Method method = Library.class.getMethod("addBook", Book.class);
      Library library = new Library();
      Book book = new Book();
      book.setTitle("Constantine");
      book.addAuthor(new Author("John", "Constantine"));
      ErrorSet violations = validator.validateParameters(library, method, new Object[] { book });
      assertTrue(violations.isEmpty());
   }

   @Test
   public void testLibraryAddNullBook() throws NoSuchMethodException {
      Method method = Library.class.getMethod("addBook", Book.class);
      Library library = new Library();
      ErrorSet violations = validator.validateParameters(library, method, new Object[] { null });
      assertEquals(1, violations.size());

      Iterator<Entry> iterator = violations.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.NotNull.message}", error.getMessageTemplate());
      assertEquals("book", error.getPathEntry());
      assertEquals("null", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testLibraryAddBookWithoutTitle() throws NoSuchMethodException {
      Method method = Library.class.getMethod("addBook", Book.class);
      Library library = new Library();
      Book book = new Book();
      book.addAuthor(new Author("John", "Constantine"));
      ErrorSet violations = validator.validateParameters(library, method, new Object[] { book });
      assertEquals(1, violations.size());

      Iterator<Entry> iterator = violations.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.NotEmpty.message}", error.getMessageTemplate());
      assertEquals("book.title", error.getPathEntry());
      assertEquals("null", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testLibraryAddBookInvalidAuthorInChapter() throws NoSuchMethodException {
      Method method = Library.class.getMethod("addBook", Book.class);
      Library library = new Library();
      Book book = new Book();
      book.setTitle("Constantine");
      book.addAuthor(new Author("John", "Constantine"));
      book.addAuthorToChapter(3, new Author("James", "Kirk"));
      Author picard = new Author("Jean-Luc", "Picard");
      picard.setCompany("United Federation of Planets");
      book.addAuthorToChapter(3, picard);
      ErrorSet violations = validator.validateParameters(library, method, new Object[] { book });
      assertEquals(1, violations.size());

      Iterator<Entry> iterator = violations.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.Size.message}", error.getMessageTemplate());
      assertEquals("book.authorsByChapter[3].<map value>[].company", error.getPathEntry());
      assertEquals(picard.getCompany(), error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testRenewPassword() throws NoSuchMethodException {
      Method method = Author.class.getMethod("renewPassword", String.class, String.class,
               String.class);
      Author author = new Author("James", "Kirk");
      ErrorSet violations = validator.validateParameters(author, method,
               new Object[] { "old", "new-password", "new-password" });
      assertTrue(violations.isEmpty());
   }

   @Test
   public void testRenewPasswordOldAndNewEquals() throws NoSuchMethodException {
      Method method = Author.class.getMethod("renewPassword", String.class, String.class,
               String.class);
      Author author = new Author("James", "Kirk");
      Object[] paramValues = new Object[] { "old-password", "old-password", "old-password" };
      ErrorSet violations = validator.validateParameters(author, method, paramValues);
      assertEquals(1, violations.size());

      Iterator<Entry> iterator = violations.iterator();
      Entry error = iterator.next();
      assertEquals("{com.lmpessoa.services.test.validating.OldAndNewPasswordsDifferent.message}",
               error.getMessageTemplate());
      assertEquals("<cross-parameter>", error.getPathEntry());
      assertEquals("{old-password,old-password,old-password}", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testRenewPasswordNewsNotEquals() throws NoSuchMethodException {
      Method method = Author.class.getMethod("renewPassword", String.class, String.class,
               String.class);
      Author author = new Author("James", "Kirk");
      Object[] paramValues = new Object[] { "old", "password-1234", "password-123" };
      ErrorSet violations = validator.validateParameters(author, method, paramValues);
      assertEquals(1, violations.size());

      Iterator<Entry> iterator = violations.iterator();
      Entry error = iterator.next();
      assertEquals("{com.lmpessoa.services.test.validating.ForceStrongPassword.different}",
               error.getMessageTemplate());
      assertEquals("retypedNewPassword", error.getPathEntry());
      assertEquals("password-123", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testRenewPasswordNewTooShort() throws NoSuchMethodException {
      Method method = Author.class.getMethod("renewPassword", String.class, String.class,
               String.class);
      Author author = new Author("James", "Kirk");
      Object[] paramValues = new Object[] { "old", "new1234", "new1234" };
      ErrorSet violations = validator.validateParameters(author, method, paramValues);
      assertEquals(1, violations.size());

      Iterator<Entry> iterator = violations.iterator();
      Entry error = iterator.next();
      assertEquals("{com.lmpessoa.services.test.validating.ForceStrongPassword.too_short}",
               error.getMessageTemplate());
      assertEquals("newPassword", error.getPathEntry());
      assertEquals("new1234", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testLibraryPopularBooks() throws NoSuchMethodException {
      Method method = Library.class.getMethod("getMostPopularBookPerAuthor");
      Library library = new Library();
      Map<Author, Book> returnValue = new HashMap<>();

      Author saramago = new Author("José", "Saramago");
      Book blindness = new Book("Blindness", saramago);
      returnValue.put(saramago, blindness);

      Author laurie = new Author("Hugh", "Laurie");
      Book gunseller = new Book("The Gunseller", laurie);
      returnValue.put(laurie, gunseller);

      ErrorSet violations = validator.validateReturnValue(library, method, returnValue);
      assertTrue(violations.isEmpty());
   }

   @Test
   public void testLibraryPopularBooksInvalidAuthor() throws NoSuchMethodException {
      Method method = Library.class.getMethod("getMostPopularBookPerAuthor");
      Library library = new Library();
      Map<Author, Book> returnValue = new HashMap<>();

      Author saramago = new Author("José", "Saramago");
      Book blindness = new Book("Blindness", saramago);
      returnValue.put(saramago, blindness);

      Author laurie = new Author("Hugh", "");
      Book gunseller = new Book("The Gunseller", laurie);
      returnValue.put(laurie, gunseller);

      ErrorSet violations = validator.validateReturnValue(library, method, returnValue);
      assertEquals(1, violations.size());

      Iterator<Entry> iterator = violations.iterator();
      Entry error = iterator.next();
      assertEquals("lastname must not be null", error.getMessageTemplate());
      assertEquals("<return value>[\", Hugh\"].authors[0].lastName", error.getPathEntry());
      assertEquals("", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testLibraryStubInvalidAuthor() throws NoSuchMethodException {
      Method method = Library.class.getMethod("stub");
      Library library = new Library();
      Map<Author, Book> returnValue = new HashMap<>();

      Author saramago = new Author("José", "Saramago");
      Book blindness = new Book("Blindness", saramago);
      returnValue.put(saramago, blindness);

      Author laurie = new Author("Hugh", "");
      Book gunseller = new Book("The Gunseller", laurie);
      returnValue.put(laurie, gunseller);

      ErrorSet violations = validator.validateReturnValue(library, method, returnValue);
      assertEquals(1, violations.size());

      Iterator<Entry> iterator = violations.iterator();
      Entry error = iterator.next();
      assertEquals("lastname must not be null", error.getMessageTemplate());
      assertEquals("<return value><K>[\", Hugh\"].lastName", error.getPathEntry());
      assertEquals("", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testLibraryPopularBooksInvalidBook() throws NoSuchMethodException {
      Method method = Library.class.getMethod("getMostPopularBookPerAuthor");
      Library library = new Library();
      Map<Author, Book> returnValue = new HashMap<>();

      Author saramago = new Author("José", "Saramago");
      Book blindness = new Book("Blindness", saramago);
      returnValue.put(saramago, blindness);

      Author laurie = new Author("Hugh", "Laurie");
      Book gunseller = new Book("", laurie);
      returnValue.put(laurie, gunseller);

      ErrorSet violations = validator.validateReturnValue(library, method, returnValue);
      assertEquals(1, violations.size());

      Iterator<Entry> iterator = violations.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.NotEmpty.message}", error.getMessageTemplate());
      assertEquals("<return value>[\"Laurie, Hugh\"].title", error.getPathEntry());
      assertEquals("", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testLibraryValid() {
      Library library = new Library();
      Author saramago = new Author("José", "Saramago");
      Book blindness = new Book("Blindness", saramago);
      library.addBook(blindness);

      Author laurie = new Author("Hugh", "Laurie");
      Book gunseller = new Book("", laurie);
      library.addBook(gunseller);

      ErrorSet violations = validator.validate(library);
      assertEquals(1, violations.size());

      Iterator<Entry> iterator = violations.iterator();
      Entry error = iterator.next();
      assertEquals("{javax.validation.constraints.NotEmpty.message}", error.getMessageTemplate());
      assertEquals("books[].title", error.getPathEntry());
      assertEquals("", error.getInvalidValue());
      assertFalse(iterator.hasNext());
   }

   @Test
   public void testLocalizedMessageInterpolation() {
      try {
         Localization.setLocales(new Locale[] { Locale.forLanguageTag("de-CH"), Locale.GERMAN });
         Category cat = new Category("if");

         ErrorSet violations = validator.validate(cat);
         assertEquals(1, violations.size());

         Iterator<Entry> iterator = violations.iterator();
         Entry error = iterator.next();
         assertEquals("{javax.validation.constraints.Size.message}", error.getMessageTemplate());
         assertEquals("muss zwischen 3 und 2147483647 liegen", error.getMessage());
         assertEquals("name", error.getPathEntry());
         assertEquals("if", error.getInvalidValue());
         assertFalse(iterator.hasNext());
      } finally {
         Localization.setLocales(new Locale[0]);
      }
   }
}
