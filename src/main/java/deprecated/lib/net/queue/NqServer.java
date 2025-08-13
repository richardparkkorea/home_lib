package deprecated.lib.net.queue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import deprecated.lib.net.nsock.NonSocketInterface;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import home.lib.lang.Thread2;
import home.lib.util.TimeUtil;
 

/**
 * 
 * @author root
 *
 */
@Deprecated
class ClientSocket implements Runnable {

	// uThread m_th = new uThread(this);

	// private NqEventManager myEventMgr = null;

	private NqServer m_parent = null;

	// public uSocket m_ss = new uSocket();
	protected Socket m_sock = null;

	protected int m_wishToBeBreak;
	protected int m_closeReservation;

	protected TimeUtil m_lastReceivedTime = new TimeUtil();

	protected INqPacketInterface IPacketHandle;

	/**
	 * 
	 */
	public ClientSocket(NqServer p, Socket sk) {
		m_sock = sk;
		m_parent = p;
		IPacketHandle = p.m_IPacketHandler.providerNewHandle(p, sk); // = new NqPacket(m_parent, m_sock);

		// m_interface = null;
		m_wishToBeBreak = 0;
		m_closeReservation = 0;

		new Thread(this).start();

	}

	/**
	 * 
	 */
	public void run() {

		// NqPacket m_pack = new NqPacket(m_parent);

		// Socket sk = m_ss;
		try {

			byte[] sbuf = new byte[1024 * 8];// byte[] sbuf = new byte[m_parent.m_maxBufferSize + 1];

			// System.out.println("b:"+sbuf.buf.length);
			while (m_wishToBeBreak == 0) {

				// sbuf.buf[0] = 0;
				// int r = WindowAPI.recv(m_ss , sbuf, sbuf.length);
				int r = 0;
				try {
					r = m_sock.getInputStream().read(sbuf, 0, sbuf.length);
				} catch (Exception e1) {

					m_closeReservation = 1;
					// System.out.println("test err1");
					return;// err

				}
				if (r > 0) {

					m_lastReceivedTime.start();

					int wantKeepContinue = 0;
					do {
						wantKeepContinue = 0;
 
						NqEvent ev = IPacketHandle.recvData(sbuf, r);

						 
						if (ev != null) {
							wantKeepContinue = 1;
							m_parent.myEventMgr.addEvent(ev);
						} else {
							// System.out.println("err 2 : "+rr+"  ="+ev.msg);
						}
 

					} while (wantKeepContinue == 1);

				}
				 
			} // while

		} finally {

			try {
				m_sock.close();
			} catch (Exception e) {

			}
			 
			m_closeReservation = 1;
		}
	}

	/**
	 * 
	 */
	public void cleanUp() {
		if (m_sock != null)
			return;

		// System.out.println("cleanup");

		m_closeReservation = 1;
		m_wishToBeBreak = 1;
		try {
			m_sock.close();
			m_sock = null;
		} catch (Exception e) {
			;
		}
		// m_th.wait2();
	}

	 
}
/**
 * 
 * @author root
 *
 */
class ServerSocket_GarbageThread implements Runnable {

	// uThread m_th = new uThread(this);
	private NqServer m_parent = null;

	public ServerSocket_GarbageThread(NqServer pr) {
		// m_outOfRun = 0;
		m_parent = pr;

		new Thread(this).start();
	}

	// public int startUp() {
	// m_outOfRun = 0;
	// return m_th.start();
	// }

	// public void cleanUp() {
	// m_outOfRun = 1;
	// m_th.wait2();
	// }

	// int m_outOfRun = 0;

	public void run() {
		while (m_parent.m_wishToBeBreak == 0) {

			m_parent.garbage();

			Thread2.sleep(100);
		}
	}

}

/**
 * 
 * <p>
 * Title:
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * 
 * <p>
 * Company:
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */

final public class NqServer implements Runnable { // ,
													// public
													// NqEventManagerClass

	// uThread m_th = new uThread(this);

	protected NqEventManager myEventMgr = new NqEventManager();

	protected Object m_syncLock = new Object(); // lang__criticalsection
													// syncClientLock;

	// protected ArrayList<ClientSocket> m_clients = new ArrayList<ClientSocket>();
	private Map<INqPacketInterface, ClientSocket> m_client2 = new HashMap<INqPacketInterface, ClientSocket>();

	protected int m_resendInterval;

	// protected uServerSocket m_serverSock = new uServerSocket();
	private ServerSocket m_serverSock = null;

	protected int m_wishToBeBreak = 0;

	protected int m_maxBufferSize;

	protected int m_maxClientCount;

	protected double m_sendTimeout;

	protected double m_receiveTimeout = (60.0 * 3);

	protected ServerSocket_GarbageThread m_gt = new ServerSocket_GarbageThread(this);

	protected INqPacketInterface m_IPacketHandler;

	public static INqPacketInterface DEFAULT_IPACKET_HANDLE = new NqPacket(null, null);
	
	private int m_port=0;

	// public long m_receivedTimeout = 0;

	// 0-none
	// 1-default
	// public int m_protocal = 1;

	/**
	 * 
	 * @param port
	 * @throws Exception
	 */
	public NqServer(int port) throws Exception {
		this(port, DEFAULT_IPACKET_HANDLE);
	}

	/**
	 * 
	 * @param port
	 * @param p
	 * @throws Exception
	 */
	public NqServer(int port, INqPacketInterface p) throws Exception {

		synchronized (m_syncLock) {
			m_IPacketHandler = p;

			m_maxBufferSize = 1024 * 64;// 256k (max data collect able size )
			m_maxClientCount = 1024;// 1024 clients

			m_resendInterval = (int) (1.5 * 1000); // 1.5 second (it auto repair)

			m_sendTimeout = 3.0; // 15 second

			m_wishToBeBreak = 0;
			
			m_port = port;

			// m_gt.m_parent = this;

			// public int startUp(int port) {

			// if (m_serverSock != null) // only one time use~
			// return 0;

			if (port != 0) { // he desn't like the accept

				// try {
				m_serverSock = new ServerSocket(port);
				// } catch(Exception e) {
				// return 0;
				// }

			} else {

			}

			// m_chanceOfALifetime = new Object();
			// m_th.start(); // thread start
			// m_gt.startUp();
			// return 1;
			// }

			new Thread(this).start();
		}// syunc
	}

	/**
 * 
 * 
 * 
 * 
 */
	public void run() {
		// debug("serversocket v2:run2");
		while (m_wishToBeBreak == 0) {

			// if (m_serverSock.m_port_number != 0) {
			if (m_serverSock != null) {

				// Socket sk = m_serverSock.accept_from_client();
				Socket sk = null;
				try {
					sk = m_serverSock.accept();
				} catch (Exception e) {
					sk = null;
				}
				if (sk == null) {
					m_wishToBeBreak = 1;
					continue;
				}

				if (getClientCount() > this.m_maxClientCount) {
					// WindowAPI.closesocket(sk);
					try {
						sk.close();
					} catch (Exception e) {

					}

					continue;
				}

				// add socket
				{
					// lang__blockLock bl(&m_lock);//lock <---
					ClientSocket nsk = new ClientSocket(this, sk);
					addClient(nsk);

					// accept event
					{
						NqEvent ev = new NqEvent();
						ev.msg = NqEvent.EventAccept;// "accept";
						ev.sk = sk;
						// ev.sk_str = sk.getRemoteSocketAddress().toString();
						// ev.cid = nsk.m_cid;
						myEventMgr.addEvent(ev);
					}
				}

			} // m_ss.alive?
			else {
				Thread2.sleep(100);//when? client mode
			}

			Thread2.sleep(10);
		} // while

		// accept event
		{
			NqEvent ev = new NqEvent();
			ev.msg = NqEvent.EventServerClose;// "close:server";
			ev.sk = null;
			myEventMgr.addEvent(ev);
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
		//return (m_serverSock != null && m_wishToBeBreak == 0);
		return (  m_wishToBeBreak == 0);
	}
	public int getPort() {
		return m_port;
	}

	public boolean serverIsAlive() {
		return (m_serverSock.isBound()==true  && m_wishToBeBreak == 0);
 	}

	/**
     *
     */
	public void cleanUp() {
		synchronized (m_syncLock) {

			m_wishToBeBreak = 1;
			try {
				m_serverSock.close();
			} catch (Exception e) {

			}
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
	public int garbage() {

		synchronized (m_syncLock) {

			for (Map.Entry<INqPacketInterface, ClientSocket> e : m_client2.entrySet()) {

				ClientSocket cs = e.getValue();

				if (cs.m_sock == null || cs.m_closeReservation == 1 || cs.m_lastReceivedTime.end() > m_receiveTimeout) {

					m_client2.remove(cs.IPacketHandle);

					Socket sock1 = cs.m_sock;

					cs.cleanUp();
					NqEvent ev = new NqEvent();
					ev.msg = NqEvent.EventClose;// "close";
					ev.sk = sock1;
					myEventMgr.addEvent(ev);
					return 1; // remove one at a time.
				}
			} // while
			return 1;
		}

	}

	
	
	
	/**
	 * 
	 * @param ip
	 *            String
	 * @param port
	 *            int
	 * @return Socket
	 */

	public INqPacketInterface connect(String ip, int port) throws Exception {
		synchronized (m_syncLock) {

			if (getClientCount() > this.m_maxClientCount) {
				// uKernelDbg.prt("connect:err:client count over!");
				return null;
			}

			Socket sk;

			// try {
			sk = new Socket(ip, port);

			if (sk == null || sk.isConnected() == false) {
				sk.close();
				return null;
			}
			// } catch (Exception e) {
			// e.printStackTrace();
			// return null;
			// }

			ClientSocket nsk = new ClientSocket(this, sk);

			addClient(nsk);

			NqEvent ev = new NqEvent();
			ev.msg = NqEvent.EventConnect;// "connected";
			ev.sk = sk;
			// ev.sk_str = sk.getRemoteSocketAddress().toString();
			// ev.cid = nsk.m_cid;
			myEventMgr.addEvent(ev);

			// System.out.println("connec ok " + sk.isConnected());

			return nsk.IPacketHandle;
		}
	}

	/**
	 * 
	 * @return int
	 */
	public int getClientCount() {
		// synchronized (syncClientLock) { // lock <---
		// debug("garbage...2");
		return m_client2.size();
		// }
	}

	/**
	 * 
	 * @param nsk
	 *            ClientSocket
	 * @return int
	 */
	public int addClient(ClientSocket nsk) {
		synchronized (m_syncLock) { // lock <---
			// debug("add...2");

			if (m_client2.size() > m_maxClientCount) {
				// error: error on before thread runing.. because delete nsk;
				//
				// nsk.cleanUp();
				// delete nsk;
				// return 0;
				nsk.m_closeReservation = 1;
			}

			m_client2.put(nsk.IPacketHandle, nsk);
			return 1;
		}
	}

	/*
	 * 
	 * @param p
	 *            int
	 * @return int
	 */
	public int removeClient(INqPacketInterface ipack) {
		synchronized (m_syncLock) { // lock <---
			// debug("remove...2");

			// int cnt = m_client2.size();
			// if (p >= cnt)
			// return 0;

			ClientSocket sk = m_client2.get(ipack);
			if (sk == null)
				return 0;

			m_client2.remove(ipack);

			sk.cleanUp();
			// delete sk;

			return 1;
		}
	}

	/**
	 * 
	 * @param p
	 *            int
	 * @return Socket
	 */
	public ClientSocket getClient(INqPacketInterface p) {
		synchronized (m_syncLock) { // lock <---
			return m_client2.get(p);
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

		synchronized (m_syncLock) {

			// int cnt = m_clients.size();

			// for (int i = 0; i < cnt; i++) {
			for (Map.Entry<INqPacketInterface, ClientSocket> e : m_client2.entrySet()) {
				byte[] rs = e.getKey().sendData(sndBuf, sndLen);
			}// for
		}
		return 1;
	}


	/**
	 * 
	 * 
	 * 
	 * @param o
	 * @return
	 */
	public int sendDataAll(Object o) {
 

		synchronized (m_syncLock) {

			// int cnt = m_clients.size();

			// for (int i = 0; i < cnt; i++) {
			for (Map.Entry<INqPacketInterface, ClientSocket> e : m_client2.entrySet()) {
				  e.getKey().sendData(o);
			}// for
		}
		return 1;
	}

	
	
	/**
     *
     */
	public void closeAllSockets() {

		synchronized (m_syncLock) {

			for (Map.Entry<INqPacketInterface, ClientSocket> e : m_client2.entrySet()) {
				e.getValue().cleanUp();

			}

			m_client2.clear();
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

	public NqEvent popAEvent() {
		return myEventMgr.popAEvent();
	}

	
	/**
	 * 
	 * @return
	 */
	public Socket[] getConnectedList() {

		ArrayList<Socket> out=new ArrayList<Socket>();
		//
		synchronized (m_syncLock) {

			for (Map.Entry<INqPacketInterface, ClientSocket> e : m_client2.entrySet()) {

				ClientSocket cs = e.getValue();

				out.add(  e.getValue().m_sock );
				 
			} // while
 
		}
		return out.toArray(new Socket[0]);
	}	
} // class
