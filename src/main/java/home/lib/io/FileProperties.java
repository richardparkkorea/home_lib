package home.lib.io;

import java.io.*;
import java.util.Enumeration;
import java.util.Properties;

import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

/**
 * 
 * 
 * 
 * 
 * 
 * @author richardpark
 *
 */

public class FileProperties {

	String m_file = null;
	Properties prop = new Properties();
	// Map<String, String> m_arr = new HashMap<String, String>();

	/**
	 * 
	 * 
	 * 
	 * @param f
	 */
	public FileProperties(String f) {
		m_file = f;
		try {
			read();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param k
	 * @return
	 */
	public String getString(String k) {
		try {
			return prop.getProperty(k);
		} catch (Exception e) {

		}
		return null;
	}

	public String get(String k) {
		return getString(k);
	}

	public boolean getB(String k) throws Exception {
		return Boolean.valueOf(get(k));

	}

	public int getI(String k) throws Exception {

		return Integer.valueOf(get(k));
	}

	public long getL(String k) throws Exception {

		return Long.valueOf(get(k));
	}

	public double getD(String k) throws Exception {

		return Double.valueOf(get(k));
	}

	public byte[] getBA(String k) throws Exception {
		return StringUtil.hexToByteArray(get(k));
	}

	/**
	 * 
	 * @param k
	 * @param v
	 */
	public void setString(String k, String v) {

		try {
			prop.setProperty(k, v);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void set(String k, String v) {
		setString(k, v);
	}

	public void set(String k, int v) {
		setString(k, "" + v);
	}

	public void set(String k, long v) {
		setString(k, "" + v);
	}

	public void set(String k, double v) {
		setString(k, "" + v);
	}

	public void set(String k, boolean v) {
		setString(k, "" + v);
	}

	public void set(String k, byte[] b) {
		setString(k, StringUtil.ByteArrayToHex(b));
	}

	/**
	 * 
	 * @return
	 */
	public Properties getHandle() {
		return prop;
	}

	public boolean containKey(String k) {
		return prop.containsKey(k);
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * @return
	 */
	public boolean save() throws Exception {
		// Properties prop = new Properties();
		OutputStream output = null;

		

			output = new FileOutputStream(m_file);

			// // set the properties value
			// prop.setProperty("database", "localhost");
			// prop.setProperty("dbuser", "mkyong");
			// prop.setProperty("dbpassword", "password");

			prop.storeToXML(output, "FileProperties");

			// save properties to project root folder
			// prop.store(output, null);

			return true;
	
	}

	/**
	 * 
	 * 
	 * 
	 */
	public void read() throws Exception {

		InputStream input = null;
 
			input = new FileInputStream(m_file);

			// load a properties file
			// prop.load(input);
			prop.loadFromXML(input);

			Enumeration<?> e = prop.propertyNames();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String value = prop.getProperty(key);
				// System.out.println("Key : " + key + ", Value : " + value);
			}

	

	}

	/**
	 * 
	 * 
	 * @param s
	 */
	public static void debug(String s, Object... args) {

		System.out.println(TimeUtil.now() + " fileproperties [" + String.format(s, args));
	}

}
