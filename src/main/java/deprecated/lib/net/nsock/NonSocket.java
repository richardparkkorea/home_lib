package deprecated.lib.net.nsock;
/**
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * http://www.exampledepot.com/egs/java.nio/DetectClosed.html
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import home.lib.lang.Thread2;
import home.lib.util.TimeUtil;
 
@Deprecated
final public class NonSocket { // ,
								// public
								// NqEventManagerClass

	NonSocket _this = this;
	// uThread m_th = new uThread(this);

	protected NonEventManager myEventMgr = new NonEventManager();

	protected Object m_cliLock = new Object(); // lang__criticalsection
												// syncClientLock;

	// protected ArrayList<ClientSocket> m_clients = new ArrayList<ClientSocket>();
	private Map<SocketChannel, NonSocketInterface> m_map = new HashMap<SocketChannel, NonSocketInterface>();

	protected int m_resendInterval;

	// protected uServerSocket m_serverSock = new uServerSocket();
	//private ServerSocket m_serverSock = null;

	protected int m_reserveToClose = 0;

	protected int m_maxBufferSize;

	protected int m_maxClientCount;

	protected double m_sendTimeout;

	protected double m_receiveTimeout = (60.0 * 3);

	// protected ServerSocket_GarbageThread m_gt = new ServerSocket_GarbageThread(this);

	protected NonSocketInterface m_instance;

 
	private int m_port = 0;

	
	ServerSocketChannel m_serverChannel = null;
	
	Selector m_selector=null;

	final ReentrantLock selectorLock = new ReentrantLock();

	
/**
 * 
 * @param port
 * @param p
 * @throws Exception
 */
	public NonSocket(int port, NonSocketInterface p) throws Exception {

		synchronized (m_cliLock) {
			m_instance = p;

			m_maxBufferSize = 1024 * 64;// 256k (max data collect able size )
			m_maxClientCount = 1024;// 1024 clients

			m_resendInterval = (int) (1.5 * 1000); // 1.5 second (it auto repair)

			

			//m_wishToBeBreak = 0;

			m_port = port;

			if(m_port==0)
				m_sendTimeout = 3.0; // default 3 sec
			else
				m_sendTimeout = 0.0; //  ServerMode was not support Timeout
			
			
			new Thread(doServerSelect).start();
			new Thread(doGarbage).start();
		}// syunc
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * non blockng select   
	 * 
	 * 
	 * 
	 */
	Runnable doServerSelect = new Runnable() {
		public void run() {

			int currentPort = m_port;
			//
			//
			while (m_reserveToClose == 0 && m_port != -1) {

				//
				//
				//ServerSocketChannel server = null;

				try {

					ByteBuffer buffer = ByteBuffer.allocate(8 * 1024);

					m_selector = Selector.open();
					//
					//m_refSelector = selector;// refer
					SelectionKey serverkey = null;

					if (m_port > 0) {

						m_serverChannel = ServerSocketChannel.open();
						//
						//m_refServer = server;// refer
						//
						m_serverChannel.socket().bind(new java.net.InetSocketAddress(m_port));
						m_serverChannel.configureBlocking(false);
						serverkey = m_serverChannel.register(m_selector, SelectionKey.OP_ACCEPT);

						//System.out.println("Server start :" + m_port);
					}

					while (m_reserveToClose == 0 && m_port != -1) {
						//

						//System.out.println("non-blocking select"); 
						 selectorLock.lock();
						 selectorLock.unlock();
						
						m_selector.select();
						Set<?> keys = m_selector.selectedKeys();

					  
							for (Iterator<?> i = keys.iterator(); i.hasNext();) {
								SelectionKey key = (SelectionKey) i.next();
								i.remove();

								  //System.out.println("get key   "+ m_port +"   "+key.isValid() );

								if (serverkey != null && key == serverkey) {

									try {

										if (key.isAcceptable()) {
											SocketChannel client = m_serverChannel.accept();
											client.configureBlocking(false);
											client.register(m_selector, SelectionKey.OP_READ);
											// clientkey.attach(new Integer(0));

											// add socket
											{
												NonSocketInterface nsk = m_instance.getNew(_this, client);
												// new ClientSocket(_this, client);

												addClient(nsk);

												// accept event
												{
													NonEvent ev = new NonEvent();
													ev.msg = NonEvent.EventAccept;// "accept";
													ev.sk = client;
													myEventMgr.addEvent(ev);
												}
											}
											// System.out.println("accept==========" + getClientCount());

										}

									} catch (Exception e) {
										_this.cleanUp();// end of server
										return;
									}

								} else {
									SocketChannel client = (SocketChannel) key.channel();

									// System.out.println("on client event==========" + getClientCount() +
									// " "+key.isReadable() + " "+key.isConnectable() + " "+key.isWritable() );

									if (key.isValid() && key.isConnectable()) {
										// Get channel with connection request
										// SocketChannel sChannel = (SocketChannel)key.channel();

										// System.out.println("connected==========");

										boolean success = client.finishConnect();
										if (!success) {
											// An error occurred; handle it

											// Unregister the channel with this selector
											key.cancel();
											continue;
										}
										
										client.register(m_selector, SelectionKey.OP_READ );
										
										

										NonSocketInterface nsk = m_instance.getNew(_this, client);

										addClient(nsk);

										// accept event
										{
											NonEvent ev = new NonEvent();
											ev.msg = NonEvent.EventConnect;// "accept";
											ev.sk = client;
											myEventMgr.addEvent(ev);
										}

									}

									try {

										if (key.isReadable()) {
											// System.out.println("readable==========");

											buffer.clear();
											int bytesread = client.read(buffer);

											if (bytesread == -1) {
												key.cancel();
												removeClient(client);
												continue;
											}
											buffer.flip();

											// do read action
											// ...
											NonSocketInterface cs = getClient(client);
											if (cs != null)
												cs.doReceive(buffer.array(), bytesread );

											// System.out.println("read len: " + buffer.array());
											continue;
										}
									} catch (Exception e) {
										// System.out.println(e);
										key.cancel();
										removeClient(client);

									}

									// buffer.flip();
									// String request = decoder.decode(buffer).toString();
									// buffer.clear();
									// if (request.trim().equals("quit")) {
									// client.write(encoder.encode(CharBuffer.wrap("Bye.")));
									// key.cancel();
									// client.close();
									// } else {

									// System.out.println("event of client==========");

								}
							}// for key
						 

					}// while

				} catch (Exception e1) {
					System.out.println(e1);
				} finally {
					//
					//
					//System.out.println("end of run==========" + currentPort);
					//

					closeAllSockets();
					try {
						if (m_serverChannel != null)
							m_serverChannel.close();
					} catch (IOException e2) {
					}
					// // accept event
					// {
					// NqEvent ev = new NqEvent();
					// ev.msg = NqEvent.EventServerClose;// "close:server";
					// ev.sk = null;
					// myEventMgr.addEvent(ev);
					// }
				}

				Thread2.sleep(1000);
			}// while
		}
	};

	
	/**
	 * 
	 * @param ip
	 *            String
	 * @param port
	 *            int
	 * @return Socket
	 */
//
	public SocketChannel connect(String ip, int port) throws Exception {
		synchronized (m_cliLock) {

			if (getClientCount() > this.m_maxClientCount) {
				// uKernelDbg.prt("connect:err:client count over!");
				return null;
			}
			
			
		    // Create a non-blocking socket channel
		    SocketChannel sChannel = SocketChannel.open();
		    sChannel.configureBlocking(false);

		    // Send a connection request to the server; this method is non-blocking
		    sChannel.connect(new InetSocketAddress(ip, port));

			
			// SelectionKey.OP_READ;
			 
			// Register the channel with selector, listening for all events
			selectorLock.lock();
			try {
				m_selector.wakeup();
				sChannel.register(m_selector, SelectionKey.OP_READ | SelectionKey.OP_CONNECT );//sChannel.validOps());
			} finally {
				selectorLock.unlock();
			}

		
			return sChannel;
			
			
//			Socket sk;
//
//			// try {
//			sk = new Socket(ip, port);
//
//			if (sk == null || sk.isConnected() == false) {
//				sk.close();
//				return null;
//			}
//			// } catch (Exception e) {
//			// e.printStackTrace();
//			// return null;
//			// }
//
//			ClientSocket nsk = new ClientSocket(this, sk);
//
//			addClient(nsk);
//
//			NqEvent ev = new NqEvent();
//			ev.msg = NqEvent.EventConnect;// "connected";
//			ev.sk = sk;
//			// ev.sk_str = sk.getRemoteSocketAddress().toString();
//			// ev.cid = nsk.m_cid;
//			myEventMgr.addEvent(ev);
//
//			// System.out.println("connec ok " + sk.isConnected());
//
//			return nsk.IPacketHandle;
		}
	}
	
	
	public void setMaxClientCount(int n) {
		m_maxClientCount = n; // cnt
	}

	public void setMaxBufferSize(int n) {
		m_maxBufferSize = n; // bytes
	}

	public int getMaxBufferSize(int n) {
		return m_maxBufferSize;
	}

	public void setSendTimeOut(double n) {
		m_sendTimeout = n; // milli second
	}

	public void setReceiveWaitTimeout(double d) {
		m_receiveTimeout = d;
	}

	public double getSendTimeout() {
		return m_sendTimeout;
	}

	public boolean isAlive() {
		// return (m_serverSock != null && m_wishToBeBreak == 0);
		return (m_reserveToClose == 0);
	}

	public int getPort() {
		return m_port;
	}
//	public boolean serverIsAlive() {
//		return (m_wishToBeBreak == 0);
//	}

	/**
     *
     */
	public void cleanUp() {
		synchronized (m_cliLock) {

			m_port=-1;
			
			
			m_reserveToClose = 1;

			
			try {
				if (m_serverChannel != null)
					m_serverChannel.close();
			} catch (Exception e) {
				;
			}
			
//			try {
//				m_serverSock.close();
//			} catch (Exception e) {
//
//			}
			// m_gt.cleanUp();
			// m_th.wait2();

			closeAllSockets();

			myEventMgr.clear();
		}
	}

	/**
	 * 
	 * @return
	 */
	Runnable doGarbage = new Runnable() {
		public void run() {
			while (_this.m_reserveToClose == 0) {

				synchronized (m_cliLock) {
					// System.out.println("garbage->close socket========1=======");

					for (Map.Entry<SocketChannel, NonSocketInterface> e : m_map.entrySet()) {

						NonSocketInterface cs = e.getValue();

						if (cs.isReserveToClose()) {
							// if (//cs.m_sock == null ||
							// cs.m_closeReservation == 1
							// || cs.m_lastReceivedTime.end() > m_receiveTimeout) {

							//System.out.println("garbage->close socket===============");
							m_map.remove(cs.getChannel());

							// Socket sock1 = cs.m_sock;

							cs.cleanUp();
							NonEvent ev = new NonEvent();
							ev.msg = NonEvent.EventClose;// "close";
							// ev.sk = sock1;\
							ev.sk = cs.getChannel();
							myEventMgr.addEvent(ev);
							break;
						}
					} // while
				}// sync

				Thread2.sleep(100);
			}

		}
	};


	
 

	/**
	 * 
	 * @return int
	 */
	public int getClientCount() {
		// synchronized (syncClientLock) { // lock <---
		// debug("garbage...2");
		return m_map.size();
		// }
	}

	/**
	 * 
	 * @param nsk
	 *            ClientSocket
	 * @return int
	 */
	public int addClient(NonSocketInterface nsk) {
		synchronized (m_cliLock) { // lock <---
			// debug("add...2");

			if (m_map.size() > m_maxClientCount) {
				// error: error on before thread runing.. because delete nsk;
				//
				// nsk.cleanUp();
				// delete nsk;
				// return 0;
				nsk.cleanUp() ;
			}

			m_map.put(nsk.getChannel(), nsk);
			return 1;
		}
	}

//	/**
//	 * 
//	 * @param p
//	 *            int
//	 * @return int
//	 */
//	public int removeClient(ClientInterface ipack) {
//		synchronized (m_syncLock) { // lock <---
//			// debug("remove...2");
//
//			// int cnt = m_client2.size();
//			// if (p >= cnt)
//			// return 0;
//
////			ClientInterface sk = m_client2.get(ipack);
////			if (sk == null)
////				return 0;
//			for (Map.Entry<SocketChannel, ClientInterface> e : m_client2.entrySet()) {
//
////				ClientInterface cs = e.getValue();
////				if (cs.getSocket() == sock)
////					return cs;
//
//				if (e.getValue() == ipack) {
//
//					ipack.cleanUp();
//					
//					m_client2.remove(e.getKey());
// 
//				}				
//			} // while
//
//			
//			
//			// delete sk;
//
//			return 1;
//		}
//	}
//

	public boolean removeClient(SocketChannel sc) {
		synchronized (m_cliLock) { // lock <---
 
			NonSocketInterface client = m_map.get(sc);

			if (client != null) {
				client.cleanUp();

				return true;
			} else
				return false;
		}
	}

	
	
 	public NonSocketInterface getClient(SocketChannel sc) {
		synchronized (m_cliLock) {
 			return m_map.get(sc);
		}

	}

	/**
	 * 
	 * @param sndBuf
	 * @param sndLen
	 * @return
	 */

	public int sendDataAll(byte[] sndBuf, int sndLen) {

		if (sndBuf == null || sndLen == 0)
			return 0;

		synchronized (m_cliLock) {

			// int cnt = m_clients.size();

			// for (int i = 0; i < cnt; i++) {
			for (Map.Entry<SocketChannel, NonSocketInterface> e : m_map.entrySet()) {
				  e.getValue().sendData(sndBuf, sndLen);
			}// for
		}
		return 1;
	}

	public int sendDataAll(Object o) {
 

		synchronized (m_cliLock) {

			// int cnt = m_clients.size();

			// for (int i = 0; i < cnt; i++) {
			for (Map.Entry<SocketChannel, NonSocketInterface> e : m_map.entrySet()) {
				 e.getValue().sendData(o);
			}// for
		}
		return 1;
	}

	
	/**
     *
     */
	public void closeAllSockets() {

		synchronized (m_cliLock) {

			for (Map.Entry<SocketChannel, NonSocketInterface> e : m_map.entrySet()) {
				e.getValue().cleanUp();

			}

			m_map.clear();
		}
	}

	/**
	 * 
	 * @return int
	 */
	public int getEventCount() {
		return myEventMgr.getEventCount();
	}

	public long getEventBytesSize() {
		return myEventMgr.getEventBufferLength();
	}

	public NonEvent popAEvent() {
		return myEventMgr.popAEvent();
	}

	/**
	 * 
	 * @return
	 */
	public Socket[] getConnectedList() {

		ArrayList<Socket> out = new ArrayList<Socket>();
		//
		synchronized (m_cliLock) {

			for (Map.Entry<SocketChannel, NonSocketInterface> e : m_map.entrySet()) {

				NonSocketInterface cs = e.getValue();

				out.add(e.getValue().getChannel().socket() );

			} // while

		}
		return out.toArray(new Socket[0]);
	}
	
	
	
	 

	
	
} // class
