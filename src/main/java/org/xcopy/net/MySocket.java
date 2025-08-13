package org.xcopy.net;

import home.lib.lang.UserException;
import home.lib.net.mq.MqBundle;
import home.lib.net.tms.TmsBroker;
import home.lib.net.tms.TmsChannel;
import home.lib.net.tms.TmsChannelInterface;
import home.lib.net.tms.TmsEventQueue;
import home.lib.net.tms.TmsItem;
import home.lib.util.TimeUtil;

import java.lang.reflect.Method;
import java.nio.channels.SocketChannel;

public class MySocket implements TmsChannelInterface {

	Object m_obj = null;

	TmsChannel m_cl = null;

	IMySocketListener m_inter = null;

	String m_ip = null;

	int m_port = -1;

	Object m_linkedClassHandle = null;

	String m_id = "" + System.nanoTime();

	// String m_pwd = "nopwd";

	TmsBroker m_sel = null;
	
	

	boolean m_selfCreated = false;
	
	private TmsBroker m_brk=null;

	private long m_cid = 0;

	// private int m_maxEventCount = 1024;

	private TmsEventQueue m_eQueue = new TmsEventQueue(1024);

	

	public TmsEventQueue getEventQueue() {
		return m_eQueue;
	}

	public SocketChannel getChannel() {
		return m_cl.getChannel();
	}

	public void setLinkedClassHandle(Object o) {
		m_linkedClassHandle = o;
	}

	public void setResendInterval(int n) {
		// no use
	}

	public void setUserObject(Object o) {
		m_obj = o;
	}

	public Object getUserObject() {
		// return m_cl.getChannel().getUserObject();
		return m_obj;
	}

	public void setMqSocketListener(IMySocketListener l) {
		m_inter = l;
	}

	/**
	 * 
	 * @param bs
	 * @param id
	 * @param ip
	 * @param port
	 * @param ltr
	 * @param sel
	 * @throws Exception
	 */
	public MySocket(TmsBroker br, String id, String ip, int port, IMySocketListener ltr) throws Exception {

		// m_bs = bs;
		//
		//

		if (ip == null)
			ip = "127.0.0.1";

		m_inter = ltr;
		m_ip = ip;
		m_port = port;

		// m_cl = new TmsChannel(m_bs, sel);
		// m_cl.setPath(id);

		m_id = id;

		if (br != null) {
			
			m_selfCreated=false;
			
			m_sel = br;
			
		} else {

			m_selfCreated = true;

			m_brk = new TmsBroker("self created(local)", "0.0.0.0", 0);
			// m_brk.jobOpt(8, 0);
			// m_brk.jobPeekCount(8);
			m_brk.setReceiveTimeoutSec(60);
			// m_brk.setSocketBufferSize(1024*1024*10);
			// m_brk.setLogger(this);
			m_brk.waitBind(8);
			
			m_sel=m_brk;

		}

	}

	/**
	 * 
	 * @return
	 */

	public Exception connect(long timeout, boolean keepConnection) throws Exception {
		return connect(timeout, keepConnection, "idname", "pwd");

	}

	public Exception connect(long timeout, boolean keepConnection, String idname, String pwd) throws Exception {

		m_cl = m_sel.connectw(m_ip, m_port, keepConnection, m_id, this, idname, pwd, this, timeout).get();

		m_cid = m_cl.getId();

		TimeUtil t = new TimeUtil();
		while (t.end_ms() < timeout) {
			TimeUtil.sleep(10);
			if (m_cl.isAlive())
				return null;
		}

		return null;
	}

	public void close(boolean keepConnection) {

		if (keepConnection) {

			// m_cl.getChannel().close();// just channel to close.

			m_sel.close(m_cid);

		} else {
			// m_cl.close();// close & remove keep connection
			m_sel.remove(m_cid);
			m_sel.close(m_cid);

			if (m_selfCreated) {
				m_brk.cleanUp();
				m_brk = null;
			}

		}

	}
	
	public TmsBroker getSelfBroker() {
		return m_brk;
	}
	

	public String getId() {
		if (m_cl == null)
			return null;

		return m_cl.getPath();
	}

	public boolean isAlive() {
		if (m_cl == null)
			return false;

		return m_cl.isAlive();
	}

	public boolean isKeeping() {
		return m_cl.getKeepConnection();
	}

	// public long getKeepingInterval() {
	//
	// }
	//
	public void setKeeping(long interval) {

		if (m_cl == null)
			return;

		// m_cl.debug("setKeeping doesn't work in wrap class");
		if (interval == 0)
			m_cl.setKeepConnection(false);
		else
			m_cl.setKeepConnection(true);
	}

	/**
	 * 
	 * @param aThrowable
	 * @return
	 */

	/**
	 * 
	 * If the timeout_msec is zero it is not wait for return, otherwise it will wait for return within timeout_msec
	 * milleseconds.
	 * 
	 * @param d
	 *            bunddle of data
	 * @param timeout_msec
	 * @return null or return value
	 */
	private MqBundle sendTo(MqBundle d, long timeout_msec) throws Exception {

		return sendTo(d.to, d, timeout_msec);

	}

	/**
	 * ( global = * )
	 * 
	 * @param to
	 * @param o
	 * @param timeout_ms
	 * @return
	 * @throws Exception
	 */
	synchronized public MqBundle sendTo(String to, MqBundle o, long timeout_ms) throws Exception {

		if (m_cl == null)
			return null;

		if (m_cl.isAlive() == false)
			return null;

		o.from = m_cl.getPath();
		o.to = to;
		o.from = m_cl.getPath();

		// if (m_cl.sendQueueCount() > m_bs)
		// m_cl.debug("t(%s) (%s) send queue size= %d'",
		// System.currentTimeMillis(), m_cl.getPath(),
		// m_cl.sendQueueCount());

		if (timeout_ms == 0) {
			m_cl.send(to, MqBundle.To(o));
			return null;
		}

		// TmsItem r = TmsItem.fromBytes( m_cl.get(to, MqBundle.To(o),
		// timeout_ms) );

		// return MqBundle.From(r.getData());
		return MqBundle.From(m_cl.get(to, MqBundle.To(o), timeout_ms));

	}

	/**
	 * to='*'
	 * 
	 * @param o
	 * @throws Exception
	 */
	public void sendBroadcast(MqBundle o) throws Exception {

		sendTo(o, 0);

	}

	/**
	 * 
	 * @param ms
	 */
	public static void sleep(long ms) {

		TimeUtil.sleep(ms);
	}

	/**
	 * ( global = * ) it only can call the public methods
	 * 
	 * @param to
	 * @param timeout
	 * @param method
	 * @param args
	 * @return
	 * @throws Exception
	 * 
	 */

	synchronized public Object callMethod(String to, long timeout, String method, Object... args) throws Exception {
		MqBundle o = new MqBundle();
		// return sendTo(hashCode(name), msgno, buf, timeout_ms);
		o.from = getId();
		o.to = to;

		o.setString("?_method", method);

		o.setObj("?_args", args);

		MqBundle r = null;

		if (to.indexOf('*') != -1 || timeout == 0) {
			sendTo(o, 0);
			return null;
		} else {
			r = sendTo(o, timeout);
		}

		if (r == null)
			return null;

		if (r.containKey("return") == false)
			return null;

		// return (Object) r.getZip("return");
		return (Object) r.getObj("return");
	}

	public Object callMethod(String to, String method, Object... args) throws Exception {
		return callMethod(to, (long) 8000, method, args);
	}

	/**
	 * 
	 * 
	 * @param cls
	 * @param methodName
	 * @param args
	 * @return
	 * @throws Exception
	 */
	static Object doMethod(Object cls, String methodName, Object... args) throws Exception {

		Class c = cls.getClass();

		Method m = null;

		if (args != null && args.length != 0) {

			Class[] cArg = new Class[args.length];
			for (int h = 0; h < cArg.length; h++) {

				Class ac = args[h].getClass();

				if (ac.equals(Long.class))
					cArg[h] = long.class;
				else if (ac.equals(Integer.class))
					cArg[h] = int.class;
				else if (ac.equals(Double.class))
					cArg[h] = double.class;
				else if (ac.equals(Byte.class))
					cArg[h] = byte.class;
				else if (ac.equals(Short.class))
					cArg[h] = short.class;
				else
					cArg[h] = args[h].getClass();

			}

			m = c.getMethod(methodName, cArg);
		} else
			m = c.getMethod(methodName, null);

		return m.invoke(cls, args);

	}

	public TmsItem doReceived(TmsChannel ask, TmsItem m, byte[] dataOfItem) {

		// System.out.format("mqsocket-recv from(%s) to(%s) \r\n ", m.getFrom(),
		// m.getTo());

		MqBundle rs = null;
		MqBundle re = null;

		try {

			re = MqBundle.From(m.getData());

			if (m_linkedClassHandle != null && re.containKey("?_method") && re.containKey("?_args")) {

				String method = re.getString("?_method");

				Object[] args = (Object[]) re.getObj("?_args");

				Object o = null;
				if (re.to.indexOf('*') != -1) {// is broadcast type?
					try {
						o = doMethod(m_linkedClassHandle, method, args);
					} catch (Exception e) {
						// no error check
					}
					return null;// no reutrn

				} else {
					o = doMethod(m_linkedClassHandle, method, args);
				}

				rs = re;
				// rs.setZip("return", o);
				rs.setObj("return", o);

			} else if (m_inter != null) {

				rs = m_inter.actionPerformed(this, re);

			}

		} catch (Exception eee) {
			// set error
			rs = re;
			rs.remoteErrorMessage = UserException.getStackTrace(eee);
			eee.printStackTrace();
		}

		try {
			if (rs != null) {
				// m.setData(MqBundle.To(rs));

				ask.sendReturn(m, MqBundle.To(rs));
				return m;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public void doConnected(TmsChannel ask) {

		m_inter.connected(this);

		debug("connnected " + ask.getId());
	}

	public void doDisconnected(TmsChannel ask) {
		m_inter.disconnected(this);

		debug("disconnnected ");
	}

	@Override
	public String toString() {
		return "mybroker wrap+mqcoket (" + m_ip + ":" + m_port + ") " + m_cl.toString();
	}

	public void debug(String f, Object... p) {
		try {
			String s = String.format(f, p);
			System.out.println(TimeUtil.now() + " mysocket " + s);

			// if (m_sel != null) {
			// m_sel.debug(this, f, p);
			// }

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public TmsBroker cli_bs() {
		return m_sel;
	}

	// public void setSocketBufferSize(long l) {
	//
	// m_sel.setSocketBufferSize(l);
	// }

	public void setTimeout(long to) {

		m_sel.setReceiveTimeoutSec(to / 1000);

	}

	@Override
	public Object onAccepteded(TmsChannel ch) {
		ch.setLoggedIn(true);
		return null;
	}

	@Override
	public boolean onConnected(TmsChannel ch) {
		doConnected(ch);

		return true;
	}

	@Override
	public void onDisconnected(TmsChannel ch) {
		doDisconnected(ch);

	}

	@Override
	public void onError(TmsChannel ch, byte[] rxd) {
		try {
			debug("%s", new String(rxd));
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	@Override
	public void onLink(TmsChannel ch) {

		ch.setLoggedIn(true);
	}

	@Override
	public TmsItem onReceived(TmsChannel ch, byte[] rxd, TmsItem item) {
		doReceived(ch, item, rxd);
		return null;
	}

	public TmsChannel getTmsChannel() {
		return m_cl;
	}

}
