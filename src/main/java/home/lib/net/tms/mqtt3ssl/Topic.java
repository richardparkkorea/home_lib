package home.lib.net.tms.mqtt3ssl;

import java.util.Arrays;

class Topic {
	// public:
	private byte[] m_barr = null;
	//
	protected byte qos = 0;

	protected IMqttSub3 callback = null;

	Topic(byte[] s, int len) {
		m_barr = Arrays.copyOf(s, len);
	}

	// Topic(byte[] s) {
	// this(s, s.length);
	// }

	public Topic(String s) {
		this(s.getBytes(), s.getBytes().length);
	}

	byte[] c_str() {
		return m_barr;
	}

	public String str() {
		return new String(m_barr);
	}

	// public static boolean matches(String myStr, String comeStr) {
	//
	// String[] my = myStr.split("/");
	// String[] come = comeStr.split("/");
	//
	// // System.out.format(" str=%s come=%s \r\n", str(), t.str() );
	//
	// for (int n = 0; n < come.length && n < my.length; n++) {
	//
	// if (my[n].equals("#"))
	// return true;
	//
	// if (my[n].equals("+") && come.length == my.length)
	// continue;
	//
	// if (come[n].equals(my[n]) == false)
	// return false;
	//
	// }
	//
	// if (come.length != my.length)
	// return false;
	//
	// // System.out.println("matches!");
	//
	// return true;
	// //
	// // // if (getIndex() == topic.getIndex()) return true;
	// // // if (str() == topic.str()) return true;
	// // if (Arrays.equals(t.m_str, m_str))
	// // return true;
	// //
	// // return false;
	// }

	public boolean matches(Topic t) {

		return matches(t.str());
	}

	/**
	 * 
	 * @param mystr
	 * @param comstr
	 * @return
	 */
	public boolean matches(String comstr) {

		if (str().equals(comstr))
			return true;

		String[] my = str().split("/");
		String[] come = comstr.split("/");

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
	}

	@Override
	public boolean equals(Object o) {

		if (o == null)
			return false;

		if (o instanceof Topic) {
			// return matches((Topic) o);
			// return Arrays.equals(m_str, ((Topic) o).m_str);
			return matches(((Topic) o));
		}

		if (o instanceof String) {
			return matches((String) o);
		}

		return false;
	}
};
