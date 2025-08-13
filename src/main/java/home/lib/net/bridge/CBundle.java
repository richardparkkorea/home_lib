package home.lib.net.bridge;

import home.lib.util.DataStream;
import home.lib.util.ZipUtil;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class CBundle {

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

	/**
	 * 
	 */

	private static final long serialVersionUID = 5346281904252724917L;

	protected Map<String, Object> m_map = new HashMap<String, Object>();

	public boolean result = false;//
	public String from = "";
	public String from_pwd = "";
	public String to = "";
	public String remote_error = null;// return Error Messsage set here
	// internal use
	public long privateWhenSend;
	public long privateSendIndex = 0;
	public byte privateCode = 's'; // 's'-send 'r'-response 'p'-post
	public String privateSocket = null;
	public boolean privateIsBroadcast = false;
	public boolean privateSetId = false;

	public CBundle() {

		privateWhenSend = System.currentTimeMillis();
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

	public void setSDouble(String name, double b) {
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
	 * @param o
	 * @return
	 */
	public static byte[] To(CBundle o) throws Exception {

		DataStream ds = new DataStream();

		// public boolean result = false;//
		ds.writeBoolean(o.result);
		// public String from = "";
		ds.writeString(o.from);
		// public String from_pwd = "";
		ds.writeString(o.from_pwd);
		// public String to = "";
		ds.writeString(o.to);
		// public String remote_error = "";// return Error Messsage set here
		ds.writeString(o.remote_error);
		// public long privateWhen;
		ds.writeLong(o.privateWhenSend);
		// public long privateSendIndex = 0;
		ds.writeLong(o.privateSendIndex);
		// public byte privateCode = 's'; // 's'-send 'r'-response 'p'-post
		ds.writeByte(o.privateCode);
		// public String privateSocket = null;
		ds.writeString(o.privateSocket);
		// public boolean privateIsBroadcast = false;
		ds.writeBoolean(o.privateIsBroadcast);
		// public boolean privateSetId = false;
		ds.writeBoolean(o.privateSetId);

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
	public static CBundle From(byte[] b) throws Exception {

		CBundle o = new CBundle();

		DataStream ds = new DataStream(b);
		ds.setPos(0);

		// public boolean result = false;//
		o.result = ds.readBoolean();
		// public String from = "";
		o.from = ds.readString();
		// public String from_pwd = "";
		o.from_pwd = ds.readString();
		// public String to = "";
		o.to = ds.readString();
		// public String remote_error = "";// return Error Messsage set here
		o.remote_error = ds.readString();
		// public long privateWhen;
		o.privateWhenSend = ds.readLong();
		// public long privateSendIndex = 0;
		o.privateSendIndex = ds.readLong();
		// public byte privateCode = 's'; // 's'-send 'r'-response 'p'-post
		o.privateCode = ds.readByte();
		// public String privateSocket = null;
		o.privateSocket = ds.readString();
		// public boolean privateIsBroadcast = false;
		o.privateIsBroadcast = ds.readBoolean();
		// public boolean privateSetId = false;
		o.privateSetId = ds.readBoolean();

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

	public static boolean memcmp(byte[] a, byte[] b, int len) {
		for (int i = 0; i < len; i++) {
			if (a[i] != b[i])
				return false;
		}
		return true;
	}

}