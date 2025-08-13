package deprecated.lib.net.mq_old;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

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

	/**
	* 
	*/

	public static boolean isIpStr(String ip) {
		String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

		return ip.matches(PATTERN);
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
	 * @param ip
	 * @return
	 */
	public static boolean ipAddrValidate(String ip) {
		String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

		return ip.matches(PATTERN);
	}
	
	
	
}