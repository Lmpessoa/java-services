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
package com.lmpessoa.services.internal;

import java.util.Locale;

import com.lmpessoa.services.Localized;

public final class ErrorMessage {

   private static final Localized.MessageSet MESSAGES = Localized
            .messagesAt("messages/ExceptionMessages.properties", Locale.getDefault());

   // com.lmpessoa.services.hosting
   public static final Localized.Message ILLEGAL_HEADER_NAME = MESSAGES
            .get("com.lmpessoa.services.hosting.Headers.illegalname");
   // com.lmpessoa.services.hosting
   public static final Localized.Message INVALID_CONTENT_TYPE = MESSAGES
            .get("com.lmpessoa.services.HttpInputStream.contenttype");

   // com.lmpessoa.services.internal
   public static final Localized.Message TOO_MANY_CONSTRUCTORS = MESSAGES
            .get("com.lmpessoa.services.internal.singleconstructor");

   // com.lmpessoa.services.internal.hosting
   public static final Localized.Message RESPONDER_REGISTERED = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.responder.registered");
   public static final Localized.Message RESPONDER_NOT_CONCRETE = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.responder.notconcrete");
   public static final Localized.Message RESPONDER_CONSTRUCTOR_MISSING = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.responder.constructor");
   public static final Localized.Message RESPONDER_INVOKE_MISSING = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.responder.invoke");
   public static final Localized.Message STATIC_CONFIGURED = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.static.config");
   public static final Localized.Message IDENTITY_CONFIGURED = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.identity.config");
   public static final Localized.Message IDENTITY_CONSTRUCTOR_MISSING = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.identity.constructor");
   public static final Localized.Message IDENTITY_DUPLICATE_POLICY = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.identity.policy");
   public static final Localized.Message HEALTH_CONFIGURED = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.health.config");
   public static final Localized.Message APPLICATION_CONFIGURED = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.configured");
   public static final Localized.Message ASYNC_PATH_CONFIGURED = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.async.pathconfig");
   public static final Localized.Message ASYNC_ILLEGAL_DEFAULT = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.async.default");
   public static final Localized.Message ASYNC_REJECT_CONFIGURED = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.async.rejectconfig");
   public static final Localized.Message ASYNC_MATCHER_CONFIGURED = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.async.matcherconfig");
   public static final Localized.Message ILLEGAL_HEADER_LINE = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.illegalheader");
   public static final Localized.Message NEXT_RESPONDER_INVOKED = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.responderinvoked");
   public static final Localized.Message PATH_MISSING = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.pathmissing");
   public static final Localized.Message SERIALIZE_CONTENT_TYPE = MESSAGES
            .get("com.lmpessoa.services.internal.hosting.contenttype");

   // com.lmpessoa.services.internal.logging
   public static final Localized.Message INVALID_VARIABLE_REFERENCE = MESSAGES
            .get("com.lmpessoa.services.internal.logging.invalidvarref");
   public static final Localized.Message UNKNOWN_VARIABLE = MESSAGES
            .get("com.lmpessoa.services.internal.logging.unknownvar");
   public static final Localized.Message SUPPLIER_DEFINED = MESSAGES
            .get("com.lmpessoa.services.internal.logging.supplierdefined");
   public static final Localized.Message VARIABLE_DEFINED = MESSAGES
            .get("com.lmpessoa.services.internal.logging.variabledefined");
   public static final Localized.Message INVALID_FACILITY_VALUE = MESSAGES
            .get("com.lmpessoa.services.internal.logging.syslogfacility");
   public static final Localized.Message NEGATIVE_TIMEOUT = MESSAGES
            .get("com.lmpessoa.services.internal.logging.syslogtimeout");
   public static final Localized.Message UNKNOWN_SYSLOG_FORMAT = MESSAGES
            .get("com.lmpessoa.services.internal.logging.syslogformat");

   // com.lmpessoa.services.internal.parsing
   public static final Localized.Message LITERAL_REQUIRED = MESSAGES
            .get("com.lmpessoa.services.internal.parsing.literalrequired");
   public static final Localized.Message UNEXPECTED_END = MESSAGES
            .get("com.lmpessoa.services.internal.parsing.unexpectedend");
   public static final Localized.Message INVALID_VARIABLE_NAME = MESSAGES
            .get("com.lmpessoa.services.internal.parsing.invalidvariable");

   // com.lmpessoa.services.internal.routing
   public static final Localized.Message INVALID_AREA_PATH = MESSAGES
            .get("com.lmpessoa.services.internal.routing.invalidarea");
   public static final Localized.Message INVALID_DEFAULT_RESOURCE = MESSAGES
            .get("com.lmpessoa.services.internal.routing.invaliddefault");
   public static final Localized.Message INVALID_AREA_NAME = MESSAGES
            .get("com.lmpessoa.services.internal.routing.invalidarea");
   public static final Localized.Message INVALID_ROUTE_PART = MESSAGES
            .get("com.lmpessoa.services.internal.routing.invalidroute");
   public static final Localized.Message ILLEGAL_PARAMETER = MESSAGES
            .get("com.lmpessoa.services.internal.routing.illegalparam");
   public static final Localized.Message MISSING_PARAMETERS = MESSAGES
            .get("com.lmpessoa.services.internal.routing.missingparams");
   public static final Localized.Message CANNOT_CAST_VALUE = MESSAGES
            .get("com.lmpessoa.services.internal.routing.cannotcast");
   public static final Localized.Message RESOURCE_NOT_CONCRETE = MESSAGES
            .get("com.lmpessoa.services.internal.routing.notconcrete");
   public static final Localized.Message ILLEGAL_TYPE_IN_ROUTE = MESSAGES
            .get("com.lmpessoa.services.internal.routing.illegaltype");
   public static final Localized.Message QUERY_IN_PATH = MESSAGES
            .get("com.lmpessoa.services.internal.routing.querypath");
   public static final Localized.Message NULL_PATH = MESSAGES
            .get("com.lmpessoa.services.internal.routing.nullpath");
   public static final Localized.Message EXPECTED_NUMBER_TYPE = MESSAGES
            .get("com.lmpessoa.services.internal.routing.expectednumber");
   public static final Localized.Message EXPECTED_STRING_TYPE = MESSAGES
            .get("com.lmpessoa.services.internal.routing.expectedstring");

   // com.lmpessoa.services.internal.security
   public static final Localized.Message INVALID_CLAIM_TYPE = MESSAGES
            .get("com.lmpessoa.services.internal.security.invalidclaim");

   // com.lmpessoa.services.internal.serializing
   public static final Localized.Message UNEXPECTED_CONTENT_TYPE = MESSAGES
            .get("com.lmpessoa.services.internal.serializing.unexpectedtype");
   public static final Localized.Message UNEXPECTED_ENCODING = MESSAGES
            .get("com.lmpessoa.services.internal.serializing.unexpectedencoding");
   public static final Localized.Message CANNOT_PARSE_DATE = MESSAGES
            .get("com.lmpessoa.services.internal.serializing.cannotparsedate");

   // com.lmpessoa.services.internal.services
   public static final Localized.Message SERVICE_NOT_CONCRETE = MESSAGES
            .get("com.lmpessoa.services.internal.services.notconcrete");
   public static final Localized.Message SERVICE_DEPENDENCY = MESSAGES
            .get("com.lmpessoa.services.internal.services.dependency");
   public static final Localized.Message SERVICE_LOWER_LIFETIME = MESSAGES
            .get("com.lmpessoa.services.internal.services.lowerreuse");
   public static final Localized.Message SERVICE_MISSING_REUSE = MESSAGES
            .get("com.lmpessoa.services.internal.services.noreuse");
   public static final Localized.Message SERVICE_SINGLETON = MESSAGES
            .get("com.lmpessoa.services.internal.services.singleton");
   public static final Localized.Message SERVICE_NOT_FOUND = MESSAGES
            .get("com.lmpessoa.services.internal.services.notfound");
   public static final Localized.Message TOO_MANY_METHODS = MESSAGES
            .get("com.lmpessoa.services.internal.services.singlemethod");
   public static final Localized.Message MISMATCHED_CALL = MESSAGES
            .get("com.lmpessoa.services.internal.services.mismatchedcall");
   public static final Localized.Message SERVICE_NOT_PER_REQUEST = MESSAGES
            .get("com.lmpessoa.services.internal.services.notrequest");
   public static final Localized.Message SERVICE_REGISTERED = MESSAGES
            .get("com.lmpessoa.services.internal.services.registered");

   // com.lmpessoa.services.internal.validating
   public static final Localized.Message AMBIGUOUS_TARGET = MESSAGES
            .get("com.lmpessoa.services.internal.validating.ambiguoustarget");
   public static final Localized.Message UNKNOWN_CONSTRAINT = MESSAGES
            .get("com.lmpessoa.services.internal.validating.unknownconstraint");
   public static final Localized.Message MISSING_METHOD = MESSAGES
            .get("com.lmpessoa.services.internal.validating.missingmethod");

   // com.lmpessoa.services.internal.ClassUtils
   public static final Localized.Message INVALID_CHAR_CAST = MESSAGES
            .get("com.lmpessoa.services.internal.ClassUtils.castchar");
   public static final Localized.Message VALUEOF_STATIC = MESSAGES
            .get("com.lmpessoa.services.internal.ClassUtils.valueof");

   public static final Localized.Message INVALID_FILE = MESSAGES
            .get("com.lmpessoa.services.internal.Property.notafile");

   private ErrorMessage() {
      // Nothing to be done here
   }
}
