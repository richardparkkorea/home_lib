package home.lib.lang;

import home.lib.util.TimeUtil;

/**
 * 
 * it will  bring some conceptually confuse with Java Thread.
 * 
 * 
 * @author richardpark
 *
 */
@Deprecated
final public class Thread2 implements Runnable {
	/**
	 * 
	 */
	private Runnable myRunnable = null;
	public double waitTimeout = 10.0;
	private int m_RunIsStillGoingOn = 0;
	private long m_arriveInRun=0;
	private Thread m_th = null;

	
	public Thread2(java.lang.Runnable r) {
		myRunnable = r;
	}


	public int start() {
		if (m_th != null || m_RunIsStillGoingOn==1) {
			System.out.println("uThread is still going on");
			return 0;
		}

		m_arriveInRun=System.currentTimeMillis();
		//
		long a=m_arriveInRun;
		//
		m_th = new Thread(this);
		m_th.start();
		//
		//System.out.println("test-1");
		TimeUtil t=new TimeUtil();
		while(t.end()<10.0 && m_arriveInRun==a) {
			
			//System.out.println( "test-  "+m_arriveInRun + "  " + a + "    "+ t.end()  );
			sleep(10);
		}
		//System.out.println("test-2");
		
		return 1;
	}

	public int is_alive() {
		return m_RunIsStillGoingOn;
	}

	public void wait2() {
		// debug("ThreadX:it is wiat for come out~1");
		if (m_th == null)
			return;

		TimeUtil tu = new TimeUtil();
		tu.start();

		while (tu.end() < this.waitTimeout) {
			if (m_RunIsStillGoingOn == 0) {
				m_th = null;
				// debug("ThreadX:it is wiat for come out~3");
				return;
			}
			try {
				Thread.sleep(10);
			} catch (java.lang.InterruptedException e) {
				e.printStackTrace();
			}
		}
		m_th = null;
		// debug("ThreadX:it is wiat for come out~2");
	}

	public void run() {
		// debug("ThreadX:run-start");
		m_RunIsStillGoingOn = 1;
		m_arriveInRun++;
		//System.out.println("test-3");
		myRunnable.run();
		m_RunIsStillGoingOn = 0;
		// debug("ThreadX:run-end");
	}

	/**
	 * 
	 * @param fmt
	 *            String
	 * @param sl
	 *            Object[]
	 */
	//public static void debug(String fmt, Object... sl) {
		/*
		 * if(true) return; if(_sst.MainClass.sst_debug!=1) return;
		 * _sst.debug.MyDebug.prt(fmt,sl);
		 */
	//}

	/**
     *
     */
	public static void sleep(long millis) {

		// window xp: shutdown err mark!!!
		if (millis < 1)
			millis = 1;

		try {
			Thread.sleep(millis);
		} catch (java.lang.InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
 

}

/*
 * public class Thread2 extends NetCommonBase implements Runnable { private
 * Runnable myRunnable=null; public double waitTimeout=10.0;
 * 
 * public Thread2() { m_RunIsStillGoingOn = 0; myRunnable=null; } public
 * Thread2(java.lang.Runnable runa) { m_RunIsStillGoingOn = 0; myRunnable=runa; }
 * 
 * protected int m_RunIsStillGoingOn = 0; private Thread m_th = null;
 * 
 * public int start() { if (m_th != null) {
 * System.out.println("_sst.ThreadX.start err(aleady running)"); System.exit(0);
 * return 0; }
 * 
 * m_th = new Thread(this); m_th.start(); return 1; }
 * 
 * public void close() { m_th = null; }
 * 
 * int isAlive() { return m_RunIsStillGoingOn; }
 * 
 * 
 * public void wait2() { //debug("ThreadX:it is wiat for come out~1"); TimeUtil
 * tu=new TimeUtil(); tu.start();
 * 
 * while(tu.end()<this.waitTimeout) { if (m_RunIsStillGoingOn == 0) { close();
 * //debug("ThreadX:it is wiat for come out~3"); return; } try{
 * Thread.sleep(10); } catch( java.lang.InterruptedException e) {
 * e.printStackTrace(); } } close();
 * //debug("ThreadX:it is wiat for come out~2"); }
 * 
 * 
 * public void run() { //debug("ThreadX:run-start"); m_RunIsStillGoingOn = 1;
 * if(myRunnable!=null) { myRunnable.run(); } else { run2(); } m_RunIsStillGoingOn =
 * 0; //debug("ThreadX:run-end"); }
 * 
 * 
 * public static void debug(String fmt,Object...sl) {
 * if(_sst.MainClass.sst_debug!=1) return; _sst.debug.MyDebug.prt(fmt,sl); }
 * 
 * //inherity public void run2() {
 * 
 * }
 * 
 * 
 * }
 */

/*
 * class Thread2 extends NetCommonBase implements Runnable { public Thread2() {
 * m_is_active = 0; }
 * 
 * protected int m_is_active = 0; private Thread m_th = null;
 * 
 * public int start() { if (m_th != null) { return 0; } m_th = new Thread(this);
 * m_th.start(); return 1; }
 * 
 * public void close() { m_th = null; }
 * 
 * int th_is_run() { return m_is_active; }
 * 
 * void debug(String fmt,Object...sl) { if(_sst.MainClass.sst_debug!=1) return;
 * _sst.debug.MyDebug.prt(fmt,sl); }
 * 
 * public void wait2() { for (int i = 0; i < 1000; i++) { // wait for 10 second
 * if (m_is_active == 0) { return; } _sst.net.netqueue.NetCommonBase.sleep(10);
 * } close(); }
 * 
 * final public void run() { debug("Thread2:run-start"); m_is_active = 1;
 * run2(); m_is_active = 0; debug("Thread2:run-end"); }
 * 
 * public void run2() {
 * 
 * }
 * 
 * 
 * }
 */
