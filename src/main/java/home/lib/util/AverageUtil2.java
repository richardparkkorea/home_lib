package home.lib.util;

import java.util.ArrayList;

public class AverageUtil2 {

	ArrayList<Long> arr = new ArrayList<Long>();

	TimeUtil tm = new TimeUtil();
	int m_max = 32;

	public AverageUtil2() {
		this(32);
	}

	public AverageUtil2(int max) {
		m_max = max;
	}

	public void add(long l) {

		synchronized (arr) {
			
			arr.add(l);
			
			if( arr.size()>m_max)
				arr.remove(0);
		}
	}

	public boolean isTimeover(long timeOverMs) {
		if (tm.end_ms() > timeOverMs)
			return true;

		return false;
	}

	public long getAverage() {

		long sum = 0;
		

		synchronized (arr) {
			
			if( arr.size()==0)
				return 0;
			
			
			for (long v : arr) {
				sum += v;
			}

			tm.start();

			return (int) ((sum / arr.size()) );
		} // syn

	}

}
