package home.lib.net.tms.mqtt3ssl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import home.lib.net.tms.mqtt3ssl.MqttServerSocket.UserData;

final public class MqttSocket {

	/**
	 * 
	 */
	MqttSocket _this = this;

	Socket m_sock = null;

	boolean m_alive = false;

	private Object m_user_obj = null;

	// @Override
	// public boolean equals(Object o) {
	//
	// return (this == o);
	//
	// }

	/**
	 * 
	 */

	/**
	 * 
	 * connect
	 * 
	 * @param ip
	 * @param port
	 * @param l
	 * @throws Exception
	 */

	public static Socket getSocket(String ip, int port, String cacert) throws Exception {

		if (cacert == null) {

			return new Socket(ip, port);

		} else {

			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			trustStore.setCertificateEntry("Custom CA", (X509Certificate) CertificateFactory.getInstance("X509")
					.generateCertificate(new ByteArrayInputStream(cacert.getBytes(StandardCharsets.UTF_8))));

			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trustStore);
			TrustManager[] trustManagers = tmf.getTrustManagers();

			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustManagers, null);

			// SSLContext sslContext = getSsl();

			// SSLSocketFactory socketFactory = sslContext.getSocketFactory();
			// SSLSocket kkSocket = (SSLSocket) socketFactory.createSocket(ip,port);

			// Create SSLSocketFactory
			SSLSocketFactory socketFactory = sslContext.getSocketFactory();

			// Create SSLSocket
			Socket socket = (Socket) socketFactory.createSocket(ip, port);

			return socket;
		}
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * @param ip
	 * @param port
	 * @param l
	 * @throws Exception
	 */

	public MqttSocket(String ip, int port, MqttSocketListener l, String caCrt) throws Exception {

		MqttSocketListener m_inter = l;

		m_sock = getSocket(ip, port, caCrt);

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

							m_inter.received(_this, buf, len);

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

		// waitConnect(6);

	}
	
	
	 
	

	/**
	 * 
	 * @param channel
	 * @param l
	 */
	protected MqttSocket(final Socket channel, final MqttServerSocket selector) {// MqttSelectorListener
																					// l) {

		// MqttSelectorListener m_svr_inter;
		// m_svr_inter = selector.m_svr;

		new Thread(new Runnable() {
			public void run() {

				boolean wasConnected = false;
				try {

					m_sock = channel;
					m_alive = true;

					UserData ud = selector.onAccept(_this);
					// m_svr_inter.selectorAccepteded(_this);***private void onAccept(MtsSocket channel) throws
					// IOException {

					wasConnected = true;

					byte[] buf = new byte[8 * 1024];

					InputStream input = m_sock.getInputStream();

					boolean live = true;
					while (live) {

						int len = input.read(buf);

						if (len > 0) {

							try {
								ud.tmrLastRecv.start();
							} catch (Exception e) {
								e.printStackTrace();
							}

							synchronized (selector.m_svr) {

								selector.m_svr.selectorReceived(_this, Arrays.copyOf(buf, len));

							}

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

							selector.channelClose(_this);

							// m_svr_inter.selectorDisconnected(_this);***private void removeChannel(SelectionKey key,
							// MtsSocket ch) {

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
	 * @param channel
	 * @param l
	 */
	protected MqttSocket(String ip, int port, final MqttServerSocket selector, String caCrt) throws Exception {// MqttSelectorListener
																					// l) {

		// MqttSelectorListener m_svr_inter;
		// m_svr_inter = selector.m_svr;
		m_sock = getSocket(ip, port, caCrt);

		new Thread(new Runnable() {
			public void run() {

				boolean wasConnected = false;
				try {

					
					m_alive = true;

					UserData ud = selector.onAccept(_this);
					// m_svr_inter.selectorAccepteded(_this);***private void onAccept(MtsSocket channel) throws
					// IOException {

					wasConnected = true;

					byte[] buf = new byte[8 * 1024];

					InputStream input = m_sock.getInputStream();

					boolean live = true;
					while (live) {

						int len = input.read(buf);

						if (len > 0) {

							try {
								ud.tmrLastRecv.start();
							} catch (Exception e) {
								e.printStackTrace();
							}

							synchronized (selector.m_svr) {

								selector.m_svr.selectorReceived(_this, Arrays.copyOf(buf, len));

							}

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

							selector.channelClose(_this);

							// m_svr_inter.selectorDisconnected(_this);***private void removeChannel(SelectionKey key,
							// MtsSocket ch) {

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

	public boolean send(byte[] data) throws Exception {

		return send(data, data.length);
	}

	public boolean send(byte[] data, int len) throws Exception {

		if (m_sock == null)
			return false;

		if (m_alive == false)
			return false;

		// try {
		m_sock.getOutputStream().write(data, 0, len);
		m_sock.getOutputStream().flush();
		return true;
		// } catch (Exception e) {
		// // // TODO Auto-generated catch block
		// // // e.printStackTrace();
		// }
		// return false;
	}

	public Object getUserObject() {
		return m_user_obj;
	}

	/**
	 * 
	 * @param o
	 * @return
	 */
	public MqttSocket setUserObject(Object o) {
		m_user_obj = o;
		return this;
	}

}
