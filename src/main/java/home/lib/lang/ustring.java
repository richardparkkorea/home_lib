package home.lib.lang;

import home.lib.util.StringUtil;

@Deprecated 
final public class ustring {

	/**
	 * 
	 */
	private String m_str = null;

	/**
	 * 
	 */
	@Override
	public String toString() {
		return m_str;
	}

	/**
	 * 
	 * @param s
	 */
	public ustring(String s) {
		m_str = s;
	}
	public ustring() {

	}

	/**
	 * 
	 * @param s
	 * @return
	 */
	public ustring apath(String s) {
		
		if( s==null)
			return this;

		if (m_str == null)
			m_str = "";
		

		m_str = StringUtil.apath(m_str, s);

		return this;
	}

}
