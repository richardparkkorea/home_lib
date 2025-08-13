package home.lib.net.nio;

import home.lib.lang.UserException;
import home.lib.util.DataStream;
import home.lib.util.ZipUtil;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class NqProperties {

	/**
	 * 
	 */
	final static int T_null = 0;
	final static int T_byte = 1;
	final static int T_int = 2;
	final static int T_long = 3;
	final static int T_double = 4;
	final static int T_string = 5;
	final static int T_byteArr = 6;
	final static int T_bool = 7;

	final static int VERSION = 0x20180001;

	/**
	 * 
	 */

	protected Map<String, Object> m_map = new HashMap<String, Object>();

	// public boolean result = false;//
	private int ver = VERSION;
	public String from = "";
	// public String msg = "";
	public String to = "";
	// public String remoteErrorMessage = null;// return Error Messsage set here
	// // internal use
	// public long privateWhenSend;
	// public long privateSendIndex = 0;
	// public byte privateCode = 's'; // 's'-send 'r'-response 'b'-broker
	// public String privateSocket = null;
	//

	//
	// temporary variables
	public boolean fromBroker = false;// broker use

	public NqProperties() {

		// privateWhenSend = System.currentTimeMillis();
	}

	/*
	 * 
	 * 
	 * 
	 * 
	 */
	public void setBoolean(String name, boolean b) {
		m_map.put(name, b);
	}

	public void setByte(String name, byte b) {
		m_map.put(name, b);
	}

	public void setInteger(String name, int b) {
		m_map.put(name, b);
	}

	public void setLong(String name, long b) {
		m_map.put(name, b);
	}

	public void setDouble(String name, double b) {
		m_map.put(name, b);
	}

	public void setString(String name, String b) {
		m_map.put(name, b);
	}

	public void setByteArray(String name, byte[] b) {
		m_map.put(name, b);
	}

	public void setObj(String name, Object o) throws Exception {
		m_map.put(name, obj2bytes(o));
	}

	public void setZip(String name, Object o) throws Exception {
		m_map.put(name, ZipUtil.zip(obj2bytes(o)));
	}

	/*
	 * 
	 * 
	 * 
	 */
	public boolean getBoolean(String name) {
		return (Boolean) m_map.get(name);
	}

	public byte getByte(String name) {
		return (Byte) m_map.get(name);
	}

	public int getInteger(String name) {
		return (Integer) m_map.get(name);
	}

	public long getLong(String name) {
		return (Long) m_map.get(name);
	}

	public double getDouble(String name) {
		return (Double) m_map.get(name);
	}

	public String getString(String name) {
		return (String) m_map.get(name);
	}

	public byte[] getByteArray(String name) {
		return (byte[]) m_map.get(name);
	}

	public Object getObj(String name) throws Exception {
		return bytes2obj((byte[]) m_map.get(name));
	}

	public Object getZip(String name) throws Exception {
		return bytes2obj(ZipUtil.unzip((byte[]) m_map.get(name)));
	}

	/**
	 * 
	 * @param object
	 * @return
	 * @throws IOException
	 */
	public static byte[] obj2bytes(Object object) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream(bos);
		out.writeObject(object);
		return bos.toByteArray();

	}

	/**
	 * 
	 * @param bytes
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Object bytes2obj(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInput in = new ObjectInputStream(bis);
		return in.readObject();

	}

	/**
	 * 
	 * @return
	 */
	public String[] getKeys() {

		synchronized (m_map) {
			int n = 0;
			if (m_map.size() == 0)
				return new String[0];

			String[] k = new String[m_map.size()];
			Iterator<Entry<String, Object>> it = m_map.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, Object> pair = it.next();
				k[n++] = pair.getKey().toString();
			}
			return k;
		}
	}

	public boolean containKey(String k) {
		synchronized (m_map) {
			int n = 0;

			Iterator<Entry<String, Object>> it = m_map.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, Object> pair = it.next();
				if (pair.getKey().toString().equals(k) == true)
					return true;
			}
			return false;
		}
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */

	public byte[] toBytes() throws Exception {
		return toBytes(this);
	}

	public static byte[] toBytes(NqProperties o) throws Exception {

		DataStream ds = new DataStream();

		// ds.writeBoolean(o.result);
		ds.write32(o.ver);
		ds.writeString(o.from);
		// ds.writeString(o.msg);
		ds.writeString(o.to);
		// ds.writeString(o.remoteErrorMessage);
		// ds.writeLong(o.privateWhenSend);
		// ds.writeLong(o.privateSendIndex);
		// ds.writeByte(o.privateCode);
		// ds.writeString(o.privateSocket);
		//
		//
		ds.writeInt(o.m_map.size());

		Iterator<Entry<String, Object>> it = o.m_map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = it.next();

			if (pair.getValue() == null) {

				ds.writeByte((byte) T_null);// type
				ds.writeString((String) pair.getKey());

			} else if (pair.getValue() instanceof Boolean) {

				ds.writeByte((byte) T_bool);// type
				ds.writeString((String) pair.getKey());
				ds.writeBoolean((Boolean) pair.getValue());

			} else if (pair.getValue() instanceof Integer) {

				ds.writeByte((byte) T_int);// type
				ds.writeString((String) pair.getKey());
				ds.writeInt((Integer) pair.getValue());

			} else if (pair.getValue() instanceof Long) {

				ds.writeByte((byte) T_long);// type
				ds.writeString((String) pair.getKey());
				ds.writeLong((Long) pair.getValue());

			} else if (pair.getValue() instanceof Double) {

				ds.writeByte((byte) T_double);// type
				ds.writeString((String) pair.getKey());
				ds.writeDouble((Double) pair.getValue());

			} else if (pair.getValue() instanceof Byte) {

				ds.writeByte((byte) T_byte);// type
				ds.writeString((String) pair.getKey());
				ds.writeByte((Byte) pair.getValue());

			} else if (pair.getValue() instanceof String) {

				ds.writeByte((byte) T_string);// type
				ds.writeString((String) pair.getKey());
				ds.writeString((String) pair.getValue());

			} else if (pair.getValue() instanceof byte[]) {
				ds.writeByte((byte) T_byteArr);// type
				ds.writeString((String) pair.getKey());

				byte[] b = (byte[]) pair.getValue();
				ds.writeInt(b.length);
				ds.writeBytes(b, b.length);

			}
			// }

		}

		return ds.getBytes();
	}

	/**
	 * 
	 * 
	 * 
	 * @param b
	 * @return
	 */
	public static NqProperties fromBytes(byte[] b) throws Exception {

		NqProperties o = new NqProperties();

		DataStream ds = new DataStream(b);
		ds.setPos(0);

		o.ver = ds.read32();
		if (o.ver != VERSION) {
			throw new UserException("version error %x", o.ver);
		}
		o.from = ds.readString();
		// o.msg = ds.readString();
		o.to = ds.readString();
		// o.remoteErrorMessage = ds.readString();
		// o.privateWhenSend = ds.readLong();
		// o.privateSendIndex = ds.readLong();
		// o.privateCode = ds.readByte();
		// o.privateSocket = ds.readString();
		//
		//
		int size = ds.readInt();

		for (int i = 0; i < size; i++) {

			byte t = ds.readByte();
			String key = ds.readString();

			switch (t) {
			case T_null:
				o.m_map.put(key, null);
				break;
			case T_bool:
				o.m_map.put(key, ds.readBoolean());
				break;
			case T_byte:
				o.m_map.put(key, ds.readByte());
				break;
			case T_int:
				o.m_map.put(key, ds.readInt());
				break;
			case T_long:
				o.m_map.put(key, ds.readLong());
				break;
			case T_double:
				o.m_map.put(key, ds.readDouble());
				break;
			case T_string:
				o.m_map.put(key, ds.readString());
				break;
			case T_byteArr:
				int l = ds.readInt();
				o.m_map.put(key, ds.readBytes(l));
				break;

			}

		}

		return o;
	}

	// public static boolean memcmp(byte[] a, byte[] b, int len) {
	// for (int i = 0; i < len; i++) {
	// if (a[i] != b[i])
	// return false;
	// }
	// return true;
	// }

}