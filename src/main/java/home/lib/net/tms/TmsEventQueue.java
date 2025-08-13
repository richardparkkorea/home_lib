package home.lib.net.tms;

import java.util.LinkedList;

public class TmsEventQueue {

	private LinkedList<TmsEvent> m_eQueue = new LinkedList<TmsEvent>();

	private int m_maxEventCount = 1024 * 8;

	public TmsEventQueue() {
		this(1024);
	}

	public TmsEventQueue(int max) {
		m_maxEventCount = max;
	}

	public void setMaxQueue(int max) {
		m_maxEventCount = max;
	}

	public int getMaxQueue() {
		return m_maxEventCount;
	}

	/**
	 * 
	 * @param ch
	 * @param event
	 * @param rx
	 * @param tm
	 * @return
	 */
	public boolean add(TmsChannel ch, char event, byte[] rx, TmsItem tm) {
		synchronized (m_eQueue) {

			if (event == 'r' && m_eQueue.size() > m_maxEventCount) {
				ch.debug("event queue is full (max:%d) \r\n", m_eQueue.size());
				return false;
			}

			if (m_eQueue.size() > m_maxEventCount * 2) {
				ch.debug("event queue has been exceeded(%s) ", m_eQueue.size());
				return false;
			}

			m_eQueue.add(new TmsEvent(ch, event, rx, tm));
			return true;
		}
	}

	/**
	 * 
	 * @return
	 */
	public TmsEvent poll() {
		synchronized (m_eQueue) {
			return m_eQueue.pollFirst();
		}
	}

	/**
	 * 
	 * @return
	 */
	public int size() {
		synchronized (m_eQueue) {
			return m_eQueue.size();
		}
	}

	// /**
	// *
	// * @param s
	// * @param args
	// */
	// public void debug(String s, Object... args) {
	//
	// try {
	//
	// System.out.println(this.getClass().getSimpleName() + "] " + String.format(s, args));
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

}
