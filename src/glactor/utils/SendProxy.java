package glactor.utils;

import glactor.core.IActorRef;


/**
 * Send proxy - Send variable of type T and process when received.
 * @author torcbek
 * @param <A> Actor implementation
 * @param <T> Message type
 */
public abstract class SendProxy<A, T>
{
    protected final IActorRef<A> actorRef;

    /**
     * Multiple instances may share the same actor reference
     * @param aRef Actor reference
     */
    public SendProxy(IActorRef<A> aRef) {
	actorRef = aRef;
    }

    /**
     * Act on sent value
     * @param actorImpl actor implementation
     * @param val value
     */
    protected abstract void act(A actorImpl, T val);

    /**
     * Send message (value)
     * @param val value
     */
    public void send(final T val) {
	final SendProxy<A, T> p = this;
	actorRef.send(new MsgSend<A>()
	{
	    @Override
	    public void recv(A actorImpl) {
		p.act(actorImpl, val);
	    }
	});
    }
}

