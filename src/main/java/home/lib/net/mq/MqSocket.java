package home.lib.net.mq;

import home.lib.lang.UserException;
import home.lib.net.nio.NqFactory;
import home.lib.net.nio.NqChannel;
import home.lib.net.nio.NqConnector;
import home.lib.net.nio.NqConnectorListener;
import home.lib.net.nio.NqItem;
import home.lib.net.nio.NqSelector;
import home.lib.util.TimeUtil;

import java.lang.reflect.Method;

public class MqSocket extends NqConnectorListener {

	private NqFactory m_bs = null;

	// static {
	//
	// m_bs = new MqBootstrap("static mqsocket wrap(cli bs)");
	//
	// m_bs.jobOpt(64, 0, 0);
	// m_bs.setSocketBufferSize(1024 * 32);
	// m_bs.setReceiveTimeoutSec(60 * 3);
	// m_bs.setWaitReturnTimeoutSec(12);
	//
	// }

	NqConnector m_cl = null;
	IMqSocketListener m_inter = null;
	String m_ip = null;
	int m_port = -1;
	Object m_linkedClassHandle = null;

	String m_id = "" + System.nanoTime();

	String m_pwd = "nopwd";

	public NqChannel getChannel() {
		return m_cl.getChannel();
	}

	public NqConnector getMqClient() {
		return m_cl;
	}

	public void setSocketBufferSize(long n) {

		m_bs.setSocketBufferSize(n);
	}

	public void setTimeout(long n) {
		m_bs.setReceiveTimeoutSec(n / 1000);
	}

	public void setLinkedClassHandle(Object o) {
		m_linkedClassHandle = o;
	}

	public void setResendInterval(int n) {
		// no use
	}

	public void setUserObject(Object o) {
		m_cl.getChannel().setUserObject(o);
	}

	public Object getUserObject() {
		return m_cl.getChannel().getUserObject();
	}

	public void setMqSocketListener(IMqSocketListener l) {
		m_inter = l;
	}

	/**
	 * get static bootstrap of clients
	 * 
	 * @return
	 */
	public NqFactory cli_bs() {
		return m_bs;
	}

	/**
	 * 
	 * @param bs
	 * @param id
	 * @param ip
	 * @param port
	 * @param ltr
	 * @throws Exception
	 */
	public MqSocket(NqFactory bs, String id, String ip, int port, IMqSocketListener ltr) throws Exception {
		this(bs, id, ip, port, ltr, null);
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
	public MqSocket(NqFactory bs, String id, String ip, int port, IMqSocketListener ltr, NqSelector sel)
			throws Exception {

		m_bs = bs;
		//
		//

		if (ip == null)
			ip = "127.0.0.1";

		m_inter = ltr;
		m_ip = ip;
		m_port = port;

		m_cl = new NqConnector(m_bs, sel);
		m_cl.setPath(id);

	}

	/**
	 * 
	 * @return
	 */
	public Exception connect(long timeout, boolean keepConnection) throws Exception {

		m_cl.connect(m_id, m_pwd, m_ip, m_port, this).waitConnect(timeout / 1000);

		return null;
	}
	/**
	 * 
	 * @param id
	 * @param pwd
	 * @param timeout
	 * @param keepConnection
	 * @return
	 * @throws Exception
	 */
	public Exception connect(String id,String pwd,long timeout, boolean keepConnection) throws Exception {

		m_id=id;
		m_pwd=pwd;
		
		m_cl.connect(m_id, m_pwd, m_ip, m_port, this).waitConnect(timeout / 1000);

		return null;
	}

	
	/**
	 * 
	 * @return
	 */
	public boolean isConnecting() {
		return m_cl.isConnecting();
	}

	// public MqBundle ping(long timeout) throws Exception {
	//
	// }
	//
	// public void sendPing() {
	//
	// }

	public void close(boolean keepConnection) {

		if (keepConnection) {

			m_cl.getChannel().close();// just channel to close.
		} else {
			m_cl.close();// close & remove keep connection
		}
	}

	public String getId() {
		return m_cl.getPath();
	}

	public boolean isAlive() {
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

		o.from = m_cl.getPath();
		o.to = to;
		o.from = m_cl.getPath();

		// if (m_cl.sendQueueCount() > m_bs)
		// m_cl.debug("t(%s) (%s) send queue size= %d'", System.currentTimeMillis(), m_cl.getPath(),
		// m_cl.sendQueueCount());

		if (timeout_ms == 0) {
			m_cl.send(to, MqBundle.To(o));
			return null;
		}

		//NqItem r = NqItem.fromBytes(   m_cl.get(to, MqBundle.To(o), timeout_ms) );

		//return MqBundle.From(r.getData());
		return   MqBundle.From( m_cl.get(to, MqBundle.To(o), timeout_ms) );

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
		return callMethod(to, (long) m_bs.getWaitReturnTimeoutSec() * 1000, method, args);
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

	@Override
	public NqItem received(NqConnector ask, NqItem m, byte[] dataOfItem) {

		// System.out.format("mqsocket-recv from(%s) to(%s) \r\n ", m.getFrom(), m.getTo());

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
				m.setData(MqBundle.To(rs));
				return m;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public void connected(NqConnector ask) {

		// this.setKeeping(interval);

		// if it's not accepter
		if (m_cl.isAccepter() == false) {
			m_cl.setKeepConnection(true);
		}

		m_inter.connected(this);

		debug("connnected ");
	}

	@Override
	public void disconnected(NqConnector ask) {
		m_inter.disconnected(this);

		debug("disconnnected ");
	}

	@Override
	public void sendSucceeded(NqConnector ask, NqItem e) {

	}

	@Override
	public void sendFailed(NqConnector ask, NqItem e) {

	}

	@Override
	public boolean putPath(NqConnector ask, String name) {

		return true;
	}

	@Override
	public String toString() {
		return "mqbroker wrap+mqcoket (" + m_ip + ":" + m_port + ") " + m_bs.toString();
	}

	public void debug(String f, Object... p) {
		try {
			// String s = String.format(f, p);
			// System.out.println(TimeUtil.now() + " mqsocket " + s);

			if (m_bs != null) {
				m_bs.debug(this, f, p);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
