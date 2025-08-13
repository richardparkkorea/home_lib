package home.lib.net.nio.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import home.lib.lang.UserException;
import home.lib.log.MyLogger;
import home.lib.net.tms.mqtt.MqttBroker;
import home.lib.util.TimeUtil;

/*
 * 
 * https://gist.github.com/ochinchina/72cc23220dc8a933fc46
 * 
 * 
 * https://www.developer.com/java/data/understanding-asynchronous-socket-channels-in-java.html
 * 
 * 
 * 
 */
public class AsyncSocket {

	private static ExecutorService m_executor = Executors.newFixedThreadPool(16);

	/**
	 * 
	 * @author richard
	 *
	 */
	private class ReadAttachment {
		AsynchronousSocketChannel channel;
		ByteBuffer buffer;
		AsyncSocketListener listener = null;
		String ip = "";

		boolean isAccept = false;

		public ReadAttachment() {
			buffer = ByteBuffer.allocate(0);
		}
	}

	/**
	 * 
	 * @author richard
	 *
	 */
	private class WriteAttachment {
		AsynchronousSocketChannel channel;

		boolean isWriting = false;
		Deque<ByteBuffer> writeData = new LinkedList<ByteBuffer>();
		AsyncSocketListener listener = null;
		String ip = "";

		boolean isAccept = false;

		public WriteAttachment() {

		}
	}

	/**
	 * 
	 */
	private AsyncSocket _this = this;
	private Object syncClose = new Object();

	private AsynchronousSocketChannel m_channel = null;

	// private MqChannelInterface m_listener1 = null;
	private OpenHandler openHandler = new OpenHandler();
	private ReadHandler readHandler = new ReadHandler();
	private WriteHandler writeHandler = new WriteHandler();
	private WriteAttachment writeAttach = null;// new WriteAttachment();

	private Object m_user_obj = null;

	private long m_socketBufferSize = 1024 * 8;
	// private boolean m_isConnecting = false;
	private InetSocketAddress m_bind = null;
	private Object syncEvent = new Object();
	private boolean m_isAccept = false;

	protected AsyncSocketListener m_inter = null;

	/**
	 * 
	 */
	public AsyncSocket() {
	}

	/**
	 * from connect and bind
	 * 
	 * @return
	 */
	public AsynchronousSocketChannel getChannel() {
		return m_channel;
	}

	/**
	 * 
	 * @param b
	 * @return
	 * @throws Exception
	 */
	public AsyncSocket setIpBind(InetSocketAddress b) throws Exception {

		m_bind = b;

		return this;
	}

	/**
	 * 
	 * @return
	 */
	public Object getUserObject() {
		return m_user_obj;
	}

	/**
	 * 
	 * @param o
	 * @return
	 */
	public AsyncSocket setUserObject(Object o) {
		m_user_obj = o;
		return this;
	}

	public boolean isAccept() {
		return m_isAccept;
	}

	/**
	 * 
	 * @param l
	 * @return
	 */
	public AsyncSocket setSocketBufferSize(long l) {
		m_socketBufferSize = l;
		return this;
	}

	public long socketBufferSize() {
		return m_socketBufferSize;
	}

	// /**
	// *
	// * @return
	// */
	// public boolean isConnecting() {
	// return m_isConnecting;
	// }

	/**
	 * 
	 * @param ip
	 * @param port
	 * @param l
	 * @return
	 * @throws Exception
	 */
	public AsyncSocket connect(final String ip, final int port, final AsyncSocketListener l) throws Exception {

		synchronized (m_executor) {
			// m_listener = l;
			// writeAttach.listener=l;
			debug("connect(%s:%d)", ip, port);

			if (isAlive()) {
				throw new UserException("already connected");
			}

			Callable<Boolean> task = () -> {
				try {

					AsynchronousSocketChannel ch = AsynchronousSocketChannel.open();
					if (m_bind != null) {
						ch.bind(m_bind);
					}

					SocketAddress serverAddr = new InetSocketAddress(ip, port);

					ReadAttachment openAttach = new ReadAttachment();
					openAttach.channel = ch;
					openAttach.buffer = ByteBuffer.allocate((int) m_socketBufferSize);
					openAttach.listener = l;

					try {
						openAttach.ip = ip + ":" + port;
					} catch (Exception e) {
						;
					}

					ch.connect(serverAddr, openAttach, openHandler);

					return isAlive();
				} catch (Exception e) {

					// e.printStackTrace();

					this.close();

					throw new IllegalStateException("task interrupted", e);

				} finally {

					if (isAlive()) {
						try {
							l.connected(_this);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

			};

			Future<Boolean> r = m_executor.submit(task);

			// return r;
			return this;
		} // sync
	}

	/**
	 * 
	 * 
	 * @param d
	 * @return
	 */
	public AsyncSocket waitConnect(double d) {
		TimeUtil t = new TimeUtil();

		while (t.end_sec() < d && isAlive() == false) {
			TimeUtil.sleep(10);
		}

		if (isAlive() == false) {
			close();
		}

		return this;
	}

	/**
	 * 
	 * 
	 * @param f
	 * @param p
	 * @return
	 */
	public static String debug(String f, Object... p) {

		String m = "asyncsocket " + String.format(f, p);

		if ((MqttBroker.debugLevel & MyLogger.DEBUG) != 0) {

			System.out.println(m);
		}
		return m;
	}

	/**
	 * 
	 * @param ch
	 * @param l
	 * @return
	 */
	public AsyncSocket accept(AsynchronousSocketChannel ch, AsyncSocketListener l) throws Exception {

		m_isAccept = true;

		// m_listener = l;

		writeAttach = new WriteAttachment();
		writeAttach.listener = l;
		writeAttach.channel = ch;
		writeAttach.isAccept = true;
		try {
			writeAttach.ip = ch.getLocalAddress().toString();
		} catch (Exception e) {

		}

		//
		//

		ReadAttachment attach = new ReadAttachment();
		attach.channel = ch;
		attach.buffer = ByteBuffer.allocate((int) m_socketBufferSize);
		// attach.isRead = true;
		attach.buffer.flip();
		attach.buffer.clear();
		attach.listener = l;

		attach.isAccept = true;
		try {
			attach.ip = ch.getLocalAddress().toString();
		} catch (Exception e) {

		}

		m_channel = ch;

		try {
			l.connected(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// m_channel.write(attach.buffer, attach, readHandler);// start read

		m_channel.read(attach.buffer, attach, readHandler);

		return this;
	}

	// /**
	// *
	// * @return
	// */
	// public AsynchronousSocketChannel getChannel() {
	// return m_channel;
	// }

	/**
	 * 
	 * @return
	 */
	public boolean isAlive() {

		// asynchrous socket
		if (m_channel != null) {
			if (m_channel.isOpen())
				return true;
		}

		return false;
	}

	/**
	 * 
	 * 
	 */
	public void close() {
		synchronized (syncClose) {

			// asynchroous socket
			if (m_channel != null) {

				// //
				try {
					m_channel.close();

				} catch (IOException e1) {
					// e1.printStackTrace();
				}

			}

		} // sync
	}

	/**
	 * 
	 * @param data
	 * @return
	 */
	public boolean send(byte[] data) {

		if (isAlive() == false)
			return false;

		synchronized (writeAttach.writeData) {

			ByteBuffer buffer = ByteBuffer.allocate(data.length);
			buffer.put(data);
			buffer.flip();

			writeAttach.writeData.add(buffer);

			if (writeAttach.isWriting) {
				return true;
			}

			writeAttach.isWriting = true;

			m_channel.write(buffer, writeAttach, writeHandler);// start read

		} // sync

		return true;

	}

	/**
	 *
	 *
	 *
	 *
	 * @author richard
	 *
	 */
	class OpenHandler implements CompletionHandler<Void, ReadAttachment> {
		// AsynchronousSocketChannel channel = null;

		@Override
		public void completed(Void result, ReadAttachment attach) {

			synchronized (syncEvent) {
				// debug("connected~" + attach);
				// m_isConnecting = false;

				writeAttach = new WriteAttachment();
				writeAttach.channel = attach.channel;
				writeAttach.listener = attach.listener;
				writeAttach.ip = attach.ip;

				ReadAttachment readAttach = new ReadAttachment();
				readAttach.channel = attach.channel;
				readAttach.buffer = ByteBuffer.allocate((int) m_socketBufferSize);
				// readAttach.isRead = true;
				readAttach.buffer.flip();
				readAttach.buffer.clear();
				readAttach.listener = attach.listener;
				readAttach.ip = attach.ip;
				// alloc channel handle
				m_channel = attach.channel;

				try {
					// m_listener.connected(_this);
					attach.listener.connected(_this);
				} catch (Exception e) {
					e.printStackTrace();
				}

				try {

					m_channel.read(readAttach.buffer, readAttach, readHandler);

				} catch (Exception e) {
					// close_event();
					try {
						attach.channel.close();
					} catch (Exception e1) {

					}
					try {
						attach.listener.disconnected(_this);
					} catch (Exception e2) {

					}
				}
			} // sync
		}

		/**
		 *
		 *
		 * @param e
		 * @param attach
		 */
		@Override
		public void failed(Throwable e, ReadAttachment attach) {
			synchronized (syncEvent) {

				// close_event();
				try {
					debug("close-----0------" + attach.ip + " " + e);
					attach.channel.close();
				} catch (Exception e1) {

				}
				try {
					attach.listener.disconnected(_this);
				} catch (Exception e2) {

				}
			} // sync

		}

	}

	/**
	 * 
	 * @author richardpark
	 *
	 */
	private class ReadHandler implements CompletionHandler<Integer, ReadAttachment> {
		// AsynchronousSocketChannel channel = null;

		@Override
		public void completed(Integer result, ReadAttachment attach) {
			synchronized (syncEvent) {
				// debug(attach + " read completed and " + result + " bytes are read. isaccept=" + isAccept());

				if (attach == null)
					return;

				if (result < 0) {

					// close_event();
					try {
						attach.channel.close();
					} catch (Exception e1) {

					}
					try {
						attach.listener.disconnected(_this);
					} catch (Exception e2) {

					}
					debug("close----1--" + attach.ip);
					return;
				}

				if (isAlive() == false)
					return;

				if (result > 0) {
					// System.out.println("ch="+ attach.channel.isOpen() +" result="+ result );

					attach.buffer.flip();
					int limits = attach.buffer.limit();
					byte bytes[] = new byte[limits];
					attach.buffer.get(bytes, 0, limits);

					// System.out.format(" read len= %d (%s) \r\b", bytes.length ,attach );
					//
					try {
						attach.listener.recv(_this, bytes, bytes.length);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				// attach.isRead = true;
				attach.buffer.clear();
				attach.channel.read(attach.buffer, attach, this);

				// System.out.format("read (%s) \r\n", attach);
			} // sync
		}

		/**
		 * 
		 * 
		 * @param e
		 * @param attach
		 */
		@Override
		public void failed(Throwable e, ReadAttachment attach) {
			synchronized (syncEvent) {
				// debug("%s-3",e);
				// close_event();
				try {
					attach.channel.close();
				} catch (Exception e1) {

				}
				try {
					attach.listener.disconnected(_this);
				} catch (Exception e2) {

				}
				debug("close----2--" + attach.ip + "  isaccept=" + isAccept() + " " + e);
			} // sync
		}

	}

	/**
	 * 
	 * 
	 * 
	 * @author richard
	 *
	 */
	private class WriteHandler implements CompletionHandler<Integer, WriteAttachment> {

		@Override
		public void completed(Integer result, WriteAttachment attach) {
			synchronized (syncEvent) {
				// System.out.println("completed | " + result + " | " + attach);

				if (attach == null)
					return;

				if (result == -1) {

					// close_event();
					try {
						attach.channel.close();
					} catch (Exception e1) {

					}
					try {
						attach.listener.disconnected(_this);
					} catch (Exception e2) {

					}
					debug("close----3--" + attach.ip);
					return;
				}

				if (isAlive() == false)
					return;

				synchronized (attach.writeData) {
					try {

						if (attach.writeData.size() > 0) {
							ByteBuffer buffer = attach.writeData.getFirst();

							// debug("send.remaining(%d)", buffer.remaining() );

							// if (buffer.remaining() > 0) {
							// attach.channel.write(attach.writeData.getFirst(), attach, this);// send remains
							// return;
							// }
							buffer.clear();
							attach.writeData.removeFirst();
						} // if

					} catch (Exception e) {

						e.printStackTrace();
					}

					if (attach.writeData.size() > 0) {
						attach.channel.write(attach.writeData.getFirst(), attach, this);// start remains
						attach.isWriting = true;
					} else {

						attach.isWriting = false;
					}

				} // sync
			} // sync
		}

		/**
		 * 
		 * 
		 * @param e
		 * @param attach
		 */
		@Override
		public void failed(Throwable e, WriteAttachment attach) {
			synchronized (syncEvent) {

				// close_event();
				try {
					attach.channel.close();
				} catch (Exception e1) {

				}
				try {
					attach.listener.disconnected(_this);
				} catch (Exception e2) {

				}
				debug("close----4--" + attach.ip + " " + e);
			} // sync
		}

	}

}
