package home.lib.util;

public class AverageUtil {

	TimeUtil tm = new TimeUtil();
	long sumLong = 0;
	long count = 0;

	public void sum(long l) {
		sumLong += l;
		count++;
	}

	public boolean isTimeover(long timeOverMs) {
		if (tm.end_ms() > timeOverMs)
			return true;

		return false;
	}

	public long getLong() {

		long res = 0;

		if (sumLong > 0 & count > 0) {

			res = (sumLong / count);

		} // if

		tm.start();
		sumLong = 0;
		count = 0;

		return res;
	}

}
