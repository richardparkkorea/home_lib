package home.lib.util;

import java.util.Arrays;

import home.lib.lang.UserException;

/**
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
 */

final public class DataStream {

	/**
	 * 
	 */
	final static public byte[] byteArr0 = new byte[0];

	private byte[] m_buf = null;
	private int m_usePos = 0; // use position
	private int m_useLen = 0;
	
	public int MaxBufferSize=(1024*1024*100);//100m

	/**
	 * 
	 * 
	 * 
	 * 
	 * @return
	 */

	/**
	 * reference internal buffer
	 * 
	 * @return
	 */
	//
	@Deprecated
	public byte[] getBuf() {
		return m_buf;
	}	
	
	public byte[] nativeBuf() {
		return m_buf;
	}
	// private byte[] internalBuf() {
	// return m_buf;
	// }
	//

	public int length() {
		return m_useLen;
	}

	public void reset() {
		m_buf = new byte[0];
		m_useLen = 0;
		m_usePos = 0;
	}

	public int getPos() {
		return m_usePos;
	}

	//
	public int setPos(int n) {
		return m_usePos;
	}

	public byte byteAt(int n) {
		return m_buf[n];
	}
	//
	// private void movePos(int n) throws Exception {
	// if (m_usePos + n > m_useLen) {
	// new Exception("DataStream.movePos err " + (m_usePos + n) + " " + m_useLen);
	// }
	// m_usePos += n;
	// }
 
	 

	private void realloc(int n) {

		if (n == 0) {
			return;
		}

		if (m_buf == null) {
			m_buf = new byte[n];

			m_useLen = n;
			return;
		}

		if (m_useLen < m_usePos + n) {
			if (m_buf.length < m_usePos + n) {
				m_buf = Arrays.copyOf(m_buf, m_usePos + n + 1024);
			}

			m_useLen = m_usePos + n;
			return;
		}

	}

	@Deprecated
	public byte[] pull(int olen) {
		return pollFirst(olen);
	}

	public byte[] pollFirst(int olen) {

		if (olen > m_useLen)
			olen = m_useLen;

		byte[] rb = Arrays.copyOfRange(m_buf, 0, olen);

		m_buf = Arrays.copyOfRange(m_buf, olen, m_buf.length);
		m_usePos -= olen;
		m_useLen -= olen;
		// System.out.println("moveToFont !" + m_usePos+ " " + m_useLen );
		return rb;
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	public DataStream() {

	}

	/**
	 * add date: 150720
	 * 
	 * @param b
	 */
	public DataStream writeBoolean(boolean b) {

		if (b)
			writeByte((byte) 1);
		else
			writeByte((byte) 0);

		return this;
	}

	public DataStream writeByte(byte b) {
		write8(b);

		return this;
	}

	public DataStream write8(byte b) {

		realloc(1);
		m_buf[m_usePos] = b;
		m_usePos += 1;

		return this;
	}

	public DataStream writeShort(short s) {
		write16(s);
		return this;
	}

	public DataStream write16(short s) {

		realloc(2);
		short_to(s, m_buf, m_usePos);
		m_usePos += 2;

		return this;
	}

	public DataStream writeInt(int v) {
		write32(v);

		return this;
	}

	public DataStream write32(int v) {
		realloc(4);
		int_to(v, m_buf, m_usePos);
		m_usePos += 4;

		return this;

	}

	public DataStream writeLong(long v) {
		write64(v);

		return this;
	}

	public DataStream write64(long v) {
		realloc(8);
		long_to(v, m_buf, m_usePos);
		m_usePos += 8;

		return this;
	}

	public DataStream writeDouble(double v) {
		realloc(8);
		double_to(v, m_buf, m_usePos);
		m_usePos += 8;

		return this;
	}

	public DataStream writeBytes(byte[] v, int len) {

		if (v == null || len == 0)
			return this;

		realloc(len);
		System.arraycopy(v, 0, m_buf, m_usePos, len);
		m_usePos += len;

		return this;
	}

	public DataStream writeBytes(byte[] v) {
		if (v == null)
			return this;
		writeBytes(v, v.length);

		return this;
	}
	
	
	
	//v1.2
	public DataStream writeFloat(float v) {
		realloc(4);
		float_to(v, m_buf, m_usePos);
		m_usePos += 4;

		return this;

	}

	

	
	
	public DataStream writeShorts(short[] v) {
		if (v == null)
			return this;
		
		
		for(int i=0;i<v.length;i++)
			writeShort(v[i]);
		

		return this;
	}

	
	
	public DataStream writeInts(int[] v) {
		if (v == null)
			return this;
		
		
		for(int i=0;i<v.length;i++)
			writeInt(v[i]);
		

		return this;
	}
	
	public DataStream writeFloats(float[] v) {
		if (v == null)
			return this;
		
		
		for(int i=0;i<v.length;i++)
			writeFloat(v[i]);
		

		return this;
	}
	
	
	public DataStream writeLongs(long[] v) {
		if (v == null)
			return this;
		
		
		for(int i=0;i<v.length;i++)
			writeLong(v[i]);
		

		return this;
	}
	
	public DataStream writeDoubles(double[] v) {
		if (v == null)
			return this;
		
		
		for(int i=0;i<v.length;i++)
			writeDouble(v[i]);
		

		return this;
	}
	/*
	 * public void writeString(String s, int strlen) { byte[] b = Arrays.copyOf(s.getBytes(), 10); writeBytes(b,
	 * b.length); }
	 */
	public DataStream writeString(String s) throws Exception {
		// try {
		if (s == null) {
			writeInt(-1);
			return this;
		}
		if (s.length() == 0) {
			writeInt(0);
			return this;
		}
		byte[] b = s.getBytes("UTF-8");

		writeInt(b.length);
		writeBytes(b, b.length);

		// } catch (Exception e) {
		// e.printStackTrace();
		//
		// }
		return this;
	}

	public void writes(Object... args) throws Exception {

		for (Object o : args) {

			// System.out.println( o.getClass().getTypeName() );

			if (o instanceof Byte) {
				writeByte((byte) o);
			} else if (o instanceof Short) {
				writeShort((short) o);
			} else if (o instanceof Integer) {
				writeInt((int) o);
			} else if (o instanceof Long) {
				writeLong((long) o);
			} else if (o instanceof Double) {
				writeDouble((double) o);
			} else if (o instanceof String) {
				writeString((String) o);
			} else if (o instanceof byte[]) {
				writeBytes((byte[]) o);
			} else {
				throw new UserException("not support type=" + o.getClass().getTypeName());
			}

		} // for
	}

	@Deprecated
	public byte[] getBytes() {
		// return Arrays.copyOfRange(m_buf, 0, m_useLen);
		return copyOf();
	}

	public byte[] copyOf() {
		return Arrays.copyOfRange(m_buf, 0, m_useLen);
	}

	public int indexOf(byte key) {
		// return Arrays.binarySearch(m_buf, 0, m_useLen, key);
		for (int h = 0; h < m_useLen; h++) {
			if (m_buf[h] == key)
				return h;
		}
		return -1;

	}

	// public DataStream(byte[] buf) {
	// m_buf = buf;
	// m_usePos = 0;
	// m_useLen = buf.length;
	// }

	public DataStream(byte[] ItWillCopy) {
		m_buf = Arrays.copyOf(ItWillCopy, ItWillCopy.length);
		m_usePos = 0;
		m_useLen = ItWillCopy.length;
	}

	/**
	 * add date: 140720
	 * 
	 * @return
	 */
	public boolean readBoolean() {

		byte b = readByte();
		if (b == 1)
			return true;
		else
			return false;

	}

	public byte readByte() {
		return read8();
	}

	public byte read8() {
		byte v = m_buf[m_usePos];
		m_usePos += 1;
		return v;
	}

	public short readShort() {
		return read16();
	}

	public short read16() {
		short v = to_short(m_buf, m_usePos);
		m_usePos += 2;
		return v;
	}

	public int readInt() {
		return read32();
	}

	public int read32() {
		int v = to_int(m_buf, m_usePos);
		m_usePos += 4;
		return v;
	}

	public long readLong() {
		return read64();
	}

	public long read64() {
		long v = to_long(m_buf, m_usePos);
		m_usePos += 8;
		return v;
	}

	public double readDouble() {
		double v = to_double(m_buf, m_usePos);
		m_usePos += 8;
		return v;
	}

	public byte[] readBytes(int len) {
		// System.out.println("read test: "+ m_buf.length +" "+ m_usePos +
		// " "+ len );
		if (len == 0)
			return new byte[0];

		byte[] v = Arrays.copyOfRange(m_buf, m_usePos, m_usePos + len);
		m_usePos += len;
		return v;
	}
	
	
	//v1.2
	public float readFloat() {
		float v = to_float(m_buf, m_usePos);
		m_usePos += 4;
		return v;
	}

	
	
	
	
	public short[] readShorts(int len) throws Exception {

		short[] res=new short[len];
		for( int i=0;i<len;i++) {
			res[i]=readShort();
		}
		
		return res;
	}
	
	
	public int[] readInts(int len) throws Exception {

		int[] res=new int[len];
		for( int i=0;i<len;i++) {
			res[i]=readInt();
		}
		
		return res;
	}
	
	

	public float[] readFloats(int len) throws Exception {

		float[] res=new float[len];
		for( int i=0;i<len;i++) {
			res[i]=readFloat();
		}
		
		return res;
	}
	
	
	public long[] readLongs(int len) throws Exception {

		long[] res=new long[len];
		for( int i=0;i<len;i++) {
			res[i]=readLong();
		}
		
		return res;
	}
	
	
	public double[] readDoubles(int len) throws Exception {

		double[] res=new double[len];
		for( int i=0;i<len;i++) {
			res[i]=readDouble();
		}
		
		return res;
	}
	
	
	
	
	

	public String readString() throws Exception {
		return readString("UTF-8");
	}

	public String readString(String encode) throws Exception {
		// try {
		int n = readInt();
		if (n == -1)
			return null;
		if (n == 0)
			return new String("");

		if (n > (m_buf.length - m_usePos))// 10m
			throw new UserException("length error = %d  str.len=%d", n, (m_buf.length - m_usePos));

		byte[] b = readBytes(n);
		return new String(b, encode);
		// } catch (Exception e) {
		// e.printStackTrace();
		//
		// }
		// return "";
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	/**
	 * 
	 * @param parValue
	 * @return
	 */

	static public byte[] int_to(int parValue, byte retValue[], int n) {
		retValue[n + 0] = (byte) (parValue & 0xFF);
		retValue[n + 1] = (byte) ((parValue >> 8) & 0xFF);
		retValue[n + 2] = (byte) ((parValue >> 16) & 0xFF);
		retValue[n + 3] = (byte) ((parValue >> 24) & 0xFF);

		return retValue;
	}

	static public byte[] int_to(int parValue) {
		return int_to(parValue, new byte[4], 0);
	}

	/**
	 * 
	 * @param parValue
	 * @param retValue
	 * @param n
	 * @return
	 */
	static public byte[] short_to(short parValue, byte retValue[], int n) {
		retValue[n + 0] = (byte) (parValue & 0xFF);
		retValue[n + 1] = (byte) ((parValue >> 8) & 0xFF);

		return retValue;
	}

	static public byte[] short_to(short parValue) {
		return short_to(parValue, new byte[2], 0);
	}

	/**
	 * 
	 * @param parValue
	 * @return
	 */
	static public byte[] long_to(long parValue) {
		return long_to(parValue, new byte[8], 0);
	}

	static public byte[] long_to(long parValue, byte retValue[], int p) {

		retValue[p + 0] = (byte) (parValue & 0xFF);
		retValue[p + 1] = (byte) ((parValue >> 8) & 0xFF);
		retValue[p + 2] = (byte) ((parValue >> 16) & 0xFF);
		retValue[p + 3] = (byte) ((parValue >> 24) & 0xFF);

		retValue[p + 4] = (byte) ((parValue >> 32) & 0xFF);
		retValue[p + 5] = (byte) ((parValue >> 40) & 0xFF);
		retValue[p + 6] = (byte) ((parValue >> 48) & 0xFF);
		retValue[p + 7] = (byte) ((parValue >> 56) & 0xFF);

		return retValue;
	}

	/**
	 * 
	 * @param dbValue
	 * @return
	 */
	static public byte[] double_to(double dbValue) {
		return double_to(dbValue, new byte[8], 0);
	}

	static public byte[] double_to(double dbValue, byte retValue[], int p) {

		long parValue = Double.doubleToLongBits(dbValue);

		retValue[p + 0] = (byte) (parValue & 0xFF);
		retValue[p + 1] = (byte) ((parValue >> 8) & 0xFF);
		retValue[p + 2] = (byte) ((parValue >> 16) & 0xFF);
		retValue[p + 3] = (byte) ((parValue >> 24) & 0xFF);

		retValue[p + 4] = (byte) ((parValue >> 32) & 0xFF);
		retValue[p + 5] = (byte) ((parValue >> 40) & 0xFF);
		retValue[p + 6] = (byte) ((parValue >> 48) & 0xFF);
		retValue[p + 7] = (byte) ((parValue >> 56) & 0xFF);

		return retValue;
	}

	static public byte[] float_to(float dbValue) {
		return float_to(dbValue, new byte[4], 0);
	}

	static public byte[] float_to(float dbValue, byte retValue[], int p) {

		int parValue = Float.floatToIntBits(dbValue);

		retValue[p + 0] = (byte) (parValue & 0xFF);
		retValue[p + 1] = (byte) ((parValue >> 8) & 0xFF);
		retValue[p + 2] = (byte) ((parValue >> 16) & 0xFF);
		retValue[p + 3] = (byte) ((parValue >> 24) & 0xFF);

		return retValue;
	}

	/**
	 * 
	 * @param bi
	 * @return
	 */
	static public int to_int(byte[] bi, int p) {

		int nbisize = (((int) bi[3 + p] & 0xff) << 24) | (((int) bi[2 + p] & 0xff) << 16)
				| (((int) bi[1 + p] & 0xff) << 8) | (int) bi[0 + p] & 0xff;

		return nbisize;
	}

	static public int to_int(byte[] bi) {
		return to_int(bi, 0);
	}

	static public short to_short(byte[] bi, int p) {

		short nbisize = (short) ((((short) bi[1 + p] & 0xff) << 8) | (short) bi[0 + p] & 0xff);

		return nbisize;
	}

	static public short to_short(byte[] bi) {
		return to_short(bi, 0);
	}

	/**
	 * 
	 * @param bi
	 * @param p
	 * @return
	 */
	static public long to_long(byte[] bi, int p) {

		long nbisize = (((long) bi[7 + p] & 0xff) << 56) | (((long) bi[6 + p] & 0xff) << 48)
				| (((long) bi[5 + p] & 0xff) << 40) | (((long) bi[4 + p] & 0xff) << 32)
				| (((long) bi[3 + p] & 0xff) << 24) | (((long) bi[2 + p] & 0xff) << 16)
				| (((long) bi[1 + p] & 0xff) << 8) | (long) bi[0 + p] & 0xff;
		return nbisize;
	}

	static public long to_long(byte[] bi) {
		return to_long(bi, 0);
	}

	static public double to_double(byte[] bi, int p) {

		long nbisize = (((long) bi[7 + p] & 0xff) << 56) | (((long) bi[6 + p] & 0xff) << 48)
				| (((long) bi[5 + p] & 0xff) << 40) | (((long) bi[4 + p] & 0xff) << 32)
				| (((long) bi[3 + p] & 0xff) << 24) | (((long) bi[2 + p] & 0xff) << 16)
				| (((long) bi[1 + p] & 0xff) << 8) | (long) bi[0 + p] & 0xff;

		return Double.longBitsToDouble(nbisize);
	}

	static public double to_double(byte[] bi) {
		return to_double(bi, 0);
	}

	static public float to_float(byte[] bi, int p) {

		int nbisize = (((int) bi[3 + p] & 0xff) << 24) | (((int) bi[2 + p] & 0xff) << 16)
				| (((int) bi[1 + p] & 0xff) << 8) | (int) bi[0 + p] & 0xff;

		return Float.intBitsToFloat(nbisize);
	}

	static public double to_float(byte[] bi) {
		return to_float(bi, 0);
	}

}
