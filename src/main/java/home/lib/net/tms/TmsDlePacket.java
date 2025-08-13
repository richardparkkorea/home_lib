package home.lib.net.tms;

import java.util.Arrays;

import home.lib.util.DataStream;
import home.lib.util.TimeUtil;

//import java.util.zip.CRC32;
//import java.util.zip.Checksum;

//
//
public class TmsDlePacket {

	private DataStream m_ds = new DataStream();

	private long m_bufferSize = 1024 * 32;

	private TimeUtil lastRxTime = new TimeUtil();

	public long m_err = 0;// accumulate error counts

	
	
	
	final public static byte STX='(';
	final public static byte ETX=')';
	final public static byte DLE_CODE='=';
	
	final public static byte CODE1='A';
	final public static byte CODE2='+';
	final public static byte CODE3='\n';
	//
	// A ( 41 ) & 0xff = be
	// + ( 2b ) & 0xff = d4
	// \n ( a ) & 0xff = f5
	// = ( 3d ) & 0xff = c2
	// ( ( 28 ) & 0xff = d7
	// ) ( 29 ) & 0xff = d6

	/**
	 * 
	 * @param buf
	 * @return
	 */
 	public static byte[] encode(byte[] buf) {
		return encode( buf, buf.length );
	}
	public static byte[] encode(byte[] buf, int len) {

		byte c = 0;
		int i;
		byte[] outTx = new byte[len * 2 + 32];

		// do BLE
		int e = 0;
		outTx[e++] = STX;
		for (i = 0; i < len; i++) {
			c = buf[i];

			if (c == CODE1 || c == CODE2 || c == CODE3 || c == STX || c == ETX || c==DLE_CODE) {

				outTx[e++] = DLE_CODE;
				outTx[e++] = (byte) (c ^ (byte) 0xff);

			} else {
				outTx[e++] = c;
			}

			if ((e + 1) > outTx.length) // 4=crc+ETX
				return null;// error

		} // for(i

		// crc?

		// etx

		outTx[e++] = ETX;

		return Arrays.copyOf(outTx, e);
	}

	/**
	 * 
	 * @param inRxd
	 * @param rxLen
	 * @return
	 */
	public static byte[] decode(byte[] inRxd, int rxLen) {

		byte[] buf = new byte[rxLen + 32];

		int i;
		int n = 0;
		byte c = 0;
		
		if( inRxd[0]!= STX)
			return null;
		
		if( inRxd[rxLen-1]!=ETX) 
			return null;
		

		 

		// except for STX, checksum, ETX
		for (i = 1; i < rxLen - 1; i++) {
			c = inRxd[i];

			if (c == DLE_CODE) {
				i++;// go next
				c = inRxd[i];
				buf[n++] = (byte) (c ^ 0xff);
			} else
				buf[n++] = c;

			if ((n + 1) > buf.length)
				return null; // error

		} // for(i
			// buf[n++] = (byte) inRxd[inRxd.length - 1];// add ETX

		return Arrays.copyOf(buf, n);

	}

//	/**
//	 * modbus crc
//	 * 
//	 * @param crc
//	 * @param data
//	 * @return
//	 */
//	/* CRC algorithm */
//	public static short mbCrc16(short crc, short data) {
//		short Poly16 = (short) 0xA001;
//		short LSB;
//		short i;
//		crc = (short) (((crc ^ data) | 0xFF00) & (crc | 0x00FF));
//		for (i = 0; i < 8; i++) {
//			LSB = (short) (crc & 0x0001);
//			crc = (short) (crc / 2);
//			if (LSB != 0)
//				crc = (short) (crc ^ Poly16);
//		}
//		return (crc);
//	}
//
//	/*
//	 * 8
//	 * 
//	 */
//	public static short mbGenerateCRC(byte[] data, int length) {
//		short crc = (short) 0xFFFF;
//		for (int i = 0; i < length; i++) {
//			crc = mbCrc16(crc, data[i]);
//		}
//
//		return crc;
//	}
	
	/**
	 * 
	 * @param l
	 * @return
	 */
//	public TmsDlePacket setBufferSize(long l) {
//		  m_bufferSize=l;
//		return this;
//	}
	
	public TmsDlePacket(long l) {
		  m_bufferSize=l;
		
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
	 * @param l
	 */
	public void setBufferSize(long l) {
		 m_bufferSize=l;
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
	public static byte[] make(TmsItem m) throws Exception {
		 
		return encode(m.toBytes());
	}
	/**
	 * 
	 * 
	 * @return
	 */
	public TmsItem poll()   throws Exception  {

		if (m_ds.length() < 11) {
			// debug("len err 11");
			return null;
		}

		if (m_ds.byteAt(0) != (byte) STX) {
			
			 debug("stx err  len(%d) stx(%x) ", m_ds.length(), m_ds.byteAt(0)) ;
			 m_ds.reset();
			return null;
		}

		int e = m_ds.indexOf(ETX);
		if (e < 0) {
			 
			return null;
		}

		if( e>getBufferLimit() ) {
			
			debug("size err %d>%d", e, getBufferLimit() );
			m_ds.reset();
			return null;
		}
		
 
		
		byte[] od = m_ds.pollFirst(e + 1);

		byte[] dd = decode(od, od.length);
		if (dd == null) {
			m_err++;
			m_ds.reset();
			debug("dle decode err");
			return null;
		}
		
	//System.out.println("dd="+dd.length);

		TmsItem m = TmsItem.fromBytes(dd);
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
