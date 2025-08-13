package deprecated.lib.net.queue;

import java.util.ArrayList;

/**
 * 
 * <p>
 * Title:
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * 
 * <p>
 * Company:
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */
@Deprecated
class NqEventManager {
	public NqEventManager() {
	}

	Object m_lock = new Object(); // lang__criticalsection m_event_lock;
	ArrayList<NqEvent> m_events = new ArrayList<NqEvent>(); // ArrayList<NqEventClass*,NqEventClass*>
																// m_event_list;

	long m_bytesLength = 0;
	long m_maxQueueBytesLimit = (1024 * 1024 * 32);// 32M:default

	public int addEvent(NqEvent cl) {
		synchronized (m_lock) {
			m_events.add(cl);

			if (m_bytesLength > m_maxQueueBytesLimit && cl.buf != null)
				return -1;// length over

			if (cl.buf != null) {
				m_bytesLength += cl.buf.length;
			}
			m_bytesLength += NqEvent.DEFAULT_BYTES_SIZE;

			return m_events.size();
		}
	}

	public NqEvent popAEvent() {
		synchronized (m_lock) { // lang__blockLock cb(&m_event_lock);
			if (m_events.size() == 0)
				return null;
			NqEvent cl = (NqEvent) m_events.get(0);
			m_events.remove(0);

			if (cl.buf != null) {
				m_bytesLength -= cl.buf.length;
			}
			m_bytesLength -= NqEvent.DEFAULT_BYTES_SIZE;

			return cl;
		}
	}

	public int getEventCount() {
		// lang__blockLock cb(&m_event_lock);
		// synchronized (m_event_lock) {
		return m_events.size();
		// }
	}

	public long getEventBufferLength() {
		return m_bytesLength;
	}

	public int clear() {
		synchronized (m_lock) { // lang__blockLock cb(&m_event_lock);
			// /clear event buffer
			// int cnt = getEventCount();
			// for (int k = 0; k < cnt; k++) {
			// popAEvent();
			// //NqEventClass e1 = popAEvent();
			// //e1 = null;
			// }
			m_events.clear();
			return 1;
		}
	}

};
