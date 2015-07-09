**A lightweight actor library based on generics**

**Keypoints:**

Encapsule plain java classes/objects into generic actors. Fast zero-copy messaging. Futures. No bytecode manipulation. Green-threads(scale to many actors). (Concurrency may also be disabled: 'ActorThreadPool.maxNoThr=0').

```
  //Essential interfaces:
    interface IActorRef<A> {  <V> FutureRef<V> call(IMessage<A, V> msg);  }
    interface IMessage<A, V> {  V act(A actorImpl);  }

  //Minimum example:
    class YourClass {float f(int arg){..}}
    IActorRef<YourClass> actor = actorThreadPool.create(YourClass.class);
    final int arg = 32;

    Future<Float> fut = actor.call(new IMessage<YourClass, Float>() {
	    public Float act(YourClass actorImpl) throws Exception {
		    return actorImpl.f(arg);
	    }
    });
```


**Features:**

  * Lightweight actor library. Using generics and java.util.concurrent (dont reinvent that wheel). Number of concepts reduced to a minimum, while still being universal. Easy to learn. Small reusable kernel.

  * Fast zero-copy messaging. Green threads(M:N mapping) -> scales to high number of actors.

  * Full method access via messages. A single message may atomically do multiple method calls on receiving actor.

  * Futures with 'threadlock' prevention. (not always solved in other actor frameworks when combining many actors with few threads)

  * Robust threading. Keeps actor implementation protected from concurrent access.(As long as you avoid 'leaking' shared mutable access via messages) Implicit thread activation. Efficient use of available thread resources. Concurrency may be turned fully off.

  * No bytecode manipulation. Could otherwise interfere with other libraries doing bytecode manipulation. Easily ported to other OO-languages with generics (also because it is lightweight)

  * Inheritance agnostic: Wrap existing classes into actor objects independent of class hierarchy. No need to inherit from special actor baseclasses.


**NB.** This project is no longer maintained.
([NBL\_Actors](https://github.com/tcbkkvik/NBL_Actors) is a newer alternative)