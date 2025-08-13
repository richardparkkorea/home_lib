package org.xcopy3;

import home.lib.lang.UserException;
import home.lib.net.tms.mqtt3ssl.IMqtt3;
import home.lib.net.tms.mqtt3ssl.IMqttSub3;
import home.lib.net.tms.mqtt3ssl.MqttBroker3;
import home.lib.net.tms.mqtt3ssl.MqttClient3;
import home.lib.util.TimeUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.*;
import java.util.zip.CRC32;

import com.google.gson.Gson;

public class FileHandler implements IMqtt3 {

	Map<String, FileStat> handles = new HashMap<String, FileStat>();

	ArrayList<FilePack> response = new ArrayList<FilePack>();

	int qos = 2;

	double m_timeout = 6;

	Gson gson = new Gson();

	class FilePack {
		public FilePack(String src) {
			String k = src.replace(File.separator, "_").replace("/", "_").replace(":", "_");
			this.key = k;
			this.pathName = src.replace(':', '_').replace("/", File.separator).replace("\\", File.separator);

		}

		public long sidx = System.nanoTime();
		String pathName;
		String key;
		long pos;
		long len;
		byte[] data;
		public long crc;
		public String from;
		public long lastModified;
		// public long idx;
		protected String resp;
		protected long packSize;
		// protected String tmpName;
		protected String[] files;
		protected String reason;
		protected String result;

	}

	class FileStat {

		String pathName;

		// String tmpName;
		boolean writeMode = false;

		TimeUtil timeout = new TimeUtil();

		CRC32 crc = new CRC32();
		long crc_last_pos = 0;

		FileStat(String tmp, boolean w) {

			tmp = tmp.replace("/", File.separator).replace("\\", File.separator);

			pathName = tmp;
			writeMode = w;
			// tmpName = tmp;

		}

		void read(long pos, byte[] outbuf) throws Exception {
			//
			//
			RandomAccessFile handle = new RandomAccessFile(pathName, "r");

			// long len = handle.length();

			// if (pos < len)
			handle.seek(pos);
			handle.read(outbuf);

			if (pos == crc_last_pos) {
				crc_last_pos += outbuf.length;
				crc.update(outbuf);
			}

			handle.close();

		}

		boolean write(long pos, byte[] payload) throws Exception {

			if (writeMode == false)
				throw new UserException("open without write mode");
			if (pathName == null)
				return false;

			RandomAccessFile handle = new RandomAccessFile(pathName, "rwd");

			long len = handle.length();

			// if (pos < len)
			// return true;// already wrote?

			if (pos != len)
				return false;

			handle.seek(len);

			handle.write(payload);
			handle.close();

			if (pos == len)
				crc.update(payload);

			return true;

		}

		long length() throws Exception {

			return new File(pathName).length();

		}

		public void close(FilePack pk, String saveName) throws Exception {

			if (pathName == null)
				return;

			if (writeMode == false)
				return;

			saveName = saveName.replace("/", File.separator).replace("\\", File.separator);

			long wlen = new File(pathName).length();

			System.gc();

			// System.out.format("%x %x \r\n", crc.getValue(), pk.crc);

			if (crc.getValue() != pk.crc || wlen != pk.len) {
				// if (wlen != pk.len) {

				System.out.println("crc error!");

				new File(pathName).delete();// err

			} else {

				File f = new File(saveName);

				if (f.isFile())
					f.delete();

				File parent = new File(Paths.get(saveName).getParent().toString());
				if (parent.exists() == false) {
					parent.mkdirs();
				}

				// System.out.println("new File(pathName).exists=" + new File(pathName).exists());
				// System.out.println("exists=" + f.exists());

				new File(pathName).setLastModified(pk.lastModified);
				// new File(pathName).renameTo(f);
				// try {
				Path oldfile = Paths.get(pathName);
				Path newfile = Paths.get(saveName);
				Files.move(oldfile, newfile);

				pathName = null;// finish

			}

		}

		public boolean exists() {

			return new File(pathName).exists();
		}

		public long lastModified() {

			return new File(pathName).lastModified();

		}

	}

	static String defaultTopic(String serverName) {
		return serverName + "/fs";
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	String m_topic;

	String m_basePath;

	MqttClient3 m_rx = new MqttClient3();

	MqttClient3 m_tx = new MqttClient3();

	long m_packetSize = 1024 * 64;//

	public FileHandler(String ip, int port) throws Exception {
		this("fs-cli." + UUID.randomUUID(), ip, port);
	}

	/**
	 * 
	 * @param id
	 * @param svrIp
	 * @param svrPort
	 * @throws Exception
	 */
	public FileHandler(String id, String svrIp, int svrPort) throws Exception {

		if (id.indexOf('*') != -1 || id.indexOf('?') != -1 || id.indexOf('#') != -1 || id.indexOf('/') != -1)
			throw new UserException("id format err ");

		// set default base path
		if (OsUtil.getOS() == OsUtil.OS.WINDOWS) {
			m_basePath = "c:" + File.separator + "tmp" + File.separator;
		} else {
			m_basePath = File.separator + "tmp" + File.separator;
		}

		m_topic = defaultTopic(id);// id + "/fs";

		long ns = System.nanoTime();

		m_rx.setId(id +  ns);
		m_rx.connect(svrIp, svrPort, this);

		m_tx.setId(id + ns);
		m_tx.connect(svrIp, svrPort, new IMqtt3() {

			@Override
			public void onConnected(MqttClient3 source) {

			}

			@Override
			public void onDisonnected(MqttClient3 source) {

			}

			@Override
			public void deliveryComplete(long pi, long past_ms) {

			}
		});

	}

	/**
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public FileHandler setBasePath(String path) throws Exception {

		if (path.endsWith(File.separator) == false)
			throw new UserException("basepath must endwith File.separator");

		if (new File(path).isDirectory() == false)
			throw new UserException("basepath is not directory");

		m_basePath = path;
		return this;

	}

	/**
	 * 
	 */
	public void close() {
		try {
			m_rx.close();
		} catch (Exception e) {
			// e.printStackTrace();
		}

		try {
			m_tx.close();
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	@Override
	public void onConnected(MqttClient3 source) {

		try {
			long pi = source.subscribe(m_topic + "/#", qos, new IMqttSub3() {

				@Override
				public void onReceived(MqttClient3 source, String topic, byte[] payload) {

					try {
						// (server name)/(uuid)/(open/read/write/close , event)
						// System.out.println(topic);

						String t[] = topic.split("/");

						if (t.length != 3)
							return;// error

						String type = t[1];
						String func = t[2];

						FilePack pk = gson.fromJson(new String(payload), FilePack.class);

						//
						// act

						if (func.equals("open")) {

							// String fn = pk.pathName;

							if (handles.containsKey(pk.key) == true)
								return;// err

							// String pathName = m_basePath + fn;

							// pathName = pathName.replace("/", File.separator).replace("\\", File.separator);

							// String tmpName = m_basePath + System.nanoTime();

							// if (new File(filepath).exists())
							// return;// err

							FileStat fs = new FileStat(m_basePath + System.nanoTime(), true);
							// fs.open();

							pk.resp = "open";
							pk.pathName = fs.pathName;
							// pk.tmpName = tmpName;

							m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);

							handles.put(pk.key, fs);

						}

						else if (func.equals("write")) {

							FileStat fs = handles.get(pk.key);

							if (fs == null)
								return;// err

							// System.out.format("write pos=%s f len=%s \r\rn", pk.pos + pk.data.length, pk.len);

							if (pk.len == fs.length()) {

								pk.pos = fs.length();
								pk.resp = "wrote";
								pk.data = null;
								m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);

								// System.out.println("wrote " + pk.pos + " " + pk.len);

							} else if (fs.write(pk.pos, pk.data)) {

								pk.pos = fs.length();
								pk.resp = "wrote";
								pk.data = null;
								m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);

								// System.out.println("wrote " + pk.pos + " " + pk.len);

							} else {
								// System.out.println("missing pos=" + pk.pos);

								pk.pos = fs.length();
								pk.resp = "missing";
								pk.data = null;
								m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);

							}

						}

						else if (func.equals("close")) {

							FileStat fs = handles.get(pk.key);

							if (fs == null)
								return;// err

							long len = fs.length();

							fs.close(pk, m_basePath + pk.pathName);

							//
							// after renameTo

							pk.len = len;
							pk.crc = fs.crc.getValue();
							// pk.lastModified = fs.lastModified();
							pk.resp = "closed";
							m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);

							handles.remove(pk.key);// **remove handle

						} else if (func.equals("get")) {

							// File src = new File(m_basePath + srcFile);

							pk.resp = "get";

							// String fn = pk.pathName;

							if (handles.containsKey(pk.key) == true)
								return;// err

							FileStat fs = new FileStat(m_basePath + pk.pathName, false);
							handles.put(pk.key, fs);

							if (fs.exists()) {
								pk.len = fs.length();
								pk.lastModified = fs.lastModified();
								m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);
							} else {
								pk.len = 0;
								pk.lastModified = 0;
								m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);
							}

						} else if (func.equals("read")) {

							// System.out.println("read pos=" + pk.pos);

							FileStat fs = handles.get(pk.key);

							if (fs == null)
								return;// err

							pk.resp = "read";

							long len = fs.length();

							long size = Math.min(pk.packSize, len - pk.pos);

							byte buf[] = new byte[(int) size];
							fs.read(pk.pos, buf);
							// handle.close();

							pk.len = len;
							pk.data = buf;
							pk.crc = fs.crc.getValue();

							m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);

							// }

						} else if (func.equals("resp")) {

							FilePack rs = gson.fromJson(new String(payload), FilePack.class);

							response.add(rs);

						} else if (func.equals("dir")) {

							String path = m_basePath
									+ pk.pathName.replace("/", File.separator).replace("\\", File.separator);
							;
							if (new File(path).isDirectory()) {
								pk.resp = "dir";

								pk.files = getFiles(m_basePath, path, pk);// pk.len will set in this function

								m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);

							} else {

								pk.resp = "err";
								pk.reason = "dir not exists";
								m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);

							}

						} else if (func.equals("length")) {

							String path = m_basePath
									+ pk.pathName.replace("/", File.separator).replace("\\", File.separator);

							if (new File(path).isFile()) {
								pk.resp = "length";

								pk.len = new File(path).length();
								pk.lastModified = new File(path).lastModified();

								m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);

							} else {

								pk.resp = "err";
								pk.reason = "get length err";
								m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);

							}

						} else if (func.equals("isdir")) {

							String path = m_basePath
									+ pk.pathName.replace("/", File.separator).replace("\\", File.separator);

							pk.resp = "isdir";
							pk.result = "" + new File(path).isDirectory();

							m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);

						} else if (func.equals("isfile")) {

							String path = m_basePath
									+ pk.pathName.replace("/", File.separator).replace("\\", File.separator);

							pk.resp = "isfile";
							pk.result = "" + new File(path).isFile();

							m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);

						} else if (func.equals("mkdirs")) {

							String path = m_basePath
									+ pk.pathName.replace("/", File.separator).replace("\\", File.separator);

							pk.resp = "mkdirs";
							pk.result = "" + new File(path).mkdirs();

							m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);

						} else if (func.equals("basedir")) {

							// String path = m_basePath
							// + pk.pathName.replace("/", File.separator).replace("\\", File.separator);

							pk.resp = "basedir";
							pk.result = m_basePath;

							m_tx.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);

						}

					} catch (Exception e) {

						e.printStackTrace();
					}

				}

				@Override
				public void subscribeSuccess(MqttClient3 source, String topic) {

					// System.out.println("subscribed " + source.getId() + " " + topic);

				}

				@Override
				public void subscribeFailed(MqttClient3 source, String topic) {

					source.discon(false);

				}

			});

			// System.out.println("client subscribed ");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void deliveryComplete(long pi, long past_ms) {

	}

	@Override
	public void onDisonnected(MqttClient3 source) {
		// System.out.println("sub ");

	}

	// public boolean WriteFile(String srcName, String sSvr, String src, String destName, String dest,
	// CopyingInterface2 cInter) throws Exception {
	//
	//
	//
	// }

	public boolean upload(String destServerName, String src) throws Exception {

		// if (srcPath.endsWith(File.separator) == false)
		// throw new UserException("basepath must endwith File.separator");

		if (new File(src).isFile() == false)
			throw new UserException("src is not file");

		String destTopic = defaultTopic(destServerName);// + "/fs";

		// CRC32 crc = new CRC32();

		FilePack pk = new FilePack(m_basePath + src);
		// File src = new File(srcPath + srcFile);
		FileStat fs = new FileStat(m_basePath + src, false);

		pk.len = fs.length();
		pk.lastModified = fs.lastModified();
		pk.from = m_topic;

		FilePack info = gson.fromJson(gson.toJson(pk), FilePack.class);// clone

		m_tx.publish(destTopic + "/open", gson.toJson(pk).getBytes(), qos);

		// RandomAccessFile fs = new RandomAccessFile(src, "r");

		// long len = src.length();

		// long pack = 1024;

		long pos = 0;
		long packetSize = m_packetSize;// 1024 * 128;

		TimeUtil to = new TimeUtil();

		int step = 1;

		// for (long p = 0; p < len; p += pack) {
		while (to.end_sec() < m_timeout) {

			if (step == 1) {

			}

			else if (step == 2) {
				long readn = Math.min(packetSize, info.len - pos);

				byte buf[] = new byte[(int) readn];

				fs.read(pos, buf);

				pk.data = buf;
				pk.pos = pos;
				pk.len = info.len;

				m_tx.publish(destTopic + "/write", gson.toJson(pk).getBytes(), qos);

				if ((pos + packetSize) < info.len) {
					pos += packetSize;
				} else {
					TimeUtil.sleep(100);
				}

			}

			while (response.size() > 0) {
				FilePack resp = response.removeFirst();

				if (resp != null) {

					if (resp.resp.equals("open")) {
						to.start();//

						step = 2;
						info = resp;

					} else if (resp.resp.equals("missing")) {
						to.start();// reflesh

						pos = resp.pos;
					} else if (resp.resp.equals("wrote")) {

						to.start();// reflesh

						if (resp.pos == info.len) {

							pk.crc = fs.crc.getValue();

							m_tx.publish(destTopic + "/close", gson.toJson(pk).getBytes(), qos);
						}
					} else if (resp.resp.equals("closed")) {

						// System.out.println("upload finished");

						return true;
					} else if (resp.resp.equals("err")) {
						to.start();//

						throw new UserException(pk.reason);

					}
				}
			}

			TimeUtil.sleep(30);
		} // for
			//

		throw new UserException("timeout!");
	}

	public boolean download(String destServerName, String dest) throws Exception {

		String destTopic = defaultTopic(destServerName);// + "/fs";

		// TimeUtil tmClose = new TimeUtil();

		TimeUtil to = new TimeUtil();
		// CRC32 crc = new CRC32();

		// String fn = "test\\lombok.jar";

		FilePack pk = new FilePack(dest);
		pk.from = m_topic;
		// pk.idx = System.nanoTime();

		FileStat fs = new FileStat(m_basePath + System.nanoTime(), true);
		FilePack info = null;

		long packetSize = m_packetSize;// 1024 * 128;

		// wait
		int step = 0;

		m_tx.publish(destTopic + "/get", gson.toJson(pk).getBytes(), qos);

		int pos = 0;

		step = 1;
		while (to.end_sec() < m_timeout) {

			if (step == 2) {

				if (fs.length() == info.len) {

					pk.data = null;
					pk.len = fs.length();
					m_tx.publish(destTopic + "/close", gson.toJson(pk).getBytes(), qos);

					TimeUtil.sleep(100);

				} else {

					pk.data = null;
					pk.pos = pos;
					pk.packSize = packetSize;// Math.min(packetSize, info.len - packetSize);
					m_tx.publish(destTopic + "/read", gson.toJson(pk).getBytes(), qos);

					if ((pos + packetSize) < info.len) {
						pos += packetSize;
					} else {
						TimeUtil.sleep(100);
					}
				}

			}

			while (response.size() > 0) {
				FilePack resp = response.removeFirst();

				if (resp != null) {
					if (resp.resp.equals("get") && step == 1) {
						step = 2;
						// fs.open();
						info = resp;

						if (resp.len == 0)
							throw new UserException("get file failed");

						to.start();// reflesh

					} else if (resp.resp.equals("read") && step == 2) {

						// System.out.println("read.resp " + (resp.pos + resp.data.length) + " fs.len= " + fs.length()
						// + " info.len= " + info.len);

						if (fs.write(resp.pos, resp.data)) {

							// System.out.println("download pos=" + resp.pos + " " + info.len + " " + fs.length());

						} else {
							pk.pos = fs.length();
						}

						to.start();// reset

					} else if (resp.resp.equals("closed") && step == 2) {

						fs.close(resp, m_basePath + pk.pathName);

						// System.out.println("how finish?");
						return true;

					} else if (resp.resp.equals("err")) {
						to.start();//

						throw new UserException(pk.reason);

					}
				}
			}

			TimeUtil.sleep(30);
		}

		throw new UserException("timeout!");
	}

	/**
	 * 
	 * @param basedir
	 * @param dir
	 * @return
	 * @throws Exception
	 */
	public String[] getFiles(String basedir, String dir, FilePack out) throws Exception {

		basedir = basedir.replace("/", File.separator).replace("\\", File.separator);

		dir = dir.replace("/", File.separator).replace("\\", File.separator);

		String s = "";
		ArrayList<String> ar = new ArrayList<String>();

		if (new File(dir).isFile()) {

			ar.add(dir.substring(basedir.length()));

		} else if (new File(dir).isDirectory()) {

			ar.add(dir);

			long bl = getFiles_sub(dir, ar, true);
			out.len = bl;

			// remove 'dir' name
			for (int i = 0; i < ar.size(); i++) {
				// System.out.format(" (%s) , (%s) \r\n", ar.get(i), basedir);

				s = ar.get(i).substring(basedir.length());
				ar.set(i, s);
			}
		}

		return ar.toArray(new String[0]);

	}

	/**
	 * 
	 * @param dir
	 * @param arr
	 * @param includeSub
	 * @return
	 */
	private static long getFiles_sub(String dir, ArrayList<String> arr, boolean includeSub) {

		long byteLength = 0;
		File[] l = new File(dir).listFiles();// .listFiles(new MyFilenameFilter());
		for (File f : l) {

			if (f.isFile()) {
				arr.add(f.getAbsolutePath());
				byteLength += f.length();
			} else if (f.isDirectory()) {
				arr.add(f.getAbsolutePath() + "\\");

				if (includeSub) {
					byteLength += getFiles_sub(f.getAbsolutePath(), arr, true);
				}
			}
		}
		return byteLength;

	}

	/**
	 * 
	 * @param destServerName
	 * @param path
	 * @param timeout
	 * @param func
	 * @return
	 * @throws Exception
	 */
	public FilePack get(String destServerName, String path, double timeout, String func) throws Exception {

		String destTopic = defaultTopic(destServerName);// + "/fs";

		FilePack pk = new FilePack(path);
		pk.from = m_topic;

		m_tx.publish(destTopic + "/" + func, gson.toJson(pk).getBytes(), qos);

		TimeUtil to = new TimeUtil();

		while (to.end_sec() < timeout) {

			while (response.size() > 0) {
				FilePack resp = response.removeFirst();

				if (resp != null) {
					if (pk.sidx == resp.sidx) {
						if (resp.resp.equals(func)) {
							to.start();//

							return resp;

						} else if (resp.resp.equals("err")) {
							to.start();//

							throw new UserException(resp.reason);

						}
					}
				}
			} // while

			TimeUtil.sleep(30);
		} // for
			//

		throw new UserException("timeout!");
	}

	public FilePack dir(String dest, String path) throws Exception {

		FilePack resp = get(dest, path, m_timeout, "dir");

		// long totalBytes=resp.len;

		// return resp.files;
		return resp;

	}

	public long length(String dest, String path) throws Exception {

		FilePack resp = get(dest, path, m_timeout, "length");

		return resp.len;

	}

	public boolean isdir(String dest, String path) throws Exception {

		FilePack resp = get(dest, path, m_timeout, "isdir");

		return "true".equals(resp.result);

	}

	public boolean isfile(String dest, String path) throws Exception {

		FilePack resp = get(dest, path, m_timeout, "isfile");

		return "true".equals(resp.result);

	}

	public boolean mkdirs(String dest, String path) throws Exception {

		FilePack resp = get(dest, path, m_timeout, "mkdirs");

		return "true".equals(resp.result);

	}

	public String basedir(String dest) throws Exception {

		FilePack resp = get(dest, "", m_timeout, "basedir");

		return resp.result;

	}

	public long lastModified(String dest, String path) throws Exception {

		FilePack resp = get(dest, path, m_timeout, "length");

		return resp.lastModified;

	}
}
