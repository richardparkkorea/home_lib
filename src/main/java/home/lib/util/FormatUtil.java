package home.lib.util;

public class FormatUtil {

 
	/**
	 * 
	 * @param no
	 * @param low
	 * @param high
	 * @return
	 */
	public static int limits(int no, int low, int high) {
		if (no < low)
			no = low;

		if (no > high)
			no = high;

		return no;
	}

	/**
	 * 
	 * @param no
	 * @param low
	 * @param high
	 * @return
	 */
	public static int limits(String no, int low, int high) {

		return limits(to_int(no), low, high);

	}

	/**
	 * 
	 * @param vv
	 * @param n
	 * @param m
	 * @return
	 */

	public static boolean is_it_in_range(String vv, int n, int m) {

		vv = vv.trim();

		//
		if (is_int(vv) == false) {

			return false;
		}

		int no = Integer.decode(vv);

		if (no < n || no > m) {

			return false;
		}
		return true;

	}

	/**
	 * no error return
	 * @param s
	 * @return
	 */
	public static int to_int(Object s) {
		try {
			String str = s.toString().trim();

			if (str.indexOf("0x") == 0 || str.indexOf("0X") == 0) {
				return Integer.decode(s.toString().trim());
			} else {
				return Integer.valueOf(s.toString().trim());
			}

		} catch (Exception e) {

		}
		return 0;
	}

	/**
	 * 
	 * @param s
	 * @return
	 */
	public static boolean is_int(Object s) {
		try {

			String str = s.toString().trim();
			if (str.indexOf("0x") == 0 || str.indexOf("0X") == 0) {
				int no = Integer.decode(s.toString().trim());
			} else {
				int no = Integer.valueOf(s.toString().trim());
			}

			return true;
		} catch (Exception e) {

		}
		return false;
	}

	/**
	 * 
	 * @param no
	 * @param low
	 * @param high
	 * @return
	 */
	public static int to_int(Object no, int low, int high) {

		return limits(to_int(no), low, high);
	}

}
