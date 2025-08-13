package deprecated.lib.net.mq2.dev_old;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.*;

/**
 * 
 * guild site link: http://rox-xmlrpc.sourceforge.net/niotut/#The server
 * 
 * @author livingroom-user
 *
 */
public class MqSelector {

	MqSelector _this = this;

	final Object m_lock = new Object();
	private int m_port;
	private Selector m_selector;
	private ServerSocketChannel m_serverChannel;

	// use it regardless of protocol option
	private ConcurrentMap<SocketChannel, MqChannel> m_channels = new ConcurrentHashMap<SocketChannel, MqChannel>();

	//
	private boolean m_alive2 = false;

	// A list of MqChangeRequest instances
	private List m_changeRequests = new LinkedList();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map<SocketChannel, List<ByteBuffer>> m_pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();

	/**
	 * 
	 * 
	 */
	// private MqSelectorInterface m_actionListener = null;

	private MqBootstrap m_bootstrap = null;

	private MqServer m_svr = null;

	/**
	 * 
	 * @param bs
	 * @param svr
	 */
	public MqSelector(MqBootstrap bs, MqServer svr) {
		m_bootstrap = bs;
		m_svr = svr;
	}

	public void bindBlock(String ip, int port) throws Exception {

		synchronized (m_lock) {
			this.m_port = port;
			// this.m_actionListener = l;

			//
			this.m_selector = SelectorProvider.provider().openSelector();// Selector.open();
			m_serverChannel = ServerSocketChannel.open();
			m_serverChannel.configureBlocking(false);

			// bind to port
			InetSocketAddress listenAddr = new InetSocketAddress(ip, port);// new InetSocketAddress((InetAddress) null,
																			// this.m_port);

			m_serverChannel.socket().bind(listenAddr);
			m_serverChannel.register(this.m_selector, SelectionKey.OP_ACCEPT);

		}

		try {
			m_alive2 = true;

			//

			// m_actionListener.startUp(_this);
			doSelectAction();
		} catch (IOException e) {
			e.printStackTrace();

			// if (m_actionListener != null)
			// m_actionListener.log(_this, e);

		} finally {
			// m_actionListener.finishUp(_this);
			m_alive2 = false;
		}

	}

	/**
	 * 
	 * 
	 * 
	 */
	public void close() {
		synchronized (m_lock) {
			try {
				m_selector.close();

			} catch (Exception e) {

			}

			try {

				m_serverChannel.close();
			} catch (Exception e) {

			}
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

			System.out.println("mqselector-" + s);
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

		// TimeUtil tm0 = new TimeUtil();
		ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 1024);
		// m_actionListener.log(this, 0, "server ready .");

		while (true) {

			try {

				// get array
				Object[] requests = null;
				synchronized (this.m_changeRequests) {
					requests = this.m_changeRequests.toArray();
					this.m_changeRequests.clear();

				} // sync

				// Iterator changes = this.m_changeRequests.iterator();
				// while (changes.hasNext()) {
				for (Object rObj : requests) {
					MqChangeRequest change = (MqChangeRequest) rObj;// changes.next();
					switch (change.type) {
					case MqChangeRequest.CHANGEOPS:
						try {
							SelectionKey key = change.socket.keyFor(this.m_selector);
							key.interestOps(change.ops);
						} catch (Exception e) {

							e.printStackTrace();
							// debug("MqChangeRequest.CHANGEOPS- %s", e.toString());

							try {

								if (m_channels.containsKey(change.socket)) {

									MqChannel ch = m_channels.get(change.socket);
									if (ch != null)
										ch.m_inter.disconnected(ch);
								}
							} catch (Exception e2) {
								e.printStackTrace();
							}
							this.removeSocketChannel(change.socket);

						}
						break;
					case MqChangeRequest.REMOVE:

						debug(" MqChangeRequest.REMOVE");

						try {
							SelectionKey key = change.socket.keyFor(this.m_selector);
							if (key != null)// if( key.isValid())
								key.cancel();
						} catch (Exception e) {
							e.printStackTrace();
						}

						try {
							if (m_channels.containsKey(change.socket)) {
								MqChannel ch = m_channels.get(change.socket);

								if (ch != null)
									ch.m_inter.disconnected(ch);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						this.removeSocketChannel(change.socket);
						break;
					case MqChangeRequest.REGISTER:
						debug(" MqChangeRequest.REGISTER");
						change.socket.register(this.m_selector, change.ops);
						break;

					}
				} // for

				// this.m_changeRequests.clear();
				// } // sync
			} catch (Exception e) {

				e.printStackTrace();
			}

			//
			//
			///
			try {
				// wait for events
				this.m_selector.select();

			} catch (Exception e) {
				e.printStackTrace();
				return;// selection error
			}

			//
			///
			//
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
									this.accept(key);
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
								//this.finishConnection(key);
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					} // sync
				} // while

			} catch (Exception e) {

				e.printStackTrace();
			}

			//
		} // while
	}

	private void accept(SelectionKey key) throws IOException {

		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel channel = serverChannel.accept();
		channel.configureBlocking(false);
		//

		try {
			// m_actionListener.accepted(_this, channel);

			MqClient asc = new MqClient(m_bootstrap);
			try {
				asc.accept(this, channel, m_svr);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

				synchronized (this.m_changeRequests) {
					this.m_changeRequests.add(new MqChangeRequest(channel, MqChangeRequest.REMOVE, 0));
					return;
				}

			}

			synchronized (m_channels) {

				// if (m_channels.containsKey(ch)) {
				m_channels.put(channel, asc.getChannel());
				// }
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// //synchronized (m_channels) {
		//
		// // not binded? then remove
		// if (m_channels.containsKey(channel) == false) {
		//
		// synchronized(this.m_changeRequests) {
		// this.m_changeRequests.add(new MqChangeRequest(channel, MqChangeRequest.REMOVE, 0));
		// }
		// return;
		// }
		// //} // sync

		channel.register(this.m_selector, SelectionKey.OP_READ);

		// System.out.println(" accept-selector.channel.count= " + m_channels.size());

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

			debug("%s", "readbytes is 0 or null");
			synchronized (this.m_changeRequests) {
				this.m_changeRequests.add(new MqChangeRequest(channel, MqChangeRequest.REMOVE, 0));
			}
			// this.m_selector.wakeup();
			try {
				key.cancel();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}

		buffer.flip();

		byte[] data = new byte[numRead];
		System.arraycopy(buffer.array(), 0, data, 0, numRead);

		try {

			MqChannel ch = m_channels.get(channel);
			ch.m_inter.recv(ch, data, data.length);

		} catch (Exception e) {
			e.printStackTrace();
		}

		// ep.append(data, data.length);

	}

	/**
	 * 
	 * @param ch
	 */
	private void removeSocketChannel(SocketChannel ch) {

		if (ch != null) {

			// String name = null;

			try {
				// if (ch != null) {
				synchronized (m_channels) {

					// System.out.println("remove socket channel : contains" + m_channels.containsKey(ch));
					// if (m_channels.containsKey(ch)) {
					m_channels.remove(ch);
					// }
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

			// System.out.println(" remove-selector.channel.count= " + m_channels.size());

			//
			// synchronized (m_nameFromChannel) {
			// name = m_nameFromChannel.get(ch);
			// if (name != null) {
			// m_nameFromChannel.remove(ch);
			// }
			// debug("remove channel name (%s) ", name);
			// }

		}

		// }

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
		synchronized (this.m_changeRequests) {
			// this.m_changeRequests.add(new MqChangeRequest(ch, MqChangeRequest.CHANGEOPS, SelectionKey.OP_READ));
			this.m_changeRequests.add(new MqChangeRequest(ch, MqChangeRequest.REMOVE, 0));

			// Finally, wake up our selecting thread so it can make the required
			// changes
			this.m_selector.wakeup();

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

		synchronized (this.m_changeRequests) {
			// Indicate we want the interest ops set changed
			this.m_changeRequests.add(new MqChangeRequest(socket, MqChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
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
		 this.m_selector.wakeup();

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

						long ns = System.nanoTime();
						while (buf.remaining() > 0) {
							// ... or the socket's buffer fills up

							if ((System.nanoTime() - ns) > (10 * 1000000)) {// max 10ms
								debug("selector write err ");
								break;
							}
						}
						queue.remove(0);
					}
				} catch (Exception e) {
					// e.printStackTrace();
					// debug("writing err (%s)(%s)", m_nameFromChannel.get(socketChannel), e.toString());
					e.printStackTrace();
					queue.clear();
				}

				if (queue.isEmpty()) {
					// We wrote away all data, so we're no longer interested
					// in writing on this socket. Switch back to waiting for
					// data.
					key.interestOps(SelectionKey.OP_READ);
					// this.m_changeRequests
					// .add(new MqChangeRequest(socketChannel, MqChangeRequest.CHANGEOPS, SelectionKey.OP_READ));

					m_pendingData.remove(socketChannel);

				}
			} else {
				key.interestOps(SelectionKey.OP_READ);
				// this.m_changeRequests
				// .add(new MqChangeRequest(socketChannel, MqChangeRequest.CHANGEOPS, SelectionKey.OP_READ));

			}
		}
	}

	/**
	 * 
	 * 
	 * 
	 */
	@Override
	public String toString() {

		String s = "mqselector@" + this.hashCode();
		s += String.format(" ch(%s) requests(%s) pending data(%s) ", this.m_channels.size(),
				this.m_changeRequests.size(), this.m_pendingData.size());
		return s;
	}
//
//	/**
//	 * 
//	 * 
//	 * @return
//	 * @throws IOException
//	 */
//	public SocketChannel connect(String ip, int port) throws IOException {
//		// Create a non-blocking socket channel
//		SocketChannel socketChannel = SocketChannel.open();
//		socketChannel.configureBlocking(false);
//
//		// Kick off connection establishment
//		socketChannel.connect(new InetSocketAddress(ip, port));
//
//		// Queue a channel registration since the caller is not the
//		// selecting thread. As part of the registration we'll register
//		// an interest in connection events. These are raised when a channel
//		// is ready to complete connection establishment.
//
//		// this.m_changeRequests.add(new MqChangeRequest(socket, MqChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
//		synchronized (this.m_changeRequests) {
//			this.m_changeRequests
//					.add(new MqChangeRequest(socketChannel, MqChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
//		}
//
//		// Finally, wake up our selecting thread so it can make the required changes
//		this.m_selector.wakeup();
//
//		return socketChannel;
//	}
//
//	/**
//	 * 
//	 * @param key
//	 * @throws IOException
//	 */
//	private void finishConnection(SelectionKey key) throws IOException {
//		SocketChannel channel = (SocketChannel) key.channel();
//
//		// Finish the connection. If the connection operation failed
//		// this will raise an IOException.
//		try {
//			channel.finishConnect();
//		} catch (IOException e) {
//			// Cancel the channel's registration with our selector
//			key.cancel();
//			System.out.println("finishConnection err!");
//			return;
//		}
//
//		try {
//			// m_actionListener.accepted(_this, channel);
//
//			MqClient asc = new MqClient(m_bootstrap);
//			try {
//				asc.accept(this, channel, m_svr);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//
//				synchronized (this.m_changeRequests) {
//					this.m_changeRequests.add(new MqChangeRequest(channel, MqChangeRequest.REMOVE, 0));
//					return;
//				}
//
//			}
//
//			synchronized (m_channels) {
//
//				// if (m_channels.containsKey(ch)) {
//				m_channels.put(channel, asc.getChannel());
//				// }
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		// Register an interest in writing on this channel
//		key.interestOps(SelectionKey.OP_WRITE);
//	}

}
