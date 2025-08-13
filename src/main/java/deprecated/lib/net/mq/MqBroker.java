package deprecated.lib.net.mq;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import home.lib.io.FilenameUtils;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

/**
 * 
 * guild site link: http://rox-xmlrpc.sourceforge.net/niotut/#The server
 * 
 * @author livingroom-user
 *
 */
public class MqBroker {

	MqBroker _this = this;

	final Object m_lock = new Object();
	private int m_port;
	private Selector m_selector;
	private ServerSocketChannel m_serverChannel;

	// use it regardless of protocol option
	private ConcurrentMap<SocketChannel, MqPacketPicker> m_channels;
	// private ConcurrentMap<String, SocketChannel> m_channelFromName;
	private ConcurrentMap<SocketChannel, String> m_nameFromChannel;
	//
	private boolean m_alive2 = false;

	private long m_receiveByteLimit = 1024 * 1024 * 8;// 16m

	public void setMaxReceivableSize(long n) {
		m_receiveByteLimit = n;
	}

	// /**
	// * protocol option 0- do not use protocol 1- used define protocol
	// *
	// * @param b
	// * protocol no
	// */
	// private byte m_protocol_option = 1;
	//
	// public void setProtocol(byte b) {
	// m_protocol_option = b;
	// }

	/**
	 * 
	 * 
	 */
	private MqBrokerListener m_actionListener = null;

	public void setActionListener(MqBrokerListener l) {
		m_actionListener = l;
	}

	/**
	 * 
	 * statistics
	 * 
	 */

	public long StatisticRxBytes = 0;

	public long StatisticTxBytes = 0;

	public long StatisticRxPacketCount = 0;

	public long StatisticTxPacketCount = 0;

	public long StatisticConnectCount = 0;

	public long StatisticDisconnectCount = 0;

	/**
	 * 
	 * 
	 * @param port
	 * @param l
	 */
	public MqBroker(int port, MqBrokerListener l) throws Exception {

		synchronized (m_lock) {
			this.m_port = port;
			this.m_actionListener = l;

			m_channels = new ConcurrentHashMap<SocketChannel, MqPacketPicker>();
			// m_channelFromName = new ConcurrentHashMap<String, SocketChannel>();

			m_nameFromChannel = new ConcurrentHashMap<SocketChannel, String>();

			//
			this.m_selector = SelectorProvider.provider().openSelector();// Selector.open();
			m_serverChannel = ServerSocketChannel.open();
			m_serverChannel.configureBlocking(false);

			// bind to port
			InetSocketAddress listenAddr = new InetSocketAddress((InetAddress) null, this.m_port);

			m_serverChannel.socket().bind(listenAddr);
			m_serverChannel.register(this.m_selector, SelectionKey.OP_ACCEPT);

		}
	}

	public void start() {
		if (isAlive() == false) {
			new Thread(m_selectRun).start();

		}
	}

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

	public int getChannelCount() {
		synchronized (m_channels) {
			return m_channels.size();
		}
	}

	private void doSelectAction() throws IOException {

		// TimeUtil tm0 = new TimeUtil();
		ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 1024);
		// m_actionListener.log(this, 0, "server ready .");

		while (true) {

			try {

				// Process any pending changes

				synchronized (this.m_changeRequests) {
					Iterator changes = this.m_changeRequests.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = (ChangeRequest) changes.next();
						switch (change.type) {
						case ChangeRequest.CHANGEOPS:
							try {
								SelectionKey key = change.socket.keyFor(this.m_selector);
								key.interestOps(change.ops);
							} catch (Exception e) {

								debug("%s", e.toString());

								try {

									if (m_channels.containsKey(change.socket)) {

										m_actionListener.removeChannel(_this, change.socket);
									}
								} catch (Exception e2) {
									e.printStackTrace();
								}
								this.removeSocketChannel(change.socket);

							}
							break;
						case ChangeRequest.REMOVE:

							debug(" ChangeRequest.REMOVE");

							try {
								SelectionKey key = change.socket.keyFor(this.m_selector);
								if (key != null)// if( key.isValid())
									key.cancel();
							} catch (Exception e) {
								e.printStackTrace();
							}

							try {
								this.removeSocketChannel(change.socket);
								StatisticDisconnectCount++;
								m_actionListener.removeChannel(_this, change.socket);

							} catch (Exception e) {
								e.printStackTrace();
							}

							break;

						}
					} // while
					this.m_changeRequests.clear();
				}
			} catch (Exception e) {

				try {
					m_actionListener.log(_this, e);
				} catch (Exception exx) {

				}
			}

			//
			//
			///
			try {
				// wait for events
				this.m_selector.select();

			} catch (Exception e) {
				try {
					m_actionListener.log(_this, e);
				} catch (Exception exx) {

				}
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

								this.accept(key);

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
							}

						} catch (Exception e) {
							m_actionListener.log(_this, 0,
									"SelectionKey key = (SelectionKey) keys.next(); " + e.toString());
						}
					} // sync
				} // while

			} catch (Exception e) {
				try {
					m_actionListener.log(_this, e);
				} catch (Exception exx) {

				}

			}

			//
		} // while
	}

	private void accept(SelectionKey key) throws IOException {

		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel channel = serverChannel.accept();
		channel.configureBlocking(false);
		//
		synchronized (m_channels) {
			m_channels.put(channel, new MqPacketPicker(m_receiveByteLimit));
		}

		channel.register(this.m_selector, SelectionKey.OP_READ);

		try {
			m_actionListener.addChannel(_this, channel);
			StatisticConnectCount++;
		} catch (Exception e) {
			e.printStackTrace();
		}

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

		MqPacketPicker ep = null;
		synchronized (m_channels) {
			ep = m_channels.get(channel);
			ep.m_debug = m_debug;
		}

		if (numRead <= 0 || ep == null) {

			debug("%s", "readbytes is 0 or null");
			this.m_changeRequests.add(new ChangeRequest(channel, ChangeRequest.REMOVE, 0));
			// this.m_selector.wakeup();
			try {
				key.cancel();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}

		buffer.flip();

		StatisticRxBytes += numRead;

		ep.m_lastCheck.start();

		byte[] data = new byte[numRead];
		System.arraycopy(buffer.array(), 0, data, 0, numRead);

		ep.append(data, data.length);

		MqBundle rs;

		while ((rs = ep.check()) != null) {
			StatisticRxPacketCount++;

			// // add channel
			// if (m_nameFromChannel.containsKey( channel) == false) {
			//
			// if (m_actionListener.addClientId(rs.from, rs.msg, channel)) {
			//
			//
			// synchronized (m_nameFromChannel) {
			// m_nameFromChannel.put(channel, rs.from);
			// }
			//
			// debug("set new channel handle (%s) ", rs.from);
			//
			// // internalSend(channel, rs);
			//
			// } else {
			// synchronized (m_channels) {
			// m_channels.remove(channel);// *** because it add in
			// // accept-method before got
			// // name
			// }
			// try {
			// rs.result = true;
			// rs.to = rs.from;
			// rs.from = "broker";
			// rs.msg = "err";
			// rs.privateCode = 'r';
			// rs.remoteErrorMessage = "rejected by server";
			// rs.fromBroker = true;
			//
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// }
			//
			// }
			// add channel
			if (m_nameFromChannel.containsKey(channel) == false) {

				if (m_actionListener.addClientId(_this, rs.from, rs.msg, channel)) {

					debug("set new channel handle (%s) ", rs.from);

					synchronized (m_nameFromChannel) {
						m_nameFromChannel.put(channel, rs.from);
					}

					// internalSend(channel, rs);

				} else {

					this.m_changeRequests.add(new ChangeRequest(channel, ChangeRequest.REMOVE, 0));
					// this.m_selector.wakeup();

					return;

				}
			}

			// if the addchannel was not make any return message?
			if (rs.fromBroker == false) {

				try {
					// 160422
					if (rs.to != null && rs.to.trim().length() == 0 && rs.msg != null) {

						// do internal command
						if (rs.msg.equals("pingreq")) {
							rs.result = true;
							rs.to = rs.from;
							rs.from = "broker";
							rs.privateCode = 'b';
							rs.msg = "pingresp";
							rs.fromBroker = true;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

			//
			//
			if (rs.fromBroker) {

				// do broker's return
				internalSend(channel, rs);

			} else if (rs.to != null && rs.to.trim().length() != 0) {

				MqBundle rr = null;
				try {
					rr = m_actionListener.actionPerformed(_this, channel, rs);

					if (rr != null) {

						for (Entry<SocketChannel, String> tar : m_nameFromChannel.entrySet()) {

							String name = tar.getValue();

							if (FilenameUtils.wildcardMatch(name, rs.to)) {// wildcard match

								if (name.equals(rs.from) == false) {// except sender
									if (tar.getValue().equals(channel) == false) {
										internalSend(tar.getKey(), rs);
									}
								}
							}
						} // for

					} // if (rr != null) {

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}

	}

	/**
	 * It is use by READ FUNCTION when some data send to CLINENTs
	 * 
	 * @param tar
	 * @param rs
	 */
	private void internalSend(SocketChannel ch, MqBundle rs) {

		try {
			StatisticTxPacketCount++;
			send(ch, MqPacketPicker.make(rs));

		} catch (Exception e) {

			try {
				ch.close();
			} catch (Exception e2) {
				;
			}
		}

	}

	/**
	 * return the socket name by SocketChannel
	 * 
	 * @param ch
	 * @return
	 */
	public String getNameFromChannel(SocketChannel ch) {

		synchronized (m_nameFromChannel) {
			return m_nameFromChannel.get(ch);
		}
	}

	// /**
	// *
	// * @param s
	// * @return
	// */
	// public SocketChannel getChannelFormName(String s) {
	// synchronized (m_channelFromName) {
	// return m_channelFromName.get(s);
	// }
	// }

	/**
	 * 
	 * @return
	 */
	public String[] getChannelNames() {
		synchronized (m_nameFromChannel) {
			return m_nameFromChannel.values().toArray(new String[0]);
		}
	}

	private void removeSocketChannel(SocketChannel ch) {

		if (ch != null) {

			String name = null;

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

			m_pendingData.remove(ch);

			// if (ch != null) {
			synchronized (m_channels) {

				// System.out.println("remove socket channel : contains" + m_channels.containsKey(ch));
				if (m_channels.containsKey(ch)) {
					m_channels.remove(ch);
				}
			}

			synchronized (m_nameFromChannel) {
				name = m_nameFromChannel.get(ch);
				if (name != null) {
					m_nameFromChannel.remove(ch);
				}
				debug("remove channel name (%s) ", name);
			}

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

		synchronized (this.m_changeRequests) {

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

			this.m_changeRequests.add(new ChangeRequest(ch, ChangeRequest.CHANGEOPS, SelectionKey.OP_READ));

			// Finally, wake up our selecting thread so it can make the required
			// changes
			this.m_selector.wakeup();

		}

	}

	/**
	 * 
	 * return channels
	 * 
	 * @param who
	 *            -wildcardmatch<br>
	 *            null' - return all channels
	 */
	public SocketChannel[] getChannels(String who) {

		synchronized (this.m_nameFromChannel) {

			ArrayList<SocketChannel> arr = new ArrayList<SocketChannel>();

			for (Entry<SocketChannel, String> tar : m_nameFromChannel.entrySet()) {

				String name = tar.getValue();

				if (FilenameUtils.wildcardMatch(name, who)) {// wildcard match

					arr.add(tar.getKey());
				}
			} // for

			return arr.toArray(new SocketChannel[0]);

		} // sync

	}

	/**
	 * 
	 * @param who-
	 *            if this is null, means return all names
	 * @return
	 */
	public String[] getChannelNames(String who) {

		synchronized (this.m_nameFromChannel) {

			ArrayList<String> arr = new ArrayList<String>();

			for (Entry<SocketChannel, String> tar : m_nameFromChannel.entrySet()) {

				String name = tar.getValue();

				if (FilenameUtils.wildcardMatch(name, who) || who == null) {// wildcard match

					arr.add(tar.getValue());
				}
			} // for

			return arr.toArray(new String[0]);

		} // sync

	}

	// /**
	// * use for none-protocol communication
	// *
	// * @param key
	// * @param buffer
	// * @throws IOException
	// */
	// private void readBytes(SelectionKey key, ByteBuffer buffer) throws IOException {
	//
	// SocketChannel channel = (SocketChannel) key.channel();
	//
	// buffer.clear();
	//
	// int numRead = -1;
	// try {
	// numRead = channel.read(buffer);
	// } catch (IOException e) {
	//
	// numRead = 0;
	// // e.printStackTrace();
	// }
	//
	// if (numRead <= 0) {
	//
	// // // synchronized (m_channels) {
	// // // m_channels.remove(channel);
	// // // }
	// //
	// // try {
	// // m_actionListener.removeChannel(channel);
	// // } catch (Exception e) {
	// // e.printStackTrace();
	// // }
	// //
	// // this.removeSocketChannel(channel);
	// //
	// // try {
	// // key.cancel();
	// // channel.close();
	// // } catch (Exception e) {
	// // e.printStackTrace();
	// // }
	//
	// debug("%s", "readbytes(none) is 0 or null");
	// this.m_changeRequests.add(new ChangeRequest(channel, ChangeRequest.REMOVE, 0));
	// this.m_selector.wakeup();
	//
	// return;
	// }
	//
	// buffer.flip();
	//
	// StatisticRxBytes += numRead;
	//
	// byte[] data = new byte[numRead];
	// System.arraycopy(buffer.array(), 0, data, 0, numRead);
	//
	// try {
	// m_actionListener.actionPerformed(_this,channel, data);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// }

	public boolean isAlive() {
		return m_alive2;
	}

	/**
	 * 
	 * 
	 */
	Runnable m_selectRun = new Runnable() {
		public void run() {

			if (isAlive() == false) {
				try {
					m_alive2 = true;

					//
					// timer
					new Thread(new Runnable() {
						TimeUtil t = new TimeUtil();

						public void run() {
							//
							while (isAlive()) {
								if (t.end_ms() > 3000) {
									t.start();
									checkChannelsTimeout();
								}

								MqSocket.sleep(100);
							}
						}
					}).start();
					//
					//

					m_actionListener.startUp(_this);
					doSelectAction();
				} catch (IOException e) {

					if (m_actionListener != null)
						m_actionListener.log(_this, e);

				} finally {
					m_actionListener.finishUp(_this);
					m_alive2 = false;
				}
			}
		}
	};

	/**
	 * 
	 * about send
	 */

	// A list of ChangeRequest instances
	private List m_changeRequests = new LinkedList();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map<SocketChannel, List<ByteBuffer>> m_pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();

	public void send(SocketChannel socket, byte[] data) {

		synchronized (this.m_changeRequests) {
			// Indicate we want the interest ops set changed
			this.m_changeRequests.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

			// And queue the data we want written
			synchronized (this.m_pendingData) {
				List queue = (List) this.m_pendingData.get(socket);
				if (queue == null) {
					queue = new ArrayList();
					this.m_pendingData.put(socket, queue);
				}
				queue.add(ByteBuffer.wrap(data));
			}
		}

		// Finally, wake up our selecting thread so it can make the required
		// changes
		this.m_selector.wakeup();

	}

	private void write(SelectionKey key) throws IOException {

		SocketChannel socketChannel = (SocketChannel) key.channel();

		synchronized (this.m_pendingData) {
			List queue = (List) this.m_pendingData.get(socketChannel);

			if (queue != null) {

				try {
					// Write until there's not more data ...
					while (!queue.isEmpty()) {
						ByteBuffer buf = (ByteBuffer) queue.get(0);
						StatisticTxBytes += socketChannel.write(buf);
						if (buf.remaining() > 0) {
							// ... or the socket's buffer fills up
							break;
						}
						queue.remove(0);
					}
				} catch (Exception e) {
					// e.printStackTrace();
					debug("writing err (%s)(%s)", m_nameFromChannel.get(socketChannel), e.toString());
					queue.clear();
				}

				if (queue.isEmpty()) {
					// We wrote away all data, so we're no longer interested
					// in writing on this socket. Switch back to waiting for
					// data.
					key.interestOps(SelectionKey.OP_READ);

					m_pendingData.remove(socketChannel);

				}
			} else {
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	/**
	 * 
	 * 
	 * 
	 */
	long m_channelTimeout = 60 * 1000 * 3;

	public void setChannelTimeout(long l) {
		m_channelTimeout = l;
	}

	/**
	 *  
	 * 
	 * 
	 */

	TimeUtil m_tmrChannelTimeout = new TimeUtil();

	private void checkChannelsTimeout() {

		try {
			boolean hasEvent = false;

			if (m_tmrChannelTimeout.end_ms() > m_channelTimeout) {
				m_tmrChannelTimeout.start();

				for (Entry<SocketChannel, MqPacketPicker> tar : m_channels.entrySet()) {

					if (tar.getValue().lastRxTime.end_ms() > m_channelTimeout) {
						tar.getValue().lastRxTime.start();
						// SocketChannel sc = tar.getKey();
						//
						//
						SocketChannel sc = tar.getKey();
						debug("Disconnect by timer %d  (%s) ", m_channelTimeout, m_nameFromChannel.get(sc));
						//
						// removeSocketChannel(sc);
						// m_actionListener.removeChannel(sc);
						// //return;

						this.m_changeRequests.add(new ChangeRequest(sc, ChangeRequest.REMOVE, 0));

						try {
							SelectionKey key = sc.keyFor(this.m_selector);
							key.cancel();
						} catch (Exception e) {
							e.printStackTrace();
						}

					}

				} // for

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean m_debug = false;

	/**
	 * 
	 * @param fmt
	 * @param args
	 */
	public void debug(String fmt, Object... args) {
		if (m_debug == false)
			return;

		try {
			System.out.println("Broker-" + String.format(fmt, args));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * 
	 * 
	 */
	@Override
	public String toString() {

		long err = 0;
		synchronized (m_channels) {
			for (Entry<SocketChannel, MqPacketPicker> tar : m_channels.entrySet()) {
				err += tar.getValue().m_err;
			} // for
		} // sync

		return String.format(
				"MqBroker@%x socks=%d port(%d) rx(%s) tx(%s)  con(%s) discon(%s)    rxp(%s) txp(%s) rxErr(%s) ",

				this.hashCode(), //
				this.getChannelCount(), //
				this.m_port, //

				StringUtil.formatBytesSize(this.StatisticRxBytes), //
				StringUtil.formatBytesSize(this.StatisticTxBytes), //

				StringUtil.formatCount(StatisticConnectCount), //
				StringUtil.formatCount(StatisticDisconnectCount), //
				StringUtil.formatCount(this.StatisticRxPacketCount), //
				StringUtil.formatCount(this.StatisticTxPacketCount), //
				StringUtil.formatCount(err)//

		);

	}

}

class ChangeRequest {
	public static final int REGISTER = 1;
	public static final int CHANGEOPS = 2;
	public static final int REMOVE = 3;

	public SocketChannel socket;
	public int type;
	public int ops;

	public ChangeRequest(SocketChannel socket, int type, int ops) {
		this.socket = socket;
		this.type = type;
		this.ops = ops;
	}
}
