package glactor.utils;

import glactor.core.IActorRef;
import glactor.core.FutureRef;
import glactor.core.IMessage;


/**
 * Generic actor call proxy
 * @author torcbek
 * @param <A> Actor implementation class (serial)
 * @param <T> Call argument type
 * @param <U> Return type
 */
public abstract class CallProxy<A, T, U>
{
    protected final IActorRef<A> actorRef;

    /**
     * Multiple instances may share the same actor reference
     * @param aRef Actor reference
     */
    public CallProxy(IActorRef<A> aRef) {
	actorRef = aRef;
    }

    /**
     * Act on sent value
     * @param actorImpl actor implementation
     * @param val value
     * @return result
     * @throws Exception
     */
    protected abstract U act(A actorImpl, T val) throws Exception;

    /**
     * Async call.
     * @param val value
     * @return Future result
     */
    public FutureRef<U> call(final T val) {
	final CallProxy<A, T, U> p = this;
	return actorRef.call(new IMessage<A, U>()
	{
	    public U act(A actorImpl) throws Exception {
		return p.act(actorImpl, val);
	    }
	});
    }

    /**
     * Async call. Sends 'null' message to this actor
     * @return Future result
     */
    public FutureRef<U> call() {
	return call(null);
    }
}
