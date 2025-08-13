package home.lib.net.tms.mqtt3ssl;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import home.lib.lang.Timer2;
import home.lib.lang.Timer2Task;
import home.lib.lang.UserException;
import home.lib.util.NetUtil;
import home.lib.util.TimeUtil;

/**
 * 
 * guild site link: http://rox-xmlrpc.sourceforge.net/niotut/#The server
 * 
 * 
 * 
 * 
 * 
 * 
 * 20201102
 * 
 * modifyed of read() and write() parts.( improve imbalance when processing huge data )
 * 
 * 
 * 
 * 
 * @author livingroom-user
 *
 */
final public class MqttServerSocket {

	class UserData {
		boolean isAccept = false;
		Object userobj = null;
		// double timeout = 60 * 60;
		TimeUtil tmrLastRecv = new TimeUtil();

		UserData(boolean accept, Object uo) {
			isAccept = accept;
			userobj = uo;
			// timeout = t;
		}
	}

	MqttServerSocket _this = this;

	// private int debugLocate = 0;

	private static int m_total_selector_count = 0;

	public static int getTotalSelectorCount() {
		return m_total_selector_count;
	}

	final Object m_lock = new Object();

	private int m_port;

	// private Selector m_selector;

	private ServerSocket m_serverChannel;

	// use it regardless of protocol option
	private ConcurrentMap<MqttSocket, UserData> m_channels = new ConcurrentHashMap<MqttSocket, UserData>();

	// private ConcurrentMap<MtSocket, TimeUtil> m_channelsLastRecv = new ConcurrentHashMap<MtSocket,
	// TimeUtil>();// 200407

	// private ConcurrentMap<MtsSocket, UserData> m_connecting = new ConcurrentHashMap<MtsSocket, UserData>();

	// private ConcurrentMap<MtsSocket, TimeUtil> m_connecting_elapse = new ConcurrentHashMap<MtsSocket, TimeUtil>();

	// double m_channelTimeoutSec = 3600;// 1 hour

	// TimeUtil tmrCheckChannelAlive = new TimeUtil();

	//
	private boolean m_alive2 = false;

	// A list of MqChangeRequest instances
	private List<NioChangeRequest> m_changeRequests = new LinkedList<>();

	// Maps a MtSocket to a list of ByteBuffer instances
	// private Map<MtSocket, List<ByteBuffer>> m_pendingData = new HashMap<MtSocket, List<ByteBuffer>>();

	// private ThreadPoolExecutor m_executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

	// private int m_thpool_count2 = 0;

	// private long m_write_bytes = 0;

	// private long m_recv_bytes = 0;

	public static long debugLevel = 0;

	private long m_socketTimeout = 0;

	public void setSocketTimerout(long sto) {
		m_socketTimeout = sto;
	}

	public long getSocketTimerout() {
		return m_socketTimeout;
	}

	// private int m_socketInitialBufferSize = 1024 * 8;
	//
	// public void setSocketInitialBufferSize(int l) {
	// m_socketInitialBufferSize = l;
	// }

	// private long m_work_time_calc = 0;

	// Timer2 m_eventThread[] = new Timer2[2];

	// public long m_selector_wt_per_sec;
	// ArrayList<Integer> m_userages = new ArrayList<Integer>();
	// AverageUtil2 m_userages = new AverageUtil2(64);

	// private int m_userage_average = 0; // 0~ 100 ~

	// boolean recevie_with_pool = false;

	// AverageUtil m_read_avg = new AverageUtil();

	// AverageUtil m_write_avg = new AverageUtil();

	// TimeUtil m_events_calc_time = new TimeUtil();

	// long m_event_cnt = 0;

	// long m_events_per_sec = 0;

	// int m_rebindCount = 0;
	/**
	 * 
	 * 
	 */

	protected MqttServerSocketListener m_svr = null;

	//
	// /**
	// *
	// *
	// * @author richard
	// *
	// */
	class NioChangeRequest {
		// public static final int REGISTER = 1;
		// public static final int CHANGEOPS = 2;
		public static final int REMOVE = 3;

		public MqttSocket socket;
		public int type;
		public int ops;

		public NioChangeRequest(MqttSocket socket, int type, int ops) {
			this.socket = socket;
			this.type = type;
			this.ops = ops;

		}
	}

	/**
	 * 
	 * @param port
	 * @param pwd
	 * @param jks
	 * @return
	 * @throws Exception
	 */
	public ServerSocket getSocket(int port, String pwd, String jks) throws Exception {

		if (jks == null) {
			return new ServerSocket(port);
		} else {

			KeyStore keyStore = KeyStore.getInstance("JKS");
			FileInputStream keyStoreStream = new FileInputStream(jks);
			keyStore.load(keyStoreStream, pwd.toCharArray());

			// Create KeyManagerFactory
			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, pwd.toCharArray());

			// Initialize SSLContext
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

			// Create SSLServerSocketFactory
			SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
			ServerSocket serverSocket = (ServerSocket) serverSocketFactory.createServerSocket(port);

			System.out.println("SSL server started on port " + port);

			return serverSocket;
		}
	}

	public int getPort() {
		return m_port;
	}

	/**
	 * 
	 * @param svr
	 */

	public MqttServerSocket(MqttServerSocketListener svr) {
		// m_bootstrap = bs;
		m_svr = svr;
	}

	public MqttServerSocket bind(String ip, int port) throws Exception {
		return bind(ip, port, null, null);
	}

	public MqttServerSocket bind(final String ip, final int port, String pwd, String jks) throws Exception {

		if (NetUtil.isAvailablePort(port) == false)
			throw new UserException("port(%s) is not available", port);

		// debugLocate = 1;

		// debugLocate = 2;
		if (isAlive())
			throw new UserException("it's alive");

		_this.m_port = port;
		// _this.m_selector = SelectorProvider.provider().openSelector();// Selector.open();
		m_serverChannel = getSocket(port, pwd, jks);
		// m_serverChannel.configureBlocking(false);
		// InetSocketAddress listenAddr = new InetSocketAddress(ip, port);

		// if (backlog == 0)
		// m_serverChannel.socket().bind(listenAddr);
		// else
		// m_serverChannel.socket().bind(listenAddr, backlog);

		// m_serverChannel.register(_this.m_selector, SelectionKey.OP_ACCEPT);

		//

		new Thread(new Runnable() {
			public void run() {

				// {

				// }

				// debugLocate = 3;
				m_total_selector_count++;

				Timer2 etmr = startEventLoop();

				debug(MqttBroker3.DEBUG, "create selector cnt=%d. hashcode=%x ", m_total_selector_count, hashCode());
				try {
					m_alive2 = true;

					// m_rebindCount++;

					// debugLocate = 4;

					doSelectAction();

					// debugLocate = 5;
				} catch (Exception e) {
					e.printStackTrace();
					// debug(MqttBroker3.DEBUG, "doSelectAction has erro!.(dl=%s) hashcode=%x", debugLocate,
					// hashCode());
					debug(e);

				} finally {

					try {
						etmr.cancel();
					} catch (Exception e) {

					}

					try {
						MqttSocket chs[] = getChannels();

						// close all channels
						for (MqttSocket sc : chs) {

							sc.close();

						} // for
					} catch (Exception e) {

					}

					// //
					// for (int h = 0; h < m_eventThread.length; h++) {
					// m_eventThread[h].cancel();
					// }

					m_total_selector_count--;

					debug(MqttBroker3.DEBUG, "doSelectAction is finished! port(%s) hashcode=%x", m_port, hashCode());

					// clear
					try {
						if (m_serverChannel != null) {
							m_serverChannel.close();
						}
					} catch (Exception e) {
						debug(e);
					}
					try {
						m_channels.clear();
						// m_channelsLastRecv.clear();// 200407
						// m_connecting.clear();
						// m_connecting_elapse.clear();
						// m_changeRequests.clear();
						// m_pendingData.clear();
					} catch (Exception e) {
						debug(e);
					}

					// m_selector = null;
					m_alive2 = false;

				}
				debug(MqttBroker3.DEBUG, "finish!!! (%s:%s) cnt=%d  ", ip, port, m_total_selector_count);

			}
		}).start();

		waitForBind(3);

		return this;
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * @author richard
	 *
	 */

	// private int sequence_id = 0;

	/**
	 * 
	 * @param d
	 * @return
	 */
	public boolean waitForBind(double timeoutSec) {
		TimeUtil t = new TimeUtil();

		while (t.end_sec() < timeoutSec && isAlive() == false) {
			TimeUtil.sleep(10);
		}

		return isAlive();
	}

	// @Deprecated
	// public boolean waitBind(double d) {
	// return waitForBind(d);
	// }

	/**
	 * 
	 * 
	 * 
	 */
	public void close() {

		m_alive2 = false;
		// debug(MqttBroker3.DEBUG, "-selector.close(); dl=%s ", debugLocate);

		try {

			m_serverChannel.close();
		} catch (Exception e) {

		}

		// try {
		// this.m_selector.wakeup();
		//
		// } catch (Exception e) {
		//
		// }

		TimeUtil.sleep(30);

		try {
			MqttSocket chs[] = getChannels();

			// close all channels
			for (MqttSocket sc : chs) {

				try {
					sc.close();
				} catch (Exception e) {

				}

			} // for
		} catch (Exception e) {

		}

		TimeUtil.sleep(30);

		// // 2021.8.26
		// try {
		// m_selector.close();
		//
		// } catch (Exception e) {
		//
		// }
		//
		// m_selector = null;

		m_serverChannel = null;

		// try {
		// m_executor.shutdownNow();
		// } catch (Exception e) {
		// // e.printStackTrace();
		// }
		// m_executor = null;

		// use it regardless of protocol option
		m_channels.clear();

		// m_channelsLastRecv.clear();

		// m_connecting.clear();

		// m_connecting_elapse.clear();

		// m_changeRequests.clear();

		// m_pendingData.clear();

	}

	/**
	 * 
	 * 
	 * @param fmt
	 * @param args
	 * @return
	 */
	public String debug(int level, String fmt, Object... args) {

		try {

			String s = String.format(fmt, args);

			// if (m_logger != null)
			// m_logger.l(level, " tmsselector(p:" + m_port + ")", "%s", s);
			// else {

			if ((debugLevel & level) != 0)
				System.out.println(TimeUtil.now() + " tmsselector(p:" + m_port + ") " + s);

			// }

			return s;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String debug(Exception ex) {

		try {

			String s = UserException.getStackTrace(ex);

			// if (m_logger != null)
			// m_logger.e(ex);
			// else {

			System.out.println(TimeUtil.now() + " tmsselector(p:" + m_port + ") " + s);
			// }

			return s;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// /**
	// *
	// * @param l
	// */
	// public void setLogger(ILogger l) {
	// m_logger = l;
	// }

	/**
	 * 
	 * 
	 * 
	 * @return
	 */
	public int getChannelCount() {
		synchronized (m_channels) {
			return m_channels.size();
		}
	}

	public Object getUserObject(MqttSocket ch) {
		synchronized (m_channels) {
			UserData u = m_channels.get(ch);
			if (u != null)
				return u.userobj;

			return null;
		}
	}

	/**
	 * 
	 * 
	 */

	private void doSelectAction() throws IOException {

		// debugLocate = 100;
		// TimeUtil tmWorkTime = new TimeUtil();

		// ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 1024);// 1m

		// while (true) {
		while (m_alive2) {

			Socket sk = m_serverChannel.accept();

			// MqttSelectorListener m_svr

			new MqttSocket(sk, _this);// ***onAccept() will call in this constructor

			// debugLocate = 101;
			// // if (m_alive2 == false || m_selector == null) {
			// // debug("selector closed!!!");
			// // return;
			// // }
			//

			//
			// debugLocate = 103;
			// long ee = System.nanoTime();
			//
			// if (this.m_selector == null) {
			// debug(new UserException("selector is closed.(port:%s)", m_port));
			// return;
			// }
			//
			// int selCount = this.m_selector.selectNow();
			//
			// debugLocate = 105;
			// //
			// long ss1 = System.nanoTime();
			// if (selCount > 0) {
			//
			// try {
			// debugLocate = 106;
			// // wakeup to work on selected keys
			// Iterator<SelectionKey> keys = this.m_selector.selectedKeys().iterator();
			//
			// debugLocate = 107;
			// while (keys.hasNext()) {
			// SelectionKey key = (SelectionKey) keys.next();
			//
			// // this is necessary to prevent the same key from coming up
			// // again the next time around.
			// keys.remove();
			//
			// debugLocate = 108;
			//
			// // System.out.println("broker test----1");
			//
			// {
			// try {
			// if (!key.isValid()) {
			//
			// debug(MqttBroker3.VERBOSE, "!key.isValid()");
			//
			// continue;
			// }
			//
			// if (key.isAcceptable()) {
			// debugLocate = 1081;
			//
			// this.onAccept(key);
			//
			// } else if (key.isReadable()) {
			// debugLocate = 1082;
			//
			// read(key, readBuffer);
			//
			// } else if (key.isWritable()) {
			// debugLocate = 1083;
			//
			// this.write(key);
			//
			// } else if (key.isConnectable()) {
			// debugLocate = 1084;
			// this.finishConnection(key);
			// }
			//
			// } catch (Exception e) {
			// debug(e);
			//
			// }
			//
			// debugLocate = 109;
			// } // sync
			// } // while
			//
			// } catch (Exception e) {
			//
			// debug(e);
			// }
			// }
			// long ee1 = System.nanoTime();

			// debugLocate = 110;
			// //
			// // // get working time every seconds
			// // m_work_time_calc += (ee - ss);
			// // m_work_time_calc += (ee1 - ss1);
			// // if (tmWorkTime.end_sec() > 1.0) {
			// // tmWorkTime.start();
			// //
			// // userage_calc(m_work_time_calc);
			// // // debug("selector wt = %s m (per sec) ", m_selector_wt_per_sec);
			// //
			// // m_work_time_calc = 0;
			// // }
			//
			// m_userages.add(((ee - ss) + (ee1 - ss1)) / 100000);// micro
			//
			// if (m_userages.isTimeover(100)) {
			// m_userage_average = (int) (m_userages.getAverage());
			// }
			//
			// //
			// // if(
			// if (m_write_avg.isTimeover(3000)) {
			// m_write_bytes = m_write_avg.getLong();
			// }
			//
			// // checkChannelAlive();
			//
			// if (m_changeRequests.size() == 0) {
			// TimeUtil.sleep(10);
			// }
			//
			// // wait for event's done

		} // while

		// debugLocate = 111;
	}

	private Timer2 startEventLoop() {

		Timer2 tmr = new Timer2().schedule(

				new Timer2Task() {

					TimeUtil tmrSocketTimeout = new TimeUtil();

					@Override
					public void start(Timer2 tmr) {

					}

					@Override
					public void run(Timer2 tmr) {

						NioChangeRequest[] changeRequests = null;
						try {
							synchronized (m_changeRequests) {
								changeRequests = m_changeRequests
										.toArray(new NioChangeRequest[m_changeRequests.size()]);
								m_changeRequests.clear();
							} // sync
						} catch (Exception e) {
							debug(e);
						}

						try {

							// Iterator changes = this.m_changeRequests.iterator();
							/// while (changes.hasNext()) {
							// NioChangeRequest change = (NioChangeRequest) changes.next();
							if (changeRequests != null) {
								for (NioChangeRequest change : changeRequests) {

									SelectionKey key = null;

									// if (change != null)
									{
										switch (change.type) {
										// case NioChangeRequest.CHANGEOPS:
										// try {
										// key = change.socket.keyFor(this.m_selector);
										//
										// key.interestOps(change.ops);
										//
										// } catch (Exception e) {
										//
										// // e.printStackTrace();
										// debug(MqttBroker3.VERBOSE, "close in CHANGEOPS (%s)",
										// m_changeRequests.size());
										//
										// removeChannel(key, change.socket);
										//
										// }
										// break;
										case NioChangeRequest.REMOVE:

											try {
												// debug(MqttBroker3.VERBOSE, " MqChangeRequest.REMOVE");
												//
												// key = change.socket.keyFor(this.m_selector);

												removeChannel(change.socket);
											} catch (Exception e2) {
												e2.printStackTrace();
											}

											break;
										// case NioChangeRequest.REGISTER:
										// // debug(" MqChangeRequest.REGISTER");
										// try {
										// change.socket.register(this.m_selector, change.ops);
										// } catch (Exception e3) {
										// debug(e3);
										// }
										// break;

										}
									}
								} // for
							}

						} catch (Exception e) {
							debug(e);
						}

						//
						//
						// @20241001
						try {

							if (tmrSocketTimeout.end_sec() > 3) {
								tmrSocketTimeout.start();

								if (m_socketTimeout != 0) {
									MqttSocket[] lst = getChannels();

									for (MqttSocket ch : lst) {

										UserData u = null;
										synchronized (m_channels) {
											u = m_channels.get(ch);
										}

										if (u != null) {
											if (u.tmrLastRecv.end_ms() > m_socketTimeout) {

												channelClose(ch);
											}
										}

									} // for

								} // if (m_socketTimeout != 0) {

							}

						} catch (Exception e) {
							debug(e);
						}

					}

					@Override
					public void stop(Timer2 tmr) {

					}

				}, 10, 10);

		return tmr;

	}

	/**
	 * 
	 * 
	 * 
	 * 
	 */
	// private void checkChannelAlive() {
	//
	// try {
	// if (tmrCheckChannelAlive.end_sec() > 60 * 10) {// every 10 min
	// tmrCheckChannelAlive.start();
	//
	// synchronized (m_channelsLastRecv) {
	//
	// for (Map.Entry<MtSocket, TimeUtil> entry : m_channelsLastRecv.entrySet()) {
	// // System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
	//
	// if (entry.getValue().end_sec() > m_channelTimeoutSec) {
	//
	// synchronized (this.m_changeRequests) {
	// debug(MqttBroker3.VERBOSE, "add remove -3.1 ");
	// this.m_changeRequests
	// .add(new NioChangeRequest(entry.getKey(), NioChangeRequest.REMOVE, 0));
	//
	// }
	//
	// }
	//
	// } // for
	//
	// } // sync
	//
	// } // if(
	// } catch (Exception e) {
	// debug(e);
	// }
	// }

	// private void checkChannelAlive() {
	//
	// try {
	// if (tmrCheckChannelAlive.end_sec() > 60) {// every 10 min
	// tmrCheckChannelAlive.start();
	//
	// synchronized (m_channels) {
	//
	// for (Map.Entry<MtSocket, UserData> entry : m_channels.entrySet()) {
	// // System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
	//
	// UserData u = entry.getValue();
	//
	// if (u.tmrLastRecv.end_sec() > u.timeout && m_channelTimeoutSec!=0) {
	//
	// synchronized (this.m_changeRequests) {
	// debug(MqttBroker3.VERBOSE, "timeover( %s > %s ) ", u.tmrLastRecv.end_sec(), u.timeout);
	// this.m_changeRequests
	// .add(new NioChangeRequest(entry.getKey(), NioChangeRequest.REMOVE, 0));
	//
	// }
	//
	// }
	//
	// } // for
	//
	// } // sync
	//
	// } // if(
	// } catch (Exception e) {
	// debug(e);
	// }
	// }

	/**
	 * 
	 * @param key
	 * @param ch
	 */
	private void removeChannel(MqttSocket ch) {

		// try {
		// if (key != null) {
		// key.cancel();
		// }
		// } catch (Exception e) {
		// ;
		// }

		try {

			// TmsConnectorListener listen = null;// 200330
			// synchronized (m_channels) {
			// if (m_channels.containsKey(ch)) {
			//
			// // listen = m_channels.get(ch);
			// }
			// }

			// if (listen != null) {

			// listen.disconnected(ch);
			// addEvent(new EventItem(listen, ch, EventItem.DISCONNECT, null));

			// }
			// addEvent(ch, 'd', null);
			m_svr.selectorDisconnected(ch);

		} catch (Exception e2) {
			e2.printStackTrace();
		}

		try {
			// if (ch != null) {
			synchronized (m_channels) {

				m_channels.remove(ch);

			}
			// synchronized (m_channelsLastRecv) {
			// m_channelsLastRecv.remove(ch);// 200407
			// }
		} catch (Exception e) {
			debug(e);
		}

		//
		try {
			// ch.socket().close();
		} catch (Exception e) {
			;
		}

		//
		try {
			ch.close();
		} catch (Exception e) {
			;
		}

		// try {
		// synchronized (this.m_pendingData) {
		// m_pendingData.remove(ch);
		// }
		// } catch (Exception e) {
		// ;
		// }

		// try {
		// synchronized (m_connecting) {
		// m_connecting.remove(ch);
		// m_connecting_elapse.remove(ch);
		// }
		// } catch (Exception e) {
		//
		// }

		// try {
		// synchronized (m_eventDatas) {
		// m_eventDatas.remove(ch);
		// }
		// } catch (Exception e) {
		//
		// }
		// }

	}

	/**
	 * 
	 * 
	 * call it from SslSocket
	 * 
	 * 
	 * @param channel
	 * @throws IOException
	 */
	protected UserData onAccept(MqttSocket channel) throws IOException {

		// ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		// MtSocket channel = serverChannel.accept();
		// channel.configureBlocking(false);
		//
		// // addEvent(new EventItem(m_svr, channel, EventItem.ACCEPT, this));

		try {

			// channel.socket().setReceiveBufferSize(m_socketInitialBufferSize);
			// channel.socket().setSendBufferSize(m_socketInitialBufferSize);

			// TmsConnectorListener rs = m_svr.accepted(this, channel);
			// if (rs != null) {
			// accept(channel, rs);
			// } else {
			// channelClose(channel);
			// }

			Object uo = m_svr.selectorAccepteded(channel);
			UserData userdata = new UserData(true, uo);
			// accept(channel, uo);
			synchronized (m_channels) {

				m_channels.put(channel, userdata);

			}

			// addEvent(channel,'a',null);
			// if (m_svr.accepteded(channel) == false) {
			// channelClose(channel);
			// }
			return userdata;

		} catch (Exception e) {
			debug(MqttBroker3.DEBUG, "error in accept %s ", e);
			channelClose(channel);
		}

		return null;

	}

	// /**
	// *
	// * @param channel
	// * @throws IOException
	// */
	// private void accept(MqttSslSocket channel, Object userobj) throws IOException {
	//
	// synchronized (m_channels) {
	//
	// m_channels.put(channel, new UserData(true, userobj));
	//
	// }
	//
	// // synchronized (m_channelsLastRecv) {
	// // m_channelsLastRecv.put(channel, new TimeUtil()); // 200407
	// // }
	// // channel.register(this.m_selector, SelectionKey.OP_READ);
	//
	// }

	// /**
	// * protocol type 1
	// *
	// * @param key
	// * @param buffer
	// * @throws IOException
	// */
	// private void read(SelectionKey key, ByteBuffer buffer) throws IOException {
	//
	// MtSocket channel = (MtSocket) key.channel();
	//
	// buffer.clear();
	//
	// int numRead = -1;
	// try {
	// numRead = channel.read(buffer);
	// } catch (IOException e) {
	//
	// synchronized (m_changeRequests) {
	// debug(MqttBroker3.VERBOSE, "read() err=" + e);
	// this.m_changeRequests.add(new NioChangeRequest(channel, NioChangeRequest.REMOVE, 0));
	// }
	// return;
	// }
	//
	// // if (numRead <= 0) {
	// if (numRead < 0) {
	//
	// // removeChannel(key, channel);
	// synchronized (m_changeRequests) {
	// debug(MqttBroker3.VERBOSE, "read() err len=" + numRead);
	// this.m_changeRequests.add(new NioChangeRequest(channel, NioChangeRequest.REMOVE, 0));
	// }
	// return;
	// }
	//
	// buffer.flip();
	//
	// byte[] data = new byte[numRead];
	// System.arraycopy(buffer.array(), 0, data, 0, numRead);
	//
	// m_recv_bytes += numRead;
	//
	// m_read_avg.sum(numRead);
	//
	// if (m_read_avg.isTimeover(3000)) {
	// m_recv_bytes = m_read_avg.getLong();
	// // debug("average of read bytes %s ", StringUtil.formatBytesSize());
	// }
	//
	// //
	// //
	// m_event_cnt++;
	// if (m_events_calc_time.end_sec() > 1.0) {
	// m_events_calc_time.start();
	// m_events_per_sec = m_event_cnt;
	// m_event_cnt = 0;
	// }
	//
	// try {
	//
	// debugLocate = 10821;
	// // submit
	// // TmsConnectorListener ch = null;
	// synchronized (m_channels) {
	// // ch = m_channels.get(channel);
	// }
	// debugLocate = 10822;
	//
	// // addEvent(new EventItem(ch, channel, EventItem.RECV, data));
	// // ch.received(channel, data, data.length);
	//
	// // addEvent(channel,'r',data);
	// if (data.length > 0) {
	// m_svr.received(channel, data);
	//
	// // synchronized (m_channelsLastRecv) {
	// // m_channelsLastRecv.get(channel).start();// 200407 updat last receive time
	// // }
	//
	// synchronized (m_channels) {
	// m_channels.get(channel).tmrLastRecv.start();// 200407 updat last receive time
	// }
	//
	// }
	// debugLocate = 10823;
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// // ep.append(data, data.length);
	//
	// }

	/**
	 * 
	 * 
	 * 
	 * @param ch
	 */

	public void channelClose(MqttSocket ch) {

		if (ch == null)
			return;

		if (isContains(ch) == false)
			return;

		synchronized (this.m_changeRequests) {
			debug(MqttBroker3.VERBOSE, "channelClose() ");
			this.m_changeRequests.add(new NioChangeRequest(ch, NioChangeRequest.REMOVE, 0));

		}

	}

	/**
	 * 
	 * @return
	 */
	public boolean isAlive() {
		return m_alive2;
	}

	public boolean isAlive(MqttSocket channel) {

		if (channel == null)
			return false;
		synchronized (m_channels) {

			return m_channels.containsKey(channel);

		}
	}

	public boolean isAccept(MqttSocket channel) {

		synchronized (m_channels) {

			if (channel == null)
				return false;

			UserData item = m_channels.get(channel);
			if (item != null) {
				return item.isAccept;
			}
		}
		return false;

	}

	public boolean isContains(MqttSocket socket) {

		if (socket == null)
			return false;

		synchronized (m_channels) {
			if (m_channels.containsKey(socket)) {
				return true;
			}
			return false;
		}
	}

	// /**
	// *
	// * @param socket
	// * @param data
	// */
	// public void send(MtsSocket socket, byte[] data) throws Exception {
	//
	// // debug("send (%s)",data.length);
	// if (socket == null) {
	// return;
	// }
	//
	// //
	// if (isContains(socket) == false) {
	// // throw new UserException("isn't containing socketchannel");
	// // return; // already closed
	// return;
	// }
	//
	// try {
	// if( socket.send(data)==false) {
	// throw new UserException("send fail!");
	// }
	// } catch (Exception ex) {
	// this.m_changeRequests.add(new NioChangeRequest(socket, NioChangeRequest.REMOVE, 0));
	// throw ex;
	// }
	//
	// // }
	//
	// // synchronized (this.m_changeRequests) {
	// // // Indicate we want the interest ops set changed
	// // this.m_changeRequests.add(new NioChangeRequest(socket, NioChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
	// // }
	// // // And queue the data we want written
	// // synchronized (this.m_pendingData) {
	// // List queue = (List) this.m_pendingData.get(socket);
	// // if (queue == null) {
	// // queue = new ArrayList();
	// // this.m_pendingData.put(socket, queue);
	// // }
	// // queue.add(ByteBuffer.wrap(data));
	// // }
	//
	// // boolean newIn = false;
	// // // And queue the data we want written
	// // synchronized (this.m_pendingData) {
	// // List queue = (List) this.m_pendingData.get(socket);
	// // if (queue == null) {
	// // queue = new ArrayList();
	// // this.m_pendingData.put(socket, queue);
	// // newIn = true;
	// // }
	// // queue.add(ByteBuffer.wrap(data));
	// // }
	// //
	// // if (newIn) {
	// // synchronized (this.m_changeRequests) {
	// // // Indicate we want the interest ops set changed
	// // this.m_changeRequests
	// // .add(new NioChangeRequest(socket, NioChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
	// // }
	// // }
	//
	// // Finally, wake up our selecting thread so it can make the required
	// // changes
	// // this.m_selector.wakeup();
	//
	// }

	// /**
	// *
	// * @param key
	// * @throws IOException
	// */
	// private void write(SelectionKey key) throws IOException {
	//
	// MtSocket socketChannel = (MtSocket) key.channel();
	//
	// synchronized (this.m_pendingData) {
	// List queue = (List) this.m_pendingData.get(socketChannel);
	//
	// if (queue != null) {
	//
	// try {
	// // Write until there's not more data ...
	// while (!queue.isEmpty()) {
	// ByteBuffer buf = (ByteBuffer) queue.get(0);
	//
	// socketChannel.write(buf);
	//
	// // debug("write(%s) remaining(%s)", buf.position(), buf.remaining());
	//
	// long ns = System.currentTimeMillis();
	//
	// while (buf.remaining() > 0) {
	// // // ... or the socket's buffer fills up
	// //
	// socketChannel.write(buf);
	// //
	// // if ((System.currentTimeMillis() - ns) > (3 * 1000)) {// max 10ms
	// // // debug("selector write remained ");
	// //
	// // // key.interestOps(SelectionKey.OP_WRITE);// try it again
	// // // 20200518 break;
	// // debug("write timeover ");
	// // socketChannel.close();
	// // // return;
	// // }
	// // // }
	// }
	//
	// for (int i = 0; i < 8 && buf.remaining() > 0; i++) {
	// socketChannel.write(buf);
	// }
	//
	// m_write_avg.sum(buf.position());
	//
	// buf.clear();
	//
	// queue.remove(0);
	// }
	// } catch (Exception e) {
	// // e.printStackTrace();
	// debug(MqttBroker3.VERBOSE, "close in write " + e.toString());
	// // queue.clear();
	//
	// // removeChannel(key, socketChannel);
	// synchronized (m_changeRequests) {
	// debug(MqttBroker3.VERBOSE, "write() err ");
	// this.m_changeRequests.add(new NioChangeRequest(socketChannel, NioChangeRequest.REMOVE, 0));
	// }
	// return;
	// }
	//
	// if (queue.isEmpty()) {
	// key.interestOps(SelectionKey.OP_READ);
	// synchronized (m_pendingData) {
	// m_pendingData.remove(socketChannel);
	// }
	//
	// }
	// } else {
	// key.interestOps(SelectionKey.OP_READ);
	// }
	// } // sync
	// }

	/**
	 * 
	 * 
	 * 
	 */
	@Override
	public String toString() {

		String s = "sslselector.rv1@" + this.hashCode();

		s += String.format("sel cnt(%s) ", m_total_selector_count);

		s += String.format("port(%s) ch(%s) requests(%s) pending data(%s)   ", m_port, this.m_channels.size(),
				this.m_changeRequests.size(), "x");

		s += String.format("   wt(%s)ms    ", userage());

		// s += String.format("tcnt(%s) ac(%s) ps(%s) qs(%s) "//
		// , m_thpool_count2, m_executor.getActiveCount(), m_executor.getPoolSize(), m_executor.getQueue().size());

		// s += String.format(" recv(%s) write(%s) events(%s) ", StringUtil.formatBytesSize(m_recv_bytes),
		// StringUtil.formatBytesSize(m_write_bytes), StringUtil.formatCount(m_events_per_sec));

		// s += "<br>";
		// for (Timer2 t : m_eventThread) {
		// s += t;
		// s += "<br>";
		// }

		return s;
	}

	// /**
	// * userage calculating
	// *
	// * @param wt
	// */
	//
	// public void userage_calc(long wt) {
	//
	//
	// synchronized (m_userages) {
	//
	//
	//
	//
	// if (wt == 0)
	// return;
	//
	// m_userages.add((int) (wt / 1000000));
	//
	// if (m_userages.size() > 32) {
	// m_userages.remove(0);
	// }
	//
	// try {
	// int n = m_userages.size();
	// long sum = 0;
	// for (int v : m_userages) {
	// sum += v;
	// }
	//
	// m_userage_average = (int) ((sum / n) * 0.1);
	// } catch (Exception e) {
	//
	// }
	//
	// }
	//
	// return;
	// }

	/**
	 * 
	 * performance userage value : 0~100%
	 * 
	 * @return
	 */
	public int userage() {

		return 100;
	}

	// /**
	// *
	// *
	// * @return
	// * @throws IOException
	// */
	// public MtSocket connect(String ip, int port, Object userobj) throws Exception {
	//
	// if (m_selector == null)
	// throw new UserException("-selector is not creat %s hashcode=%x alive(%s)", m_selector, hashCode(),
	// m_alive2);
	//
	// // Create a non-blocking socket channel
	// MtSocket socketChannel = MtSocket.open();
	// socketChannel.configureBlocking(false);
	//
	// // socketChannel.socket().setReceiveBufferSize(m_socketInitialBufferSize);
	// // socketChannel.socket().setSendBufferSize(m_socketInitialBufferSize);
	//
	// // Kick off connection establishment
	// socketChannel.connect(new InetSocketAddress(ip, port));
	//
	// synchronized (m_connecting) {
	//
	// UserData item = new UserData(false, userobj);
	// // item.userobj = userobj;
	// m_connecting.put(socketChannel, item);
	// m_connecting_elapse.put(socketChannel, new TimeUtil());
	// }
	//
	// synchronized (this.m_changeRequests) {
	// this.m_changeRequests
	// .add(new NioChangeRequest(socketChannel, NioChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
	// // this.m_selector.wakeup();
	// }
	//
	// // Finally, wake up our selecting thread so it can make the required changes
	//
	// return socketChannel;
	// }
	//
	// /**
	// *
	// *
	// * @param ch
	// * @param timeoutSec
	// */
	// public void waitForConnecting(MtSocket ch, double timeoutSec) {
	//
	// TimeUtil t = new TimeUtil();
	//
	// while (t.end_sec() < timeoutSec && isConnecting(ch)) {
	//
	// TimeUtil.sleep(10);
	//
	// }
	//
	// }

	// /**
	// *
	// * @param key
	// * @throws IOException
	// */
	// private void finishConnection(SelectionKey key) throws IOException {
	// MtSocket channel = (MtSocket) key.channel();
	//
	// // Finish the connection. If the connection operation failed
	// // this will raise an IOException.
	// try {
	// channel.finishConnect();
	// } catch (IOException e) {
	//
	// synchronized (m_changeRequests) {
	// debug(MqttBroker3.VERBOSE, "finishConnnection() err ");
	// this.m_changeRequests.add(new NioChangeRequest(channel, NioChangeRequest.REMOVE, 0));
	// }
	// debug(e);
	//
	// try {
	// synchronized (m_connecting) {
	// m_connecting.remove(channel);
	// m_connecting_elapse.remove(channel);
	// }
	// } catch (Exception ee) {
	//
	// }
	//
	// return;
	// }
	//
	// try {
	//
	// //
	//
	// UserData item = null;
	// synchronized (m_connecting) {
	//
	// item = m_connecting.remove(channel);
	// m_connecting_elapse.remove(channel);
	// }
	//
	// synchronized (m_channels) {
	// m_channels.put(channel, item);
	// }
	// // synchronized (m_channelsLastRecv) {
	// // m_channelsLastRecv.put(channel, new TimeUtil());// 200407
	// // }
	// // addEvent(new EventItem(listen, channel, EventItem.CONNECT, null));
	// // listen.connected(channel);
	// // addEvent(channel,'c',null);
	// m_svr.connected(channel, item.userobj);
	//
	// } catch (Exception e) {
	// // removeChannel(key, channel);
	// synchronized (m_changeRequests) {
	// debug(e);
	// this.m_changeRequests.add(new NioChangeRequest(channel, NioChangeRequest.REMOVE, 0));
	// }
	//
	// }
	//
	// // Register an interest in writing on this channel
	// key.interestOps(SelectionKey.OP_READ);
	//
	// }

	// /**
	// *
	// * @param ch
	// * @return
	// */
	// public boolean isConnecting(MtSocket ch) {
	//
	// if (ch == null)
	// return false;
	//
	// synchronized (m_connecting) {
	//
	// // 190930 bugfix!
	// TimeUtil t = m_connecting_elapse.get(ch);
	// if (t != null) {
	// if (t.end_sec() > 32) {
	// System.out.println(" connecting timevoer!!!" + ch);
	//
	// try {
	// m_connecting.remove(ch);
	// m_connecting_elapse.remove(ch);
	// ch.close();
	// } catch (Exception e) {
	//
	// }
	//
	// }
	// }
	//
	// return m_connecting.containsKey(ch);
	//
	// }
	//
	// }

	/**
	 * 
	 * @return
	 */
	public String getLocalAddress() {
		if (isAlive() == false)
			return null;

		try {
			return m_serverChannel.getLocalSocketAddress().toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	/**
	 * 
	 * @return
	 */
	// public int pendingCount() {
	//
	// synchronized (this.m_pendingData) {
	// int r = 0;
	// int n = m_pendingData.size();
	//
	// for (int i = 0; i < n; i++) {
	// if (m_pendingData.get(i) != null) {
	// r += m_pendingData.get(i).size();
	// }
	// }
	// return r;
	// }
	// }

	public int pendingCount() {

		// synchronized (this.m_pendingData) {
		// int r = 0;
		//
		// for (List<ByteBuffer> l : m_pendingData.values()) {
		//
		// if (l != null) {
		// r += l.size();
		// }
		// }
		// return r;
		// }
		return 0;
	}

	public int changeRequestCount() {
		return m_changeRequests.size();
	}

	// /**
	// *
	// * @param d
	// */
	// public void setChannelTimeoutSec(double d) {
	// m_channelTimeoutSec = d;
	// }
	//
	// /**
	// *
	// * @return
	// */
	// public double getChannelTimeoutSec() {
	// return m_channelTimeoutSec;
	// }

	public int getRebindCount() {
		// return m_rebindCount;
		return 0;
	}
	// /**
	// *
	// * @return
	// */
	// public boolean isPeddingOverload() {
	// synchronized (m_pendingData) {
	//
	// if ((m_channels.size() * 32) < m_pendingData.size()) {
	// return true;
	// }
	// return false;
	// }
	// }

	// void setChannelTimeoutSec(MtSocket ch, double d) {
	// UserData u = m_channels.get(ch);
	//
	// // if( u!=null)
	// u.timeout = d;
	//
	// }
	//
	// double getChannelTimeoutSec(MtSocket ch) {
	//
	// UserData u = m_channels.get(ch);
	//
	// if (u != null)
	// return u.timeout;
	//
	// return m_channelTimeoutSec;
	// }

	public MqttSocket[] getChannels() {

		synchronized (m_channels) {
			return m_channels.keySet().toArray(new MqttSocket[0]);
		}

	}

	public void connectTo(String ip, int port, String cacrt) throws Exception {

		new MqttSocket(ip, port, this, cacrt);

	}
}
