package deprecated.lib.net.mq2.dev_old;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.TimerTask;
import java.util.concurrent.Future;

import home.lib.lang.Timer2;
import home.lib.lang.UserException;
import home.lib.util.TimeUtil;

final public class MqServer implements MqClientInterface {

	private Object recvSync = new Object();

	private MqServer _this = this;

	private MqBootstrap m_bootstrap = null;

	private AsynchronousServerSocketChannel m_server = null;

	private MqSelector m_selector = null;

	// ArrayList<MqClient> m_channels = new ArrayList<MqClient>();

	// Map<MqClient, TimeUtil> m_recvTime = new HashMap<MqClient, TimeUtil>();

	private boolean m_alive = false;

	private MqServerInterface m_inter = null;

	// private boolean m_isClosed = false;

	private Timer2 m_tmrListen = null;

	private String m_bindIp = "";

	private int m_port = 0;

	/**
	 * 
	 * @param bs
	 */
	public MqServer(MqBootstrap bs) {
		m_bootstrap = bs;

	}

	public MqServer bind(String bindIp, int port, MqServerInterface in) throws Exception {

		if (isAlive()) {
			throw new UserException("server(port:%d) is alive", port);
		}

		m_bindIp = bindIp;
		m_port = port;
		m_inter = in;

		m_tmrListen = new Timer2().schedule(new TimerTask() {
			public void run() {

				// if (m_isClosed == false) {
				try {
					m_alive = true;

					m_server = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(bindIp, port));
					debug("server[port=%s] started", port);

					// startTimer();// start it's own timer

					// Log.l("start nio server (%s:%d) ", ip,port );
					while (true) {

						Future<AsynchronousSocketChannel> future = m_server.accept();
						AsynchronousSocketChannel channel = future.get();

						String name = channel.getRemoteAddress().toString();
						// Log.l(" accepted remote addr=" + name);

						MqClient asc = new MqClient(m_bootstrap);
						asc.accept(channel, _this);

						// asc.setUserObject(new UserObj());

					} // while

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					m_alive = false;

					try {
						if (m_server != null) {
							m_server.close();
						}
					} catch (Exception e) {

					}
					m_server = null;

				}

				m_bootstrap.clear();
				TimeUtil.sleep(1000);
				// } // isclosed?

			}
		}, 100, 100);

		return this;
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * @param bindIp
	 * @param port
	 * @param in
	 * @return
	 * @throws Exception
	 */
	public MqServer select(String bindIp, int port, MqServerInterface in) throws Exception {

		if (isAlive()) {
			throw new UserException("server(port:%d) is alive", port);
		}

		m_bindIp = bindIp;
		m_port = port;

		m_inter = in;

		m_tmrListen = new Timer2().schedule(new TimerTask() {
			public void run() {

				// if (m_isClosed == false) {
				try {
					m_alive = true;

					m_selector = new MqSelector(m_bootstrap, _this);

					m_selector.bindBlock(bindIp, port);

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					m_alive = false;

					try {
						m_selector.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					m_selector = null;
				}

				m_bootstrap.clear();
				TimeUtil.sleep(1000);
				// } // isclosed?

			}
		}, 100, 100);

		return this;
	}

	/**
	 * 
	 * 
	 * @param d
	 * @return
	 */
	public boolean waitBind(double d) {
		TimeUtil t = new TimeUtil();

		while (t.end_sec() < d && isAlive() == false) {
			TimeUtil.sleep(10);
		}

		return isAlive();
	}

	/**
	 * 
	 * @param ch
	 */
	public void channelClose(SocketChannel ch) {

		if (m_selector != null) {
			m_selector.channelClose(ch);
		} else {

			try {
				ch.close();
			} catch (Exception e) {

			}
		}

	}

	// /**
	// *
	// *
	// *
	// */
	// private void startTimer() {
	//
	// final Timer2 tmr = new Timer2();
	// tmr.schedule(new TimerTask() {
	// public void run() {
	//
	// if (isAlive() == false) {
	// tmr.cancel();
	// return;
	// }
	//
	// synchronized (m_channels) {
	// try {
	//
	// for (MqClient ch : m_channels) {
	//
	// // UserObj uo = (UserObj) ch.getUserObject();/
	// TimeUtil tu = m_recvTime.get(ch);
	//
	// if (tu != null) {
	// if (tu.end_sec() > m_dReceiveTimeoutSec && m_dReceiveTimeoutSec != 0) {
	// tu.start();
	// ch.close();
	// }
	// }
	// } // for
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// } // sync
	//
	// }
	// }, 1000, 1000);
	// }

	/**
	 * 
	 * 
	 */
	public void close() {
		// if (isAlive() == false)
		// return;
		// m_isClosed = true;

		if (m_server != null) {
			try {
				m_server.close();
			} catch (Exception e) {

			}
		} // if

		if (m_tmrListen != null) {
			try {

				m_tmrListen.cancel();

			} catch (Exception e) {

			}
		}

		// synchronized (m_channels) {
		// for (int h = 0; h < m_channels.size(); h++) {
		//
		// try {
		// m_channels.get(h).close();
		// } catch (Exception e) {
		//
		// }
		// } // for
		//
		// m_channels.clear();
		// }
		m_server = null;
		m_tmrListen = null;
	}

	// public boolean isClosed() {
	// return m_isClosed;
	// }
	// /**
	// */
	//
	// public int channelCount() {
	// return m_channels.size();
	// }
	//
	// /**
	// *
	// * @return
	// */
	// public MqClient[] getChannels() {
	// return m_channels.toArray(new MqClient[0]);
	// }

	/**
	 * 
	 * 
	 * 
	 * @return
	 */
	public boolean isAlive() {
		return m_alive;
	}

	@Override
	public void connected(MqClient sk) {
		// synchronized (m_channels) {
		// m_channels.add(sk);
		// // m_recvTime.put(sk, new TimeUtil());
		// } // sync

		m_inter.connected(this, sk);

	}

	@Override
	public void disconnected(MqClient sk) {

		// synchronized (m_channels) {
		// m_channels.remove(sk);
		// // m_recvTime.remove(sk);
		// } // sync
		m_inter.disconnected(this, sk);

	}

	@Override
	public MqItem recv(MqClient ask, MqItem m) {

		synchronized (recvSync) {

			MqItem rs = m_inter.recv(this, ask, m);
			return rs;

		} // sync0
	}

	// /**
	// *
	// *
	// * @param m
	// */
	// public void doRecv(MqItem m) {
	//
	//
	//
	//
	//
	// }

	/**
	 * 
	 * 
	 * @return
	 */
	public String getLocalAddress() {
		if (isAlive() == false)
			return null;

		try {
			return m_server.getLocalAddress().toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	/**
	 * 
	 * 
	 * @param s
	 */
	public static void debug(String s, Object... args) {

		System.out.println("mqserver [" + String.format(s, args));
	}

	@Override
	public void sendSucceeded(MqClient ask, MqItem e) {
		m_inter.sendSucceeded(this, ask, e);

	}

	@Override
	public void sendFailed(MqClient ask, MqItem e) {
		m_inter.sendFailed(this, ask, e);

	}

	@Override
	public String toString() {

		String s = "mqserver@" + this.hashCode();
		s += String.format("/%s:%d", m_bindIp, m_port);

		if (m_selector != null) {
			s += "+";
			s += m_selector.toString();
		}
		return s;

	}

	@Override
	public boolean putPath(MqClient ask, String name) {

		return m_inter.putPath(ask, name);
		// return true;//ok put it!
	}
}
