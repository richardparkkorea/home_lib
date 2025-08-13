package home.lib.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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

public class BytesUtil {

	/**
	 *
	 *
	 * depreciate in value
	 *
	 * @param vals
	 *            Object[]
	 * @return byte[]
	 */
	@Deprecated
	public static byte[] getBytes(Object... vals) {
		// set recv
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);

			for (int i = 0; i < vals.length; i++) {
				Object v = vals[i];
				// System.out.println("type: "+v.getClass().toString());

				if (v instanceof Integer) {
					dos.writeInt((Integer) v);
				} else if (v instanceof Long) {
					dos.writeLong((Long) v);
				} else if (v instanceof Double) {
					dos.writeDouble((Double) v);
				} else if (v instanceof String) {
					dos.writeUTF((String) v);
				} else if (v instanceof byte[]) {
					// System.out.println("byte size: "+((byte[])v).length);
					dos.write((byte[]) v);
				} else {
					System.out.println("cast err:unknow type:" + v.getClass().toString());
					System.exit(0);
				}
			} // for

			// System.out.println("test-----:"+bos.size());
			return bos.toByteArray();
			// byte[] bs = bos.toByteArray();
			// sv2.setReturnValue(sk, bs, bs.length);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * 
	 * @param str
	 * @param fixLength
	 * @return
	 */
	@Deprecated
	public static byte[] getBytesWithFixLength(String str, int fixLength) {

		// set recv
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);

			dos.writeUTF((String) str);

			if (dos.size() < fixLength) {
				dos.write(new byte[fixLength - dos.size()]);
			}

			return bos.toByteArray();
			// byte[] bs = bos.toByteArray();
			// sv2.setReturnValue(sk, bs, bs.length);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

		// return java.util.Arrays.copyOf(str.getBytes(), fixLength);

		// return Arrays.copyOf( str.getBytes() , fixLength );

	}

	/**
	 * 
	 * @param first
	 * @param second
	 * @return
	 */
	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	/**
	 * 
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static ArrayList<byte[]> split(byte[] array, byte[] delimiter) {
		ArrayList<byte[]> byteArrays = new ArrayList<byte[]>();
		if (delimiter.length == 0) {
			return byteArrays;
		}
		int begin = 0;

		outer: for (int i = 0; i < array.length - delimiter.length + 1; i++) {
			for (int j = 0; j < delimiter.length; j++) {
				if (array[i + j] != delimiter[j]) {
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
}
