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
package com.lmpessoa.services.core.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import com.lmpessoa.services.util.logging.ILogger;
import com.lmpessoa.services.util.logging.Logger;
import com.lmpessoa.services.util.logging.NullLogWriter;

public class ExecutionServiceTest {

   private static final ILogger log = new Logger(ExecutionServiceTest.class, new NullLogWriter());
   private ExecutionService service;

   @Before
   public void setup() {
      service = new ExecutionService(1, log);
   }

   @Test
   public void testSingleJob() throws InterruptedException, ExecutionException {
      String id = service.submit(() -> "success", "test");
      Future<?> result = service.get(id);
      assertEquals("success", result.get());
      assertTrue(result.isDone());
   }

   @Test
   public void testMultipleJobs() throws InterruptedException, ExecutionException {
      List<String> results = new ArrayList<>();
      for (int i = 0; i < 20; ++i) {
         final int j = i;
         results.add(service.submit(() -> j));
      }
      for (int i = 0; i < 20; ++i) {
         Future<?> result = service.get(results.get(i));
         assertEquals(i, result.get());
         assertTrue(result.isDone());
      }
   }

   @Test(expected = ExecutionException.class)
   public void testException() throws InterruptedException, ExecutionException {
      String id = service.submit(() -> {
         throw new NullPointerException();
      });
      Future<?> result = service.get(id);
      result.get();
   }

   @Test(expected = RejectedExecutionException.class)
   public void testAddAfterShutdown() {
      assertFalse(service.isShutdown());
      service.shutdown();
      assertTrue(service.isShutdown());
      service.submit(() -> "success");
   }

   @Test
   public void testCancelFinishedJob() throws InterruptedException, ExecutionException {
      String id = service.submit(() -> "success");
      Future<?> result = service.get(id);
      assertEquals("success", result.get());
      assertFalse(result.cancel(true));
      assertFalse(result.isCancelled());
   }

   @Test(expected = InterruptedException.class)
   public void testInterruptExecution() throws InterruptedException, ExecutionException {
      String id = service.submit(() -> {
         Thread.sleep(2000);
         return "finished";
      });
      Future<?> result = service.get(id);
      Thread.sleep(100);
      result.cancel(true);
      result.get();
   }

   @Test(expected = CancellationException.class)
   public void testCancelBeforeExecution() throws InterruptedException, ExecutionException {
      service.submit(() -> {
         Thread.sleep(2000);
         return "finished";
      });
      String id = service.submit(() -> "success");
      Future<?> result = service.get(id);
      result.cancel(false);
      result.get();
   }

   @Test(expected = TimeoutException.class)
   public void testTimeoutWaitingResult()
      throws InterruptedException, ExecutionException, TimeoutException {
      String id = service.submit(() -> {
         Thread.sleep(2000);
         return "finished";
      });
      Future<?> result = service.get(id);
      result.get(1, TimeUnit.MILLISECONDS);
   }
}
