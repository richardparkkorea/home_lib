package deprecated.lib.net.mq2.dev_old;

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
 
import home.lib.util.TimeUtil;

final public class MqChannelServer implements MqChannelInterface {

	MqChannelServer _this = this;

	private AsynchronousServerSocketChannel m_server = null;



	ArrayList<MqChannel> m_channels = new ArrayList<MqChannel>();
	
	Map<MqChannel, TimeUtil> m_recvTime=new HashMap<MqChannel, TimeUtil>();
	

	boolean m_alive = false;

	MqChannelInterface m_inter = null;

	/**
	 * 
	 */
	private double m_dReceiveTimeoutSec = 60;




	
	private long m_lSocketBufferSize=1024*8;
	
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
	 */
	public MqChannelServer() {
		
	}
	
	
	
	public MqChannelServer bind(String bindIp, int port, MqChannelInterface in) throws Exception {

		m_inter = in;

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

						MqChannel asc = new MqChannel();
						asc.setSocketBufferSize(m_lSocketBufferSize).accept(channel, _this);
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

		
		
		
		return this;
	}
	
	/**
	 * 
	 * 
	 * @param d
	 * @return
	 */
	public boolean waitBind(double d) {
		TimeUtil t=new TimeUtil();
		
		while( t.end_sec()<d && isAlive()==false) {
			TimeUtil.sleep(10);
		}
		
		if( isAlive()==false) {
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
	public MqChannelServer setSocketBufferSize(long s) {
		this.m_lSocketBufferSize = s;
		return this;
	}


	/**
	 * 
	 * @param d
	 * @return
	 */
	public MqChannelServer receiveTimeoutSec(double d) {
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

						for (MqChannel ch : m_channels) {

							//UserObj uo = (UserObj) ch.getUserObject();/
							TimeUtil tu=m_recvTime.get(ch);

							if( tu!=null) {
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
	public MqChannel[] getChannels() {
		return m_channels.toArray(new MqChannel[0]);
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
	public void connected(MqChannel sk) {
		synchronized (m_channels) {
			m_channels.add(sk);
			m_recvTime.put(sk, new TimeUtil());
		} // sync
		m_inter.connected(sk);
	}

	@Override
	public void disconnected(MqChannel sk) {
		synchronized (m_channels) {
			m_channels.remove(sk);
			m_recvTime.remove(sk);
		} // sync
		m_inter.disconnected(sk);
		
		
	}

	@Override
	public void recv(MqChannel sk, byte[] arg1, int arg2) {

		//UserObj uo = (UserObj) sk.getUserObject();
		TimeUtil tu=m_recvTime.get(sk);
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

}
