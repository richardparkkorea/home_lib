package home.lib.net.tms;

import java.util.Arrays;

import home.lib.io.Crc32;
import home.lib.util.DataStream;
import home.lib.util.TimeUtil;

public class TmsItem {

	// final public static int VERSION=0x20180002;
	final public static int VERSION = 0x20201100;

	private int ver = VERSION;

	protected byte code = 0; // s-send, r-return, a-ack, l-link, e-error, i-check id

	protected long sendIdx = 0;

	protected long respIdx = 0;

	protected long when = 0;

	protected byte[] data = new byte[0];

	protected String from = "f";

	protected String to = "t";

	// protected long fromIdInBroker = 0;
	// bit0 - ask return
	protected long askAccepterId = 0;// it will set in the broker

	protected byte option = 0;

	protected String id;

	protected String pwd;

	// protected short resend=0;

	// protected short timeout=0;

	protected int qWeight = 0;// broker queue weight

	protected int resend = 0;//

	protected int timeout = 0;//

	final public static byte ASK_RETURN = 0x1;

	final public static byte EXACTLY_ONCE = 0x2;//use it to receive exactly once for a single packet from the sender, usually it works with get() funciton.
	
	//final public static byte SINGLE_TARGET = 0x4;

	public TmsItem() {
		when = System.currentTimeMillis();
	}

	/**
	 * 
	 * @param d
	 */
	public TmsItem(byte[] d) {

		when = System.currentTimeMillis();
		data = Arrays.copyOf(d, d.length);
	}

	public TmsItem(long sn, long w, String f, String t) {
		sendIdx = sn;
		when = w;
		from = f;
		to = t;
	}

	// /**
	// *
	// * @param md
	// * @throws Exception
	// */
	// public NioItem(NioBundle md) throws Exception {
	//
	// when = System.currentTimeMillis();
	// data = md.toBytes();
	// }

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
	 * @param b
	 */
	public void setData(byte[] b) {
		data = Arrays.copyOf(b, b.length);
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
	public static byte[] toBytes(TmsItem m) throws Exception {

		byte[] b = m.data;

		// make bytes
		DataStream d = new DataStream();

		d.write32(m.ver);// version(1)
		d.write32(0);// length reserve
		d.writeByte(m.code);//
		d.writeByte(m.option);//
		d.write64(m.sendIdx);//
		d.write64(m.respIdx);//
		d.write64(m.when);//
		d.write64(m.askAccepterId);//

		d.write32(m.qWeight);//
		d.write32(m.resend);//
		d.write32(m.timeout);//

		d.writeString(m.from);
		d.writeString(m.to);

		d.writeString(m.id);
		d.writeString(m.pwd);

		d.write32(b.length);// data length(4)

		if (b.length > 0) {
			d.writeBytes(b, b.length);// data..
		}

		DataStream.int_to(d.length() + 4, d.getBuf(), 4);// length + crc32

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
	public static TmsItem fromBytes(byte[] dd) throws Exception {
		// System.out.println( "dd= "+ StringUtil.ByteArrayToHex( dd)+" "+dd.length );

		TmsItem m = new TmsItem();
		DataStream d = new DataStream(dd);

		m.ver = d.read32();
		if (m.ver != VERSION) {
			// m_err++;
			debug("version err (%x)", m.ver);
			return null;
		}

		int length = d.read32();
		if (dd.length != length) {
			debug("length err ( %d != %d )", dd.length, length + 4);
			return null;
		}

		m.code = d.read8();
		m.option = d.read8();
		m.sendIdx = d.read64();
		m.respIdx = d.read64();
		m.when = d.read64();
		m.askAccepterId = d.read64();
		m.qWeight = d.read32();
		m.resend = d.read32();
		m.timeout = d.read32();

		m.from = d.readString();
		m.to = d.readString();

		m.id = d.readString();
		m.pwd = d.readString();

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
	public TmsItem copyOf() throws Exception {

		return fromBytes(toBytes());
	}

	/**
	 * 
	 * 
	 * @param s
	 */
	public static void debug(String s, Object... args) {

		System.out.println(TimeUtil.now() + " mqitem [" + String.format(s, args));
	}

	@Override
	public boolean equals(Object o) {

		if (o instanceof TmsItem) {

			TmsItem re = (TmsItem) o;

			if (this.sendIdx == re.sendIdx && this.when == re.when && this.from.equals(re.from)) {
				return true;
			}
		}

		return false;

	}

	public boolean isAskReturn() {
		return ((option & ASK_RETURN) == ASK_RETURN);
	}

	public boolean isExactlyOnce() {
		return ((option & EXACTLY_ONCE) == EXACTLY_ONCE);
	}
	
	
	
	
}