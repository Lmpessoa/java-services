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
package com.lmpessoa.services.internal.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.test.services.AbstractTestService;
import com.lmpessoa.services.test.services.Requested;
import com.lmpessoa.services.test.services.RequestedImpl;
import com.lmpessoa.services.test.services.Singleton;
import com.lmpessoa.services.test.services.SingletonDependent;
import com.lmpessoa.services.test.services.SingletonImpl;
import com.lmpessoa.services.test.services.TestService;
import com.lmpessoa.services.test.services.Transient;
import com.lmpessoa.services.test.services.TransientDependent;
import com.lmpessoa.services.test.services.TransientImpl;
import com.lmpessoa.services.test.services.TwoConstructorsService;

public class ServiceMapTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private ServiceMap map;

   @Before
   public void setup() {
      Locale.setDefault(Locale.ROOT);
      map = new ServiceMap();
   }

   @Test
   public void testRegisterPrimitive() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Service 'int' does not specify a reuse level");
      map.put(int.class);
   }

   @Test
   public void testRegisterAbstractClass() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Service provider must be a public concrete class");
      map.put(AbstractTestService.class);
   }

   @Test
   public void testTooManyConstructors() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Class '" + TwoConstructorsService.class.getName()
               + "' must have only one constructor (found: 2)");
      map.put(TwoConstructorsService.class);
   }

   @Test
   public void testSameSingleton() {
      map.put(TestService.class);
      TestService o1 = map.get(TestService.class);
      TestService o2 = map.get(TestService.class);
      assertSame(o1, o2);
   }

   @Test
   public void testSingletonWithSubclass() {
      map.put(Singleton.class, SingletonImpl.class);
      Singleton o = map.get(Singleton.class);
      assertTrue(o instanceof SingletonImpl);
   }

   @Test
   public void testDifferentTransient() {
      map.put(Transient.class, TransientImpl.class);
      Transient o1 = map.get(Transient.class);
      Transient o2 = map.get(Transient.class);
      assertNotSame(o1, o2);
   }

   @Test
   public void testSingletonUsingUnregistered() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage(
               "Service '" + TransientDependent.class.getName() + "' depends on service '"
                        + Transient.class.getName() + "' which is not registered");
      map.put(TransientDependent.class);
   }

   @Test
   public void testSingletonUsingTransient() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage(
               "Service '" + TransientDependent.class.getName() + "' depends on service '"
                        + Transient.class.getName() + "' which has a lower lifetime");
      map.put(Transient.class, TransientImpl.class);
      map.put(TransientDependent.class);
   }

   @Test
   public void testTransientUsingSingleton() {
      map.put(Singleton.class, SingletonImpl.class);
      map.put(SingletonDependent.class);
      assertNotNull(map.get(SingletonDependent.class));
   }

   @Test
   public void testSecondRegistration() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Service '" + Singleton.class.getName() + "' is already registered");
      map.putSupplier(Singleton.class, () -> new SingletonImpl());
      assertNotNull(map.get(Singleton.class));
      map.put(Singleton.class, SingletonImpl.class);
   }

   @Test
   public void testInvokeStaticCorrect() throws IllegalAccessException, InvocationTargetException,
      InstantiationException, NoSingleMethodException {
      map.putSupplier(Singleton.class, () -> new SingletonImpl());
      Singleton o = map.get(Singleton.class);
      String str = (String) map.invoke(ServiceMapTest.class, "getRegistryToString");
      assertEquals(o.toString(), str);
   }

   @Test
   public void testInvokeInstanceCorrect() throws IllegalAccessException, InvocationTargetException,
      InstantiationException, NoSingleMethodException {
      map.putSupplier(Singleton.class, () -> new SingletonImpl());
      Singleton o = map.get(Singleton.class);
      String str = (String) map.invoke(this, "getObserverToString");
      assertEquals(o.toString(), str);
   }

   @Test
   public void testInvokeStaticWithInstance() throws IllegalAccessException,
      InvocationTargetException, InstantiationException, NoSingleMethodException {
      map.putSupplier(Singleton.class, () -> new SingletonImpl());
      Singleton o = map.get(Singleton.class);
      String result = (String) map.invoke(this, "getRegistryToString");
      assertEquals(o.toString(), result);
   }

   @Test
   public void testInvokeInstanceWithStatic() throws IllegalAccessException,
      InvocationTargetException, InstantiationException, NoSingleMethodException {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Mismatched static/instance method call");
      map.put(Singleton.class, new SingletonImpl());
      map.invoke(ServiceMapTest.class, "getObserverToString");
   }

   public void testInjectRequestOutOfHandlerJob() {
      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("Cannot locate object pool for this service map");
      map.put(TestService.class);
      map.get(TestService.class);
   }

   @Test
   public void testInjectPerRequest() throws InterruptedException {
      final RequestedImpl[] objs = new RequestedImpl[2];
      map.put(RequestedImpl.class);
      Thread t0 = new Thread(() -> objs[0] = map.get(RequestedImpl.class));
      t0.start();
      Thread t1 = new Thread(() -> objs[1] = map.get(RequestedImpl.class));
      t1.start();
      t0.join();
      t1.join();
      assertNotSame(objs[0], objs[1]);
   }

   @Test
   public void testInjectWithInstancing() throws InterruptedException {
      final Requested[] objs = new Requested[3];
      map.put(Requested.class, RequestedImpl.class);
      Thread t0 = new Thread(() -> {
         objs[0] = map.get(Requested.class);
         objs[1] = map.get(Requested.class);
      });
      t0.start();
      Thread t1 = new Thread(() -> objs[2] = map.get(Requested.class));
      t1.start();
      t0.join();
      t1.join();
      assertSame(objs[0], objs[1]);
      assertNotSame(objs[1], objs[2]);
   }

   @Test
   public void testUnregiestredInjection() {
      thrown.expect(NoSuchElementException.class);
      thrown.expectMessage("Class 'int' is not a registered service");
      map.get(int.class);
   }

   @Test
   public void testInvokeWithNoMethod() throws IllegalAccessException, InvocationTargetException,
      InstantiationException, NoSingleMethodException {
      thrown.expect(NoSingleMethodException.class);
      thrown.expectMessage("Class '" + ServiceMapTest.class.getName()
               + "' must have exactly one method named 'unexistant' (found: 0)");
      map.invoke(this, "unexistant");
   }

   @Test
   public void testInvokeWithDuplicateMethod() throws IllegalAccessException,
      InvocationTargetException, InstantiationException, NoSingleMethodException {
      thrown.expect(NoSingleMethodException.class);
      thrown.expectMessage("Class '" + ServiceMapTest.class.getName()
               + "' must have exactly one method named 'existing' (found: 2)");
      map.invoke(this, "existing");
   }

   @Test
   public void testRegisterWithSupplier() {
      map.putSupplier(Singleton.class, () -> new SingletonImpl());
      Singleton o1 = map.get(Singleton.class);
      Singleton o2 = map.get(Singleton.class);
      assertNotNull(o1);
      assertSame(o1, o2);
   }

   @Test
   public void testRegisterSingletonInstance() {
      map.put(Singleton.class, new SingletonImpl());
      Singleton o1 = map.get(Singleton.class);
      Singleton o2 = map.get(Singleton.class);
      assertNotNull(o1);
      assertSame(o1, o2);
   }

   @Test
   public void testRegisterTransientInstance() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Service instances can only be registered if reuse is ALWAYS");
      map.put(Transient.class, new TransientImpl());
   }

   @Test
   public void testRegisterStringInstance() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Service 'java.lang.String' does not specify a reuse level");
      map.put(String.class, "test");
   }

   // Test data ----------

   public static String getRegistryToString(Singleton observer) {
      return observer.toString();
   }

   public String getObserverToString(Singleton observer) {
      return observer.toString();
   }

   public void existing(String s) {
      // Test method, does nothing
   }

   public void existing(int i) {
      // Test method, does nothing
   }
}
