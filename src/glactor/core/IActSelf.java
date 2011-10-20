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
 * Self referring actor.
 * If implemented by class A, initSelf(..) will
 * be called from ActorThreadPool.create(A impl)->IActorRef<A>
 * (at actor creation time)
 * <pre>
 * Usage:
 *  class MyActorImpl implements IActorSelf<MyActorImpl>{...}
 *  IActorRef&lt;A> actor = actorThreadPool.create(new MyActorImpl(..))
 * </pre>
 * @param <A> actor implementation class,
 * @see ActorThreadPool
 * @author torcbek
 */
public interface IActSelf<A>
{
    /**
     * Initiate self-reference
     * @param self my actor reference
     */
    void initSelf(IActorRef<A> self);
}
