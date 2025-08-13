package deprecated.lib.net.bridge;

import deprecated.lib.net.bridge.CBundle;
import deprecated.lib.net.bridge.CTcpLinkerListener;
 
import deprecated.lib.net.bridge.CTcpQueuePacketPicker;
import home.lib.util.TimeUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;

@Deprecated
public class AsyncLink implements AsyncSocketInterface {

	static {
		new Thread(new AsyncLinkKeeper()).start();
	}

	/*
	 * 
	 * 
	 * 
	 * 
	 */
	AsyncLink _this = this;

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

	// DataOutputStream outToServer = null;

	// DataInputStream inFromServer = null;

	CTcpLinkerListener m_actionListener = null;

	// ConcurrentLinkedQueue in_msg=new ConcurrentLinkedQueue<CEvent>();
	public long m_timeoutVal = 1000 * 60 * 3;// 3 minute

	long MAX_SOCKET_BUFF_SIZE = CTcpQueue.BASE_BUFFER_SIZE;

	long m_lastWorkTime = 0;

	boolean m_keepConnecting = true;// 161014 false;

	long m_keepConnectingRetryInterval = 32;//sec

	int m_resendInterval = 1000;// milliseconds

	boolean m_timeoutCheck_isAlive = false;

	boolean m_isReceiving = false;

	Object m_linkedClassHandle = null;

	long m_sendTimeout = 3000;

	/*
	 * Automatic Socket keep-alive If the socket is disconnected then it will try to connect to the TARGET every 3
	 * seconds And connection retries is unlimited until you stop
	 * 
	 * @param b
	 */

	public void keepConnection(long interval) {

		m_keepConnectingRetryInterval = interval;
		if (interval > 0)
			m_keepConnecting = true;
		else
			m_keepConnecting = false;
	}

	/*
	 * if it does not receive any data from dest while m seconds then disconnect the tcp
	 * 
	 * @param m
	 */
	public void setTimeout(long m) {
		m_timeoutVal = m;
	}

	/*
	 * 
	 * @param m
	 */
	public void setSendTimeout(long m) {
		m_sendTimeout = m;
	}

	/*
	 * set the number of retransmission when use the sendTo function. (internal use variable)
	 * 
	 * @param n
	 */
	public void setResendInterval(int n) {
		m_resendInterval = n;
	}

	/*
	 * 
	 * @param m
	 */
	public void setMaxBuffer(int m) {
		MAX_SOCKET_BUFF_SIZE = m;
	}

	/*
	 * 
	 * 
	 * @param cls
	 */
	public void setClsHandle(Object cls) {
		m_linkedClassHandle = cls;
	}

	/*
	 * id == null , give randome ID
	 * 
	 * @param id
	 * @param ip
	 * @param port
	 */
	public AsyncLink(String id, String ip, int port, Object cls) throws Exception {

		//
		AsyncLinkKeeper.add(this);

		//
		synchronized (m_lock) {

			if (id == null) {
				throw new Exception("not allow 'null' id");
			}

			m_id = id;
			m_ip = ip;
			m_port = port;

			// setActionListener(l);
			setMaxBuffer(1024 * 8);// 32k
			setResendInterval(1000);// 500ms

			m_linkedClassHandle = cls;

		}

	}

	/*
	 * 
	 * 
	 * 
	 */
	protected void finalize() throws Throwable {
		try {
			AsyncLinkKeeper.remove(this);// add this to link keeper
		} finally {
			super.finalize();
		}
	}

	/*
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

		if (args != null || args.length!=0) {

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

	/*
	 * 
	 * 
	 * @param l
	 */
	public void setActionListener(CTcpLinkerListener l) {
		m_actionListener = l;
	}

	/*
	 * 
	 * @return
	 */
	public Exception connect() throws Exception {
		synchronized (m_lock) {
			if (isAlive2() == false) {
				try {
					
					TimeUtil t=new TimeUtil();

					clientSocket = new AsyncSocket(m_ip, m_port, this, (int) MAX_SOCKET_BUFF_SIZE, 3000);

					if (m_protocol_option != 0) {

						CBundle e2 = new CBundle();// set this ID in
						e2.from = getId2();
						e2.from_pwd = "echo~";
						e2.privateSetId = true;

						CBundle b = sendTo(e2, 3000);

						if (b == null) {
							clientSocket.close();
							throw new Exception("id register failed");
						}
						
						 

					}

					System.out.println("AsyncLink.connect time : "+ t.end_ms() +" ms");
					
					if (m_actionListener != null) {
						m_actionListener.startUp();
					}

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

	/*
	 * 
	 * close link (with set keepConnection=false )
	 * 
	 */
	public void close() {
		synchronized (m_lock) {
			try {
				m_keepConnecting = false;
				clientSocket.close();
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

	@Override
	public void recv(AsyncSocket ask,byte[] buf, int len) {

		if (len == 0)
			return;// do nothing

		// System.out.format(" len = %d \r\n", len );

		m_lastWorkTime = System.currentTimeMillis();

		try {
			if (m_protocol_option == 0) {
				if (m_actionListener != null) {
					m_actionListener.actionPerformed(buf, len);
				}
			} else {
				doRead(buf, len, m_returnValues, m_receivedValues, m_ep);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// m_actionListener.actionPerformed(re);

		// System.out.println( ">>> client read "+m_id+" "+len );

	}

	// m_lastReturn.privateSendIndex = Long.MAX_VALUE;
	ArrayList<CBundle> m_returnValues = null;
	ArrayList<CBundle> m_receivedValues = null;
	CTcpQueuePacketPicker m_ep = null;

	@Override
	public void connected(AsyncSocket ask) {
		m_alive2 = true;

		m_lastWorkTime = System.currentTimeMillis();

		m_returnValues = new ArrayList<CBundle>();
		m_receivedValues = new ArrayList<CBundle>();
		m_ep = new CTcpQueuePacketPicker(MAX_SOCKET_BUFF_SIZE);
		System.out.println("async socket connected");

	}

	@Override
	public void disconnected(AsyncSocket ask) {
		m_alive2 = false;

		System.out.println("async socket disconnected");
	}

	/*
	 * protocol option 0- do not use protocol 1- used define protocol
	 * 
	 * @param b
	 *            protocol no
	 */
	public void setProtocol(int b) {
		m_protocol_option = b;
	}

	private int m_protocol_option = 1;

	/*
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
				clientSocket.send(CTcpQueuePacketPicker.make(lr));
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
			CTcpQueuePacketPicker ep) throws Exception {

		// System.out.println(clientSocket.toString()+"====================> client do read 11 ");

		ep.append(buf, len);

		// System.out.println(clientSocket.toString()+"====================> client do read 22");

		CBundle re;

		while ((re = ep.check()) != null) {

			// m_in_events.add(re);

			if (re.to.equals(this.m_id) || re.privateIsBroadcast == true) {
				if (re.privateCode == 's') {

					String from = re.from;
					String to = re.to;
					long si = re.privateSendIndex;
					String skname = re.privateSocket;
					long when = re.privateWhenSend;

					// System.out.println("test==> "+ m_lastReturn);

					// if (m_lastReturn != null && m_lastReturn.to != null && m_lastReturn.privateSendIndex == si
					// && m_lastReturn.to.equals(re.from) && m_lastReturn.privateSocket.equals(re.privateSocket)
					// && m_lastReturn.privateWhenSend == re.privateWhenSend) {
					//
					// // re-transmitting the previous response value
					// outToServer.write(CTcpQueuePacketPicker.make(m_lastReturn));

					if (searchInRecentlyReturns(returnValues, re)) {

					} else if (searchInRecentlyRecevied(receviedValues, re) == false) {// new index data}

						// add to received list
						byte[] bb2 = CBundle.To(re);
						receviedValues.add(CBundle.From(bb2));
						if (receviedValues.size() > 8) {
							receviedValues.remove(0);
						}

						//
						// call listener
						CBundle rs = null;
						re.remote_error = null;
						// m_lastReturn = null;// when the new data is arrive then it set to null
						try {

							if (m_actionListener != null) {
								rs = m_actionListener.actionPerformed(re);
							}

							if (m_linkedClassHandle != null && re.containKey("method") && re.containKey("args")) {

								String method = re.getString("method");
								Object[] args = (Object[]) re.getZip("args");

								Object o = doMethod(m_linkedClassHandle, method, args);

								rs = re;
								rs.setZip("return", o);

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

								clientSocket.send(CTcpQueuePacketPicker.make(rs));

								byte[] bb = CBundle.To(rs);
								// m_lastReturn = CTcpBundle.From(bb);

								returnValues.add(CBundle.From(bb));
								if (returnValues.size() > 8) {
									returnValues.remove(0);
								}

								// System.out.println("set return " + rs.l);
							}
							//
							// else if (rs == null) {
							// rs=re;
							// rs.result=false;
							// rs.from = to;
							// rs.to = from;
							// rs.privateSendIndex = rs.privateSendIndex-0xff;//make different no
							// rs.privateCode = 0;//meaning nothing
							// re.m_map.clear();
							//
							// outToServer.write(CTcpPacketPicker.make(rs));
							// // System.out.println("set return " + rs.l);
							// }
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

	/*
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

	/*
	 * 
	 * If the timeout_msec is zero it is not wait for return, otherwise it will wait for return within timeout_msec
	 * milleseconds.
	 * 
	 * @param d
	 *            bunddle of data
	 * @param timeout_msec
	 * @return null or return value
	 */
	private CBundle sendTo(CBundle d, long timeout_msec) throws Exception {

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

				// try {
				clientSocket.send(CTcpQueuePacketPicker.make(d));
				// } catch (Exception e) {
				// wait for reconnect
				// }
				// m_sendOk = null;

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

				if (resend.end_ms() > m_resendInterval) {
					resend.start();

					// try {
					clientSocket.send(CTcpQueuePacketPicker.make(d));
					// } catch (Exception e) {
					// wait for reconnect
					// }
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

			// // }// for(n
			// if (m_sendOk.remote_error != null) {
			// throw new Exception("RemoteException:" + m_sendOk.remote_error);
			// }
			// return m_sendOk;

			return null;
		} // synchronized(sendlock

	}

	public CBundle sendBroadcast(CBundle o) throws Exception {
		o.privateIsBroadcast = true;
		return sendTo("*", o, 0);
	}

	public CBundle sendTo(String to, CBundle o) throws Exception {
		return sendTo(to, o, 0);
	}

	public CBundle sendTo(String to, CBundle o, long timeout_ms) throws Exception {
		// return sendTo(hashCode(name), msgno, buf, timeout_ms);
		o.from = this.m_id;
		o.to = to;

		return sendTo(o, timeout_ms);
	}

	/*
	 * 
	 * @param to
	 * @param timeout
	 * @param method
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public Object callMethod(String to, String method, Object... args) throws Exception {
		CBundle o = new CBundle();
		// return sendTo(hashCode(name), msgno, buf, timeout_ms);
		o.from = this.m_id;
		o.to = to;

		if (to.equals("*"))
			new RemoteException("broadcasting dose not support!");

		o.setLong("cmd", 0);
		o.setString("method", method);
		o.setZip("args", args);

		CBundle r = sendTo(o, m_sendTimeout);

		if (r.containKey("return") == false)
			return null;

		return (Object) r.getZip("return");
	}

	/*
	 * Writes <code>len</code> bytes from the specified byte array starting at offset <code>off</code> to the underlying
	 * output stream. If no exception is thrown, the counter <code>written</code> is incremented by <code>len</code>.
	 *
	 * @param b
	 *            the data.
	 * @param off
	 *            the start offset in the data.
	 * @param len
	 *            the number of bytes to write.
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @see java.io.FilterOutputStream#out
	 */
	public void sendTo(byte[] b, int off, int len) throws Exception {
		// outToServer.write(b, off, len);

		if (clientSocket == null || clientSocket.isAlive() == false)
			new Exception("socket is closed");

		clientSocket.send(Arrays.copyOfRange(b, off, len));

	}

	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
			;
		}
	}

}
