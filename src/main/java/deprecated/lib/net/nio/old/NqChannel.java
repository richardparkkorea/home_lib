package deprecated.lib.net.nio.old;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.TimerTask;

import home.lib.lang.Timer2;
import home.lib.lang.UserException;
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
	private static NqSelector m_connect_selector = null;

	private static void connect_selector_initialize() throws Exception {

		if (m_connect_selector != null)
			return;

		m_connect_selector = new NqSelector(null);

		//
		// running it unlimitly
		new Timer2().schedule(new TimerTask() {
			public void run() {

				try {
					m_connect_selector.close();
					m_connect_selector.bind("0.0.0.0", 0).get();

				} catch (Exception e) {

					e.printStackTrace();
				}

			}

		}, 1, 1000).oneCallWaiting(3);
		;

	}

	public static NqSelector connect_selector() {
		return m_connect_selector;
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	// private MqChannel _this = this;
	private Object syncClose = new Object();

	private Object m_user_obj = null;

	private long m_socketBufferSize = 1024 * 8;

	private boolean m_isAccept = false;

	private NqSelector m_selector = null;

	private SocketChannel m_socket = null;// socket channel from selector

	// protected MqChannelListener m_inter = null;

	private NqChannelListener m_listen = null;

	private Object m_call_lock = new Object();

	private SocketChannel m_connecting_ch = null;

	/**
	 * 
	 */
	public NqChannel() {

		try {
			connect_selector_initialize();
		} catch (Exception e) {
			e.printStackTrace();
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

		debug("connect(%s:%d)", ip, port);

		m_listen = l;
		m_selector = connect_selector();

		m_connecting_ch = connect_selector().connect(ip, port, this);

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
		String m = "nqchannel(d) " + String.format(f, p);
		System.out.println(TimeUtil.now() + " " + m);
		return m;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isAlive() {

		if (m_socket != null) {

			// System.out.println("isopen="+m_socket.isOpen());

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
		synchronized (syncClose) {

			try {
				m_selector.channelClose(m_socket);
			} catch (Exception e) {

			}
			m_socket = null;
			// }
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

		m_selector.send(m_socket, data);
		return true;

	}

	public boolean isConnecting() {

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

		m_isAccept = true;
		m_socket = ch;
		m_selector = sel;
		m_listen = l;

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

		m_listen.disconnected(ask);

	}

	@Override
	public Object _lock() {

		return m_call_lock;

	}

	public NqSelector selector() {
		return m_selector;
	}

}
