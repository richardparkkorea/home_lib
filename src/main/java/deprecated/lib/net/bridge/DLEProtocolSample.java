package deprecated.lib.net.bridge;

import java.util.Arrays;

class DLEProtocolSample {

	public static short crc16tab[] = new short[] { (short) 0x0000, (short) 0x1021, (short) 0x2042, (short) 0x3063,
			(short) 0x4084, (short) 0x50a5, (short) 0x60c6, (short) 0x70e7, (short) 0x8108, (short) 0x9129,
			(short) 0xa14a, (short) 0xb16b, (short) 0xc18c, (short) 0xd1ad, (short) 0xe1ce, (short) 0xf1ef,
			(short) 0x1231, (short) 0x0210, (short) 0x3273, (short) 0x2252, (short) 0x52b5, (short) 0x4294,
			(short) 0x72f7, (short) 0x62d6, (short) 0x9339, (short) 0x8318, (short) 0xb37b, (short) 0xa35a,
			(short) 0xd3bd, (short) 0xc39c, (short) 0xf3ff, (short) 0xe3de, (short) 0x2462, (short) 0x3443,
			(short) 0x0420, (short) 0x1401, (short) 0x64e6, (short) 0x74c7, (short) 0x44a4, (short) 0x5485,
			(short) 0xa56a, (short) 0xb54b, (short) 0x8528, (short) 0x9509, (short) 0xe5ee, (short) 0xf5cf,
			(short) 0xc5ac, (short) 0xd58d, (short) 0x3653, (short) 0x2672, (short) 0x1611, (short) 0x0630,
			(short) 0x76d7, (short) 0x66f6, (short) 0x5695, (short) 0x46b4, (short) 0xb75b, (short) 0xa77a,
			(short) 0x9719, (short) 0x8738, (short) 0xf7df, (short) 0xe7fe, (short) 0xd79d, (short) 0xc7bc,
			(short) 0x48c4, (short) 0x58e5, (short) 0x6886, (short) 0x78a7, (short) 0x0840, (short) 0x1861,
			(short) 0x2802, (short) 0x3823, (short) 0xc9cc, (short) 0xd9ed, (short) 0xe98e, (short) 0xf9af,
			(short) 0x8948, (short) 0x9969, (short) 0xa90a, (short) 0xb92b, (short) 0x5af5, (short) 0x4ad4,
			(short) 0x7ab7, (short) 0x6a96, (short) 0x1a71, (short) 0x0a50, (short) 0x3a33, (short) 0x2a12,
			(short) 0xdbfd, (short) 0xcbdc, (short) 0xfbbf, (short) 0xeb9e, (short) 0x9b79, (short) 0x8b58,
			(short) 0xbb3b, (short) 0xab1a, (short) 0x6ca6, (short) 0x7c87, (short) 0x4ce4, (short) 0x5cc5,
			(short) 0x2c22, (short) 0x3c03, (short) 0x0c60, (short) 0x1c41, (short) 0xedae, (short) 0xfd8f,
			(short) 0xcdec, (short) 0xddcd, (short) 0xad2a, (short) 0xbd0b, (short) 0x8d68, (short) 0x9d49,
			(short) 0x7e97, (short) 0x6eb6, (short) 0x5ed5, (short) 0x4ef4, (short) 0x3e13, (short) 0x2e32,
			(short) 0x1e51, (short) 0x0e70, (short) 0xff9f, (short) 0xefbe, (short) 0xdfdd, (short) 0xcffc,
			(short) 0xbf1b, (short) 0xaf3a, (short) 0x9f59, (short) 0x8f78, (short) 0x9188, (short) 0x81a9,
			(short) 0xb1ca, (short) 0xa1eb, (short) 0xd10c, (short) 0xc12d, (short) 0xf14e, (short) 0xe16f,
			(short) 0x1080, (short) 0x00a1, (short) 0x30c2, (short) 0x20e3, (short) 0x5004, (short) 0x4025,
			(short) 0x7046, (short) 0x6067, (short) 0x83b9, (short) 0x9398, (short) 0xa3fb, (short) 0xb3da,
			(short) 0xc33d, (short) 0xd31c, (short) 0xe37f, (short) 0xf35e, (short) 0x02b1, (short) 0x1290,
			(short) 0x22f3, (short) 0x32d2, (short) 0x4235, (short) 0x5214, (short) 0x6277, (short) 0x7256,
			(short) 0xb5ea, (short) 0xa5cb, (short) 0x95a8, (short) 0x8589, (short) 0xf56e, (short) 0xe54f,
			(short) 0xd52c, (short) 0xc50d, (short) 0x34e2, (short) 0x24c3, (short) 0x14a0, (short) 0x0481,
			(short) 0x7466, (short) 0x6447, (short) 0x5424, (short) 0x4405, (short) 0xa7db, (short) 0xb7fa,
			(short) 0x8799, (short) 0x97b8, (short) 0xe75f, (short) 0xf77e, (short) 0xc71d, (short) 0xd73c,
			(short) 0x26d3, (short) 0x36f2, (short) 0x0691, (short) 0x16b0, (short) 0x6657, (short) 0x7676,
			(short) 0x4615, (short) 0x5634, (short) 0xd94c, (short) 0xc96d, (short) 0xf90e, (short) 0xe92f,
			(short) 0x99c8, (short) 0x89e9, (short) 0xb98a, (short) 0xa9ab, (short) 0x5844, (short) 0x4865,
			(short) 0x7806, (short) 0x6827, (short) 0x18c0, (short) 0x08e1, (short) 0x3882, (short) 0x28a3,
			(short) 0xcb7d, (short) 0xdb5c, (short) 0xeb3f, (short) 0xfb1e, (short) 0x8bf9, (short) 0x9bd8,
			(short) 0xabbb, (short) 0xbb9a, (short) 0x4a75, (short) 0x5a54, (short) 0x6a37, (short) 0x7a16,
			(short) 0x0af1, (short) 0x1ad0, (short) 0x2ab3, (short) 0x3a92, (short) 0xfd2e, (short) 0xed0f,
			(short) 0xdd6c, (short) 0xcd4d, (short) 0xbdaa, (short) 0xad8b, (short) 0x9de8, (short) 0x8dc9,
			(short) 0x7c26, (short) 0x6c07, (short) 0x5c64, (short) 0x4c45, (short) 0x3ca2, (short) 0x2c83,
			(short) 0x1ce0, (short) 0x0cc1, (short) 0xef1f, (short) 0xff3e, (short) 0xcf5d, (short) 0xdf7c,
			(short) 0xaf9b, (short) 0xbfba, (short) 0x8fd9, (short) 0x9ff8, (short) 0x6e17, (short) 0x7e36,
			(short) 0x4e55, (short) 0x5e74, (short) 0x2e93, (short) 0x3eb2, (short) 0x0ed1, (short) 0x1ef0 };

	//
	public static short crc16_ccitt(byte[] buf, int len) {
		int counter;
		int p = 0;
		short crc = 0;
		for (counter = 0; counter < len; counter++)
			crc = (short) ((crc << 8) ^ crc16tab[((crc >> 8) ^ buf[p++]) & 0x00FF]);
		return crc;
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	int m_extInter_send_idx = 0;
	int m_extInter_recv_idx = 0;

	byte[] set_tx_extInter(byte rcuno, byte keydata, byte[] data) {

		byte outTx[] = new byte[128];
		int outTxBufMax = 80;

		byte i, n = 0, e = 0;
		byte buf[] = new byte[128];
		byte c, f;
		byte checksum = 0;
		byte func = 0;// function code
		byte t = 0;

		int lenpos = 0;

		// data
		n = 0;

		buf[n++] = 0x58;// micronic & master
		buf[n++] = rcuno;// type(address)
		buf[n++] = (byte) ((m_extInter_send_idx++) & 0xff);// send idx
		buf[n++] = (byte) ((m_extInter_recv_idx) & 0xff);// recv idx
		buf[n++] = keydata;// key data

		// data length
		lenpos = n;
		buf[n++] = 0;

		for (i = 0; i < data.length; i++) {
			buf[n++] = data[i];
		}

		// switch (func) {
		// case 0x00:// ?ÉÅ?Éú?†ïÎ≥? ( rcu->gateway)
		// {
		//
		// f = 0;
		//
		// } // case
		// }// switch

		if (lenpos == 0 || (lenpos + 1) > n)
			return null;// length error

		buf[lenpos] = (byte) (n - (lenpos + 1));// set length

		// //get checksum
		// checksum = 0;
		// for (i = 0; i < n; i++)
		// checksum ^= buf[i];
		//
		// buf[n++] = checksum;
		//

		//
		// do BLE
		e = 0;
		outTx[e++] = (byte) 0x80;
		for (i = 0; i < n; i++) {
			c = buf[i];

			if (c == (byte) 0xde || c == (byte) 0xc0 || c == (byte) 0xcf || c == (byte) 0x80 || c == (byte) 0x8f) {

				outTx[e++] = (byte) 0xde;
				outTx[e++] = (byte) (c ^ (byte) 0xff);
			} else
				outTx[e++] = c;

			if ((e + 4) >= outTxBufMax) // 4=crc+ETX
				return null;// error

		} // for(i

		short crc16 = 0;

		crc16 = crc16_ccitt(outTx, e);
		outTx[e] = 0;
		if ((crc16 & 0x8000) == 0x8000)
			outTx[e] |= 0x02;
		if ((crc16 & 0x0080) == 0x0080)
			outTx[e] |= 0x01;

		outTx[e + 1] = (byte) ((crc16 >> 8) & 0x7F);// crc ?ÉÅ?úÑ Î∞îÏù¥?ä∏
		outTx[e + 2] = (byte) (crc16 & 0x7F); // crc ?ïò?úÑ Î∞îÏù¥?ä∏

		outTx[e + 3] = (byte) 0x8f;

		return Arrays.copyOf(outTx, e + 4);
	}

	/**
	 * 
	 * 
	 * 
	 */
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * 
	 * 
	 * 
	 */

	int m_cur = 0;
	byte m_buf[] = new byte[128];

	public byte[] addByte(byte c) {
		// short crc = 0;
		// short crc16 = 0;
		String out = "";

		//

		switch (c) {
		// case '#':
		// out = "#";
		// // return 1;
		// return out;
		// // break;

		case (byte) 0xc0:
			m_cur = 0;
			m_buf[m_cur++] = c;
			break;

		case (byte) 0xcf: {

			m_buf[m_cur++] = c;

			int len = m_cur;

			m_cur = 0;
			m_buf[len] = 0;

			// System.out.println( "***"+new String(m_buf));

			if (len > 5) {

				int rx_crc16 = (m_buf[len - 3] << 8) | (m_buf[len - 2]);

				if ((m_buf[len - 4] & 0x02) == 0x02)
					rx_crc16 |= 0x8000;

				if ((m_buf[len - 4] & 0x01) == 0x01)
					rx_crc16 |= 0x0080;

				byte backup_byte = m_buf[len - 4];
				m_buf[len - 4] &= 0xfc;

				int crc16 = crc16_ccitt(m_buf, len - 3 - 1) & 0xffff;
				rx_crc16 &= 0xffff;

				// m_buf[len - 4] = backup_byte;

				// System.out.format("crc compare(%x)=(%x)
				// \r\n", crc16, rx_crc16);

				//
				//
				if (crc16 == rx_crc16) {
					// System.out.println(""+len+" == "+ new String(m_buf,0,len) );
					// System.out.println( bytesToHex( Arrays.copyOf(m_buf, len)));
					// System.out.println(String.format("check crc %x=%x ", crc16,rx_crc16));

					// out = new String(m_buf, 0, len);

					// return 1;//scceed
					return Arrays.copyOf(m_buf, len);
				}

				// String strErr;
				// strErr.Format(" %x != %x %c ", crc16,crc ,m_buf[len-5] );
				// out=strErr + (char*)m_buf ;
			}

			m_cur = 0;
			// return 100;//crc err
			return null;// new byte[0];

		}
			// break;

		default:
			if (m_cur >= (m_buf.length - 1))
				m_cur = 0;

			m_buf[m_cur++] = c;

			break;
		}// switch

		// return 0;//fail
		return null;
	}// method

	public String getLightChar(byte r) {

		r = (byte) (r & 0x3);

		if (r == (byte) 0x00)
			return ("x");
		else if (r == (byte) 0x01)
			return ("0");
		else if (r == (byte) 0x03)
			return (".");

		return "?";
	}

	/*
	 * 
	 * 
	 * 
	 */
	public String rxAnalysis(byte[] inRxd) {

		byte buf[] = new byte[1024];
		int n = 0;
		int rxLen = inRxd.length;
		int i;
		byte c;

		StringBuffer sb = new StringBuffer();

		buf[n++] = (byte) 0xc0;// add STX

		// except for STX, checksum, ETX
		for (i = 1; i < rxLen - 1; i++) {
			c = inRxd[i];

			if (c == (byte) 0xde) {
				i++;// go next
				c = inRxd[i];
				buf[n++] = (byte) (c ^ 0xff);
			} else
				buf[n++] = c;

			if (n > 80)
				return null; // error

		} // for(i

		byte rcuno = buf[2];

		sb.append(String.format("\r\nrcuno = %d \r\n", rcuno));

		byte keydata = buf[5];

		n = 7;

		while (n < (rxLen - 4)) {

			byte data00 = buf[n + 0];
			byte data01 = buf[n + 1];
			byte data02 = buf[n + 2];
			byte data03 = buf[n + 3];
			byte data04 = buf[n + 4];
			byte data05 = buf[n + 5];
			byte data06 = buf[n + 6];
			byte data07 = buf[n + 7];

			if (data00 != 0x77) {

				// return String.format("data00 err=%x n=%d", data00, n);
				// return null;// error
				n = rxLen;
				continue;// break the while
			}

			switch (data01) {
			case (byte) 0x01:

				if ((data03 & 0x2) == 0x2) {
					sb.append("room in ");
				} else
					sb.append("room out ");
				sb.append("\r\n");

				if ((data04 & 0x1) == 0x1) {
					sb.append("dnd in ");
				}
				sb.append("\r\n");
				if ((data04 & 0x2) == 0x2) {
					sb.append("mur in ");
				}
				sb.append("\r\n");
				if ((data04 & 0x10) == 0x10) {
					sb.append("emer in  ");
				}
				sb.append("\r\n");
				if ((data04 & 0x20) == 0x20) {
					sb.append("extEmer in ");
				}
				sb.append("------------ \r\n");
				n += 5;
				break;

			case (byte) 0x2:

				sb.append(String.format(" ts no=%d \r\n", data02));

				sb.append(String.format(" %d.%d / %d.%d \r\n", data05, data06, data03, data04));

				if ((data07 & 0x40) == 0x40)
					sb.append(String.format(" cool  \r\n", data02));
				else
					sb.append(String.format(" heat  \r\n", data02));

				if ((data07 & 0x80) == 0x80)
					sb.append(String.format(" h/m/l/o = %xx  \r\n", (data07 & 0x3)));
				else
					sb.append(String.format(" on/off = %x  \r\n", (data07 & 0x3)));

				sb.append("------------ \r\n");

				n += 8;
				break;

			case (byte) 0x03:

				sb.append(getLightChar((byte) ((data03 >> 0) & 0x3)));
				sb.append(getLightChar((byte) ((data03 >> 2) & 0x3)));
				sb.append(getLightChar((byte) ((data03 >> 4) & 0x3)));
				sb.append(getLightChar((byte) ((data03 >> 6) & 0x3)));

				sb.append(getLightChar((byte) ((data04 >> 0) & 0x3)));
				sb.append(getLightChar((byte) ((data04 >> 2) & 0x3)));
				sb.append(getLightChar((byte) ((data04 >> 4) & 0x3)));
				sb.append(getLightChar((byte) ((data04 >> 6) & 0x3)));

				sb.append(getLightChar((byte) ((data05 >> 0) & 0x3)));
				sb.append(getLightChar((byte) ((data05 >> 2) & 0x3)));
				sb.append(getLightChar((byte) ((data05 >> 4) & 0x3)));
				sb.append(getLightChar((byte) ((data05 >> 6) & 0x3)));

				sb.append(getLightChar((byte) ((data06 >> 0) & 0x3)));
				sb.append(getLightChar((byte) ((data06 >> 2) & 0x3)));
				sb.append(getLightChar((byte) ((data06 >> 4) & 0x3)));
				sb.append(getLightChar((byte) ((data06 >> 6) & 0x3)));

				sb.append("\r\n");
				sb.append("------------ \r\n");
				n += 7;
				break;

			case (byte) 0x04:

				sb.append(String.format("dim 0 = %x \r\n", ((data03 >> 0) & 0xf)));
				sb.append(String.format("dim 1 = %x \r\n", ((data03 >> 4) & 0xf)));

				sb.append(String.format("dim 2 = %x \r\n", ((data04 >> 0) & 0xf)));
				sb.append(String.format("dim 3 = %x \r\n", ((data04 >> 4) & 0xf)));

				sb.append(String.format("dim 4 = %x \r\n", ((data05 >> 0) & 0xf)));
				sb.append(String.format("dim 5 = %x \r\n", ((data05 >> 4) & 0xf)));

				sb.append(String.format("dim 6 = %x \r\n", ((data06 >> 0) & 0xf)));
				sb.append(String.format("dim 7 = %x \r\n", ((data06 >> 4) & 0xf)));

				sb.append("------------ \r\n");
				n += 7;
				break;

			case (byte) 0x05:

				sb.append(String.format("curtain 0 = %x \r\n", ((data03 >> 0) & 0xf)));
				sb.append(String.format("curtain 1 = %x \r\n", ((data03 >> 4) & 0xf)));

				sb.append(String.format("curtain 2 = %x \r\n", ((data04 >> 0) & 0xf)));
				sb.append(String.format("curtain 3 = %x \r\n", ((data04 >> 4) & 0xf)));

				sb.append(String.format("curtain 4 = %x \r\n", ((data05 >> 0) & 0xf)));
				sb.append(String.format("curtain 5 = %x \r\n", ((data05 >> 4) & 0xf)));

				sb.append(String.format("curtain 6 = %x \r\n", ((data06 >> 0) & 0xf)));
				sb.append(String.format("curtain 7 = %x \r\n", ((data06 >> 4) & 0xf)));

				sb.append("------------ \r\n");
				n += 7;
				break;

			case (byte) 0x20:// get setting

				sb.append(String.format("temp high limit = %d \r\n", data03));
				sb.append(String.format("temp low limit = %d \r\n", data04));

				n += 12;

				break;

			default:
				System.out.format("unknow code= %x \r\n", data01);
				n = rxLen;

				break;
			}

		} // while

		return sb.toString();
	}

}
