package deprecated.lib.net.mq;

import home.lib.io.FilenameUtils;
import home.lib.util.TimeUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.ArrayList;

 

public class MqSocket implements ASocketChannelInterface {

	static {
		// add to keeper
		new Thread(new MqSocketKeeper()).start();

	}

	public boolean m_debug = false;

	public void debug(String fmt, Object... args) {
		if (m_debug == false)
			return;

		try {
			System.out.println("MqSocket-" + String.format(fmt, args));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public long m_connectionTimeout = 6000;

	MqSocket _this = this;

	final String m_id;

	final Object m_lock = new Object();

	final Object m_sendLock = new Object();

	long m_sendIndex = 0;

	long m_privateWhenSend = 0;

	MqBundle m_sendOk = null;

	String m_ip;

	int m_port;

	boolean m_alive = false;

	private ASocketChannel m_clientSocket = null;
	
	public ASocketChannel getChannel() {
		return m_clientSocket;
	}
 
	
	
	TimeUtil m_last_rx_time = new TimeUtil();

	private long m_keepConnectingRetryInterval = 32 * 1000;// ms

	private int m_retry_interval = 3000;// milliseconds

	public long m_timeoutVal = 60 * 1000 * 3;

	private boolean m_isKeeping = true;

	TimeUtil m_tmrConnectionAttemptInterval = new TimeUtil();

	// private long m_receiveByteLimit = 1024 * 64;// 16m
	//
	// public void setReceiveByteLimit(long n) {
	// m_receiveByteLimit = n;
	// }

	private long m_socketBufferSize = 1024 * 8;

	public void setSocketBufferSize(long n) {
		m_socketBufferSize = n;
	}

	public void setTimeout(long n) {
		m_timeoutVal = n;
	}

	private Object m_linkedClassHandle;

	public void setLinkedClassHandle(Object o) {
		m_linkedClassHandle = o;
	}

	// /**
	// *
	// * @param m
	// */
	// public void setSendTimeout(long m) {
	// m_sendTimeout = m;
	// }

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
	 */
	Object m_userObj = null;

	public void setUserObject(Object o) {
		m_userObj = o;
	}

	public Object getUserObject() {
		return m_userObj;
	}

	/**
	 * 
	 * 
	 */
	MqSocketListener m_listerner = null;

	public void setMqSocketListener(MqSocketListener l) {
		m_listerner = l;
	}

	/**
	 * id == null , give randome ID <br>
	 * the null ip is indicate local ip<br>
	 * 
	 * @param id
	 * @param ip
	 * @param port
	 */
	public MqSocket(String id, String ip, int port, MqSocketListener ltr) throws Exception {
		synchronized (m_lock) {

			if (id == null) {
				throw new Exception("not allow 'null' id");
			}

			if (ip == null)
				ip = "127.0.0.1";

			m_id = id;
			m_ip = ip;
			m_port = port;
			m_listerner = ltr;
		}
	}

	/**
	 * 
	 * 
	 * 
	 */
	@Override
	protected void finalize() throws Throwable {
		try {
			MqSocketKeeper.remove(this);// add this to link keeper
		} finally {
			super.finalize();
		}
	}

	
	/**
	 * 
	 * @return
	 */
	public Exception connect(long timeout,boolean keepConnection) throws Exception {

		m_connectionTimeout = timeout;

		synchronized (m_lock) {
			if (isAlive() == false) {
				// boolean debug0 = m_debug;
				try {
					
					if (keepConnection) { //stay connected regardless of exceptions
						MqSocketKeeper.add(this);
						m_isKeeping = true;
					}
					
					
					// m_debug = true;
					// TimeUtil t = new TimeUtil();
					m_tmrCanSendPing.start();

					m_clientSocket = new ASocketChannel(m_ip, m_port, this, (int) m_socketBufferSize, timeout);

					// ping(0);
					// ping(0);
					MqBundle b = ping(m_connectionTimeout);

					if (b == null) {
						m_clientSocket.close();
						throw new Exception("MqSocket- ping check failed");
					}

					MqSocketKeeper.add(this);

					if (m_keepConnectingRetryInterval > 0)
						m_isKeeping = true;

					// System.out.println("AsyncLink.connect time : " + t.end_ms() + " ms return=>" +
					// b.getString("res"));

				} catch (Exception e) {

					//e.printStackTrace();
					try {
						m_listerner.log(this, e); 
					}catch(Exception e1) {
						
					}

					if (m_clientSocket != null) {
						debug("MqSocket - close after connection failed");
						m_clientSocket.close();

						return e;
					}
				} finally {
					// m_debug = debug0;
				}
			} // if( is alive

			return null;
		}

	}
	
	
//	/**
//	 * connect socket with keep connections
//	 * normally the 'connect' function doesn't keep connection if the 'connect' fucntion has exception when it try 
//	 * @return
//	 */
//	public Exception connect(long timeout ) throws Exception {
//
//		return connect(timeout,false);
// 	 
//	}
	
	

	public MqBundle ping(long timeout) throws Exception {
		MqBundle e2 = new MqBundle();// set this ID in
		e2.from = getId();
		e2.msg = "pingreq";
		e2.to = "";
		// e2.privateSetId = true;
		// System.out.println(e2.msg+" "+getId() );
		debug("pingreq(%s)", getId());

		return sendTo(e2, timeout);
	}

	/**
	 * 
	 * ping automatically sent ever (timeoutval/3) ms
	 * 
	 * 
	 */
	TimeUtil m_tmrCanSendPing = new TimeUtil();

	public void sendPing() {

		if (isAlive()) {
			if (m_tmrCanSendPing.end_ms() > Math.max((m_timeoutVal / 3), 3 * 1000)) {
				m_tmrCanSendPing.start();
				try {
					ping(0);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
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

				MqSocketKeeper.remove(this);// add this to link keeper
				m_isKeeping = false;

				if (m_clientSocket != null) {
					m_clientSocket.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String getId() {
		return m_id;
	}

	public boolean isAlive() {

		if (m_clientSocket == null)
			return false;

		// return m_alive2;
		return m_clientSocket.isAlive();
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

	/**
	 * 
	 * @param interval
	 */
	public void setKeeping(long interval) {

		m_keepConnectingRetryInterval = interval;
		if (interval > 0)
			m_isKeeping = true;
		else
			m_isKeeping = false;
	}

	@Override
	public void recv(ASocketChannel ask, byte[] buf, int len) {

		if (len == 0)
			return;// do nothing

		try {
			doRead(buf, len, m_returnValues, m_receivedValues, m_ep);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// m_lastReturn.privateSendIndex = Long.MAX_VALUE;
	ArrayList<MqBundle> m_returnValues = null;
	ArrayList<MqBundle> m_receivedValues = null;
	MqPacketPicker m_ep = null;

	@Override
	public void connected(ASocketChannel ask) {
		m_alive = true;

		m_last_rx_time.start();

		m_returnValues = new ArrayList<MqBundle>();
		m_receivedValues = new ArrayList<MqBundle>();
		m_ep = new MqPacketPicker(m_socketBufferSize);// 10m
		m_ep.m_debug = m_debug;
		// System.out.println("async socket connected");

		if (m_listerner != null) {
			try {
				m_listerner.connected(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void disconnected(ASocketChannel ask) {
		m_alive = false;

		// System.out.println("async socket disconnected");

		if (m_listerner != null) {
			try {
				m_listerner.disconnected(this);
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

	private boolean searchInRecentlyReturns(ArrayList<MqBundle> returnValues, MqBundle re) throws Exception {

		for (int i = 0; i < returnValues.size(); i++) {

			MqBundle lr = returnValues.get(i);

			if (lr.to != null && lr.privateSendIndex == re.privateSendIndex && lr.to.equals(re.from)
					&& lr.privateSocket.equals(re.privateSocket) && lr.privateWhenSend == re.privateWhenSend) {

				m_clientSocket.send(MqPacketPicker.make(lr));
				return true;
			}
		}

		return false;
	}

	private boolean searchInRecentlyRecevied(ArrayList<MqBundle> receivedValues, MqBundle re) throws Exception {

		for (int i = 0; i < receivedValues.size(); i++) {

			MqBundle lr = receivedValues.get(i);

			if (lr.to != null && lr.privateSendIndex == re.privateSendIndex// && lr.to.equals(re.to)
					&& lr.privateSocket.equals(re.privateSocket) && lr.privateWhenSend == re.privateWhenSend) {

				return true;
			}
		}

		return false;
	}

	public void doRead(byte[] buf, int len, ArrayList<MqBundle> returnValues, ArrayList<MqBundle> receviedValues,
			MqPacketPicker ep) throws Exception {

		ep.append(buf, len);

		MqBundle re;

		while ((re = ep.check()) != null) {

			m_last_rx_time.start();
			// m_in_events.add(re);

			// if (re.to.equals(this.m_id) ) {
			if (FilenameUtils.wildcardMatch(this.m_id, re.to)) {

				if (re.privateCode == 's') {// receive(frmo sender)

					String from = re.from;
					String to = re.to;
					long si = re.privateSendIndex;
					String skname = re.privateSocket;
					long when = re.privateWhenSend;

					if (searchInRecentlyReturns(returnValues, re)) {

						// target data is already responsed?

					} else if (searchInRecentlyRecevied(receviedValues, re) == false) {
						// it's new data?

						// add to received list
						byte[] bb2 = MqBundle.To(re);
						receviedValues.add(MqBundle.From(bb2));
						if (receviedValues.size() > 4) {// 1024) {
							receviedValues.remove(0);
						}

						//
						// call listener
						MqBundle rs = null;
						re.remoteErrorMessage = null;

						try {

							if (m_linkedClassHandle != null && re.containKey("?_method") && re.containKey("?_args")) {

								String method = re.getString("?_method");

								Object[] args = (Object[]) re.getObj("?_args");

								Object o = null;
								if (re.to.indexOf('*') != -1) {
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

							} else if (m_listerner != null) {

								rs=m_listerner.actionPerformed(this, re);

							}

						} catch (Exception eee) {
							// set error
							rs = re;
							rs.remoteErrorMessage = getStackTrace(eee);
							eee.printStackTrace();
						}

						//
						// do not send a RETURN when broadcast message
						// arrived
						if (re.to.indexOf('*') == -1) {
							if (rs != null) {
								rs.result = true;
								rs.from = to;
								rs.to = from;
								rs.privateSendIndex = si;
								rs.privateCode = 'r';
								rs.privateSocket = skname;
								rs.privateWhenSend = when;

								m_clientSocket.send(MqPacketPicker.make(rs));

								byte[] bb = MqBundle.To(rs);

								returnValues.add(MqBundle.From(bb));
								if (returnValues.size() > 4) {
									returnValues.remove(0);
								}

							}

						}

					}

				} else if (re.privateCode == 'r' && re.result == true) {// response

					if (m_sendIndex == re.privateSendIndex && m_sendOk == null
							&& m_privateWhenSend == re.privateWhenSend) {

						m_sendOk = re;
					}

				} else if (re.privateCode == 'b' && re.result == true) {// internal

					if (m_sendIndex == re.privateSendIndex && m_sendOk == null
							&& m_privateWhenSend == re.privateWhenSend) {
						m_sendOk = re;

						debug("%s(%s) - internal msg ", re.msg, getId());
						// System.out.println(re.msg+" "+getId());

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
	private MqBundle sendTo(MqBundle d, long timeout_msec) throws Exception {

		synchronized (m_sendLock) {

			if (isAlive() == false)
				return null;

			String skstr = this.m_clientSocket.m_channel.getLocalAddress().toString();
			// try {
			//
			m_sendIndex++;
			m_privateWhenSend = System.currentTimeMillis();// add: 150722

			// for (int n = 0; n < m_retransmissionCount + 1; n++) {
			m_sendOk = null;

			// synchronized (m_lock) {

			d.from = this.getId();// replace
			d.privateSocket = skstr;
			d.privateWhenSend = m_privateWhenSend;// add:150722
			d.privateSendIndex = m_sendIndex;
			d.privateCode = 's'; // send type

			m_clientSocket.send(MqPacketPicker.make(d));

			if (timeout_msec == 0) // if there is no wait to close
									// after one sent
			{
				return null;
			}

			// }
			//
			//
			long until = System.currentTimeMillis() + timeout_msec;
			TimeUtil resend = new TimeUtil();
			//
			while ((until > System.currentTimeMillis())) {

				if (resend.end_ms() > m_retry_interval) {
					resend.start();

					m_clientSocket.send(MqPacketPicker.make(d));

				}

				sleep(1);

				if (m_sendOk != null) {

					if (m_sendOk.remoteErrorMessage != null) {
						throw new UserRemoteException(m_sendOk.remoteErrorMessage);
					}
					return m_sendOk;
				}

				if (isAlive() == false)
					return null;
			}

			return null;
		} // synchronized(sendlock

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
	public MqBundle sendTo(String to, MqBundle o, long timeout_ms) throws Exception {
		o.from = this.m_id;
		o.to = to;

		return sendTo(o, timeout_ms);
	}

	/**
	 * to='*'
	 * 
	 * @param o
	 * @throws Exception
	 */
	public void sendBroadcast(MqBundle o) throws Exception {
		o.from = this.m_id;
		o.to = "*";
		sendTo(o, 0);

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

	public Object callMethod(String to, long timeout, String method, Object... args) throws Exception {
		MqBundle o = new MqBundle();
		// return sendTo(hashCode(name), msgno, buf, timeout_ms);
		o.from = this.m_id;
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
		
		if( r==null)
			return null;

		if (r.containKey("return") == false)
			return null;

		// return (Object) r.getZip("return");
		return (Object) r.getObj("return");
	}

	public Object callMethod(String to, String method, Object... args) throws Exception {
		return callMethod(to, m_connectionTimeout, method, args);
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

	
	

}
