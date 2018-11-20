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
package com.lmpessoa.services.test.validating;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.groups.Default;

@AvailableInStore(groups = { Availability.class })
public class Book {

   @NotEmpty(groups = { FirstLevelCheck.class, Default.class })
   private String title;

   @Valid
   @NotEmpty
   private final List<Author> authors = new ArrayList<>();

   @Valid
   private final Map<String, Review> reviewsPerSource = new HashMap<>();

   @Valid
   private Review pickedReview;

   private final Set<@NotBlank String> tags = new HashSet<>();

   private final Map<Integer, Set<@NotBlank String>> tagsByChapter = new HashMap<>();

   private final Set<@Valid Category> categories = new HashSet<>();

   private final Map<Integer, Set<@Valid Author>> authorsByChapter = new HashMap<>();

   public Book(String title, Author... authors) {
      this.authors.addAll(Arrays.asList(authors));
      this.title = title;
   }

   public Book() {
      // Does nothing
   }

   public void addAuthor(Author author) {
      this.authors.add(author);
   }

   public void addReview(String source, Review review) {
      this.reviewsPerSource.put(source, review);
   }

   public void addTag(String tag) {
      this.tags.add(tag);
   }

   public void addTags(String... tags) {
      this.tags.addAll(Arrays.asList(tags));
   }

   public void addTagToChapter(int chapter, String tag) {
      if (!this.tagsByChapter.containsKey(chapter)) {
         this.tagsByChapter.put(chapter, new HashSet<>());
      }
      this.tagsByChapter.get(chapter).add(tag);
   }

   public void addCategory(Category category) {
      this.categories.add(category);
   }

   public void addAuthorToChapter(int chapter, Author author) {
      if (!this.authorsByChapter.containsKey(chapter)) {
         this.authorsByChapter.put(chapter, new LinkedHashSet<>());
      }
      this.authorsByChapter.get(chapter).add(author);
   }

   public String getTitle() {
      return title;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   public Map<String, Review> getReviews() {
      return reviewsPerSource;
   }

   public Review getPickedReview() {
      return pickedReview;
   }

   public void setPickedReview(Review review) {
      this.pickedReview = review;
   }
}
