package home.lib.lang;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * DataStream is better to use! 
 * @author richard
 *
 */
@Deprecated
final public class StdStream {

	/**
	 * 
	 */
	final static public byte[] byteArr0 = new byte[0];

	private byte[] m_buf = null;
	private int m_usePos = 0; // use position
	private int m_useLen = 0;

	private Object lock = new Object();

	/**
	 * 
	 * @return
	 */
	@Deprecated
	public byte[] bufferHandle() {
		return m_buf;
	}

	public int length() {
		return m_useLen;
	}

	@Deprecated
	public void reset() {
		clear();
	}

	public void clear() {
		synchronized (lock) {
			m_buf = new byte[0];
			m_useLen = 0;
			m_usePos = 0;
		}
	}

	public int getPos() {
		return m_usePos;
	}

	public int setPos(int n) {
		return m_usePos;
	}

	public void movePos(int n) throws Exception {
		if (m_usePos + n > m_useLen) {
			// System.out.println(" std_stream2 : movePos err " + n);
			new Exception("movePos err " + (m_usePos + n) + "  " + m_useLen);
		}
		m_usePos += n;
	}

	/**
	 * use copyOf
	 * 
	 * @return
	 */
	@Deprecated
	public byte[] toByteArray() {
		return copyOf();
	}

	public byte[] copyOf() {
		synchronized (lock) {
			if (m_buf == null || m_useLen == 0)
				return null;

			return Arrays.copyOf(m_buf, m_useLen);
		}
	}

	/**
	 * 
	 * @param n
	 *            int
	 */
	private void realloc(int n) {

		if (n == 0) {
			return;
		}

		if (n < 0) {
			System.out.println("std_stream2 : allocation err!");

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

	/**
	 * write
	 */
	public StdStream() {

	}

	/**
	 * 
	 * @param b
	 *            byte[]
	 */
	// public void writeByteArr(byte... b) {
	// for (int i = 0; i < b.length; i++) {
	// writeByte(b[i]);
	// }
	// }

	public void writeByte(byte b) {
		synchronized (lock) {
			realloc(1);
			m_buf[m_usePos] = b;
			m_usePos += 1;
		}

	}

	public void writeInt32(int v) {
		synchronized (lock) {
			realloc(4);
			StdDataTypes.int_to(v, m_buf, m_usePos);
			m_usePos += 4;
		}

	}

	public void writeInt64(long v) {
		synchronized (lock) {
			realloc(8);
			StdDataTypes.long_to(v, m_buf, m_usePos);
			m_usePos += 8;
		}

	}

	public void writeDouble(double v) {
		synchronized (lock) {
			realloc(8);
			StdDataTypes.double_to(v, m_buf, m_usePos);
			m_usePos += 8;
		}

	}
	
	public void writeBytes(byte[] v ) {
		writeBytes(v,v.length);
	}

	public void writeBytes(byte[] v, int len) {
		synchronized (lock) {
			realloc(len);
			System.arraycopy(v, 0, m_buf, m_usePos, len);
			m_usePos += len;
		}
	}

	/*
	 * public void writeString(String s, int strlen) { byte[] b = Arrays.copyOf(s.getBytes(), 10); writeBytes(b,
	 * b.length); }
	 */
	public void writeString(String s) throws Exception {
		synchronized (lock) {
			// try {
			if (s.length() == 0) {
				writeInt32(0);
				return;
			}
			byte[] b = s.getBytes("UTF-8");

			writeInt32(b.length);
			writeBytes(b, b.length);

			// } catch (Exception e) {
			// e.printStackTrace();
			//
			// }
		}
	}

	public void writeObject(Object o) throws Exception {
		synchronized (lock) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(bos);
			out.writeObject(o);
			byte[] bs = bos.toByteArray();

			writeInt32(bs.length);
			writeBytes(bs, bs.length);

			out.close();
			bos.close();
		}
	}

	/**
	 * read
	 * 
	 * @param b
	 *            byte[]
	 */

	public StdStream(byte[] b) {
		m_buf = b;
		m_usePos = 0;
		m_useLen = b.length;
	}

	public byte readByte() {
		synchronized (lock) {
			byte v = m_buf[m_usePos];
			m_usePos += 1;
			return v;
		}
	}

	public int readInt32() {
		synchronized (lock) {
			int v = StdDataTypes.to_int(m_buf, m_usePos);
			m_usePos += 4;
			return v;
		}
	}

	public long readInt64() {
		synchronized (lock) {
			long v = StdDataTypes.to_long(m_buf, m_usePos);
			m_usePos += 8;
			return v;
		}
	}

	public double readDouble() {
		synchronized (lock) {
			double v = StdDataTypes.to_double(m_buf, m_usePos);
			m_usePos += 8;
			return v;
		}
	}

	public byte[] readBytes(int len) {
		synchronized (lock) {
			// System.out.println("read test: "+ m_buf.length +" "+ m_usePos +
			// " "+ len );
			byte[] v = Arrays.copyOfRange(m_buf, m_usePos, m_usePos + len);
			m_usePos += len;
			return v;
		}
	}

	/*
	 * public String readString(int strlen) { byte[] b = readBytes(strlen); return new String(b).trim(); }
	 */
	public String readString() throws Exception {
		synchronized (lock) {
			// try {
			int n = readInt32();
			if (n == 0)
				return "";
			byte[] b = readBytes(n);
			return new String(b, "UTF-8");
			// } catch (Exception e) {
			// e.printStackTrace();
			//
			// }
			// return "";
		}
	}

	public Object readObject() throws Exception {
		synchronized (lock) {
			int len = readInt32();
			byte[] bs = readBytes(len);

			ByteArrayInputStream bis = new ByteArrayInputStream(bs);
			ObjectInput in = new ObjectInputStream(bis);
			Object o = in.readObject();
			bis.close();
			in.close();

			return o;
		}
	}
	
	/**
	 * use copyOf
	 * @return
	 */
	@Deprecated
	public byte[] toBytes() {
		synchronized (lock) {
			// System.out.println("read test: "+ m_buf.length +" "+ m_usePos +
			// " "+ len );
			//return Arrays.copyOfRange(m_buf, 0, m_useLen);
			return copyOf();
		}
	}

	// /**
	// *
	// * @param olen
	// * int
	// * @return int
	// */
	// public int moveToFront(int olen) {
	//
	// if (m_useLen < olen) {
	// System.out.println("moveToFont err!" + olen + " ");
	//
	// return -2;
	// }
	// // memcpy(m_buf, m_buf + olen, (m_use_length - olen));
	// this.m_buf = Arrays.copyOfRange(m_buf, olen, m_buf.length);
	// m_usePos -= olen;
	// m_useLen -= olen;
	// // System.out.println("moveToFont !" + m_usePos+ " " + m_useLen );
	// return olen;
	// }

	public byte[] popFont(int olen) throws Exception {
		synchronized (lock) {

			if (m_useLen < olen) {
				// System.out.println("moveToFont err!" + olen + " ");
				new Exception("popFront Error  len( " + m_useLen + ")<" + olen);

				return byteArr0;
			}

			byte[] rb = Arrays.copyOfRange(m_buf, 0, olen);

			// memcpy(m_buf, m_buf + olen, (m_use_length - olen));
			m_buf = Arrays.copyOfRange(m_buf, olen, m_buf.length);
			m_usePos -= olen;
			m_useLen -= olen;
			// System.out.println("moveToFont !" + m_usePos+ " " + m_useLen );
			return rb;
		}
	}

	
	/**
	 * 
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static ArrayList<byte[]> split(byte[] array, byte[] delimiter)
	{
		ArrayList<byte[]> byteArrays = new ArrayList<byte[]>();
	   if (delimiter.length == 0)
	   {
	      return byteArrays;
	   }
	   int begin = 0;

	   outer: for (int i = 0; i < array.length - delimiter.length + 1; i++)
	   {
	      for (int j = 0; j < delimiter.length; j++)
	      {
	         if (array[i + j] != delimiter[j])
	         {
	            continue outer;
	         }
	      }

	      // If delimiter is at the beginning then there will not be any data.
	      if (begin != i)
	         byteArrays.add(Arrays.copyOfRange(array, begin, i));
	      begin = i + delimiter.length;
	   }

	   // delimiter at the very end with no data following?
	   if (begin != array.length)
	      byteArrays.add(Arrays.copyOfRange(array, begin, array.length));

	   return byteArrays;
	}
	
	
	/**
	 * 
	 * @param delimiter
	 * @return
	 */
	public ArrayList<byte[]> split( byte[] delimiter){
		
		return split( copyOf(), delimiter);
	}
	
	
}
