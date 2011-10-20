package glactor.utils;

import glactor.core.*;

/**
 * Self referring actor baseclass
 * @author torcbek
 * @param <A> actor implementation
 */
public abstract class ActorSelf<A> implements IActSelf<A>
{
    protected IActorRef<A> selfRef;

    public void initSelf(IActorRef<A> aRef) {
	selfRef = (IActorRef<A>) aRef;
    }

    /**
     * Get my self reference
     * @return
     */
    public IActorRef<A> self() {
	return selfRef;
    }

    /**
     * Send message to destination actor from this actor.
     * If message is instance of MsgCallReply, sets response adresss to this actor.
     * @param <V> response datatype (if MsgCallReply)
     * @param dest destination actor
     * @param msg message
     */
    public <V> void sendTo(final IActorRef dest, final IMessage<A, V> msg) {
	if (msg instanceof MsgCallReply) {
	    ((MsgCallReply) msg).setCaller(selfRef);
	}
	dest.send(msg);
    }

}