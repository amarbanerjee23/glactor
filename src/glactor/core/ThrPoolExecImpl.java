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


/**
 * Actor Threadpool with java.util.concurrent.Executors
 * @author torcbek
 */
public class ThrPoolExecImpl extends ActorThreadPool{
    final int maxThreads;
    private AtomicInteger noThreads = new AtomicInteger(0);
    private AtomicInteger noTasks = new AtomicInteger(0);
    private ExecutorService ex;

    public ThrPoolExecImpl(int maxNumThreads) {
        maxThreads = maxNumThreads;
	ex = Executors.newCachedThreadPool();
//        if (maxNumThreads < 1) {
//            ex = Executors.newCachedThreadPool();
//        } else {
//            ex = Executors.newFixedThreadPool(maxNumThreads);
//        }
    }

    public int getNoTasks() {
        return noTasks.get();
    }

    public int getMaxActiveThreads() {
        return maxThreads;
    }

    public int getNoActiveThreads() {
        return noThreads.get();
    }

    public boolean isAllThreadsBusy(){
        return noThreads.get() >= maxThreads;
    }

    public void shutdown() {
        ex.shutdown();
    }

    public void submit(final Runnable act) {
        noTasks.incrementAndGet();
	if (maxThreads == 0){
	    act.run();
	    noTasks.decrementAndGet();
	    return;
	}
        Runnable wr = new Runnable(){
            public void run() {
                noThreads.incrementAndGet();
                act.run();
                noThreads.decrementAndGet();
                noTasks.decrementAndGet();
            }
        };
        ex.submit(wr);
    }
}
