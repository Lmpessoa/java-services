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

import java.util.function.Predicate;

/**
 * Provides means to further configure identity support by the application.
 *
 * <p>
 * Methods in this interface can be used to configure the behaviour of the identity engine.
 * </p>
 */
public interface IIdentityOptions {

   /**
    * Defines a policy to be used to restrict users from parts of the application.
    *
    * <p>
    * A policy defines a complex rule that can be used to restrict users from accessing parts of
    * your application. While roles are usually only tested for presence in the user's identity,
    * policies can actually evaluate the presence and content of information in a user's identity.
    * For example, the following example ensures a user must belong to a certain department to
    * access a method guarded by it:
    * </p>
    *
    * <pre>
    * options.addPolicy("human-resources", identity -> {
    *    Collection<Claim> depts = identity.getAll("urn:deptname");
    *    for (Claim dept : depts) {
    *       if ("Human Resources".equals(dept.getValue())) {
    *          return true;
    *       }
    *    }
    *    return false;
    * });
    * </pre>
    *
    * <p>
    * Given the definition, methods can be protected with this policy using the {@link Authorize}
    * annotation. Here is an example:
    * </p>
    *
    * <pre>
    * &#64;Authorize(policy = "human-resources")
    * </pre>
    *
    * <p>
    * Note that policy names are case sensitive. However, it is recommended that policy names
    * differing only by case are avoided.
    * </p>
    *
    * @param policyName
    * @param policyMethod
    */
   void addPolicy(String policyName, Predicate<IIdentity> policyMethod);
}
