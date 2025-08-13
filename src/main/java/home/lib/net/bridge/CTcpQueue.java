package home.lib.net.bridge;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import home.lib.util.TimeUtil;

/*
 * 
 * guild site link: http://rox-xmlrpc.sourceforge.net/niotut/#The server
 * 
 * @author livingroom-user
 *
 */
@Deprecated
public class CTcpQueue implements Runnable {

	public final static long BASE_BUFFER_SIZE = 1024 * 128;

	final Object m_lock = new Object();
	private int port;
	private Selector selector;
	private ServerSocketChannel serverChannel;

	// use it regardless of protocol option
	private ConcurrentMap<SocketChannel, CTcpQueuePacketPicker> m_channels;
	private ConcurrentMap<String, SocketChannel> channelFromName;
	private ConcurrentMap<SocketChannel, String> nameFromChannel;
	//
	private boolean m_alive2 = false;
	private CTcpQueueListener m_actionListener = null;

	private int m_initialSocketBuffSize = 1024 * 64;

	public void setSocketBuffSize(int n) {
		m_initialSocketBuffSize = n;
	}

	// // //
	//
	// private long m_packetErrorAccumulation = 0;//
	//
	// /*
	// * the count of protocol errors of received packaet
	// *
	// * @return the cumulative vluae
	// */
	// public long getPacketErrorCount() {
	// return m_packetErrorAccumulation;
	// }
	//
	// private long m_packetReadAccumulation = 0;//
	//
	// /*
	// * the count of received packets
	// *
	// * @return the cumulative value
	// */
	// public long getPacketReadCount() {
	// return m_packetReadAccumulation;
	// }

	// private long m_packetReadSPacketAccumulation = 0;//
	//
	// public long getSPacketCount() {
	// return m_packetReadSPacketAccumulation;
	// }
	//
	// private long m_packetReadRPacketAccumulation = 0;//
	//
	// public long getRPacketCount() {
	// return m_packetReadRPacketAccumulation;
	// }
	//
	// private long m_packetReadNPacketAccumulation = 0;//
	//
	// public long getNPacketCount() {
	// return m_packetReadNPacketAccumulation;
	// }

	/*
	 * protocol option 0- do not use protocol 1- used define protocol
	 * 
	 * @param b
	 *            protocol no
	 */
	public void setProtocol(byte b) {
		m_protocol_option = b;
	}

	private byte m_protocol_option = 1;

	private long m_checkTimeout = 0;

	public void setChannelAliveTimeout(long l) {
		m_checkTimeout = l;
	}

	/*
	 * 
	 * 
	 * @param port
	 * @param l
	 */
	public CTcpQueue(int port) throws Exception {

		synchronized (m_lock) {
			this.port = port;

			m_channels = new ConcurrentHashMap<SocketChannel, CTcpQueuePacketPicker>();
			channelFromName = new ConcurrentHashMap<String, SocketChannel>();

			nameFromChannel = new ConcurrentHashMap<SocketChannel, String>();

			//

			this.selector = SelectorProvider.provider().openSelector();// Selector.open();
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);

			// bind to port
			InetSocketAddress listenAddr = new InetSocketAddress((InetAddress) null, this.port);

			serverChannel.socket().bind(listenAddr);
			serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

		}
	}

	public void setActionListener(CTcpQueueListener l) {
		m_actionListener = l;
	}

	public void start() {
		if (isAlive() == false) {
			new Thread(this).start();
		}
	}

	public void close() {
		synchronized (m_lock) {
			try {
				selector.close();

			} catch (Exception e) {

			}

			try {

				serverChannel.close();
			} catch (Exception e) {

			}
		}
	}

	public int getChannelCount() {
		synchronized (m_channels) {
			return m_channels.size();
		}
	}

	public void start_server() throws IOException {

		TimeUtil tm0 = new TimeUtil();
		ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 128);
		// m_actionListener.log(this, 0, "server ready .");

		while (true) {

			// Process any pending changes

			synchronized (this.changeRequests) {
				Iterator changes = this.changeRequests.iterator();
				while (changes.hasNext()) {
					ChangeRequest change = (ChangeRequest) changes.next();
					switch (change.type) {
					case ChangeRequest.CHANGEOPS:
						try {
							SelectionKey key = change.socket.keyFor(this.selector);
							key.interestOps(change.ops);
						} catch (Exception e) {
							// e.printStackTrace();
							//
							try {

								// boolean exist=false;
								// synchronized(m_channels) {
								// exist=m_channels.containsKey(change.socket);
								// }
								//
								if (m_channels.containsKey(change.socket)) {
									// m_actionListener.log(e);
									// m_actionListener.log(0,e.toString());
									m_actionListener.removeSocket(change.socket);
								}
							} catch (Exception e2) {
								e.printStackTrace();
							}
							this.removeSocketChannel(change.socket);

						}
					}
				}
				this.changeRequests.clear();
			}

			// wait for events
			this.selector.select();

			// wakeup to work on selected keys
			Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();

			while (keys.hasNext()) {
				SelectionKey key = (SelectionKey) keys.next();

				// this is necessary to prevent the same key from coming up
				// again the next time around.
				keys.remove();

				synchronized (m_lock) {
					try {
						if (!key.isValid()) {
							// m_actionListener.log(this, 0, "===>key isValid: " +
							// key.isValid());

							// SocketChannel channel = (SocketChannel) key.channel();
							// read(key, readBuffer);//do remove act
							continue;
						}

						if (key.isAcceptable()) {

							this.accept(key);

							// if (m_protocol_option == 0) {
							// try {
							// m_actionListener.addSocket((SocketChannel)
							// key.channel());
							// } catch (Exception e) {
							// e.printStackTrace();
							// }
							// }

						} else if (key.isReadable()) {

							if (m_protocol_option == 0) {
								read_none(key, readBuffer);
							} else {
								read(key, readBuffer);
							}

						} else if (key.isWritable()) {
							this.write(key);
						}

						// else if (key.isConnectable()) {
						// this.connect(key);
						// }

					} catch (Exception e) {
						m_actionListener.log(0, "SelectionKey key = (SelectionKey) keys.next(); " + e.toString());
					}
				} // sync
			} // while
		} // while
	}

	private void accept(SelectionKey key) throws IOException {

		// System.out.println("accept");
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel channel = serverChannel.accept();
		channel.configureBlocking(false);
		//
		synchronized (m_channels) {
			m_channels.put(channel, new CTcpQueuePacketPicker(m_initialSocketBuffSize));
		}
		channel.setOption(StandardSocketOptions.SO_SNDBUF, m_initialSocketBuffSize);
		channel.setOption(StandardSocketOptions.SO_RCVBUF, m_initialSocketBuffSize);

		channel.register(this.selector, SelectionKey.OP_READ);

		try {
			m_actionListener.addSocket(channel);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// m_connectAccumulation++;

	}

	/*
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

		CTcpQueuePacketPicker ep = null;
		synchronized (m_channels) {
			ep = m_channels.get(channel);
		}

		if (numRead <= 0 || ep == null) {

			synchronized (nameFromChannel) {

				try {
					m_actionListener.removeSocket(channel);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// String name = nameFromChannel.get(channel);
				//
				// synchronized (m_channels) {
				// this.m_channels.remove(channel);
				// }
				//
				// nameFromChannel.remove(channel);
				// if (name != null)
				// this.channelFromName.remove(name);

				this.removeSocketChannel(channel);

				//
				//
				try {
					m_actionListener.log(0, String.format("CTcoQueue Remove =>%d %d %d", nameFromChannel.size(),
							m_channels.size(), nameFromChannel.size()));
				} catch (Exception e) {
					e.printStackTrace();
				}

				try {
					key.cancel();
					channel.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return;
		}

		buffer.flip();

		ep.m_lastCheck.start();

		byte[] data = new byte[numRead];
		System.arraycopy(buffer.array(), 0, data, 0, numRead);

		// System.out.println("queue (" + port + ") recv 111: " + numRead + " " + ep.length());

		ep.append(data, data.length);

		// System.out.println("queue (" + port + ") recv 222: " + numRead + " " + ep.length());

		CBundle rs;

		// System.out.println("recv 1: "+data.length );

		while ((rs = ep.check()) != null) {

			// m_packetReadAccumulation++;
			//
			// if (rs.privateCode == 's')
			// m_packetReadSPacketAccumulation++;
			// if (rs.privateCode == 'r')
			// m_packetReadRPacketAccumulation++;
			// if (rs.privateCode == 0)
			// m_packetReadNPacketAccumulation++;

			// System.out.println("recv 2: "+data.length );

			try {
				// System.out.println("echo~0 ("+ rs.to+" "+rs.from_pwd);
				// 160422
				if (rs.to.trim().length() == 0 && rs.from_pwd != null) {
					// System.out.println("echo~1");
					if (rs.from_pwd.equals("echo~")) {
						rs.result = true;
						rs.to = rs.from;
						rs.privateCode = 'r';
						rs.setLong("generatedId()", System.currentTimeMillis());

						// System.out.println("echo~2");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (channelFromName.containsValue(channel) == false) {
				// if (rs.privateSetId == true) {

				if (m_actionListener.addClientId(rs.from, rs.from_pwd, channel)) {

					channelFromName.put(rs.from, channel);
					synchronized (nameFromChannel) {
						nameFromChannel.put(channel, rs.from);
					}

					internalSend(channel, rs);

				} else {
					synchronized (m_channels) {
						m_channels.remove(channel);// *** because it add in
													// accept-method before got
													// name
					}
				}

			}

			else if (rs.to != null && rs.to.trim().length() != 0) {

				CBundle rr = null;
				try {
					rr = m_actionListener.actionPerformed(rs);
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (rr != null) {

					// send to all sockets
					if (rs.privateIsBroadcast == true) {

						for (Entry<String, SocketChannel> tar : channelFromName.entrySet()) {

							String name = tar.getKey();

							// System.out.println("******* server test : "+
							// name );

							if (name.equals(rs.from) == false) {// except
																// itself
								// rs.to = name;//
								internalSend(tar.getValue(), rs);
							}

							// System.out.println("*******send all");
						}

					} else {// send a client
						SocketChannel tar = channelFromName.get(rs.to);
						//
						if (tar != null) {

							internalSend(tar, rs);

						}
					}

				} // if (rr != null) {
			}

		}

		// collect error counts
		// if (ep.getLastError() != 0)
		// m_packetErrorAccumulation++;

	}

	/*
	 * It is use by READ FUNCTION when some data send to CLINENTs
	 * 
	 * @param tar
	 * @param rs
	 */
	private void internalSend(SocketChannel ch, CBundle rs) {

		try {
			byte[] ds = CTcpQueuePacketPicker.make(rs);
			ByteBuffer buf = ByteBuffer.wrap(ds);
			// SocketChannel ch = (SocketChannel) tar.channel();
			ch.write(buf);
		} catch (Exception e) {

			try {
				// SocketChannel ch = (SocketChannel) tar.channel();
				ch.close();
			} catch (Exception e2) {
				;
			}
		}

	}

	/*
	 * simply send byte array buffer
	 * 
	 * @param tar
	 * @param ds
	 *            //
	 */
	// public void send(SelectionKey tar, byte[] ds) throws Exception {
	//
	// ByteBuffer buf = ByteBuffer.wrap(ds);
	// SocketChannel ch = (SocketChannel) tar.channel();
	// ch.write(buf);
	//
	// }

	/*
	 * return the socket name by SocketChannel
	 * 
	 * @param ch
	 * @return
	 */
	public String getNameFromChannel(SocketChannel ch) {

		synchronized (nameFromChannel) {
			return nameFromChannel.get(ch);
		}
	}

	/*
	 * 
	 * @param s
	 * @return
	 */
	public SocketChannel getChannelFormName(String s) {
		synchronized (channelFromName) {
			return channelFromName.get(s);
		}
	}

	// /*
	// *
	// *
	// *
	// * @param name
	// * @param ch
	// */
	// private void removeSocketChannel(String name) {
	//
	// //
	// //
	// if (name != null) {
	//
	// SocketChannel ch = null;
	//
	// synchronized (channelFromName) {
	// ch = channelFromName.get(name);
	// channelFromName.remove(name);
	// }
	//
	// if (ch != null) {
	// synchronized (m_channels) {
	// m_channels.remove(ch);
	// }
	//
	// synchronized (nameFromChannel) {
	// nameFromChannel.remove(ch);
	// }
	// //
	// try {
	// ch.socket().close();
	// } catch (Exception e) {
	// ;
	// }
	//
	// //
	// try {
	// ch.close();
	// } catch (Exception e) {
	// ;
	// }
	// }
	// //removeSocketChannel(ch);
	//
	// }
	//
	// }

	private void removeSocketChannel(SocketChannel ch) {

		if (ch != null) {

			String name = null;

			// if (ch != null) {
			synchronized (m_channels) {

				// System.out.println("remove socket channel : contains" + m_channels.containsKey(ch));
				if (m_channels.containsKey(ch)) {
					m_channels.remove(ch);
				}
			}

			synchronized (nameFromChannel) {
				name = nameFromChannel.get(ch);
				if (name != null) {
					nameFromChannel.remove(ch);
				}
			}

			synchronized (channelFromName) {
				if (name != null) {
					channelFromName.remove(name);
				}
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
		}

		// }

	}

	/*
	 * 
	 * 
	 * 
	 * @param ch
	 */

	public void channelClose(SocketChannel ch) {

		synchronized (this.changeRequests) {

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

			this.changeRequests.add(new ChangeRequest(ch, ChangeRequest.CHANGEOPS, SelectionKey.OP_READ));

			// Finally, wake up our selecting thread so it can make the required
			// changes
			this.selector.wakeup();

		}

	}

	/*
	 * 
	 * 
	 * 
	 * 
	 * @param name
	 */
	public void channelClose(String name) {

		SocketChannel ch = null;

		synchronized (channelFromName) {
			ch = channelFromName.get(name);
			channelFromName.remove(name);
		}

		if (ch != null) {

			synchronized (nameFromChannel) {
				nameFromChannel.remove(ch);
			}

			// synchronized (m_channels) {
			// m_channels.remove(ch); //<--- remove it in the selector's loop
			// }

			channelClose(ch);
		}

	}

	/*
	 * use for none-protocol communication
	 * 
	 * @param key
	 * @param buffer
	 * @throws IOException
	 */
	private void read_none(SelectionKey key, ByteBuffer buffer) throws IOException {

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

			// synchronized (m_channels) {
			// m_channels.remove(channel);
			// }

			try {
				m_actionListener.removeSocket(channel);
			} catch (Exception e) {
				e.printStackTrace();
			}

			this.removeSocketChannel(channel);

			try {
				key.cancel();
				channel.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}

		buffer.flip();

		byte[] data = new byte[numRead];
		System.arraycopy(buffer.array(), 0, data, 0, numRead);

		try {
			m_actionListener.actionPerformed(channel, data);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public boolean isAlive() {
		return m_alive2;
	}

	@Override
	public void run() {
		try {
			m_alive2 = true;
			m_actionListener.startUp();
			start_server();
		} catch (IOException e) {

			if (m_actionListener != null)
				m_actionListener.log(e);

		} finally {
			m_actionListener.finishUp();
			m_alive2 = false;
		}
	}

	/*
	 * 
	 * about send
	 */

	// A list of ChangeRequest instances
	private List changeRequests = new LinkedList();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();

	public void send(SocketChannel socket, byte[] data) {

		synchronized (this.changeRequests) {
			// Indicate we want the interest ops set changed
			this.changeRequests.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

			// And queue the data we want written
			synchronized (this.pendingData) {
				List queue = (List) this.pendingData.get(socket);
				if (queue == null) {
					queue = new ArrayList();
					this.pendingData.put(socket, queue);
				}
				queue.add(ByteBuffer.wrap(data));
			}
		}

		// Finally, wake up our selecting thread so it can make the required
		// changes
		this.selector.wakeup();

	}

	private void write(SelectionKey key) throws IOException {

		SocketChannel socketChannel = (SocketChannel) key.channel();

		synchronized (this.pendingData) {
			List queue = (List) this.pendingData.get(socketChannel);

			if (queue != null) {

				try {
					// Write until there's not more data ...
					while (!queue.isEmpty()) {
						ByteBuffer buf = (ByteBuffer) queue.get(0);
						socketChannel.write(buf);
						if (buf.remaining() > 0) {
							// ... or the socket's buffer fills up
							break;
						}
						queue.remove(0);
					}
				} catch (Exception e) {
					e.printStackTrace();
					queue.clear();
				}

				if (queue.isEmpty()) {
					// We wrote away all data, so we're no longer interested
					// in writing on this socket. Switch back to waiting for
					// data.
					key.interestOps(SelectionKey.OP_READ);

					// synchronized (this.pendingData) {
					pendingData.remove(socketChannel);
					// }
				}
			} else {
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}// sync

}

class ChangeRequest {
	public static final int REGISTER = 1;
	public static final int CHANGEOPS = 2;

	public SocketChannel socket;
	public int type;
	public int ops;

	public ChangeRequest(SocketChannel socket, int type, int ops) {
		this.socket = socket;
		this.type = type;
		this.ops = ops;
	}
}
