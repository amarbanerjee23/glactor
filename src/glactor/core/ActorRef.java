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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Actor reference with Future handling. Local implementation (non-remote).
 * Makes asynchronous calls to protected user actor implementation.
 * @author torcbek
 * @param <A> User defined actor implementation
 */
public class ActorRef<A> implements IActorRef<A>
{
    protected final ActorCore<Runnable> core;
    protected final A actorImpl;
//    private CountDownLatch emptyLatch = new CountDownLatch(1);
    static final Logger logger = Logger.getLogger(ActorRef.class.toString());

    /**
     *
     * @param cz Aactor implementation class
     * @param threadP thread environment
     * @throws Exception
     */
    public ActorRef(Class cz, ActorThreadPool threadP) throws Exception {
	this((A) cz.newInstance(), threadP);
    }
    
    /**
     * 
     * @param impl Actor user implementation
     * @param env Actor thread environment
     */
    public ActorRef(A impl, ActorThreadPool env) {
	this.actorImpl = impl;
	core = new ActorCore<Runnable>(env)
	{
	    @Override
	    protected void receive(Runnable task)  {
		synchronized (actorImpl)
		{
		    task.run();
		}
	    }

//	    @Override
//	    protected void onEmpty() {
//		emptyLatch.countDown();
//	    }
	    
//	    @Override
//	    protected void onException(Exception e) {
//		handleException(e);
//	    }
	};
    }

    public Class getImplClass(){
	return actorImpl.getClass();
    }

    protected void handleException(Exception e) {
	if (actorImpl instanceof IExceptHandler){
	    ((IExceptHandler)actorImpl).handleException(e);
	}else{
	    logger.log(Level.ALL, "", e);
	}
    }

    /**
     * Wait until actors message queue becomes empty.
     * (experimental.  todo? what if N threads?)
     * @throws InterruptedException
     */
//    public synchronized void awaitDone() throws InterruptedException {
//	emptyLatch.await();
////	if (emptyLatch.getCount() < 1)
//	    emptyLatch = new CountDownLatch(1); //todo??
//    }

    /**
     * Wait for current pending messages to be consumed
     */
    public boolean awaitMessages() throws InterruptedException
    {
	return awaitMessages(0);
    }
    
    public boolean awaitMessages(long timeout) throws InterruptedException
    {
	final CountDownLatch latch = new CountDownLatch(1);
	core.send(new Runnable()
	{
	    public void run() {
		latch.countDown();
	    }
	});
	if (timeout > 0)
	    return latch.await(timeout, TimeUnit.MILLISECONDS);
	latch.await();
	return true;
    }

    public ActorThreadPool getThreadPool(){
	return core.threadPool;
    }

    /**
     * Internal Future signal interface (Used in FutureRef.awaitAny)
     */
    protected interface ISignal
    {
	/**
	 * Called from ActorRef.FutureTaskA.done().
	 * Signals that future value is ready.
	 */
	void signal();
    }

    /**
     * Extended FutureTask.
     * With listener and threadlock avoidance (may call ActorCore.runCore()).
     * @param <V> Future return type
     * @see java.util.concurrent.FutureTask
     */
    public static class FutureTaskA<V> extends FutureTask<V>
    {
	private ActorCore core;
	private ISignal listener;
	public FutureTaskA(Callable<V> callable, ActorCore core) {
	    super(callable);
	    this.core = core;
	}

	/**
	 * Get (await) future result. 
	 * Tries to do useful work while waiting to avoid possible 'threadlock'
	 * @return result
	 * @throws InterruptedException
	 * @throws ExecutionException 
	 */
	@Override
	public V get() throws InterruptedException, ExecutionException {
	    while (!super.isDone() && core.threadPool.isAllThreadsBusy()
		    && core.runCore()) {
	    }
	    return super.get();
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException,
		ExecutionException, TimeoutException {
	    long tmax = System.currentTimeMillis() + unit.toMillis(timeout);
	    while (!super.isDone() && core.threadPool.isAllThreadsBusy()
		    && core.runCore()) {
		if (System.currentTimeMillis() > tmax) {
		    return super.get(0, TimeUnit.MILLISECONDS);
		}
	    }
	    return super.get(timeout, unit);
	}

	public void setDoneListener(ISignal call){
	    listener = call;
	}
	
	@Override
	protected void done() {
	    listener.signal();
	}
    }

    public void send(Runnable msg) {
	core.send(msg);
    }

    /**
     * Send message to this actor. 
     * Exceptions are handled in ((IExceptHandler)actorImpl)
     * .handleException(e) if implemented.
     * @param msg message. (Cannot be a Runnable)
     */
    public void send(final IMessage<A, ?> msg) {
	if (msg == null) {
            throw new NullPointerException("ICall message == null");
        }
	core.send(new Runnable()
	{
	    public void run() {
		try { 
		    synchronized(msg) {
			msg.act(actorImpl);
		    }
		} catch (Exception ex) {
		    handleException(ex);
		}
	    }
	});
    }

    public <V> FutureRef<V> call(final IMessage<A, V> msg) {
	if (msg == null) {
            throw new NullPointerException("ICall message == null");
        }
	return coreSendFuture(new Callable<V>(){
	    public V call() throws Exception {
		synchronized (msg)
		{
		    return msg.act(actorImpl);
		}
	    }
	});
    }

    /**
     * Send callable as Future task to actor core.
     * @param <V> future type
     * @param call callable to process
     * @return Future
     */
    protected <V> FutureRef<V> coreSendFuture(Callable<V> call) {
	FutureTaskA<V> fMsg = new FutureTaskA<V>(call, core);
	core.send(fMsg);
	return new FutureRef(fMsg);
    }

}
