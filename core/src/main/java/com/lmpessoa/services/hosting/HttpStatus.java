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
package com.lmpessoa.services.hosting;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class HttpStatus {

   private static Map<Integer, String> STATUSES = new HashMap<>();
   static {
      try {
         final URI uri = HttpStatus.class.getResource("/status.codes").toURI();
         List<String> statuses = Files.readAllLines(Paths.get(uri));
         for (String line : statuses) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
               int code = Integer.parseInt(parts[0]);
               STATUSES.put(code, parts[1]);
            }
         }
      } catch (IOException | URISyntaxException e) {
         // Any error here is treated as fatal
         e.printStackTrace();
         System.exit(1);
      }
   }

   public static String getLabelFor(int code) {
      return STATUSES.get(code);
   }

   private HttpStatus() {
   }
}
