package home.lib.util;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * references..<br>
 * http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html
 * 
 * 
 * use ustring
 * 
 * @author not attributable
 * @version 1.0
 * 
 */

final public class StringUtil {
	/**
	 *
	 */
	public StringUtil() {

	}

	/**
	 * 
	 * 
	 * 
	 * @param str
	 * @param sc
	 * @return
	 */
	public static String[] split(String str, String sc) {
		if (str == null || str.trim().length() == 0)
			return new String[0];

		String s = str;
		ArrayList<String> strlist = new ArrayList<String>();
		int count = s.length();
		int i;
		int sp = 0;
		// s = s + sc.charAt(0); //*importent //add one character like null
		// character
		s = s + sc.charAt(0);

		for (i = 0; i < count; i++) {
			char c = s.charAt(i);
			// /Jump
			while ((i < count && sc.indexOf(c) != -1)) {
				i++;
				c = s.charAt(i);
			}
			sp = i; // start pos
			// /Get Length
			while ((i < count && sc.indexOf(c) == -1)) {
				i++;
				c = s.charAt(i);
			}
			if (i != sp) {
				strlist.add(s.substring(sp, i));
				sp = i + 1;
			}
		} // for

		return strlist.toArray(new String[0]);
		/*
		 * int arrCount = strlist.size(); String[] resStrArr = new String[arrCount]; for (i = 0; i < arrCount; i++) {
		 * resStrArr[i] = strlist.get(i).toString(); } return resStrArr;
		 */

	} // ArrayList split( String s, char[] sc)

	@Deprecated
	public static String[] split_noRemoveEmptyEntities(String str, String sc) {
		String s = str;
		ArrayList<String> strlist = new ArrayList<String>();
		int count = s.length();
		int i;
		int sp = 0;
		// s = s + sc.charAt(0); //*importent //add one character like null
		// character
		s = s + sc.charAt(0);

		for (i = 0; i < count; i++) {
			char c = s.charAt(i);
			// /Jump
			if ((i < count && sc.indexOf(c) != -1)) {
				i++;
				c = s.charAt(i);
			}
			sp = i; // start pos
			// /Get Length
			while ((i < count && sc.indexOf(c) == -1)) {
				i++;
				c = s.charAt(i);
			}
			if (i != sp) {
				strlist.add(s.substring(sp, i));
				sp = i + 1;
			} else {
				strlist.add("");
				sp = i + 1;
			}
		} // for

		return strlist.toArray(new String[0]);
		/*
		 * int arrCount = strlist.size(); String[] resStrArr = new String[arrCount]; for (i = 0; i < arrCount; i++) {
		 * resStrArr[i] = strlist.get(i).toString(); } return resStrArr;
		 */

	} // ArrayList split( String s, char[] sc)

	/**
	 * 
	 * 
	 * @param str
	 * @param osrc
	 * @param nstr
	 * @return
	 */
	public static String replace(String str, String osrc, String nstr) {
		StringBuffer resStr = new StringBuffer();
		int start = 0;
		int pos = 0;

		// blank check
		if (osrc.length() == 0)
			return str;
		if (str.length() == 0)
			return str;

		while (pos != -1) {
			pos = str.indexOf(osrc, start);

			// System.out.println( " ustring-replace : s:"+start+" p:"+ pos +
			// " ("+str+") ("+osrc +") ("+nstr +") ");

			if (pos == -1) {
				// resStr+=str.substring( start, str.length() );
				resStr.append(str.substring(start, str.length()));
			} else {
				// resStr+=str.substring( start, pos );
				resStr.append(str.substring(start, pos));
				// resStr+=nstr;
				resStr.append(nstr);
				start = pos + osrc.length();
			}
		}
		// return resStr;
		return new String(resStr);
	}

	// public static String format(String fmtCSharpType, String[] params) {
	// //throws Exception
	// public static String format(String fmt) {
	// return fmt;
	// }

	public static String format(String fmtCSharpType, Object... params) { // throws
																			// Exception

		if (params == null || params.length == 0) {
			return fmtCSharpType;
		}

		// System.out.println("format--s1");
		// int i;
		int size = fmtCSharpType.length();

		String dstr = fmtCSharpType;
		String result = "";

		int ps = 0;
		int sp = 0;

		try {
			while (ps < size) {
				// System.out.println("test--1x");
				// start ... -> { or }
				sp = ps;
				while (ps < size - 1 && dstr.charAt(ps) != '{' && dstr.charAt(ps) != '}') {
					ps++;
				}
				// System.out.println("test--2x");
				// copy
				if (sp != ps) {
					result += dstr.substring(sp, ps);
				}
				// System.out.println("test--3x");

				// format check
				if (ps < size - 1) {

					char c1 = dstr.charAt(ps);
					char c2 = dstr.charAt(ps + 1);
					// {{
					if (c1 == '{' && c2 == '{') {
						result += '{';
						ps += 2;
					}
					// }}
					else if (c1 == '}' && c2 == '}') {
						result += '}';
						ps += 2;
					}
					// {10}
					else if (c1 == '{' && "0123456789".indexOf(c2) != -1) {
						// get index value
						ps++;
						String istr = "";
						while ("0123456789".indexOf(dstr.charAt(ps)) != -1) {
							istr += dstr.charAt(ps);
							ps++;
						}
						// '}' check
						if (dstr.charAt(ps) != '}') {
							throw new Exception("format error 1");
						}
						ps++;

						// System.out.println( "get num : "+istr );
						// result+=istr;
						int idx = Integer.valueOf(istr).intValue();
						result += params[idx];

					} else { // if
						throw new Exception("format error 2");
					}
				} else {
					char c1 = dstr.charAt(ps);
					if (c1 == '{' || c1 == '}') {
						throw new Exception("format error 3");
					} else {
						result += c1;
						ps++;
					}
				}
				// System.out.println("test--4x");
			} // while
				// System.out.println("test--5x");

			// System.out.println("format--s2");
		} catch (Exception exc) {
			// System.out.println("format--s3");
			exc.printStackTrace();
			// System.err.println(fmtCSharpType + "\n");
			//
		}
		// System.out.println("test--6x");

		// System.out.println("format--s4");
		return result;
	}

	/**
	 * split by word
	 * 
	 * @param str
	 * @param sc
	 * @return
	 */
	public static String[] wSplit(String str, String... sc) {

		int pos = 0;
		int i;
		int p;
		ArrayList<String> strlist = new ArrayList<String>();
		int mini = -1;
		int sc_index = 0;
		int strLength = str.length();

		while (mini < strLength && pos < strLength) {
			mini = strLength;

			for (i = 0; i < sc.length; i++) {
				p = str.indexOf(sc[i], pos);
				if (p != -1 && mini > p) {
					mini = p;
					sc_index = i;
				}
			} // for i

			if (mini < strLength && mini != -1) {
				if (pos != mini) {
					strlist.add(str.substring(pos, mini));
				}
				pos = mini + sc[sc_index].length();
			} else if (pos < strLength) {
				strlist.add(str.substring(pos, strLength));
			}

		} // while

		int arrCount = strlist.size();
		String[] resStrArr = new String[arrCount];
		for (i = 0; i < arrCount; i++) {
			resStrArr[i] = strlist.get(i).toString();
		}
		return resStrArr;
	} // ArrayList split( String s, char[] sc)

	/**
	 * 
	 * @param data
	 *            byte[]
	 * @return String
	 */
	public static String ByteArrayToHex(byte data) {
		return ByteArrayToHex(new byte[] { data }, 0);
	}

	public static String ByteArrayToHex(byte[] data) {
		return ByteArrayToHex(data, 0);
	}

	public static String ByteArrayToHex(byte[] data, boolean b) {
		if (b == false)
			return ByteArrayToHex(data, 0);
		else
			return ByteArrayToHex(data, 1);
	}

	public static String ByteArrayToHex(byte[] data, int type) {
		StringBuffer result = new StringBuffer();
		String hexMap[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

		for (int i = 0; i < data.length; i++) {
			int l = ((((int) data[i])) & 0xF0) >> 4;
			int r = ((int) data[i]) & 0x0F;
			result.append(hexMap[l] + hexMap[r]);
			if (type == 1)
				result.append(" ");
			if (type == 2)
				result.append(";");
		}

		return result.toString();
	}

	public static byte[] hexToByteArray(String hex) {
		if (hex == null || hex.length() == 0) {
			return null;
		}
		byte[] ba = new byte[hex.length() / 2];
		for (int i = 0; i < ba.length; i++) {
			ba[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return ba;
	}

	public static byte[] hexToByteArrayWithBlank(String hex) {
		if (hex == null || hex.length() == 0) {
			return null;
		}
		byte[] ba = new byte[hex.length() / 3];
		for (int i = 0; i < ba.length; i++) {
			ba[i] = (byte) Integer.parseInt(hex.substring(3 * i, 3 * i + 2), 16);
		}
		return ba;
	}

	/**
	 * 
	 * use File.pathAdd
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	@Deprecated
	public static String apath(String a, String b) {
		return apath(a, b, File.separator);
	}

	/**
	 * use File.pathAdd
	 * 
	 * @param a
	 * @param b
	 * @param separator
	 * @return
	 */
	@Deprecated
	public static String apath(String a, String b, String separator) {
		a = a.trim();
		b = b.trim();

		if (a.endsWith(separator) == false && b.startsWith(separator) == false) {
			return (a + separator + b);
		} else if (a.endsWith(separator) && b.startsWith(separator)) {

			return a + b.substring(1);
		}

		return (a + b);
	}

	// public static String apath(String wpt, String dir) {
	//
	// ---
	// if (dir.length() == 0)
	// return wpt;
	//
	// int len = wpt.length();
	// if (len == 0) {
	// return dir;
	// }
	//
	// char[] storebuf = wpt.toCharArray();
	// char[] buf = dir.toCharArray();
	//
	// int lm = 0;
	// if (storebuf[len - 1] == '\\' || storebuf[len - 1] == '/') {
	// lm = 1;
	// }
	// int fm = 0;
	// if (buf[0] == '\\' || buf[0] == '/') {
	// fm = 1;
	// }
	//
	// if (lm == 0 && fm == 0) {
	// String ns = "";
	// ns = wpt;
	// ns += "\\";
	// ns += dir;
	// return ns;
	// }
	//
	// if ((lm == 1 && fm == 0) || (lm == 0 && fm == 1)) {
	//
	// String ns = wpt;
	// ns += dir;
	// return ns;
	// }
	//
	// if (lm == 1 && fm == 1) {
	//
	// String ns = wpt;
	// ns += dir.substring(1, dir.length());
	// return ns;
	// }
	//
	// return wpt;
	// }

	/**
	 * 
	 * 
	 * 
	 * 
	 * @param c
	 * @return
	 */
	public static String byteToBinary(byte c) {

		StringBuffer sb = new StringBuffer();
		//
		int val = c;
		for (int i = 0; i < 8; i++) {
			sb.append((val & 0x80) == 0 ? 0 : 1);
			val <<= 1;
		}
		return sb.toString();
	}

	/**
	 * 
	 * 
	 * 
	 * @param str
	 * @param size
	 * @param padChar
	 * @return
	 */
	public static String padRight(String str, int size, char padChar) {
		StringBuffer padded = new StringBuffer(str);
		while (padded.length() < size) {
			padded.append(padChar);
		}
		return padded.toString();
	}

	/**
	 * 
	 * @param str
	 * @param size
	 * @param padChar
	 * @return
	 */
	public static String padLeft(String str, int size, char padChar) {
		StringBuffer padded = new StringBuffer();
		while (padded.length() + str.length() < size) {
			padded.append(padChar);
		}
		padded.append(str);
		return padded.toString();
	}

	/**
	 * 
	 * 
	 * 
	 */

	public static String formatBytesSize(long size) {
		String hrSize = null;

		double b = size;
		double k = size / 1024.0;
		double m = ((size / 1024.0) / 1024.0);
		double g = (((size / 1024.0) / 1024.0) / 1024.0);
		double t = ((((size / 1024.0) / 1024.0) / 1024.0) / 1024.0);

		DecimalFormat dec = new DecimalFormat("0.00");

		if (t > 1) {
			hrSize = dec.format(t).concat(" tb");
		} else if (g > 1) {
			hrSize = dec.format(g).concat(" gb");
		} else if (m > 1) {
			hrSize = dec.format(m).concat(" mb");
		} else if (k > 1) {
			hrSize = dec.format(k).concat(" kb");
		} else {
			hrSize = new DecimalFormat("0").format(b).concat(" b");
		}

		return hrSize;
	}

	public static String formatCount(long size) {
		String hrSize = null;

		double b = size;
		double k = size / 1000.0;
		double m = ((size / 1000.0) / 1000.0);
		double g = (((size / 1000.0) / 1000.0) / 1000.0);
		double t = ((((size / 1000.0) / 1000.0) / 1000.0) / 1000.0);

		DecimalFormat dec = new DecimalFormat("0.00");

		if (t > 1) {
			hrSize = dec.format(t).concat(" tr");// trillion
		} else if (g > 1) {
			hrSize = dec.format(g).concat(" bi");// billion
		} else if (m > 1) {
			hrSize = dec.format(m).concat(" mi");// million
		} else if (k > 1) {
			hrSize = dec.format(k).concat(" th");// thousand
		} else {
			hrSize = new DecimalFormat("0").format(b).concat(" ");
		}

		return hrSize;
	}

	/**
	 * 
	 * wordsCount("../../../", "../") = 3
	 * 
	 * @param str
	 * @param words
	 * @return
	 */
	public static int wordsCount(String str, String words) {

		// String tar="../";
		//
		// String str="../../ab../c../";
		//
		int n = 0;
		int cnt = 0;
		while ((n = str.indexOf(words, n)) != -1) {
			n++;
			cnt++;

		}

		return cnt;
	}

	
	
	
 
	/**
	 * 
	 * if string length is greater than 'omitafter', the string is truncated at 'omitafter' position.
	 * 
	 * 
	 * @param s
	 * @param omitAfter
	 * @return
	 */
	public static String omitAfter(String s, int omitAfter) {
 
		if (s.length() > omitAfter) {
			return s.substring(0, omitAfter)  ;
		}
		return s;
	}
	  
	
} // class ustring
