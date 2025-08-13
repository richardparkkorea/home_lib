package home.lib.io;

import java.io.*;
import java.util.Enumeration;
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

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
public class FileProperties2 {

	String m_prefix = "";
	String m_file = null;
	Properties prop = new Properties();
	// Map<String, String> m_arr = new HashMap<String, String>();

	/**
	 * 
	 * 
	 * 
	 * @param f
	 */
	public FileProperties2(String f, String p) {
		m_file = f;
		m_prefix = p;
		try {
			read();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public boolean containsKey(String k) {
		return prop.containsKey(k);
	}

	/**
	 * 
	 * @param k
	 * @return
	 */
	public String getString(String k) {
		try {
			return prop.getProperty(m_prefix + k);
		} catch (Exception e) {

		}
		return null;
	}

	public String get(String k) {
		return getString(k);
	}

	public boolean getB(String k)  {
		
		try {
		if( get(k).trim().toLowerCase().equals("true"))
			return true;
		}catch(Exception e) {
			
		}
		
		return false;

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
			prop.setProperty(m_prefix + k, v);
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

	// more
	public void set(String k, JComboBox jc) {

		for (int i = 0; i < jc.getItemCount(); i++) {
			set(k + i, "" + jc.getItemAt(i));
		}
		set(k + ".sel", "" + jc.getSelectedItem());
		set(k + ".cnt", "" + jc.getItemCount());
	}

	public void get(String k, JComboBox jc) {

		try {
			int cnt = getI(k + ".cnt");

			for (int i = 0; i < cnt; i++) {
				String s = get(k + i);
				jc.addItem(s);

			}
			jc.setSelectedItem(get(k + ".sel"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void set(String k, JTextField tb) {

		set(k, tb.getText());
	}

	public void get(String k, JTextField tb) {
		tb.setText(get(k));
	}

	
	
	public void set(String k, JCheckBox tb) {

		set(k, tb.isSelected());
	}

	public void get(String k, JCheckBox tb) {
		tb.setSelected( getB(k) );
	}

	
	
	
	/**
	 * 
	 * @return
	 */
	public Properties getHandle() {
		return prop;
	}

	public boolean containKey(String k) {
		return prop.containsKey(m_prefix + k);
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
