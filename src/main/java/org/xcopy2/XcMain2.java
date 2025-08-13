package org.xcopy2;

import java.awt.Image;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

public class XcMain2 {

//	public static void main(String[] args) {
//
//		// System.out.println("jxcopy version 1.908-29");
//		//System.out.println("jxcopy version 0.0103-1");// 20200902
//		System.out.println("jxcopy version 0.0203-1");// 20240322
//
//		// try {
//		// UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
//		// } catch (Exception e1) {
//		// // TODO Auto-generated catch block
//		// e1.printStackTrace();
//		// }
//
//		start(args);
//	}

	public static void start(String[] args) {

		for (String a : args) {
			System.out.format("\"%s\" ", a);
		}
		System.out.println("");

		// "javaw.exe" -jar "c:\dll\jxcopy.jar" -c -over -src 127.0.0.1 "c:\tmp" -bdir "C:\_gdrive\lkdev74share1" -dest
		// 127.0.0.1 "c:\tmp"

		// "C:\Program Files (x86)\Java\jdk1.8.0_181\bin\javaw.exe" -jar "c:\dll\jxcopy.jar" -c -over -src 127.0.0.1
		// "%1" -bdir "C:\_gdrive\lkdev74share1" -dest 127.0.0.1 "%1"

		// "C:\Program Files (x86)\Java\jdk1.8.0_181\bin\javaw.exe" -jar "c:\dll\jxcopy.jar" -c -over -src 127.0.0.1
		// "%1" -bdir "C:\_gdrive\lkdev74share1" -dest 127.0.0.1 "%1"

		// format check
		String srcIp = getIp()[0];// "127.0.0.1";
		String src = "";
		String destIp = getIp()[0];// "127.0.0.1";
		String dest = "";

		// String destIp2 = getIp()[0];// "127.0.0.1";
		// String dest2 = "";

		String baseDirectory = "";
		// String baseDirectory2 = "";

		//
		// get parameter
		//
		boolean overwrite = false;
		boolean serverMode = false;
		boolean serverGuiMode = false;
		boolean ignoreErrors = false;
		String serverIp = null;
		int port = 8475;
		boolean mirroring = false;

		boolean ui = true;

		String extFilter = "";

		int p = 0;
		while (p < args.length) {

			if (args[p].equals("-p") && p < args.length) {

				port = Integer.valueOf(args[p + 1].trim());
				p++;

			} else

			if (args[p].equals("-over") && p < args.length) {

				overwrite = true;

			} else if (args[p].equals("-s") && p + 1 < args.length) {

				serverMode = true;
				serverIp = args[p + 1];
				p++;

				// } else if (args[p].equals("-sip") && p + 1 < args.length) {
				//
				//
				// serverIp = args[p + 1];
				// p++;

			}

			else if (args[p].equals("-sg") && p + 1 < args.length) {// server gui mode

				serverGuiMode = true;
				serverIp = args[p + 1];
				p++;

			} else if (args[p].equals("-mirroring") && p < args.length) {

				mirroring = true;

			}

			else if (args[p].equals("-c") && p < args.length) {

				// /C Continues copying even if errors occur.
				ignoreErrors = true;
			} else if (args[p].equals("-bdir") && p + 1 < args.length) {

				//
				baseDirectory = args[p + 1];
				p++;
			}

			// else if (args[p].equals("-bdir2") && p + 1 < args.length) {
			//
			// //
			// baseDirectory2 = args[p + 1];
			// p++;
			// }
			else if (args[p].equals("-ef") && p + 1 < args.length) {

				extFilter = args[p + 1];
				p++;

			} else if (args[p].equals("-src") && p + 1 < args.length) {

				if (checkIpFormat(args[p + 1])) {
					srcIp = args[p + 1];
					src = args[p + 2];
					p++;
					p++;
				} else {
					src = args[p + 1];
					p++;
				}

			} else if (args[p].equals("-dest") && p + 1 < args.length) {

				// dest1
				if (checkIpFormat(args[p + 1])) {
					destIp = args[p + 1];
					dest = args[p + 2];
					p++;
					p++;
				} else {
					dest = args[p + 1];
					p++;
				}
			} else if (args[p].equals("-text") && p < args.length) {

				ui = false;

			} else if (args[p].equals("-xcmain") && p < args.length) {
				
			}

			//
			// else if (args[p].equals("-dest2") && p + 1 < args.length) {
			//
			// // dest2
			// if (checkIpFormat(args[p + 1])) {
			//
			// destIp2 = args[p + 1];
			// dest2 = args[p + 2];
			// p++;
			// p++;
			//
			// } else {
			//
			// dest2 = args[p++];
			// p++;
			// }
			//
			// }

			else
				break;

			p++;
		} // while

		if (serverGuiMode) {

			new frmServer2(serverIp).setVisible(true);

			return;

		}

		else if (serverMode) {

			// server mode on
			try {

				new IoHandle2(serverIp, port, overwrite);

				System.out.println("Server Mode");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;

		} else if (src.trim().length() != 0 && dest.trim().length() != 0) {

			if (baseDirectory.trim().length() != 0) {
				// dest 1
				dest = dest.replace(':', '_');
				dest = StringUtil.apath(baseDirectory, dest);
			}

			if (src.length() >= 2 && src.substring(0, 2).equals(".\\")) {
				src = src.substring(2) + System.getProperty("user.dir");
			}

			if (src.length() >= 3 && src.substring(0, 3).equals("..\\")) {
				src = new File(src.substring(3) + System.getProperty("user.dir")).getParent();
			}

			IoHandle2 han = null;
			try {
				han = new IoHandle2();

				System.out.println("==========client==========");

				OneFileHandle2 ss = new OneFileHandle2(null, srcIp, port);
				OneFileHandle2 tt = new OneFileHandle2(null, destIp, port);

				ss.setTargetName(srcIp + "[" + port);
				tt.setTargetName(destIp + "[" + port);

				han.m_srcIp = srcIp;
				han.m_destIp = destIp;

				// RmiClient rc = new RmiClient();
				if (ui) {
					frmCopying2 cf = new frmCopying2(han, true);
				}

				int[] n = null;

				if (mirroring) {

					while (true) {
						n = han.mirror(ss, src, tt, dest, ignoreErrors, null, extFilter);
						TimeUtil.sleep(69 * 1000);
					}
				} else
					n = han.XCopy(ss, src, tt, dest, ignoreErrors, null);

				han.addLog(n[1] + " copied and " + n[0] + " errors ");

				if (n[0] != 0)
					System.in.read();// getch();

			} catch (Exception e) {
				han.addLog(e.toString());
			}

		} else {
			System.out.println("format error");

			// "C:\\Program Files (x86)\\Java\\jdk1.8.0_171\\bin\\java.exe" -jar "c:\\dll\\jxcopy.jar" -c -over -src
			// 127.0.0.1 "%1" -bdir "C:\\_gdrive\\lkdev74share1" -dest 127.0.0.1 "%1"
			//
			// "C:\Program Files (x86)\Java\jdk1.8.0_171\bin\java.exe" -jar jxcopy.jar -c -over -src 127.0.0.1 "%1"
			// -bdir "C:\_gdrive\lkdev74share1" -dest 127.0.0.1 "%1"

			// "C:\Program Files (x86)\Java\jre1.8.0_261\bin\java.exe" -jar "c:\dll\jxcopy.jar" -c -over -src 127.0.0.1
			// "%1" -bdir "C:\_gdrive\lkdev74share1" -dest 127.0.0.1 "%1"

			System.out.println(
					"\"-c\" \"-over\" \"-src\" \"127.0.0.1\" \"D:\\Downloads\\apache-activemq-5.15.9-bin\" \"-bdir\" \"d:\\_gdrive\\lkdev74share1\" \"-dest\" \"127.0.0.1\" \"D:\\Downloads\\apache-activemq-5.15.9-bin\" ");

			System.out.println(
					"-c -over -src 127.0.0.1 \"%1\" -bdir \"d:\\_gdrive\\share\" -dest 127.0.0.1 \"%1\" -mirroring -ef \"zip;doc;xlsx;ppt;xls\"");
			System.out.println("-s 127.0.0.1 -over ");
			System.out.println("-sg 127.0.0.1 -over ");
			System.out.println("");
			System.out.println("-over Overwrite an existing destination file.");
			System.out.println("-s    Server start.");
			System.out.println("-c    Continues copying even if errors occur.");
			System.out.println("-bdir base directory of destination.");

			System.exit(0);
		}

		// System.exit(0);
	}

	/**
	 * 
	 * 
	 * 
	 */
	public static String[] getIp() {

		try {

			ArrayList<String> arr = new ArrayList<String>();

			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			while (e.hasMoreElements()) {
				NetworkInterface n = (NetworkInterface) e.nextElement();
				Enumeration<InetAddress> ee = n.getInetAddresses();
				while (ee.hasMoreElements()) {
					InetAddress i = (InetAddress) ee.nextElement();
					if (i.isLinkLocalAddress() == false && i.isLoopbackAddress() == false
							&& i.getHostAddress().indexOf(':') == -1) {
						// System.out.println("==>"+i.getHostAddress());
						// return i.getHostAddress();
						arr.add(i.getHostAddress());
					}

				}
			}
			return arr.toArray(new String[1]);
		} catch (Exception e) {
			return new String[] { "127.0.0.1", };
		}
	}

	/**
	 * 
	 * @param ipAddress
	 * @return true = success && false = fail
	 */
	public static boolean checkIpFormat(String ipAddress) {
		char[] arr = ipAddress.toCharArray();
		int charCount = 0;
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == '.') {
				charCount++;
			}
		}

		if (charCount != 3) {
			// System.out.println("Wrong Number of Octets");
			return false;
		}

		StringTokenizer tok = new StringTokenizer(ipAddress, ".", false);
		int count = 0;
		while (tok.hasMoreTokens()) {
			count++;
			String temp = tok.nextToken().toString();

			try {
				int value = Integer.parseInt(temp);
				if (value < 0 || value > 254) {
					return false;
				}
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}

	public static String getSizeText(long len) {

		String s = "";

		if (len < 1024) {

			s = "" + len + "b";

		} else if (len < (1024 * 1024)) {

			s = "" + (len / 1024) + "k";

		} else {

			s = "" + (len / (1024 * 1024)) + "m";

		}

		return s;
	}

	/**
	 * 
	 * 
	 * 
	 * @param ip
	 * @param port
	 * @param over
	 * @throws Exception
	 */
	public static IoHandle2 server(String id, String ip, int port, boolean over) throws Exception {
		return new IoHandle2(id, ip, port, over);
	}

	/**
	 * 
	 * @param port
	 * @param baseDir
	 * @param srcIp
	 * @param destIp
	 * @param src
	 * @param dest
	 * @param ignoreErrors
	 * @param show
	 * @param cInter
	 * @return
	 * @throws Exception
	 */
	public static int[] copy(String bridgeId, int port, String srcIp, String destIp, String srcDir, String[] src,
			String destDir, boolean ignoreErrors, boolean show, CopyingInterface2 cInter) throws Exception {

		IoHandle2 han = new IoHandle2();
		//
		// if (baseDir.trim().length() != 0) {
		// // dest 1
		// dest = dest.replace(':', '_');
		// dest = StringUtil.apath(baseDir, dest);
		// }

		// OneFileHandle ss = new OneFileHandle(null, srcIp, port);
		// OneFileHandle tt = new OneFileHandle(null, destIp, port);
		OneFileHandle2 ss = new OneFileHandle2(null, bridgeId, port);
		OneFileHandle2 tt = new OneFileHandle2(null, bridgeId, port);

		ss.setTargetName(srcIp);
		tt.setTargetName(destIp);

		han.m_srcIp = srcIp;
		han.m_destIp = destIp;

		// RmiClient rc = new RmiClient();
		if (show) {
			frmCopying2 cf = new frmCopying2(han, false);
		}
		int[] n = han.XCopy2(ss, srcDir, src, tt, destDir, ignoreErrors, cInter);

		return n;

	}

	/**
	 * 
	 * @param path
	 * @param description
	 * @return
	 */
	// Obtain the image URL
	public static Image createImage(String path, String description) {
		URL imageURL = XcMain2.class.getResource(path);

		if (imageURL == null) {
			System.err.println("Resource not found: " + path);
			return null;
		} else {
			return (new ImageIcon(imageURL, description)).getImage().getScaledInstance(16, 16, 0);
		}
	}

	/**
	 * 
	 * @param s
	 * @param w
	 * @param h
	 * @return
	 */
	public static ImageIcon getIcon(String s, int w, int h) {

		try {
			URL imageURL = XcMain2.class.getResource(s);

			return new ImageIcon(ImageIO.read(imageURL).getScaledInstance(w, h, 0));
		} catch (java.lang.IllegalArgumentException e2) {
			e2.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
		return null;
	}

}

/**
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * @author user
 * 
 */
class MyFilenameFilter implements FilenameFilter {

	@Override
	public boolean accept(File dir, String name) {
		return true;// name.toLowerCase().endsWith(".zip");
	}

}