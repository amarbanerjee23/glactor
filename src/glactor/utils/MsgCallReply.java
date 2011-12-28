package glactor.utils;

import glactor.core.IActorRef;
import glactor.core.IMessage;

/**
 * Actor call/reply message
 * @param <A> actor class
 * @param <V> return type
 * @author torcbek
 */
public abstract class MsgCallReply<A,V> implements IMessage<A, V>
{
    private IActorRef<A> caller;

    public MsgCallReply(IActorRef<A> caller) {
	this.caller = caller;
    }

    public MsgCallReply() {
    }

    /**
     * Set caller actor reference (for response messages)
     * @param caller
     */
    public void setCaller(IActorRef<A> caller) {
	this.caller = caller;
    }

    public V act(A actorImpl) throws Exception
    {
	final V result = call(actorImpl);
	if (caller == null) {
	    reply(result); //no caller actor thread => please synchronize.
	    return result;
	}
	final MsgCallReply<A, V> msg = this;
	caller.send(new Runnable()
	{ //return result as message back to originating caller
	    public void run() {
		synchronized (msg) {
		    msg.reply(result);
		}
	    }
	});
	return result;
    }

    /**
     * Call action - done at destination actor
     * @param actorImpl destination actor
     * @return result
     * @throws Exception
     */
    public abstract V call(A actorImpl) throws Exception;

    /**
     * Reply action - done at source actor
     * @param msg result returned from call action
     */
    public abstract void reply(V msg);

}
