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
package com.lmpessoa.shrt.model;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class LinkInfo {

   private transient final Collection<Visitor> visitors;
   private transient final String creator;
   private final ZonedDateTime created;
   private final String link;
   private final String url;

   public LinkInfo(String link, String url, String creator, LocalDateTime created,
      Collection<Visitor> visitors) {
      this.visitors = Collections.unmodifiableCollection(visitors);
      this.created = created.atZone(ZoneId.systemDefault());
      this.creator = creator;
      this.link = link;
      this.url = url;
   }

   public String getLink() {
      return link;
   }

   public String getUrl() {
      return url;
   }

   public String getCreator() {
      return creator;
   }

   public ZonedDateTime getCreated() {
      return created;
   }

   public int getTotalVisitors() {
      return visitors.size();
   }

   public Map<String, Integer> getVisitorsPerCountry() {
      return visitors.stream().collect(Collectors.groupingBy(v -> v.getCountryCode(),
               Collectors.reducing(0, e -> 1, Integer::sum)));
   }

   public Map<YearMonth, Integer> getVisitorsPerMonth() {
      return visitors.stream().collect(Collectors.groupingBy(v -> v.getMonth(),
               Collectors.reducing(0, e -> 1, Integer::sum)));
   }

   public static final class Visitor {

      private final YearMonth month;
      private final String country;

      public Visitor(LocalDateTime date, String country) {
         this.month = YearMonth.from(date.atZone(ZoneId.systemDefault()));
         this.country = country;
      }

      public String getCountryCode() {
         return country;
      }

      public YearMonth getMonth() {
         return month;
      }
   }
}
