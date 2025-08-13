package deprecated.lib.net.nio.old;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.*;

import home.lib.lang.Timer2;
import home.lib.lang.UserException;
import home.lib.util.AverageUtil;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

/**
 * 
 * guild site link: http://rox-xmlrpc.sourceforge.net/niotut/#The server
 * 
 * @author livingroom-user
 *
 */
public class NqSelector {

	NqSelector _this = this;

	final Object m_lock = new Object();
	private int m_port;
	private Selector m_selector;
	private ServerSocketChannel m_serverChannel;

	// use it regardless of protocol option
	private ConcurrentMap<SocketChannel, NqChannelListener> m_channels = new ConcurrentHashMap<SocketChannel, NqChannelListener>();

	private ConcurrentMap<SocketChannel, NqChannelListener> m_connecting = new ConcurrentHashMap<SocketChannel, NqChannelListener>();

	//
	private boolean m_alive2 = false;

	// A list of MqChangeRequest instances
	private List<NioChangeRequest> m_changeRequests = new LinkedList<>();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map<SocketChannel, List<ByteBuffer>> m_pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();

	private ThreadPoolExecutor m_executor = null;

	private int m_thpool_count2 = 0;

	private long m_write_bytes = 0;

	private long m_recv_bytes = 0;

	private long m_work_time_calc = 0;

	Timer2 m_eventThread[] = new Timer2[2];

	// public long m_selector_wt_per_sec;
	ArrayList<Integer> m_userages = new ArrayList<Integer>();

	private int m_userage_average = 0;

	boolean recevie_with_pool = false;

	AverageUtil m_read_avg = new AverageUtil();
	
	int cloesCallCount=0;

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

	//
	/**
	 *
	 * @author richard
	 *
	 */
	class EventItem {

		public static final byte CONNECT = 1;
		public static final byte RECV = 2;
		public static final byte DISCONNECT = 3;
		public static final byte ACCEPT = 4;

		NqChannelListener listen;
		SocketChannel ch;
		byte[] data;
		byte ops = 0;
		NqSelectorListener svrlisten;
		NqSelector sel;

		public EventItem(NqChannelListener l, SocketChannel c, byte o, byte[] d) {
			listen = l;
			ch = c;
			data = d;
			ops = o;
		}

		public EventItem(NqSelectorListener l, SocketChannel c, byte o, NqSelector s) {
			svrlisten = l;
			ch = c;
			sel = s;
			ops = o;
		}

	}

	// the problem is broadcast in server!!!!

	// private Map<SocketChannel, ArrayList<EventItem>> m_eventDatas = new HashMap<SocketChannel,
	// ArrayList<EventItem>>();
	private ArrayList<EventItem> m_eventDatas = new ArrayList<EventItem>();
	public static Map<SocketChannel, String> currentWorkSocket = new HashMap<SocketChannel, String>();

	/**
	 * 
	 * @param e
	 */
	private void addEvent(EventItem e) {
		synchronized (m_eventDatas) {
			// ArrayList<EventItem> queue = m_eventDatas.get(e.ch);
			// if (queue == null) {
			// queue = new ArrayList<EventItem>();
			// m_eventDatas.put(e.ch, queue);
			// }
			// queue.add(e);
			//
			// m_eventList.add(e.ch);

			m_eventDatas.add(e);
		}
	}

	/**
	 * 
	 * @return
	 */
	private EventItem pollFirstEvent() {
		synchronized (m_eventDatas) {
			// if (m_eventList.size() > 0) {
			//
			// SocketChannel ch = m_eventList.remove(0);
			// if (ch == null)
			// return null;
			// else
			// return m_eventDatas.remove(ch);
			//
			// }
			// return null;

			for (int h = 0; h < m_eventDatas.size() && h < 128; h++) {
				EventItem e = m_eventDatas.get(h);

				synchronized (currentWorkSocket) {
					if (currentWorkSocket.get(e.ch) == null) {

						currentWorkSocket.put(e.ch, "");

						return m_eventDatas.remove(h);
					}

				}

			}
		} // syn

		return null;
	}

	/**
	 * 
	 * @param e
	 */
	private void releaseChannelEvent(SocketChannel ch) {
		synchronized (currentWorkSocket) {

			currentWorkSocket.remove(ch);
		} // syn

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

		synchronized (m_lock) {

			if (m_executor != null)
				throw new UserException("selector is alive");

			m_executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
		}

		//
		// start select thread
		//
		Callable<Object> task = () -> {

			synchronized (m_lock) {

				if (isAlive())
					throw new UserException("selector is alive");

				this.m_port = port;
				this.m_selector = SelectorProvider.provider().openSelector();// Selector.open();
				m_serverChannel = ServerSocketChannel.open();
				m_serverChannel.configureBlocking(false);
				InetSocketAddress listenAddr = new InetSocketAddress(ip, port);
				m_serverChannel.socket().bind(listenAddr);
				m_serverChannel.register(this.m_selector, SelectionKey.OP_ACCEPT);

			}

			try {
				m_alive2 = true;
				//

				// start recv thread
				for (int h = 0; h < m_eventThread.length; h++) {
					m_eventThread[h] = new Timer2().schedule(new eventWorker(), 10, 10);
				}

				doSelectAction();

			} catch (IOException e) {
				e.printStackTrace();

			} finally {

				m_alive2 = false;

				//
				for (int h = 0; h < m_eventThread.length; h++) {
					m_eventThread[h].cancel();
				}

			}
			debug("finish!!! (%s:%s) ", ip, port);

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

	// private int sequence_id = 0;

	long m_event_cnt = 0;
	long m_events_per_sec = 0;

	
	class eventWorker extends TimerTask {

		TimeUtil t = new TimeUtil();
		// long loopCnt = 0;
		long inOut = 0;


		/**
		 * 
		 * 
		 */
		public eventWorker() {
			// my_id = sequence_id;
		}

		/**
		 * 
		 * 
		 */
		public void run() {

			int cnt = 0;
			while (cnt < 128) {
				cnt++;
				SocketChannel cw = null;
				// long ss = System.nanoTime();

				EventItem r = pollFirstEvent();

				if (r == null)
					return;

				cw = r.ch;
				m_thpool_count2++;
				// debug("in");
				inOut++;

				try {

					//
					//
					if (r != null && r.listen != null) {// not null & channel is alive
						synchronized (r.listen._lock()) {

							if (r.ops == EventItem.RECV) {

								if (m_port == 0) {
									String s = String.format("test");
									// String b=s;
									// debug("recv sk(%s)", r.ch);
								}

								// && m_channels.get(r.ch) != null
								r.listen.received(r.ch, r.data, r.data.length);

							} else if (r.ops == EventItem.CONNECT) {

								r.listen.connected(r.ch);

							} else if (r.ops == EventItem.DISCONNECT) {

								r.listen.disconnected(r.ch);

							}
						} // sync
					} // if( connect, disconnect, recv

					if (r != null && r.svrlisten != null) {
						if (r.ops == EventItem.ACCEPT) {
							try {
								NqChannelListener rs = r.svrlisten.accepted(r.sel, r.ch);
								if (rs != null) {
									accept(r.ch, rs);
								} else {
									channelClose(r.ch);
								}
							} catch (Exception e) {
								debug("error in accept %s ", e);
								channelClose(r.ch);
							}

						}

					} // if( accept

					TimeUtil.sleep(1);
				} catch (Exception e) {

				} finally {
					// debug("out");
					// long ee = System.nanoTime();
					// m_work_time_calc += (ee - ss);

					releaseChannelEvent(cw);

					m_thpool_count2--;
					inOut--;

					// debug("current work sock count=%s ", currentWorkSocket.size());
				}

				m_event_cnt++;
				if (t.end_sec() > 1) {
					t.start();
					m_events_per_sec=m_event_cnt;
					//debug("events per sec %s ", m_event_cnt);
				}

				// TimeUtil.sleep(1);

				// loopCnt++;
				// if (t.end_sec() > 3) {
				// t.start();
				// debug("event th cnt=%s inout=%s ", loopCnt, inOut);
				//
				// }
				TimeUtil.sleep(1);
			}

		}// while(infinite
	}

	/**
	 * 
	 * @param ms
	 */
	public void sleep(long ms) {
		try {
			Thread.sleep(10);
		} catch (Exception e) {
			;
		}
	}

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
		
		cloesCallCount++;
		
		synchronized (m_lock) {
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

			try {
				m_executor.shutdownNow();
			} catch (Exception e) {
				// e.printStackTrace();
			}
			m_executor = null;
		}
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

			System.out.println(TimeUtil.now() + " mqselector(p:" + m_port + ") " + s);
			return s;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
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

		TimeUtil tmWorkTime = new TimeUtil();

		ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 1024);// 1m

		while (true) {

			long ss = System.nanoTime();
			// try {

			synchronized (this.m_changeRequests) {

				try {

					Iterator changes = this.m_changeRequests.iterator();
					while (changes.hasNext()) {
						NioChangeRequest change = (NioChangeRequest) changes.next();

						SelectionKey key = null;

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
					} // for

				} catch (Exception e) {
					e.printStackTrace();
				}

				m_changeRequests.clear();
			} // sync

			long ee = System.nanoTime();

			int selCount = this.m_selector.selectNow();

			//
			long ss1 = System.nanoTime();
			if (selCount > 0) {

				try {
					// wakeup to work on selected keys
					Iterator<SelectionKey> keys = this.m_selector.selectedKeys().iterator();

					while (keys.hasNext()) {
						SelectionKey key = (SelectionKey) keys.next();

						// this is necessary to prevent the same key from coming up
						// again the next time around.
						keys.remove();

						// System.out.println("broker test----1");

						synchronized (m_lock) {
							try {
								if (!key.isValid()) {

									continue;
								}

								if (key.isAcceptable()) {

									try {
										this.onAccept(key);
									} catch (Exception e) {
										e.printStackTrace();
									}
								} else if (key.isReadable()) {

									// if (m_protocol_option == 0) {
									// readBytes(key, readBuffer);
									// } else {

									// System.out.println("broker test----2 read");
									try {
										read(key, readBuffer);
									} catch (Exception exr) {
										exr.printStackTrace();
									}
									// }

								} else if (key.isWritable()) {
									try {
										this.write(key);
									} catch (Exception exw) {
										exw.printStackTrace();
									}
								} else if (key.isConnectable()) {
									this.finishConnection(key);
								}

							} catch (Exception e) {
								e.printStackTrace();
							}
						} // sync
					} // while

				} catch (Exception e) {

					e.printStackTrace();
				}
			}
			long ee1 = System.nanoTime();

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

			// TimeUtil.sleep(3);

			// wait for event's done

		} // while
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

			if (m_channels.containsKey(ch)) {

				NqChannelListener listen = m_channels.get(ch);
				if (ch != null) {

					addEvent(new EventItem(listen, ch, EventItem.DISCONNECT, null));

				}

			}
		} catch (Exception e2) {
			e2.printStackTrace();
		}

		try {
			// if (ch != null) {
			synchronized (m_channels) {

				m_channels.remove(ch);

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
			}
		} catch (Exception e) {

		}

		try {
			synchronized (m_eventDatas) {
				m_eventDatas.remove(ch);
			}
		} catch (Exception e) {

		}
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

		addEvent(new EventItem(m_svr, channel, EventItem.ACCEPT, this));

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

			numRead = 0;
			// e.printStackTrace();
		}

		if (numRead <= 0) {

			// removeChannel(key, channel);
			this.m_changeRequests.add(new NioChangeRequest(channel, NioChangeRequest.REMOVE, 0));
			return;
		}

		buffer.flip();

		byte[] data = new byte[numRead];
		System.arraycopy(buffer.array(), 0, data, 0, numRead);

		m_recv_bytes += numRead;

		m_read_avg.sum(numRead);

		if (m_read_avg.isTimeover(3000)) {
			debug("average of read bytes %s ", StringUtil.formatBytesSize(m_read_avg.getLong()));
		}

		try {
			int cnt = (m_channels.size() * 10);
			while (m_eventDatas.size() > cnt) {
				TimeUtil.sleep(10);
			}
		} catch (Exception e) {

		}

		// debug("recv (%s) ", numRead);
		try {

			// submit
			NqChannelListener ch = m_channels.get(channel);

			addEvent(new EventItem(ch, channel, EventItem.RECV, data));

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

		synchronized (this.m_changeRequests) {

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

	/**
	 * 
	 * @param socket
	 * @param data
	 */
	public void send(SocketChannel socket, byte[] data) {

		// debug("send (%s)",data.length);

		synchronized (m_channels) {
			if (m_channels.containsKey(socket) == false) {
				debug("write: target channel is closed");
				return; // already closed
			}
		}

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

						long ns = System.nanoTime();
						while (buf.remaining() > 0) {
							// ... or the socket's buffer fills up

							socketChannel.write(buf);

							if ((System.nanoTime() - ns) > (10 * 1000000)) {// max 10ms
								// debug("selector write remained ");

								// key.interestOps(SelectionKey.OP_WRITE);// try it again
								break;
								// return;
							}
							// }
						}

						m_write_bytes += buf.position();

						buf.clear();

						queue.remove(0);
					}
				} catch (Exception e) {
					// e.printStackTrace();
					debug("close in write");
					// queue.clear();

					// removeChannel(key, socketChannel);
					this.m_changeRequests.add(new NioChangeRequest(socketChannel, NioChangeRequest.REMOVE, 0));

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

		String s = "mqselector-2 @" + this.hashCode();
		s += String.format("port(%s) ch(%s) requests(%s) pending data(%s) connecting(%s) ", m_port,
				this.m_channels.size(), this.m_changeRequests.size(), this.m_pendingData.size(), m_connecting.size());

		s += String.format(" recv.queue(%s) cws(%s)  wt(%s)ms ", m_eventDatas.size(), currentWorkSocket.size(),
				userage());

//		s += String.format("tcnt(%s) ac(%s) ps(%s)  qs(%s)     "//
//				, m_thpool_count2, m_executor.getActiveCount(), m_executor.getPoolSize(), m_executor.getQueue().size());

		s += String.format(" recv(%s) write(%s) events(%s)", StringUtil.formatBytesSize(m_recv_bytes),
				StringUtil.formatBytesSize(m_write_bytes),StringUtil.formatCount( m_events_per_sec ) );

		s += "<br>";
		for (Timer2 t : m_eventThread) {
			s += t;
			s += "<br>";
		}

		return s;
	}

 
	/**
	 * userage calculating
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
			throw new UserException("selector is not creat %s  cc(%s) ", m_selector,cloesCallCount );

		// Create a non-blocking socket channel
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);

		// Kick off connection establishment
		socketChannel.connect(new InetSocketAddress(ip, port));

		synchronized (m_connecting) {
			m_connecting.put(socketChannel, listen);
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

			this.m_changeRequests.add(new NioChangeRequest(channel, NioChangeRequest.REMOVE, 0));
			debug("finishConnection err!  %s", e);

			return;
		}

		try {

			//

			NqChannelListener listen = null;
			synchronized (m_connecting) {
				listen = m_connecting.remove(channel);
			}

			synchronized (m_channels) {
				m_channels.put(channel, listen);
			}

			addEvent(new EventItem(listen, channel, EventItem.CONNECT, null));

		} catch (Exception e) {
			// removeChannel(key, channel);
			this.m_changeRequests.add(new NioChangeRequest(channel, NioChangeRequest.REMOVE, 0));

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

}
