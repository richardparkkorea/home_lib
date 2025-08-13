package deprecated.lib.net.bridge;

import home.lib.util.TimeUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;

@Deprecated
public class CTcpLinker implements Runnable {

	/**
	 * 
	 * 
	 * 
	 * 
	 */
	CTcpLinker _this = this;

	final String m_id;

	final Object m_lock = new Object();

	final Object m_sendLock = new Object();

	long m_sendIndex = 0;

	long m_privateWhenSend = 0;

	CBundle m_sendOk = null;

	String m_ip;

	int m_port;

	boolean m_alive2 = false;

	public Socket clientSocket = null;

	DataOutputStream outToServer = null;

	DataInputStream inFromServer = null;

	CTcpLinkerListener m_actionListener = new CTcpLinkerListener();

	// ConcurrentLinkedQueue in_msg=new ConcurrentLinkedQueue<CEvent>();
	private long m_timeoutVal = 1000 * 60 * 3;// 3 minute

	long MAX_SOCKET_BUFF_SIZE = CTcpQueue.BASE_BUFFER_SIZE;

	long m_lastWorkTime = 0;

	boolean m_sustainConnection =true;//161014  false;

	long m_sustainConnection_interval = 10 * 1000;//10   

	int m_resendInterval = 1000;// milliseconds

	boolean m_timeoutCheck_isAlive = false;

	boolean m_isReceiving = false;

	/**
	 * Automatic Socket keep-alive If the socket is disconnected then it will try to connect to the TARGET every 3
	 * seconds And connection retries is unlimited until you stop
	 * 
	 * @param b
	 */
	public void sustainConnection(boolean b) {
		m_sustainConnection = b;

		if (m_sustainConnection) {
			if (m_timeoutCheck_isAlive == false) {
				new Thread(new timeoutCheck(clientSocket)).start();
			}
		}
	}

	public void sustainConnection(boolean b, long interval) {

		m_sustainConnection_interval = interval;
		sustainConnection(b);
	}

	/**
	 * if it does not receive any data from dest while m seconds then disconnect the tcp
	 * 
	 * @param m
	 */
	public void setTimeout(long m) {
		m_timeoutVal = m;
	}

	/**
	 * set the number of retransmission when use the sendTo function. (internal use variable)
	 * 
	 * @param n
	 */
	public void setResendInterval(int n) {
		m_resendInterval = n;
	}

	/**
	 * 
	 * @param m
	 */
	public void setMaxBuffer(int m) {
		MAX_SOCKET_BUFF_SIZE = m;
	}

	/**
	 * id == null , give randome ID
	 * 
	 * @param id
	 * @param ip
	 * @param port
	 */
	public CTcpLinker(String id, String ip, int port) throws Exception {
		synchronized (m_lock) {

			if (id == null) {
				// id="" + System.currentTimeMillis()+"_"+this.toString();
				// java.net.InetAddress i = java.net.InetAddress.getLocalHost();
				// id="/"+i.getHostName()+"/"+i.getHostAddress()+"/"+Integer.toHexString(hashCode());
				id = "" + System.currentTimeMillis() + "@" + Integer.toHexString(hashCode());
			}

			m_id = id;
			m_ip = ip;
			m_port = port;
			// m_actionListener = l;
			// MAX_SOCKET_BUFF_SIZE = mb;

			// bindSocket();

		}
	}

	/**
	 * 
	 * @param id
	 * @param ip
	 * @param port
	 * @param l
	 */
	public static CTcpLinker getInstance(String id, String ip, int port, CTcpLinkerListener l) throws Exception {

		CTcpLinker t = new CTcpLinker(id, ip, port);

		t.setActionListener(l);
		t.setMaxBuffer(1024 * 32);// 32k
		t.connect();
		t.register();
		t.setResendInterval(500);// 500ms
		t.sustainConnection(true);

		return t;
	}

	// public CTcpLinker(String id, String ip, int port, CTcpLinkerListener l)
	// throws Exception {
	// this(id, ip, port, l, CTcpQueue.BASE_BUFFER_SIZE);
	// }

	public CTcpLinker(int port) throws Exception {
		this(null, "127.0.0.1", port);
	}

	/**
	 * 
	 * 
	 * @param l
	 */
	public void setActionListener(CTcpLinkerListener l) {
		m_actionListener = l;
	}

	/**
	 * 
	 * @return
	 */
	public Exception connect() throws Exception {
		synchronized (m_lock) {
			try {
				if (isAlive2() == false) {

					// System.out.println("try to recovery("+m_id+")");

					clientSocket = new Socket(m_ip, m_port);

					outToServer = new DataOutputStream(clientSocket.getOutputStream());

					inFromServer = new DataInputStream(clientSocket.getInputStream());

					new Thread(this).start();// 150318 change to self start.

					if (m_protocol_option != 0) {
						register();
					}
				}

			} catch (Exception e) {
				return e;
			}

			return null;
		}

	}

	public void close() {
		synchronized (m_lock) {
			try {
				m_sustainConnection = false;
				clientSocket.close();
			} catch (Exception e) {
				m_actionListener.log(e);
			}
		}
	}

	public String getId2() {
		return m_id;
	}

	public boolean isAlive2() {
		return m_alive2;
	}

	@Override
	public void run() {
		try {
			m_alive2 = true;
			m_actionListener.startUp();
			start_client();
		} catch (Exception e) {

			m_actionListener.log(e);
			// e.printStackTrace();
		} finally {

			m_alive2 = false;
			m_actionListener.finishUp();

			// closesocket
			try {
				clientSocket.close();
			} catch (Exception e2) {

			}
		}

	}

	/**
	 * protocol option 0- do not use protocol 1- used define protocol
	 * 
	 * @param b
	 *            protocol no
	 */
	public void setProtocol(int b) {
		m_protocol_option = b;
	}

	private int m_protocol_option = 1;

	/**
	 * 
	 * register ID to the SERVER
	 * 
	 */
	  
	public CBundle register() throws Exception {

		// synchronized (m_lock) {
		//
		// CBundle e2 = new CBundle();// set this ID in
		// e2.from = getId2();
		// e2.privateSetId = true;
		// // server
		// try {
		// outToServer.write(CTcpQueuePacketPicker.make(e2));
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }

		CBundle e2 = new CBundle();// set this ID in
		e2.from = getId2();
		e2.from_pwd = "echo~";
		e2.privateSetId = true;

		return sendTo(e2, 3000);

	}

	/**
	 * 
	 * @throws Exception
	 */
	public void start_client() throws Exception {

		// about last data
		// CTcpBundle m_lastReturn = new CTcpBundle();
		// m_lastReturn.privateSendIndex = Long.MAX_VALUE;
		ArrayList<CBundle> returnValues = new ArrayList<CBundle>();
		ArrayList<CBundle> receivedValues = new ArrayList<CBundle>();

		m_lastWorkTime = System.currentTimeMillis();

		CTcpQueuePacketPicker ep = new CTcpQueuePacketPicker(MAX_SOCKET_BUFF_SIZE);

		byte[] buf = new byte[1024 * 64];
		while (true) {

			int len = 0;
			try {
				len = inFromServer.read(buf);
			} catch (Exception e) {
				 
				//m_actionListener.log(e);
				m_actionListener.log(0,""+m_id+" socket closed");
			}

			m_lastWorkTime = System.currentTimeMillis();
			if (len > 0) {

			} else {
				clientSocket.close();
				return;
			}

			// System.out.println(
			// clientSocket.toString()+"================>recevie lenght: "+ len
			// );

			//
			//

			try {
				if (m_protocol_option == 0) {
					m_actionListener.actionPerformed(buf, len);
				} else {
					doRead(buf, len, returnValues, receivedValues, ep);
				}
			} catch (Exception e) {
				m_actionListener.log(e);
			}

			// m_actionListener.actionPerformed(re);

			// System.out.println( ">>> client read "+m_id+" "+len );

		}

	}

	private boolean searchInRecentlyReturns(ArrayList<CBundle> returnValues, CBundle re) throws Exception {

		for (int i = 0; i < returnValues.size(); i++) {

			CBundle lr = returnValues.get(i);

			if (lr.to != null && lr.privateSendIndex == re.privateSendIndex && lr.to.equals(re.from)
					&& lr.privateSocket.equals(re.privateSocket) && lr.privateWhenSend == re.privateWhenSend) {

				// System.out.println("do searchInRecentlyReturns at " + System.currentTimeMillis());
				// re-transmitting the previous response value
				outToServer.write(CTcpQueuePacketPicker.make(lr));
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
						if (receviedValues.size() > 1024) {
							receviedValues.remove(0);
						}

						//
						// call listener
						CBundle rs = null;
						re.remote_error = null;
						// m_lastReturn = null;// when the new data is arrive then it set to null
						try {

							rs = m_actionListener.actionPerformed(re);

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

								outToServer.write(CTcpQueuePacketPicker.make(rs));

								byte[] bb = CBundle.To(rs);
								// m_lastReturn = CTcpBundle.From(bb);

								returnValues.add(CBundle.From(bb));
								if (returnValues.size() > 1024) {
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

						// if (re.remote_error == null) {
						// success
						m_sendOk = re;
						// } else {
						// fail
						// m_actionListener.remoteError(re.remote_error);
						// }
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
	 * automatic connection recover
	 * 
	 * @author livingroom-user
	 *
	 */
	private class timeoutCheck implements Runnable {

		Socket m_sk = null;

		public timeoutCheck(Socket sk) {
			m_sk = sk;
		}

		public void run() {

			try {
				m_timeoutCheck_isAlive = true;

				TimeUtil t = new TimeUtil();
				// TimeUtil t2=new TimeUtil();

				do {
					sleep(300);

					//
					try {
						while (m_alive2) {
							if ((m_lastWorkTime + m_timeoutVal) < System.currentTimeMillis()) {

								m_lastWorkTime = System.currentTimeMillis();
								try {
									if (m_sk != null) {
										m_sk.close();
									}
								} catch (Exception e) {
									// do nothing
								}

							}

							sleep(100);
						} // while

					} catch (Exception e) {

					}

					//  
					if (t.end_ms() > m_sustainConnection_interval && m_sustainConnection == true) {
						t.start();

						try {
							if(isAlive2()==false) { 
								connect();
							}
						} catch (Exception e) {

						}
					}

				} while (m_sustainConnection);

			} finally {
				m_timeoutCheck_isAlive = false;
				m_sustainConnection = false;
			}
		}
	};

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
	private CBundle sendTo(CBundle d, long timeout_msec) throws Exception {

		synchronized (m_sendLock) {

			// try {
			//
			m_sendIndex++;
			m_privateWhenSend = System.currentTimeMillis();// add: 150722

			// for (int n = 0; n < m_retransmissionCount + 1; n++) {
			m_sendOk = null;

			synchronized (m_lock) {

				d.from = this.getId2();// replace
				d.privateSocket = this.clientSocket.getLocalSocketAddress().toString();
				d.privateWhenSend = m_privateWhenSend;// add:150722
				d.privateSendIndex = m_sendIndex;
				d.privateCode = 's'; // send type

				// try {
				outToServer.write(CTcpQueuePacketPicker.make(d));
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
					outToServer.write(CTcpQueuePacketPicker.make(d));
					// } catch (Exception e) {
					// wait for reconnect
					// }
				}

				sleep(1);

				if (m_sendOk != null) {

					if (m_sendOk.remote_error != null) {
						throw new RemoteException(" net2java exception=>" + m_sendOk.remote_error);
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

	/**
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
		outToServer.write(b, off, len);
	}

	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
			;
		}
	}

}
