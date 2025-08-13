package home.lib.net.tms.mqtt3ssl.sup;

import home.lib.lang.UserException;
import home.lib.net.mq.MqBundle;
import home.lib.net.tms.mqtt3ssl.*;
import home.lib.util.TimeUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class MySocketSup implements IMqtt3 {

	Object m_obj = null;

	private String m_prefix = "___mysocketwrap___";

	MySocketSup _this = this;

	class Response {
		TimeUtil past = new TimeUtil();
		byte[] val = null;
		String key = null;

		public Response(String k, byte[] v) {
			val = v;
			key = k;
		}
	};

	// TmsChannel m_cl = null;

	MqttClient3 m_cl = null;

	Map<String, Response> m_response = new HashMap<String, Response>();

	IMySocketSupListener m_inter = null;

	String m_ip = null;

	int m_port = -1;

	Object m_linkedClassHandle = null;

	String m_path = "" + System.nanoTime();

	String m_clientId = "myclient" + System.nanoTime();

	// String m_pwd = "nopwd";

	// TmsBroker m_sel = null;

	// boolean m_selfCreated = false;

	// private TmsBroker m_brk = null;

	// private long m_cid = 0;

	// private int m_maxEventCount = 1024;

	// private TmsEventQueue m_eQueue = new TmsEventQueue(1024);
	//
	// public TmsEventQueue getEventQueue() {
	// return m_eQueue;
	// }

	// public SocketChannel getChannel() {
	// return m_cl.getChannel();
	// }

	public void setClientId(String s) {
		m_clientId = s;
	}

	public String getClientId() {
		return m_clientId;
	}

	public void setLinkedClassHandle(Object o) {
		m_linkedClassHandle = o;
	}

	public void setResendInterval(int n) {
		// no use
	}

	public void setUserObject(Object o) {
		m_obj = o;
	}

	public Object getUserObject() {
		// return m_cl.getChannel().getUserObject();
		return m_obj;
	}

	public void setMqSocketListener(IMySocketSupListener l) {
		m_inter = l;
	}

	public void setPrefix(String p) throws Exception {
		if (p == null || p.trim().length() == 0 || p.indexOf("/") != -1 || p.indexOf("#") != -1) {
			throw new UserException("prefix format error, # and / are not available");
		}

		m_prefix = p;
	}

	/**
	 * 
	 * @param br
	 * @param id
	 * @param ip
	 * @param port
	 * @param ltr
	 * @throws Exception
	 */
	public MySocketSup(Object br, String id, String ip, int port, IMySocketSupListener ltr) throws Exception {
		this("___mysocketwrap___", br, id, ip, port, ltr);
	}

	public MySocketSup(String _prefix, Object br, String id, String ip, int port, IMySocketSupListener ltr)
			throws Exception {

		if (_prefix != null) {
			if (_prefix.trim().length() > 0) {
				m_prefix = _prefix;
			}
		}
		// if (br != null)
		// throw new UserException("not support broker!");

		// m_bs = bs;
		//
		//

		if (ip == null)
			ip = "127.0.0.1";

		m_inter = ltr;
		m_ip = ip;
		m_port = port;

		// m_cl = new TmsChannel(m_bs, sel);
		// m_cl.setPath(id);

		m_path = id;

		m_clientId = id;

		// if (br != null) {
		//
		// m_selfCreated=false;
		//
		// m_sel = br;
		//
		// } else {

		// m_selfCreated = true;

		// m_brk = new TmsBroker("self created(local)", "0.0.0.0", 0);
		// // m_brk.jobOpt(8, 0);
		// // m_brk.jobPeekCount(8);
		// m_brk.setReceiveTimeoutSec(60);
		// // m_brk.setSocketBufferSize(1024*1024*10);
		// // m_brk.setLogger(this);
		// m_brk.waitBind(8);
		//
		// m_sel=m_brk;

		// }

	}

	/**
	 * 
	 * @return
	 */

	public Exception connect(long timeout, boolean keepConnection) throws Exception {
		return connect(timeout, keepConnection, "idname", "pwd");

	}

	public Exception connect(long timeout, boolean keepConnection, String idname, String pwd) throws Exception {

		// m_cl = m_sel.connectw(m_ip, m_port, keepConnection, m_path, this, idname, pwd, this, timeout).get();
		//
		// m_cid = m_cl.getId();
		//
		// TimeUtil t = new TimeUtil();
		// while (t.end_ms() < timeout) {
		// TimeUtil.sleep(10);
		// if (m_cl.isAlive())
		// return null;
		// }

		if (m_cl != null)
			throw new UserException("handle is not null");

		m_cl = new MqttClient3();

		m_cl.setId(m_clientId);
		m_cl.connect(m_ip, m_port, this);
		m_cl.waitForConnack(timeout);

		return null;
	}

	@Override
	public void onConnected(MqttClient3 source) {

		m_inter.connected(this);

		// debug("connnected " + ask.getId());

		// System.out.println("client conected " + source.getId() + " " + (connectedcnt++));

		int qos = 0;

		try {
			long pi = source.subscribe(m_prefix + "/#", qos, new IMqttSub3() {

				@Override
				public void onReceived(MqttClient3 source, String topic, byte[] payload) {

					// System.out.format("callback topic=[%s] payload.len=%d \r\n", topic, payload.length);

					String[] tl = topic.split("/");

					// String tt = prefix + "/resp/" + from + "/" + to + "/" + method + "/" + ns;

					if (tl.length >= 6) {
						String pf = tl[0];
						String type = tl[1];
						String from = tl[2];
						String to = tl[3];
						String method = tl[4];
						String ns = tl[5];

						if (pf.equals(m_prefix) && (m_path.equals(to) || to.equals("*"))) {

							if (type.equals("resp")) {

								synchronized (m_response) {
									for (Response r : m_response.values()) {
										if (r.past.end_sec() > 30) {
											m_response.remove(r.key);
											r.val = null;
										}
									}
								} // sync

								m_response.put(ns, new Response(ns, payload));

							} else if (type.equals("get") || type.equals("send")) {

								if (m_linkedClassHandle != null) {

									try {

										Object args[] = (Object[]) bytes2obj(payload);
										Object r = doMethod(m_linkedClassHandle, method, args);

										if (r != null && type.equals("get") && to.equals("*") == false) {

											String nt = pf + "/resp/" + to + "/" + from + "/" + method + "/" + ns;

											byte[] rbs = obj2bytes(r);

											source.publish(nt, rbs, 0);

										}
									} catch (Exception e) {
										// e.printStackTrace();
									}

								}
							} else if (type.equals("gettx") || type.equals("sendtx")) {

								try {
									MqBundle r = m_inter.actionPerformed(_this, MqBundle.From(payload));

									if (r != null && type.equals("gettx") && to.equals("*") == false) {

										String nt = pf + "/resp/" + to + "/" + from + "/__/" + ns;

										byte[] rbs = MqBundle.To(r); // obj2bytes(r);
										// String s = String.format("return to (%s/resp) relay q=%s %s", nt,
										// source.publishQueueSize(), payload.length);

										// System.out.println(s);

										source.publish(nt, rbs, 2);
									}
								} catch (Exception e) {
									e.printStackTrace();
								}

							}

						}
					} // length==6

				}

				@Override
				public void subscribeSuccess(MqttClient3 source, String topic) {

					// System.out.println("subscribed " + source.getId() + " " + topic);

				}
				// onSubackSuccess
				// onSubackFail

				@Override
				public void subscribeFailed(MqttClient3 source, String topic) {

					source.discon(false);

				}

			});

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void deliveryComplete(long pi, long past_ms) {

	}

	@Override
	public void onDisonnected(MqttClient3 source) {

		m_inter.disconnected(this);

		// debug("disconnnected ");

	}

	public void close(boolean keepConnection) {

		if (keepConnection) {

			// m_cl.getChannel().close();// just channel to close.

			// m_sel.close(m_cid);
			m_cl.discon(false);

		} else {
			// // m_cl.close();// close & remove keep connection
			// m_sel.remove(m_cid);
			// m_sel.close(m_cid);
			//
			// if (m_selfCreated) {
			// m_brk.cleanUp();
			// m_brk = null;
			// }
			m_cl.close();

		}

	}

	// public TmsBroker getSelfBroker() {
	// return m_brk;
	// }

	public String getId() {
		if (m_cl == null)
			return null;

		// return m_cl.getPath();
		return m_cl.getId();

	}

	public boolean isAlive() {
		if (m_cl == null)
			return false;

		return m_cl.isAlive();
	}

	public boolean isKeeping() {
		// return m_cl.getKeepConnection();
		return true;
	}

	// public long getKeepingInterval() {
	//
	// }
	//
	public void setKeeping(long interval) {

		// if (m_cl == null)
		// return;
		//
		// // m_cl.debug("setKeeping doesn't work in wrap class");
		// if (interval == 0)
		// m_cl.setKeepConnection(false);
		// else
		// m_cl.setKeepConnection(true);

		// alwasys keep!
	}

	/**
	 * 
	 * @param aThrowable
	 * @return
	 */

	// /**
	// *
	// * If the timeout_msec is zero it is not wait for return, otherwise it will wait for return within timeout_msec
	// * milleseconds.
	// *
	// * @param d
	// * bunddle of data
	// * @param timeout_msec
	// * @return null or return value
	// */
	// private MqBundle sendTo(MqBundle d, long timeout_msec) throws Exception {
	//
	// return sendTo(d.to, d, timeout_msec);
	//
	// }

	/**
	 * ( global = * )
	 * 
	 * @param to
	 * @param o
	 * @param timeout_ms
	 * @return
	 * @throws Exception
	 */
	synchronized public MqBundle sendTo(String to, MqBundle o, long timeout) throws Exception {
		// synchronized public MqBundle sendTo(String to, MqBundle o) throws Exception {

		if (m_cl == null)
			return null;

		if (m_cl.isAlive() == false)
			return null;

		if (to.trim().equals("*")) {
			to = "*";
			timeout = 0;
		}

		o.from = m_cl.getId();
		o.to = to;

		String ns = "" + System.nanoTime();

		String from = m_path;

		if (timeout == 0) {

			m_cl.publish(m_prefix + "/sendtx/" + from + "/" + to + "/__/" + ns, MqBundle.To(o), 0);
		} else {

			m_response.clear();

			m_cl.publish(m_prefix + "/gettx/" + from + "/" + to + "/__/" + ns, MqBundle.To(o), 2);

			TimeUtil t = new TimeUtil();

			while (t.end_ms() < timeout) {
				TimeUtil.sleep(10);

				synchronized (m_response) {

					Response r = m_response.remove(ns);

					if (r != null) {
						if (r.val == null)
							return null;

						return MqBundle.From(r.val);
					}
				} // sync

			}
		}

		return null;
	}

	/**
	 * 
	 * @param topic
	 * @param timeout
	 * @param method
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public synchronized Object callMethod(String topic, long timeout, String method, Object... args) throws Exception {

		String ns = "" + System.nanoTime();

		String from = m_path;

		String to = topic;

		if (to.trim().equals("*")) {
			to = "*";
			timeout = 0;
		}

		// m_inter.log(null, 0, String.format("callmethod(%s, ...)", tt));

		byte[] b = obj2bytes(args);

		if (timeout == 0) {

			m_cl.publish(m_prefix + "/send/" + from + "/" + to + "/" + method + "/" + ns, b, 0);

		} else {
			m_response.clear();

			m_cl.publish(m_prefix + "/get/" + from + "/" + to + "/" + method + "/" + ns, b, 2);

			TimeUtil t = new TimeUtil();

			while (t.end_ms() < timeout) {
				TimeUtil.sleep(10);

				synchronized (m_response) {

					Response r = m_response.remove(ns);

					if (r != null) {
						if (r.val == null)
							return null;

						return bytes2obj(r.val);
					}
				} // sync

			}
		}

		return null;

	}

	/**
	 * to='*'
	 * 
	 * @param o
	 * @throws Exception
	 */
	public void sendBroadcast(MqBundle o) throws Exception {

		sendTo("*", o, 0);

	}

	/**
	 * 
	 * @param ms
	 */
	public static void sleep(long ms) {

		TimeUtil.sleep(ms);
	}

	public Object callMethod(String to, long timeout, String method) throws Exception {
		Object[] arr = null;
		return callMethod(to, timeout, method, arr);

	}

	public Object callMethod(String to, String method, Object... args) throws Exception {
		return callMethod(to, (long) 8000, method, args);

	}

	@Override
	public String toString() {
		return "mybroker wrap+mqcoket (" + m_ip + ":" + m_port + ") " + m_cl.toString();
	}

	public void debug(String f, Object... p) {
		try {
			String s = String.format(f, p);
			System.out.println(TimeUtil.now() + " mysocket " + s);

			// if (m_sel != null) {
			// m_sel.debug(this, f, p);
			// }

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// public TmsBroker cli_bs() {
	// return m_sel;
	// }

	// public void setSocketBufferSize(long l) {
	//
	// m_sel.setSocketBufferSize(l);
	// }

	// public void setTimeout(long to) {
	//
	// m_sel.setReceiveTimeoutSec(to / 1000);
	//
	// }

	// @Override
	// public Object onAccepteded(TmsChannel ch) {
	// ch.setLoggedIn(true);
	// return null;
	// }
	//
	// @Override
	// public boolean onConnected(TmsChannel ch) {
	// doConnected(ch);
	//
	// return true;
	// }
	//
	// @Override
	// public void onDisconnected(TmsChannel ch) {
	// doDisconnected(ch);
	//
	// }
	//
	// @Override
	// public void onError(TmsChannel ch, byte[] rxd) {
	// try {
	// debug("%s", new String(rxd));
	// } catch (Exception ex) {
	// ex.printStackTrace();
	// }
	//
	// }
	//
	// @Override
	// public void onLink(TmsChannel ch) {
	//
	// ch.setLoggedIn(true);
	// }

	// @Override
	// public TmsItem onReceived(TmsChannel ch, byte[] rxd, TmsItem item) {
	// doReceived(ch, item, rxd);
	// return null;
	// }
	//
	// public TmsChannel getTmsChannel() {
	// return m_cl;
	// }

	/**
	 * 
	 * @param cls
	 * @param methodName
	 * @param args
	 * @return
	 * @throws Exception
	 */
	static Object doMethod(Object cls, String methodName, Object... args) throws Exception {

		Class c = cls.getClass();

		Method m = null;

		if (args != null && args.length != 0) {

			Class[] cArg = new Class[args.length];
			for (int h = 0; h < cArg.length; h++) {

				Class ac = args[h].getClass();

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
					cArg[h] = args[h].getClass();

			}

			m = c.getMethod(methodName, cArg);
		} else
			m = c.getMethod(methodName, null);

		return m.invoke(cls, args);

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

}
