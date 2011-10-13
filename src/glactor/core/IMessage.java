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
 * Basic actor message interface.
 * @see IActorRef
 * @author torcbek
 * @param <A> Receiving actor type
 * @param <V> Return value type
 */
public interface IMessage<A, V>
{
    /**
     * Act on received message.
     * Call methods on the actor receiving this message
     * @param actorImpl Receiving actor (direct access)
     * @return result or null
     * @throws Exception
     */
    V act(A actorImpl) throws Exception;
}
