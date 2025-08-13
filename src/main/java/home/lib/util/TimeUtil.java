
package home.lib.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
//import java.util.Timer;

/**
 *
 * <p>
 * Title:
 * </p>
 *
 * <p>
 * Description:
 * </p>
 *
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 *
 * <p>
 * Company:
 * </p>
 *
 * @author not attributable
 * @version 1.0
 * 
 *          System.currentTimeMillis() is sensitive to system clock
 * 
 *          System.nanoTime() is not sensitive to system clock as it measures the time elapsed.
 * 
 */

public class TimeUtil {

	/**
	 *
	 */
	public TimeUtil() {
		super();
		start();

	}


	/**
	 *
	 */
	private long m_cur = 0;

	public void start() {
		m_cur = (System.nanoTime() / 1000000L);
	}

	/**
	 * end_sec is better use
	 * 
	 * @return double - unit(second)
	 */
	@Deprecated
	public double end() {
		return end_sec();
	}

	public double end_sec() {
		double d = ((System.nanoTime() / 1000000L) - m_cur);
		d /= 1000.0;
		return d;
	}

	/**
	 * 
	 * @return
	 */
	public long end_ms() {
		return ((System.nanoTime() / 1000000L) - m_cur);
	}

	public boolean elapsed_ms(long timeout) {
		return (end_ms() > timeout);
	}

	public void start(long after) {
		m_cur = (System.nanoTime() / 1000000L);

	}
	
	
	
	
	
	
	
	

	/**
	 * 
	 * @return
	 */
	public static long currentTimeMillis() {

		// long m = System.currentTimeMillis();
		// m = (m / 1000) * 1000;// get sec
		//
		// long n = System.nanoTime();
		// n = n / 1000000;// nano sec to mill sec
		// n = n - ((long) (n / 1000) * 1000);// get milli-sec
		//
		// return (m + n);

		return (System.nanoTime() / 1000000L);

	}

	public static String milliseconds2time(long millis) {

		// long millis = utime.end_ms();
		long second = (millis / 1000) % 60;
		long minute = (millis / (1000 * 60)) % 60;
		long hour = (millis / (1000 * 60 * 60)) % 24;

		return String.format("%02d:%02d:%02d", hour, minute, second);

	}

	/**
	 * 
	 * milliseconds to datetime
	 */
	public static String milli2ElapsedDate(long different) {

		long secondsInMilli = 1000;
		long minutesInMilli = secondsInMilli * 60;
		long hoursInMilli = minutesInMilli * 60;
		long daysInMilli = hoursInMilli * 24;

		long elapsedDays = different / daysInMilli;
		different = different % daysInMilli;

		long elapsedHours = different / hoursInMilli;
		different = different % hoursInMilli;

		long elapsedMinutes = different / minutesInMilli;
		different = different % minutesInMilli;

		long elapsedSeconds = different / secondsInMilli;

		// return String.format("%d days, %d hours, %d minutes, %d seconds ", elapsedDays, elapsedHours, elapsedMinutes,
		// elapsedSeconds);
		return String.format("%d days %d:%d:%d", elapsedDays, elapsedHours, elapsedMinutes, elapsedSeconds);

	}

	public static void sleep(long millis) {

		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {

			e.printStackTrace();
		}

	}

	/*
	 * private uTimeInterface m_if=null; private int myEventNumber=0; private Timer myTimer=null;
	 * 
	 * public void setTimer(uTimeInterface inter, int milli_sec,int eventNumber) { killTimer(); // m_if = inter;
	 * myTimer=new Timer(); myTimer.schedule(this,0,milli_sec); myEventNumber=eventNumber; } public void killTimer() {
	 * if( myTimer!=null) { myTimer.cancel(); myTimer=null; } }
	 * 
	 * 
	 * public void run() { if( m_if==null) return; try{ m_if.onTimer(this.myEventNumber); } catch(Exception e) {
	 * e.printStackTrace(); } }//method
	 */

	/**
	 * 
	 */
	/**
	*
	*/
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

	/**
	 *
	 * @return String
	 */
	public static String now() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());

	}

	/**
	 * convert milliseconds to date format "%d days, %d:%d:%d "<br>
	 * 
	 * @param different
	 * @return
	 */
	public static String ms2Date(long different) {

		// milliseconds
		// long different = endDate.getTime() - startDate.getTime();

		// System.out.println("startDate : " + startDate);
		// System.out.println("endDate : "+ endDate);
		// System.out.println("different : " + different);

		long secondsInMilli = 1000;
		long minutesInMilli = secondsInMilli * 60;
		long hoursInMilli = minutesInMilli * 60;
		long daysInMilli = hoursInMilli * 24;

		long elapsedDays = different / daysInMilli;
		different = different % daysInMilli;

		long elapsedHours = different / hoursInMilli;
		different = different % hoursInMilli;

		long elapsedMinutes = different / minutesInMilli;
		different = different % minutesInMilli;

		long elapsedSeconds = different / secondsInMilli;

		// return String.format("%d days, %d hours, %d minutes, %d seconds ", elapsedDays, elapsedHours, elapsedMinutes,
		// elapsedSeconds);
		return String.format("%d days, %d:%d:%d  ", elapsedDays, elapsedHours, elapsedMinutes, elapsedSeconds);

	}
	
	
	

	/**
	 *  until ~ isover?
	 * 
	 */
	private boolean m_untilActivate = false;
	private long m_until = 0;

	public void until(long untilMs) {
		m_until = currentTimeMillis() + untilMs;
		m_untilActivate = true;
	}

	public boolean isOver() {

		if (m_untilActivate == false)
			return false;

		if (m_until < currentTimeMillis() ) {
			m_untilActivate = false;
			return true;
		}
		return false;

	}
	
	

}// class
