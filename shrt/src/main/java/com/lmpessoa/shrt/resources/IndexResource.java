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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

import javax.validation.constraints.Pattern;

import com.lmpessoa.services.ContentType;
import com.lmpessoa.services.HttpInputStream;
import com.lmpessoa.services.NotFoundException;
import com.lmpessoa.services.Redirect;
import com.lmpessoa.services.Route;
import com.lmpessoa.services.hosting.ConnectionInfo;
import com.lmpessoa.services.security.Authorize;
import com.lmpessoa.services.views.ViewAndModel;
import com.lmpessoa.shrt.model.ILinksManager;
import com.lmpessoa.shrt.model.LinkInfo;

public class IndexResource {

   private final ConnectionInfo remote;
   private final ILinksManager links;

   public IndexResource(ILinksManager links, ConnectionInfo remote) {
      this.remote = remote;
      this.links = links;
   }

   @Authorize
   public Redirect get() throws MalformedURLException {
      // TODO Implements root access
      return null;
   }

   public Object get(@Pattern(regexp = "[a-z0-9]{3,9}") String link)
      throws MalformedURLException, SQLException {
      LinkInfo result = links.info(link);
      if (result == null) {
         throw new NotFoundException();
      }
      links.log(link, remote.getRemoteAddress().getHostAddress());
      return Redirect.to(result.getUrl());
   }

   @Authorize
   public ViewAndModel info(@Pattern(regexp = "[a-z0-9]{3,9}") String link) throws SQLException {
      LinkInfo result = links.info(link);
      if (result == null) {
         throw new NotFoundException();
      }
      links.log(link, remote.getRemoteAddress().getHostAddress());
      return new ViewAndModel("preview", result);
   }

   @Authorize
   @Route("info/{0}.map.svg")
   public InputStream worldMap(@Pattern(regexp = "[a-z0-9]{3,9}") String link)
      throws SQLException, URISyntaxException, IOException {
      LinkInfo info = links.info(link);
      Path worldSvg = Paths.get(LinkInfo.class.getResource("/world.svg").toURI());
      String[] worldLines = Files.readAllLines(worldSvg, StandardCharsets.UTF_8)
               .toArray(new String[0]);
      Path gradientFile = Paths.get(LinkInfo.class.getResource("/gradients.txt").toURI());
      String[] gradient = Files.readAllLines(gradientFile, StandardCharsets.UTF_8)
               .toArray(new String[0]);
      Map<String, Integer> visitors = info.getVisitorsPerCountry();
      int max = visitors.values().stream().mapToInt(i -> i).max().orElse(0);
      if (max > 0) {
         String countryName = null;
         String fill = "eeeeee";
         int count = 0;
         for (int i = 0; i < worldLines.length; ++i) {
            String line = worldLines[i].trim();
            if (line.startsWith("id=\"")) {
               String country = line.substring(4, 6);
               count = visitors.getOrDefault(country, 0);
               int grad = count * 100 / max;
               fill = gradient[grad];
            } else if (line.startsWith("data-name=\"")) {
               countryName = line.substring(11, line.length() - 1);
            } else if (line.startsWith("fill=\"")) {
               worldLines[i] = "     fill=\"#" + fill + "\"";
            } else if (line.equals("/>") && count > 0) {
               worldLines[i] = String.format("  ><title>%s, %d visitor%s</title></path>",
                        countryName, count, count > 1 ? "s" : "");
            }
         }
      }
      return new HttpInputStream(String.join("\n", worldLines).getBytes(UTF_8), ContentType.SVG,
               UTF_8);
   }

   @Authorize
   @Route("info/{0}.graph.svg")
   public HttpInputStream visitorGraph(@Pattern(regexp = "[a-z0-9]{3,9}") String link)
      throws SQLException {
      LinkInfo info = links.info(link);
      Map<YearMonth, Integer> visitors = info.getVisitorsPerMonth();
      int max = visitors.values().stream().mapToInt(i -> i).max().orElse(1);
      YearMonth ym = YearMonth.now().minusMonths(27);
      StringBuilder result = new StringBuilder();
      result.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
      result.append(
               "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"550\" height=\"275\" version=\"1.1\" viewBox=\"0 0 600 300\">\n");
      result.append("  <g fill=\"#d9d7d4\">\n");
      result.append("    <rect x=\"0\" y=\"240\" width=\"600\" height=\"1\" />\n");
      result.append("    <rect x=\"20\" y=\"241\" width=\"1\" height=\"10\" />\n");
      result.append("    <rect x=\"100\" y=\"241\" width=\"1\" height=\"10\" />\n");
      result.append("    <rect x=\"180\" y=\"241\" width=\"1\" height=\"10\" />\n");
      result.append("    <rect x=\"260\" y=\"241\" width=\"1\" height=\"10\" />\n");
      result.append("    <rect x=\"340\" y=\"241\" width=\"1\" height=\"10\" />\n");
      result.append("    <rect x=\"420\" y=\"241\" width=\"1\" height=\"10\" />\n");
      result.append("    <rect x=\"500\" y=\"241\" width=\"1\" height=\"10\" />\n");
      result.append(String.format(
               "    <text x=\"20\" y=\"265\" font-size=\"11px\" text-anchor=\"middle\">%s '%d</text>\n",
               ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault()).toUpperCase(),
               ym.getYear() % 1000));
      result.append(String.format(
               "    <text x=\"260\" y=\"265\" font-size=\"11px\" text-anchor=\"middle\">%s '%d</text>\n",
               ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault()).toUpperCase(),
               ym.getYear() % 1000 + 1));
      result.append(String.format(
               "    <text x=\"500\" y=\"265\" font-size=\"11px\" text-anchor=\"middle\">%s '%d</text>\n",
               ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault()).toUpperCase(),
               ym.getYear() % 1000 + 2));
      result.append("  </g>\n");

      result.append("  <g fill=\"#ff6900\">\n");
      for (int i = 0; i < 28; ++i, ym = ym.plusMonths(1)) {
         int count = visitors.getOrDefault(ym, 0);
         int height = count * 200 / max;
         if (count > 0) {
            result.append(String.format("    <rect x=\"%d\" y=\"%d\" width=\"18\" height=\"%d\">\n",
                     i * 20 + 11, 240 - height, height));
            result.append(String.format("      <title>%s %d<br/>Total clicks: %d</title>\n",
                     ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()),
                     ym.getYear(), count));
            result.append("    </rect>\n");
         }
      }
      result.append("  </g>\n");
      result.append("</svg>\n");
      return new HttpInputStream(result.toString().getBytes(UTF_8), ContentType.SVG, UTF_8);
   }
}
