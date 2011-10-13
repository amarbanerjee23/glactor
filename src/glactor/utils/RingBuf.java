package glactor.utils;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Threadsafe ringbuffer, linked list implementation.
 * Upon insertion, oldest elements are removed
 * while size > maxSize.
 *
 * (Usage example: Debug a concurrent program
 *  by continuously storing the last N events)
 * @author torcbek
 * @param <T> element type.
 */
public class RingBuf<T>
{
//	private Queue<T> lst = new LinkedList<T>();
//	private Queue<T> lst = new LinkedBlockingQueue<T>();
    private Queue<T> lst = new ConcurrentLinkedQueue<T>();
    private AtomicBoolean a = new AtomicBoolean();
    public final int maxSize; //max ringbuffer size

    /**
     *
     * @param size max ringbuffer size
     */
    public RingBuf(int size) {
	maxSize = size;
    }

    /**
     * Add element to ringbuffer
     * @param elem element to add
     */
    public void add(T elem) {
	lst.add(elem);
	if (a.compareAndSet(false, true)) {
	    while (lst.size() > maxSize) {
		lst.remove();
	    }
	    a.set(false);
	}
    }

    /**
     * Get ringbuffer iterator
     * @return iterator
     */
    public Iterator<T> iterator() {
	return lst.iterator();
    }
}
