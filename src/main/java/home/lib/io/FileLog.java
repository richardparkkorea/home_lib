package home.lib.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import home.lib.util.FileUtil;
import home.lib.util.TimeUtil;

/**
 * 
 * @author Administrator
 * 
 */
final public class FileLog {

	private int recursiveCount = 10;
	private long MaxKiloBytesOfAFile = 512;
	private String myNames[] = null;
	private String writingFileName;
	private boolean writeWithDateInfo = true;
	
	private boolean m_useDailyType = false;

	/**
	 * 
	 * @param name
	 * @param ad
	 *            - add date
	 */
	public FileLog(String name, boolean ad) {
		writeWithDateInfo = ad;
		init(name, 10, 512);
	}

	/**
	 * 
	 * @param name
	 * @param ad
	 *            - writeWithDateInfo
	 * @param r
	 *            - rotation
	 * @param kbSize
	 *            -sigle file byte size
	 */
	public FileLog(String name, boolean ad, int r, int kbSize) {
		writeWithDateInfo = ad;
		init(name, r, kbSize);
	}
	
	
	public FileLog(String name, boolean ad, int r, int kbSize,boolean isDaily) {
		writeWithDateInfo = ad;
		
		setDailyOrRecursive(isDaily);
		init(name, r, kbSize);
	}

	
	

	/**
	 * 
	 * @param name
	 * @param cnt
	 * @param kb
	 */
	private void init(String name, int cnt, int kb) {
		recursiveCount = cnt;
		MaxKiloBytesOfAFile = kb;

		myNames = FileUtil._splitpath(name);
		writingFileName = getWritableName();
	}
	
	public void setDailyOrRecursive(boolean b) {
		m_useDailyType=b;
		writingFileName = getWritableName();
	}

	/**
	 * 
	 * @return String
	 */
	private String getWritableName() {

		if (m_useDailyType) {

			String pattern = "yyyyMMdd";
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
			String date = simpleDateFormat.format(new Date());

			String s = String.format("%s%s%s%s%s", myNames[0], myNames[1], myNames[2], date, myNames[3]);

			return s;

		} else {

			if (recursiveCount == 0)
				return "";// didn't initialized

			String wfn = ""; // writable file name
			long lwd = Long.MAX_VALUE;// 0x7fffffff; // last writed date
			int lwd_idx = 0;

			//

			for (int i = 0; i < recursiveCount; i++) {

				String s = String.format("%s%s%s-%s%s", myNames[0], myNames[1], myNames[2], i, myNames[3]);

				File ff = new File(s);
				if (ff.exists() == false || (ff.exists() != false && ff.length() < MaxKiloBytesOfAFile * 1024)) {
					wfn = s;
				}

				long ft = ff.lastModified();
				if (ft < lwd) {
					lwd = ft;
					lwd_idx = i; // find min
				}

			}
			if (wfn.length() == 0) {
				String s = String.format("%s%s%s-%s%s", myNames[0], myNames[1], myNames[2], lwd_idx, myNames[3]);

				new File(s).delete();

				wfn = s;

			}
			return wfn;

		}
	}

	/**
	 * 
	 * @param format(c++type)
	 * @param params
	 * @return
	 */
	public int write(String format, Object... params) {

		if (recursiveCount == 0)
			return -1;// didn't initialized

		// format
		String msg;
		msg = String.format(format, params);

		// printf("trace--2.4\n");
		if (new File(writingFileName).exists()) {
			if (new File(writingFileName).length() > MaxKiloBytesOfAFile * 1024) {
				writingFileName = getWritableName();
			}
		}

		if (writingFileName.length() == 0)
			return 0;

		if (writeWithDateInfo == true)
			msg = String.format("%s> %s\n", TimeUtil.now(), msg);

		// System.out.println( writingFileName+ " "+ostr.length() );

		FileWriter outFile;
		try {
			outFile = new FileWriter(writingFileName, true);
			PrintWriter out = new PrintWriter(outFile);
			out.println(msg);
			outFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			return 0;
		}

		return 1;
	}

	/**
	 * 
	 */
	public void close() {
		writingFileName = "";
	}

}