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
 */package com.lmpessoa.shrt.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.constraints.NotNull;

@Retention(RUNTIME)
@Target({ METHOD, PARAMETER, FIELD })
@Constraint(validatedBy = Url.Validator.class)
@NotNull
public @interface Url {

   public String message()

   default "{com.lmpessoa.services.shrt.validation.Url.message}";

   public Class<?>[] groups() default {};

   public Class<? extends Payload>[] payload() default {};

   static class Validator implements ConstraintValidator<Url, String> {

      @Override
      public boolean isValid(String value, ConstraintValidatorContext context) {
         return value.matches(
                  "^(http|ftp)s?:\\/\\/([^:@\\n\\s](:[^@\\n\\s])@)?([1-9][0-9]{0,2}(.[1-9][0-9]{0,2}){3}"
                           + "|[^./\\n\\s?#]+(\\.[^./\\n\\s?#]+)+)(:[1-9][0-9]*)?(\\/[^/\\n\\s?#]*)*"
                           + "(#[a-zA-Z0-9&=_-]*)?(\\?[a-zA-Z0-9=%&_-]+)?");
      }
   }
}
