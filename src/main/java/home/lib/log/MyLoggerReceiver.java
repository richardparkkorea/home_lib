package home.lib.log;

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import home.lib.lang.UserException;
import home.lib.util.TimeUtil;

final public class MyLoggerReceiver

{

	MyLoggerReceiver _this = this;

	ThreadPoolExecutor m_executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

	private Object _lock = new Object();
	private DatagramSocket m_udp = null;
	private Socket m_tcp = null;
	private boolean m_alive = false;
	private Map<MyLoggerListener, MyLoggerReceiver> m_listener = new HashMap<MyLoggerListener, MyLoggerReceiver>();

	private String m_encode = null;

	private int m_port = -1;

	private String m_ip = null;

	ArrayList<String> m_and = new ArrayList<String>();
	ArrayList<String> m_or = new ArrayList<String>();
	ArrayList<String> m_deny = new ArrayList<String>();

	/**
	 * 
	 */
	public MyLoggerReceiver() {

	}

	public MyLoggerReceiver setEncode(String s) {
		if (s == null)
			return this;

		m_encode = s;
		return this;
	}

	/**
	 * 
	 * @return
	 */
	public int getPort() {
		if (m_udp == null)
			return -1;// err

		// return m_server.getPort();
		return m_port;
	}

	public MyLoggerReceiver addListener(MyLoggerListener l) {

		synchronized (m_listener) {
			if (m_listener.containsKey(l) == false) {
				m_listener.put(l, this);
			}
		} // sync

		return this;
	}

	public MyLoggerReceiver removeListener(MyLoggerListener l) {

		synchronized (m_listener) {
			m_listener.remove(l);
		}

		return this;
	}

	/**
	 * 
	 * @return
	 */
	public int getListenerCount() {
		return m_listener.size();
	}

	protected Future<String> start(int p) throws Exception {
		return start(null, p, "");
	}

	protected Future<String> start(int p, ArrayList<String> and, ArrayList<String> or, ArrayList<String> deny)
			throws Exception {

		String search_word = "";

		for (String a : or)
			search_word += " " + a;

		for (String a : and)
			search_word += " +" + a;

		for (String a : deny)
			search_word += " -" + a;

		return start(null, p, search_word);
	}

	/**
	 * 
	 * @param search_word
	 */
	public void setFilters(String search_word)

	{

		ArrayList<String> and = new ArrayList<String>();
		ArrayList<String> or = new ArrayList<String>();
		ArrayList<String> deny = new ArrayList<String>();

		String[] ws = search_word.trim().split("\\s+");
		for (String w : ws) {

			if (w.length() == 0)
				continue;

			if (w.charAt(0) != '+' && w.charAt(0) != '-') {// or

				or.add(w);

			}

		}

		for (String w : ws) {

			if (w.length() < 1)
				continue;

			if (w.charAt(0) == '-') {// not

				deny.add(w.substring(1));

			} else if (w.charAt(0) == '+') {// and

				and.add(w.substring(1));

			}

		}

		m_and.clear();
		m_and.addAll(and);

		m_or.clear();
		m_or.addAll(or);

		m_deny.clear();
		m_deny.addAll(deny);

	}

	public boolean checkWords(String s) {

		if (s == null)
			return false;

		if (s.trim().length() == 0)
			return false;

		// InetAddress ip = rxPacket.getAddress();
		// int port = rxPacket.getPort();

		boolean isok = false;

		// if nothing is inputed
		if (m_and.size() == 0 && m_or.size() == 0) {
			isok = true;
			// System.out.format("and or : length==0 \n");
		}

		// check or
		for (String a : m_or) {
			if (s.indexOf(a) != -1) {
				isok = true;
				// System.out.format(" or : (%s) == (%s) \n", s,a );
			}
		}

		// check and
		if (m_or.size() == 0) {
			isok = true;// set true , first
			// System.out.format(" or : length==0 \n");
		}

		for (String a : m_and) {
			if (s.indexOf(a) == -1) {
				isok = false;
			}
		}

		// check deny
		if (isok == true) {
			for (String a : m_deny) {
				if (s.indexOf(a) != -1) {
					isok = false;
				}
			}
		}

		if (isok) {

			return true;

		}

		return false;

	}

	/**
	 * 
	 * @param port
	 * @param sw
	 * @return
	 * @throws Exception
	 */
	public Future<String> startUdpMode(int port, String sw) throws Exception {

		return start(null, port, sw);
	}

	/**
	 * 
	 * @param ip
	 * @param port
	 * @param sw
	 * @return
	 * @throws Exception
	 */
	public Future<String> startTcpMode(String ip, int port, String sw) throws Exception {

		return start(ip, port, sw);
	}

	/**
	 * 
	 * @param ip
	 * @param p
	 * @param search_word
	 * @return
	 * @throws Exception
	 */
	protected Future<String> start(String ip, int p, String search_word) throws Exception

	{

		if (isAlive()) {
			throw new UserException("udp is alive");
		}

		Future<String> rs = m_executor.submit(() -> { // thread pool

			setFilters(search_word);

			try {

				if (ip == null || ip.trim().length() == 0)
					m_udp = new DatagramSocket(p);
				else
					m_tcp = new Socket(ip, p);

				synchronized (_lock) {

					if (isAlive())
						return null;

					m_alive = true;
				} // lock

				m_ip = ip;
				m_port = p;

				byte[] rxData = new byte[1024 * 8];

				// byte[] txData = new byte[1024 * 64];

				while (true)

				{

					DatagramPacket rxPacket = null;

					if (m_udp != null) {
						rxPacket = new DatagramPacket(rxData, rxData.length);
						m_udp.receive(rxPacket);
					} else {
						int r = m_tcp.getInputStream().read(rxData);

						rxPacket = new DatagramPacket(rxData, r);
						rxPacket.setLength(r);
					}

					try {
						// System.out.println( "rx.len="+ rxPacket.getLength() );

						if (rxPacket.getLength() > 0) {
							byte rx[] = Arrays.copyOf(rxPacket.getData(), rxPacket.getLength());

							String ss = null;

							//
							try {
								// is jon string?

								if (m_encode != null)
									ss = new String(rx, m_encode);
								else
									ss = new String(rx);

							} catch (Exception e) {

							}

							String lines[] = ss.split("\\r?\\n");
							for (String s : lines) {

								if (s.trim().length() == 0)
									continue;

								// InetAddress ip = rxPacket.getAddress();
								// int port = rxPacket.getPort();

								boolean isok = false;

								// if nothing is inputed
								if (m_and.size() == 0 && m_or.size() == 0) {
									isok = true;
									// System.out.format("and or : length==0 \n");
								}

								// check or
								for (String a : m_or) {
									if (s.indexOf(a) != -1) {
										isok = true;
										// System.out.format(" or : (%s) == (%s) \n", s,a );
									}
								}

								// check and
								if (m_or.size() == 0) {
									isok = true;// set true , first
									// System.out.format(" or : length==0 \n");
								}

								for (String a : m_and) {
									if (s.indexOf(a) == -1) {
										isok = false;
									}
								}

								// check deny
								if (isok == true) {
									for (String a : m_deny) {
										if (s.indexOf(a) != -1) {
											isok = false;
										}
									}
								}

								if (isok) {

									String when = TimeUtil.now();
									String from = "";
									if (m_udp != null) {
										from = "" + rxPacket.getSocketAddress();

									}
									if (m_tcp != null) {
										from = "" + m_tcp.getRemoteSocketAddress();
									}

									synchronized (m_listener) {

										for (MyLoggerListener l : m_listener.keySet()) {

											try {

												l.ulogReceive(_this, when, from, 0, s);
											} catch (Exception ee) {
												ee.printStackTrace();
											}
										} // for

									} // sync

								}

							} // for
						}

					} catch (Exception e) {
						// e.printStackTrace();
						System.out.format("uloggerreceiver-1  %s\r\n", e);
					}
					// s = s.toUpperCase ();

					// txData = s.getBytes ();

					// DatagramPacket txPacket = new DatagramPacket (txData, txData.length, ip, port);

					// m_server.send(txPacket);

				} // while

			} finally {

				close();
				m_alive = false;

				synchronized (m_listener) {

					for (MyLoggerListener l : m_listener.keySet()) {

						try {

							l.ulogClosed(_this);
						} catch (Exception ee) {
							ee.printStackTrace();
						}
					} // for
				} // sync

			}
		} // v
		);

		return rs;
	}

	/**
	 * 
	 * 
	 */
	public void close() {
		try {
			if (m_udp != null)
				m_udp.close();
		} catch (Exception e) {

		}

		try {
			if (m_tcp != null)
				m_tcp.close();
		} catch (Exception e) {

		}

		m_udp = null;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isAlive() {

		synchronized (_lock) {
			return m_alive;
		}
	}

	/**
	 * 
	 * @param sec
	 */
	public void waitToStart(int sec) {
		TimeUtil t = new TimeUtil();

		while (t.end_sec() < sec && isAlive() == false) {
			TimeUtil.sleep(10);
		} // while
	}

	/**
	 * 
	 * @param start
	 * @param end
	 * @return
	 * 
	 *         return the available port number otherwise return error (-1)
	 */
	public static int findLocalPortFree(int start, int end) {

		// synchronized (_lock) {
		for (int p = start; p < end; p++) {

			DatagramSocket u = null;

			try {
				new DatagramSocket(p).close();

				return p;
			} catch (Exception e) {
				u = null;
				continue;
			}

		} // for

		return -1;
		// }
	}
}
