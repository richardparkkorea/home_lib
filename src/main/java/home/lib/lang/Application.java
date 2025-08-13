package home.lib.lang;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Application {

	final public static String LIB_VER = "1.1";
	// final public static String BUILD_DATE="190927";
	// final public static String BUILD_DATE="190930";//nqselector bugfix
	// final public static String BUILD_DATE="191008";//improve nqconnector ( about auto peek!)
	// final public static String BUILD_DATE="20201208";//including tms & gson
	// final public static String BUILD_DATE="2020.01";//20201209 including tms & gson

	final public static String[] history = {
			"2021.01 TmsBroker.getChannelNames() //the pathname is null before receive it from the client ", // 20201209
			"2021.01 TmsBroker.cleanUp() & checkSelectorBindStat() // add selector auto rebind  ", // 20201210
			"2021.01 remove 'class ULoggerData' // if the jar is encrypted, there's a changce that will encounter problem   ", // 20201223
			"2021.02 TmsSelecotr 			//if (numRead <= 0) {  --->			if (numRead < 0) {     ", // 20210218
			"2021.03 AverageUtil2 bug fix!    ", // 20210306
			"2021.03 Fixed some bugs in TmsBroker", // 20210310 if (get(id) != null && m_selector.isAlive(
													// tc.getChannel() )) {
			"2021.04 Fixed some bugs in TmsSelector / NqSelector", // 20210427 related numRead variable in read() method
			"2021.08 reinforced cleanUp() in the tmsselector, tmsbroker and mybroker",
			"2021.09 adjusted link-timer in TmsChannel.doConnectorLink(); ", // accters timeout and buffer size send to
																				// connector.
			"2021.10 add method setChannelInterface() in TmsBroker ", // 20211020
			"2021.10 add method callMethod, doMethod in TmsChannel ", // 20211020
			"2021.10 TmsChannel is imporved ", // 20211114

			"examples   ", "java -cp jxcopy.jar home.lib.log.ULogger // udp test",
			"java -cp jxcopy.jar home.lib.net.tms.TmsExample // udp test",

	};

	public static void main(String[] args) {

		System.out.println("history print");
		for (String s : history) {
			System.out.println(s);
		}
	}

	/**
	 * add path to java.library.path
	 * 
	 * 
	 * 
	 * @param pathToAdd
	 * @throws Exception
	 */
	public static void addLibraryPath(String pathToAdd) throws Exception {

		try {
			final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
			usrPathsField.setAccessible(true);

			// get array of paths
			final String[] paths = (String[]) usrPathsField.get(null);

			// check if the path to add is already present
			for (String path : paths) {
				if (path.equals(pathToAdd)) {
					return;
				}
			}

			// add the new path
			final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
			newPaths[newPaths.length - 1] = pathToAdd;
			usrPathsField.set(null, newPaths);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param o
	 * @return
	 */
	public static String getAppPath(Object o) {

		//
		String path = o.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		try {
			String decodedPath = URLDecoder.decode(path, "UTF-8");

			// System.out.println("app path:" + new File(decodedPath).getParent());

			return new File(decodedPath).getParent();

		} catch (UnsupportedEncodingException e) {

		}
		return null;

	}

	/**
	 * 
	 * @param fallback
	 * @return
	 */
	public static String getProcessId(final String fallback) {
		// Note: may fail in some JVM implementations
		// therefore fallback has to be provided

		// something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
		final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		final int index = jvmName.indexOf('@');

		if (index < 1) {
			// part before '@' empty (index = 0) / '@' not found (index = -1)
			return fallback;
		}

		try {
			return Long.toString(Long.parseLong(jvmName.substring(0, index)));
		} catch (NumberFormatException e) {
			// ignore
		}
		return fallback;
	}

	/**
	 * 
	 * @return
	 * 
	 *         win , osx, nux
	 * 
	 * 
	 */
	public static String getOsType() {

		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			// Operating system is based on Windows
			return "win";
		} else if (os.contains("osx")) {
			// Operating system is Apple OSX based
			return "osx";
		} else if (os.contains("nix") || os.contains("aix") || os.contains("nux")) {
			// Operating system is based on Linux/Unix/*AIX
			return "nux";
		}

		return os;
	}

	/**
	 * 
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static String getResourceFileAsString(String fileName) throws IOException {
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		try (InputStream is = classLoader.getResourceAsStream(fileName)) {
			if (is == null)
				return null;
			try (InputStreamReader isr = new InputStreamReader(is); BufferedReader reader = new BufferedReader(isr)) {
				return reader.lines().collect(Collectors.joining(System.lineSeparator()));
			}
		}
	}

}
