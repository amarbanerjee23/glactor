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
 * Actor Exception handler.
 * @author torcbek
 */
public interface IExceptHandler
{
    /**
     * Handle exception.
     * Called from ActorRef.send() if Exception is thrown
     * and its actor class implements this interface.
     * @see ActorRef
     * @param e Exception
     */
    void handleException(Exception e);
}
