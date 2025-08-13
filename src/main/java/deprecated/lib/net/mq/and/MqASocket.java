package deprecated.lib.net.mq.and;

import home.lib.io.FilenameUtils;
import home.lib.lang.UserException;
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

 

import deprecated.lib.net.mq.MqBundle;
import deprecated.lib.net.mq.MqPacketPicker;
import deprecated.lib.net.mq.MqSocket;

/**
 * 
 * normal socket for android
 * 
 * @author richardpark
 *
 */
public class MqASocket implements Runnable {

	/**
	 * 
	 * 
	 */
	static {
		// add to keeper
		new Thread(new MqASocketKeeper()).start();

	}
	/** 
	 * 
	 * 
	 * 
	 * 
	 */
	MqASocket _this = this;

	final String m_id;

	final Object m_lock = new Object();

	final Object m_sendLock = new Object();

	long m_sendIndex = 0;

	long m_privateWhenSend = 0;

	MqBundle m_sendOk = null;

	String m_ip;

	int m_port;

	boolean m_alive2 = false;

	public Socket clientSocket = null;

	DataOutputStream outToServer = null;

	DataInputStream inFromServer = null;

	MqASocketListener m_actionListener = null;

	// ConcurrentLinkedQueue in_msg=new ConcurrentLinkedQueue<CEvent>();
	public long m_timeoutVal = 1000 * 60 * 3;// 3 minute
	
	public int reconnectIntervalMs=10*1000;
	
	TimeUtil try2connect = new TimeUtil();

	long MAX_SOCKET_BUFF_SIZE = 8 * 1024;// .BASE_BUFFER_SIZE;

	long m_lastWorkTime = 0;

	//boolean m_sustainConnection = true;// 161014 false;

	// long m_sustainConnection_interval = 10 * 1000;// 10 

	int m_resendInterval = 1000;// milliseconds

	boolean m_timeoutCheck_isAlive = false;

	boolean m_isReceiving = false;

	long m_connectTimeout = 6000;

	/**
	 * Automatic Socket keep-alive If the socket is disconnected then it will try to connect to the TARGET every 3
	 * seconds And connection retries is unlimited until you stop
	 * 
	 * @param b
	 */
//	public void keepConnection(boolean b) {
//		m_sustainConnection = b;
//
//		if (m_sustainConnection) {
//			if (m_timeoutCheck_isAlive == false) {
//				new Thread(new timeoutCheck(clientSocket)).start();
//			}
//		}
//	}
	Object m_userObj = null;

	public void setUserObject(Object o) {
		m_userObj = o;
	}

	public Object getUserObject() {
		return m_userObj;
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
	public void setSocketBufferSize(int m) {
		MAX_SOCKET_BUFF_SIZE = m;

	}

	/**
	 * id == null , give randome ID
	 * 
	 * @param id
	 * @param ip
	 * @param port
	 */
	public MqASocket(String id, String ip, int port, MqASocketListener ltr) throws Exception {
		synchronized (m_lock) {

			if (id == null) {
				// id="" + System.currentTimeMillis()+"_"+this.toString();
				// java.net.InetAddress i = java.net.InetAddress.getLocalHost();
				// id="/"+i.getHostName()+"/"+i.getHostAddress()+"/"+Integer.toHexString(hashCode());
				id = "" + System.currentTimeMillis() + "@" + Integer.toHexString(hashCode());
			}

			if (ip == null)
				ip = "127.0.0.1";

			m_id = id;
			m_ip = ip;
			m_port = port;
			m_actionListener = ltr;

		}
	}

	/**
	 * 
	 * 
	 * @param l
	 */
	public void setActionListener(MqASocketListener l) {
		m_actionListener = l;
	}

	/**
	 * 
	 * @return
	 */
	public Exception connect(long timeout) throws Exception {
		synchronized (m_lock) {
			try {
				if (isAlive() == false) {

					m_connectTimeout = timeout;

					// System.out.println("try to recovery("+m_id+")");

					clientSocket = new Socket(m_ip, m_port);

					outToServer = new DataOutputStream(clientSocket.getOutputStream());

					inFromServer = new DataInputStream(clientSocket.getInputStream());

					new Thread(this).start();// 150318 change to self start.
					
					TimeUtil t=new TimeUtil();
					while(t.end_ms()<timeout && isAlive()==false) {
						MqSocket.sleep(10);
					}

					MqBundle b = ping(timeout);

					if (b == null) {
						clientSocket.close();
						throw new Exception("MqSocket- ping check failed");
					}
					
					MqASocketKeeper.add(this);

				}

			} catch (Exception e) {
				return e;
			}

			return null;
		}

	}

	public boolean m_debug = false;

	public void debug(String fmt, Object... args) {
		if (m_debug == false)
			return;

		try {
			System.out.println("MqASocket-" + String.format(fmt, args));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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

	public void close() {
		synchronized (m_lock) {
			try {
				
				MqASocketKeeper.remove(this);
				//m_sustainConnection = false;
				clientSocket.close();
				
			} catch (Exception e) {
				m_actionListener.log(this, e);
			}
		}
	}

	public String getId() {
		return m_id;
	}

	public boolean isAlive() {
		return m_alive2;
	}

	@Override
	public void run() {
		try {
			m_alive2 = true;

			//keepConnection(true);

			m_actionListener.connected(this);
			start_client();
		} catch (Exception e) {

			m_actionListener.log(this, e);
			// e.printStackTrace();
		} finally {

			m_alive2 = false;
			m_actionListener.disconnected(this);

			// closesocket
			try {
				clientSocket.close();
			} catch (Exception e2) {

			}
		}

	}

	/**
	 * 
	 * @throws Exception
	 */
	public void start_client() throws Exception {

		// about last data
		// CTcpBundle m_lastReturn = new CTcpBundle();
		// m_lastReturn.privateSendIndex = Long.MAX_VALUE;
		ArrayList<MqBundle> returnValues = new ArrayList<MqBundle>();
		ArrayList<MqBundle> receivedValues = new ArrayList<MqBundle>();

		m_lastWorkTime = System.currentTimeMillis();

		MqPacketPicker ep = new MqPacketPicker(MAX_SOCKET_BUFF_SIZE);

		byte[] buf = new byte[1024 * 64];
		while (true) {

			int len = 0;
			try {
				len = inFromServer.read(buf);
			} catch (Exception e) {

				// m_actionListener.log(e);
				m_actionListener.log(this, 0, "" + m_id + " socket closed");
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

				doRead(buf, len, returnValues, receivedValues, ep);

			} catch (Exception e) {
				m_actionListener.log(this, e);
			}

			// m_actionListener.actionPerformed(re);

			// System.out.println( ">>> client read "+m_id+" "+len );

		}

	}

	private boolean searchInRecentlyReturns(ArrayList<MqBundle> returnValues, MqBundle re) throws Exception {

		for (int i = 0; i < returnValues.size(); i++) {

			MqBundle lr = returnValues.get(i);

			if (lr.to != null && lr.privateSendIndex == re.privateSendIndex && lr.to.equals(re.from)
					&& lr.privateSocket.equals(re.privateSocket) && lr.privateWhenSend == re.privateWhenSend) {

				// System.out.println("do searchInRecentlyReturns at " + System.currentTimeMillis());
				// re-transmitting the previous response value
				outToServer.write(MqPacketPicker.make(lr));
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

		// System.out.println(clientSocket.toString()+"====================> client do read 11 ");

		ep.append(buf, len);

		// System.out.println(clientSocket.toString()+"====================> client do read 22");

		MqBundle re;

		while ((re = ep.check()) != null) {

			// m_in_events.add(re);

			// if (re.to.equals(this.m_id) || re.privateIsBroadcast == true) {
			if (FilenameUtils.wildcardMatch(this.m_id, re.to)) {
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
					// outToServer.write(MqPacketPicker.make(m_lastReturn));

					if (searchInRecentlyReturns(returnValues, re)) {

					} else if (searchInRecentlyRecevied(receviedValues, re) == false) {// new index data}

						// add to received list
						byte[] bb2 = MqBundle.To(re);
						receviedValues.add(MqBundle.From(bb2));
						if (receviedValues.size() > 8) {
							receviedValues.remove(0);
						}

						//
						// call listener
						MqBundle rs = null;
						re.remoteErrorMessage = null;
						// m_lastReturn = null;// when the new data is arrive then it set to null
						try {

							rs = m_actionListener.actionPerformed(this, re);

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

								outToServer.write(MqPacketPicker.make(rs));

								byte[] bb = MqBundle.To(rs);
								// m_lastReturn = CTcpBundle.From(bb);

								returnValues.add(MqBundle.From(bb));
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

						// if (re.remoteErrorMessage == null) {
						// success
						m_sendOk = re;
						// } else {
						// fail
						// m_actionListener.remoteError(re.remoteErrorMessage);
						// }
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
	 * automatic connection recover
	 * 
	 * @author livingroom-user
	 *
	 */
//	private class timeoutCheck implements Runnable {
//
//		Socket m_sk = null;
//
//		public timeoutCheck(Socket sk) {
//			m_sk = sk;
//		}
//
//		public void run() {
//
//			try {
//				m_timeoutCheck_isAlive = true;
//
//				TimeUtil t = new TimeUtil();
//				// TimeUtil t2=new TimeUtil();
//
//				do {
//					sleep(300);
//
//					//
//					try {
//						while (isAlive()) {
//
//							sendPing();
//
//							if ((m_lastWorkTime + m_timeoutVal) < System.currentTimeMillis()) {
//
//								m_lastWorkTime = System.currentTimeMillis();
//								try {
//									if (m_sk != null) {
//										m_sk.close();
//									}
//								} catch (Exception e) {
//									// do nothing
//								}
//
//							}
//
//							sleep(100);
//						} // while
//
//						// �ڵ� ���� ���� ���
//						if (t.end_ms() > 10 * 1000 && m_sustainConnection == true) {
//							t.start();
//
//							if (isAlive() == false) {
//								connect(m_connectTimeout);
//							}
//
//						}
//
//					} catch (Exception e) {
//
//					}
//
//				} while (m_sustainConnection);
//
//			} finally {
//				m_timeoutCheck_isAlive = false;
//				m_sustainConnection = false;
//			}
//		}
//	};

	
 
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
		
		if( isAlive()==false)
			throw new UserException("socket is not alive");

		synchronized (m_sendLock) {

			// try {
			//
			m_sendIndex++;
			m_privateWhenSend = System.currentTimeMillis();// add: 150722

			// for (int n = 0; n < m_retransmissionCount + 1; n++) {
			m_sendOk = null;

			synchronized (m_lock) {

				d.from = this.getId();// replace
				d.privateSocket = "android";//this.clientSocket.getLocalSocketAddress().toString();
				d.privateWhenSend = m_privateWhenSend;// add:150722
				d.privateSendIndex = m_sendIndex;
				d.privateCode = 's'; // send type

				// try {
				outToServer.write(MqPacketPicker.make(d));
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
					outToServer.write(MqPacketPicker.make(d));
					// } catch (Exception e) {
					// wait for reconnect
					// }
				}

				sleep(1);

				if (m_sendOk != null) {

					if (m_sendOk.remoteErrorMessage != null) {
						throw new RemoteException(" MqASocket exception=>" + m_sendOk.remoteErrorMessage);
					}
					return m_sendOk;
				}

				if (isAlive() == false)
					return null;
			}

			// // }// for(n
			// if (m_sendOk.remoteErrorMessage != null) {
			// throw new Exception("RemoteException:" + m_sendOk.remoteErrorMessage);
			// }
			// return m_sendOk;

			return null;
		} // synchronized(sendlock

	}

	public MqBundle sendBroadcast(MqBundle o) throws Exception {
		// o.privateIsBroadcast = true;
		return sendTo("*", o, 0);
	}

	public MqBundle sendTo(String to, MqBundle o) throws Exception {
		return sendTo(to, o, 0);
	}

	public MqBundle sendTo(String to, MqBundle o, long timeout_ms) throws Exception {
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
