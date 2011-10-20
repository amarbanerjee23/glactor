package glactor.examples;

import glactor.core.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import glactor.utils.CallProxy;
import glactor.utils.FutureDirect;
import glactor.utils.FutureSL;
import glactor.utils.MsgSend;
import glactor.utils.SendProxy;

/**
 *
 * @author torcbek
 */
public class ActorExamples
{

    /**
     * Parallel Sort - Baseclass.
     * Serial implementation, overrided in
     * parallell subclasses.
     */
    public static class PSortBase{
	int no;
	/**
	 * Sort base method
	 * @param arr array to sort
	 * @param minLen minimum array length 
	 * @return sorted
	 * @throws Exception
	 */
	public int[] sort(int[] arr, int minLen) throws Exception {
	    no++;
	    int N = arr.length;
	    if (N <= minLen || N < 5) {
		int[] tmp = copy(arr, 0, N);
		Arrays.sort(tmp);
		return tmp;
	    } else {
		int k = N / 2;
		Future<int[]> f1 = call(copy(arr, 0, k), minLen);
		Future<int[]> f2 = call(copy(arr, k, N), minLen);
		return merge(f1.get(), f2.get());
	    }
	}
	/**
	 * Future call
	 * @param arr unsorted array
	 * @param minLen minimum array length for parallel partitioning
	 * @return sorted array 
	 * @throws Exception
	 */
	Future<int[]> call(final int[] arr, final int minLen) throws Exception{
	    return new FutureDirect<int[]>(sort(arr, minLen));
	}
    }
    
    /**
     * Some parallel sort implementations, Future based
     * @throws Exception
     */
    public static void SortRecursiveFutureTst() throws Exception {
	log("\nSortRecursiveFutureTst..");
	int noThr = 4;
	final AtomicInteger msgCount = new AtomicInteger();
	final ActorThreadPool threadP = new ThrPoolBasicImpl(noThr);
	/**
	 * 1- PSortBase: Serial sort 
	 *    or
	 * 2- Sort: Parallel sort, by overriding Future<int[]> fcall..
	 */
	class Sort extends PSortBase {
	    @Override
	    public Future<int[]> call(final int[] arr, final int minLen)
	    {
		msgCount.incrementAndGet();
		IActorRef<Sort> async = threadP.create(new Sort());
		return async.call(new IMessage<Sort, int[]>()
		{
		    public int[] act(Sort impl) throws Exception {
			return impl.sort(arr, minLen);
		    }
		});
	    }
	}
	/**
	 * 3- Or simpler; using stateless Future:
	 *  (Parallell sort is really a too simple
	 *   problem to need actors; no state needs
	 *  to be kept between async calls.)
	 *  
	 * Sort StatLess:
	 */
        class SortSL extends PSortBase {
	    @Override
	    public Future<int[]> call(final int[] arr, final int minLen)
	    {
		return FutureSL.create(threadP, new Callable<int[]>() {
		    public int[] call() throws Exception {
			return sort(arr, minLen);
		    }
		});
	    }
	}
	try {
	    final int num_values = 100000;
//	    final int num_values = 1000000;
	    final int[] values = makeRandomIntArr(num_values);
	    log("PSort test: #values = " + values.length
		    + " #extra_threads = " + noThr);
	    PSortBase[] sorters = {
		new PSortBase(), //serial
		new Sort(),     //actor parallel
		new SortSL()}; //stateless parallel (FutureSL)
	    for (PSortBase s : sorters) {
		s.sort(values, 100); //warm-up; JIT compile
	    }
	    msgCount.set(0);
	    int count = 0;
	    for (int nParts = 1; ++count < 9; nParts *= 2) {
		int minLen = num_values / nParts;
		if (minLen < 10) {
		    break;
		}
		long[] times = new long[3];
		for (int i = 0; i < sorters.length; i++) {
		    long t0 = System.currentTimeMillis();
		    //Sorting..
		    int[] res = sorters[i].sort(values, minLen);
		    times[i] = System.currentTimeMillis() - t0;
		    if (!isAscending(res)){
			log("   -> ERR: arr["+ i + "] not sorted !");
		    }
		}
		log(String.format(" %d nParts: %3d ->"
			+ "  sync:%3dms  actors:%3dms"
			+ "  stateless:%3dms  #messages:%d"
			, count, nParts, times[0], times[1], times[2]
			, msgCount.get()));
		msgCount.set(0);
	    }
	} catch (Exception e) {
	    log(e);
	}
    }

    /**
     * Copy array
     * @param arr array
     * @param i
     * @param j
     * @return subarray arr[i],arr[i+1],... arr[j-1]
     */
    static int[] copy(int[] arr, int i, int j) {
	int[] a = new int[j - i];
	System.arraycopy(arr, i, a, 0, j - i);
	return a;
    }

    static int[] makeRandomIntArr(int len) {
	int[] values = new int[len];
	Random r = new Random();
	for (int i = 0; i < values.length; i++) {
	    values[i] = r.nextInt(values.length);
	}
	return values;
    }

    /**
     * Merge sorted arrays
     * @param a sorted
     * @param b sorted
     * @return merged and sorted from a,b
     */
    static int[] merge(int[] a, int[] b) {
	int A = a.length;
	int B = b.length;
	int[] merged = new int[A + B];
	int i = 0, j = 0, k = 0;
	while (i < A && j < B) {
	    merged[k++] = (a[i] < b[j]) ? a[i++] : b[j++];
	}
	while (i < A) {
	    merged[k++] = a[i++];
	}
	while (j < B) {
	    merged[k++] = b[j++];
	}
//        System.arraycopy(a, i, merged, k, A - i);
//        System.arraycopy(b, j, merged, k + A - i, B - j);
	return merged;
    }

    static boolean isAscending(int[] arr) {
	for (int i = 1; i < arr.length; i++) {
	    if (arr[i - 1] > arr[i]) {
		return false;
	    }
	}
	return true;
    }

    /**
     * Simple (spare) logging
     * @param s
     */
    static void log(Object s) {

	if (s instanceof Exception) {
	    Exception ex = (Exception) s;
	    StackTraceElement se = Thread.currentThread().getStackTrace()[0];
//	    ex.printStackTrace();
	    Logger.getLogger(se.toString()).log(Level.SEVERE, null, ex);
	    return;
	}
	System.out.println(s);
    }

    /**
     * Prime Number generating Actor
     * Chain of (concurrent) actors increases with problem size 
     * while running. (1 actor per prime number)
     */
    public static class PrimeNumActor
    {
	static volatile int actor_count;
	private int msg_count; //#received messages after initialization
	final int my_prime;
	class Impl
	{ //Actor implementation, inner-class pattern (compact)
	    void filterPrime(int number) {
		++msg_count;
		if (number % my_prime != 0) {
		    if (next == null) { //increases concurrency..
			next = new PrimeNumActor(async.getThreadPool(), number);
		    } else {
			next.send(number);
		    }
		}
	    }
	}
	private PrimeNumActor next;
	private final IActorRef<Impl> async;

	public PrimeNumActor(ActorThreadPool tp, int number) {
	    my_prime = number;
	    async = tp.create(new Impl());//fork parallel green-thread
	    log(" prime " + (++actor_count) + " : " + my_prime);
	}

	/**
	 * Async send prime candidate to next filter actor in chain
	 * @param num prime candidate
	 */
	public void send(final int num) {
	    async.send(new MsgSend<Impl>() {
		@Override
		public void recv(Impl impl) {
		    impl.filterPrime(num);
		}});
	}

	/**
	 * 'Recursive' countdown. Sends a countdown message travelling
	 * the full chain of actors via 'next' actor references.
	 * Use before calling msg.latch.await()
	 * @param latch Latch wrapped as message; sent through actor chain.
	 */
	private void countDown(final CountDownLatch latch) {
	    async.send(new Runnable() {
		public void run() {
		    if (next != null) {
			next.countDown(latch);
		    }else{
			latch.countDown();
			//all msgBoxes in chain empty => done
		    }
		}
	    });
	}

	/**
	 * Wait for completion
	 * @throws InterruptedException if current thread was interrupted
	 */
	public void await() throws InterruptedException {
	    CountDownLatch latch = new CountDownLatch(1);
	    countDown(latch);
	    latch.await();
	}

	static void Tst() throws InterruptedException {
	    log("\nPrimeNumber.Tst..");
	    ThrPoolBasicImpl threadP = new ThrPoolBasicImpl(4);
	    PrimeNumActor primeActor = new PrimeNumActor(threadP, 2);
	    int limit = 1047419; //first 81 946 primes
//	    limit = 1020390; //first 80 000 primes
//	    limit = 611953  ;//first 50 000 primes
//	    limit = 224737; //first 20 000 primes
//	    limit = 104729; //first 10 000 primes (35s)
	    limit = 10007; //first 1230 primes (1,6s)
	    limit = 1009; //first 169 primes
//	    limit = 102; //first 25 primes
	    long t0 = System.currentTimeMillis();
	    for (int i = 3; i < limit; i++) {
		primeActor.send(i);
	    }
	    primeActor.await();
//	    log(" -- Prime counters: --");
	    int msgCount = 0, lastPrime = 0, actorCount = 0;
	    PrimeNumActor curr = primeActor;
	    while (curr != null) {
		++actorCount;
		lastPrime = curr.my_prime;
		msgCount += curr.msg_count;
//		log(String.format("  pr.%4d => count %4d", curr.my_prime, curr.msg_count));
		curr = curr.next;
	    }
	    int noSubmit = threadP.getSubmitCount();
	    log("Total #messages: " + msgCount
		    + "  #thread.submits: " + noSubmit
		    + "  #actors: " + actorCount);
	    long dt = System.currentTimeMillis() - t0;
	    log("done, dtime[ms]: " + dt + "   lastPrime: " + lastPrime);
	}
    }


    /**
     * Example using Proxy wrappes around an actor reference
     * @throws Exception
     */
    public static void ActorProxyTst() throws Exception {
	log("\nActorProxyTst..");
	ActorThreadPool threadP = new ThrPoolBasicImpl(4);
	class Impl{int value=0, count;}
	IActorRef<Impl> actor = threadP.create(new Impl());
	SendProxy setProxy = new SendProxy<Impl, Integer>(actor) {
	    @Override
	    protected void act(Impl impl, Integer value) {
		impl.value = value;
	    }
	};
	CallProxy getProxy = new CallProxy<Impl, Integer, String>(actor) {
	    @Override
	    protected String act(Impl impl, Integer value) throws Exception {
		return "" + impl.value;
	    }
	};
	for (int i = 0; i < 64; i = i * 2 + 1) {
	    Future previous = getProxy.call();
	    setProxy.send(i);
	    log(" " + i + " -> " + previous.get());
	}
	actor.awaitMessages(0);
	log("done");
    }


    public static class YourClass
    {
	int val;
	void set(int arg){
	    val = arg;
	}
	double doSomething(int arg){
	    int tmp = val;
	    val = arg * 2;
	    return Math.PI * tmp;
	}
	int get() {
	    return val;
	}
    }

    /**
     * Usage in a nutshell - minimal example
     * @throws Exception
     */
    public static void Usage_in_a_nutshell() throws Exception {
	log("Usage_in_a_nutshell");
	ActorThreadPool actorThreadPool = new ThrPoolBasicImpl(4);
	IActorRef<YourClass> actor = actorThreadPool.create(YourClass.class);
	//getter
	Future<Integer> fut = actor.call(new IMessage<YourClass, Integer>() {
	    public Integer act(YourClass actorImpl) throws Exception {
		return actorImpl.get();
	    }
	});
	final int val = fut.get();
	//only send => set value in actor
	actor.send(new MsgSend<YourClass>() {
	    public void recv(YourClass actorImpl) {
		actorImpl.set(val + 100);
	    }
	});
	//call with sideeffect, returning Future result
	Future<Double> fut1 = actor.call(new IMessage<YourClass, Double>() {
	    public Double act(YourClass actorImpl) throws Exception {
		double v = actorImpl.doSomething(val);
		actorImpl.set(val);
		return v;
	    }
	});
	//atomically; do more in one message: 
	Future<Double> fut2 = actor.call(new IMessage<YourClass, Double>() {
	    public Double act(YourClass actorImpl) throws Exception {
		int val = actorImpl.get();
		actorImpl.set(val + 100);
		return actorImpl.doSomething(val);
	    }
	});
	log(String.format(".. arg:%d  res:%f  res2:%f", val, fut1.get(), fut2.get()));
	log("done");
    }

    public static void main(String[] args) throws Exception {
	Usage_in_a_nutshell();
	ActorProxyTst();
	PrimeNumActor.Tst();
	SortRecursiveFutureTst();

	/*
	 * TODO?
	 *  library:  better message reply utils?
	 *	- transactions?
	 *	- remote/distributed actors?
	 *
	 *  document: Principles?..
	 *	patterns, futures, self reference
	 *	, active/runnable messages, actor protection
	 *	, recursive async calls, wrappers, finals, return messages
	 *	, atomic grouping)
	 *
	 *  demos:   +visual demo?
	 */
    }

}
