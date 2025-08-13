package deprecated.lib.net.mq2.dev_old;

import java.util.Arrays;

import home.lib.io.Crc32;
import home.lib.util.DataStream;

public class MqItem {

	
	final public static int VERSION=0x20180002;
	
	private int ver = VERSION;
	protected byte code = 0; // s-send r-return a-ack l-link e-error
	protected long sendIdx = 0;
	protected long respIdx = 0;
	protected long when = 0;
	protected byte[] data = new byte[0];
	protected String from = "f";
	protected String to = "t";

	// bit0 - ask return
	protected byte option = 0;
	final public static byte ASK_RETURN = 0x1;

	public MqItem() {
		when = System.currentTimeMillis();
	}

	/**
	 * 
	 * @param d
	 */
	public MqItem(byte[] d) {

		when = System.currentTimeMillis();
		data = Arrays.copyOf(d, d.length);
	}

	/**
	 * 
	 * @param md
	 * @throws Exception
	 */
	public MqItem(MqProperties md) throws Exception {

		when = System.currentTimeMillis();
		data = md.toBytes();
	}

	/**
	 * 
	 * @return
	 */
	public String getFrom() {
		return this.from;
	}

	/**
	 * 
	 * @return
	 */
	public String getTo() {
		return this.to;
	}

	/**
	 * 
	 * @return
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public MqProperties getProperties() throws Exception {
		return MqProperties.fromBytes(this.data);
	}

	/**
	 * 
	 * @param b
	 */
	public void setData(byte[] b) {
		data = Arrays.copyOf(b, b.length);
	}

	/**
	 * 
	 * @param md
	 * @throws Exception
	 */
	public void setProperties(MqProperties md) throws Exception {
		data = md.toBytes();
	}

	/**
	 * 
	 * @return
	 */
	public byte[] toBytes() throws Exception {
		return toBytes(this);

	}

	/**
	 * 
	 * @param m
	 * @return
	 */
	public static byte[] toBytes(MqItem m) throws Exception {

		byte[] b = m.data;

		// make bytes
		DataStream d = new DataStream();

		d.write32(m.ver);// version(1)
		d.writeByte(m.code);//
		d.writeByte(m.option);//
		d.write64(m.sendIdx);//
		d.write64(m.respIdx);//
		d.write64(m.when);//
		d.writeString(m.from);
		d.writeString(m.to);

		d.write32(b.length);// data length(4)

		if (b.length > 0) {
			d.writeBytes(b, b.length);// data..
		}

		// make crc
		// get crc32
		Crc32 checksum = new Crc32();
		checksum.update(d.getBuf(), 0, d.length());
		long checksumValue = checksum.getValue();

		// get crc16(modbus)
		// short crc = MqDle.mbGenerateCRC(d.getBuf(), d.length() );

		// d.write16(crc);// crc(2)
		d.write32((int) checksumValue);// crc(4)

		// do dle convert

		return d.copyOf();

	}

	/**
	 * 
	 * @param dd
	 * @return
	 */
	public static MqItem fromBytes(byte[] dd) throws Exception {
		// System.out.println( "dd= "+ StringUtil.ByteArrayToHex( dd)+" "+dd.length );

		MqItem m = new MqItem();
		DataStream d = new DataStream(dd);

		m.ver = d.read32();
		if (m.ver != VERSION ) {
			// m_err++;
			debug("version err");
			return null;
		}

		m.code = d.read8();
		m.option = d.read8();
		m.sendIdx = d.read64();
		m.respIdx = d.read64();
		m.when = d.read64();
		m.from = d.readString();
		m.to = d.readString();

		int len = d.read32();
		if (len < 0 || len > (1024 * 1024 * 32)) { // 0~32m
			// m_err++;
			throw new Exception("length err " + len);
			// return null;
		}

		if (len != 0) {
			m.data = d.readBytes(len);
		}

		// make crc

		Crc32 checksum = new Crc32();
		checksum.update(dd, 0, d.getPos());
		long checksumValue = checksum.getValue();

		// short crc16 = d.read16();

		long crc32 = (long) d.read32() & 0xffffffffL;

		if (checksumValue != crc32) {
			// m_err++;
			debug(String.format("crc32 err  (%x!=%x)", checksumValue, crc32));
			return null;
		}

		return m;

	}
	
	
	/**
	 * 
	 * 
	 * @return
	 * @throws Exception
	 */
	public MqItem copyOf() throws Exception {

		return fromBytes(toBytes());
	}	

	/**
	 * 
	 * 
	 * @param s
	 */
	public static void debug(String s, Object... args) {

		System.out.println("mqitem [" + String.format(s, args));
	}

}