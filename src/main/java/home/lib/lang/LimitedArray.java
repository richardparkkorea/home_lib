package home.lib.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LimitedArray<TYPE, VALUE> {

	private Object _lock = new Object();

	private ArrayList<TYPE> m_arr = new ArrayList<TYPE>();

	private ArrayList<Long> m_sec = new ArrayList<Long>();

	private Map<TYPE, VALUE> m_map = new HashMap<TYPE, VALUE>();

	private int m_limit = 0;

	public LimitedArray(int limit) {
		m_limit = limit;
	}

	/**
	 * 
	 * @param o
	 */
	public boolean add(TYPE o, VALUE v) {

		synchronized (_lock) {
			boolean r = false;
			if (m_arr.contains(o) == false) {

				r = m_arr.add(o);
				r = m_sec.add(System.currentTimeMillis());
				m_map.put(o, v);

				if (m_arr.size() > m_limit) {
					TYPE a=m_arr.remove(0);
					m_sec.remove(0);
					m_map.remove(a);
				}
			}

			if (m_arr.size() != m_sec.size()) {
				System.out.format("TmsLimitedArray m_arr.size()!=m_sec.size() si not equals %s!=%s \r\n", m_arr.size(),
						m_sec.size());
			}

			if (m_arr.size() < m_map.size()) {
				System.out.format("TmsLimitedArray size error m_arr.size()<m_map.size() %s!=%s \r\n", m_arr.size(),
						m_map.size());
			}

			return r;
		}

	}

	public boolean add(TYPE o) {
		return add(o, null);
	}

	public VALUE getValue(TYPE o) {
		synchronized (_lock) {
			return m_map.get(o);
		}
	}

	/**
	 * 
	 * @param o
	 * @return
	 */
	public boolean contains(TYPE o) {
		synchronized (_lock) {
			return m_arr.contains(o);
		}
	}

	public int size() {
		synchronized (_lock) {
			return m_arr.size();
		}
	}

	public void clear() {
		synchronized (_lock) {
			m_arr.clear();
			m_sec.clear();
			m_map.clear();
		}
	}

	/**
	 * 
	 * @param elapsedSecond
	 */
	public void removeByTime(double elapsedSecond) {

		synchronized (_lock) {
			int p = 0;
			for (int i = 0; i < m_sec.size(); i++) {

				long when = m_sec.get(p);
				if ((System.currentTimeMillis() - when) > (long) (elapsedSecond * 1000)) {
					TYPE a=m_arr.remove(p);
					m_sec.remove(p);
					m_map.remove(a);

				} else {
					p++;
				}
			} // for
		}//sync
	}

	public TYPE get(int idx) {
		synchronized (_lock) {
			return m_arr.get(idx);
		}
	}

	public TYPE[] toArray(TYPE[] t) {
		synchronized (_lock) {

			return m_arr.toArray(t);
		}
	}

}
