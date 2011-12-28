/**
 * Copyright 2011 Tor C Bekkvik
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package glactor.core;

import java.util.concurrent.*;

/**
 * Future reference.
 * Wrapper for FutureTask&lt;V> (returned from actor calls); hides run method.
 * @param <V> return type
 * @see IActorRef
 * @see java.util.concurrent.Future
 * @author torcbek
 */
public class FutureRef<V> implements Future<V>
{
    private ActorRef.FutureTaskA<V> fut;
    
    /**
     * @param fut  FutureTaskA<V> -> FutureTask<V> -> RunnableFuture<V>
     */
    public FutureRef(ActorRef.FutureTaskA<V> fut) {
	this.fut = fut;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
	return fut.cancel(mayInterruptIfRunning);
    }

    public V get() throws InterruptedException, ExecutionException {
	return fut.get();
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException,
	    ExecutionException, TimeoutException {
	return fut.get(timeout, unit);
    }

    public boolean isCancelled() {
	return fut.isCancelled();
    }

    public boolean isDone() {
	return fut.isDone();
    }

    /**
     * Wait for all Future's to finish
     * @param lst list of Future's to wait for
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static <V> void awaitAll(FutureRef<V>... lst)
	    throws InterruptedException, ExecutionException{
	for(Future<V> fr:lst) fr.get();
    }

    /**
     * Wait for at least one Future to finish
     * @param lst list of Future's to wait for
     * @return array index of first Future who finished
     * @throws InterruptedException
     */
    public static <V> int awaitAny(FutureRef<V>... lst)
	    throws InterruptedException {
	//todo ok?
	final CountDownLatch latch = new CountDownLatch(1);
	final int[] res = new int[1];
	for (int i = 0; i < lst.length; i++) {
	    final int ii = i;
	    lst[i].fut.setDoneListener(new ActorRef.ISignal()
	    {
		public void signal() {
		    if (latch.getCount() > 0) {
			res[0] = ii;
			latch.countDown();
		    }
		}
	    });
	    if (lst[i].isDone()) {
		return i;
	    }
	}
	latch.await();
	return res[0];
    }
}
