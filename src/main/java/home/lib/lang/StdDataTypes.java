package home.lib.lang;

final public class StdDataTypes {
	// public std_bitShifts() {
	// }

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

	/*
	 * 
	 * @param parValue
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

	/*
	 * 
	 * @param bi
	 * @return
	 * @throws PkFormatException
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

	/*
	 * 
	 * @param bi
	 * @return
	 * @throws PkFormatException
	 */
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

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	public static void testSample() {
		{
			byte[] b = null;
			double d = Double.MIN_VALUE;

			b = StdDataTypes.double_to(d);
			System.out.println("  " + d + "   " + StdDataTypes.to_double(b) + "  " + (StdDataTypes.to_double(b) == d));

			d = 0.0;
			b = StdDataTypes.double_to(d);
			System.out.println("  " + d + "   " + StdDataTypes.to_double(b) + "  " + (StdDataTypes.to_double(b) == d));

			d = Double.MAX_VALUE;

			b = StdDataTypes.double_to(d);
			System.out.println("  " + d + "   " + StdDataTypes.to_double(b) + "  " + (StdDataTypes.to_double(b) == d));

			System.exit(0);
		}

	}
}
