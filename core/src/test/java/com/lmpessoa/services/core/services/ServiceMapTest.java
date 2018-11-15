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
package com.lmpessoa.services.core.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.NoSuchElementException;
import java.util.Observable;
import java.util.Observer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lmpessoa.services.BrokerObserver;
import com.lmpessoa.services.core.routing.AbstractRouteType;
import com.lmpessoa.services.core.services.IServiceMap;
import com.lmpessoa.services.core.services.NoSingleMethodException;
import com.lmpessoa.services.core.services.ServiceMap;

public class ServiceMapTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   private ServiceMap map;

   @Before
   public void setup() {
      map = new ServiceMap();
      map.useSingleton(IServiceMap.class, map);
   }

   @Test
   public void testRegisterPrimitive() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Provider class must be a concrete class");
      map.useSingleton(int.class);
   }

   @Test
   public void testRegisterAbstractClass() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Provider class must be a concrete class");
      map.useSingleton(AbstractRouteType.class);

   }

   @Test
   public void testTooManyConstructors() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Provider class must have only one constructor (found: 2)");
      map.useSingleton(TwoConstructorsClass.class);
   }

   @Test
   public void testSameSingleton() {
      map.useSingleton(Observable.class);
      Observable o1 = map.get(Observable.class);
      Observable o2 = map.get(Observable.class);
      assertSame(o1, o2);
   }

   @Test
   public void testSingletonWithSubclass() {
      map.useSingleton(Observer.class, BrokerObserver.class);
      Observer o = map.get(Observer.class);
      assertTrue(o instanceof BrokerObserver);
   }

   @Test
   public void testDifferentTransient() {
      map.useTransient(Observer.class, BrokerObserver.class);
      Observer o1 = map.get(Observer.class);
      Observer o2 = map.get(Observer.class);
      assertNotSame(o1, o2);
   }

   @Test
   public void testSingletonUsingUnregistered() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Dependent service " + Observer.class.getName() + " is not registered");
      map.useSingleton(TransientDependent.class);
   }

   @Test
   public void testSingletonUsingTransient() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage(
               "Class " + TransientDependent.class.getName() + " has a lower lifetime than one of its dependencies");
      map.useTransient(Observer.class, BrokerObserver.class);
      map.useSingleton(TransientDependent.class);
   }

   @Test
   public void testTransientUsingSingleton() {
      map.useSingleton(Observer.class, BrokerObserver.class);
      map.useTransient(SingletonDependent.class);
      assertNotNull(map.get(SingletonDependent.class));
   }

   @Test
   public void testSecondRegistration() {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Service " + Observer.class.getName() + " is already registered");
      map.useSingleton(Observer.class, BrokerObserver.class);
      assertNotNull(map.get(Observer.class));
      map.useTransient(Observer.class, BrokerObserver.class);
   }

   @Test
   public void testInvokeStaticCorrect() throws Exception {
      map.useSingleton(Observer.class, new BrokerObserver());
      Observer o = map.get(Observer.class);
      String str = (String) map.invoke(ServiceMapTest.class, "getRegistryToString");
      assertEquals(o.toString(), str);
   }

   @Test
   public void testInvokeInstanceCorrect() throws Exception {
      map.useSingleton(Observer.class, new BrokerObserver());
      Observer o = map.get(Observer.class);
      String str = (String) map.invoke(this, "getObserverToString");
      assertEquals(o.toString(), str);
   }

   @Test
   public void testInvokeStaticWithInstance() throws Exception {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Mismatched static/instance method call");
      map.useSingleton(Observer.class, new BrokerObserver());
      map.invoke(this, "getRegistryToString");
   }

   @Test
   public void testInvokeInstanceWithStatic() throws Exception {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Mismatched static/instance method call");
      map.useSingleton(Observer.class, new BrokerObserver());
      map.invoke(ServiceMapTest.class, "getObserverToString");
   }

   public void testInjectRequestOutOfHandlerJob() {
      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("Cannot locate object pool for this service map");
      map.usePerRequest(Object.class);
      map.get(Object.class);
   }

   @Test
   public void testInjectPerRequest() throws InterruptedException {
      final BrokerObserver[] objs = new BrokerObserver[2];
      map.usePerRequest(BrokerObserver.class);
      Thread t0 = new Thread(() -> {
         objs[0] = map.get(BrokerObserver.class);
      });
      t0.start();
      Thread t1 = new Thread(() -> {
         objs[1] = map.get(BrokerObserver.class);
      });
      t1.start();
      t0.join();
      t1.join();
      assertNotSame(objs[0], objs[1]);
   }

   @Test
   public void testInjectWithInstancing() throws InterruptedException {
      final Observer[] objs = new Observer[3];
      map.usePerRequest(Observer.class, BrokerObserver.class);
      Thread t0 = new Thread(() -> {
         objs[0] = map.get(Observer.class);
         objs[1] = map.get(Observer.class);
      });
      t0.start();
      Thread t1 = new Thread(() -> {
         objs[2] = map.get(Observer.class);
      });
      t1.start();
      t0.join();
      t1.join();
      assertSame(objs[0], objs[1]);
      assertNotSame(objs[1], objs[2]);
   }

   @Test
   public void testUnregiestredInjection() {
      thrown.expect(NoSuchElementException.class);
      thrown.expectMessage("Service not found: int");
      map.get(int.class);
   }

   @Test
   public void testInvokeWithNoMethod() throws Exception {
      thrown.expect(NoSingleMethodException.class);
      thrown.expectMessage("Class " + ServiceMapTest.class.getName()
               + " must have exactly one method named 'unexistant' (found: 0)");
      map.invoke(this, "unexistant");
   }

   @Test
   public void testInvokeWithDuplicateMethod() throws Exception {
      thrown.expect(NoSingleMethodException.class);
      thrown.expectMessage(
               "Class " + ServiceMapTest.class.getName() + " must have exactly one method named 'existing' (found: 2)");
      map.invoke(this, "existing");
   }

   // Test data ----------

   public static class TransientDependent {

      public TransientDependent(Observer observable) {
         // Test method, does nothing
      }
   }

   public static class SingletonDependent {

      public SingletonDependent(Observer observable) {
         // Test method, does nothing
      }
   }

   public static class TwoConstructorsClass {

      public TwoConstructorsClass() {
         // Test method, does nothing
      }

      public TwoConstructorsClass(int i) {
         // Test method, does nothing
      }
   }

   public static String getRegistryToString(Observer observer) {
      return observer.toString();
   }

   public String getObserverToString(Observer observer) {
      return observer.toString();
   }

   public void existing(String s) {
      // Test method, does nothing
   }

   public void existing(int i) {
      // Test method, does nothing
   }
}