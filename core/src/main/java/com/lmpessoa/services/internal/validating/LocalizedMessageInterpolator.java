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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.validation.MessageInterpolator;

import com.lmpessoa.services.internal.hosting.Localization;

final class LocalizedMessageInterpolator implements MessageInterpolator {

   private final MessageInterpolator delegate;

   public LocalizedMessageInterpolator(MessageInterpolator delegate) {
      this.delegate = Objects.requireNonNull(delegate);
   }

   @Override
   public String interpolate(String messageTemplate, Context context) {
      String defaultMessage = interpolate(messageTemplate, context, Locale.ROOT);
      List<Locale> locales = new ArrayList<>();
      locales.addAll(Arrays.asList(Localization.getLocales()));
      if (!locales.contains(Locale.getDefault())) {
         locales.add(Locale.getDefault());
      }
      for (Locale locale : locales) {
         String message = interpolate(messageTemplate, context, locale);
         if (message != null && !message.equals(defaultMessage)) {
            return message;
         }
      }
      return defaultMessage;
   }

   @Override
   public String interpolate(String messageTemplate, Context context, Locale locale) {
      return delegate.interpolate(messageTemplate, context, locale);
   }

}
