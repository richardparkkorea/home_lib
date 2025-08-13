package deprecated.lib.net.mq.and;

import java.util.ArrayList;

import home.lib.lang.Thread2;
import home.lib.util.TimeUtil;

/**
 * 
 * automatic connection recover
 * 
 * @author livingroom-user
 *
 */
class MqASocketKeeper implements Runnable {

	public static boolean runOnce = true;

	
	public static int m_index = 0;

	public static ArrayList<MqASocket> m_list = null;
	
	//public static double CONNECTION_ATTEMPT_INTERVAL=6.0;

	static {

		m_list = new ArrayList<MqASocket>();

		// new Thread(new staticTimeoutCheck()).start();

	}

	/**
	 * 
	 * @param a
	 * @return
	 */
	public static boolean add(MqASocket a) {
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
	public static boolean remove(MqASocket a) {
		synchronized (m_list) {
			return m_list.remove(a);
		}
	}

	/**
	 * 
	 * 
	 */
	public MqASocketKeeper() {

		

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
			System.out.println("MqASocketKeeper - thread.run ");
			runOnce = false;

			// m_timeoutCheck_isAlive = true;

			TimeUtil t = new TimeUtil();
			// TimeUtil t2=new TimeUtil();

			
			do {

				try {
					MqASocket.sleep(30);

					synchronized (m_list) {
						m_index++;
						if (m_index >= m_list.size())
							m_index = 0;

						if (m_list.size() > 0) {

							MqASocket a = m_list.get(m_index);

							// check timeout
							if (a.isAlive()) {
								

								a.sendPing();

								if ((a.m_lastWorkTime + a.m_timeoutVal) < System.currentTimeMillis()) {

									System.out.format("MqASocketKeeper - timeover (%d  )  (%s) \r\n",a.m_timeoutVal ,a.clientSocket  );
									
									
									a.m_lastWorkTime = System.currentTimeMillis();
									try {
										if (a.clientSocket != null) {
											a.clientSocket.close();
										}
									} catch (Exception e) {
										// do nothing
									}

								}

								MqASocket.sleep(100);
							}  
							//  
							else if (a.try2connect.end_ms() > a.reconnectIntervalMs  ) {
								a.try2connect.start();

								System.out.format("MqASocketKeeper - reconnect (%d  )  (%s) \r\n",a.reconnectIntervalMs ,a.clientSocket  );
								
								if (a.isAlive() == false) {
									a.connect(a.m_connectTimeout);
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
