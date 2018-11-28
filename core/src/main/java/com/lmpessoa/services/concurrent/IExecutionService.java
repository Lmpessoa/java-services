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
package com.lmpessoa.services.concurrent;

import static com.lmpessoa.services.services.Reuse.ALWAYS;

import java.util.Set;
import java.util.concurrent.Future;

import com.lmpessoa.services.services.Service;

/**
 * Represents a service for execution of asynchronous tasks.
 *
 * <p>
 * Through this interface, execution services can be queried about the tasks it has executed and
 * were not yet purged and whether or not the execution service at hand has been shut down. New
 * tasks, however, cannot be submitted through this interface.
 * </p>
 */
@Service(reuse = ALWAYS)
public interface IExecutionService {

   /**
    * Returns an object representing the submitted task with the given ID.
    *
    * <p>
    * The returned result of calling this method represents a task submitted for execution which is
    * associated with the given ID. This object implements the {@see Future} interface and this can
    * be used to retrieve the status and the result of the asynchronous task as well as cancelling
    * it.
    * </p>
    *
    * <p>
    * Note that tasks may be purged from the execution service prior to this call if its retention
    * time expired, thus this method may return {#code null} even if a task with the given ID
    * previously existed.
    * </p>
    *
    * @param jobId the ID of the task to be returned.
    * @return an object representing the result of the task with the given ID.
    */
   Future<?> get(String jobId);

   /**
    * Returns the set of task results retained by the execution service.
    *
    * <p>
    * This set is kept in synchronisation with the execution service and may reflect later updates
    * to it due to being submitted new tasks or purging expired ones.
    * </p>
    *
    * @return the set of task IDs whose results are still retained by the execution service.
    */
   Set<String> keySet();

   /**
    * Returns whether this execution service was requested to shut down.
    *
    * <p>
    * Note that this method will only return true if the shut down process of the execution service
    * has been requested but there is no guarantee with this result that it has effectively
    * terminated processing any pending tasks. Executor service instances may provide additional
    * methods to verify if the shut down has completed.
    * </p>
    *
    * @return {@code true} if the execution service was shut down, {#code false} otherwise.
    */
   boolean isShutdown();
}
