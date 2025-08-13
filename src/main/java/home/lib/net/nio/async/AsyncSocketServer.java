package home.lib.net.nio.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Future;

import home.lib.lang.Timer2;
import home.lib.lang.UserException;
import home.lib.net.nio.sync.SyncSocket;
import home.lib.util.TimeUtil;

public class AsyncSocketServer implements AsyncSocketListener {

	AsyncSocketServer _this = this;

	private AsynchronousServerSocketChannel m_server = null;

	int m_maxClientCount = 0;

	ArrayList<AsyncSocket> m_channels = new ArrayList<AsyncSocket>();

	Map<AsyncSocket, TimeUtil> m_recvTime = new HashMap<AsyncSocket, TimeUtil>();

	boolean m_alive = false;

	AsyncSocketListener m_inter = null;

	/**
	 * 
	 */
	private double m_dReceiveTimeoutSec = 60;

	private long m_lSocketBufferSize = 1024 * 8;
	
	int m_port=0;

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
	 */
	public AsyncSocketServer(String bindIp, int port, AsyncSocketListener in) throws Exception {

		 
		
		m_inter = in;
		
		m_port=port;

		m_server = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(bindIp, port));

		new Thread(new Runnable() {
			public void run() {

				try {
					m_alive = true;

					startTimer();// start it's own timer

					// Log.l("start nio server (%s:%d) ", ip,port );
					while (true) {

						Future<AsynchronousSocketChannel> future = m_server.accept();
						AsynchronousSocketChannel channel = future.get();

						String name = channel.getRemoteAddress().toString();
						// Log.l(" accepted remote addr=" + name);

						if (m_maxClientCount == 0 || (m_channels.size() < m_maxClientCount)) {

							AsyncSocket asc = new AsyncSocket();
							asc.setSocketBufferSize(m_lSocketBufferSize).accept(channel, _this);

						} else {
							try {
								channel.close();
							} catch (Exception e) {

							}
						}
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

			}
		}).start();

		//return this;
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

		if (isAlive() == false) {
			close();
		}

		return isAlive();
	}

	/**
	 * 
	 * @return
	 */
	public double receiveTimeoutSec() {
		return m_dReceiveTimeoutSec;
	}

	/**
	 * 
	 * @return
	 */
	public long getSocketBufferSize() {
		return m_lSocketBufferSize;
	}

	/**
	 * 
	 * @param s
	 * @return
	 */
	public AsyncSocketServer setSocketBufferSize(long s) {
		this.m_lSocketBufferSize = s;
		return this;
	}

	/**
	 * 
	 * @param d
	 * @return
	 */
	public AsyncSocketServer receiveTimeoutSec(double d) {
		m_dReceiveTimeoutSec = d;
		return this;
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

						for (AsyncSocket ch : m_channels) {

							// UserObj uo = (UserObj) ch.getUserObject();/
							TimeUtil tu = m_recvTime.get(ch);

							if (tu != null) {
								if (tu.end_sec() > m_dReceiveTimeoutSec && m_dReceiveTimeoutSec != 0) {
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

	/**
	 */

	public int channelCount() {
		return m_channels.size();
	}

	/**
	 * 
	 * @return
	 */
	public AsyncSocket[] getChannels() {
		return m_channels.toArray(new AsyncSocket[0]);
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
	public void connected(AsyncSocket sk) {
		synchronized (m_channels) {
			m_channels.add(sk);
			m_recvTime.put(sk, new TimeUtil());
		} // sync
		m_inter.connected(sk);
	}

	@Override
	public void disconnected(AsyncSocket sk) {
		synchronized (m_channels) {
			m_channels.remove(sk);
			m_recvTime.remove(sk);
		} // sync
		m_inter.disconnected(sk);

	}

	@Override
	public void recv(AsyncSocket sk, byte[] arg1, int arg2) {

		// UserObj uo = (UserObj) sk.getUserObject();
		TimeUtil tu = m_recvTime.get(sk);
		if (tu != null) {
			tu.start();
		}
		//
		//

		m_inter.recv(sk, arg1, arg2);
	}

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
	 * 
	 */
	public void setMaxClientCount(int n) {
		m_maxClientCount = n;
	}

//	public void addConnectedSocket(AsyncSocket sk) {
//		synchronized (m_channels) {
//			m_channels.add(sk);
//			m_recvTime.put(sk, new TimeUtil());
//		} // sync
//	}
	
	public void connect(String ip,int port) throws Exception {
		
		if( m_channels.size()>=m_maxClientCount ) 
			throw new UserException("the number of available sockets has been excceed");
		
		
		AsyncSocket sk= new AsyncSocket().setSocketBufferSize( m_lSocketBufferSize).connect(ip, port, _this ).waitConnect(6000);
		
	
		 
	}
	
	

	public void sendAll(byte[] tx) {

		synchronized (m_channels) {

			for (AsyncSocket sk : m_channels) {
				try {
					if(sk.isAlive())
					sk.send(tx);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		} // syn
	}
	
	public int getPort() {
		return m_port;
	}
}
