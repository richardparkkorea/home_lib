package home.lib.io;
/**
 * 
 * modbus crc code 
 * 
 * @author richard
 *
 */
public class Crc16  {
	

	/**
	 * modbus crc
	 * 
	 * @param crc
	 * @param data
	 * @return
	 */
	/* CRC algorithm */
	public static short calculate(short crc, short data) {
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
	public static short generate(byte[] data, int length) {
		short crc = (short) 0xFFFF;
		for (int i = 0; i < length; i++) {
			crc = calculate(crc, data[i]);
		}

		return crc;
	}
}