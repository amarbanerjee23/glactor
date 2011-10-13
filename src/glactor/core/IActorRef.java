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
 * Actor reference (proxy) for implementation class A
 * Instances are normally created with ActorThreadPool.create(..)
 * A IActorRef can be shared between multiple threads or actors.
 *
  <pre>
  Features:
    1.Lightweight actor library
	    Using generics and java.util.concurrent (dont reinvent that wheel).
	    Number of concepts reduced to a minimum, while still being universal.
	    Easy to learn. Small reusable kernel.
    2.Fast zero-copy messaging.
	    Green threads(M:N mapping) -> scales to high number of actors.
    3.Full method access via messages (IMessage callback)
    4.Futures with 'threadlock' prevention.
    5.Robust threading
	    Keeps actor implementation protected from concurrent access.
	    (As long as you avoid 'leaking' shared mutable access via messages)
	    Implicit thread activation. Efficient use of available thread resources.
	    Concurrency may be turned fully off (ActorThreadPool.maxThreads=0)
    6.No bytecode manipulation.
	    Could otherwise interfere with other libraries doing bytecode manipulation.
	    Easily ported to other OO-languages with generics (also because it is lightweight)
    7.Inheritance agnostic: Wrap existing classes into actor objects independent of class hierarchy.
    No need to inherit from special actor baseclasses.

  Essential interfaces:
    interface IActorRef<A> {  <V> FutureRef<V> call(IMessage<A, V> msg);  }
    interface IMessage<A, V> {  V act(A actorImpl);  }

  Usage in a nutshell:
    class YourClass {float f(int arg){..}}
    IActorRef<YourClass> actor = actorThreadPool.create(YourClass.class);
    final int arg = 32;

    Future<Float> fut = actor.call(new IMessage<YourClass, Float>() {
	    public Float act(YourClass actorImpl) throws Exception {
		    return actorImpl.f(arg);
	    }
    });
  </pre>
 * 
 * @param <A> Actor implementation (plain java class)
 * @see ActorThreadPool
 * @author torcbek
 */
public interface IActorRef<A>
{
    /**
     * Send a Runnable message to this actor.
     * Can be used to trigger other events at message activation time, ie.
     * when the message is read from the message queue at receiving actor.
     * (low level message type, also used internally)
     * @param msg message
     */
     void send(Runnable msg);

    /**
     * Send a message to this actor.
     * @param msg message. (Cannot be a Runnable)
     */
     void send(final IMessage<A, ?> msg);

    /**
     * Call this actor - returns a future value
     * (The most general message method)
     * @param <V> future return type
     * @param msg message (Cannot be a Runnable)
     * @return Future value
     */
     <V> FutureRef<V> call(final IMessage<A, V> msg);

    /**
     * Wait for current pending messages to be consumed
     * @param timeout max wait time in milliseconds if >0 (infinity if <=0)
     * @return true if current messages was consumed
     * @throws InterruptedException if wait was interrupted
     */
    boolean awaitMessages(long timeout) throws InterruptedException;
     
    /**
     * Get my actor threadpool
     * @return threadpool
     */
    ActorThreadPool getThreadPool();
    
    /**
     * Get my actor implementation class
     * @return class A
     */
    Class getImplClass();

}
