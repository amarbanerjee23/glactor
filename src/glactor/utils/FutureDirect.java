package glactor.utils;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Future with precomputed result (immediately available)
 * @param <V> return value type
 * @author torcbek
 */
public class FutureDirect<V> implements Future<V>
{
    private V val;

    public FutureDirect(V value) {
	val = value;
    }

    public boolean cancel(boolean mayInterrupt) {
	return false;
    }

    public V get() {
	return val;
    }

    public V get(long timeout, TimeUnit unit) {
	return val;
    }

    public boolean isCancelled() {
	return false;
    }

    public boolean isDone() {
	return true;
    }
}
