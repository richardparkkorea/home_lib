package home.lib.net.tms.mqtt.file;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import home.lib.util.TimeUtil;

/**
 * 
 * @author richard
 *
 */
class MFileResponses {// < eventid, milliseconds >

	Map<Long, Long> tmr = new HashMap<Long, Long>();

	Map<Long, byte[]> payload = new HashMap<Long, byte[]>();

	public long MAX = 1024 * 8;

	public long TIMEOUT = 60 * 1000;// 60 sec

	Gson gson = new Gson();

	/**
	 * 
	 * @param k
	 */
	public void put(long k, byte[] data) throws Exception {

		synchronized (tmr) {

			if (tmr.size() < MAX) {

				tmr.put(k, System.currentTimeMillis());
				payload.put(k, data);

				if (tmr.size() != payload.size()) {
					tmr.clear();
					payload.clear();
					throw new Exception(" if( tmr.size()!=payload.size()) { err!");
				}

				gc();
			}

		} // sync

	}

	/**
	 * 
	 */
	public void gc() {

		synchronized (tmr) {
			for (Long l : tmr.keySet()) {

				if ((tmr.get(l).longValue() + TIMEOUT) < System.currentTimeMillis()) {
					tmr.remove(l);
					payload.remove(l);
				}

			} // for
		} // sync
	}

	/**
	 * 
	 * @param k
	 * @param timeout
	 * @return
	 */
	public Map<String, Object> waitResp(long k, long timeout) {

		long cur = System.currentTimeMillis();

		while ((cur + timeout) > System.currentTimeMillis()) {

			synchronized (tmr) {
				if (tmr.containsKey(k)) {
					tmr.remove(k);
					// System.out.println("m.size=" + super.size());
					byte[] r = payload.remove(k);

					Map<String, Object> m = gson.fromJson(new String(r), Map.class);

					return m;

				}
			} // sync
			TimeUtil.sleep(30);
		}

		return null;
	}

}
