package home.lib.net.tms.mqtt.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import home.lib.net.tms.mqtt.IMqtt;
import home.lib.net.tms.mqtt.MqttBroker;
import home.lib.net.tms.mqtt.MqttClient;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

public class MqttFile_tmp {

	public static void test1() throws Exception {

		System.out.println("mqtt test");
		//
		MqttBroker brk = new MqttBroker(1883);

		brk.begin();

		byte qos = 2;
		//
		MqttClient sub = new MqttClient();
		//
		sub.setId("sub1");
		sub.connect("127.0.0.1", 1883).waitForConnack(3);
		sub.subscribe("a/+/c",   new IMqtt() {

			@Override
			public void callback(MqttClient source, String topic, byte[] payload) {

				// System.out.format("callback topic=[%s] payload=[%s] \r\n", topic,
				// new String(Arrays.copyOf(payload, payload.length)));

				// System.out.format("callback topic=[%s] payload.lenght=[%s] buf=%s \r\n", topic, payload.length,
				// StringUtil.omitAfter( StringUtil.ByteArrayToHex(payload),100));

				System.out.format("callback topic=[%s]  payload.lenght=[%s] buf=%s \r\n", topic, payload.length,
						StringUtil.omitAfter(new String(payload), 100));

			}

		});

		//
		MqttClient pub = new MqttClient();
		//
		pub.setId("client1");
		//
		// //mc.connect("114.200.254.181", 1883);
		pub.connect("127.0.0.1", 1883).waitForConnack(3);

		pub.publish("a/x/c", "payload" + TimeUtil.now() + "qos0~", 0);
		pub.publish("a/x/c", "payload" + TimeUtil.now() + "qos1~", 1);
		pub.publish("a/x/c", "payload" + TimeUtil.now() + "qos2~", 2);

		for (int i = 0; i < 1024; i++) {
			pub.publish("a/x/c", "payload" + TimeUtil.now() + "qos0~ cnt=" + i, 0);
		}

		byte[] d = new byte[1024 * 32];

		d[0] = (byte) 0x88;
		pub.publish("a/x/c", d, d.length, 0);

	}

	static boolean sendOk = false;
	static boolean initialize = false;
	static int retry = 0;

	static Random rand = new Random();

	public static void test2() throws Exception {

		// {
		// RandomAccessFile file = new RandomAccessFile("d:\\tmp\\randomAccessFileTest", "rw");
		// file.seek(100);
		// file.write(new byte[32]);
		// file.seek(0);
		// file.write(new byte[32]);
		//
		//
		// file.close();
		// }

		System.out.println("mqtt test");
		//
		MqttBroker brk = new MqttBroker(1883);

		brk.begin();

		byte qos = 2;

		MqttClient subtx = new MqttClient();
		//
		subtx.setId("subtx");
		//
		// //mc.connect("114.200.254.181", 1883);
		subtx.connect("127.0.0.1", 1883).waitForConnack(3);

		//
		//
		//
		MqttClient subrx = new MqttClient();
		//
		subrx.setId("subrx");
		subrx.connect("127.0.0.1", 1883).waitForConnack(3);
		subrx.subscribe("a/taskid/#",   new IMqtt() {

			CRC32 crc = new CRC32();
			long rxLen = 0;
			// int packetReceviedFlag[]=null;

			Map<Long, byte[]> packets = new HashMap<Long, byte[]>();

			@Override
			public void callback(MqttClient source, String topic, byte[] payload) {

				// System.out.format("callback topic=[%s] payload=[%s] \r\n", topic,
				// new String(Arrays.copyOf(payload, payload.length)));

				// System.out.format("callback topic=[%s] payload.lenght=[%s] buf=%s \r\n", topic, payload.length,
				// StringUtil.omitAfter( StringUtil.ByteArrayToHex(payload),100));

				String[] tl = topic.split("/");
				// for (String t : tl)
				// System.out.format(" %s ", t);
				//
				// System.out.println("");
				//
				// System.out.format("callback topic=[%s] payload.lenght=[%s] buf=%s \r\n", topic, payload.length,
				// StringUtil.omitAfter(new String(payload), 100));

				if (tl[2].equals("winit")) {

					String v = new String(payload);
					String[] vl = v.split("\\|");

					packets.clear();
					crc = new CRC32();
					// int packetCount = Integer.valueOf(vl[0]);

					// packetReceviedFlag=new int[packetCount];
					// for( int n=0;n<packetReceviedFlag.length;n++)
					// packetReceviedFlag[n]=0;

					try {
						String t = String.format("b/taskid/initiazeok");
						subtx.publish(t, String.format("%s", rxLen), 0);
					} catch (Exception e) {

						e.printStackTrace();
					}

				}

				if (tl[2].equals("finish")) { // (target) /taskid/ finish

					String v = new String(payload);

					System.out.format("finish - %s \n", v);

					String[] vl = v.split("\\|");

					long sum = Long.valueOf(vl[0]);
					long flen = Long.valueOf(vl[1]);
					long mdate = Long.valueOf(vl[2]);

					try {
						if (flen == rxLen) {
							String t = String.format("b/taskid/finishok");
							subtx.publish(t, String.format("%s|%s", rxLen, crc.getValue()), 0);

						} else {
							String t = String.format("b/taskid/rp");
							subtx.publish(t, String.format("%s", rxLen), 0);

						}
					} catch (Exception e) {

						e.printStackTrace();
					}

					System.out.format("callback topic=[%s]  payload.lenght=[%s] payload=%s \r\n", topic, payload.length,
							v);
				} else if (tl[2].equals("w")) { // (target) /taskid/ w / (position)

					long pos = Integer.valueOf(tl[3]);

					if (rxLen == pos) {// && (rand.nextInt() % 1000) != 1) {

						// ok
						packets.put(pos, payload);// put~~~~

						while (packets.containsKey(rxLen) && packets.size() > 0) {

							// System.out.format("1 packets.size= %s \n",packets.size() );

							byte[] d = packets.remove(rxLen);

							crc.update(d);

							System.out.format("2 packets.size= %s  rxlen=%s   d.length=%s \n", packets.size(), rxLen,
									d.length);

							rxLen += d.length;// go to next position
						}

						// rxLen += payload.length;

						try {
							String t = String.format("b/taskid/wok");
							subtx.publish(t, String.format("%s", rxLen), 0);
						} catch (Exception e) {

							e.printStackTrace();
						}

						System.out.format("callback topic=[%s]  payload.lenght=[%s] \r\n", topic, payload.length);
					} else {
						// request missing packet

						if (pos > rxLen && packets.size() < 100) {
							packets.put(pos, payload);// put~~~~
						}

						try {
							String t = String.format("b/taskid/rp");
							subtx.publish(t, String.format("%s", rxLen), 0);
						} catch (Exception e) {

							e.printStackTrace();
						}
					}

				}

			}

		});

		//
		//
		//

		//
		MqttClient pubtx = new MqttClient();
		//
		pubtx.setId("pubtx");
		//
		// //mc.connect("114.200.254.181", 1883);
		pubtx.connect("127.0.0.1", 1883).waitForConnack(3);

		// pub.publish("a/x/c", "payload" + TimeUtil.now() + "qos0~", 0);
		// pub.publish("a/zzz/c", "payload" + TimeUtil.now() + "qos1~", 1);
		// pub.publish("a/rrr/c", "payload" + TimeUtil.now() + "qos2~", 2);

		for (int i = 0; i < 1024; i++) {
			// pub.publish("a/x/c", "payload" + TimeUtil.now() + "qos0~ cnt=" + i, 0);
		}

		byte[] d = new byte[1024 * 32];

		d[0] = (byte) 0x88;
		// pub.publish("a/x/c", d, d.length, 0);

		//
		//
		//
		long packetSize = 1024 * 60;

		String filePath = "d:\\tmp\\test.zip";
		// long seek = 0;
		long sendLen = 0;

		RandomAccessFile file = new RandomAccessFile(filePath, "rw");

		long fileLen = file.length();

		retry = 0;
		sendOk = false;
		long sum = getChecksum(filePath);
		//
		//
		//
		MqttClient pubrx = new MqttClient();
		//
		pubrx.setId("pubrx");
		pubrx.connect("127.0.0.1", 1883).waitForConnack(3);
		pubrx.subscribe("b/taskid/#",  new IMqtt() {

			@Override
			public void callback(MqttClient source, String topic, byte[] payload) {

				System.out.format("callback topic=[%s]  payload.lenght=[%s] \r\n", topic, payload.length);

				String[] tl = topic.split("/");

				if (tl[2].equals("initiazeok")) {//

					initialize = true;
					System.out.format("initialize ok \r\n");
					retry = 0;

				} else if (tl[2].equals("wok")) {//

					retry = 0;

				} else if (tl[2].equals("finishok")) {// (taget)/(taskid)/finishok

					String v = new String(payload);
					
					String[] vl = v.split("\\|");

					if (vl[1].equals("" + sum)) {

						sendOk = true;
						System.out.format("send ok! %s \r\n", new String(payload));
					}
					else {
						System.out.format("checksum err! %s \r\n", new String(payload));
					}

				} else if (tl[2].equals("rp")) {// (taget)/(taskid)/rp/(seek)

					try {
						long pos = Integer.valueOf(new String(payload));

						long sendLen = packetSize;

						if ((pos + sendLen) > fileLen)
							sendLen = fileLen - pos;

						if (sendLen > 0) {

							file.seek(pos);
							byte[] bytes = new byte[(int) sendLen];
							file.read(bytes);

							String t = String.format("a/taskid/w/%s", pos);

							pubtx.publish(t, bytes, bytes.length, 0);

						}
						System.out.format(" pos=%s len=%s \r\n", pos, fileLen);

					} catch (Exception e) {
						e.printStackTrace();
					}

					// if ((v % packetSize) != 0) {
					// System.out.println("error!");
					// }
					// seek = v;

				}

			}

		});

		//
		// upload 
		//
		long seek = 0;

		while (retry < 60 && sendOk == false) {

			if (initialize == false) {

				String topic = String.format("a/taskid/winit");

				long mdate = new File(filePath).lastModified();

				long packetCount = (fileLen / packetSize);
				if ((fileLen % packetSize) != 0)
					packetCount++;

				pubtx.publish(topic, String.format("%s|%s|%s|%s|%s", packetCount, sum, fileLen, mdate, filePath), 0);

				TimeUtil.sleep(1000);
				retry++;
			} else if (seek < fileLen) { // packet sending

				retry = 0;

				sendLen = packetSize;

				if ((seek + sendLen) > fileLen)
					sendLen = fileLen - seek;

				file.seek(seek);
				byte[] bytes = new byte[(int) sendLen];
				file.read(bytes);

				String topic = String.format("a/taskid/w/%s", seek);

				pubtx.publish(topic, bytes, bytes.length, 0);

				seek += sendLen;// increasing

				System.out.format(" ( %s / %s ) \r\n", StringUtil.formatBytesSize(seek),
						StringUtil.formatBytesSize(fileLen));

			} else {// finalizing

				String topic = String.format("a/taskid/finish");

				long mdate = new File(filePath).lastModified();

				pubtx.publish(topic, String.format("%s|%s|%s", sum, fileLen, mdate), 0);

				TimeUtil.sleep(1000);
				retry++;
			}
		}

		file.close();
		
		
		//
		// download 
		//
		

		System.out.println("file closed");

	}

	public static long getChecksum(String fileName) throws Exception {
		// try {
		// fileName = fileName.replace("/", File.separator).replace("\\", File.separator);

		if (new File(fileName).exists() == false)
			return 0L;

		// System.out.println("do crc-1");

		CheckedInputStream cis = null;
		long fileSize = 0;
		// Computer CRC32 checksum
		cis = new CheckedInputStream(new FileInputStream(fileName), new CRC32());

		fileSize = new File(fileName).length();

		byte[] buf = new byte[2048];
		while (cis.read(buf) >= 0) {
		}

		long checksum = cis.getChecksum().getValue();

		return checksum;

	}

	// private static void appendData(String filePath, String data) throws IOException {
	// RandomAccessFile raFile = new RandomAccessFile(filePath, "rw");
	// raFile.seek(raFile.length());
	// System.out.println("current pointer = " + raFile.getFilePointer());
	// raFile.write(data.getBytes());
	// raFile.close();
	//
	// }
	//
	// private static void writeData(String filePath, String data, int seek) throws IOException {
	// RandomAccessFile file = new RandomAccessFile(filePath, "rw");
	// file.seek(seek);
	// file.write(data.getBytes());
	// file.close();
	// }
	//
	// private static byte[] readFromFile(String filePath, int seek, int readlen) throws IOException {
	// RandomAccessFile file = new RandomAccessFile(filePath, "r");
	//
	// file.seek(seek);
	// byte[] bytes = new byte[readlen];
	// file.read(bytes);
	// file.close();
	// return bytes;
	// }

}
