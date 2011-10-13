package glactor.utils;

import glactor.core.IMessage;

/**
 * Message, send only (One-way message; dont return future value)
 * @author torcbek
 * @param <A> Receiving actor implementation
 */
public abstract class MsgSend<A> implements IMessage<A, Object>
{
    /**
     * Receive action;
     * Call methods on the actor receiving this message
     * @param actorImpl Receiving actor (direct access)
     * @throws Exception
     */
    public abstract void recv(A actorImpl) throws Exception;

    public Object act(A actorImpl) throws Exception {
	recv(actorImpl);
	return null; //no value, but return event can have meaning
    }

}
