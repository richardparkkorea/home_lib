package home.lib.net.tms.mqtt.file;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import home.lib.lang.UserException;
import home.lib.net.tms.mqtt.IMqtt;
import home.lib.net.tms.mqtt.MqttClient;
import home.lib.util.FileUtil;
import home.lib.util.TimeUtil;

public class MFileHandle_tmp {

	// /**
	// *
	// * @author richard
	// *
	// */
	// static class FileFilter implements FilenameFilter {
	//
	// @Override
	// public boolean accept(File dir, String name) {
	// return true;// name.toLowerCase().endsWith(".zip");
	// }
	//
	// }

	MFileResponses responses = new MFileResponses();

	MqttClient pubtx = null;

	MqttClient pubrx = null;

	// ArrayList<Long> responses = new ArrayList<Long>();

	String m_topic = "";
	Gson gson = new Gson();

	/**
	 * 
	 * topic format
	 * 
	 * 
	 * (name) / (kind1) / (kind2) / (func) / depend on the func....
	 * 
	 * 
	 */

	public void cleanUp() {

		if (pubtx != null)
			pubtx.close();

		if (pubrx != null)
			pubrx.close();

		pubtx = null;
		pubrx = null;

	}

	/**
	 * 
	 * @param name
	 * @param uniqueId
	 * @throws Exception
	 */
	public MFileHandle_tmp(String name, long taskid) throws Exception {

		m_topic = name + "/io/file/" + taskid;

		//
		pubtx = new MqttClient();
		pubtx.setId("pubtx");
		//
		// //mc.connect("114.200.254.181", 1883);
		pubtx.connect("127.0.0.1", 1883).waitForConnack(3);

		//
		//
		//
		pubrx = new MqttClient();
		//
		pubrx.setId("pubrx");
		pubrx.connect("127.0.0.1", 1883).waitForConnack(3);

		byte qos = 0;

		//
		//
		//

		pubrx.subscribe(m_topic + "/#",  new IMqtt() {

			@Override
			public void callback(MqttClient source, String topic, byte[] payload) {

				try {
					System.out.format("callback topic=[%s]  payload.lenght=[%s] \r\n", topic, payload.length);

					String[] tl = topic.split("/");

					String func = tl[4];

					if (func.equals("resp")) {//

						// System.out.println("resp=" + new String(payload));

						Map<String, Object> m = gson.fromJson(new String(payload), Map.class);

						responses.put(Long.valueOf("" + m.get("event_id")), payload);

					} else  if (func.equals("func")) {//

						System.out.println("payload=" + new String(payload));

						Map<String, Object> m = gson.fromJson(new String(payload), Map.class);

						if (m.get("func").equals("ls")) {
							func_ls(topic,m);
						}

					}

					// else if (func.equals("open") && tl[5].equals("w")) {// ready for write
					//
					// open_w(topic, payload);
					//
					// } //

				} catch (Exception e) {

					e.printStackTrace();
				}

			}

		});

	}

	/**
	 * 
	 * @param topic
	 * @param a
	 * @return
	 * @throws Exception
	 */
	public long pub(String topic, Object... a) throws Exception {

		long eid = System.nanoTime();

		Map<String, Object> m = new HashMap<String, Object>();

		if ((a.length % 2) != 0)
			throw new UserException("pub if( (a.length%2)!=0 ) err");

		for (int i = 0; i < a.length; i += 2) {
			m.put(a[i + 0].toString(), a[i + 1]);
		}

		m.put("from", m_topic + "/resp");
		m.put("event_id", "" + eid);

		String payload = gson.toJson(m);

		// System.out.println("="+payload);

		pubtx.publish(topic, payload, 0);

		return eid;
	}

	/**
	 * 
	 * @param topic
	 * @param payload
	 * @param a
	 * @return
	 * @throws Exception
	 */
	public boolean respPub(String topic,Map<String, Object> m, Object... a) throws Exception {
		//Map<String, Object> m = gson.fromJson(new String(payload), Map.class);

		if (m.containsKey("event_id") && m.containsKey("from")) {

			String to = (String) m.get("from");

			if ((a.length % 2) != 0)
				throw new UserException("pub if( (a.length%2)!=0 ) err");

			for (int i = 0; i < a.length; i += 2) {
				m.put(a[i + 0].toString(), a[i + 1]);
			}

			m.put("from", topic);

			String p = gson.toJson(m);

			// System.out.println(" respub.payload.length=" + p.length());

			pubtx.publish(to, p, 0);
			return true;
		}

		throw new UserException("there's no eventid or from");

		// return false;
	}

	/**
	 * 
	 * @param topic
	 * @param payload
	 * @return
	 * @throws Exception
	 */
	public void func_ls(String topic, Map<String, Object> m) throws Exception {

		// System.out.println("eventid.type="+m.get("event_id").getClass().getTypeName() );

		String dir = "" + m.get("dir");
		boolean recursive = (boolean) m.get("recursive");
		long bl = 0;

		//
		dir = dir.replace("/", File.separator).replace("\\", File.separator);

		ArrayList<String> ar = new ArrayList<String>();

		// System.out.format("r_getFileList(%s,%s) \r\n", dir, includeSub);

		bl = FileUtil.listFiles(dir, recursive, null, ar);

		// remove 'dir' name
		for (int i = 0; i < ar.size(); i++) {
			String s = ar.get(i).substring(dir.length());
			ar.set(i, s);
		}

		String[] fl = ar.toArray(new String[0]);

		respPub(topic, m, "count", fl.length, "total-length", bl, "files", fl);

	}

	public void ls(String dest, long taskid, String dir) throws Exception {

		String to = dest + "/io/file/" + taskid;

		long eid = pub(to+"/func", "func", "ls", "dir", dir, "recursive", true);

		Map<String, Object> m = responses.waitResp(eid, 6 * 1000);

		// System.out.println("check=" + new String(resp));

		System.out.println(m.get("files").getClass().getTypeName());

		for (String f : (ArrayList<String>) m.get("files")) {
			// System.out.println(f);
		}

	}

	//
	// /**
	// *
	// * @param topic
	// * @param payload
	// * @throws Exception
	// */
	// private void open_w(String topic, byte[] payload) throws Exception {
	//
	// Map<String, Object> m = gson.fromJson(new String(payload), Map.class);
	//
	// if (m.containsKey("event_id") && m.containsKey("from")) {
	//
	// String to = m.get("from");
	//
	// m.put("from", topic);
	// m.put("w-handle", "" + System.nanoTime());
	//
	// String p = gson.toJson(m);
	// pubtx.publish(to, p, 0);
	//
	// }
	//
	// }
	//
	// /**
	// *
	// * @param dest
	// * @param taskid
	// * @param fileName
	// * @throws Exception
	// */
	// public void upload(String dest, long taskid,String fileName) throws Exception {
	//
	// String to = dest + "/io/file/" + taskid;
	//
	// long eid = System.nanoTime();
	//
	// Map<String, Object> m = new HashMap<String, Object>();
	//
	// m.put("from", m_topic + "/resp");
	// m.put("event_id", "" + eid);
	// m.put("file_name",fileName);
	//
	// String payload = gson.toJson(m);
	//
	// // pubtx.publish(topic, String.format("%s|%s|%s|%s|%s", packetCount, sum, fileLen, mdate, filePath), 0);
	// pubtx.publish(to + "/open/w", payload, 0);
	//
	// boolean resp = responses.find(eid, 3 * 1000);
	// System.out.println("check=" + resp);
	// }

}
