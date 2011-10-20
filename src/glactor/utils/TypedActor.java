package glactor.utils;

import glactor.core.*;
import java.util.concurrent.*;

/**
 * TypedActor<T,U>: Actor wrapper around user defined 'U onReceive(T)' method.
 * @author torcbek
 * @param <T> message type
 * @param <U> return type
 */
public class TypedActor<T, U> extends ActorRef<TypedActor.Impl<T, U>>
{
    public interface Impl<T, U>
    {
	U onReceive(T msg);
    }

    public TypedActor(ActorThreadPool tp, Impl<T, U> impl) {
	super(impl, tp);
    }

    public Future<U> call(final T msg) {
	return super.coreSendFuture(new Callable<U>()
	{
	    public U call() throws Exception {
		return actorImpl.onReceive(msg);
	    }
	});
    }

    public void send(final T msg) {
	super.send(new Runnable()
	{
	    public void run() {
		actorImpl.onReceive(msg);
	    }
	});
    }
}
