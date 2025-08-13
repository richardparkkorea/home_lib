package home.lib.net.tms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;



public class TmsFunc implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3957739774016707506L;
	public String func;
	public Object[] req;
	public Object resp;

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
	 * @param tc
	 * @param to
	 * @param timeout
	 * @param func
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public static Object callMethod(TmsChannel tc, String to, long timeout, String func, Object... params)
			throws Exception {

		TmsFunc t = new TmsFunc();
		t.func = func;
		t.req = params;
		
		if (to.contains("*"))
			timeout=0;//return immediately

		

		byte[] r = tc.get(to, obj2bytes(t), timeout);


		if (r != null) {
			Object o = bytes2obj(r);
			if (o instanceof TmsFunc) {
				return ((TmsFunc) o).resp;
			}
		}

		return null;

	}
	

	/**
	 * 
	 * @param cls
	 * @param tc
	 * @param rx
	 * @param e
	 * @return
	 * @throws Exception
	 */
	public static boolean doMethod(Object cls, TmsChannel tc, byte[] rx, TmsItem e) throws Exception {

		TmsFunc t = null;

		if (rx == null)
			return false;

		try {

			Object o = bytes2obj(rx);

			t = (TmsFunc) o;
		} catch (Exception ex) {
			return false;
		}

		Class c = cls.getClass();

		Method m = null;

		if (t.req != null && t.req.length != 0) {

			Class[] cArg = new Class[t.req.length];
			for (int h = 0; h < cArg.length; h++) {

				Class ac = t.req[h].getClass();

				if (ac.equals(Long.class))
					cArg[h] = long.class;
				else if (ac.equals(Integer.class))
					cArg[h] = int.class;
				else if (ac.equals(Double.class))
					cArg[h] = double.class;
				else if (ac.equals(Byte.class))
					cArg[h] = byte.class;
				else if (ac.equals(Short.class))
					cArg[h] = short.class;
				else
					cArg[h] = t.req[h].getClass();

			}

			m = c.getMethod(t.func, cArg);
		} else
			m = c.getMethod(t.func, null);

		Object resp = m.invoke(cls, t.req);

		if (e.isAskReturn()) {
			t.resp = resp;
			tc.sendReturn(e, obj2bytes(t));
		}

		return true;
	}

}
