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

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight generic Actor base. One instance represents
 * one actor.
 * May be used directly to implement absolute minimal actors,
 * but lacks other useful properties such as Future handling
 * and better protection. More ideal as a basis
 * for higher abstraction level actor frameworks.
 * <p>
 * Fast(zero-copy), thread safe scheduling to process
 * messages received from other threads via send(T message). 
 * All received messages are eventually processed, unless
 * actor is stopped with messages remaining.
 * <p>
 * Locally, all processing is single-threaded, but since multiple 
 * actors can run simultaneously (as actors do in general), high
 * concurrency is easily achieved.
 * <p>
 * This is a critical piece, so proving correctness shoud be kept easy;
 * avoid changing this implementation or keep it clean and simple
 * to help reliability. Add extra functionality in subclasses
 * or other classes, not here.
 *
 * @author torcbek
 * @param <T> Message type
 */
public abstract class ActorCore<T> implements Runnable
{
    protected final LinkedList<T> msgBox = new LinkedList<T>();
    protected AtomicBoolean isScheduled = new AtomicBoolean();
    protected AtomicBoolean isRunningCore = new AtomicBoolean();
    protected ActorThreadPool threadPool;

    /* --------------------------------
     * Repeating cycle:
     *
     * --> send(m):
     *	    mQ += m
     *	    if active(F->T)
     *		threadPool.submit(this)
     * <-- (external thread
     * threadPool --> run:
     *		runCore :mQ--
     *		active(F)
     *		if mQ>0
     *		    if active(F->T)
     *			threadPool.submit(this)
     * threadPool <--
     *
     * --------------------------------
     */

    public ActorCore(ActorThreadPool tp) {
	threadPool = tp;
    }

    /**
     * Send a message to this actor
     * @param msg Message to be procesed later (null allowed).
     * see receive(T msg)
     */
    public void send(T msg) {//N-threaded
	synchronized (msgBox) {
	    msgBox.add(msg);
	}
	schedule();
    }

    /**
     * Sschedule = submit this actor to threadpool.
     * Returns without blocking if already scheduled.
     */
    private void schedule() {
	if (isScheduled.compareAndSet(false, true)) {
	    onSubmit();
	    threadPool.submit(this); //=> ready
	}
    }

    /**
     * Run (runs core to process messages), and reschedules if messages remain.
     * Execution is 1-treaded: Only call from my own "actor thread"
     * , ie. from threadPool as a consequence of earlier
     * call to threadPool.submit(this).
     */
    public void run() {
	runCore();
	isScheduled.set(false);
	onRelease();
	synchronized (msgBox) {
	    if (msgBox.size() > 0) {
		schedule();
	    } else { //N->0 messages
		onEmpty();
	    }
	}
    }

    /*
     * Protected methods, default implementations does nothing.  
     * Subclasses may override to invoke completion callbacks or
     * perform bookkeeping (but avoid time consuming implementations!)
     */

    /**
     * This actor was just submitted to threadpool, ready to run
     * At least 1 message to process
     */
    protected void onSubmit() {
    }

    /**
     * Releasing thread
     */
    protected void onRelease() {
    }

    /**
     * My message queue became empty.
     */
    protected void onEmpty() {
    }

    /**
     * Exception was thrown
     * @param e 
     */
    protected void onException(Exception e) {
    }

    /**
     * Run core if not already runnin and process messages.
     * <p>
     * May be called from an external thread attempting to solve
     * 'threadlock' problem where limited #threads
     * are available, and all are waiting on 'Future' locks.
     * <p>
     * A thread waiting on a Future (response to a message)
     * on this actorCore, can avoid being caught in a threadlock
     * by calling runCore() instead of just waiting. (liveness)
     * <p>
     * Single-threaded, non-blocking performance achieved
     * with 'AtomicBoolean isRunning' flag.
     * 
     * @return true if accepted & executed (if not already 'isRunning')
     */
    public boolean runCore() {
	if (isRunningCore.compareAndSet(false, true)) {
	    try {
		int N;
		synchronized (msgBox) {
		    N = msgBox.size();
		}
		T msg;
		while (N-- > 0) {
		    synchronized (msgBox) {
			msg = msgBox.poll();
		    }
		    try {
			receive(msg);
		    } catch (Exception e) {
			onException(e);
		    }
		}
	    } finally {
		isRunningCore.set(false);
	    }
	    return true;
	}
	return false;
    }

    /**
     * Process a message sent to this actor.
     * User defined.
     * @param msg Message received
     * @throws Exception
     */
    protected abstract void receive(T msg) throws Exception;//1-threaded!
}
