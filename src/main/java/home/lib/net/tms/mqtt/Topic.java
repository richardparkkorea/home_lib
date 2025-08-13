package home.lib.net.tms.mqtt;

import java.util.Arrays;

public class Topic {
	// public:
	byte[] m_str = null;

	Topic(byte[] s, int len) {
		m_str = Arrays.copyOf(s, len);
	}

	// Topic(byte[] s) {
	// this(s, s.length);
	// }

	public Topic(String s) {
		this(s.getBytes(), s.getBytes().length);
	}

	byte[] c_str() {
		return m_str;
	}

	public String str() {
		return new String(m_str);
	}
	
	
	public static boolean matches(String myStr, String comeStr) {

		String[] my = myStr.split("/");
		String[] come = comeStr.split("/");

		// System.out.format(" str=%s come=%s \r\n", str(), t.str() );

		for (int n = 0; n < come.length && n < my.length; n++) {

			if (my[n].equals("#"))
				return true;

			if (my[n].equals("+") && come.length == my.length)
				continue;

			if (come[n].equals(my[n]) == false)
				return false;

		}

		if (come.length != my.length)
			return false;

		// System.out.println("matches!");

		return true;
		//
		// // if (getIndex() == topic.getIndex()) return true;
		// // if (str() == topic.str()) return true;
		// if (Arrays.equals(t.m_str, m_str))
		// return true;
		//
		// return false;
	}

	

	public boolean matches(Topic t) {

		String[] my = str().split("/");
		String[] come = t.str().split("/");

		// System.out.format(" str=%s come=%s \r\n", str(), t.str() );

		for (int n = 0; n < come.length && n < my.length; n++) {

			if (my[n].equals("#"))
				return true;

			if (my[n].equals("+") && come.length == my.length)
				continue;

			if (come[n].equals(my[n]) == false)
				return false;

		}

		if (come.length != my.length)
			return false;

		// System.out.println("matches!");

		return true;
		//
		// // if (getIndex() == topic.getIndex()) return true;
		// // if (str() == topic.str()) return true;
		// if (Arrays.equals(t.m_str, m_str))
		// return true;
		//
		// return false;
	}

	@Override
	public boolean equals(Object o) {

		if (o == null)
			return false;

		if (o instanceof Topic) {
			// return matches((Topic) o);
			return Arrays.equals(m_str, ((Topic) o).m_str);
		}
		return false;
	}
};
