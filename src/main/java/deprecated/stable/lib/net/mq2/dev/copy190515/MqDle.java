package deprecated.stable.lib.net.mq2.dev.copy190515;

import java.util.Arrays;

import home.lib.io.Crc32;

import home.lib.util.DataStream;
import home.lib.util.TimeUtil;

/*
 * 
 * 
 * AT command escape!
 * 
 * A = \r \n --> -(x)
 * 
 * 
 * 
 * 
 * @author richard
 *
 */
public class MqDle {

	
	final public static byte STX='(';
	final public static byte ETX=')';
	final public static byte DLE_CODE='=';
	
	final public static byte CODE1='A';
	final public static byte CODE2='+';
	final public static byte CODE3='\n';
	//
	// A ( 41 ) & 0xff = be
	// + ( 2b ) & 0xff = d4
	//
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

	/**
	 * modbus crc
	 * 
	 * @param crc
	 * @param data
	 * @return
	 */
	/* CRC algorithm */
	public static short mbCrc16(short crc, short data) {
		short Poly16 = (short) 0xA001;
		short LSB;
		short i;
		crc = (short) (((crc ^ data) | 0xFF00) & (crc | 0x00FF));
		for (i = 0; i < 8; i++) {
			LSB = (short) (crc & 0x0001);
			crc = (short) (crc / 2);
			if (LSB != 0)
				crc = (short) (crc ^ Poly16);
		}
		return (crc);
	}

	/*
	 * 8
	 * 
	 */
	public static short mbGenerateCRC(byte[] data, int length) {
		short crc = (short) 0xFFFF;
		for (int i = 0; i < length; i++) {
			crc = mbCrc16(crc, data[i]);
		}

		return crc;
	}

	
}
