package deprecated.lib.net.mq2.dev_old;

import home.lib.util.DataStream;
import home.lib.util.TimeUtil;

//import java.util.zip.CRC32;
//import java.util.zip.Checksum;

//
//
public class MqPacket {

	private DataStream m_ds = new DataStream();

	private long m_bufferSize = 1024 * 8;

	private TimeUtil lastRxTime = new TimeUtil();

	public long m_err = 0;// accumulate error counts

	/**
	 * 
	 * @param l
	 * @return
	 */
	public MqPacket setBufferSize(long l) {
		  m_bufferSize=l;
		return this;
	}
	
	/**
	 * 
	 * @return
	 */
	public long getBufferSize() {
		return m_bufferSize;
	}
	
	/**
	 * 
	 * @return
	 */
	private long getBufferLimit() {
		return ((m_bufferSize*2)+16);
	}

	/***
	 * 
	 * 
	 * 
	 * @param b
	 * @param len
	 */
	public void append(byte[] b, int len) {
		m_ds.writeBytes(b, len);

		if (m_ds.length() > getBufferLimit() ) {
			m_ds.reset();
		}
		
	//	debug("append (%d/%d) ", m_ds.length(), m_bufferSize );
	}

	/**
	 * 
	 * @return
	 */
	public int length() {
		return m_ds.length();
	}

	
	/**
	 * 
	 * @param m
	 * @return
	 */
	public static byte[] make(MqItem m) throws Exception {
		 
		return MqDle.encode(m.toBytes());
	}
	/**
	 * 
	 * 
	 * @return
	 */
	public MqItem poll()   throws Exception  {

		if (m_ds.length() < 11) {
			// debug("len err 11");
			return null;
		}

		if (m_ds.byteAt(0) != (byte) MqDle.STX) {
			
			 debug("stx err  len(%d) stx(%x) ", m_ds.length(), m_ds.byteAt(0)) ;
			 m_ds.reset();
			return null;
		}

		int e = m_ds.indexOf(MqDle.ETX);
		if (e < 0) {
			 
			return null;
		}

		if( e>getBufferLimit() ) {
			
			debug("size err %d>%d", e, getBufferLimit() );
			m_ds.reset();
			return null;
		}
		
 
		
		byte[] od = m_ds.pollFirst(e + 1);

		byte[] dd = MqDle.decode(od, od.length);
		if (dd == null) {
			m_err++;
			m_ds.reset();
			debug("dle decode err");
			return null;
		}
		
	//System.out.println("dd="+dd.length);

		MqItem m = MqItem.fromBytes(dd);
		if (m == null) {
			m_err++;
			return null;
		}

		lastRxTime.start();

		return m;

	}

	/**
	 * 
	 * 
	 * @param s
	 */
	public static String debug(String s, Object... args) {

		String m="mqpacket stream[" + String.format(s, args);
		System.out.println(m);
		return m;
	}

	

}
