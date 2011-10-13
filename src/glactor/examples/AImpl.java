package glactor.examples;

import java.util.concurrent.*;
import glactor.core.*;


public class AImpl
{
    int x;

    public static void main(String[] args) throws Exception
    {
	ActorThreadPool tp =  ActorThreadPool.getDefault(3);
	IActorRef<AImpl> actor = tp.create(AImpl.class);
	actor.send(new IMessage<AImpl, Object>() {
	    @Override
	    public Object act(AImpl ai) throws Exception {
		ai.x = 34234;
		return null;
	    }
	});
	Future<Object> fut = actor.call(new IMessage<AImpl, Object>() {
	    @Override
	    public Object act(AImpl ai) throws Exception {
		return ai.x;
	    }
	});
	int val = (Integer) fut.get();
	System.out.println("val " + val);
    }

}
