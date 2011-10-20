package glactor.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import glactor.core.ActorThreadPool;

/**
 * FutureTask suitable as stateless actors
 * @param <V> return type
 * @author torcbek
 */
//public abstract class FutureSL<V> implements Callable<V>, Future<V>
public class FutureSL<V> extends FutureTask<V>
{
    protected ActorThreadPool threadP;

    private FutureSL(Callable<V> callable) {
	super(callable);
    }

    /**
     * Create a FutureSL (submitted to actor threadpool)
     * @param <V> future return type
     * @param threadP actor threadpool
     * @param callable
     * @return Future result
     */
    public static <V> FutureSL<V> create(
	    ActorThreadPool threadP, Callable<V> callable) {
	FutureSL fut = new FutureSL<V>(callable);
	fut.threadP = threadP;
	threadP.submit(fut);
	return fut;
    }

    /**
     * Get Future result
     * @return result
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Override
    public V get() throws InterruptedException, ExecutionException {
	if (!isDone() && threadP.isAllThreadsBusy()) {
	    run();
	}
	return super.get();
    }

    /*
    protected RunnableFuture <V> future;
    public FutureSL(ActorThreadPool atp) {
	threadPool = atp;
	future = new FutureTask<V>(this);
	atp.submit(future);
    }
    public boolean cancel(boolean mayInterruptIfRunning) {
	return future.cancel(mayInterruptIfRunning);
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException,
	    ExecutionException, TimeoutException {
	return future.get(timeout, unit);
    }

    public boolean isCancelled() {
	return future.isCancelled();
    }

    public boolean isDone() {
	return future.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
	if (!future.isDone() && threadPool.isAllThreadsBusy()) {
	    future.run();
	}
	return future.get();
    }*/

}