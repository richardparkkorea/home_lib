package org.xcopy;

import home.lib.lang.UserException;

import home.lib.net.mq.MqBundle;
import home.lib.util.TimeUtil;

import java.io.File;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.xcopy.net.IMyBrokerListener;
import org.xcopy.net.MyBroker;

/*
 * 


 
 */

public class IoHandle {

	Map<String, Long> m_mirror_files=new HashMap<String,Long>();
	public String m_title="copying";

	
	MyBroker m_server = null;
	public long m_entryPos = 0;// entry copying postion
	public long m_entryLen = 0;// entry file size
	public long m_curPos = 0;// current copying position
	public long m_curLen = 0;// current file size
	public long m_speed = 0;
	public long m_measureSpeed = 0;

	public boolean m_letsStop = false;

	public long m_targetFileCount = 0;
	public long m_currentCopyingIndex = 0;

	public long m_successCount = 0;
	public long m_failCount = 0;

	public TimeUtil m_measureTimer = new TimeUtil();
	public String m_currentFileName = "";

	OneFileHandle m_handle = null;
	//
	//
	public String m_srcIp = "";
	public String m_destIp = "";

	ArrayList<String> m_logs = new ArrayList<String>();

	public IoHandle(String id, int port, boolean overwrite) throws Exception {

		System.out.println(
				"start initialization..." + TimeUtil.now() + "  id=" + id + " port=" + port + " ow=" + overwrite);

		try {

			m_server = new MyBroker(port, new listener());

			// m_server.m_debug = true;

			m_server.start(6);
			m_server.setMaxReceivableSize(1024 * 1000 * 10);

			System.out.println("created queue");

			Thread.sleep(100);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("initialize fail.");
			// System.exit(0);
		}

		System.out.println("end initialization..." + TimeUtil.now());

		Thread.sleep(100);

		m_handle = new OneFileHandle(id + "[" + port, id, port);// id meaning is IP Address.
		m_handle.m_overwrite = overwrite;

		System.out.format(" handle name= %s,  \r\n", id + ":" + port);

		Thread.sleep(100);
		// revoke io handles because the folders and files locked after acceess by this
		// you should must be use the System.gc regurally
		new Thread(closeForOpenIOHandles).start();

		// m_eventServer.start();

	}

	public boolean isAlive() {

		if (m_server != null)
			return m_server.isAlive();

		return false;

	}

	public void close() {

		try {

			m_handle.close();

		} catch (Exception e) {

		}

		TimeUtil.sleep(10);

		try {

			m_server.getBroker().closeAll();

		} catch (Exception e) {

		}
		TimeUtil.sleep(10);

		try {

			m_server.close();

		} catch (Exception e) {

		}

	}

	@Override
	public String toString() {

		if (m_server != null)
			return m_server.toString();

		return "iohandle@" + this.hashCode();
	}

	class listener implements IMyBrokerListener {

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
			return true;
		}

	}

	public IoHandle(String id, String server, int port, boolean overwrite) throws Exception {

		m_handle = new OneFileHandle(id, server, port);// id meaning is IP Address.
		m_handle.m_overwrite = overwrite;

		System.out.format(" handle name= %s \r\n", id);

		Thread.sleep(100);
		// revoke io handles because the folders and files locked after acceess by this
		// you should must be use the System.gc regurally
		new Thread(closeForOpenIOHandles).start();

		// m_eventServer.start();

	}

	public OneFileHandle oh() {
		return m_handle;
	}

	// no server mode
	public IoHandle() throws Exception {

	}

	Runnable closeForOpenIOHandles = new Runnable() {
		public void run() {

			try {
				while (m_handle.isAlive()) {

					System.gc();
					Thread.sleep(1000);

				}
			} catch (Exception e) {
				;
			}
		}
	};

	public void addLog(String s) {
		synchronized (m_logs) {
			m_logs.add(s);
			if (m_logs.size() > 1024 * 5) {
				m_logs.remove(0);
			}

		}
	}

	public int[] XCopy(OneFileHandle rmi_s, String srcName, OneFileHandle rmi_d, String destName, boolean ignoreError,
			CopyingInterface cInter) {
		int copied = 0;

		int successiveError = 0;
		int errCnt = 0;
		//

		srcName = srcName.trim();
		destName = destName.trim();

		try {

			if (rmi_s.isFile(srcName) == true) {

				m_targetFileCount = 1;
				m_currentCopyingIndex = 1;

				m_entryLen = rmi_s.getLength(srcName);

				String baseDir = rmi_d.getBaseDir();

				//
				// single file
				rmi_d.mkdirs(new File(destName).getParent());

				boolean b = false;

				if (ignoreError) {
					try {
						b = WriteFile(rmi_s, srcName, rmi_d, destName, cInter);
					} catch (Exception e) {
						System.out.println(e.toString());
					}
				} else {
					b = WriteFile(rmi_s, srcName, rmi_d, destName, cInter);
				}

				if (b == true) {
					addLog(srcName + " ==> " + destName + " succeed ");
					copied++;
					m_successCount++;

				} else {
					addLog(srcName + " ==> " + destName + " fail ");
					errCnt++;
					m_failCount++;
				}

			} else if (rmi_s.isDirectory(srcName) == true) {
				//
				// copy directory
				rmi_d.mkdirs(destName);
				String[] l = rmi_s.getFileList(srcName, true);

				m_entryLen = rmi_s.getByteLengthOfDir(srcName);

				for (String s : l) {

					int e = s.length();
					if (s.substring(e - 1, e).equals("\\") == false)
						m_targetFileCount++;

				}

				for (String s : l) {

					if (rmi_s.isDirectory(srcName + s)) {

						if (rmi_d.isDirectory(destName + s) == false) {
							rmi_d.mkdirs(destName + s);// make directory
							addLog(destName + s);
						}

					} else if (rmi_s.isFile(srcName + s)) {

						m_currentCopyingIndex++;

						addLog(srcName + s + " ==> " + destName + s);
						boolean b = false;
						if (ignoreError) {
							try {
								b = WriteFile(rmi_s, srcName + s, rmi_d, destName + s, cInter);// copy file
							} catch (Exception e) {
								addLog(e.toString());
							}

						} else {
							b = WriteFile(rmi_s, srcName + s, rmi_d, destName + s, cInter);// copy file
						}

						if (b == true) {
							addLog(" succeed ");
							copied++;
							m_successCount++;

							successiveError = 0;
						} else {
							addLog(" fail");
							errCnt++;
							m_failCount++;
							successiveError++;
						}

					}

				} // for
			} else {
				addLog("unknow source");
				// System.exit(0);
			}

			System.gc();

			return new int[] { errCnt, copied };
		} catch (Exception e) {
			e.printStackTrace();
			// System.exit(0);
		}

		System.gc();
		return new int[] { 0, 0 };

	}

	/**
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static String[] combine(String[] a, String[] b) {
		int length = a.length + b.length;
		String[] result = new String[length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	public int[] XCopy2(OneFileHandle rmi_s, String srcDir, String[] srcList, OneFileHandle rmi_d, String destDir,
			boolean ignoreError, CopyingInterface cInter) {
		int copied = 0;

		ArrayList<String> files = new ArrayList<String>();

		//
		//
		//
		try {
			//
			// collect file names
			for (String src : srcList) {

				m_entryLen += rmi_s.getByteLengthOfDir(src);
				String[] l = rmi_s.getFileList2(srcDir, src);

				for (String s : l) {

					// add folder name
					files.add(s);

					m_targetFileCount++;

				}
				// }
			}

			// Registry registry = null;
			int successiveError = 0;
			int errCnt = 0;
			//

			// srcName = srcName.trim();
			destDir = destDir.trim();

			rmi_d.mkdirs(destDir);

			for (String s : files) {

				String sname = (srcDir + s).replace("\\\\", "\\");
				String dname = (destDir + s).replace("\\\\", "\\");

				System.out.format(" xcopy  src=%s , dest=%s \r\n", sname, dname);

				//

				if (rmi_s.isDirectory(sname)) {

					m_currentCopyingIndex++;

					if (rmi_d.isDirectory(dname) == false) {
						rmi_d.mkdirs(dname);// make directory
						addLog(dname);
					}

					if (rmi_d.isDirectory(dname)) {
						addLog(" succeed ");
						copied++;
						m_successCount++;

						successiveError = 0;
					} else {
						addLog(" fail");
						errCnt++;
						m_failCount++;
						successiveError++;
					}

				} else if (rmi_s.isFile(sname)) {

					m_currentCopyingIndex++;

					rmi_d.mkdirs(new File(dname).getParent());

					addLog(sname + " ==> " + dname);
					boolean b = false;
					if (ignoreError) {
						try {
							b = WriteFile(rmi_s, sname, rmi_d, dname, cInter);// copy file
						} catch (Exception e) {
							addLog(e.toString());
						}

					} else {
						b = WriteFile(rmi_s, sname, rmi_d, dname, cInter);// copy file
					}

					if (b == true) {
						addLog(" succeed ");
						copied++;
						m_successCount++;

						successiveError = 0;
					} else {
						addLog(" fail");
						errCnt++;
						m_failCount++;
						successiveError++;
					}

				}

			} // for

			System.gc();

			return new int[] { errCnt, copied };
		} catch (Exception e) {
			e.printStackTrace();
			// System.exit(0);
		}

		System.gc();
		return new int[] { 0, 0 };

	}

	/**
	 * 
	 * remote -> local
	 * 
	 * 
	 */
	public boolean WriteFile(OneFileHandle sSvr, String src, OneFileHandle dSvr, String dest, CopyingInterface cInter)
			throws Exception {

		long[] sin = sSvr.getFileInfo(src);
		long[] din = dSvr.getFileInfo(dest);

		long pos = 0;
		long mdate = sin[0];// sSvr.getlastModified(src);
		long length = sin[1];// sSvr.getLength(src);

		m_curPos = 0;// 20140205
		m_curLen = length;
		m_currentFileName = src;

		TimeUtil tt = new TimeUtil();

		// if (mdate == dSvr.getlastModified(dest) && length == dSvr.getLength(dest)
		// && sSvr.doChecksum(src) == dSvr.doChecksum(dest)) {

		if (sin[0] == din[0] && sin[1] == din[1] && sin[2] == din[2]) {

			
			m_mirror_files.put( src,  length );
			
			
			m_entryPos += length;
			if (cInter != null)
				cInter.copying(this);
			return true;// exist
		}

		int packetLen = 1 * 1024 * 512;// 512k

		dSvr.m_remoteWritingState.str = dest;
		dSvr.m_remoteWritingState.l = 0;

		long wrote = -1;
		// do {
		// pos = dSvr.m_remoteWritingState.l;
		do {

			long t = System.currentTimeMillis();
			byte[] bs = null;

			bs = sSvr.ReadData(src, mdate, length, pos, packetLen);

			if (bs == null)
				throw new Exception("ReadFile = null");
			//

			dSvr.WriteFile(dest, mdate, length, pos, bs, bs.length);

			pos += bs.length;

			// 20140205
			m_entryPos += bs.length;
			m_curPos += bs.length;

			m_measureSpeed += bs.length;

			if (m_measureTimer.end_sec() > 10.0) {
				m_measureTimer.start();
				m_speed = (m_measureSpeed / 10);
				m_measureSpeed = 0;
			}

			long responseTime = System.currentTimeMillis() - t;
			// response time check
			if (responseTime < 1000) {
				packetLen = (int) (packetLen * 1.2);
			}
			if (responseTime > 1000) {
				packetLen -= (int) (packetLen * 0.8);
			}
			// if (packetLen < 512 * 1) {
			// packetLen = 512 * 1;
			// }

			if (packetLen < (1024 * 64)) {
				packetLen = (1024 * 64);
			}

			if (packetLen > 1024 * 1024 * 2) {
				packetLen = 1024 * 1024 * 2;
			}

			if (cInter != null)
				cInter.copying(this);

			if (m_letsStop) {
				sSvr.close();
				dSvr.close();
				return false;
			}

		} while (pos < length && dSvr.isAlive() && sSvr.isAlive());
		// Thread.sleep(100);

		//
		// if (dSvr.getlastModified(dest) == mdate && dSvr.getLength(dest) == length
		// && sSvr.doChecksum(src) == dSvr.doChecksum(dest))
		// return true;
		// else {
		// // 1566459559000=1566459559175, 122067=122067, 212742198=212742198
		//
		// addLog(String.format("%s=%s, %s=%s, %s=%s", dSvr.getlastModified(dest), mdate, dSvr.getLength(dest), length,
		// dSvr.doChecksum(dest), sSvr.doChecksum(src)));
		//
		// if ((dSvr.getlastModified(dest) / 1000) == (mdate / 1000) && dSvr.getLength(dest) == length
		// && sSvr.doChecksum(src) == dSvr.doChecksum(dest)) {
		// return true;
		// }
		//
		// return false;
		// }
		long s_srv_cs = sSvr.doChecksum(src);

		// long[] src_i=sSvr.getFileInfo(src)
		long[] s_dest_i = dSvr.getFileInfo(dest);
		;// last modify, length , checksum
		
		
		

		if (s_dest_i[0] == mdate && s_dest_i[1] == length && s_srv_cs == s_dest_i[2]) {
		
			
			m_mirror_files.put( src,  length );
			return true;
		}
		else {
			// 1566459559000=1566459559175, 122067=122067, 212742198=212742198

			// addLog(String.format("%s=%s, %s=%s, %s=%s", dSvr.getlastModified(dest), mdate, dSvr.getLength(dest),
			// length,
			// dSvr.doChecksum(dest), sSvr.doChecksum(src)));

			if ((s_dest_i[0] / 1000) == (mdate / 1000) && s_dest_i[1] == length && s_srv_cs == s_dest_i[2]) {
				
				m_mirror_files.put( src,  length );
				return true;
			}

			return false;
		}

	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	public int[] XCopy(OneFileHandle rmi_s, String[] src, OneFileHandle rmi_d, String dest, boolean ignoreError,
			CopyingInterface cInter) {
		int copied = 0;

		// Registry registry = null;
		int successiveError = 0;
		int errCnt = 0;
		//
		m_entryLen = 0;

		// src = src.trim();
		dest = dest.trim();

		try {

			// copy directory
			rmi_d.mkdirs(dest);

			ArrayList<String> fl = new ArrayList<String>();

			for (String sn : src) {

				if (rmi_s.isDirectory(sn) == true) {

					for (String s : rmi_s.getFileList(sn, true)) {
						fl.add(s);
					}
					m_entryLen += rmi_s.getByteLengthOfDir(sn);

				} else if (rmi_s.isFile(sn)) {
					fl.add(sn);
					m_entryLen += rmi_s.getLength(sn);
				}

			}

			for (String s : fl) {

				int e = s.length();
				if (s.substring(e - 1, e).equals("\\") == false)
					m_targetFileCount++;

			}

			for (String s : fl) {

				if (rmi_s.isDirectory(src + s)) {

					if (rmi_d.isDirectory(dest + s) == false) {
						rmi_d.mkdirs(dest + s);// make directory
						addLog(dest + s);
					}

				} else if (rmi_s.isFile(src + s)) {

					m_currentCopyingIndex++;

					addLog(src + s + " ==> " + dest + s);
					boolean b = false;
					if (ignoreError) {
						// try {
						b = WriteFile(rmi_s, src + s, rmi_d, dest + s, cInter);// copy file
						// } catch (Exception e) {
						// addLog(e.toString());
						// }

					} else {
						b = WriteFile(rmi_s, src + s, rmi_d, dest + s, cInter);// copy file
					}

					if (b == true) {
						addLog(" succeed ");
						copied++;
						m_successCount++;

						successiveError = 0;
					} else {
						addLog(" fail");
						errCnt++;
						m_failCount++;
						successiveError++;
					}

				}

			} // for

			System.gc();

			return new int[] { errCnt, copied };
		} catch (Exception e) {
			e.printStackTrace();
			// System.exit(0);
		}

		System.gc();
		return new int[] { 0, 0 };

	}

	public static boolean extFilter(String extFilter, String fileName) {

		if (extFilter == null || extFilter.trim().length() == 0)
			return true;

		for (String ext : extFilter.toLowerCase().split(";")) {

			if (fileName.toLowerCase().endsWith("." + ext))
				return true;

		}

		return false;

	}
	
	
	public static String changeSeparate(String path) {
		
		return path.replace("/", File.separator).replace("\\",File.separator);
	}

	/**
	 * 
	 * @param rmi_s
	 * @param srcName
	 * @param rmi_d
	 * @param destName
	 * @param ignoreError
	 * @param cInter
	 * @return
	 */
	
	
	
	
	public int[] mirror(OneFileHandle rmi_s, String srcName, OneFileHandle rmi_d, String destName, boolean ignoreError,
			CopyingInterface cInter, String extFilter) {
		int copied = 0;

		m_title="mirroring";
		//System.out.println("start mirror");
		
		int errCnt = 0;
		//

		srcName = srcName.trim();
		destName = destName.trim();

		try {

			System.out.println("start mirror-1");
			if (rmi_s.isFile(srcName) == true) {
				//System.out.println("start mirror-1.1");

				addLog("mirroring dose not support single file copying.");
				return new int[] { 0, 0 };

			} else if (rmi_s.isDirectory(srcName) == true) {
				//
				//System.out.format("start mirror-2 (%s) (%s) \r\n",destName,srcName);
				
				// copy directory
				rmi_d.mkdirs(destName);
				String[] l = rmi_s.getFileList(srcName, false);

				// m_entryLen = rmi_s.getByteLengthOfDir(srcName);

				for (String s : l) {

					System.out.println("f=" + s);

					if (rmi_s.isDirectory(srcName + s)) {

						if (rmi_d.isDirectory(destName + s) == false) {
							rmi_d.mkdirs(destName + s);// make directory
							//addLog(destName + s);
						}

						int[] r = mirror(rmi_s, srcName + s, rmi_d, destName + s, ignoreError, cInter, extFilter);// recursive

					} else if (rmi_s.isFile(srcName + s)) {
						
						System.out.format("ext check = %s  %s  %s  \r\n",  extFilter(extFilter, srcName + s), extFilter, srcName+s);

						if (extFilter(extFilter, srcName + s)) {

							m_currentCopyingIndex++;

							addLog(srcName + s + " ==> " + destName + s);
							boolean b = false;
							if (ignoreError) {
								try {
									b = WriteFile(rmi_s, srcName + s, rmi_d, destName + s, cInter);// copy file
								} catch (Exception e) {
									addLog(e.toString());
								}

							} else {
								b = WriteFile(rmi_s, srcName + s, rmi_d, destName + s, cInter);// copy file
							}

							if (b == true) {
 								
								addLog(" succeed ");
								copied++;
								m_successCount++;

							} else {
								addLog(" fail");
								errCnt++;
								m_failCount++;

							}
							
							
							m_targetFileCount = m_mirror_files.size();

							long tl=0;
							for( Long fs: m_mirror_files.values() ) {
								tl+=fs.longValue();
							}
							m_entryPos=tl;
							
							
							
							
							
							
						} // if( extFilter

					}

					TimeUtil.sleep(100);
				} // for
			} else {
				addLog("unknow source");
				// System.exit(0);
			}

			System.gc();

			return new int[] { errCnt, copied };
		} catch (Exception e) {
			e.printStackTrace();
			// System.exit(0);
		}

		System.gc();
		return new int[] { 0, 0 };

	}

	/**
	 * 
	 * 
	 */
	public void init() {
		 
		m_entryPos = 0;// entry copying postion
		//m_entryLen = 0;// entry file size

		m_currentCopyingIndex=0;

		m_successCount = 0;
		m_failCount = 0;
		
		
	}
}
