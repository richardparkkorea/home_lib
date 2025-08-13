package deprecated.stable1.lib.net.mq.old0;

import home.lib.io.Crc32;
 
import home.lib.util.DataStream;
import home.lib.util.TimeUtil;

//import java.util.zip.CRC32;
//import java.util.zip.Checksum;

//
//
public class MqPacketPicker {
	
 
	public   boolean m_debug=true; 
	
	final public static long PACKET_VERSION=20180323;

	public long MAX_BUFF_SIZE = 1024 * 8;// CTcpQueue2.BASE_BUFFER_SIZE;// 8M
	
	public long m_err=0;//
	/**
	 * 
	 */
	public TimeUtil lastRxTime=new TimeUtil(); 
 	//

	private DataStream m_ds = new DataStream();
	public byte[] m_lastPull = null;

	private long m_lastErrorCode = 0;

	public TimeUtil m_lastCheck = new TimeUtil();

	public long getLastError() {
		return m_lastErrorCode;
	}

	public MqPacketPicker(long max) {
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

	public static byte[] make(MqBundle o) throws Exception {

		byte[] data = MqBundle.To(o);

		DataStream ds = new DataStream();

		ds.writeByte((byte) '(');
		ds.writeLong(PACKET_VERSION);
 
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

	 
	/**
	 * 
	 * @return
	 */
	public MqBundle check() {

		m_lastErrorCode = 0;

		if (m_ds.length() < headSize()) // wait til receive length field.
			return null;

		byte[] aa = m_ds.getBytes();

		DataStream r = new DataStream(aa);

		byte stx = r.readByte();
		if (stx != '(') {
			m_ds.reset(); // error
			m_lastErrorCode = 1;
			debug("MqPacketPicker(stx err)=>" + stx);
			m_err++;
			return null;
		}

		long ver = r.readLong();
		
		if( ver!=PACKET_VERSION) {
			debug("MqPacketPicker(version err)=>" + ver);
			m_err++;
			m_ds.reset(); // error
 			return null;
			
		}
	 

		int dataLen = r.readInt();
		if (dataLen < 0 || dataLen > MAX_BUFF_SIZE) {//  
			m_ds.reset(); // error
			debug("MqPacketPicker(datalen err)=>" + dataLen + " max="+MAX_BUFF_SIZE);
			m_err++;
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
			debug("MqPacketPicker(crc err)");
			m_err++;
			return null;
		}

		byte etx = r.readByte();
		if (etx != ')') {
			m_ds.reset();
			m_lastErrorCode = 4;
			debug("MqPacketPicker(etx err)");
			m_err++;
			return null;
		}

		int pullLength = r.getPos();
		m_ds.pull(pullLength);// remove received
		 

		MqBundle ke = null;
		try {
			ke = MqBundle.From(data);
		} catch (Exception e) {
			e.printStackTrace();
			debug("MqPacketPicker(bundle unpack err)");
			m_lastErrorCode = 5;
			m_err++;
			return null;
		}
		
		
		//
		lastRxTime.start();
		
		
		return ke;
 
	}

	/**
	 * 
	 * 
	 * @param s
	 */
	public   void debug(String s, Object... args) {
		if (m_debug == false)
			return;
		System.out.println(String.format(s, args));
	}

}
