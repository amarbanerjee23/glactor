package glactor.core;

import glactor.core.ThrPoolBasicImpl;
import glactor.core.ActorCore;
import glactor.core.ActorThreadPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import glactor.utils.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.After.*;
import static org.junit.Assert.*;

/**
 *
 * @author torcbek
 */
public class ActorCoreTest
{
    public ActorCoreTest() {
    }
/*
    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }*/

    public static class ActorCoreImpl extends ActorCore
    {
	AtomicBoolean active = new AtomicBoolean();

	public ActorCoreImpl(ActorThreadPool threadP) {
	    super(threadP);
	}
	Object lastMsg;
	volatile int receiveCount;
	volatile int lastMsgNo;

	public void receive(Object msg) {
	    ++receiveCount;
	    lastMsg = msg;
	    if (active.compareAndSet(false, true)) {
		if (msg instanceof Integer) {
		    int no = (Integer) msg;
		    assertEquals(no, lastMsgNo + 1); //"n+1 expected (FIFO)"
		    lastMsgNo = no;
		} else if (msg instanceof Runnable) {
		    ((Runnable) msg).run();
		}
		//should just return; already in runCore()
		assertFalse(runCore());
		active.set(false);
	    } else {
		++exceptCount;
		fail("Concurrent access not allowed here");
	    }
	}

	@Override
	public String toString() {
	    return "ActorCoreImpl{" + "lastMsg=" + lastMsg 
		    + ", receiveCount=" + receiveCount
		    + ", lastMsgNo=" + lastMsgNo + ", nEmpty=" + nEmpty
		    + ", nSubmit=" + nSubmit
		    + ", exceptCount=" + exceptCount + '}';
	}
	volatile int nEmpty;
	CountDownLatch emptyLatch;

	@Override
	protected void onEmpty() {
	    ++nEmpty;
	    if (emptyLatch != null) {
		emptyLatch.countDown();
	    }
	}
	volatile int nSubmit;
	CountDownLatch submitLatch;

	@Override
	protected void onSubmit() {
	    ++nSubmit;
	    if (submitLatch != null) {
		submitLatch.countDown();
	    }
	}
	long release_ms;

	@Override
	protected void onRelease() {
	    if (release_ms > 0) {
		try {
		    Thread.sleep(release_ms);
		} catch (InterruptedException ex) {
		    Logger.getLogger(ActorCoreTest.class.getName()).
			    log(Level.WARNING, null, ex);
		}
	    }
	}
	volatile int exceptCount;

	@Override
	protected void onException(Exception e) {
	    ++exceptCount;
	    fail("onException");
//	    e.printStackTrace();
	}

    }

    static void tstSub(int nThr) throws Exception {

	ActorThreadPool threadP = new ThrPoolBasicImpl(nThr);
	final ActorCoreImpl core = new ActorCoreImpl(threadP);
	int no = 0;
	System.out.println("\n------------------------");
	System.out.println("Test, #Threads = " + nThr);
	System.out.println("------------------------");
	/* --------------------------------
	 * CYCLE
	 * Assert this repeated sequence..
	 * --------------------------------
	 *
	 * --> send(m):
	 *	    mQ += m
	 *	    if active(F->T)
	 *		threadPool.submit(this)
	 * <-- (external thread
	 * threadPool --> run:
	 *		runCore :mQ--
	 *		active(F)
	 *		if mQ>0
	 *		    if active(F->T)
	 *			threadPool.submit(this)
	 * threadPool <--
	 *
	 * --------------------------------
	 */

	//test 1
	for (int i = 0; i < 3; i++) {
	    core.emptyLatch = new CountDownLatch(1);
	    core.send(++no);
	    core.emptyLatch.await();
	}
	System.out.println("->Subtest 1; " + core);
	assertEquals(core.exceptCount, 0);
	assertEquals(core.lastMsgNo, no);
	assertTrue(core.lastMsg != null);

	//test 2
	core.emptyLatch = new CountDownLatch(1);
	final int no_1 = ++no;
	core.send(new Runnable()
	{
	    public void run() {
		core.send(no_1);
	    }
	});
	core.emptyLatch.await();
	System.out.println("->Subtest 2; " + core);
	assertEquals(core.exceptCount, 0);
	assertEquals(core.lastMsgNo, no);
	assertTrue(core.lastMsg != null);

	//test 3
	core.release_ms = 9;
	core.submitLatch = new CountDownLatch(2);
	core.emptyLatch = new CountDownLatch(1);
	while (core.submitLatch.getCount() > 0) {
	    core.send(++no);
	}
	core.emptyLatch.await();
	System.out.println("->Subtest 3; " + core);

	assertEquals(core.exceptCount, 0);
	assertEquals(core.lastMsgNo, no);
	assertTrue(core.lastMsg != null);
	System.out.println("ok");
    }

    /**
     * Test of send method, of class ActorCore.
     */
    @Test
    public void testCore() throws Exception {
	tstSub(0);
	tstSub(1);
	tstSub(7);
    }

    @Test
    public void testPingpong() throws InterruptedException, ExecutionException {
	System.out.println("\ntestPingpong()");
	final ActorThreadPool threadP = new ThrPoolBasicImpl(2);
	final CountDownLatch latch = new CountDownLatch(1);
	class Impl
	{
	    int lastRecv;
	}
	class Act extends CallProxy<Impl, Integer, Integer>
	{
	    Act other;
	    String id;

	    public Act(String id) {
		super(threadP.create(new Impl()));
		this.id = id;
	    }

	    @Override
	    protected Integer act(Impl impl, Integer val) throws Exception {
		if (val < 0) {
		    return impl.lastRecv;
		}
		System.out.println(" " + id + " " + val);
		if (impl.lastRecv != 0) {
		    assertEquals(impl.lastRecv, val + 2);
		}
		impl.lastRecv = val;
		if (val > 0) {
		    other.call(val - 1);
		} else {
		    latch.countDown();
		}
		return impl.lastRecv;
	    }
	}
	Act ping = new Act("ping");
	Act pong = new Act("         pong");
	ping.other = pong;
	pong.other = ping;
	ping.call(11);
	latch.await();
	assertEquals((int) ping.call(-1).get(), 1);
	assertEquals((int) pong.call(-1).get(), 0);
	System.out.println("done");
    }

    public static void main(String[] args) throws Exception {
	ActorCoreTest tst = new ActorCoreTest();
	tst.testPingpong();
	tst.testCore();
	/*
	 * Each message sent & queued => processed excactly once:
	 *	#messages sent == #processedMsgs
	 */
    }
}
