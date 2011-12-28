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



/**
 * Thread pool for actor scheduling
 * @author torcbek
 */
public abstract class ActorThreadPool
{
    /**
     * Instantiate
     */
    public ActorThreadPool() {}

    /**
     * Get default actor threadpool implementation
     * @param maxNoThr max nunber of threads
     * @return Actor ThreadPool
     */
    public static ActorThreadPool getDefault(int maxNoThr)
    {
	return new ThrPoolBasicImpl(maxNoThr);
    }

    /**
     * Submit actor task to this thread pool.
     * @param task Runnable (usually actor with pending messages)
     */
    public abstract void submit(Runnable task);

    /**
     * Try to shut down this thread pool.
     */
    public abstract void shutdown(); //todo?

    /**
     * Check if all threads are busy
     * @return true = all threads busy
     */
    public abstract boolean isAllThreadsBusy();

    /**
     * Create actor from implementation class.
     * @param <A> actor implementation class
     * @param cz class
     * @return Actor proxy for instantiated actor implementation
     * @throws InstantiationException if newInstance failed
     * @throws IllegalAccessException 
     */
    public <A> IActorRef<A> create(Class<A> cz)
	throws InstantiationException, IllegalAccessException {
	return create((A)cz.newInstance());
    }

    /**
     * Create actor from implementation instance
     * @param <A> Any class
     * @param actorImpl Actor implementation instance
     * @return new actor reference/proxy
     */
    public <A> IActorRef<A> create(A actorImpl) {
	ActorRef<A> self = new ActorRef<A>(actorImpl, this);
	if (actorImpl instanceof IActSelf){
	    ((IActSelf<A>)actorImpl).initSelf(self);
	}
        return self;
    }

}
