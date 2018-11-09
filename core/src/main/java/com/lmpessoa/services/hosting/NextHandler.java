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
package com.lmpessoa.services.hosting;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Wraps the call to the next handler in the application.
 *
 * <p>
 * A proxy to the next handler is created automatically by the application for each handler class
 * registered. This proxy ensures each handler is instantiated only when required (when a the
 * previous handler explicitly {@link #invoke}s the next handler).
 * </p>
 *
 * <p>
 * The proxy can only be invoked once per request.
 * </p>
 */
public final class NextHandler {

   private final HandlerMediator mediator;
   private final int index;
   private boolean invoked = false;

   NextHandler(HandlerMediator mediator, int index) {
      this.mediator = mediator;
      this.index = index;
   }

   /**
    * Calls the next handler registered in the application.
    * <p>
    * New handlers must be aware that they are expected to return a result of their execution aftr
    * completion. Implementors of new handlers may receive that result from the return of calling this
    * function.
    * </p>
    *
    * @return the result returned by the next handler.
    */
   public Object invoke() {
      if (invoked) {
         throw new IllegalStateException("Next handler has already been called");
      }
      invoked = true;
      Class<?> handlerClass = mediator.getHandler(index);
      if (handlerClass == null) {
         return null;
      }
      NextHandler next = new NextHandler(mediator, index + 1);
      try {
         Constructor<?> constructor = handlerClass.getConstructor(NextHandler.class);
         constructor.setAccessible(true);
         Object handler = constructor.newInstance(next);
         return mediator.getServices().invoke(handler, "invoke");
      } catch (InvocationTargetException e) {
         if (e.getCause() instanceof HttpException) {
            throw (HttpException) e.getCause();
         }
         if (e.getCause() instanceof InternalServerError) {
            throw (InternalServerError) e.getCause();
         }
         throw new InternalServerError(e.getCause());
      } catch (Exception e) {
         throw new InternalServerError(e);
      }
   }
}
