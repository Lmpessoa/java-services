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
package com.lmpessoa.util.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public final class XmlArray extends XmlElement implements List<XmlElement> {

   private final List<XmlElement> items = new ArrayList<>();

   @Override
   protected String buildXmlAtLevel(int indentLevel) {
      StringBuilder result = new StringBuilder();
      for (XmlElement item : items) {
         result.append(item.buildXmlAtLevel(indentLevel));
         result.append("\n");
      }
      return result.toString();
   }

   @Override
   public int size() {
      return items.size();
   }

   @Override
   public boolean isEmpty() {
      return items.isEmpty();
   }

   @Override
   public boolean contains(Object item) {
      return items.contains(item);
   }

   @Override
   public Iterator<XmlElement> iterator() {
      return items.iterator();
   }

   @Override
   public Object[] toArray() {
      return items.toArray();
   }

   @Override
   public <T> T[] toArray(T[] a) {
      return items.toArray(a);
   }

   @Override
   public boolean add(XmlElement item) {
      return items.add(item);
   }

   @Override
   public boolean remove(Object item) {
      return items.remove(item);
   }

   @Override
   public boolean containsAll(Collection<?> items) {
      return this.items.containsAll(items);
   }

   @Override
   public boolean addAll(Collection<? extends XmlElement> items) {
      return this.items.addAll(items);
   }

   @Override
   public boolean addAll(int index, Collection<? extends XmlElement> items) {
      return this.items.addAll(index, items);
   }

   @Override
   public boolean removeAll(Collection<?> items) {
      return this.items.removeAll(items);
   }

   @Override
   public boolean retainAll(Collection<?> items) {
      return this.items.retainAll(items);
   }

   @Override
   public void clear() {
      items.clear();
   }

   @Override
   public XmlElement get(int index) {
      return items.get(index);
   }

   @Override
   public XmlElement set(int index, XmlElement item) {
      return items.set(index, item);
   }

   @Override
   public void add(int index, XmlElement item) {
      items.add(index, item);
   }

   @Override
   public XmlElement remove(int index) {
      return items.remove(index);
   }

   @Override
   public int indexOf(Object item) {
      return items.indexOf(item);
   }

   @Override
   public int lastIndexOf(Object item) {
      return items.lastIndexOf(item);
   }

   @Override
   public ListIterator<XmlElement> listIterator() {
      return items.listIterator();
   }

   @Override
   public ListIterator<XmlElement> listIterator(int index) {
      return items.listIterator(index);
   }

   @Override
   public List<XmlElement> subList(int fromIndex, int toIndex) {
      return items.subList(fromIndex, toIndex);
   }

}
