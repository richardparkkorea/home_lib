package home.lib.net.nio.sync;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import home.lib.util.DataStream;
import home.lib.util.TimeUtil;

public class SyncSocket {

	/**
	 * 
	 */
	SyncSocket _this = this;
	Socket m_sock = null;
	Object m_user_obj = null;
	boolean m_alive = false;
	/**
	 * 
	 */
	SyncSocketListener m_inter = null;

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
	 */
	public void setUserObject(Object o) {
		m_user_obj = o;
	}

	/**
	 * 
	 * connect
	 * 
	 * @param ip
	 * @param port
	 * @param l
	 * @throws Exception
	 */
	public SyncSocket(String ip, int port, SyncSocketListener l) throws Exception {

		this(ip, port, l, 1024 * 8, 3000, null, null);

	}

	/**
	 * connect
	 * 
	 * @param ip
	 * @param port
	 * @param l
	 * @param rxTxBufLen
	 * @param connectionTimeout
	 * @throws Exception
	 */
	public SyncSocket(String ip, int port, SyncSocketListener l, int rxTxBufLen, long connectionTimeout)
			throws Exception {
		this(ip, port, l, rxTxBufLen, connectionTimeout, null, null);
	}

	/**
	 * connect
	 * 
	 * @param ip
	 * @param port
	 * @param l
	 * @param rxTxBufLen
	 * @param connectionTimeout
	 * @param bind
	 * @param uo
	 * @throws Exception
	 */
	public SyncSocket(String ip, int port, SyncSocketListener l, int rxTxBufLen, long connectionTimeout,
			InetSocketAddress bind, Object uo) throws Exception {

		m_inter = l;
		setUserObject(uo);

		start(ip, port, null, rxTxBufLen);

		waitConnect(connectionTimeout / 1000);

		// m_ch.setSocketBufferSize(rxTxBufLen).connect(ip, port, this).waitConnect(connectionTimeout / 1000);

	}

	/**
	 * 
	 * @param ch
	 * @param l
	 * @param rxTxBufLen
	 * @throws Exception
	 */
	public SyncSocket(Socket ch, SyncSocketListener l, int rxTxBufLen) throws Exception {

		m_inter = l;
		// m_ch.setSocketBufferSize(rxTxBufLen).accepted(sel, ch, this);

		start(null, 0, ch, rxTxBufLen);

		waitConnect(6.0 / 1000);
	}
	 

	/**
	 * 
	 * @param d
	 * @return
	 */
	public SyncSocket waitConnect(double d) {
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
	 * @param sk
	 */
	private void start(String ip, int port, Socket sk, int bufSize) throws Exception {

		new Thread(new Runnable() {
			public void run() {

				boolean wasConnected = false;
				try {

					if (sk != null) {
						m_sock = sk;
					} else {
						m_sock = new Socket(ip, port);
					}

					m_alive = true;

					m_sock.setReceiveBufferSize(bufSize);
					m_sock.setSendBufferSize(bufSize);

					try {
						// debug("connected");
						m_inter.connected(_this);

					} catch (Exception e) {
						;
					}

					wasConnected = true;

					byte[] buf = new byte[bufSize];

					InputStream input = m_sock.getInputStream();

					boolean live = true;
					while (live) {

						int len = input.read(buf);

						if (len > 0) {
							m_inter.recv(_this, buf, len);
						} else {
							m_sock.close();
							live = false;
						}
					} // while

				} catch (Exception e) {
					// e.printStackTrace();
				} finally {

					try {
						m_sock.close();
					} catch (Exception e) {
						;
					}

					try {
						// debug("disconnected sk(%s:%s) ", ip,port);
						if (wasConnected) {
							m_inter.disconnected(_this);
						}
					} catch (Exception e) {
						;
					}

					m_alive = false;
					m_sock = null;
				}

			}
		}).start();
	}

	/**
	 * 
	 * @return
	 */
	public Socket getChannel() {
		return m_sock;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isAlive() {
		return m_alive;
	}

	/**
	 * 
	 */
	public void close() {
		try {
			m_sock.close();

		} catch (Exception e) {

		}
		m_sock = null;
	}

	/**
	 * 
	 * @param data
	 * @return
	 */
	public boolean send(byte[] data) {

//		// System.out.format("sk=%s data=%s \n", m_sock, data );
//
//		// debug("send len=%s ", data.length);
//
//		try {
//			m_sock.getOutputStream().write(data);
//			m_sock.getOutputStream().flush();
//			return true;
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			// e.printStackTrace();
//		}
//
//		return false;
		
		return send( data, data.length);
	}

	public boolean send(byte[] data, int len) {

		// System.out.format("sk=%s data=%s \n", m_sock, data );

		// debug("send len=%s ", data.length);

		try {
			m_sock.getOutputStream().write(data, 0, len);
			m_sock.getOutputStream().flush();
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}

		return false;
	}
	
	
	public boolean send(short s) {
		
		byte[]r=DataStream.short_to(s);
		return send(r, r.length);
		
	}
	public boolean send(int s) {
		
		byte[]r=DataStream.int_to(s);
		return send(r, r.length);
		
	}
	public boolean send(long s) {
		
		byte[]r=DataStream.long_to(s);
		return send(r, r.length);
		
	}
	/**
	 * 
	 * @param f
	 * @param p
	 */
	public static void debug(String f, Object... p) {
		try {
			String s = String.format(f, p);
			System.out.println(TimeUtil.now() + " SyncSocket " + s);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
