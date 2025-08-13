package home.lib.lang;

import java.util.TimerTask;

import home.lib.util.TimeUtil;

public class Timer2 {

	Timer2 _this = this;

	boolean m_alive = false;
	boolean m_cancel = false;
	boolean m_have_one_call = false;
	TimeUtil m_cycleTime = new TimeUtil();
	boolean m_isStarted = false;

	/**
	 * 
	 * 
	 * 
	 */
	public Timer2() {

	}

	/**
	 * 
	 * @param timeoutMs
	 */
	synchronized public Timer2 cancel(long timeoutMs) {
		m_cancel = true;

		//
		//
		TimeUtil t = new TimeUtil();
		while (t.end_ms() < timeoutMs && isAlive()) {
			TimeUtil.sleep(10);
		} // while

		return this;
	}

	/**
	 * 
	 */
	public Timer2 cancel() {
		return cancel(0);
	}

	/**
	 * 
	 * 
	 */
	public boolean isAlive() {
		return m_alive;
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 */
	public boolean isStarted() {
		return m_isStarted;
	}

	/**
	 * 
	 * @param task
	 * @param delay
	 * @return
	 */
	 public Timer2 schedule(final Timer2Task task, final int delay) {
		return schedule(task, delay, 0);
	}

	/**
	 * 
	 * @param task
	 * @param delay
	 * @param period
	 */
	synchronized public Timer2 schedule(final Timer2Task task, final int delay, final int period) {
		// if (m_cancel) {
		// System.out.println("Timer2 is already canceled");

		if (isAlive())
			return this;

		m_isStarted = false;
		m_cancel = false;

		
		new Thread(new Runnable() {
			public void run() {

				try {
					m_alive = true;
					m_have_one_call = false;
					m_cycleTime.start();

					//
					// do first task
					TimeUtil t = new TimeUtil();
					while (t.end_ms() < delay) {
						TimeUtil.sleep(10);
						if (m_cancel) {
							return;// end of thread
						}
					}

					try {
						task.start(_this);
					} catch (Exception e) {
						e.printStackTrace();
					}
					m_isStarted = true;

					if (m_cancel) {
						return;// end of thread
					}

					try {
						task.run(_this);// at first
					} catch (Exception e) {
						e.printStackTrace();
					}
					t.start();

					m_have_one_call = true;

					if (period <= 0)
						return;

					//
					// start period task
					while (true) {

						if (m_cancel) {
							return;// end of thread
						}

						if (t.end_ms() > period) {
							t.start();
							try {
								task.run(_this);// at first
							} catch (Exception e) {
								e.printStackTrace();
							}
							m_cycleTime.start();

						}
						TimeUtil.sleep(10);
					} // while

				} finally {

					m_alive = false;
					m_have_one_call = false;

					try {
						task.stop(_this);// at first
					} catch (Exception e) {
						e.printStackTrace();
					}

				}

			}// .run

		}).start();

		return this;
	}// method

	/**
	 * 
	 * @param task
	 * @param delay
	 * @param period
	 */
	@Deprecated 
	public Timer2 schedule(final TimerTask task, final int delay, final int period) {
		// if (m_cancel) {
		// System.out.println("Timer2 is already canceled");

		if (isAlive())
			return this;

		new Thread(new Runnable() {
			public void run() {

				try {
					m_alive = true;
					m_cancel = false;
					m_have_one_call = false;
					m_cycleTime.start();

					//
					// do first task
					TimeUtil t = new TimeUtil();
					while (t.end_ms() < delay) {
						TimeUtil.sleep(10);
						if (m_cancel) {
							return;// end of thread
						}
					}
					try {
						task.run();// at first
					} catch (Exception e) {
						e.printStackTrace();
					}
					t.start();

					m_have_one_call = true;

					//
					// start period task
					while (true) {

						if (m_cancel) {
							return;// end of thread
						}

						if (t.end_ms() > period) {
							t.start();
							try {
								task.run();// at first
							} catch (Exception e) {
								e.printStackTrace();
							}
							m_cycleTime.start();

						}
						TimeUtil.sleep(10);
					} // while

				} finally {

					m_alive = false;
					m_have_one_call = false;
				}

			}// .run

		}).start();

		return this;
	}// method

	/**
	 * 
	 * 
	 * @param task
	 * @param delay
	 */
	public Timer2 schedule(final TimerTask task, final int delay) {

		if (isAlive())
			return this;

		// if (m_cancel)
		// System.out.println("Timer2 is already canceled");
		new Thread(new Runnable() {
			public void run() {

				try {
					m_alive = true;
					m_cancel = false;
					m_have_one_call = false;
					m_cycleTime.start();
					//
					// do first task
					TimeUtil t = new TimeUtil();
					while (t.end_ms() < delay) {
						TimeUtil.sleep(10);
						if (m_cancel) {
							return;// end of thread
						}
					}

					try {
						task.run();// at first
					} catch (Exception e) {
						e.printStackTrace();
					}
					t.start();

				} finally {
					m_alive = false;
					m_have_one_call = false;
				}
			}// .run

		}).start();

		return this;
	}// method

	public void oneCallWaiting(double sec) {

		TimeUtil t = new TimeUtil();

		while (t.end_sec() < sec && m_have_one_call == false) {
			TimeUtil.sleep(10);
		} // while

	}

	/**
	 * 
	 * 
	 */
	@Override
	public String toString() {
		try {
			return "timer2@" + this.hashCode() + " alive=" + this.isAlive() + " ltt=" + m_cycleTime.end_ms() + " ms    is cancel="+ isCancel();

		} catch (Exception e) {
			return "timer2@" + e.getMessage();
		}
	}

	/**
	 * 
	 * @return
	 */
	public boolean isCancel() {
		return m_cancel;
	}

	/**
	 * 
	 * @param timeout
	 */
	public void waitForFinish(long timeout) {
		TimeUtil tm = new TimeUtil();

		while (!tm.elapsed_ms(timeout) && isAlive()) {
			TimeUtil.sleep(10);
		}
	}
	
	
	
	
}
