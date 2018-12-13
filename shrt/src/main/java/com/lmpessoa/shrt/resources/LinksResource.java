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
package com.lmpessoa.shrt.resources;

import java.net.MalformedURLException;
import java.sql.SQLException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.lmpessoa.services.ConflictException;
import com.lmpessoa.services.NotFoundException;
import com.lmpessoa.services.security.Authorize;
import com.lmpessoa.shrt.model.CreateLinkRequest;
import com.lmpessoa.shrt.model.ExpandLinkRequest;
import com.lmpessoa.shrt.model.ILinksManager;
import com.lmpessoa.shrt.model.LinkInfo;
import com.lmpessoa.shrt.model.RenameLinkRequest;

@Authorize
public class LinksResource {

   private final ILinksManager links;

   public LinksResource(ILinksManager links) {
      this.links = links;
   }

   public LinkInfo get(@NotNull @Valid ExpandLinkRequest payload)
      throws MalformedURLException, SQLException {
      LinkInfo result = links.info(payload.getShortUrl());
      if (result == null) {
         throw new NotFoundException();
      }
      return result;
   }

   public LinkInfo post(@NotNull @Valid CreateLinkRequest payload)
      throws MalformedURLException, SQLException {
      String shortUrl;
      String longUrl = payload.getLongUrl();
      if (payload.getShortUrl() != null) {
         shortUrl = links.shorten(longUrl, payload.getShortUrl());
      } else {
         shortUrl = links.shorten(longUrl);
      }
      if (shortUrl == null) {
         throw new NotFoundException();
      }
      return links.info(shortUrl);
   }

   public LinkInfo patch(@NotNull @Valid RenameLinkRequest payload) throws SQLException {
      if (links.expand(payload.getOldSlug()) == null) {
         throw new NotFoundException();
      }
      if (links.expand(payload.getNewSlug()) != null) {
         throw new ConflictException("sh.rt link already in use: " + payload.getNewSlug());
      }
      links.rename(payload.getOldSlug(), payload.getNewSlug());
      return links.info(payload.getNewSlug());
   }

   public void delete(@NotNull @Valid ExpandLinkRequest payload) {
      if (links.expand(payload.getShortUrl()) != null) {
         throw new NotFoundException();
      }
      links.delete(payload.getShortUrl());
   }
}
