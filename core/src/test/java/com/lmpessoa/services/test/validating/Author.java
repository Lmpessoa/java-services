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

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

public class Author {

   private String firstName;

   @NotEmpty(message = "lastname must not be null")
   private String lastName;

   private String companyName;

   public Author(String firstName, String lastName) {
      this.firstName = firstName;
      this.lastName = lastName;
   }

   public Author() {
      // Does nothing
   }

   public String getFirstName() {
      return firstName;
   }

   public String getLastName() {
      return lastName;
   }

   @Size(max = 25)
   public String getCompany() {
      return companyName;
   }

   public void setCompany(String companyName) {
      this.companyName = companyName;
   }

   @Override
   public String toString() {
      return '"' + lastName + (firstName != null ? ", " + firstName : "") + '"';
   }

   @AssertTrue(message = "{com.lmpessoa.services.test.validating.SecurityChecking.message}")
   private boolean isFirstName() {
      return companyName == null || firstName != null;
   }

   @ForceStrongPassword
   @OldAndNewPasswordsDifferent
   public void renewPassword(String oldPassword, String newPassword, String retypedNewPassword) {
      // Nothing to do here now
   }
}
