package deprecated.lib.net.bridge2;

import deprecated.lib.net.bridge.AsyncSocket;
import deprecated.lib.net.bridge.AsyncSocketInterface;
import deprecated.lib.net.bridge.CBundle;
import home.lib.util.TimeUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.StandardSocketOptions;
import java.rmi.RemoteException;
import java.util.ArrayList;

@Deprecated
public class RmSocket implements AsyncSocketInterface {

	static {
		// add to keeper
		new Thread(new RmSocketKeeper()).start();

	}

	/**
	 * 
	 * 
	 * 
	 * 
	 */

	RmSocket _this = this;

	final String m_id;

	final Object m_lock = new Object();

	final Object m_sendLock = new Object();

	long m_sendIndex = 0;

	long m_privateWhenSend = 0;

	CBundle m_sendOk = null;

	String m_ip;

	int m_port;

	boolean m_alive2 = false;

	// public Socket clientSocket = null;

	public AsyncSocket clientSocket = null;

	long m_lastWorkTime = 0;

	private long m_keepConnectingRetryInterval = 32 * 1000;// ms

	private int m_retry_interval = 3000;// milliseconds

	private boolean m_timeoutCheck_isAlive = false;

	private boolean m_isReceiving = false;

	private Object m_linkedClassHandle = null;

	private long m_sendTimeout = 3000;

	public long m_timeoutVal = 60 * 1000;

	private boolean m_isKeeping = true;

	TimeUtil tmrConnectionAttemptInterval = new TimeUtil();

	AsyncSocketInterface m_actionListener = null;

	private long MAX_RECEIVABLE_SIZE = 1024 * 1024 * 8;// 16m

	public void setMaxReceivableSize(long n) {
		MAX_RECEIVABLE_SIZE = n;
	}

	private long SOCKET_BUFFER_SIZE = 1024 * 8;

	public void setSocketBufferSize(long n) {
		SOCKET_BUFFER_SIZE = n;
	}

	public void setTimeout(long n) {
		m_timeoutVal = n;
	}

	public void setActionListener(AsyncSocketInterface l) {
		m_actionListener = l;
	}
	/**
	 * if it does not receive any data from dest while m seconds then disconnect the tcp
	 * 
	 * @param m
	 */
	// public void setTimeout(long m) {
	// m_timeoutVal = m;
	// }

	/**
	 * 
	 * @param m
	 */
	public void setSendTimeout(long m) {
		m_sendTimeout = m;
	}

	/**
	 * set the number of retransmission when use the sendTo function. (internal use variable)
	 * 
	 * @param n
	 */
	public void setResendInterval(int n) {
		m_retry_interval = n;
	}

	/**
	 * 
	 * 
	 * @param cls
	 */
	public void setClsHandle(Object cls) {
		m_linkedClassHandle = cls;
	}
	
	
	/**
	 * 
	 * 
	 */
	RmSocketListener m_listerner=null;
	public void setRmSocketListener(RmSocketListener l) {
		m_listerner=l;
	}

	/**
	 * id == null , give randome ID
	 * 
	 * @param id
	 * @param ip
	 * @param port
	 */
	public RmSocket(String id, String ip, int port, Object cls) throws Exception {

		//

		//
		synchronized (m_lock) {

			if (id == null) {
				throw new Exception("not allow 'null' id");
			}

			m_id = id;
			m_ip = ip;
			m_port = port;

			// setActionListener(l);
			setResendInterval(1000);// 500ms

			m_linkedClassHandle = cls;

		}

	}

	/**
	 * 
	 * 
	 * 
	 */
	protected void finalize() throws Throwable {
		try {
			RmSocketKeeper.remove(this);// add this to link keeper
		} finally {
			super.finalize();
		}
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

	/**
	 * 
	 * @return
	 */
	public Exception connect(long timeout) throws Exception {
		synchronized (m_lock) {
			if (isAlive2() == false) {
				try {

					TimeUtil t = new TimeUtil();

					clientSocket = new AsyncSocket(m_ip, m_port, this, (int) SOCKET_BUFFER_SIZE, timeout);

					clientSocket.getChannel().setOption(StandardSocketOptions.SO_SNDBUF, (int) SOCKET_BUFFER_SIZE);
					clientSocket.getChannel().setOption(StandardSocketOptions.SO_RCVBUF, (int) SOCKET_BUFFER_SIZE);

					CBundle e2 = new CBundle();// set this ID in
					e2.from = getId2();
					e2.from_pwd = "putId";
					e2.privateSetId = true;

					CBundle b = sendTo(e2, timeout);

					if (b == null) {
						clientSocket.close();
						throw new Exception("id register failed");
					}

					RmSocketKeeper.add(this);

					if (m_keepConnectingRetryInterval > 0)
						m_isKeeping = true;

					System.out.println("AsyncLink.connect time : " + t.end_ms() + " ms return=>" + b.getString("res"));

				} catch (Exception e) {

					if (clientSocket != null) {
						clientSocket.close();

						return e;
					}
				}
			} // if( is alive

			return null;
		}

	}

	/**
	 * 
	 * close link (with set keepConnection=false )
	 * 
	 */
	public void close() {
		synchronized (m_lock) {
			try {

				RmSocketKeeper.remove(this);// add this to link keeper
				m_isKeeping = false;

				if (clientSocket != null) {
					clientSocket.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String getId2() {
		return m_id;
	}

	public boolean isAlive2() {

		if (clientSocket == null)
			return false;
		// return m_alive2;
		return clientSocket.isAlive();
	}

	public boolean isKeeping() {
		return m_isKeeping;
	}

	public long getKeepingInterval() {
		return m_keepConnectingRetryInterval;
	}

	/**
	 * Automatic Socket keep-alive If the socket is disconnected then it will try to connect to the TARGET every 3
	 * seconds And connection retries is unlimited until you stop
	 * 
 
	 */

	public void setKeeping(long interval) {

		m_keepConnectingRetryInterval = interval;
		if (interval > 0)
			m_isKeeping = true;
		else
			m_isKeeping = false;
	}

	@Override
	public void recv(AsyncSocket ask, byte[] buf, int len) {

		if (len == 0)
			return;// do nothing

		// System.out.format(" len = %d \r\n", len );

		m_lastWorkTime = System.currentTimeMillis();

		try {
			doRead(buf, len, m_returnValues, m_receivedValues, m_ep);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// m_lastReturn.privateSendIndex = Long.MAX_VALUE;
	ArrayList<CBundle> m_returnValues = null;
	ArrayList<CBundle> m_receivedValues = null;
	RmPacketPicker m_ep = null;

	@Override
	public void connected(AsyncSocket ask) {
		m_alive2 = true;

		m_lastWorkTime = System.currentTimeMillis();

		m_returnValues = new ArrayList<CBundle>();
		m_receivedValues = new ArrayList<CBundle>();
		m_ep = new RmPacketPicker(MAX_RECEIVABLE_SIZE);// 10m
		System.out.println("async socket connected");

		if (m_actionListener != null) {
			try {
				m_actionListener.connected(ask);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void disconnected(AsyncSocket ask) {
		m_alive2 = false;

		System.out.println("async socket disconnected");

		if (m_actionListener != null) {
			try {
				m_actionListener.disconnected(ask);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * register ID to the SERVER
	 * 
	 */

	private boolean searchInRecentlyReturns(ArrayList<CBundle> returnValues, CBundle re) throws Exception {

		for (int i = 0; i < returnValues.size(); i++) {

			CBundle lr = returnValues.get(i);

			if (lr.to != null && lr.privateSendIndex == re.privateSendIndex && lr.to.equals(re.from)
					&& lr.privateSocket.equals(re.privateSocket) && lr.privateWhenSend == re.privateWhenSend) {

				// System.out.println("do searchInRecentlyReturns at " + System.currentTimeMillis());
				// re-transmitting the previous response value
				clientSocket.send(RmPacketPicker.make(lr));
				return true;
			}
		}

		return false;
	}

	private boolean searchInRecentlyRecevied(ArrayList<CBundle> receivedValues, CBundle re) throws Exception {

		for (int i = 0; i < receivedValues.size(); i++) {

			CBundle lr = receivedValues.get(i);

			if (lr.to != null && lr.privateSendIndex == re.privateSendIndex// && lr.to.equals(re.to)
					&& lr.privateSocket.equals(re.privateSocket) && lr.privateWhenSend == re.privateWhenSend) {

				return true;
			}
		}

		return false;
	}

	public void doRead(byte[] buf, int len, ArrayList<CBundle> returnValues, ArrayList<CBundle> receviedValues,
			RmPacketPicker ep) throws Exception {

		// System.out.println(clientSocket.toString()+"====================> client do read 11 ");

		ep.append(buf, len);

		// System.out.println(clientSocket.toString()+"====================> client do read 22");

		CBundle re;

		while ((re = ep.check()) != null) {

			// m_in_events.add(re);

			if (re.to.equals(this.m_id) || re.privateIsBroadcast == true) {
				if (re.privateCode == 's') {

					/// System.out.println("callmethod-------------1");

					String from = re.from;
					String to = re.to;
					long si = re.privateSendIndex;
					String skname = re.privateSocket;
					long when = re.privateWhenSend;

					if (searchInRecentlyReturns(returnValues, re)) {

					} else if (searchInRecentlyRecevied(receviedValues, re) == false) {// new index data}

						// add to received list
						byte[] bb2 = CBundle.To(re);
						receviedValues.add(CBundle.From(bb2));
						if (receviedValues.size() > 4) {// 1024) {
							receviedValues.remove(0);
						}

						//
						// call listener
						CBundle rs = null;
						re.remote_error = null;
						// m_lastReturn = null;// when the new data is arrive then it set to null
						try {

							if (m_linkedClassHandle != null && re.containKey("method") && re.containKey("args")) {

								String method = re.getString("method");
								// Object[] args = (Object[]) re.getZip("args");
								Object[] args = (Object[]) re.getObj("args");

								Object o = null;
								if (re.privateIsBroadcast == true) {
									try {
										o = doMethod(m_linkedClassHandle, method, args);
									} catch (Exception e) {
										// no error check
									}

								} else {
									o = doMethod(m_linkedClassHandle, method, args);
								}

								rs = re;
								// rs.setZip("return", o);
								rs.setObj("return", o);

							} else {
								// 1.803.01
								if (m_listerner != null) {
									 
									m_listerner.actionPerformed(re);
									 
								}
							}

						} catch (Exception eee) {
							// set error
							rs = re;
							rs.remote_error = getStackTrace(eee);
							eee.printStackTrace();
						}

						//
						// do not send a RETURN when broadcast message
						// arrived
						if (re.privateIsBroadcast == false) {
							if (rs != null) {
								rs.result = true;
								rs.from = to;
								rs.to = from;
								rs.privateSendIndex = si;
								rs.privateCode = 'r';
								rs.privateSocket = skname;
								rs.privateWhenSend = when;

								clientSocket.send(RmPacketPicker.make(rs));

								byte[] bb = CBundle.To(rs);
								// m_lastReturn = CTcpBundle.From(bb);

								returnValues.add(CBundle.From(bb));
								if (returnValues.size() > 4) {
									returnValues.remove(0);
								}

								// System.out.println("set return " + rs.l);
							}

						}

					}

				} else if (re.privateCode == 'r' && re.result == true) {

					if (m_sendIndex == re.privateSendIndex && m_sendOk == null
							&& m_privateWhenSend == re.privateWhenSend) {

						m_sendOk = re;
					}

				}

			} // re.to.equals(this.m_id)

		}
	}

	/**
	 * 
	 * @param aThrowable
	 * @return
	 */
	public static String getStackTrace(Throwable aThrowable) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}

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
	public CBundle sendTo(CBundle d, long timeout_msec) throws Exception {

		synchronized (m_sendLock) {

			if (isAlive2() == false)
				return null;

			// try {
			//
			m_sendIndex++;
			m_privateWhenSend = System.currentTimeMillis();// add: 150722

			// for (int n = 0; n < m_retransmissionCount + 1; n++) {
			m_sendOk = null;

			synchronized (m_lock) {

				d.from = this.getId2();// replace
				d.privateSocket = this.clientSocket.m_channel.getLocalAddress().toString();
				d.privateWhenSend = m_privateWhenSend;// add:150722
				d.privateSendIndex = m_sendIndex;
				d.privateCode = 's'; // send type

				clientSocket.send(RmPacketPicker.make(d));

				if (timeout_msec == 0) // if there is no wait to close
										// after one sent
				{
					return null;
				}

			} // synchronized(m_lock
				// } catch (Exception e) {
				//
				// }
				//
			long until = System.currentTimeMillis() + timeout_msec;
			TimeUtil resend = new TimeUtil();
			//
			while ((until > System.currentTimeMillis())) {

				if (resend.end_ms() > m_retry_interval) {
					resend.start();

					clientSocket.send(RmPacketPicker.make(d));

				}

				sleep(1);

				if (m_sendOk != null) {

					if (m_sendOk.remote_error != null) {
						throw new RemoteException(m_sendOk.remote_error);
					}
					return m_sendOk;
				}

				if (isAlive2() == false)
					return null;
			}

			return null;
		} // synchronized(sendlock

	}
 
	public Object callMethod(String to, String method, Object... args) throws Exception {
		return callMethod(to, m_sendTimeout, method, args);
	}

	public Object callMethod(String to, long timeout, String method, Object... args) throws Exception {
		CBundle o = new CBundle();
		// return sendTo(hashCode(name), msgno, buf, timeout_ms);
		o.from = this.m_id;
		o.to = to;

		if (o.to.equals("*")) {
			o.privateIsBroadcast = true;
		}

		o.setLong("cmd", 0);
		o.setString("method", method);
		// o.setZip("args", args);
		o.setObj("args", args);

		CBundle r = null;

		if (o.privateIsBroadcast || timeout == 0) {
			sendTo(o, 0);
			return null;
		} else {
			r = sendTo(o, timeout);
		}

		if (r.containKey("return") == false)
			return null;

		// return (Object) r.getZip("return");
		return (Object) r.getObj("return");
	}

	/**
	 * 
	 * @param ms
	 */
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
			;
		}
	}

}
