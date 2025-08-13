package home.lib.net.tms.mqtt.file;

import java.io.File;
import java.io.FileInputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import com.google.gson.Gson;

import home.lib.lang.Timer2;
import home.lib.lang.Timer2Task;
import home.lib.lang.UserException;
import home.lib.net.tms.mqtt.IMqtt;
import home.lib.net.tms.mqtt.MqttClient;
import home.lib.util.TimeUtil;

public class MFileHandle {

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

		if (pubrx != null)
			pubrx.close();

		pubrx = null;

	}

	/**
	 * 
	 * @return
	 */
	public String getBaseTopic() {
		return m_topic;
	}

	/**
	 * 
	 * @param name
	 * @param taskid
	 * @throws Exception
	 */
	public MFileHandle(String name, long taskid) throws Exception {

		String ip = "114.200.254.181";
		// String ip = "127.0.0.1";

		// m_topic = name + "/io/file/" + taskid;
		m_topic = "a";

		//
		pubrx = new MqttClient().setId(name + ":rx");
		pubrx.connect(ip, 1883);//

		byte qos = 0;

		//
		pubrx.subscribe(m_topic + "/#", (byte) 0, new IMqtt() {

			@Override
			public void callback(MqttClient source, String topic, byte[] payload) {

				try {
					System.out.format("callback topic=[%s]  payload.lenght=[%s] payload=%s \r\n", topic, payload.length,
							new String(payload));

					pubrx.publish("b/2", "response in callback " + new String(payload), 0);
					// pubtx.publish(topic, name, qos)

					//
					// String[] tl = topic.split("/");
					//
					// String func = tl[4];
					//
					// if (func.equals("resp")) {//
					//
					// // System.out.println("resp=" + new String(payload));
					//
					// Map<String, Object> m = gson.fromJson(new String(payload), Map.class);
					//
					// responses.put(Long.valueOf("" + m.get("event_id")), payload);
					//
					// } else if (func.equals("func")) {//
					//
					// System.out.println("payload=" + new String(payload));
					//
					// Map<String, Object> m = gson.fromJson(new String(payload), Map.class);
					//
					// if (m.get("func").equals("ls")) {
					// func_ls(topic, m);
					// }
					//
					// }
					//
					// // else if (func.equals("open") && tl[5].equals("w")) {// ready for write
					// //
					// // open_w(topic, payload);
					// //
					// // } //

				} catch (Exception e) {

					e.printStackTrace();
				}

			}

		}, 8.0);

		// pubrx.subscribe(m_topic + "1/#", (byte) 0, new IMqtt() {
		//
		// @Override
		// public void callback(MqttClient source, String topic, byte[] payload) {
		//
		// try {
		// System.out.format("callback topic=[%s] payload.lenght=[%s] \r\n", topic, payload.length);
		//
		// } catch (Exception e) {
		//
		// e.printStackTrace();
		// }
		//
		// }
		//
		// }, 8.0);

		Timer2 tm2 = new Timer2().schedule(new Timer2Task() {

			@Override
			public void start(Timer2 tmr) {
				// TODO Auto-generated method stub

			}

			@Override
			public void run(Timer2 tmr) {

				try {
					pubrx.publish("a/1", TimeUtil.now(), (byte) 0);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			@Override
			public void stop(Timer2 tmr) {
				// TODO Auto-generated method stub

			}

		}, 100, 1000);

		// TimeUtil.sleep(1000);
		// System.out.println("unsubscribe");
		// pubrx.unsubscribe(m_topic + "/#");

	}

	/**
	 * 
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public static long getChecksum(String fileName) throws Exception {

		if (new File(fileName).exists() == false)
			throw new UserException("'%s' is not exist", fileName);

		CheckedInputStream cis = new CheckedInputStream(new FileInputStream(fileName), new CRC32());

		byte[] buf = new byte[2048];
		while (cis.read(buf) >= 0) {
		}

		long checksum = cis.getChecksum().getValue();

		cis.close();

		return checksum;

	}

	// /**
	// *
	// * @param topic
	// * @param a
	// * @return
	// * @throws Exception
	// */
	// public long pub(String topic, Object... a) throws Exception {
	//
	// long eid = System.nanoTime();
	//
	// Map<String, Object> m = new HashMap<String, Object>();
	//
	// if ((a.length % 2) != 0)
	// throw new UserException("pub if( (a.length%2)!=0 ) err");
	//
	// for (int i = 0; i < a.length; i += 2) {
	// m.put(a[i + 0].toString(), a[i + 1]);
	// }
	//
	// m.put("from", m_topic + "/resp");
	// m.put("event_id", "" + eid);
	//
	// String payload = gson.toJson(m);
	//
	// // System.out.println("="+payload);
	//
	// pubtx.publish(topic, payload, 0);
	//
	// return eid;
	// }
	//
	// /**
	// *
	// * @param topic
	// * @param payload
	// * @param a
	// * @return
	// * @throws Exception
	// */
	// public boolean respPub(String topic, Map<String, Object> m, Object... a) throws Exception {
	// // Map<String, Object> m = gson.fromJson(new String(payload), Map.class);
	//
	// if (m.containsKey("event_id") && m.containsKey("from")) {
	//
	// String to = (String) m.get("from");
	//
	// if ((a.length % 2) != 0)
	// throw new UserException("pub if( (a.length%2)!=0 ) err");
	//
	// for (int i = 0; i < a.length; i += 2) {
	// m.put(a[i + 0].toString(), a[i + 1]);
	// }
	//
	// m.put("from", topic);
	//
	// String p = gson.toJson(m);
	//
	// // System.out.println(" respub.payload.length=" + p.length());
	//
	// pubtx.publish(to, p, 0);
	// return true;
	// }
	//
	// throw new UserException("there's no eventid or from");
	//
	// // return false;
	// }
	//
	// /**
	// *
	// * @param topic
	// * @param payload
	// * @return
	// * @throws Exception
	// */
	// public void func_ls(String topic, Map<String, Object> m) throws Exception {
	//
	// // System.out.println("eventid.type="+m.get("event_id").getClass().getTypeName() );
	//
	// String dir = "" + m.get("dir");
	// boolean recursive = (boolean) m.get("recursive");
	// long bl = 0;
	//
	// //
	// dir = dir.replace("/", File.separator).replace("\\", File.separator);
	//
	// ArrayList<String> ar = new ArrayList<String>();
	//
	// // System.out.format("r_getFileList(%s,%s) \r\n", dir, includeSub);
	//
	// bl = FileUtil.listFiles(dir, recursive, null, ar);
	//
	// // remove 'dir' name
	// for (int i = 0; i < ar.size(); i++) {
	// String s = ar.get(i).substring(dir.length());
	// ar.set(i, s);
	// }
	//
	// String[] fl = ar.toArray(new String[0]);
	//
	// respPub(topic, m, "count", fl.length, "total-length", bl, "files", fl);
	//
	// }
	//
	// public void ls(String dest, long taskid, String dir) throws Exception {
	//
	// String to = dest + "/io/file/" + taskid;
	//
	// long eid = pub(to + "/func", "func", "ls", "dir", dir, "recursive", true);
	//
	// Map<String, Object> m = responses.waitResp(eid, 6 * 1000);
	//
	// // System.out.println("check=" + new String(resp));
	//
	// System.out.println(m.get("files").getClass().getTypeName());
	//
	// for (String f : (ArrayList<String>) m.get("files")) {
	// // System.out.println(f);
	// }
	//
	// }

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
