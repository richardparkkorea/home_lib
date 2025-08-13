package home.lib.log;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import home.lib.lang.Timer2;
import home.lib.lang.Timer2Task;
import home.lib.lang.UserException;
import home.lib.util.TimeUtil;

final public class MyLogger implements ILogger {

	final public static int VERBOSE = 0x00000001;
	final public static int LOG = 0x00000002;
	final public static int DEBUG = 0x00000004;
	final public static int WARNING = 0x00000008;
	final public static int EXCEPTION = 0x00000010;

	private DatagramSocket m_ds = null;

	// private int m_dport = 1234;// debug port
	private Map<String, Integer> m_ports = new HashMap<String, Integer>();

	// private String m_from = "";

	private String m_encode = null;// "utf-8";

	private Map<Socket, String> m_tcpClient = new HashMap<Socket, String>();

	private Timer2 m_timer = null;

	ServerSocket m_serverSocket = null;

	// /**
	// * return new isntance<br>
	// * it will ignore port number 0
	// *
	// * @param ip
	// * @param port
	// * @return
	// */
	// public static MyLogger _new(String ip, int port) {
	// try {
	// // return new ULogger(ip, 0);
	// return new MyLogger(ip, port);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// return null;
	// }

	/**
	 * it will ignore port number 0
	 * 
	 * @param ip
	 * @param port
	 * @return
	 * @throws Exception
	 */

	public MyLogger() {

	}

	public void close() {

		try {
			m_timer.cancel();
		} catch (Exception e) {
			;
		}
		try {
			m_serverSocket.close();
		} catch (Exception e) {
			;
		}

		try {
			m_ds.close();
		} catch (Exception e) {
			;
		}

		m_ports.clear();
		m_timer = null;
		m_serverSocket = null;
		m_ds = null;
		m_tcpClient.clear();

	}

	public MyLogger startUdp() throws Exception {

		if (m_ds == null) {
			m_ds = new DatagramSocket();
		}

		return this;
	}

	public int getTcpClientCount() {
		return m_tcpClient.size();
	}

	public int getUdpPortCount() {
		return m_ports.size();
	}

	/**
	 * 
	 * 
	 * @param port
	 * @return
	 * @throws Exception
	 */
	public MyLogger startTcpServer(int port) throws Exception {

		if (m_timer != null)
			return this;

		m_timer = new Timer2().schedule(new Timer2Task() {

			@Override
			public void start(Timer2 tmr) {

			}

			@Override
			public void run(Timer2 tmr) {

				try {
					m_serverSocket = new ServerSocket(port);

					while (true) {
						Socket socket = m_serverSocket.accept();

						new ClientThread(socket);

					} // while

				} catch (Exception e) {
					e.printStackTrace();
				} finally {

					try {
						m_serverSocket.close();
					} catch (IOException e) {
						;
					}
					m_serverSocket = null;
				}

			}

			@Override
			public void stop(Timer2 tmr) {

			}

		}, 10);

		return this;

	}

	/**
	 * 
	 * @author richard
	 *
	 */
	private class ClientThread {
		public ClientThread(Socket socket) {

			Timer2 tm = new Timer2();
			tm.schedule(new Timer2Task() {

				@Override
				public void start(Timer2 tmr) {

				}

				@Override
				public void run(Timer2 tmr) {

					System.out.println("ulogger] start new client thread ");
					byte[] buf = new byte[256];
					try {
						synchronized (m_tcpClient) {
							m_tcpClient.put(socket, socket.getRemoteSocketAddress().toString());
						} // sync

						try {
							InputStream is = socket.getInputStream();
							while (true) {

								int n = is.read(buf);
								if (n < 0) {
									socket.close();
									return;
								}

								TimeUtil.sleep(100);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}

					} finally {

						synchronized (m_tcpClient) {
							m_tcpClient.remove(socket);
						} // sync
						
						tmr.cancel();//self destroy
					}

				}

				@Override
				public void stop(Timer2 tmr) {

				}

			}, 10);

		}
	}

	/**
	 * 
	 * @param e
	 * @return
	 */
	public MyLogger setEncode(String e) {
		m_encode = e;
		return this;
	}

	/**
	 * port range( 0 ~ 65535 )
	 * 
	 * @param port
	 */
	public int addUdpTarget(String name, int port) {
		synchronized (m_ports) {

			if (0 <= port && port < 65535) {
				if (m_ports.containsKey(name) == false) {
					m_ports.put(name, port);
				}
			}

			return m_ports.size();
		} // sync
	}

	public int getPort(String name) {
		synchronized (m_ports) {
			if (m_ports.containsKey(name)  ) {
				return m_ports.get(name).intValue();
			}

		} // sync
		return -1;
	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public int removeUdpTargetByBame(String name) {
		synchronized (m_ports) {

			m_ports.remove(name);

			m_ports.keySet().removeAll(Collections.singleton(name));

			return m_ports.size();
		} // sync
	}

	/**
	 * 
	 * @param port
	 * @return
	 */
	public int removeUdpTargetByPort(int port) {
		synchronized (m_ports) {

			m_ports.values().removeAll(Collections.singleton(port));

			// test 1
			m_ports.values().removeIf(val -> val.equals(port));

			return m_ports.size();

		} // sync
	}

	// /**
	// *
	// * @param name
	// * @param ip
	// * @param port
	// * @throws Exception
	// */
	// public void addTcpTarget(String name, String ip, int port) throws Exception {
	// synchronized (m_ports) {
	//
	// Socket sk = new Socket(ip, port);
	//
	// new ClientThread(sk);
	//
	// } // sync
	// }

	/**
	 * 
	 * @param udpTargetName
	 * @param s
	 * @return
	 */
	public int udp(String udpTargetName, String s) {

		int sendCount = 0;

		try {

			String str = s;

			// String str=s;

			InetAddress iaddr = InetAddress.getByName("127.0.0.1");

			synchronized (m_ports) {
				//for (Map.Entry<String, Integer> e : m_ports.entrySet()) {

					//if (udpTargetName == null || e.getKey().equals(udpTargetName)) {
				
				if (m_ports.containsKey(udpTargetName)  ) {
					int port= m_ports.get(udpTargetName).intValue();
					

						byte[] bs = null;

						if (m_encode == null) {
							bs = str.getBytes();
						} else {
							bs = str.getBytes(m_encode);
						}

						DatagramPacket dp = new DatagramPacket(bs, bs.length, iaddr,port);

						if (m_ds != null) {
							m_ds.send(dp);
							sendCount++;
						}

					//} // if
				} //
			} // sync

			//
			//
			synchronized (m_tcpClient) {
				for (Entry<Socket, String> e : m_tcpClient.entrySet()) {

					try {

						if (udpTargetName == null || e.getValue().equals(udpTargetName)) {

							byte[] bs = null;

							if (m_encode == null) {
								bs = str.getBytes();
							} else {
								bs = str.getBytes(m_encode);
							}

							e.getKey().getOutputStream().write(bs);

						}

					} catch (Exception ex) {
						;
					}

				} // for

			} // sync

		} catch (Exception e) {
			e.printStackTrace();
		}

		// Log.fl(format, args);

		return sendCount;

	}

	/**
	 * 
	 */
	public int l(String fmt, Object... args) {

		return udp(null, String.format(fmt, args));

	}

	/**
	 * 
	 */
	public int e(Exception e) {

		return udp(null, UserException.getStackTrace(e));
	}

	@Override
	public int l(int level, String from, String fmt, Object... args) {

		return udp(null, String.format(fmt, args));
	}

	@Override
	public int e(int level, String from, Exception e) {

		return udp(null, UserException.getStackTrace(e));
	}

}
