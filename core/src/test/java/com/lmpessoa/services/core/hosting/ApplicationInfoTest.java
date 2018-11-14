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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.Logger;

public final class ApplicationInfoTest {

   private ApplicationInfo info;
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
               "logging.packages.0.level=ERROR\n" + //
               "logging.packages.1.name=com.lmpessoa.services.data\n" + //
               "logging.packages.1.level=DEBUG");
      info = new ApplicationInfo(ApplicationInfoTest.class,
               ApplicationInfo.parseConfigMapFile("PROPERTIES", config));
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
                        "        level: ERROR\n" + //
                        "      - name: com.lmpessoa.services.data\n" + //
                        "        level: DEBUG\n");
      info = new ApplicationInfo(ApplicationInfoTest.class,
               ApplicationInfo.parseConfigMapFile("YML", config));
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
      info = new ApplicationInfo(ApplicationInfoTest.class,
               ApplicationInfo.parseConfigMapFile("JSON", config));
      assertParsedConfig(info);
   }

   private void assertParsedConfig(ApplicationInfo info) throws IOException {
      assertEquals(5050, info.getPort());
      assertEquals(InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 }), info.getBindAddress());
      assertFalse(info.isXmlEnabled());
      ILogger log = info.getLogger();
      log.info("Test 1");
      log.error("Test 2");
      ((Logger) log).join();
      String[] logLines = Files.readAllLines(logFile.toPath()).toArray(new String[0]);
      assertArrayEquals(new String[] { "[ERROR] Test 2", "" }, logLines);
   }
}
