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
import java.util.concurrent.atomic.*;
import java.util.logging.*;

/**
 * Basic Actor Threadpool with java Threads
 * @author torcbek
 */
public class ThrPoolBasicImpl extends ActorThreadPool{
    private final int maxThreads;
    private final AtomicInteger noThreads = new AtomicInteger(0);
    private AtomicInteger maxNoThreads = new AtomicInteger();
    private AtomicInteger maxWorkSize= new AtomicInteger(0);

//    private ConcurrentLinkedQueue<Runnable> work = new ConcurrentLinkedQueue<Runnable>();
    private LinkedBlockingDeque<Runnable>
	    work = new LinkedBlockingDeque<Runnable>();
    private static final Logger
	    logger = Logger.getLogger(ThrPoolBasicImpl.class.toString());
    protected int submitCount;

    public ThrPoolBasicImpl(int maxNumThreads) {
        maxThreads = maxNumThreads;
    }

    /**
     * Reset counters
     */
    public void resetStatistics(){
	maxWorkSize.set(0);
	maxNoThreads.set(0);
    }

    private class Worker implements Runnable {
        
        public void run() {
           for(;;) {
                Runnable act = null;
                try {
                    act = work.poll(5, TimeUnit.MILLISECONDS);
                    if (act == null) break;
                    act.run();
                } catch (Exception ex) {
		    logger.log(Level.SEVERE, "Unexpected exception", ex);
		    break;
		}
            }
            int N = noThreads.decrementAndGet();
            if (N < 1){
		noThreads.set(0);
		onFinish();
	    }
        }
    }

    public void submit(Runnable act) {
	++submitCount;
	if (maxThreads == 0){
	    act.run();
	    return;
	}
        work.add(act);
	int sz = work.size();
	while (maxWorkSize.get() < sz) {
	    maxWorkSize.set(sz);
	}
	synchronized (noThreads) {
	    if (noThreads.get() < maxThreads) {
		int n = noThreads.incrementAndGet();
		if (n > maxNoThreads.get()) {
		    maxNoThreads.set(n);
		}
		Thread tr = new Thread(new Worker());
//                    tr.setDaemon(true);
		tr.start();
	    }
	}
    }

    public int getSubmitCount() {
	return submitCount;
    }

    public int getMaxWorkSize(){// max(work.size())
	return maxWorkSize.get();
    }

    public int getMaxNoThreads() {
	return maxNoThreads.get(); //max(noThreads)
    }

    /**
     * Get current #threads active
     * @return #active threads
     */
    public int getNoThreads() { //noThreads < maxThreads

        return noThreads.get();
    }

    public int getThreadLimit() {
        return maxThreads;
    }

    public boolean isAllThreadsBusy(){
        return noThreads.get() >= maxThreads;
    }

    /**
     * A Worker thread finished
     */
    public void onFinish(){
    }

    public void shutdown() {
        work.clear();
        //List<Runnable> shutdownNow = ex.shutdownNow();
    }
}
