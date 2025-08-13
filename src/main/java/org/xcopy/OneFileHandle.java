package org.xcopy;

import home.lib.lang.UserException;
import home.lib.net.mq.IMqSocketListener;
import home.lib.net.mq.MqBundle;
import home.lib.net.mq2.dev.MqBootstrap;
import home.lib.util.TimeUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import org.xcopy.net.IMyBrokerListener;
import org.xcopy.net.IMySocketListener;
import org.xcopy.net.MyBroker;
import org.xcopy.net.MySocket;

//-src 127.0.0.1 "e:\web\setup.zip" -bdir "C:\_gdrive\lkdev3sw1" -dest 127.0.0.1 "e:\web\setup.zip"

//-s 127.0.0.1 -over

public class OneFileHandle {

	OneFileHandle _this = this;

	public String m_baseDir = "";

	public MySocket m_client = null;

	String m_targetName = "";

	long m_sendTimeout = 32 * 1000;

	boolean m_overwrite = false;

	// Timer m_timer0 = null;

	public FilePacket m_remoteWritingState = new FilePacket();

	// public static MqBootstrap m_bs=new MqBootstrap("static cli bs");
	public MyBroker m_bs = null;
	// public MyBroker m_bs=new MyBroker("static cli bs");

	public OneFileHandle(String id, String svrIp, int svrPort) throws Exception {

		if (id == null) {
			id = "" + System.currentTimeMillis() + "@" + Integer.toHexString(hashCode());
		}

		System.out.format("OneFileHandle=%s \r\n", id);

		m_bs = new MyBroker(0, new cMyBrokerListener());
		m_bs.start(6);

		try {
			m_client = new MySocket(m_bs.getBroker(), id, svrIp, svrPort, new DefaultMqSocketListener());
			// m_client.m_debug=true;
			m_client.setLinkedClassHandle(this);

			m_client.connect(6000, true);

			System.out.println("created link");

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("my name is (" + m_client.getId() + ")");
	}

	public void close() {
		try {
			m_client.close(false);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			m_bs.getBroker().closeAll();
		} catch (Exception e) {
			e.printStackTrace();
		}
		TimeUtil.sleep(10);

		try {
			m_bs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// try {
		// m_timer0.cancel();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

	}

	class cMyBrokerListener implements IMyBrokerListener {

		@Override
		public void addChannel(MyBroker svr, SocketChannel sk) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean addClientId(MyBroker svr, String path, String id, String pwd, SocketChannel sk) {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public void removeChannel(MyBroker svr, SocketChannel sk) {
			// TODO Auto-generated method stub

		}

		@Override
		public MqBundle actionPerformed(MyBroker svr, SocketChannel sk, MqBundle e) {
			// TODO Auto-generated method stub
			return e;
		}

		@Override
		public void log(MyBroker svr, Exception e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void log(MyBroker svr, int level, String s) {
			// TODO Auto-generated method stub

		}

		@Override
		public void startUp(MyBroker svr) {
			// TODO Auto-generated method stub

		}

		@Override
		public void finishUp(MyBroker svr) {
			// TODO Auto-generated method stub

		}

		@Override
		public void returnError(MyBroker mqBroker, Object svr, Object con, UserException e) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean login(MyBroker mqBroker, Object svr, Object nqConnector, String id, String pwd) {
			// TODO Auto-generated method stub
			return false;
		}

	};

	public void setTargetName(String tar) {
		m_targetName = tar;
	}

	public boolean isAlive() {
		return m_client.isAlive();
	}

	long m_callTimeout = 32 * 1000;

	public void setBsaeDir(String s) {
		m_baseDir = s;
	}

	public String getBaseDir() throws Exception {

		return (String) m_client.callMethod(m_targetName, m_callTimeout, "r_getBaseDir");
	}

	public String r_getBaseDir() throws Exception {
		return m_baseDir;
	}

	/**
	 * 
	 * @param dir
	 * @return
	 */
	public String[] getFileList(String dir, boolean includeSub) throws Exception {

		return (String[]) m_client.callMethod(m_targetName, m_callTimeout, "r_getFileList", dir, includeSub);

	}

	/**
	 * 
	 * 
	 * 
	 * @param dir
	 * @return
	 * @throws RemoteException
	 */
	public String[] r_getFileList(String dir, Boolean includeSub) throws Exception {

		//
		dir = dir.replace("/", File.separator).replace("\\", File.separator);

		ArrayList<String> ar = new ArrayList<String>();

		System.out.format("r_getFileList(%s,%s) \r\n", dir, includeSub);

		if (new File(dir).isFile()) {
			ar.add(new File(dir).getName());
		} else {

			long bl = getFiles(dir, ar, includeSub);

			// remove 'dir' name
			for (int i = 0; i < ar.size(); i++) {
				String s = ar.get(i).substring(dir.length());
				ar.set(i, s);
			}
		}

		return ar.toArray(new String[0]);

	}

	//
	//
	//
	public String[] getFileList2(String basedir, String dir) throws Exception {

		return (String[]) m_client.callMethod(m_targetName, m_callTimeout, "r_getFileList2", basedir, dir);

	}

	/**
	 * 
	 * 
	 * 
	 * @param dir
	 * @return
	 * @throws RemoteException
	 */
	public String[] r_getFileList2(String basedir, String dir) throws Exception {

		basedir = basedir.replace("/", File.separator).replace("\\", File.separator);
		dir = dir.replace("/", File.separator).replace("\\", File.separator);

		String s = "";
		ArrayList<String> ar = new ArrayList<String>();

		if (new File(dir).isFile()) {
			ar.add(dir.substring(basedir.length()));
		} else {

			// s = dir.substring(basedir.length());
			// if (s.trim().length() > 0) {
			// ar.add(s );
			// }
			ar.add(dir);

			long bl = getFiles(dir, ar, true);

			// remove 'dir' name
			for (int i = 0; i < ar.size(); i++) {
				System.out.format(" (%s) ,  (%s) \r\n", ar.get(i), basedir);

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
	 * @return
	 */
	private static long getFiles(String dir, ArrayList<String> arr, boolean includeSub) {

		long byteLength = 0;
		File[] l = new File(dir).listFiles(new MyFilenameFilter());
		for (File f : l) {

			if (f.isFile()) {
				arr.add(f.getAbsolutePath());
				byteLength += f.length();
			} else if (f.isDirectory()) {
				arr.add(f.getAbsolutePath() + "\\");

				if (includeSub) {
					byteLength += getFiles(f.getAbsolutePath(), arr, true);
				}
			}
		}
		return byteLength;

	}

	/**
	 * 
	 * @param dir
	 * @return
	 */
	public long getByteLengthOfDir(String dir) throws Exception {

		return (Long) m_client.callMethod(m_targetName, m_callTimeout, "r_getByteLengthOfDir", dir);

	}

	public long r_getByteLengthOfDir(String dir) throws Exception {

		dir = dir.replace("/", File.separator).replace("\\", File.separator);

		if (new File(dir).isFile()) {
			return new File(dir).length();
		}

		ArrayList<String> ar = new ArrayList<String>();

		long bl = getFiles(dir, ar, true);

		return bl;
	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public long getLength(String name) throws Exception {

		return (Long) m_client.callMethod(m_targetName, m_callTimeout, "r_getLength", name);

	}

	public long r_getLength(String name) throws Exception {

		name = name.replace("/", File.separator).replace("\\", File.separator);

		return new File(name).length();

	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public long getlastModified(String name) throws Exception {

		return (Long) m_client.callMethod(m_targetName, m_callTimeout, "r_getlastModified", name);

	}

	public long r_getlastModified(String name) throws Exception {

		name = name.replace("/", File.separator).replace("\\", File.separator);

		return new File(name).lastModified();

	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public boolean isFile(String name) throws Exception {

		return (Boolean) m_client.callMethod(m_targetName, m_callTimeout, "r_isFile", name);

	}

	public boolean r_isFile(String name) throws Exception {

		name = name.replace("/", File.separator).replace("\\", File.separator);

		return new File(name).isFile();

	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public boolean isDirectory(String name) throws Exception {

		return (Boolean) m_client.callMethod(m_targetName, m_callTimeout, "r_isDirectory", name);

	}

	public boolean r_isDirectory(String name) throws Exception {

		name = name.replace("/", File.separator).replace("\\", File.separator);

		return new File(name).isDirectory();
	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public boolean mkdirs(String dir) throws Exception {

		System.out.format("mkdir(%s) \r\n", dir);

		return (Boolean) m_client.callMethod(m_targetName, m_callTimeout, "r_mkdirs", dir);

	}

	public boolean r_mkdirs(String dir) throws Exception {

		dir = dir.replace("/", File.separator).replace("\\", File.separator);

		System.out.format("r_mkdir(%s) \r\n", dir);

		// System.out.println("mkdir : " + dir );
		// if( new File(dir).exists()==false) {
		try {
			return new File(dir).mkdirs();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
		// }
		// return true;
	}

	/**
	 * 
	 * @param fileName
	 * @return
	 */
	public long doChecksum(String fileName) throws Exception {

		TimeUtil t = new TimeUtil();

		// m_callTimeout

		long to = 32 * 1000;
		long r = (Long) m_client.callMethod(m_targetName, to, "r_doChecksum", fileName);

		// System.out.println("return of doCheckSum: " + r + " elapsed time: " + t.end());
		return r;

	}

	public long r_doChecksum(String fileName) throws Exception {
		// try {
		fileName = fileName.replace("/", File.separator).replace("\\", File.separator);

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

	/**
	 * 
	 * @param name
	 * @return
	 */
	public long[] getFileInfo(String name) throws Exception {

		return (long[]) m_client.callMethod(m_targetName, m_callTimeout, "r_getFileInfo", name);

	}

	public long[] r_getFileInfo(String name) throws Exception {

		long[] rs = new long[3];

		rs[0] = r_getlastModified(name);
		rs[1] = r_getLength(name);
		rs[2] = r_doChecksum(name);

		return rs;

	}

	/**
	 * 
	 * @param name
	 * @param mdate
	 * @param length
	 * @param pos
	 * @param data
	 * @param dataLen
	 * @return
	 * @throws RemoteException
	 */
	public long WriteFile(String name, long mdate, long length, long pos, byte[] data, int dataLen) throws Exception {

		return (Long) m_client.callMethod(m_targetName, m_callTimeout, "r_WriteFile", name, mdate, length, pos, data,
				dataLen);

	}

	public long r_WriteFile(String name, long mdate, long length, long pos, byte[] data, int dataLen) throws Exception {

		name = name.replace("/", File.separator).replace("\\", File.separator);

		// System.out.println("Server Wirte File "+name+" "+ pos);
		String newName = name + "_" + mdate + "_" + length;

		if (m_overwrite == false && new File(name).exists() &&
		// window recognized the file name CE75CB-1.DLL with same with
		// CE75CB-1.DLL_1008893700000_36864
				new File(newName).length() != pos) {

			throw new Exception("can't overwrite. exist(" + new File(name).exists() + ") name(" + name + ")len:"
					+ new File(name).length() + " pos:" + pos + " datalen:" + dataLen + " " + new File(name).getParent()
					+ ">>" + new File(name).getName());
			// return -1;//exist & can't overwrite
		}

		File file = new File(newName);

		// delete temporary file
		if (pos == 0 && file.isFile() == true) {
			file.delete();
		}
		// delete target file
		if (pos == 0 && new File(name).isFile() == true) {
			new File(name).delete();
		}

		RandomAccessFile rand = new RandomAccessFile(file, "rwd");

		// if (length > 0) {
		if (rand.length() == pos && length > 0) {

			rand.seek(pos);
			rand.write(data, 0, dataLen);
		}
		rand.close();

		if (file.length() == length || length == 0) {
			file.setLastModified(mdate);
			file.renameTo(new File(name));

			System.out.println("success: " + name + "  " + dataLen);

		}

		return file.length();

	}

	public byte[] ReadData(String name, long mdate, long length, long pos, int packetLen) throws Exception {

		return (byte[]) m_client.callMethod(m_targetName, m_callTimeout, "r_ReadData", name, mdate, length, pos,
				packetLen);

	}

	public byte[] r_ReadData(String name, long mdate, long length, long pos, int packetLen) throws Exception {
		// try {

		name = name.replace("/", File.separator).replace("\\", File.separator);

		// minimum 1k
		if (packetLen < 1024) {
			packetLen = 1024;
		}
		// maximum 1M
		if (packetLen > 1024 * 1204 * 2) {
			packetLen = 1024 * 1204 * 2;
		}

		byte[] rs = new byte[packetLen];//

		File f = new File(name);
		if (f.lastModified() != mdate || f.length() != length)
			throw new RemoteException("is not match about the last modified or length");

		if (f.length() == 0)
			return new byte[0];

		RandomAccessFile rand = new RandomAccessFile(f, "r");

		rand.seek(pos);
		int len = rand.read(rs);
		rand.close();
		// System.out.println("data: " + pos + " " + len);

		if (len == -1)
			return new byte[0];
		// return null;

		return Arrays.copyOf(rs, len);
		// } catch (Exception e) {
		// throw new RemoteException(e.toString());
		// }
	}

}

class DefaultMqSocketListener implements IMySocketListener {

	@Override
	public MqBundle actionPerformed(MySocket ms, MqBundle e) {
		// TODO Auto-generated method stub
		return e;
	}

	@Override
	public void log(MySocket ms, Exception e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void log(MySocket ms, int level, String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connected(MySocket ms) {
		// TODO Auto-generated method stub

	}

	@Override
	public void disconnected(MySocket ms) {
		// TODO Auto-generated method stub

	}

}