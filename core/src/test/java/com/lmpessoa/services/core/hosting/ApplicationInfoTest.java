/*
 * Copyright (c) 2017 Leonardo Pessoa
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
package com.lmpessoa.services.core.hosting;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.core.hosting.ApplicationServerInfo;

public final class ApplicationInfoTest {

   private ApplicationServerInfo info;
   private File logFile;

   @Before
   public void setup() throws IOException {
      logFile = File.createTempFile("test_", ".log");
   }

   @After
   public void teardown() {
      logFile.delete();
   }

   @Test
   public void testSettingsWithProperties() throws IOException {
      StringReader config = new StringReader("server.port=5050\n" + //
               "server.address=0.0.0.0\n" + //
               "logging.writer.type=file\n" + //
               "logging.writer.filename=" + logFile.getAbsolutePath() + "\n" + //
               "logging.writer.template=[{Severity}] {Message}\n" + //
               "logging.default=WARNING\n" + //
               "logging.packages.0.name=com.lmpessoa.services.util.logging\n" + //
               "logging.packages.0.level=INFO\n" + //
               "logging.packages.1.name=com.lmpessoa.services.data\n" + //
               "logging.packages.1.level=DEBUG");
      info = new ApplicationServerInfo("PROPERTIES", config);
      assertParsedConfig(info);
   }

   @Test
   public void testSettingsWithYaml() throws IOException {
      StringReader config = new StringReader( //
               "server:\n" + //
                        "   port: 5050\n" + //
                        "   address: 0.0.0.0\n" + //
                        "logging:\n" + //
                        "   writer:\n" + //
                        "      type: file\n" + //
                        "      filename: " + logFile.getAbsolutePath() + "\n" + //
                        "      template: \"[{Severity}] {Message}\"\n" + //
                        "   default: WARNING\n" + //
                        "   packages:\n" + //
                        "      - name: com.lmpessoa.services.util.logging\n" + //
                        "        level: INFO\n" + //
                        "      - name: com.lmpessoa.services.data\n" + //
                        "        level: DEBUG\n");
      info = new ApplicationServerInfo("YML", config);
      assertParsedConfig(info);
   }

   @Test
   public void testSettingsWithJson() throws IOException {
      StringReader config = new StringReader("{\n" + //
               "  \"server\" : {\n" + //
               "    \"port\" : \"5050\",\n" + //
               "    \"address\" : \"0.0.0.0\"\n" + //
               "  },\n" + //
               "  \"logging\" : {\n" + //
               "    \"writer\" : {\n" + //
               "      \"type\" : \"file\",\n" + //
               "      \"filename\" : \"" + logFile.getAbsolutePath() + "\",\n" + //
               "      \"template\" : \"[{Severity}] {Message}\"\n" + //
               "    },\n" + //
               "    \"default\" : \"WARNING\",\n" + //
               "    \"packages\" : [\n" + //
               "      {\n" + //
               "        \"name\" : \"com.lmpessoa.services.util.logging\",\n" + //
               "        \"level\" : \"INFO\"\n" + //
               "      },\n" + //
               "      {\n" + //
               "        \"name\" : \"com.lmpessoa.services.data\",\n" + //
               "        \"level\" : \"DEBUG\"\n" + //
               "      }\n" + //
               "    ]\n" + //
               "  }\n" + //
               "}\n");
      info = new ApplicationServerInfo("JSON", config);
      assertParsedConfig(info);
   }

   private void assertParsedConfig(ApplicationServerInfo info) throws IOException {
      assertEquals(Optional.of("0.0.0.0"), info.getProperty("server.address"));
      assertEquals(Optional.of(5050), info.getIntProperty("server.port"));
      assertEquals(Optional.of("file"), info.getProperty("logging.writer.type"));
      Map<String, String> packages = new HashMap<>();
      packages.put("0.name", "com.lmpessoa.services.util.logging");
      packages.put("0.level", "INFO");
      packages.put("1.name", "com.lmpessoa.services.data");
      packages.put("1.level", "DEBUG");
      assertEquals(packages, info.getProperties("logging.packages"));
   }
}
