package deprecated.stable1.lib.net.mq.old0;

import java.util.ArrayList;

import home.lib.util.TimeUtil;

/**
 * 
 * automatic connection recover
 * 
 * @author livingroom-user
 *
 */
class MqSocketKeeper implements Runnable {

	public static boolean runOnce = true;

	
	public static int m_index = 0;

	public static ArrayList<MqSocket> m_list = null;
	
	//public static double CONNECTION_ATTEMPT_INTERVAL=6.0;

	static {

		m_list = new ArrayList<MqSocket>();

		// new Thread(new staticTimeoutCheck()).start();

	}

	/**
	 * 
	 * @param a
	 * @return
	 */
	public static boolean add(MqSocket a) {
		synchronized (m_list) {

			if (m_list.indexOf(a) != -1)
				return true;

			return m_list.add(a);
		}
	}

	/**
	 * 
	 * @param a
	 * @return
	 */
	public static boolean remove(MqSocket a) {
		synchronized (m_list) {
			return m_list.remove(a);
		}
	}

	/**
	 * 
	 * 
	 */
	public MqSocketKeeper() {

		System.out.println("MqSocketKeeper - start ");

	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	public void run() {

		if (runOnce == false)
			return;

		try {

			runOnce = false;

			// m_timeoutCheck_isAlive = true;

			TimeUtil t = new TimeUtil();
			// TimeUtil t2=new TimeUtil();

			do {

				try {
					MqSocket.sleep(30);

					synchronized (m_list) {
						m_index++;
						if (m_index >= m_list.size())
							m_index = 0;

						if (m_list.size() > 0) {

							MqSocket a = m_list.get(m_index);

							// check timeout

							if (a.isAlive()) {
								
								
								
								
								a.sendPing();//reular check
								
								 
								
								if ( a.m_last_rx_time.end_ms() >    a.m_timeoutVal ) {

							 
									a.m_last_rx_time.start();
									try {
										
										a.debug("MqSocketKeeper - Disconnect by keeper(%s) timeout(%d)  ", a.getId(),a.m_timeoutVal );

										a.getChannel().close();
										
										if(a.m_listerner!=null ) {
											a.m_listerner.disconnected(a); 
										}
										
 

									} catch (Exception e) {
										e.printStackTrace();
									}

								}

								// a.sleep(100);
							} // if

							// try to reconnect
							else if (t.end_ms() > 100 && a.isKeeping()) {
								t.start();

								try {
									if (a.isAlive() == false ) {
										
										if (a.m_tmrConnectionAttemptInterval.end_ms() > a.getKeepingInterval() ) {
											a.m_tmrConnectionAttemptInterval.start();
											a.debug("try reconnection %s  cto(%d) ",a.getId(),a.m_connectionTimeout );
											 
											a.connect(a.m_connectionTimeout,true);
										}

									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

						} // if( m_list.size()>0)

					} // sync

				} catch (Exception e) {
					e.printStackTrace();

				}
			} while (true);

		} finally {
			runOnce = true;
			 
		}
	}
};
