package home.lib.net.nio.sync;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import home.lib.lang.Timer2;
import home.lib.lang.UserException;
import home.lib.util.TimeUtil;

final public class SyncSocketServer implements SyncSocketListener {

	SyncSocketServer _this = this;

	int m_maxClientCount = 0;
	
	int m_port=0;

	ServerSocket m_server = null;
	// private AsynchronousServerSocketChannel m_server = null;

	public String getLocalAddress() {
		if (isAlive() == false)
			return null;

		try {
			return m_server.getLocalSocketAddress().toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	ArrayList<SyncSocket> m_channels = new ArrayList<SyncSocket>();

	Map<SyncSocket, TimeUtil> m_recvTime = new HashMap<SyncSocket, TimeUtil>();

	boolean m_alive = false;

	SyncSocketListener m_inter = null;

	/**
	 * 
	 */
	private double m_dReceiveTimeoutSec = 600;

	public void receiveTimeoutSec(double d) {
		m_dReceiveTimeoutSec = d;
	}

	public double receiveTimeoutSec() {
		return m_dReceiveTimeoutSec;
	}

	private long m_lSocketBufferSize = 1024 * 8;

	public long getSocketBufferSize() {
		return m_lSocketBufferSize;
	}

	public void setSocketBufferSize(long s) {
		this.m_lSocketBufferSize = s;
	}
	// /**
	// *
	// *
	// * @author richardpark
	// *
	// */
	//
	// class UserObj {
	// public TimeUtil lastRx = new TimeUtil();
	// }

	/**
	 * 
	 * 
	 * 
	 * @param ip
	 * @param port
	 */
	public SyncSocketServer(String ip, int port, SyncSocketListener in) throws Exception {

		m_inter = in;
		
		m_port=port;

		// m_server=new MqSelector(this);
		// m_server.bind(ip, port);

		// m_server = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(ip, port));
		//
		new Thread(new Runnable() {
			public void run() {

				try {
					m_alive = true;

					startTimer();// start it's own timer

					m_server = new ServerSocket(port);

					// Log.l("start nio server (%s:%d) ", ip,port );
					while (true) {

						Socket sk = m_server.accept();

						if (m_maxClientCount == 0 || (m_channels.size() < m_maxClientCount)) {

							SyncSocket asc = new SyncSocket(sk, _this, (int) m_lSocketBufferSize);

						} else {
							try {
								sk.close();
							} catch (Exception e) {

							}
						}

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

			}
		}).start();

	}

	/**
	 * 
	 * 
	 * 
	 */
	private void startTimer() {

		final Timer2 tmr = new Timer2();
		tmr.schedule(new TimerTask() {
			public void run() {

				if (isAlive() == false) {
					tmr.cancel();
					return;
				}

				synchronized (m_channels) {
					try {

						for (SyncSocket ch : m_channels) {

							// UserObj uo = (UserObj) ch.getUserObject();/
							TimeUtil tu = m_recvTime.get(ch);

							if (tu != null) {
								if (tu.end() > m_dReceiveTimeoutSec && m_dReceiveTimeoutSec != 0) {
									tu.start();
									ch.close();
								}
							}
						} // for

					} catch (Exception e) {
						e.printStackTrace();
					}
				} // sync

			}
		}, 1000, 1000);
	}

	/**
	 * 
	 * 
	 */
	public void close() {
		// if (isAlive() == false)
		// return;

		if (m_server != null) {
			try {
				m_server.close();
			} catch (Exception e) {

			}
		} // if

		synchronized (m_channels) {
			for (int h = 0; h < m_channels.size(); h++) {

				try {
					m_channels.get(h).close();
				} catch (Exception e) {

				}
			} // for

			m_channels.clear();
		}
		m_server = null;
	}

	// /**
	// *
	// * @param b
	// */
	// public void sendAll(byte[] b) {
	//
	// for (ASocketchannel ch : m_channels) {
	// try {
	//
	// ch.send(b);
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
	// }

	/**
	 */

	public int channelCount() {
		return m_channels.size();
	}

	/**
	 * 
	 * @return
	 */
	public SyncSocket[] getChannels() {
		return m_channels.toArray(new SyncSocket[0]);
	}

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
	public void connected(SyncSocket sk) {
		synchronized (m_channels) {
			m_channels.add(sk);
			m_recvTime.put(sk, new TimeUtil());
		} // sync
		m_inter.connected(sk);
	}

	@Override
	public void disconnected(SyncSocket sk) {
		synchronized (m_channels) {
			m_channels.remove(sk);
			m_recvTime.remove(sk);
		} // sync
		m_inter.disconnected(sk);

	}

	@Override
	public void recv(SyncSocket sk, byte[] arg1, int arg2) {

		// UserObj uo = (UserObj) sk.getUserObject();
		TimeUtil tu = m_recvTime.get(sk);
		if (tu != null) {
			tu.start();
		}
		//
		//

		m_inter.recv(sk, arg1, arg2);
	}
	//
	// @Override
	// public MqChannelListener accepted(MqSelector svr, SocketChannel ask) throws Exception {
	//
	// ASocketChannel asc;
	// //try {
	// asc = new ASocketChannel(ask, _this, (int)m_lSocketBufferSize);
	// return asc;
	//// } catch (Exception e) {
	//// // TODO Auto-generated catch block
	//// e.printStackTrace();
	//// }
	////
	////
	//// return null;
	// }

	public void setMaxClientCount(int n) {
		m_maxClientCount = n;
	}

	public void connect(String ip,int port) throws Exception {
		
		if( m_channels.size()>=m_maxClientCount ) 
			throw new UserException("the number of available sockets has been excceed");
		
		
		SyncSocket sk= new SyncSocket(ip, port, _this,(int)m_lSocketBufferSize,(long)6000 ); 
	
		
		 
	}

	public void sendAll(byte[] tx) {

		synchronized (m_channels) {

			for (SyncSocket sk : m_channels) {
				//try {
					
					if( sk.isAlive())
						sk.send(tx);
					
				//} catch (Exception e) {
				//	e.printStackTrace();
				//}
			}

		} // syn
	}

	public int getPort() {
		return m_port;
	}
}
