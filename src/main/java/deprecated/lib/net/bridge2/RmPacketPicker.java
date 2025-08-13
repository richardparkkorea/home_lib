package deprecated.lib.net.bridge2;

import home.lib.io.Crc32;
import deprecated.lib.net.bridge.CBundle;
import home.lib.util.DataStream;
import home.lib.util.TimeUtil;

//import java.util.zip.CRC32;
//import java.util.zip.Checksum;

//
//
public class RmPacketPicker {

	public static boolean m_debug=false; 

	public long MAX_BUFF_SIZE = 1024 * 8;// CTcpQueue2.BASE_BUFFER_SIZE;// 8M
	//

	private DataStream m_ds = new DataStream();
	public byte[] m_lastPull = null;

	private long m_lastErrorCode = 0;

	public TimeUtil m_lastCheck = new TimeUtil();

	public long getLastError() {
		return m_lastErrorCode;
	}

	public RmPacketPicker(long max) {
		MAX_BUFF_SIZE = max;
	}

 
	public int headSize() {
		// STX(1)
		// version(8)
		// from(8)
		// to(8)
		// data length(4)
		// data(n)
		// crc32(8)
		// ETX(1)
		return 1 + 8 + 4 + 8 + 1;// package size, except data length.
	}

	public static byte[] make(CBundle o) throws Exception {

		byte[] data = CBundle.To(o);

		DataStream ds = new DataStream();

		ds.writeByte((byte) '(');
		ds.writeLong(20141019);
		// ds.writeLong(o.from);
		// ds.writeLong(o.to);
		ds.writeInt(data.length);
		if (data.length != 0)
			ds.writeBytes(data, data.length);

		// Checksum checksum = new CRC32();
		Crc32 checksum = new Crc32();
		checksum.update(data, 0, data.length);
		long checksumValue = checksum.getValue();

		ds.writeLong(checksumValue);
		ds.writeByte((byte) ')');

		return ds.getBytes();
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
	}

	public int length() {
		return m_ds.length();
	}

 
	public CBundle check() {

		m_lastErrorCode = 0;

		if (m_ds.length() < headSize()) // wait til receive length field.
			return null;

		byte[] aa = m_ds.getBytes();

		DataStream r = new DataStream(aa);

		byte stx = r.readByte();
		if (stx != '(') {
			m_ds.reset(); // error
			m_lastErrorCode = 1;
			debug("CTcpPacketPicker::packet-check-err1 =>" + stx);
			return null;
		}

		long ver = r.readLong();
		// long from = r.readLong();
		// long to = r.readLong();
		//
		// //if (from == 0 || to == 0) {
		// if (from == 0x00) {
		// m_ds.reset();// error
		// m_lastErrorCode=2;
		// //System.out.println("packet-check-err2");
		// return null;
		// }

		int dataLen = r.readInt();
		if (dataLen < 0 || dataLen > MAX_BUFF_SIZE) {// 10M
			m_ds.reset(); // error
			debug("CTcpPacketPicker::packet-check-err1.1 (" + dataLen + ")");
			return null;
		}

		if (aa.length < r.getPos() + dataLen + 8 + 1) {
			return null;// wait for more receives
		}

		byte[] data = new byte[0];
		if (dataLen > 0)
			data = r.readBytes(dataLen);

		long crc = r.readLong();

		// Checksum checksum = new CRC32();
		Crc32 checksum = new Crc32();
		checksum.update(data, 0, data.length);
		if (checksum.getValue() != crc) {
			m_ds.reset();
			m_lastErrorCode = 3;
			debug("CTcpPacketPicker::packet-check-err3");
			return null;
		}

		byte etx = r.readByte();
		if (etx != ')') {
			m_ds.reset();
			m_lastErrorCode = 4;
			debug("CTcpPacketPicker::packet-check-err4");
			return null;
		}

		int pullLength = r.getPos();
		m_ds.pull(pullLength);// remove received
		// m_ds.pull( data.length+1+8+8+8+4+8+1);//remove received

		CBundle ke = null;
		try {
			ke = CBundle.From(data);
		} catch (Exception e) {
			e.printStackTrace();
			debug("CTcpPacketPicker::packet-check-err5");
			m_lastErrorCode = 5;
			return null;
		}
		return ke;

		// return m_ds.pull( data.length+1+8+8+8+4+8+1);//remove received

		// System.out.println("no err-success");

		// return KtsEvent.From(data);
	}

	/**
	 * 
	 * 
	 * @param s
	 */
	public static void debug(String s, Object... args) {
		if (m_debug == false)
			return;
		System.out.println(String.format(s, args));
	}

}
