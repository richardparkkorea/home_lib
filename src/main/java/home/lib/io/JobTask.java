package home.lib.io;

import java.util.LinkedList;

import home.lib.util.TimeUtil;

/*
 * 
 * class UserJobItem {<br>
 * public int func = 0;<br>
 * }<br>
 * ------------------------------------------------------<br>
 * 
 * JobTask<UserJobItem> m_jt = new JobTask<UserJobItem>(); ------------------------------------------------------<br>
 * 
 * initialize<br>
 * m_jt.resendMs(1 * 1000);<br>
 * m_jt.TimeoutMs(6 * 1000);<br>
 * ------------------------------------------------------<br>
 * 
 * add task<br>
 * m_jt.add(new UserJobItem());<br>
 * ------------------------------------------------------<br>
 * 
 * in ontimer<br>
 * 
 * UserJobItem mi = m_jt.autoPeek(scope);<br>
 * ------------------------------------------------------<br>
 * 
 * set response<br>
 * m_jt.setResp(mi, true);<br>
 * ------------------------------------------------------<br>
 * 
 * report errors<br>
 * if (m_jt.errCount() > 0) {<br>
 * Log.l("%s ���� ����.", sendDesc(m_jt.pollFirstErr()));<br>
 * m_sendErrorCount++;<br>
 * }<br>
 * 
 * ------------------------------------------------------<br>
 * current state report<br>
 * s += String.format( "tx queue(%d)<br>connection timeout( %s )ms <br> send err(%d) ", m_jt.size(), <br>
 * df2.format(dSOCKET_RX_TIME_OUT),m_sendErrorCount);<br>
 * ------------------------------------------------------<br>
 */
public class JobTask<E> {

	JobTaskInterface m_it = null;

	public JobTask(JobTaskInterface it) {
		m_it = it;

	}
	
	public long m_succeed=0;
	public long m_fail=0;

	public long lMaxTasks = 1024 * 3;
	public long lResendMs = 1000;
	public long lTxTimeoutMs = (6000);

	
	
	public long getSucceedCount() {
		return m_succeed;
	}
	public long getFailCount() {
		return m_fail;
	}
	
	
	
	public long getResendMs() {
		return lResendMs;
	}

	public long getTimeoutMs() {
		return lTxTimeoutMs;
	}

	/**
	 * 
	 * @param l
	 */
	public void resendMs(long l) {
		lResendMs = l;
	}

	/**
	 * 
	 * @param l
	 */
	public void TimeoutMs(long l) {
		lTxTimeoutMs = l;
	}

	/**
	 * 
	 * @author richardpark
	 *
	 */
	class JobItem {
		E obj;
		TimeUtil until = new TimeUtil();
		TimeUtil retry = new TimeUtil();
		boolean firstTx = true;
		boolean response = false;

	}

	/**
	 * 
	 */
	LinkedList<JobItem> m_qu = new LinkedList<JobItem>();

	// LinkedList<JobItem> m_err = new LinkedList<JobItem>();

	/**
	 * 
	 * @return
	 */
	public int size() {
		return m_qu.size();
	}

	/**
	 * 
	 * @param n
	 * @return
	 */
	public E get(int n) {
		synchronized (m_qu) {
			return m_qu.get(n).obj;
		}
	}

	/**
	 * 
	 */
	public void clear() {
		synchronized (m_qu) {
			m_qu.clear();
		}
	}

	public void setMaxTask(long l) {
		lMaxTasks = l;
	}

	public long getMaxTask() {
		return lMaxTasks;
	}

	/**
	 * return <br>
	 * true - add false - queue is full!
	 * 
	 * @param obj
	 */
	public boolean add(E obj) {

		JobItem j = new JobItem();
		j.obj = obj;

		// System.out.println("add modbus tx " + m_stQqueue.size());
		synchronized (m_qu) {
			if (m_qu.size() < lMaxTasks) {// limit
				m_qu.add(j);
				return true;
			}

			return false;
		}
	}

	/**
	 * 
	 * @param obj
	 * @param r
	 * @return
	 */
	public int setResp(E obj, boolean r) {
		synchronized (m_qu) {

			for (int i = 0; i < m_qu.size(); i++) {

				JobItem j = m_qu.get(i);
				if (j.obj.equals(obj)) {
					j.response = true;
					return i;
				}
			}
			return -1;
		}
	}

	
	
	/**
	 * 
	 * @param obj
	 * @return
	 */
	public E find(E obj) {
		synchronized (m_qu) {

			for (int i = 0; i < m_qu.size(); i++) {

				JobItem j = m_qu.get(i);
				if (j.obj.equals(obj)) {
					return (E)j.obj;
				}
			}
			return null;
		}
	}
	
	/**
	 * 
	 * 
	 * @param scope
	 * @return
	 */
	public E autoPeek(int scope) {
		synchronized (m_qu) {
			if (m_qu.size() == 0)
				return null;

			int n = Math.min(scope, m_qu.size());

			// remove items (all scope, back -> front )
			//
			for (int i = n - 1; i >= 0; i--) {

				JobItem j = m_qu.get(i);

				if (j.response) {

					JobItem ji = m_qu.remove(i);
					try {
						m_succeed++;
						m_it.doSucceeded(this, ji.obj);
					} catch (Exception e) {
						e.printStackTrace();
					}

				} else if (j.until.end_ms() > lTxTimeoutMs && j.firstTx == false) {

					JobItem ji = m_qu.remove(i);

					// System.out.println("jobtask timeover");

					// if the timeout value is zero, no error is counted and no
					// error messages is displayed
					if (lTxTimeoutMs != 0) {

						try {
							m_fail++;
							m_it.doFailed(this, ji.obj);
						} catch (Exception e) {
							e.printStackTrace();
						}

					}

				}

			} // for(i

			//
			//
			if (m_qu.size() == 0)
				return null;

			n = Math.min(scope, m_qu.size());
			// pick one and return
			//
			for (int i = 0; i < n; i++) {

				JobItem j = m_qu.get(i);

				if (j.firstTx) {
					j.until.start();
					j.retry.start();
					j.firstTx = false;

					if (lTxTimeoutMs == 0) {
						m_qu.remove(i);
					}

					return j.obj;
				} else if (j.retry.end_ms() > lResendMs) {

					j.retry.start();
					return j.obj;

				}

			} // for(i

			return null;
		} // sync

	}

}
