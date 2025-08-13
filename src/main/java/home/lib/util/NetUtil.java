package home.lib.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetUtil {

	////////////////////////////////////////////////////////
	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * static members
	 * 
	 * 
	 */

	/**
	 * 
	 * 
	 * @return
	 */
	public static String[] getLocalIps() throws Exception {

		String s = "";
		;
		ArrayList<String> ips = new ArrayList<String>();

		Enumeration e = NetworkInterface.getNetworkInterfaces();
		while (e.hasMoreElements()) {
			NetworkInterface n = (NetworkInterface) e.nextElement();
			Enumeration ee = n.getInetAddresses();
			while (ee.hasMoreElements()) {
				InetAddress i = (InetAddress) ee.nextElement();
				// System.out.println(i.getHostAddress());

				if (i.getHostAddress().split("\\.").length == 4) {
					// if (i.getHostAddress().split("\\.")[0].trim().equals("127") == false) {
					s += i.getHostAddress() + " / ";
					// }

					ips.add(i.getHostAddress());
				}
			}
		} // while

		return ips.toArray(new String[0]);
	}

	// public static boolean m_fromExtention = false;
	// public static FrmMain m_frame = null;

	public static Map<String, InetAddress> getNetworkInfo() throws SocketException {

		Map<String, InetAddress> rs = new HashMap<String, InetAddress>();

		Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
		for (NetworkInterface netint : Collections.list(nets)) {
			displayInterfaceInformation(netint, rs);
		}

		return rs;
	}

	/**
	 * 
	 * @param netint
	 * @param rs
	 * @throws SocketException
	 */
	private static void displayInterfaceInformation(NetworkInterface netint, Map<String, InetAddress> rs)
			throws SocketException {

		Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
		for (InetAddress inetAddress : Collections.list(inetAddresses)) {

			String ip = inetAddress.getHostAddress();
			if (isIpStr(ip)) {
				// System.out.printf("Display name: %s\n", netint.getDisplayName());
				// System.out.printf("Name: %s\n", netint.getName());
				// System.out.printf("InetAddress: %s\n", ip);
				// System.out.printf("\n");

				rs.put(netint.getName(), inetAddress);
			}

		}

	}

	/**
	 * 
	 * InetAddress ipAddress = InetAddress.getLocalHost(); getMacAddress(ipAddress);
	 * 
	 * @param ipAddress
	 * @return
	 * @throws UnknownHostException
	 * @throws SocketException
	 */
	public static String getMacAddress(InetAddress ipAddress) throws UnknownHostException, SocketException {
		// InetAddress ipAddress = InetAddress.getLocalHost();
		NetworkInterface networkInterface = NetworkInterface.getByInetAddress(ipAddress);
		byte[] macAddressBytes = networkInterface.getHardwareAddress();
		StringBuilder macAddressBuilder = new StringBuilder();

		for (int macAddressByteIndex = 0; macAddressByteIndex < macAddressBytes.length; macAddressByteIndex++) {
			String macAddressHexByte = String.format("%02X", macAddressBytes[macAddressByteIndex]);
			macAddressBuilder.append(macAddressHexByte);

			if (macAddressByteIndex != macAddressBytes.length - 1) {
				macAddressBuilder.append(":");
			}
		}

		return macAddressBuilder.toString();
	}

	/**
	 * 
	 * 127.0.0.1:1234
	 * 
	 * @param addr
	 * @return
	 */
	public static boolean isValidAddress(String addr) {

		if (addr.split(":").length == 2) {

			String ip = addr.split(":")[0];
			String port = addr.split(":")[1];
			// port format check
			int p = -1;
			try {
				p = Integer.valueOf(port);
			} catch (Exception e) {
				return false;
			}

			if (0 > p || p > 65535)
				return false;

			// ip format check
			return (isValidIp(ip) || isValidDomain(ip));
		}

		return false;

	}

	/**
	 * 
	 * @param addr
	 * @return
	 * @throws Exception
	 */
	public static String getIpFromAddress(String addr) throws Exception {

		String ip = addr.split(":")[0];
		// String port = addr.split(":")[1];

		return ip;
	}

	/**
	 * 
	 * @param addr
	 * @return
	 * @throws Exception
	 */
	public static int getPortFromAddress(String addr) throws Exception {

		// String ip = addr.split(":")[0];
		int port = Integer.valueOf( addr.split(":")[1] );

		return port;
	}

	/**
	 * 
	 * @param ip
	 * @return
	 */
	public static boolean isValidIp(String ip) {
		String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

		return ip.matches(PATTERN);
	}

	@Deprecated
	public static boolean ipAddrValidate(String ip) {
		return isValidIp(ip);
	}

	@Deprecated
	public static boolean isIpStr(String ip) {

		return ipAddrValidate(ip);
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	public static int findRandomOpenPortOnAllLocalInterfaces() throws Exception {

		return getFreePort();

	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public static int getFreePort() throws Exception {

		ServerSocket socket = new ServerSocket(0);

		int res = socket.getLocalPort();

		socket.close();

		return res;

	}

	/**
	 * 
	 * @param p
	 * @return
	 */
	public static boolean isAvailablePort(int p) {

		try {
			ServerSocket socket = new ServerSocket(p);
			socket.close();
			return true;
		} catch (Exception e) {

		}

		return false;

	}

	/**
	 * Function to validate domain name.
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isValidDomain(String str) {
		// Regex to check valid domain name.
		String regex = "^((?!-)[A-Za-z0-9-]" + "{1,63}(?<!-)\\.)" + "+[A-Za-z]{2,6}";

		// Compile the ReGex
		Pattern p = Pattern.compile(regex);

		// If the string is empty
		// return false
		if (str == null) {
			return false;
		}

		// Pattern class contains matcher()
		// method to find the matching
		// between the given string and
		// regular expression.
		Matcher m = p.matcher(str);

		// Return if the string
		// matched the ReGex
		return m.matches();
	}

	/**
	 * 
	 * @param s
	 * @return
	 */
	public static String getIpFromDomain(String s) {
		try {
			InetAddress giriAddress = java.net.InetAddress.getByName(s);

			String address = giriAddress.getHostAddress();

			return address;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return s;
	}

}