package home.lib.net.nio;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.TimerTask;

import home.lib.lang.Timer2;
import home.lib.lang.UserException;
import home.lib.log.MyLogger;
import home.lib.net.tms.mqtt.MqttBroker;
import home.lib.util.TimeUtil;

/*
 * 
 * https://gist.github.com/ochinchina/72cc23220dc8a933fc46
 * 
 * 
 */
public class NqChannel implements NqChannelListener {

	/**
	 * 
	 */
	// private static NqSelector m_connect_selector = null;
	//
	// private static void connect_selector_initialize() throws Exception {
	//
	// if (m_connect_selector != null)
	// return;
	//
	// m_connect_selector = new NqSelector(null);
	//
	// //
	// // running it unlimitly
	// new Timer2().schedule(new TimerTask() {
	// public void run() {
	//
	// try {
	// m_connect_selector.close();
	// m_connect_selector.bind("0.0.0.0", 0).get();
	//
	// } catch (Exception e) {
	//
	// e.printStackTrace();
	// }
	//
	// }
	//
	// }, 1, 1000).oneCallWaiting(3);
	// ;
	//
	// }
	//
	// public static NqSelector connect_selector() {
	// return m_connect_selector;
	// }

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	// private MqChannel _this = this;
	// private Object syncClose = new Object();

	private Object m_user_obj = null;

	private long m_socketBufferSize = 1024 * 8;

	private boolean m_isAccept = false;

	private NqSelector m_selector = null;

	private boolean m_isSelectorOwner = false;

	private SocketChannel m_socket = null;// socket channel from selector

	// protected MqChannelListener m_inter = null;

	private NqChannelListener m_listen = null;

	private Object m_call_lock = new Object();

	private SocketChannel m_connecting_ch = null;

	/**
	 * 
	 */

	public NqChannel(NqSelector sel) throws Exception {
		if (sel == null) {
			m_isSelectorOwner = true;
			m_selector = null;
		} else {
			m_isSelectorOwner = false;
			m_selector = sel;
		}
	}

	/**
	 * from selector
	 * 
	 * @return
	 */
	public SocketChannel getSocket() {
		return m_socket;
	}

	/**
	 * from connect and bind
	 * 
	 * @return
	 */
	public SocketChannel getChannel() {
		return m_socket;
	}

	/**
	 * 
	 * @param b
	 * @return
	 * @throws Exception
	 */
	public NqChannel setIpBind(InetSocketAddress b) throws Exception {

		// m_bind = b;

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
	public NqChannel setUserObject(Object o) {
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

	public NqChannel setSocketBufferSize(long l) {
		m_socketBufferSize = l;
		return this;
	}

	public long socketBufferSize() {
		return m_socketBufferSize;
	}

	/**
	 * 
	 * @param ip
	 * @param port
	 * @param l
	 * @return
	 * @throws Exception
	 */
	public NqChannel connect(final String ip, final int port, final NqChannelListener l) throws Exception {

		if (isAlive()) {
			throw new UserException("already connected");
		}

		if (isConnecting()) {
			throw new UserException("still connecting");
		}

		debug("connect(%s:%d)", ip, port);

		if (m_isSelectorOwner) {// 200107

			 
			if (m_selector == null) {

				m_selector = new NqSelector(null);
				m_selector.bind("0.0.0.0", 0);
				m_selector.waitBind(6);
			}
		}

		m_listen = l;

		m_connecting_ch = m_selector.connect(ip, port, this);

		// return r;
		return this;
		// } // sync
	}

	/**
	 * 
	 * 
	 * @param d
	 * @return
	 */
	public NqChannel waitConnect(double d) {
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
		String m = "nqchannel " + String.format(f, p);
		

		if ((MqttBroker.debugLevel & MyLogger.DEBUG) != 0) {

			System.out.println(TimeUtil.now() + " " + m);
		}
		
		return m;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isAlive() {

		if (m_socket != null) {

			// System.out.println("isopen="+m_socket.isOpen() +" isconnected= " + m_socket.isConnected() );

			boolean a = m_socket.isOpen();
			boolean b = m_socket.isConnected();

			a = a;
			b = b;

			return (m_socket.isOpen());

		}
		// }

		return false;
	}

	/**
	 * 
	 * 
	 */
	public void close() {
		// synchronized (syncClose)
		{

			try {
				// System.out.println("nqchannel.close-1");
				m_selector.channelClose(m_socket);
				// System.out.println("nqchannel.close-2");
			} catch (Exception e) {

			}

			try {
				m_socket.close();

			} catch (Exception e) {
				;
			}

			m_socket = null;

			//
			if (m_isSelectorOwner) {
				try {
					if (m_selector != null)
						m_selector.close();
				} catch (Exception e) {

				}
				m_selector = null;

				// debug("nqonnector.close remove it's own selector");

			}

			// }
		} // sync
	}

	/**
	 * 
	 * @param data
	 * @return
	 */
	public boolean send(byte[] data) throws Exception {

		if (isAlive() == false)
			return false;

		m_selector.send(m_socket, data);
		return true;

	}

	public boolean isConnecting() {

		if (m_selector == null)
			return false;

		return m_selector.isConnecting(m_connecting_ch);
	}

	/**
	 * 
	 * @param sel
	 * @param ch
	 * @param l
	 * @return
	 */
	public NqChannel accepted(NqSelector sel, SocketChannel ch, NqChannelListener l) {

		if (m_isSelectorOwner) {
			new UserException("NaChannel(connector type) call accepted! ").printStackTrace();
			try {
				m_selector.close();
			} catch (Exception e) {

			}
		}

		m_isAccept = true;
		m_socket = ch;
		m_selector = sel;
		m_listen = l;

		m_isSelectorOwner = false;

		return this;
	}

	@Override
	public void received(SocketChannel ask, byte[] buf, int len) {

		m_listen.received(ask, buf, len);

	}

	@Override
	public void connected(SocketChannel ask) {

		m_isAccept = false;
		m_socket = ask;

		m_connecting_ch = null;

		m_listen.connected(ask);

	}

	@Override
	public void disconnected(SocketChannel ask) {

		try {
			m_listen.disconnected(ask);
		} catch (Exception e) {
			;
		}

		this.close();// 200311

	}

	@Override
	public Object _lock() {

		return m_call_lock;

	}

	public NqSelector selector() {
		return m_selector;
	}

	@Override
	public String toString() {

		String s = "";

		s += String.format("nqchannel@%s  alive(%s) is accepted(%s) ", hashCode(), this.isAlive(), this.isAccept());
		return s;
	}

}
