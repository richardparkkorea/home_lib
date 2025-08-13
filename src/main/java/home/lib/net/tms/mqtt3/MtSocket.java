package home.lib.net.tms.mqtt3;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

class MtSocket {

	/**
	 * 
	 */
	MtSocket _this = this;
	
	Socket m_sock = null;
	
	boolean m_alive = false;
	/**
	 * 
	 */
	MqttSocketListener m_inter = null;

	/**
	 * 
	 * connect
	 * 
	 * @param ip
	 * @param port
	 * @param l
	 * @throws Exception
	 */
	public MtSocket(String ip, int port, MqttSocketListener l) throws Exception {

		m_inter = l;

		m_sock = new Socket(ip, port);

		

		new Thread(new Runnable() {
			public void run() {

				boolean wasConnected = false;
				try {

					m_alive = true;
					m_inter.connected(_this);

					wasConnected = true;

					byte[] buf = new byte[8 * 1024];

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
		// }

		//waitConnect(6);

	}

	
	public Socket getChannel() {
		return m_sock;
	}

	
	public boolean isAlive() {
		return m_alive;
	}


	public void close() {
		try {
			m_sock.close();

		} catch (Exception e) {

		}
		m_sock = null;
	}


	public boolean send(byte[] data) {

		return send(data, data.length);
	}

	public boolean send(byte[] data, int len) {


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

}
