package home.lib.log;

import java.io.*;

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import home.lib.lang.UserException;
import home.lib.util.TimeUtil;


@Deprecated 
final public class ULoggerReceiver

{

	ThreadPoolExecutor m_executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

	private Object _lock = new Object();
	private DatagramSocket m_server = null;
	private boolean m_alive = false;
	private Map<ULoggerListener, ULoggerReceiver> m_listener = new HashMap<ULoggerListener, ULoggerReceiver>();

	private Gson m_gson = new GsonBuilder().create();

	private String m_encode = null;

	private int m_port = -1;

	/**
	 * 
	 */
	public ULoggerReceiver() {

	}

	public ULoggerReceiver setEncode(String s) {
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
		if (m_server == null)
			return -1;// err

		// return m_server.getPort();
		return m_port;
	}

	public ULoggerReceiver addListener(ULoggerListener l) {

		synchronized (m_listener) {
			if (m_listener.containsKey(l) == false) {
				m_listener.put(l, this);
			}
		} // sync

		return this;
	}

	public ULoggerReceiver removeListener(ULoggerListener l) {

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

	public Future<String> start(int p) throws Exception {
		return start(p, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>());
	}

	
	
	
	
	/**
	 * 
	 * 
	 * @param p
	 * @param and
	 * @param or
	 * @param deny
	 * @throws Exception
	 */
	public Future<String> start(int p, ArrayList<String> and, ArrayList<String> or, ArrayList<String> deny)
			throws Exception

	{

		if (isAlive()) {
			throw new UserException("udp is alive");
		}

		Future<String> rs = m_executor.submit(() -> {

			try {

				synchronized (_lock) {

					if (isAlive())
						return null;

					m_alive = true;
				} // lock

				m_server = new DatagramSocket(p);
				m_port = p;

				byte[] rxData = new byte[1024 * 64];

				byte[] txData = new byte[1024 * 64];

				while (true)

				{

					DatagramPacket rxPacket = new DatagramPacket(rxData, rxData.length);

					m_server.receive(rxPacket);

					try {
						// System.out.println( "rx.len="+ rxPacket.getLength() );

						if (rxPacket.getLength() > 0) {
							byte rx[] = Arrays.copyOf(rxPacket.getData(), rxPacket.getLength());

							// // set default
							ULoggerData d = new ULoggerData();
							d.from = "-";
							d.when = TimeUtil.now();
							d.level = ULogger.LOG;
							d.str = new String(rx);

							//
							try {
								// is jon string?
								if (d.str.startsWith("{") && d.str.endsWith("}")) {
									if (m_encode != null)
										d = m_gson.fromJson(new String(rx, m_encode), ULoggerData.class);
									else
										d = m_gson.fromJson(new String(rx), ULoggerData.class);
								}

							} catch (Exception e) {

							}

							String s = "[" + d.from + "] " + d.str;

							InetAddress ip = rxPacket.getAddress();

							int port = rxPacket.getPort();

							boolean isok = false;

							// if nothing is inputed
							if (and.size() == 0 && or.size() == 0) {
								isok = true;
							}

							// check or
							for (String a : or) {
								if (s.indexOf(a) != -1) {
									isok = true;
								}
							}

							// check and
							if (or.size() == 0) {
								isok = true;// set true , first
							}

							for (String a : and) {
								if (s.indexOf(a) == -1) {
									isok = false;
								}
							}

							// check deny
							if (isok == true) {
								for (String a : deny) {
									if (s.indexOf(a) != -1) {
										isok = false;
									}
								}
							}

							if (isok) {

								synchronized (m_listener) {

									for (ULoggerListener l : m_listener.keySet()) {

										try {
											l.ulogReceive(rxPacket, d.when, d.from, d.level, d.str);
										} catch (Exception ee) {
											ee.printStackTrace();
										}
									} // for

								} // sync

							}
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

			} catch (Exception e) {

				// e.printStackTrace();
				System.out.format("uloggerreceiver-2  %s\r\n", e);

			} finally {

				close();
				m_alive = false;
			}

			// return "ok";
			return "o";
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
			if (m_server != null)
				m_server.close();
		} catch (Exception e) {

		}

		m_server = null;
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
	 * 		return the available port number otherwise return error (-1)
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
