/*
 * Copyright (c) 2017 Leonardo Pessoa
 * http://github.com/lmpessoa/java-services
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
package com.lmpessoa.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.hosting.ApplicationBridge;
import com.lmpessoa.util.ArgumentReader;

public class ArgumentReaderTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private ArgumentReader reader;

   @Before
   public void setup() {
      reader = new ArgumentReader();
   }

   @Test
   public void testLongWithIntArgument() throws IllegalAccessException {
      reader.setOption("port", Integer::valueOf);
      Map<String, Object> result = reader.parse(new String[] { "--port", "5617" });
      assertTrue(result.containsKey("port"));
      assertEquals(5617, result.get("port"));
   }

   @Test
   public void testShortFromLongName() throws IllegalAccessException {
      reader.setOption("port", Integer::valueOf);
      Map<String, Object> result = reader.parse(new String[] { "-p", "5617" });
      assertTrue(result.containsKey("port"));
      assertEquals(5617, result.get("port"));
   }

   @Test
   public void testLongNameMissingArgument() throws IllegalAccessException {
      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("Option requires an argument: port");
      reader.setOption("port", Integer::valueOf);
      reader.parse(new String[] { "--port" });
   }

   @Test
   public void testWrongArgumentType() throws Throwable {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid value for port: 'xxx'");
      reader.setOption("port", Integer::valueOf);
      reader.parse(new String[] { "--port", "xxx" });
   }

   @Test
   public void testNonExistantArgument() throws IllegalAccessException {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid option: --xport");
      reader.setOption("port", Integer::valueOf);
      reader.parse(new String[] { "--xport" });
   }

   @Test
   public void testLongFlagArgument() throws IllegalAccessException {
      reader.setFlag("enable");
      Map<String, Object> result = reader.parse(new String[] { "--enable" });
      assertTrue(result.containsKey("enable"));
      assertTrue((Boolean) result.get("enable"));
   }

   @Test
   public void testShortFlagArgument() throws IllegalAccessException {
      reader.setFlag("enable", 'X');
      Map<String, Object> result = reader.parse(new String[] { "-X" });
      assertTrue(result.containsKey("enable"));
      assertTrue((Boolean) result.get("enable"));
   }

   @Test
   public void testShortFlagWithWrongCase() throws IllegalAccessException {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid option: -x");
      reader.setFlag("enable", 'X');
      reader.parse(new String[] { "-x" });
   }

   @Test
   public void testFlagsExpectNoArgument() throws IllegalAccessException {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid option: false");
      reader.setFlag("enable", 'X');
      reader.parse(new String[] { "-X", "false" });
   }

   @Test
   public void testAddressByIP() throws IllegalAccessException, UnknownHostException {
      reader.setOption("bind", ApplicationBridge::parseIpAddress);
      Map<String, Object> result = reader.parse(new String[] { "--bind", "0.0.0.0" });
      assertTrue(result.containsKey("bind"));
      assertTrue(result.get("bind") instanceof InetAddress);
      assertEquals(InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 }), result.get("bind"));
   }

   @Test
   public void testAddressLocalhost() throws IllegalAccessException, UnknownHostException {
      reader.setOption("bind", ApplicationBridge::parseIpAddress);
      Map<String, Object> result = reader.parse(new String[] { "--bind", "localhost" });
      assertTrue(result.containsKey("bind"));
      assertTrue(result.get("bind") instanceof InetAddress);
      assertEquals(InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), result.get("bind"));
   }

   @Test
   public void testAddressByName() throws IllegalAccessException {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid value for bind: 'server'");
      reader.setOption("bind", ApplicationBridge::parseIpAddress);
      reader.parse(new String[] { "--bind", "server" });
   }
}
