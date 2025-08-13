package deprecated.stable1.lib.net.mq.old0;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Future;

import deprecated.lib.net.mq.ASocketChannel;
import deprecated.lib.net.mq.ASocketChannelInterface;
import home.lib.lang.Timer2;
import home.lib.util.TimeUtil;

final public class ASocketServer implements ASocketChannelInterface {

	ASocketServer _this = this;

	private AsynchronousServerSocketChannel m_server = null;

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

	ArrayList<ASocketChannel> m_channels = new ArrayList<ASocketChannel>();
	
	Map<ASocketChannel, TimeUtil> m_recvTime=new HashMap<ASocketChannel, TimeUtil>();
	

	boolean m_alive = false;

	ASocketChannelInterface m_inter = null;

	/**
	 * 
	 */
	private double m_dReceiveTimeoutSec = 60;

	public void receiveTimeoutSec(double d) {
		m_dReceiveTimeoutSec = d;
	}

	public double receiveTimeoutSec() {
		return m_dReceiveTimeoutSec;
	}

	
	private long m_lSocketBufferSize=1024*8;
	
	public long getSocketBufferSize() {
		return m_lSocketBufferSize;
	}

	public void setSocketBufferSize(long s) {
		this.m_lSocketBufferSize = s;
	}
//	/**
//	 * 
//	 * 
//	 * @author richardpark
//	 *
//	 */
//
//	class UserObj {
//		public TimeUtil lastRx = new TimeUtil();
//	}

	/**
	 * 
	 * 
	 * 
	 * @param ip
	 * @param port
	 */
	public ASocketServer(String ip, int port, ASocketChannelInterface in) throws Exception {

		m_inter = in;

		m_server = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(ip, port));

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

						ASocketChannel asc = new ASocketChannel(channel, _this, (int)m_lSocketBufferSize);
						//asc.setUserObject(new UserObj());
						

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

						for (ASocketChannel ch : m_channels) {

							//UserObj uo = (UserObj) ch.getUserObject();/
							TimeUtil tu=m_recvTime.get(ch);

							if( tu!=null) {
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
	public ASocketChannel[] getChannels() {
		return m_channels.toArray(new ASocketChannel[0]);
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
	public void connected(ASocketChannel sk) {
		synchronized (m_channels) {
			m_channels.add(sk);
			m_recvTime.put(sk, new TimeUtil());
		} // sync
		m_inter.connected(sk);
	}

	@Override
	public void disconnected(ASocketChannel sk) {
		synchronized (m_channels) {
			m_channels.remove(sk);
			m_recvTime.remove(sk);
		} // sync
		m_inter.disconnected(sk);
		
		
	}

	@Override
	public void recv(ASocketChannel sk, byte[] arg1, int arg2) {

		//UserObj uo = (UserObj) sk.getUserObject();
		TimeUtil tu=m_recvTime.get(sk);
		if (tu != null) {
			tu.start();
		}
		//
		//

		m_inter.recv(sk, arg1, arg2);
	}



}
