package org.xcopy3;

import home.lib.lang.UserException;
import home.lib.net.tms.mqtt3ssl.IMqtt3;
import home.lib.net.tms.mqtt3ssl.IMqttSub3;
import home.lib.net.tms.mqtt3ssl.MqttBroker3;
import home.lib.net.tms.mqtt3ssl.MqttClient3;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.CRC32;

import org.xcopy3.FileHandler.FilePack;

import com.google.gson.Gson;

public class MqttTest4 {

	// static class FilePack {
	// public FilePack(String src) {
	// String k = src.replace(File.separator, "_").replace("/", "_");
	// this.key = k;
	// this.pathName = src;
	//
	// }
	//
	// String pathName;
	// String key;
	// long pos;
	// long len;
	// byte[] data;
	// public long crc;
	// public String from;
	// public long lastModified;
	// public long idx;
	// protected String resp;
	// protected long packSize;
	// // protected String tmpName;
	// protected String[] files;
	//
	// }
	//
	// static class FileStat {
	//
	// String pathName;
	//
	// // String tmpName;
	// boolean writeMode = false;
	//
	// TimeUtil timeout = new TimeUtil();
	//
	// CRC32 crc = new CRC32();
	// long crc_last_pos = 0;
	//
	// FileStat(String tmp, boolean w) {
	//
	// tmp = tmp.replace("/", File.separator).replace("\\", File.separator);
	//
	// pathName = tmp;
	// writeMode = w;
	// // tmpName = tmp;
	//
	// }
	//
	// // boolean open() throws Exception {
	// //
	// // if (new File(tmpName).exists())
	// // return false;
	// //
	// // return true;
	// //
	// // }
	//
	// void read(long pos, byte[] outbuf) throws Exception {
	//
	// // if (pathName == null || pathName.trim().length() == 0)
	// // throw new UserException("pathName is empty");
	//
	// //
	// //
	// RandomAccessFile handle = new RandomAccessFile(pathName, "r");
	//
	// // long len = handle.length();
	//
	// // if (pos < len)
	// handle.seek(pos);
	// handle.read(outbuf);
	//
	// if (pos == crc_last_pos) {
	// crc_last_pos += outbuf.length;
	// crc.update(outbuf);
	// }
	//
	// handle.close();
	//
	// }
	//
	// boolean write(long pos, byte[] payload) throws Exception {
	//
	// if (writeMode == false)
	// throw new UserException("open without write mode");
	// // if (tmpName == null || tmpName.trim().length() == 0)
	// // throw new UserException("tmpName is empty");
	//
	// RandomAccessFile handle = new RandomAccessFile(pathName, "rw");
	//
	// long len = handle.length();
	//
	// // if (pos < len)
	// // return true;// already wrote?
	//
	// if (pos != len)
	// return false;
	//
	// handle.seek(len);
	//
	// handle.write(payload);
	// handle.close();
	//
	// if (pos == len)
	// crc.update(payload);
	//
	// return true;
	//
	// }
	//
	// long length() throws Exception {
	//
	// // if (tmpName == null || tmpName.trim().length() == 0)
	// // throw new UserException("tmpName is empty");
	//
	// return new File(pathName).length();
	//
	// }
	//
	// public void close(FilePack pk, String saveName) throws Exception {
	//
	// if (pathName == null)
	// return;
	//
	// if (writeMode == false)
	// return;
	//
	// saveName = saveName.replace("/", File.separator).replace("\\", File.separator);
	// // if (pathName == null || pathName.trim().length() == 0)
	// // throw new UserException("tmpName is empty");
	//
	// // RandomAccessFile handle = new RandomAccessFile(pathName, "rw");
	//
	// // long wlen = handle.length();
	//
	// // handle.close();
	//
	// long wlen = new File(pathName).length();
	//
	// System.gc();
	//
	// System.out.format("%x %x \r\n", crc.getValue(), pk.crc);
	//
	// if (crc.getValue() != pk.crc || wlen != pk.len) {
	// // if (wlen != pk.len) {
	//
	// System.out.println("crc error!");
	//
	// new File(pathName).delete();// err
	//
	// } else {
	//
	// File f = new File(saveName);
	//
	// if (f.isFile())
	// f.delete();
	//
	// File parent = new File(Paths.get(saveName).getParent().toString());
	// if (parent.exists() == false) {
	// parent.mkdirs();
	// }
	//
	// // System.out.println("new File(pathName).exists=" + new File(pathName).exists());
	// // System.out.println("exists=" + f.exists());
	//
	// new File(pathName).setLastModified(pk.lastModified);
	// // new File(pathName).renameTo(f);
	// // try {
	// Path oldfile = Paths.get(pathName);
	// Path newfile = Paths.get(saveName);
	// Files.move(oldfile, newfile);
	//
	// pathName = null;// finish
	//
	// // } catch (IOException e) {
	// // e.printStackTrace();
	// // }
	//
	// // System.out.println("exists=" + f.exists());
	//
	// }
	//
	// }
	//
	// public boolean exists() {
	//
	// return new File(pathName).exists();
	// }
	//
	// public long lastModified() {
	//
	// return new File(pathName).lastModified();
	//
	// }
	//
	// }
	//
	// /**
	// *
	// * @param basedir
	// * @param dir
	// * @return
	// * @throws Exception
	// */
	// public static String[] getFiles(String basedir, String dir) throws Exception {
	//
	// basedir = basedir.replace("/", File.separator).replace("\\", File.separator);
	// dir = dir.replace("/", File.separator).replace("\\", File.separator);
	//
	// String s = "";
	// ArrayList<String> ar = new ArrayList<String>();
	//
	// if (new File(dir).isFile()) {
	// ar.add(dir.substring(basedir.length()));
	// } else {
	//
	// // s = dir.substring(basedir.length());
	// // if (s.trim().length() > 0) {
	// // ar.add(s );
	// // }
	// ar.add(dir);
	//
	// long bl = getFiles_sub(dir, ar, true);
	//
	// // remove 'dir' name
	// for (int i = 0; i < ar.size(); i++) {
	// System.out.format(" (%s) , (%s) \r\n", ar.get(i), basedir);
	//
	// s = ar.get(i).substring(basedir.length());
	// ar.set(i, s);
	// }
	// }
	//
	// return ar.toArray(new String[0]);
	//
	// }
	//
	// /**
	// *
	// * @param dir
	// * @param arr
	// * @param includeSub
	// * @return
	// */
	// private static long getFiles_sub(String dir, ArrayList<String> arr, boolean includeSub) {
	//
	// long byteLength = 0;
	// File[] l = new File(dir).listFiles(new FilenameFilter() {
	// @Override
	// public boolean accept(File dir, String name) {
	// return true;// name.toLowerCase().endsWith(".zip");
	// }
	//
	// });
	//
	// for (File f : l) {
	//
	// if (f.isFile()) {
	//
	// arr.add(f.getAbsolutePath());
	//
	// byteLength += f.length();
	//
	// } else if (f.isDirectory()) {
	//
	// arr.add(f.getAbsolutePath() + "\\");
	//
	// if (includeSub) {
	// byteLength += getFiles_sub(f.getAbsolutePath(), arr, true);
	// }
	// }
	// }
	// return byteLength;
	//
	// }
	//
	// static Map<String, FileStat> handles = new HashMap<String, FileStat>();
	//
	// public static void test2_old() throws Exception {
	//
	// Gson gson = new Gson();
	//
	// System.out.println("mqtt test");
	// //
	// MqttBroker3 brk = new MqttBroker3(1883);
	// brk.begin();
	//
	// String srcServerName = "src";
	//
	// String destServerName = "dest";
	//
	// String srcPath = "C:\\Temp\\src\\";
	//
	// String srcFile = "sub1\\lombok.jar";
	//
	// String destPath = "C:\\Temp\\dest\\";
	//
	// String srcTopic = srcServerName + "/fs";
	//
	// String destTopic = destServerName + "/fs";
	//
	// //
	// //
	// if (srcPath.endsWith(File.separator) == false)
	// return;// err
	//
	// if (destPath.endsWith(File.separator) == false)
	// return;// err
	//
	// if (new File(srcPath).isDirectory() == false)
	// return;// err
	//
	// if (new File(destPath).isDirectory() == false)
	// return;// err
	//
	// if (new File(srcPath + srcFile).isFile() == false)
	// return; // err
	//
	// //
	// //
	// int qos = 2;
	//
	// MqttClient3 mysub1 = new MqttClient3();
	//
	// mysub1.setId("sub1" + System.nanoTime());
	// mysub1.connect("127.0.0.1", 1883, new IMqtt3() {
	//
	// @Override
	// public void onConnected(MqttClient3 source) {
	//
	// try {
	// long pi = source.subscribe(srcTopic + "/#", qos, new IMqttSub3() {
	//
	// @Override
	// public void onReceived(MqttClient3 source, String topic, byte[] payload) {
	//
	// System.out.println(topic);
	// try {
	// // (server name)/(uuid)/(open/read/write/close , event)
	//
	// String t[] = topic.split("/");
	//
	// if (t.length != 3)
	// return;// error
	//
	// String type = t[1];
	// String func = t[2];
	//
	// FilePack pk = gson.fromJson(new String(payload), FilePack.class);
	//
	// //
	// // act
	//
	// if (func.equals("list")) {
	//
	// pk.resp = "dir";
	//
	// String[] files = getFiles(destPath, pk.pathName);
	//
	// pk.files = files;
	//
	// // pk.tmpName = tmpName;
	//
	// source.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);
	//
	// } else if (func.equals("open")) {
	//
	// // String fn = pk.pathName;
	//
	// if (handles.containsKey(pk.key) == true)
	// return;// err
	//
	// // String pathName = destPath + fn;
	//
	// // pathName = pathName.replace("/", File.separator).replace("\\", File.separator);
	//
	// // String tmpName = destPath + System.nanoTime();
	//
	// // if (new File(filepath).exists())
	// // return;// err
	//
	// FileStat fs = new FileStat(destPath + System.nanoTime(), true);
	// // fs.open();
	//
	// pk.resp = "open";
	// pk.pathName = fs.pathName;
	// // pk.tmpName = tmpName;
	//
	// source.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);
	//
	// handles.put(pk.key, fs);
	//
	// }
	//
	// else if (func.equals("write")) {
	//
	// FileStat fs = handles.get(pk.key);
	//
	// if (fs == null)
	// return;// err
	//
	// if (fs.write(pk.pos, pk.data)) {
	//
	// pk.pos = fs.length();
	// pk.resp = "wrote";
	//
	// source.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);
	//
	// System.out.println("wrote " + pk.pos + " " + pk.len);
	//
	// } else {
	// System.out.println("missing pos=" + pk.pos);
	//
	// pk.pos = fs.length();
	// pk.resp = "missing";
	//
	// source.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);
	//
	// }
	//
	// }
	//
	// else if (func.equals("close")) {
	//
	// FileStat fs = handles.get(pk.key);
	//
	// if (fs == null)
	// return;// err
	//
	// long len = fs.length();
	//
	// fs.close(pk, destPath + pk.pathName);
	//
	// //
	// // after renameTo
	//
	// pk.len = len;
	// pk.crc = fs.crc.getValue();
	// pk.resp = "closed";
	// source.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);
	//
	// handles.remove(pk.key);// **remove handle
	//
	// } else if (func.equals("get")) {
	//
	// // File src = new File(destPath + srcFile);
	//
	// pk.resp = "get";
	//
	// // String fn = pk.pathName;
	//
	// if (handles.containsKey(pk.key) == true)
	// return;// err
	//
	// FileStat fs = new FileStat(destPath + pk.pathName, false);
	// handles.put(pk.key, fs);
	//
	// if (fs.exists()) {
	// pk.len = fs.length();
	// pk.lastModified = fs.lastModified();
	// source.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);
	// } else {
	// pk.len = 0;
	// pk.lastModified = 0;
	// source.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);
	// }
	//
	// } else if (func.equals("read")) {
	//
	// // System.out.println("read pos=" + pk.pos);
	//
	// FileStat fs = handles.get(pk.key);
	//
	// if (fs == null)
	// return;// err
	//
	// // File src = new File(destPath + srcFile);
	//
	// pk.resp = "read";
	//
	// // if (fs.exists()) {
	//
	// // RandomAccessFile handle = new RandomAccessFile(src, "r");
	//
	// long len = fs.length();
	//
	// // if (pk.pos >= len) {
	// // System.out.println("read err " + pk.pos + " " + len);
	// // return;
	// // }
	// //
	// // handle.seek(pk.pos);
	//
	// long size = Math.min(pk.packSize, len - pk.pos);
	//
	// byte buf[] = new byte[(int) size];
	// fs.read(pk.pos, buf);
	// // handle.close();
	//
	// pk.len = len;
	// pk.data = buf;
	// pk.crc = fs.crc.getValue();
	//
	// source.publish(pk.from + "/resp", gson.toJson(pk).getBytes(), 0);
	//
	// // }
	//
	// }
	//
	// } catch (Exception e) {
	//
	// e.printStackTrace();
	// }
	//
	// }
	//
	// @Override
	// public void subscribeSuccess(MqttClient3 source, String topic) {
	//
	// System.out.println("subscribed " + source.getId() + " " + topic);
	//
	// }
	//
	// @Override
	// public void subscribeFailed(MqttClient3 source, String topic) {
	//
	// source.discon(false);
	//
	// }
	//
	// });
	//
	// System.out.println("client subscribed ");
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// }
	//
	// @Override
	// public void deliveryComplete(long pi, long past_ms) {
	//
	// }
	//
	// @Override
	// public void onDisonnected(MqttClient3 source) {
	// System.out.println("sub ");
	//
	// }
	//
	// });
	//
	// //
	// //
	// //
	//
	// ArrayList<FilePack> response = new ArrayList<FilePack>();
	//
	// MqttClient3 mysub2 = new MqttClient3();
	//
	// mysub2.setId("sub1" + System.nanoTime());
	// mysub2.connect("127.0.0.1", 1883, new IMqtt3() {
	//
	// @Override
	// public void onConnected(MqttClient3 source) {
	//
	// try {
	// long pi = source.subscribe(destTopic + "/#", qos, new IMqttSub3() {
	//
	// @Override
	// public void onReceived(MqttClient3 source, String topic, byte[] payload) {
	//
	// System.out.println(topic);
	// try {
	// // (server name)/(uuid)/(open/read/write/close , event)
	//
	// String t[] = topic.split("/");
	//
	// if (t.length != 3)
	// return;// error
	//
	// String type = t[1];
	// String func = t[2];
	//
	// if (func.equals("resp")) {
	//
	// FilePack pk = gson.fromJson(new String(payload), FilePack.class);
	//
	// response.add(pk);
	// }
	//
	// } catch (Exception e) {
	//
	// e.printStackTrace();
	// }
	//
	// }
	//
	// @Override
	// public void subscribeSuccess(MqttClient3 source, String topic) {
	//
	// System.out.println("subscribed " + source.getId() + " " + topic);
	//
	// }
	//
	// @Override
	// public void subscribeFailed(MqttClient3 source, String topic) {
	//
	// source.discon(false);
	//
	// }
	//
	// });
	//
	// System.out.println("client subscribed ");
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// }
	//
	// @Override
	// public void deliveryComplete(long pi, long past_ms) {
	//
	// }
	//
	// @Override
	// public void onDisonnected(MqttClient3 source) {
	// System.out.println("sub ");
	//
	// }
	//
	// });
	//
	// TimeUtil.sleep(2000);
	//
	// // upload
	// {
	// // CRC32 crc = new CRC32();
	//
	// FilePack pk = new FilePack(srcFile);
	// // File src = new File(srcPath + srcFile);
	// FileStat fs = new FileStat(srcPath + srcFile, false);
	//
	// pk.len = fs.length();
	// pk.lastModified = fs.lastModified();
	// pk.from = destTopic;
	//
	// FilePack info = gson.fromJson(gson.toJson(pk), FilePack.class);// clone
	//
	// mysub2.publish(srcTopic + "/open", gson.toJson(pk).getBytes(), qos);
	//
	// // RandomAccessFile fs = new RandomAccessFile(src, "r");
	//
	// // long len = src.length();
	//
	// // long pack = 1024;
	//
	// long pos = 0;
	// long packetSize = 1024 * 8;
	//
	// TimeUtil to = new TimeUtil();
	//
	// int step = 1;
	//
	// // for (long p = 0; p < len; p += pack) {
	// while (to.end_sec() < 6) {
	//
	// if (step == 1) {
	//
	// }
	//
	// else if (step == 2) {
	// long readn = Math.min(packetSize, info.len - pos);
	//
	// byte buf[] = new byte[(int) readn];
	//
	// fs.read(pos, buf);
	//
	// pk.data = buf;
	// pk.pos = pos;
	//
	// mysub2.publish(srcTopic + "/write", gson.toJson(pk).getBytes(), qos);
	//
	// if ((pos + packetSize) < info.len) {
	// pos += packetSize;
	// }
	//
	// }
	//
	// if (response.size() > 0) {
	// FilePack resp = response.removeFirst();
	//
	// if (resp != null) {
	//
	// if (resp.resp.equals("open")) {
	// to.start();//
	//
	// if (resp.len == 0) {
	// throw new UserException("file access error");
	// }
	//
	// step = 2;
	// info = resp;
	//
	// } else if (resp.resp.equals("missing")) {
	// to.start();// reflesh
	//
	// pos = resp.pos;
	// } else if (resp.resp.equals("wrote")) {
	//
	// to.start();// reflesh
	//
	// if (resp.pos == info.len) {
	//
	// pk.crc = fs.crc.getValue();
	//
	// mysub2.publish(srcTopic + "/close", gson.toJson(pk).getBytes(), qos);
	// }
	// } else if (resp.resp.equals("closed")) {
	//
	// System.out.println("upload finished");
	//
	// break;
	// }
	// }
	// }
	//
	// TimeUtil.sleep(10);
	// } // for
	// //
	// }
	//
	// // download
	// {
	//
	// TimeUtil to = new TimeUtil();
	// // CRC32 crc = new CRC32();
	//
	// String fn = "test\\lombok.jar";
	//
	// FilePack pk = new FilePack(fn);
	// pk.from = destTopic;
	// pk.idx = System.nanoTime();
	//
	// FileStat fs = new FileStat(srcPath + System.nanoTime(), true);
	// FilePack info = null;
	//
	// long packetSize = 1024 * 8;
	//
	// // wait
	// int step = 0;
	//
	// mysub2.publish(srcTopic + "/get", gson.toJson(pk).getBytes(), qos);
	//
	// int pos = 0;
	//
	// step = 1;
	// while (to.end_sec() < 6) {
	//
	// if (step == 2) {
	//
	// pk.pos = pos;
	// pk.packSize = Math.min(packetSize, info.len - packetSize);
	// mysub2.publish(srcTopic + "/read", gson.toJson(pk).getBytes(), qos);
	//
	// if ((pos + packetSize) < info.len) {
	// pos += packetSize;
	// }
	//
	// }
	//
	// if (response.size() > 0) {
	// FilePack resp = response.removeFirst();
	//
	// if (resp != null) {
	// if (resp.resp.equals("get") && step == 1) {
	// step = 2;
	// // fs.open();
	// info = resp;
	//
	// to.start();// reflesh
	//
	// } else if (resp.resp.equals("read") && step == 2) {
	//
	// System.out.println("read.resp " + resp.pos);
	//
	// if (fs.write(resp.pos, resp.data)) {
	//
	// System.out.println("download pos=" + resp.pos + " " + info.len + " " + fs.length());
	//
	// } else {
	// pk.pos = fs.length();
	// }
	//
	// if (fs.length() == info.len) {
	//
	// mysub2.publish(srcTopic + "/close", gson.toJson(pk).getBytes(), qos);
	// }
	//
	// to.start();// reset
	//
	// } else if (resp.resp.equals("closed") && step == 2) {
	//
	// fs.close(resp, srcPath + fn);
	// // System.out.println("how finish?");
	// break;
	//
	// }
	// }
	// }
	//
	// TimeUtil.sleep(10);
	// }
	//
	// //
	// }
	//
	// }

	public static void test2() throws Exception {

		Gson gson = new Gson();

		System.out.println("mqtt test");
		//
		MqttBroker3 brk = new MqttBroker3(1883);
		brk.begin();

		String srcServerName = "src";

		//String destServerName = "dest";

		String srcPath = "C:\\Temp\\src\\";

		String srcFile = "sub1\\lombok.jar";
		//String destFile = "sub1\\lombok2.jar";

		//String destPath = "C:\\Temp\\dest\\";

		// String srcTopic = srcServerName + "/fs";

		// String destTopic = destServerName + "/fs";

		//
		//
		if (srcPath.endsWith(File.separator) == false)
			return;// err

//		if (destPath.endsWith(File.separator) == false)
//			return;// err

		if (new File(srcPath).isDirectory() == false)
			return;// err

//		if (new File(destPath).isDirectory() == false)
//			return;// err

		if (new File(srcPath + srcFile).isFile() == false)
			return; // err

		FileHandler listener = new FileHandler(srcServerName, "127.0.0.1", 1883);
		listener.setBasePath(srcPath);

		FileHandler cli = new FileHandler( "127.0.0.1", 1883);
		cli.setBasePath("C:\\Temp\\dest\\");
		//dest.setBasePath(destPath);

		// src.upload(destServerName, srcFile);
		// src.download(destServerName, destFile);

		System.out.println("basepath=" + cli.basedir(srcServerName));

		FilePack pk = cli.dir(srcServerName, "test");
		String[] rs = pk.files;

		System.out.println("total bytes=" + StringUtil.formatBytesSize(pk.len));

		for (String s : rs) {

			boolean isfile = cli.isfile(srcServerName, s);

			long len = 0;

			if (isfile) {
				len = cli.length(srcServerName, s);
				
				cli.download(srcServerName,  s);
			}

			System.out.println(s + " " + isfile + " " + StringUtil.formatBytesSize(len));
		} // for

		listener.close();

		cli.close();

	}

	public static void test(String[] args) {

		try {
			new MqttTest4().test2();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
