package home.lib.serial;



import com.fazecast.jSerialComm.SerialPort;

import home.lib.lang.UserException;

/**
 * 
 * use jSerialComm-2.5.1.jar
 * 
 * 
 * @author richard
 *
 */
final public class CommLib {

	/**
	 * 
	 */
	final public static int ERROR_SUCCESS = 0;

	// Port availability
	//
	final public static int EPortUnknownError = -1; // Unknown error occurred
	final public static int EPortAvailable = 0; // Port is available
	final public static int EPortNotAvailable = 1; // Port is not present
	final public static int EPortInUse = 2; // Port is in use

	/**
	 * 
	 * @param strPort
	 * @return
	 */
	public static int checkPort(String strPort) {

		SerialPort p = SerialPort.getCommPort(strPort);

		boolean usable = false;
		if (p.openPort()) {
			usable = true;
		}

		p.closePort();

		if (usable)
			return EPortAvailable;

		return EPortUnknownError;

	}

	/**
	 * 
	 */
	@Override
	public String toString() {

		return "CommLib2@" + this.hashCode();

	}

	/**
	 * 
	 * @return
	 */
	public static String[] getPorts() {

		SerialPort[] p = SerialPort.getCommPorts();

		String[] ret = new String[p.length];

		for (int h = 0; h < p.length; h++) {
			ret[h] = p[h].getSystemPortName();
		}

		return ret;

	}

	/**
	 * 
	 * @param p
	 * @return
	 */
	public static boolean isPortExsit(String p) {

		for (String s : getPorts()) {
			if (s.toLowerCase().trim().equals(p.toLowerCase().trim())) {
				return true;
			}
		}
		return false;
	}

	//
	// //
	// // Handshaking
	// final public static int EHandshakeUnknown = -1; // Unknown
	// final public static int EHandshakeOff = 0; // No handshaking
	// final public static int EHandshakeHardware = 1; // Hardware handshaking (RTS/CTS)
	// final public static int EHandshakeSoftware = 2; // Software handshaking (XON/XOFF)

	public static int EVEN_PARITY = SerialPort.EVEN_PARITY;
	public static int ODD_PARITY = SerialPort.ODD_PARITY;
	public static int NO_PARITY = SerialPort.NO_PARITY;
	public static int MARK_PARITY = SerialPort.MARK_PARITY;
	public static int SPACE_PARITY = SerialPort.SPACE_PARITY;

	public static int ONE_POINT_FIVE_STOP_BITS = SerialPort.ONE_POINT_FIVE_STOP_BITS;
	public static int ONE_STOP_BIT = SerialPort.ONE_STOP_BIT;
	public static int TWO_STOP_BITS = SerialPort.TWO_STOP_BITS;

	final protected Object m_lock = new Object();

	// Load the library
	static {

	}

	/**
	 * 
	 */
	public CommLib() {

	}

	protected void finalize() throws Throwable {

	}

	SerialPort m_handle = null;

	public SerialPort handle() {
		return m_handle;
	}

	 

	/**
	 * ERROR_SUCCESS or fail
	 * @param str
	 * @return
	 * @throws Exception
	 */
	public static Object[] checkConnectionString(String str) throws Exception {

		// example
		// com1,9600,8,none,1

		String[] s = str.trim().split(",");
		if (s.length != 5)
			throw new UserException("format error. ex) com1,9600,8,none,1 ");

		String port = s[0];
		int baud;
		int databit;
		int parity;
		int stopbit;

		try {
			baud = Integer.valueOf(s[1]).intValue();
		} catch (Exception e) {
			throw new UserException("baudrate format err");
		}

		
		
		databit = CommLib.indexOfDatabit( s[2]);
		if (databit == -1) {
			throw new UserException("databit format err");
		}
		
		

		parity = CommLib.indexOfParityText(s[3]);
		if (parity == -1) {
			throw new UserException("partiy format err");
		}

		stopbit = CommLib.indexOfStopBitText( s[4]);
		if (stopbit == -1) {
			throw new UserException("stopbit format err");
		}

		return new Object[] { port, baud, databit, parity, stopbit};

	}

	public int Open(String strPort, int iBaudRate, int iDataBits, int iParity, int iStopBits) {

		synchronized (m_lock) {
			// return Open(m_handle, strPort, iBaudRate, iDataBits, iParity, iStopBits);
			m_handle = SerialPort.getCommPort(strPort);
			m_handle.setBaudRate(iBaudRate);
			;
			m_handle.setNumDataBits(iDataBits);
			m_handle.setParity(iParity);
			m_handle.setNumStopBits(iStopBits);

			m_handle.openPort();

			if (m_handle.isOpen())
				return ERROR_SUCCESS;
			else
				return -1;

		}
	}

	public boolean isOpen() {

		if (m_handle == null)
			return false;

		if (m_handle.isOpen())
			return true;

		return false;
	}

	public int SetTimeouts(String who, int timeout) {
		synchronized (m_lock) {

			return -1;
		}
	}

	public int SetTimeouts(String who, int timeout, int rtc, int rtm, int stc, int stm) {
		synchronized (m_lock) {
			// return SetTimeouts(m_handle, who, timeout, rtc, rtm, stc, stm);
			return -1;
		}
	}

	// data field supportable length : 4k
	public byte[] DataRead() {

		if (m_handle == null)
			return null;

		if (m_handle.bytesAvailable() <= 0)
			return new byte[0];

		byte[] readBuffer = new byte[m_handle.bytesAvailable()];

		int numRead = m_handle.readBytes(readBuffer, readBuffer.length);

		return readBuffer;

	}

	// data field supportable length : 4k
	public int WriteData(byte[] data) throws Exception {

		if (m_handle == null)
			throw new UserException("port is not opened");
		// synchronized (m_lock) {
		// return WriteData(m_handle, data);
		// }
		return m_handle.writeBytes(data, data.length);
	}

	public int Close() {
		synchronized (m_lock) {

			if (m_handle == null)
				return -1;

			if (m_handle.closePort()) {
				m_handle = null;
				return ERROR_SUCCESS;
			} else {
				m_handle = null;
				return -1;
			}
		}
	}

	public String getLastError() {
		synchronized (m_lock) {
			return "";
		}
	}

	public int getLastErrorCode() {
		synchronized (m_lock) {
			return -1;
		}

	}

	public long SetupHandshaking(long eHandshake) {
		synchronized (m_lock) {
			// return SetupHandshaking(m_handle,eHandshake);
			return 0;
		}
	}

	public long Purge() {
		synchronized (m_lock) {
			// return Purge(m_handle );
			return 0;
		}

	}

	public long SetMask(long dwMask) {
		synchronized (m_lock) {
			// return SetMask(m_handle, dwMask) ;
			return 0;
		}

	}

	public long SetEventChar(int bEventChar, int fAdjustMask) {
		synchronized (m_lock) {
			// return SetEventChar( m_handle, bEventChar, fAdjustMask );
			return 0;
		}
	}

	public long Break() {
		synchronized (m_lock) {
			// return Break( m_handle );
			return 0;
		}

	}

	/**
	 * 
	 */
	public static void test(String args[]) {

		System.out.println("***start***");

		CommLib cl = new CommLib();

		// #define NOPARITY 0
		// #define ONESTOPBIT 0
		cl.Open("COM5", 19200, 8, 0, 0);

		boolean fContinue = true;
		do {
			try {
				cl.WriteData("######".getBytes());
			} catch (Exception e) {
			}

			byte[] r = cl.DataRead();

			if (r == null) {
				System.out.println("COM port error");
				fContinue = false;
			} else if (r.length == 0) {
				System.out.println("no data");
			} else {
				for (int i = 0; i < r.length; i++) {
					System.out.print((char) r[i]);
				}
				System.out.println("");
			}

			System.gc();
		} while (fContinue);

		System.out.format("get last error (%s) ", cl.getLastError());
		cl.Close();

		cl = null;

		System.out.println("***end***");
		System.gc();
	}

	/**
	 * 
	 * @param s
	 * @return
	 */
	public static int indexOfParityText(String s) {
		s = s.toLowerCase().trim();

		if (s.equals("none"))
			return SerialPort.NO_PARITY;
		if (s.equals("even"))
			return SerialPort.EVEN_PARITY;
		if (s.equals("odd"))
			return SerialPort.ODD_PARITY;
		if (s.equals("mark"))
			return SerialPort.MARK_PARITY;
		if (s.equals("space"))
			return SerialPort.SPACE_PARITY;

		return -1;
	}

	/**
	 * 
	 * @param s
	 * @return
	 */
	public static int indexOfStopBitText(String s) {
		s = s.toLowerCase().trim();

		if (s.equals("1"))
			return SerialPort.ONE_STOP_BIT;
		if (s.equals("1.5"))
			return SerialPort.ONE_POINT_FIVE_STOP_BITS;
		if (s.equals("2"))
			return SerialPort.TWO_STOP_BITS;

		return -1;
	}
	
	
	
	/**
	 * 
	 * @param s
	 * @return
	 */
	public static int indexOfDatabit(String s) {
		s = s.toLowerCase().trim();

		if (s.equals("4"))
			return 4;
		
		if (s.equals("5"))
			return 5;
		
		if (s.equals("6"))
			return 6;
		
		if (s.equals("7"))
			return 7;
		
		if (s.equals("8"))
			return 8;
 
		return -1;
	}
	
	//
	// /**
	// *
	// *
	// *
	// *
	// */
	//
	//
	//
	// public void keepAlive(int checkIntervalSec) {
	//
	// if (m_tmr_alive != null)
	// return;
	//
	// int l = checkIntervalSec * 1000;
	//
	// m_tmr_alive = new Timer2().schedule(new TimerTask() {
	// public void run() {
	//
	// if(m_handle.isOpen()==false && m_handle) {
	//
	// }
	// int chk = checkPort(strPort);
	//
	// if (chk == CommLib.EPortAvailable) {
	//
	// }
	//
	// }
	//
	// }, l, l);
	//
	// }

}
