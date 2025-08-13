package deprecated.lib.net.bridge;

import java.util.ArrayList;

import home.lib.util.TimeUtil;

/**
 * 
 * automatic connection recover
 * 
 * @author livingroom-user
 *
 */
@Deprecated
public class AsyncLinkKeeper implements Runnable {

	public static boolean runOnce = true;

	public static int m_index = 0;

	public static ArrayList<AsyncLink> m_list = null;

	static {

		m_list = new ArrayList<AsyncLink>();

		// new Thread(new staticTimeoutCheck()).start();

	}

	/**
	 * 
	 * @param a
	 * @return
	 */
	public static boolean add(AsyncLink a) {
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
	public static boolean remove(AsyncLink a) {
		synchronized (m_list) {
			return m_list.remove(a);
		}
	}

	/**
	 * 
	 * 
	 */
	public AsyncLinkKeeper() {

		System.out.println("CAsyncLinkTimeoutCheck - start ");

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
					AsyncLink.sleep(30);

					synchronized (m_list) {
						m_index++;
						if (m_index >= m_list.size())
							m_index = 0;

						if (m_list.size() > 0) {

							AsyncLink a = m_list.get(m_index);

							// check timeout

							if (a.isAlive2()) {
								if ((a.m_lastWorkTime + a.m_timeoutVal) < System.currentTimeMillis()) {

									long tt = (System.currentTimeMillis() - (a.m_lastWorkTime + a.m_timeoutVal));

									a.m_lastWorkTime = System.currentTimeMillis();
									try {

										a.clientSocket.close();

										if (a.m_actionListener != null) {
											a.m_actionListener.finishUp();
										}
										System.out.println("AsyncLinkKeeper timeout... over(" + tt + ")ms timeout("
												+ a.m_timeoutVal + ")ms id(" + a.m_id + ") hashcode(" + a.hashCode()
												+ ")");

									} catch (Exception e) {
										e.printStackTrace();
									}

								}

								// a.sleep(100);
							} // if

							// try to reconnect
							else if (t.end_ms() > a.m_keepConnectingRetryInterval && a.m_keepConnecting == true) {
								t.start();

								try {
									if (a.isAlive2() == false) {
										System.out.println("AsyncLinkKeeper try to connecting..." + a);
										a.connect();

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
			// m_timeoutCheck_isAlive = false;
			// m_sustainConnection = false;
		}
	}
};
