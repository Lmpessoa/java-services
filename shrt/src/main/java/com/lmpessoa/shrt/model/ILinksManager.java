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

import static com.lmpessoa.services.services.Reuse.ALWAYS;

import java.sql.SQLException;
import java.time.Duration;

import com.lmpessoa.services.security.IIdentity;
import com.lmpessoa.services.services.IHealthProvider;
import com.lmpessoa.services.services.Service;

@Service(reuse = ALWAYS)
public interface ILinksManager extends IHealthProvider {

   /* Link related */

   String shorten(String url);

   String shorten(String url, String slug);

   String expand(String slug);

   boolean rename(String oldSlug, String newSlug);

   boolean delete(String slug);

   void log(String slug, String ipAddress);

   LinkInfo info(String slug) throws SQLException;

   /* User related */

   boolean isValidUser(String user, String password);

   IIdentity getUserOfToken(String token);

   void setTokenForUser(String user, String token, Duration expires);

   void removeToken(String token);
}
