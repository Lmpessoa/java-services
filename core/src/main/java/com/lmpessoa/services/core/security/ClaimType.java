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
package com.lmpessoa.services.core.security;

/**
 * Enumerates a series of common/shared claim types.
 * <p>
 * The identifiers provided by these constants can be used between multiple sources/issuers in a
 * manner of stating their claims contain the very same type of information. Note, however, that
 * this does not mean the format of the data for the same claim type from different sources will be
 * of the same type of object. Developers must evaluate and cast values to the correct value type
 * before using any values.
 * </p>
 */
public final class ClaimType {

   public static final String ACCOUNT_NAME = "identity:claims:accountname";
   public static final String AVATAR_URL = "identity:claims:avatarurl";
   public static final String COUNTRY = "identity:claims:country";
   public static final String DATE_OF_BIRTH = "identity:claims:dateofbirth";
   public static final String DISPLAY_NAME = "identity:claims:displayname";
   public static final String EMAIL = "identity:claims:email";
   public static final String EXPIRATION = "identity:claims:expiration";
   public static final String EXPIRED = "identity:claims:expired";
   public static final String GENDER = "identity:claims:gender";
   public static final String GIVEN_NAME = "identity:claims:givenname";
   public static final String HOME_PHONE = "identity:claims:homephone";
   public static final String LOCALE = "identity:claims:locality";
   public static final String MOBILE_PHONE = "identity:claims:mobilephone";
   public static final String NAME = "identity:claims:name";
   public static final String OTHER_PHONE = "identity:claims:otherphone";
   public static final String POSTAL_CODE = "identity:claims:postalcode";
   public static final String ROLE = "identity:claims:role";
   public static final String SERIAL_NUMBER = "identity:claims:serialnumber";
   public static final String STATE_OR_PROVINCE = "identity:claims:stateorprovince";
   public static final String STREET_ADDRESS = "identity:claims:streetaddress";
   public static final String SURNAME = "identity:claims:surname";
   public static final String THUMBPRINT = "identity:claims:thumbprint";
   public static final String VERSION = "identity:claims:version";
   public static final String WEBPAGE = "identity:claims:webpage";
   public static final String WORK_PHONE = "identity:claims:workphone";

   private ClaimType() {
   }
}
