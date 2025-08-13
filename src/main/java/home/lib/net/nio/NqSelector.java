package home.lib.net.nio;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.*;

import home.lib.lang.Timer2;
import home.lib.lang.UserException;
import home.lib.log.ILogger;
import home.lib.util.AverageUtil;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

/**
 * 
 * guild site link: http://rox-xmlrpc.sourceforge.net/niotut/#The server
 * 
 * 
 * 20201102
 * 
 * modifyed of read() and write() parts.( improve imbalance when processing huge data )
 * 
 * 
 * 
 * @author livingroom-user
 *
 */
public class NqSelector {

	NqSelector _this = this;

	private int debugLocate = 0;

	private static int m_total_selector_count = 0;

	public static int getTotalSelectorCount() {
		return m_total_selector_count;
	}

	final Object m_lock = new Object();
	private int m_port;
	private Selector m_selector;
	private ServerSocketChannel m_serverChannel;

	// use it regardless of protocol option
	private ConcurrentMap<SocketChannel, NqChannelListener> m_channels = new ConcurrentHashMap<SocketChannel, NqChannelListener>();
	private ConcurrentMap<SocketChannel, TimeUtil> m_channelsLastRecv = new ConcurrentHashMap<SocketChannel, TimeUtil>();// 200407

	private ConcurrentMap<SocketChannel, NqChannelListener> m_connecting = new ConcurrentHashMap<SocketChannel, NqChannelListener>();
	private ConcurrentMap<SocketChannel, TimeUtil> m_connecting_elapse = new ConcurrentHashMap<SocketChannel, TimeUtil>();

	double NO_RECV_TIME_OUT_SEC = 60 * 60;// 1 hour

	void setChannelTimeout(double d) {
		NO_RECV_TIME_OUT_SEC = d;
	}

	double getChannelTimeout() {
		return NO_RECV_TIME_OUT_SEC;
	}

	TimeUtil tmrCheckChannelAlive = new TimeUtil();

	//
	private boolean m_alive2 = false;

	// A list of MqChangeRequest instances
	private List<NioChangeRequest> m_changeRequests = new LinkedList<>();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map<SocketChannel, List<ByteBuffer>> m_pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();

	private ThreadPoolExecutor m_executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

	private int m_thpool_count2 = 0;

	private long m_write_bytes = 0;

	private long m_recv_bytes = 0;

	private long m_work_time_calc = 0;

	// Timer2 m_eventThread[] = new Timer2[2];

	// public long m_selector_wt_per_sec;
	ArrayList<Integer> m_userages = new ArrayList<Integer>();

	private int m_userage_average = 0;

	boolean recevie_with_pool = false;

	AverageUtil m_read_avg = new AverageUtil();

	AverageUtil m_write_avg = new AverageUtil();

	private ILogger m_logger = null;

	TimeUtil m_events_calc_time = new TimeUtil();
	
	long m_event_cnt = 0;
	
	long m_events_per_sec = 0;

	/**
	 * 
	 * 
	 */

	private NqSelectorListener m_svr = null;

	/**
	 * 
	 * 
	 * @author richard
	 *
	 */
	class NioChangeRequest {
		public static final int REGISTER = 1;
		public static final int CHANGEOPS = 2;
		public static final int REMOVE = 3;

		public SocketChannel socket;
		public int type;
		public int ops;

		public NioChangeRequest(SocketChannel socket, int type, int ops) {
			this.socket = socket;
			this.type = type;
			this.ops = ops;

		}
	}

	// //
	// /**
	// *
	// * @author richard
	// *
	// */
	// class EventItem {
	//
	// public static final byte CONNECT = 1;
	// public static final byte RECV = 2;
	// public static final byte DISCONNECT = 3;
	// public static final byte ACCEPT = 4;
	//
	// NqChannelListener listen;
	// SocketChannel ch;
	// byte[] data;
	// byte ops = 0;
	// NqSelectorListener svrlisten;
	// NqSelector sel;
	//
	// public EventItem(NqChannelListener l, SocketChannel c, byte o, byte[] d) {
	// listen = l;
	// ch = c;
	// data = d;
	// ops = o;
	// }
	//
	// public EventItem(NqSelectorListener l, SocketChannel c, byte o, NqSelector s) {
	// svrlisten = l;
	// ch = c;
	// sel = s;
	// ops = o;
	// }
	//
	// }
	//
	// // the problem is broadcast in server!!!!
	//
	// // private Map<SocketChannel, ArrayList<EventItem>> m_eventDatas = new HashMap<SocketChannel,
	// // ArrayList<EventItem>>();
	// private ArrayList<EventItem> m_eventDatas = new ArrayList<EventItem>();
	// public static Map<SocketChannel, String> currentWorkSocket = new HashMap<SocketChannel, String>();
	//
	// /**
	// *
	// * @param e
	// */
	// private void addEvent(EventItem e) {
	// synchronized (m_eventDatas) {
	// // ArrayList<EventItem> queue = m_eventDatas.get(e.ch);
	// // if (queue == null) {
	// // queue = new ArrayList<EventItem>();
	// // m_eventDatas.put(e.ch, queue);
	// // }
	// // queue.add(e);
	// //
	// // m_eventList.add(e.ch);
	//
	// m_eventDatas.add(e);
	// }
	// }
	//
	// /**
	// *
	// * @return
	// */
	// private EventItem pollFirstEvent() {
	// synchronized (m_eventDatas) {
	// // if (m_eventList.size() > 0) {
	// //
	// // SocketChannel ch = m_eventList.remove(0);
	// // if (ch == null)
	// // return null;
	// // else
	// // return m_eventDatas.remove(ch);
	// //
	// // }
	// // return null;
	//
	// for (int h = 0; h < m_eventDatas.size() && h < 128; h++) {
	// EventItem e = m_eventDatas.get(h);
	//
	// synchronized (currentWorkSocket) {
	// if (currentWorkSocket.get(e.ch) == null) {
	//
	// currentWorkSocket.put(e.ch, "");
	//
	// return m_eventDatas.remove(h);
	// }
	//
	// }
	//
	// }
	// } // syn
	//
	// return null;
	// }
	//
	// /**
	// *
	// * @param e
	// */
	// private void releaseChannelEvent(SocketChannel ch) {
	// synchronized (currentWorkSocket) {
	//
	// currentWorkSocket.remove(ch);
	// } // syn
	//
	// }
	public int getPort() {
		return m_port;
	}

	/**
	 * 
	 * @param svr
	 */

	public NqSelector(NqSelectorListener svr) {
		// m_bootstrap = bs;
		m_svr = svr;
	}

	public Future<Object> bind(String ip, int port) throws Exception {

		// {
		//
		// if (m_executor != null)
		// throw new UserException("selector is alive");
		//
		// m_executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
		// }

		debugLocate = 1;
		//
		// start select thread
		//
		Callable<Object> task = () -> {

			// {

			debugLocate = 2;
			if (isAlive())
				throw new UserException("selector is alive");

			this.m_port = port;
			this.m_selector = SelectorProvider.provider().openSelector();// Selector.open();
			m_serverChannel = ServerSocketChannel.open();
			m_serverChannel.configureBlocking(false);
			InetSocketAddress listenAddr = new InetSocketAddress(ip, port);
			m_serverChannel.socket().bind(listenAddr);
			m_serverChannel.register(this.m_selector, SelectionKey.OP_ACCEPT);

			// }

			debugLocate = 3;
			m_total_selector_count++;

			debug("create selector cnt=%d ", m_total_selector_count);
			try {
				m_alive2 = true;

				debugLocate = 4;
				doSelectAction();
				debugLocate = 5;
			} catch (IOException e) {
				e.printStackTrace();

			} finally {

				m_alive2 = false;

				// //
				// for (int h = 0; h < m_eventThread.length; h++) {
				// m_eventThread[h].cancel();
				// }

				m_total_selector_count--;

				// clear
				try {
					m_selector.close();
				} catch (Exception e) {

				}
				try {
					m_channels.clear();
					m_channelsLastRecv.clear();// 200407
					m_connecting.clear();
					m_connecting_elapse.clear();
					m_changeRequests.clear();
					m_pendingData.clear();
				} catch (Exception e) {
					e.printStackTrace();
				}

				m_selector = null;

			}
			debug("finish!!! (%s:%s) cnt=%d  ", ip, port, m_total_selector_count);

			return true;
		};

		Future<Object> res = m_executor.submit(task);
		return res;

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

	/**
	 * 
	 * @param d
	 * @return
	 */
	public boolean waitBind(double d) {
		TimeUtil t = new TimeUtil();

		while (t.end_sec() < d && isAlive() == false) {
			TimeUtil.sleep(10);
		}

		return isAlive();
	}

	/**
	 * 
	 * 
	 * 
	 */
	public void close() {

		m_alive2 = false;
		debug("selector.close();  dl=%s ", debugLocate);

		try {
			this.m_selector.wakeup();

		} catch (Exception e) {

		}

		//
		try {
			m_selector.close();

		} catch (Exception e) {

		}

		m_selector = null;

		try {

			m_serverChannel.close();
		} catch (Exception e) {

		}
		m_serverChannel = null;

		TimeUtil.sleep(30);

		SocketChannel chs[] = null;
		synchronized (m_channels) {
			chs = m_channels.entrySet().toArray(new SocketChannel[m_channels.size()]);
		}
		// close all channels
		for (SocketChannel sc : chs) {

			try {
				sc.close();
			} catch (Exception e) {

			}

		} // for

		// try {
		// m_executor.shutdownNow();
		// } catch (Exception e) {
		// // e.printStackTrace();
		// }
		// m_executor = null;

	}

	/**
	 * 
	 * 
	 * @param fmt
	 * @param args
	 * @return
	 */
	public String debug(String fmt, Object... args) {

		try {

			String s = String.format(fmt, args);

			if (s.equals("java.lang.NullPointerException")) {
				s += "-";

			}

			if (m_logger != null)
				m_logger.l("%s", s);
			else
				System.out.println(TimeUtil.now() + " mqselector(p:" + m_port + ") " + s);

			return s;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * @param l
	 */
	public void setLogger(ILogger l) {
		m_logger = l;
	}

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

	/**
	 * 
	 * 
	 */

	private void doSelectAction() throws IOException {

		debugLocate = 100;
		TimeUtil tmWorkTime = new TimeUtil();

		ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 1024);// 1m

		// while (true) {
		while (m_alive2) {

			debugLocate = 101;
			// if (m_alive2 == false || m_selector == null) {
			// debug("selector closed!!!");
			// return;
			// }

			long ss = System.nanoTime();
			// try {
			debugLocate = 102;

			NioChangeRequest[] changeRequests = null;
			synchronized (this.m_changeRequests) {
				changeRequests = this.m_changeRequests.toArray(new NioChangeRequest[m_changeRequests.size()]);
				m_changeRequests.clear();
			} // sync

			try {

				// Iterator changes = this.m_changeRequests.iterator();
				/// while (changes.hasNext()) {
				// NioChangeRequest change = (NioChangeRequest) changes.next();
				for (NioChangeRequest change : changeRequests) {

					SelectionKey key = null;

					// if (change != null)
					{
						switch (change.type) {
						case NioChangeRequest.CHANGEOPS:
							try {
								key = change.socket.keyFor(this.m_selector);

								key.interestOps(change.ops);

							} catch (Exception e) {

								// e.printStackTrace();
								debug("close in CHANGEOPS (%s)", m_changeRequests.size());

								removeChannel(key, change.socket);

							}
							break;
						case NioChangeRequest.REMOVE:

							debug(" MqChangeRequest.REMOVE");

							key = change.socket.keyFor(this.m_selector);

							removeChannel(key, change.socket);

							break;
						case NioChangeRequest.REGISTER:
							// debug(" MqChangeRequest.REGISTER");
							change.socket.register(this.m_selector, change.ops);
							break;

						}
					}
				} // for

			} catch (Exception e) {
				e.printStackTrace();
			}

			// synchronized (m_changeRequests) {
			// m_changeRequests.clear();
			// }
			// } // sync

			// if (m_alive2 == false || m_selector == null) {
			// debug("selector closed.!!!");
			// return;
			// }

			debugLocate = 103;
			long ee = System.nanoTime();

			int selCount = this.m_selector.selectNow();

			// debugLocate = 104;
			// if (m_alive2 == false || m_selector == null) {
			// debug("selector closed..!!!");
			// return;
			// }

			debugLocate = 105;
			//
			long ss1 = System.nanoTime();
			if (selCount > 0) {

				try {
					debugLocate = 106;
					// wakeup to work on selected keys
					Iterator<SelectionKey> keys = this.m_selector.selectedKeys().iterator();

					debugLocate = 107;
					while (keys.hasNext()) {
						SelectionKey key = (SelectionKey) keys.next();

						// this is necessary to prevent the same key from coming up
						// again the next time around.
						keys.remove();

						debugLocate = 108;

						// System.out.println("broker test----1");

						{
							try {
								if (!key.isValid()) {

									debug("!key.isValid()");

									continue;
								}

								if (key.isAcceptable()) {
									debugLocate = 1081;

									this.onAccept(key);

								} else if (key.isReadable()) {
									debugLocate = 1082;

									read(key, readBuffer);

								} else if (key.isWritable()) {
									debugLocate = 1083;

									this.write(key);

								} else if (key.isConnectable()) {
									debugLocate = 1084;
									this.finishConnection(key);
								}

							} catch (Exception e) {
								e.printStackTrace();

							}

							debugLocate = 109;
						} // sync
					} // while

				} catch (Exception e) {

					e.printStackTrace();
				}
			}
			long ee1 = System.nanoTime();

			debugLocate = 110;
			//
			// get working time every seconds
			m_work_time_calc += (ee - ss);
			m_work_time_calc += (ee1 - ss1);
			if (tmWorkTime.end_sec() > 1.0) {
				tmWorkTime.start();

				userage_calc(m_work_time_calc);
				// debug("selector wt = %s m (per sec) ", m_selector_wt_per_sec);

				m_work_time_calc = 0;
			}

			//
			// if(
			if (m_write_avg.isTimeover(3000)) {
				m_write_bytes = m_write_avg.getLong();
			}

			checkChannelAlive();

			if (m_changeRequests.size() == 0) {
				TimeUtil.sleep(1);
			}

			// wait for event's done

		} // while

		debugLocate = 111;
	}

	private void checkChannelAlive() {

		try {
			if (tmrCheckChannelAlive.end_sec() > 60 * 10) {// every 10 min
				tmrCheckChannelAlive.start();

				synchronized (m_channelsLastRecv) {

					for (Map.Entry<SocketChannel, TimeUtil> entry : m_channelsLastRecv.entrySet()) {
						// System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());

						if (entry.getValue().end_sec() > NO_RECV_TIME_OUT_SEC) {

							synchronized (this.m_changeRequests) {
								debug("add remove -3.1 ");
								this.m_changeRequests
										.add(new NioChangeRequest(entry.getKey(), NioChangeRequest.REMOVE, 0));

							}

						}

					} // for

				} // sync

			} // if(
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param socket
	 */
	private void removeChannel(SelectionKey key, SocketChannel ch) {

		try {
			if (key != null) {
				key.cancel();
			}
		} catch (Exception e) {
			;
		}

		try {

			NqChannelListener listen = null;// 200330
			synchronized (m_channels) {
				if (m_channels.containsKey(ch)) {

					listen = m_channels.get(ch);
				}
			}

			if (listen != null) {

				listen.disconnected(ch);
				// addEvent(new EventItem(listen, ch, EventItem.DISCONNECT, null));

			}

		} catch (Exception e2) {
			e2.printStackTrace();
		}

		try {
			// if (ch != null) {
			synchronized (m_channels) {

				m_channels.remove(ch);

			}
			synchronized (m_channelsLastRecv) {
				m_channelsLastRecv.remove(ch);// 200407
			}
		} catch (Exception e) {
			;
		}

		//
		try {
			ch.socket().close();
		} catch (Exception e) {
			;
		}

		//
		try {
			ch.close();
		} catch (Exception e) {
			;
		}

		try {
			synchronized (this.m_pendingData) {
				m_pendingData.remove(ch);
			}
		} catch (Exception e) {
			;
		}

		try {
			synchronized (m_connecting) {
				m_connecting.remove(ch);
				m_connecting_elapse.remove(ch);
			}
		} catch (Exception e) {

		}

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
	 * @param key
	 * @throws IOException
	 */
	private void onAccept(SelectionKey key) throws IOException {

		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel channel = serverChannel.accept();
		channel.configureBlocking(false);

		// addEvent(new EventItem(m_svr, channel, EventItem.ACCEPT, this));

		try {
			NqChannelListener rs = m_svr.accepted(this, channel);
			if (rs != null) {
				accept(channel, rs);
			} else {
				channelClose(channel);
			}
		} catch (Exception e) {
			debug("error in accept %s ", e);
			channelClose(channel);
		}

	}

	/**
	 * 
	 * 
	 * @param channel
	 * @param asc
	 * @throws IOException
	 */
	private void accept(SocketChannel channel, NqChannelListener asc) throws IOException {

		synchronized (m_channels) {

			m_channels.put(channel, asc);

		}

		synchronized (m_channelsLastRecv) {
			m_channelsLastRecv.put(channel, new TimeUtil()); // 200407
		}
		channel.register(this.m_selector, SelectionKey.OP_READ);

	}

	/**
	 * protocol type 1
	 * 
	 * @param key
	 * @param buffer
	 * @throws IOException
	 */
	private void read(SelectionKey key, ByteBuffer buffer) throws IOException {

		SocketChannel channel = (SocketChannel) key.channel();

		buffer.clear();

		int numRead = -1;
		try {
			numRead = channel.read(buffer);
		} catch (IOException e) {

			synchronized (m_changeRequests) {
				debug("add remove -1 err"+e);
				this.m_changeRequests.add(new NioChangeRequest(channel, NioChangeRequest.REMOVE, 0));
			}
			return;
 			
		}

		if (numRead < 0) {

			// removeChannel(key, channel);
			synchronized (m_changeRequests) {
				debug("add remove -1 ");
				this.m_changeRequests.add(new NioChangeRequest(channel, NioChangeRequest.REMOVE, 0));
			}
			return;
		}

		buffer.flip();

		byte[] data = new byte[numRead];
		System.arraycopy(buffer.array(), 0, data, 0, numRead);

		m_recv_bytes += numRead;

		m_read_avg.sum(numRead);

		if (m_read_avg.isTimeover(3000)) {
			m_recv_bytes = m_read_avg.getLong();
			// debug("average of read bytes %s ", StringUtil.formatBytesSize());
		}

		//
		//
		m_event_cnt++;
		if (m_events_calc_time.end_sec() > 1.0) {
			m_events_calc_time.start();
			m_events_per_sec = m_event_cnt;
			m_event_cnt = 0;
		}

		// try {
		// int cnt = (m_channels.size() * 10);
		// while (m_eventDatas.size() > cnt) {
		// TimeUtil.sleep(10);
		// }
		// } catch (Exception e) {
		//
		// }

		// debug("recv (%s) ", numRead);

		try {

			debugLocate = 10821;
			// submit
			NqChannelListener ch = null;
			synchronized (m_channels) {
				ch = m_channels.get(channel);
			}
			debugLocate = 10822;

			// addEvent(new EventItem(ch, channel, EventItem.RECV, data));
			ch.received(channel, data, data.length);

			synchronized (m_channelsLastRecv) {
				m_channelsLastRecv.get(channel).start();// 200407 updat last receive time
			}
			debugLocate = 10823;
		} catch (Exception e) {
			e.printStackTrace();
		}

		// ep.append(data, data.length);

	}

	/**
	 * 
	 * 
	 * 
	 * @param ch
	 */

	public void channelClose(SocketChannel ch) {

		if (ch == null)
			return;

		if (isContains(ch) == false)
			return;

		synchronized (this.m_changeRequests) {
			debug("add remove -2 ");
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

	public boolean isContains(SocketChannel socket) {

		synchronized (m_channels) {
			if (m_channels.containsKey(socket)) {
				return true;
			}
			return false;
		}
	}

	/**
	 * 
	 * @param socket
	 * @param data
	 */
	public void send(SocketChannel socket, byte[] data) throws Exception {

		// debug("send (%s)",data.length);

		//
		if (isContains(socket) == false) {
			// throw new UserException("isn't containing socketchannel");
			// return; // already closed
			return;
		}
		// }

		synchronized (this.m_changeRequests) {
			// Indicate we want the interest ops set changed
			this.m_changeRequests.add(new NioChangeRequest(socket, NioChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
		}
		// And queue the data we want written
		synchronized (this.m_pendingData) {
			List queue = (List) this.m_pendingData.get(socket);
			if (queue == null) {
				queue = new ArrayList();
				this.m_pendingData.put(socket, queue);
			}
			queue.add(ByteBuffer.wrap(data));
		}

		// Finally, wake up our selecting thread so it can make the required
		// changes
		// this.m_selector.wakeup();

	}

	/**
	 * 
	 * @param key
	 * @throws IOException
	 */
	private void write(SelectionKey key) throws IOException {

		SocketChannel socketChannel = (SocketChannel) key.channel();

		synchronized (this.m_pendingData) {
			List queue = (List) this.m_pendingData.get(socketChannel);

			if (queue != null) {

				try {
					// Write until there's not more data ...
					while (!queue.isEmpty()) {
						ByteBuffer buf = (ByteBuffer) queue.get(0);

						socketChannel.write(buf);

						// debug("write(%s) remaining(%s)", buf.position(), buf.remaining());

						long ns = System.currentTimeMillis();
						 while (buf.remaining() > 0) {
						
							// ... or the socket's buffer fills up

							socketChannel.write(buf);

//							if ((System.currentTimeMillis() - ns) > (3 * 1000)) {// max 10ms
//								// debug("selector write remained ");
//
//								// key.interestOps(SelectionKey.OP_WRITE);// try it again
//								// 20200518 break;
//								debug("write timeover ");
//								socketChannel.close();
//								// return;
//							}
							// }
						}

						m_write_avg.sum(buf.position());

						buf.clear();

						queue.remove(0);
					}
				} catch (Exception e) {
					// e.printStackTrace();
					debug("close in write");
					// queue.clear();

					// removeChannel(key, socketChannel);
					synchronized (m_changeRequests) {
						debug("add remove -3 ");
						this.m_changeRequests.add(new NioChangeRequest(socketChannel, NioChangeRequest.REMOVE, 0));
					}
					return;
				}

				if (queue.isEmpty()) {
					key.interestOps(SelectionKey.OP_READ);
					m_pendingData.remove(socketChannel);

				}
			} else {
				key.interestOps(SelectionKey.OP_READ);
			}
		} // sync
	}

	/**
	 * 
	 * 
	 * 
	 */
	@Override
	public String toString() {

		String s = "nqselector.rv1@" + this.hashCode();

		s += String.format("sel cnt(%s) ", m_total_selector_count);

		s += String.format("port(%s) ch(%s) requests(%s) pending data(%s) connecting(%s) ", m_port,
				this.m_channels.size(), this.m_changeRequests.size(), this.m_pendingData.size(), m_connecting.size());

		s += String.format("   wt(%s)ms  ", userage());

		// s += String.format("tcnt(%s) ac(%s) ps(%s) qs(%s) "//
		// , m_thpool_count2, m_executor.getActiveCount(), m_executor.getPoolSize(), m_executor.getQueue().size());

		s += String.format(" recv(%s) write(%s) events(%s) ", StringUtil.formatBytesSize(m_recv_bytes),
				StringUtil.formatBytesSize(m_write_bytes), StringUtil.formatCount(m_events_per_sec));

		// s += "<br>";
		// for (Timer2 t : m_eventThread) {
		// s += t;
		// s += "<br>";
		// }

		return s;
	}

	/**
	 * userage calculating
	 * 
	 * @param wt
	 */

	public void userage_calc(long wt) {

		synchronized (m_userages) {

			if (wt == 0)
				return;

			m_userages.add((int) (wt / 1000000));

			if (m_userages.size() > 32) {
				m_userages.remove(0);
			}

			try {
				int n = m_userages.size();
				long sum = 0;
				for (int v : m_userages) {
					sum += v;
				}

				m_userage_average = (int) ((sum / n) * 0.1);
			} catch (Exception e) {

			}

		}

		return;
	}

	/**
	 * 
	 * performance userage value : 0~100%
	 * 
	 * @return
	 */
	public int userage() {

		return m_userage_average;
	}

	/**
	 * 
	 * 
	 * @return
	 * @throws IOException
	 */
	public SocketChannel connect(String ip, int port, NqChannelListener listen) throws Exception {

		if (m_selector == null)
			throw new UserException("selector is not creat %s ", m_selector);

		// Create a non-blocking socket channel
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);

		// Kick off connection establishment
		socketChannel.connect(new InetSocketAddress(ip, port));

		synchronized (m_connecting) {
			m_connecting.put(socketChannel, listen);
			m_connecting_elapse.put(socketChannel, new TimeUtil());
		}

		synchronized (this.m_changeRequests) {
			this.m_changeRequests
					.add(new NioChangeRequest(socketChannel, NioChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
			// this.m_selector.wakeup();
		}

		// Finally, wake up our selecting thread so it can make the required changes

		return socketChannel;
	}

	/**
	 * 
	 * @param key
	 * @throws IOException
	 */
	private void finishConnection(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();

		// Finish the connection. If the connection operation failed
		// this will raise an IOException.
		try {
			channel.finishConnect();
		} catch (IOException e) {

			synchronized (m_changeRequests) {
				debug("add remove -4 ");
				this.m_changeRequests.add(new NioChangeRequest(channel, NioChangeRequest.REMOVE, 0));
			}
			debug("finishConnection err!  %s", e);

			try {
				synchronized (m_connecting) {
					m_connecting.remove(channel);
					m_connecting_elapse.remove(channel);
				}
			} catch (Exception ee) {

			}

			return;
		}

		try {

			//

			NqChannelListener listen = null;
			synchronized (m_connecting) {
				listen = m_connecting.remove(channel);
				m_connecting_elapse.remove(channel);
			}

			synchronized (m_channels) {
				m_channels.put(channel, listen);
			}
			synchronized (m_channelsLastRecv) {
				m_channelsLastRecv.put(channel, new TimeUtil());// 200407
			}
			// addEvent(new EventItem(listen, channel, EventItem.CONNECT, null));
			listen.connected(channel);

		} catch (Exception e) {
			// removeChannel(key, channel);
			synchronized (m_changeRequests) {
				debug("add remove -5 ");
				this.m_changeRequests.add(new NioChangeRequest(channel, NioChangeRequest.REMOVE, 0));
			}

		}

		// Register an interest in writing on this channel
		key.interestOps(SelectionKey.OP_READ);

	}

	/**
	 * 
	 * @param ch
	 * @return
	 */
	public boolean isConnecting(SocketChannel ch) {

		if (ch == null)
			return false;

		synchronized (m_connecting) {

			// 190930 bugfix!
			TimeUtil t = m_connecting_elapse.get(ch);
			if (t != null) {
				if (t.end_sec() > 32) {
					System.out.println(" connecting timevoer!!!" + ch);

					try {
						m_connecting.remove(ch);
						m_connecting_elapse.remove(ch);
						ch.close();
					} catch (Exception e) {

					}

				}
			}

			return m_connecting.containsKey(ch);

		}

	}

	/**
	 * 
	 * @return
	 */
	public String getLocalAddress() {
		if (isAlive() == false)
			return null;

		try {
			return m_serverChannel.getLocalAddress().toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	/**
	 * 
	 * @return
	 */
	public boolean isPeddingOverload() {
		synchronized (m_pendingData) {

			if ((m_channels.size() * 32) < m_pendingData.size()) {
				return true;
			}
			return false;
		}
	}
}
