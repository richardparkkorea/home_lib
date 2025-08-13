package home.lib.net.tms;

import java.util.LinkedList;

import home.lib.util.TimeUtil;

public class TmsJobTask<E> {

	TmsJobTaskInterface m_it = null;

	public TmsJobTask(TmsJobTaskInterface it) {
		m_it = it;

	}

	public long m_succeed = 0;
	public long m_fail = 0;

	public long lMaxTasks = 1024 * 3;

	public long getSucceedCount() {
		return m_succeed;
	}

	public long getFailCount() {
		return m_fail;
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

		long lResendMs = 1000;
		long lTxTimeoutMs = (6000);

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
		synchronized (m_qu) {
			return m_qu.size();
		}
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

	@SuppressWarnings("unchecked")
	public E[] toArray(E[] a) {
		synchronized (m_qu) {

			int size = m_qu.size();

			if (a.length < size) {
				a = (E[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
			}

			for (int i = 0; i < m_qu.size(); i++) {

				a[i] = m_qu.get(i).obj;
			}

			return a;
		}
	}

	/**
	 * return <br>
	 * true - add false - queue is full!
	 * 
	 * rs - resend time(ms) to - timeout (ms)
	 * 
	 * @param obj
	 */
	public boolean add(E obj, long rs, long to) {

		JobItem j = new JobItem();
		j.obj = obj;
		j.lResendMs = rs;
		j.lTxTimeoutMs = to;

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
					return (E) j.obj;
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

				} else if (j.until.end_ms() > j.lTxTimeoutMs && j.firstTx == false) {

					JobItem ji = m_qu.remove(i);

					// System.out.println("jobtask timeover");

					// if the timeout value is zero, no error is counted and no
					// error messages is displayed
					if (j.lTxTimeoutMs != 0) {

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

					if (j.lTxTimeoutMs == 0) {
						m_qu.remove(i);
					}

					return j.obj;
				} else if (j.retry.end_ms() > j.lResendMs) {

					j.retry.start();
					return j.obj;

				}

			} // for(i

			return null;
		} // sync

	}

}
