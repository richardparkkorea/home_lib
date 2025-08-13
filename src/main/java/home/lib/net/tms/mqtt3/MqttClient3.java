package home.lib.net.tms.mqtt3;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import home.lib.lang.Timer2;
import home.lib.lang.Timer2Task;
import home.lib.lang.UserException;
import home.lib.log.MyLogger;
import home.lib.net.tms.TmsItem;
import home.lib.net.tms.TmsSelectorListener;
import home.lib.util.DataStream;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

//
//  C - clent   : myloop work in createTimer
//  A - accept
//  B - broker  : clients.myloop work in broker.myloop
//
//
//                                (     broker        ) 
//  C ------------------------------A             A-------------------------------C
//
//  (publish)
//  
//  qos 1  
//
//  publish ----->
//
//                          <----puback       
//  
//
//                                          (subscribe)
//                                             publish  --->
//                                                                           <----puback       
//
//

//
//
//
//(publish)
//
//qos 2  
//
//publish ----->
//
//                    <----pubrecv       
//
//pubrel ----->
//
//                    <----pubcomp       
//
//
//
//                                      (subscribe)
//                                           qos 2  
//
//                                           publish ----->
//
//                                                                          <----pubrecv       
//
//                                           pubrel ----->
//
//                                                                          <----pubcomp       
//
//
//
//
//
//
//
//
//
//
//
//  subscribe (..) .................... subscribeSuccess of subscribeFail 
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//

/**
 * 
 */
public class MqttClient3 implements TmsSelectorListener, MqttSocketListener {
	// using CallBack = void (*)( MqttClient* source, Topic& topic, byte[] payload, int payload_length);

	// private static long g_seq = 0;

	// private final long m_seq = ((g_seq++) << 16);

	// enum Flags
	// {
	final public static int Flags_FlagUserName = 128;
	final public static int Flags_FlagPassword = 64;
	final public static int Flags_FlagWillRetain = 32; // unsupported
	final public static int Flags_FlagWillQos = 16 | 8; // unsupported
	final public static int Flags_FlagWill = 4; // unsupported
	final public static int Flags_FlagCleanSession = 2; // unsupported
	final public static int Flags_FlagReserved = 1;
	// };

	class PubItem {

		//
		//

		Topic t;
		MqttMessage m;
		byte qos = 0;
		byte[] data = null;
		TimeUtil past = new TimeUtil();
		TimeUtil resend = new TimeUtil();
		int qos2Received = 0;
		long pi_bk = -1;// packet identity backup

		PubItem(Topic tt, byte qq, MqttMessage mm, byte[] dd) {
			t = tt;
			m = mm;
			qos = qq;
			data = dd;
		}

		PubItem(Topic tt, byte qq, byte[] dd) {
			t = tt;
			m = null;
			qos = qq;
			data = dd;
		}

		PubItem setPi(long pi) {
			pi_bk = pi;
			return this;
		}

	}

	class SubItem {

		//
		//

		String topic;
		byte qos = 0;
		TimeUtil past = new TimeUtil();
		public long pi;
		IMqttSub3 callback = null;

	}

	//
	// static
	//
	static Map<MqttClient3, String> clientArr = new HashMap<MqttClient3, String>();

	public static int addToMgr(MqttClient3 c) {
		synchronized (clientArr) {

			if (clientArr.get(c) == null) {
				clientArr.put(c, c.getId());
			}

			return clientArr.size();
		} // sync

	}

	static String removeFromMgr(MqttClient3 c) {
		synchronized (clientArr) {
			return clientArr.remove(c);
		} // sync
	}

	public static boolean isKeepConnection(MqttClient3 c) {
		synchronized (clientArr) {
			return clientArr.containsKey(c);
		} // sync
	}

	public static int mgrClientCount() {
		return clientArr.size();
	}

	static Timer2 myTimer = null;

	TimeUtil m_aliveTime = new TimeUtil();// time elapsed since created

	//
	// memebers
	//
	Object m_openCloseLock = new Object();

	boolean m_isClosed = true;

	// server side
	// Map<Long, PubItem> m_store = new HashMap<Long, PubItem>();

	Map<Long, PubItem> m_clientQos2Recv = new HashMap<Long, PubItem>();

	Map<Long, PubItem> m_pubDataQueue = new HashMap<Long, PubItem>();

	Map<Long, PubItem> m_acceptQos2Recv = new HashMap<Long, PubItem>();

	TimeUtil m_tmrCheckStat = new TimeUtil();

	Map<Long, SubItem> m_subAckPi = new HashMap<Long, SubItem>();

	// Map<Long, PubReleaseItem> pubRelList = new HashMap<Long, PubReleaseItem>();
	// ArrayList<Long> pubRelDoneList = new ArrayList<Long>();

	// Map<Long, MqttMessage> pubDoneList = new HashMap<Long, MqttMessage>();

	// long packetIdentifier = 0x1234;

	boolean m_isConnected = false;
	byte m_mqtt_flags;
	int m_keepAlive = 0;
	// long m_checkAlive;
	TimeUtil m_checkAlive = new TimeUtil();
	MqttMessage m_message = new MqttMessage();

	MtSocket m_client = null;

	// TODO having a pointer on MqttBroker may produce larger binaries
	// due to unecessary function linked if ever parent is not used
	// (this is the case when MqttBroker isn't used except here)
	MqttBroker3 m_broker = null; // connection to local broker

	// TmsSelector selector = null;
	// long m_pubSec = 6;
	// AsyncSocket ssocket=null;

	// boolean self_generated = false;// is selector self-generated?

	SocketChannel m_channel = null; // connection to remote broker

	// ArrayList<Topic> m_subscriptions = new ArrayList<Topic>();
	Map<String, Topic> m_subscriptions = new HashMap<String, Topic>();

	String m_clientId = "";

	// IMqttSub3 m_callback = null;

	TimeUtil m_reconnectionInerval = new TimeUtil();

	public long m_receviedCount = 0;

	long m_packetIdentifierCount = 1;

	String m_ip = "";

	int m_port = 0;

	String m_user = null;

	String m_pwd = null;

	boolean m_workInCallBack = false;

	TimeUtil m_tmNoRecv = new TimeUtil();

	long m_publishBytesSize = 0;

	long m_qos12ResendIntervalSeccond = 3;

	long m_qos12TimeoutSecond = 3 * 5;

	boolean m_isConnecting = false;

	private IMqtt3 m_conCallback;

	private Object m_userObj = null;

	/**
	 * 
	 * 
	 * 
	 * @throws Exception
	 */
	public MqttClient3() throws Exception {

		m_broker = null;
		//
		m_checkAlive.until(5000);

	}

	//
	// // private constructor used by broker only
	public MqttClient3(MqttBroker3 broker, SocketChannel new_client) throws Exception {

		m_channel = new_client;

		m_broker = broker;

		//
		m_checkAlive.until(5000);
	}
	//
	//

	long getPacketIdentifier() {
		return ((m_packetIdentifierCount++) & 0xffff);// System.currentTimeMillis();
	}

	/**
	 * 
	 * @param ip
	 * @param port
	 * @return
	 * @throws Exception
	 */

	public MqttClient3 connect(String ip, int port, IMqtt3 cb) throws Exception {
		m_isClosed = false;

		return _connect(ip, port, 60, null, null, cb, true);
	}

	public MqttClient3 connect(String ip, int port, int ka, String user, String pwd, IMqtt3 cb) throws Exception {
		m_isClosed = false;
		return _connect(ip, port, ka, user, pwd, cb, true);
	}

	/**
	 * 
	 * @param ip
	 * @param port
	 * @param ka
	 * @param user
	 * @param pwd
	 * @param waitConnact
	 *            - wait for a connact
	 * @return
	 * @throws Exception
	 */
	public synchronized MqttClient3 _connect(String ip, int port, int ka, String user, String pwd, IMqtt3 cb,
			boolean connectNow) throws Exception {

		synchronized (m_openCloseLock) {

			creatTimer();// static func

			if (m_isClosed)
				return this;

			if (isConnecting())
				return this;

			if (isAlive())
				throw new UserException("it's alive");

			if (ip.trim().length() == 0 || port == 0)
				throw new UserException("connection info err (%s:%s)", m_ip, m_port);

			debug("_connect %s %s brk=%s", ip, port, m_broker);

			// if( connected())
			// return this;
			// debug("cnx: closing");
			m_conCallback = cb;
			m_ip = ip;
			m_port = port;
			m_user = user;
			m_pwd = pwd;
			m_keepAlive = ka;

			discon(true);
			m_client = null;

			m_checkAlive.until(16000);

			if (m_broker != null) {
				throw new UserException("accept sockets couldn't connect()");
			}

			if (connectNow) {
				try {
					m_isConnecting = true;

					new MtSocket(ip, port, this);

				} catch (Exception e) {
					m_isConnecting = false;
					throw e;
				}
			} // if

			addToMgr(this);// <-------------- start the connectivity maintenance feature

			return this;
		} // sync
			// onConnect(this, client, null, null);
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * @param ask
	 * @throws Exception
	 */
	private void sendConact(MtSocket ask) throws Exception {

		m_client = ask;

		byte f = 0;

		if (m_user != null && m_user.length() > 0) {
			f |= (byte) 0x80;
		}
		if (m_pwd != null && m_pwd.length() > 0) {
			f |= (byte) 0x40;
		}

		MqttMessage msg = new MqttMessage(MqttMessage.Type_Connect);

		msg.add("MQTT".getBytes(), 4);// msg.add("MQTT",4);
		msg.add((byte) 0x4); // Mqtt protocol version 3.1.1
		msg.add((byte) f); // Connect flags user / name

		msg.add((byte) 0x00); // keep_alive
		msg.add((byte) m_keepAlive);
		msg.add(m_clientId);

		if (m_user != null && m_user.length() > 0) {
			msg.add(m_user);
		}
		if (m_pwd != null && m_pwd.length() > 0) {
			msg.add(m_pwd);
		}

		msg.sendTo(ask);
		msg.reset();

		checkAlive(-3);// 다음 핑쏠 타이밍

	}

	/**
	 * 
	 * 
	 * 
	 */
	public static void destroyTimer() {
		if (myTimer != null) {
			myTimer.cancel();
			myTimer = null;
		}
	}

	/**
	 * 
	 */
	private static void creatTimer() {
		//
		// static timer
		if (myTimer == null) {
			myTimer = new Timer2().schedule(new Timer2Task() {

				int loop = 0;

				@Override
				public void start(Timer2 tmr) {

				}

				@Override
				public void run(Timer2 tmr) {

					MqttClient3[] arr = null;

					synchronized (clientArr) {
						arr = clientArr.keySet().toArray(new MqttClient3[0]);
					} // sync

					// MqttBroker.debug(MyLogger.DEBUG, String.format("alive.tmr=%s/%s ",loop, arr.length) );

					if (arr.length == 0)
						return;

					if ((loop) >= arr.length)
						loop = 0;

					for (MqttClient3 c : arr) {
						try {
							c.myLoop();
						} catch (Exception e) {
							;
						}
						// TimeUtil.sleep(1);
					}

					// debug(MyLogger.DEBUG, "do myLoop(id:%s)", arr[loop].getId());

					loop++;

				}

				@Override
				public void stop(Timer2 tmr) {

				}

			}, 100, 1000);
		}
	}

	/**
	 * now this is included in the connect() function.
	 * 
	 * @param sec
	 * @return
	 */
	@Deprecated
	public boolean waitForConnack(double sec) {

		TimeUtil t = new TimeUtil();
		while (t.end_sec() < sec && m_isConnected == false) {
			TimeUtil.sleep(10);
		}

		return m_isConnected;

	}

	/**
	 * 
	 * @return
	 */
	public boolean isAlive() {

		// aceept
		if (m_broker != null) {
			return (m_channel != null && m_broker.getSelector().isAlive(m_channel));
		}

		// connect
		return (m_client != null && m_client.isAlive());

	}

	public boolean isConnecting() {
		return m_isConnecting;
	}

	public boolean isConnected() {
		return m_isConnected;
	}

	/**
	 * 
	 * @param bSendDisconnect
	 */
	public void discon(boolean bSendDisconnect) {
		// debug("close " + id().c_str());
		// m_isConnected = false;
		if (m_broker != null) // connected to a remote broker
		{
			if (bSendDisconnect && m_broker.getSelector().isAlive(m_channel)) {
				m_message.create(MqttMessage.Type_Disconnect);
				m_message.sendTo(this);
			}

			try {
				m_broker.getSelector().channelClose(m_channel);
			} catch (Exception e) {

			}

			// client->stop();
			try {
				m_channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				m_channel = null;
			}

		}

		if (m_client != null) {
			if (bSendDisconnect && m_client.isAlive()) {
				m_message.create(MqttMessage.Type_Disconnect);
				m_message.sendTo(this);
			}

			try {
				m_client.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				m_client = null;
			}

		}

		m_pubDataQueue.clear();

		m_acceptQos2Recv.clear();

		m_clientQos2Recv.clear();

	}

	/**
	 * 
	 */
	public boolean close() {

		synchronized (m_openCloseLock) {
			m_isClosed = true;
		}

		try {
			if (m_client != null) {
				m_client.getChannel().close();
			}
		} catch (Exception e) {

		}

		try {
			synchronized (m_pubDataQueue) {
				this.m_pubDataQueue.clear();
			}
		} catch (Exception e) {

		}

		boolean r = (removeFromMgr(this) != null);

		discon(true);

		m_broker = null;

		m_userObj = null;

		// m_callback = null;
		m_subscriptions.clear();

		return r;
	}

	/**
	 * 
	 * @param buf
	 */
	void write(byte[] buf) {
		// if (client != null) {
		// client.send(buf, length);

		try {
			if (m_broker != null) {

				m_broker.getSelector().send(m_channel, buf);
			} else if (m_client != null) {

				m_client.send(buf);

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// }
	}

	void sendAck(byte k, long pi) {
		byte[] dd = new byte[] { k, 0x2, (byte) (pi >> 8), (byte) (pi & 0xff) };
		write(dd);

	}

	public String getId() {
		return m_clientId;
	}

	public MqttClient3 setId(String new_id) {
		m_clientId = new_id;
		return this;
	}

	synchronized void myLoop() {

		// if the socket is not alive with connect mode
		if (m_reconnectionInerval.end_sec() > 2.0) {
			m_reconnectionInerval.start();

			if (m_broker == null) {

				if (m_client == null || m_client.isAlive() == false) {

					if (m_keepAlive != 0) {
						if (m_ip.trim().length() > 0 && m_port != 0) {
							try {

								_connect(m_ip, m_port, m_keepAlive, m_user, m_pwd, m_conCallback, true);

								m_reconnectionInerval.start();
							} catch (Exception e) {
								//e.printStackTrace();
								debug("_connect fail %s",e);
							}
						} // if
					}
				} // if
				else {

					int to = (int) ((m_keepAlive * 1.2) + 3);
					if (m_tmNoRecv.end_sec() > to) {

						debug("no recv err(%s sec)", m_tmNoRecv.end_sec());

						m_tmNoRecv.start();
						m_client.close();
					}

				}

			} // if
		}

		// check timeout
		if (m_checkAlive.isOver()) {

			if (m_broker != null) {
				debug("accepter(%s) is timeover!", getId());

				discon(true); // 서버 accpter는 타임아웃에 걸리면 연결을 종료

			} else if (m_client != null && m_client.isAlive()) {
				// 클라이언트는 핑 전송

				byte[] dd = new byte[] { MqttMessage.Type_PingReq, 0 };

				try {

					write(dd);

				} catch (Exception e) {

					discon(false);
					debug(e);

				}

				checkAlive(-3);// 다음 핑 전송할 타이밍 keepalive - 3초
			}
		}

		if (isAlive()) {

			//
			// republish
			{

				long bytes = 0;

				PubItem[] pi = null;
				synchronized (m_pubDataQueue) {
					pi = m_pubDataQueue.values().toArray(new PubItem[0]);
				}

				if (pi != null) {
					for (PubItem p : pi) {

						if (p.data != null) {
							bytes += p.data.length;
						}
						if (p.m.buffer != null) {
							bytes += p.m.buffer.length();
						}

						if (p.qos > 0 && p.resend.end_sec() > this.m_qos12ResendIntervalSeccond) {
							p.resend.start();

							p.m.setDup(true);

							p.m.sendTo(this); // resend

							debug("loopPublish: resend pi=%s ", p.m.packetIdentify);
						}

						if (p.past.end_sec() > this.m_keepAlive || p.past.end_sec() > this.m_qos12TimeoutSecond) {
							debug("qos 1/2 timeover!   " + p.past.end_sec() + " " + m_pubDataQueue.size());

							System.out.format("qos 1/2 timeover!   " + p.past.end_sec() + " " + m_pubDataQueue.size());

							discon(true);// ---

						}

					} // for
				}

				m_publishBytesSize = bytes;
			}

			//
			// check timeover
			if (m_tmrCheckStat.end_sec() > 3) {
				m_tmrCheckStat.start();

				PubItem[] lst;
				synchronized (m_acceptQos2Recv) {
					lst = m_acceptQos2Recv.values().toArray(new PubItem[0]);
				}
				for (PubItem p : lst) {
					if (p.past.end_sec() > this.m_qos12TimeoutSecond + 8) {

						synchronized (m_acceptQos2Recv) {
							m_acceptQos2Recv.remove(p.pi_bk);
						}

					}
				}

				synchronized (m_clientQos2Recv) {
					lst = m_clientQos2Recv.values().toArray(new PubItem[0]);
				}
				for (PubItem p : lst) {
					if (p.past.end_sec() > this.m_qos12TimeoutSecond + 8) {

						synchronized (m_clientQos2Recv) {
							m_clientQos2Recv.remove(p.pi_bk);
						}

					}
				}
			}

			//
			// suback timeover

			{
				SubItem[] pi = null;
				synchronized (m_subAckPi) {
					pi = m_subAckPi.values().toArray(new SubItem[0]);
				}

				if (pi != null) {
					for (SubItem p : pi) {

						if (p.past.end_sec() > this.m_qos12TimeoutSecond) {

							try {
								p.callback.subscribeFailed(this, p.topic);

							} catch (Exception e) {
								debug(e);
							}
							synchronized (m_subAckPi) {
								m_subAckPi.remove(p.pi);
							}

						}

					} // for
				}

			}

		}

	}

	// Publish from client to the world
	// MqttError publish( Topic&, byte[] payload, int pay_length);

	MqttMessage getPublishMessage(Topic topic, byte[] payload, int pay_length, int qos) throws Exception {
		MqttMessage msg = new MqttMessage((byte) (MqttMessage.Type_Publish | (qos << 1)));
		msg.add(topic);

		if (qos != 0) {

			long pi = getPacketIdentifier();
			// add packet identifier
			msg.add((byte) ((pi >> 8) & 0xff));
			msg.add((byte) (pi & 0xff));

			msg.packetIdentify = pi;

			// synchronized (pubDoneList) {
			// pubDoneList.clear();
			// pubDoneList.put(pi, msg);
			// }

		}

		msg.add(payload, pay_length, false);
		msg.complete();

		return msg;
	}

	public synchronized long publish(String tstr, byte[] payload, int qos) throws Exception {
		return publish(tstr, payload, payload.length, qos);
	}

	/**
	 * 
	 * @param tstr
	 * @param payload
	 * @param pay_length
	 * @param qos
	 * @return packet identify<br>
	 * @throws Exception
	 */
	public synchronized long publish(String tstr, byte[] payload, int pay_length, int qos) throws Exception {

		byte res = 0;

		if (isAlive() == false)
			throw new UserException("not connect");

		if (m_isConnected == false) {

			if (m_aliveTime.end_sec() > 6) {// If you try to pulish data while not connecting for more than 6 seconds
				if (m_client != null) {
					m_client.close();
				}
			}

			throw new UserException("conack is not received(not connected yet!)");
		}

		// if (workInCallBack && qos>=2) {
		if (qos > 2) {
			throw new UserException("only qos=0,1,2 experimental support");
		}

		// if (workInCallBack && 1>1) {
		// throw new UserException("publish qos=%s not support in callback func", qos);
		// }

		Topic topic = new Topic(tstr);
		//
		//
		// qos <<= 1;

		MqttMessage msg = getPublishMessage(topic, payload, pay_length, qos);

		// if (m_broker != null) {
		// res = m_broker.publish(this, topic, msg);
		// } else

		// if (m_client != null)
		res = msg.sendTo(this);
		// else
		// res = MqttMessage.MqttError_MqttNowhereToSend;

		if (qos > 0) {
			synchronized (m_pubDataQueue) {
				// pubList.clear();// it removed in puback, also publicomplete
				m_pubDataQueue.put(msg.packetIdentify, new PubItem(topic, (byte) qos, msg, null));
			}
		}
		//
		return msg.packetIdentify;
	}

	// public boolean qosWaitSeconds(long sec) {
	//
	// m_pubSec = sec;
	//
	// return true;
	// }

	public long publish(String t, String s, int qos) throws Exception {
		return publish(t, s.getBytes(), s.getBytes().length, qos);
	}

	public void subscribe(String[] tstr, int qos, IMqttSub3 fun) throws Exception {
		for (String t : tstr) {
			subscribe(t, qos, fun);
		}

	}

	public long subscribe(String tstr, int qos, IMqttSub3 fun) throws Exception {

		if (isAlive() == false)
			throw new UserException("not connect");

		if (m_isConnected == false) {

			throw new UserException("conack is not received");
		}

		if (m_broker != null) {
			throw new UserException("accepter can't subscribe!");
		}

		if (m_subscriptions.containsKey(tstr)) {
			throw new UserException("topic %s is aleady contained", tstr);
		}

		// debug("subsribe(" + topic.c_str() + ")");
		// byte ret = MqttMessage.MqttError_MqttOk;

		Topic topic = new Topic(tstr);

		// m_callback = fun;

		// if (m_broker == null) // remote broker
		// {
		long pi = send_subcribe(topic, MqttMessage.Type_Subscribe, (byte) qos);

		debug("subscribe t(%s) pi=%s", tstr, pi);

		synchronized (m_subAckPi) {

			SubItem si = new SubItem();
			si.topic = tstr;
			si.qos = (byte) qos;
			si.pi = pi;
			si.callback = fun;
			m_subAckPi.put(pi, si);
		}
		// //
		// //
		// if (waitSuback > 0) {
		// TimeUtil t = new TimeUtil();
		// while (t.end_sec() < waitSuback && subAckPi.indexOf(pi) == -1) {
		// TimeUtil.sleep(10);
		// }
		//
		// if (subAckPi.indexOf(pi) == -1) {
		// m_client.close();
		// throw new UserException("suback is not receviced (name:%s)", m_clientId);
		// }
		// }

		return pi;

		// } else {
		// // return parent.subscribe(topic, qos);
		// return 0;
		// }
	}

	public byte unsubscribe(String t) throws Exception {

		Topic topic = new Topic(t);

		if (m_subscriptions.containsKey(t)) {
			m_subscriptions.remove(t);
			if (m_broker == null) //
			{
				send_unsubscribe(topic, MqttMessage.Type_UnSubscribe, (byte) 0);
			}
		}
		return MqttMessage.MqttError_MqttOk;
	}

	byte sendTopic(Topic topic, byte type) throws Exception {
		MqttMessage msg = new MqttMessage((byte) type, (byte) 2);

		// TODO manage packet identifier
		long pi = getPacketIdentifier();

		msg.add((byte) ((pi >> 8) & 0xff));
		msg.add((byte) (pi & 0xff));

		msg.add(topic);

		return msg.sendTo(this);
	}

	long send_subcribe(Topic topic, byte type, byte qos) throws Exception {
		MqttMessage msg = new MqttMessage((byte) type, (byte) 2);

		// TODO manage packet identifier
		long pi = getPacketIdentifier();

		msg.add((byte) ((pi >> 8) & 0xff));
		msg.add((byte) (pi & 0xff));

		msg.add(topic);
		msg.add((byte) qos);

		msg.sendTo(this);

		return pi;
	}

	long send_unsubscribe(Topic topic, byte type, byte qos) throws Exception {
		MqttMessage msg = new MqttMessage((byte) type, (byte) 2);

		// TODO manage packet identifier
		long pi = getPacketIdentifier();

		msg.add((byte) ((pi >> 8) & 0xff));
		msg.add((byte) (pi & 0xff));

		msg.add(topic);
		// msg.add((byte) qos);

		// TODO instead we should wait (state machine) for SUBACK / UNSUBACK ?
		msg.sendTo(this);

		return pi;
	}

	//
	// void resubscribe() throws Exception {
	// debug("resubscribe");
	//
	// if (subscriptions.size() != 0) {
	//
	// for (Topic topic : subscriptions) {
	//
	// send_subcribe(topic, MqttMessage.Type_Subscribe, (byte) 0);
	//
	// debug("resubscribe [%s] \n",topic.str() );
	// }
	//
	// }
	// }

	// bool isSubscribedTo( Topic& topic) ;
	Topic isSubscribedTo(Topic topic) {

		for (Topic subscription : m_subscriptions.values()) {

			if (subscription.matches(topic))
				return subscription;
		}

		return null;
	}

	// republish a received publish if it matches any in subscriptions
	byte publishIfSubscribed(Topic topic, byte[] data) {
		byte retval = MqttMessage.MqttError_MqttOk;

		Topic rs = isSubscribedTo(topic);
		if (rs != null) {

			MqttMessage msg = null;
			try {
				// System.out.println( topic.str());
				msg = getPublishMessage(topic, data, data.length, rs.qos);
			} catch (Exception e) {
				return MqttMessage.MqttError_MqttInvalidMessage;// error
			} // make new message

			retval = msg.sendTo(this);

			if (rs.qos > 0) {
				synchronized (m_pubDataQueue) { // on broker
					m_pubDataQueue.put(msg.packetIdentify, new PubItem(topic, (byte) rs.qos, msg, null));
				}
			}

		}
		return retval;
	}

	// void clientAlive(int more_seconds);

	void checkAlive(int more_seconds) {
		if (m_keepAlive != 0) {

			m_checkAlive.until(1000 * (m_keepAlive + more_seconds));

		} else {

		}
	}

	void debug(String f, Object... a) {
		try {
			MqttBroker3.debug(f, a);
		} catch (Exception ex) {

		}

	}

	void debug(long lvl, String f, Object... a) {
		try {
			MqttBroker3.debug(lvl, f, a);
		} catch (Exception ex) {

		}

	}

	void debug(Exception e) {
		try {
			MqttBroker3.debug(e);
		} catch (Exception ex) {

		}
	}

	//
	// void outstring(String prefix, byte[] p, byte len) {
	// // return;
	// // Serial << prefix << "='";
	// // while(len--) Serial << (char)*p++;
	// // Serial << '\'' << endl;
	// }

	int processMessage(MqttMessage in) throws Exception {

		MqttMessage mesg = in.copyOf();

		debug(MyLogger.DEBUG, "msg.len=" + mesg.buffer.copyOf().length + " mesg.buf= %s",
				StringUtil.omitAfter(StringUtil.ByteArrayToHex(mesg.buffer.copyOf()), 100));

		// }
		// #endif
		byte[] header = mesg.getVHeader();
		// byte[] payload;
		// int len=0;
		boolean bclose = true;

		DataStream payload = new DataStream();

		switch (mesg.type() & (byte) 0XF0) {
		case MqttMessage.Type_Connect:// ::Type::Connect:

			if (m_isConnected) {
				debug("already connected");
				break;
			}

			// System.out.println("header=" + StringUtil.ByteArrayToHex(header) + " len=" + header.length);
			// payload = header+10;

			m_mqtt_flags = header[7];
			m_keepAlive = (header[8] << 8) | (header[9]);// ??
			// if (strncmp("MQTT", header+2,4))
			if (strncmp("MQTT", header, 2, 4) == false) {
				debug("bad mqtt header");
				break;
			}
			if (header[6] != 0x04) { // protocol ver 3.1.1
				debug("unknown level");
				discon(true);
				break; // Level 3.1.1
			}

			int cur = 10;

			// ClientId
			cur = MqttMessage.getPayload(cur, header, payload);
			if (payload.length() > 0) {
				m_clientId = new String(payload.copyOf());// ::string(payload, len);
			} else {
				m_clientId = "";
			}

			if (m_clientId == null || m_clientId.length() == 0) {
				m_clientId = "auto" + UUID.randomUUID();
			}

			setId(m_clientId);

			debug("con id= %s ", m_clientId);

			byte[] user = new byte[0];
			byte[] pwd = new byte[0];

			// payload += len;

			if ((m_mqtt_flags & Flags_FlagWill) != 0) // Will topic
			{
				// mesg.getString(payload, len); // Will Topic
				cur = MqttMessage.getPayload(cur, header, payload);

				debug("will topic=%s ", new String(payload.copyOf()));
				// outstring("WillTopic", payload, len);
				// payload += len;

				// mesg.getString(payload, len); // Will Message
				cur = MqttMessage.getPayload(cur, header, payload);

				debug("will message=%s ", new String(payload.copyOf()));
				// outstring("WillMessage", payload, len);
				// payload += len;
			}
			// FIXME forgetting credential is allowed (security hole)
			// if ((!mqtt_flags & Flags_FlagUserName)!=0)
			if ((m_mqtt_flags & Flags_FlagUserName) != 0) {
				// mesg.getString(payload, len);
				cur = MqttMessage.getPayload(cur, header, payload);
				debug("user=%s ", new String(payload.copyOf()));

				user = payload.copyOf();

				// payload += len;
			}
			if ((m_mqtt_flags & Flags_FlagPassword) != 0) {
				// mesg.getString(payload, len);
				cur = MqttMessage.getPayload(cur, header, payload);

				pwd = payload.copyOf();

				debug("pwd=%s ", new String(payload.copyOf()));
				// payload += len;
			}

			// if (user != null && pwd != null) {
			try {
				if (m_broker != null) {
					if (!m_broker.checkUser(m_clientId, user, pwd)) {
						bclose = true;
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				bclose = true;
				break;

			}
			// }

			// debug("Connected client:" + m_clientId + ", keep alive=" + m_keepAlive + ". " + this.hashCode() + " brk="
			// + (m_broker != null));

			bclose = false;
			m_isConnected = true; {
			MqttMessage msg = new MqttMessage(MqttMessage.Type_ConnAck);
			msg.add((byte) 0); // Session present (not implemented)
			msg.add((byte) 0); // Connection accepted
			msg.sendTo(this);

			// debug("brk send conack = " + StringUtil.ByteArrayToHex(msg.buffer.copyOf()));

		}
			break;

		case MqttMessage.Type_ConnAck:// ::Type::ConnAck:

			debug("id(%s) connack", getId());

			m_isConnecting = false;
			m_isConnected = true;
			bclose = false;
			// resubscribe();

			try {
				m_conCallback.onConnected(this);
			} catch (Exception e) {
				debug(e);
			}

			break;

		case MqttMessage.Type_SubAck:// ::Type::SubAck:

			if (true) {

				int v = mesg.vheader;

				long pi = 0;
				byte[] bb = mesg.buffer.copyOf();
				pi |= ((bb[v + 0] & 0xff) << 8);
				pi |= (bb[v + 1] & 0xff);

				debug("id(%s) suback pi=%s vheader=%s", getId(), pi, mesg.vheader);

				try {
					SubItem si = null;
					synchronized (m_subAckPi) {
						si = m_subAckPi.remove(pi);
					}

					if (si != null) {

						Topic topic = new Topic(si.topic);
						topic.qos = si.qos;
						topic.callback = si.callback;
						if (m_subscriptions.containsKey(si.topic) == false) {
							m_subscriptions.put(si.topic, topic); // add subscribe channel of connector
						}

						si.callback.subscribeSuccess(this, si.topic);
					}
				} catch (Exception e) {
					debug(e);
				}

			}

			// debug(MyLogger.VERBOSE, "msg.len=" + mesg.buffer.copyOf().length + " mesg.buf= %s",
			// StringUtil.omitAfter(StringUtil.ByteArrayToHex(mesg.buffer.copyOf()), 100));

			if (!m_isConnected)
				break;
			// Ignore acks
			bclose = false;
			break;
		case MqttMessage.Type_PubAck:// e::Type::PubAck:

			// debug("id(%s) puback m.size=%d sk=%s ch=%s", getId(), pubDoneList.size(), m_client, m_channel);
			// debug("puback id(%s) ", getId());

			if (!m_isConnected)
				break;

			if (true) {
				byte[] bb = mesg.buffer.copyOf();

				if (bb.length < 2 || bb.length != bb[1] + 2) {
					debug("malform error %s", StringUtil.ByteArrayToHex(mesg.buffer.copyOf()));
					break;
				}

				long pi = 0;
				pi |= ((bb[2] & 0xff) << 8);
				pi |= (bb[3] & 0xff);
				//
				PubItem item = null;
				synchronized (m_pubDataQueue) {
					item = m_pubDataQueue.remove(pi);
				}

				try {
					m_workInCallBack = true;
					if (item != null) {
						m_conCallback.deliveryComplete(pi, item.past.end_ms());
					}
				} catch (Exception e) {
					debug(e);
				} finally {
					m_workInCallBack = false;
				}

				debug("puback id(%s) publist.size= %s ", getId(), m_pubDataQueue.size());

			}

			bclose = false;
			break;

		case MqttMessage.Type_PubReceived:// e::Type::PubAck:

			debug("pubrec id(%s) ", getId());
			if (!m_isConnected)
				break;

			if (true) {
				byte[] bb = mesg.buffer.copyOf();

				if (bb.length < 2 || bb.length != bb[1] + 2) {
					debug("malform error %s", StringUtil.ByteArrayToHex(mesg.buffer.copyOf()));
					discon(false);
					break;
				}

				long pi = 0;
				pi = ((bb[2] & 0xff) << 8);
				pi |= (bb[3] & 0xff);

				//
				// byte[] dd = new byte[] { MqttMessage.Type_PubRelease | 0x2, 0x2, bb[2], bb[3] };
				//
				// // client.send(dd);
				// write(dd);

				sendAck((byte) (MqttMessage.Type_PubRelease | 0x2), pi);
			}

			bclose = false;
			break;

		case MqttMessage.Type_PubRelease:// e::Type::PubAck: 50023346

		{
			// debug("id(%s) pubrel ", getId());
			if (!m_isConnected)
				break;

			byte[] bb = mesg.buffer.copyOf();

			if (bb.length < 2 || bb.length != bb[1] + 2) {
				debug("malform error %s", StringUtil.ByteArrayToHex(mesg.buffer.copyOf()));
				discon(false);
				break;
			}

			long pi = 0;
			pi = ((bb[2] & 0xff) << 8);
			pi |= (bb[3] & 0xff);

			debug("id(%s) pubrel pi=%s", getId(), pi);

			if (m_broker != null) { // broker side

				// if( m_broker.getQueueCountOfClients() >150) {

				sendAck(MqttMessage.Type_PubComplate, pi);

				PubItem i = null;
				synchronized (m_acceptQos2Recv) {
					i = m_acceptQos2Recv.get(pi);
				} // sync

				if (i != null) {

					if (i.qos2Received == 0) {
						i.qos2Received = 1;

						m_broker.publish(this, i.t, i.data); // publish to subscribers
						i.data = null;// reduce memory usage
					}

				}

				//

			} else {// client side

				sendAck(MqttMessage.Type_PubComplate, pi);

				PubItem i = null;
				synchronized (m_clientQos2Recv) {
					i = m_clientQos2Recv.get(pi);
				} // sync

				if (i != null) {

					try {
						m_workInCallBack = true;
						if (i.qos2Received == 0) {
							i.qos2Received = 1;

							// System.out.println(i.t.str() );

							Topic rs1 = null;
							synchronized (m_subscriptions) {
								// rs1 = m_subscriptions.get(i.t.str());
								for (Topic tt : m_subscriptions.values()) {

									if (tt.matches(i.t.str())) {
										rs1 = tt;
										break;
									}
								} // for
							} // sync

							if (rs1 != null) {
								rs1.callback.onReceived(this, i.t.str(), i.data);
							}
							i.data = null;// buffer clear
						}
					} catch (Exception e) {
						debug(e);
					} finally {
						m_workInCallBack = false;
					}
				}

			}

			debug(" pubrel clientQos2Recv.size= %s acceptQos2Recv.size=%s subscribies=%s pi=%s ",
					m_acceptQos2Recv.size(), m_clientQos2Recv.size(), this.m_subscriptions.size(), pi);

			bclose = false;
		}
			break;

		case MqttMessage.Type_PubComplate:// e::Type::PubAck: 70023346

			// debug("id(%s) pubcomp m.size=%d ", getId(), pubDoneList.size());
			// debug("pubcomp id(%s) ", getId());

			if (!m_isConnected)
				break;

			if (true) {
				byte[] bb = mesg.buffer.copyOf();

				if (bb.length < 2 || bb.length != bb[1] + 2) {
					debug("malform error %s", StringUtil.ByteArrayToHex(mesg.buffer.copyOf()));
					discon(false);
					break;
				}

				long pi = 0;
				pi |= ((bb[2] & 0xff) << 8);
				pi |= (bb[3] & 0xff);

				PubItem item = null;
				synchronized (m_pubDataQueue) {
					item = m_pubDataQueue.remove(pi);
				}

				try {
					m_workInCallBack = true;
					if (m_conCallback != null && item != null) {
						m_conCallback.deliveryComplete(pi, item.past.end_ms());
					}
				} catch (Exception e) {
					debug(e);
				} finally {
					m_workInCallBack = false;
				}

				debug("pubcomp id(%s) publist.size= %s pi=%s ", getId(), m_pubDataQueue.size(), pi);

			}

			bclose = false;
			break;

		case MqttMessage.Type_PingResp:// ::Type::PingResp:

			debug("id(%s) pingresp ", this.getId());
			bclose = false;
			break;

		case MqttMessage.Type_PingReq:// ::Type::PingReq:
			if (!m_isConnected)
				break;

			debug("id(%s) pingreq ", this.getId());

			if (m_channel != null) {
				byte[] dd = new byte[] { MqttMessage.Type_PingResp, 0 };

				write(dd);

				bclose = false;
			} else {
				debug("internal pingreq ?");
			}
			break;

		case MqttMessage.Type_Subscribe:// ::Type::Subscribe:
		case MqttMessage.Type_UnSubscribe:// ::Type::UnSubscribe:
		{
			if (!m_isConnected)
				break;

			// debug("sub.header=" + StringUtil.ByteArrayToHex(header) + " len=" + header.length);

			long pi = 0;// packet identifier
			pi |= ((header[0] & 0xff) << 8);
			pi |= (header[1] & 0xff);

			if ((byte) (mesg.type() & (byte) 0XF0) == (byte) MqttMessage.Type_Subscribe)
				debug("brk:id(%s) subscribe pi=%s", getId(), pi);
			else
				debug("brk:id(%s) unsubscribe", getId());

			cur = 2;// payload = header+2;

			// debug("un/subscribe loop");
			// while(payload < mesg.end())
			while (cur < header.length) {
				// mesg.getString(payload, len); // Topic
				// debug("topic.len= (%s) header.len=%s cur=%s ", payload.length() , header.length,cur );

				cur = MqttMessage.getPayload(cur, header, payload);

				debug("topic.len= (%s) header.len=%s  cur=%s ", payload.length(), header.length, cur);
				// outstring(" un/subscribes", payload, len);
				// subscribe(Topic(payload, len));
				Topic topic = new Topic(payload.copyOf(), payload.length());
				// payload += len;
				if ((byte) (mesg.type() & 0XF0) == (byte) MqttMessage.Type_Subscribe) {

					// System.out.format(" header= %s \n", StringUtil.ByteArrayToHex(msg.));

					// byte qos = *payload++;
					byte qos = header[cur++];

					// if (qos != 0) {
					// debug("subscribe: Unsupported QOS=" + qos);
					// }

					if (m_subscriptions.containsKey(topic.str()) == false) {
						topic.qos = qos;// qos level of subscribe
						m_subscriptions.put(topic.str(), topic);
					}

					MqttMessage msg = new MqttMessage(MqttMessage.Type_SubAck);

					msg.add((byte) ((pi >> 8) & 0xff));
					msg.add((byte) (pi & 0xff));
					msg.add((byte) 0);
					msg.sendTo(this);
					//

				} else {

					m_subscriptions.remove(topic);

				}
			}
			// debug("end loop");
			bclose = false;
			// TODO SUBACK
		}
			break;

		case MqttMessage.Type_UnSuback:// ::Type::UnSuback:
			if (!m_isConnected)
				break;

			debug("id(%s) unsuback ", this.getId());

			bclose = false;
			break;

		case MqttMessage.Type_Publish:// its a recevied ::Type::Publish:

			if (m_isConnected || m_broker != null) {
				byte qos = (byte) ((mesg.type() & 0x6) >> 1);

				byte retain = (byte) (mesg.type() & 0x1);

				byte dup = (byte) ((mesg.type() & 0x8) >> 3);

				// System.out.format(" pub qos=%s retain=%s dup=%s \r\n", qos, retain, dup);

				cur = 0;

				// mesg.getString(payload, len);
				cur = MqttMessage.getPayload(cur, header, payload);

				Topic tp = new Topic(payload.copyOf(), payload.length());

				// debug("pub.topic=%s", new String(payload.copyOf()));

				long pi = 0;
				if (qos != 0) {

					pi |= ((header[cur + 0] & 0xff) << 8);
					pi |= (header[cur + 1] & 0xff);

					cur += 2;

					// exactly once!

				}

				// debug("brk:id(%s) pub pi=%s tp=[%s]", this.getId(), pi, tp.str());

				byte[] data = Arrays.copyOfRange(header, cur, header.length);// rest of packet

				if (qos != 0)
					cur += 2; // ignore packet identifier if any
				// len = mesg.length() - cur;

				//
				//
				//
				if (m_broker == null) { // client

					Topic rTp = isSubscribedTo(tp);
					if (rTp != null) {

						if (qos == 0) {

							try {
								m_workInCallBack = true;
								rTp.callback.onReceived(this, tp.str(), data);
							} catch (Exception e) {
								debug(e);
							} finally {
								m_workInCallBack = false;
							}

						} else if (qos == 1) {

							sendAck(MqttMessage.Type_PubAck, pi);

							try {
								m_workInCallBack = true;

								rTp.callback.onReceived(this, tp.str(), data);
							} catch (Exception e) {
								debug(e);
							} finally {

								m_workInCallBack = false;
							}

						} else if (qos == 2) {
							sendAck(MqttMessage.Type_PubReceived, pi);

							synchronized (m_clientQos2Recv) {
								if (m_clientQos2Recv.containsKey(pi) == false) {
									m_clientQos2Recv.put(pi, new PubItem(tp, qos, data).setPi(pi));
								}
							}

						}

						m_receviedCount++;

					}
				}

				else if (m_broker != null) // broker
				{
					// debug("publishing to parent");

					if (qos == 0) {
						m_broker.publish(this, tp, data);
					}

					else if (qos == 1) {

						// MqttMessage newMsg = getPublishMessage(published, data, data.length, 0);
						mesg.packetIdentify = pi;
						mesg.setDup(false);
						m_broker.publish(this, tp, data);

						sendAck(MqttMessage.Type_PubAck, pi);

					}

					else if (qos == 2) {

						sendAck(MqttMessage.Type_PubReceived, pi);

						synchronized (m_acceptQos2Recv) {
							if (m_acceptQos2Recv.containsKey(pi) == false) {
								mesg.packetIdentify = pi;
								mesg.setDup(false);

								m_acceptQos2Recv.put(pi, new PubItem(tp, qos, mesg, data).setPi(pi));
							}
						}

					}

				}
				bclose = false;
			}
			break;

		case MqttMessage.Type_Disconnect:// ::Type::Disconnect:
			// TODO should discard any will msg
			debug("id(%s) discon ", this.getId());

			discon(false);

			if (!m_isConnected)
				break;
			m_isConnected = false;

			bclose = false;
			break;

		default:
			bclose = true;
			break;
		}

		if (bclose) {

			debug("*************** Error msg %x  m_isConnected=%s brk=%s  id(%s) ", mesg.type(), m_isConnected,
					m_broker, this.getId());
			try {
				debug(StringUtil.ByteArrayToHex(in.buffer.copyOf()));
			} catch (Exception e) {
				e.printStackTrace();
			}

			// mesg.hexdump("-------ERROR ------");
			// dump();
			// Serial + endl;
			discon(true);
			return 0;
		} else {

			if (m_broker != null) {
				checkAlive(10); // 브로커 accpeter는 keepalive time+10초 동안 핑을 기다린다
			}

			// clientAlive(0);
		}
		return 1;
	}

	boolean strncmp(String s, byte[] b, int from, int length) {

		if (s.length() != (length))
			return false;

		for (int n = 0; n < s.length(); n++) {
			if (s.charAt(n) != b[from + n])
				return false;
		}

		return true;

	}

	public SocketChannel handle() {
		return m_channel;
	}

	//
	//
	// server side
	//
	@Override
	public TmsItem received(SocketChannel ch, byte[] rxd) {

		for (int n = 0; n < rxd.length; n++) {

			byte c = rxd[n];

			m_message.incoming(c);

			if (m_message.state == MqttMessage.State_Error) {

				debug(MyLogger.WARNING, "MqttMessage.State_Error");
				try {

					ch.close();
				} catch (IOException e) {
					e.printStackTrace();

				}
				return null;
			}

			if (m_message.type() != 0) {

				debug(MyLogger.DEBUG, "recv type= %x   len=%s   state=%s ", m_message.type(), m_message.buffer.length(),
						m_message.state);

				try {
					processMessage(m_message);
				} catch (Exception e) {
					debug(e);
					try {
						ch.close();
					} catch (Exception ex) {

					}
				}
				m_message.reset();
			}
		} // for

		return null;
	}

	@Override
	public void disconnected(SocketChannel ch) {
		debug("id(%s) socket disconnected", getId());

		m_isConnected = false;
		m_message.buffer.reset();
	}

	@Override
	public Object accepteded(SocketChannel ch) {
		debug("accepted " + ch);
		return null;
	}

	@Override
	public boolean connected(SocketChannel channel, Object userojb) {
		debug("connected");
		return true;
	}

	//
	//
	//
	// client side
	//

	@Override
	public void recv(MtSocket ask, byte[] buf, int len) {

		m_tmNoRecv.start();

		byte[] rxd = Arrays.copyOf(buf, len);

		debug(MyLogger.DEBUG, "client.recv=%s len=%s   msg.buf.len=%s ",
				StringUtil.omitAfter(StringUtil.ByteArrayToHex(rxd), 100), rxd.length, m_message.buffer.length());

		for (int n = 0; n < rxd.length; n++) {

			byte c = rxd[n];

			m_message.incoming(c);
			if (m_message.type() != 0) {

				debug(MyLogger.DEBUG, "recv type= %x", m_message.type());

				try {
					processMessage(m_message);
				} catch (Exception e) {
					debug(e);
					try {
						ask.close();
					} catch (Exception ex) {

					}
				}
				m_message.reset();
			}
		} // for

	}

	@Override
	public void connected(MtSocket ask) {
		debug("client.connected");
		m_tmNoRecv.start();
		m_message.reset();
		m_subAckPi.clear();
		m_subscriptions.clear();
		try {
			sendConact(ask);
		} catch (Exception e) {
			debug(e);
		}

		m_reconnectionInerval.start();

		// if (waitConnact > 0) {
		// TimeUtil t = new TimeUtil();
		// while (t.end_sec() < waitConnact && m_isConnected == false) {
		// TimeUtil.sleep(10);
		// }
		//
		// if (m_isConnected == false) {
		// m_client.close();
		// throw new UserException("connact is not receviced (name:%s)", m_clientId);
		// }
		// }

	}

	@Override
	public void disconnected(MtSocket ask) {
		debug("client.id(%s) socket disconnected", getId());

		System.out.format("client.id(%s) socket disconnected  \n", getId());

		if (m_isConnected) {
			try {
				m_conCallback.onDisonnected(this);
			} catch (Exception e) {
				debug(e);
			}
		}

		m_isConnecting = false;
		m_isConnected = false;
		m_message.reset();
		m_subAckPi.clear();
		m_subscriptions.clear();
		m_reconnectionInerval.start();

	}

	public int qos12QueueSize() {
		return m_acceptQos2Recv.size() + m_clientQos2Recv.size();
	}

	public int publishQueueSize() {

		synchronized (m_pubDataQueue) {
			return m_pubDataQueue.size();
		}
	}

	public long publishQueueBytesSize() {
		return m_publishBytesSize;
	}

	public void setQosResendingInterval(long n) {
		m_qos12ResendIntervalSeccond = n;
	}

	public void setQosResendingTimeout(long n) {
		m_qos12TimeoutSecond = n;
	}

	public int subscriptionCount(String t) {

		synchronized (m_subscriptions) {
			return m_subscriptions.size();
		}
	}

	public boolean isSubscribed(String t) {

		synchronized (m_subscriptions) {
			return m_subscriptions.containsKey(t);
		}
	}

	public boolean isSubscribing(String t) {

		SubItem[] arr = null;
		synchronized (m_subAckPi) {

			arr = m_subAckPi.values().toArray(new SubItem[0]);

		}

		if (arr != null) {
			for (SubItem a : arr) {
				if (a.topic.equals(t)) {
					return true;
				}
			}
		}
		return false;
	}

	public int pubQueueSize() {
		return m_pubDataQueue.size();
	}

	/**
	 * 
	 * extension
	 * 
	 */
	public void setUserObject(Object o) {
		m_userObj = o;
	}

	public Object getUserObject() {
		return m_userObj;
	}

	public synchronized MqttClient3 _keepConnection(String ip, int port, int ka, String user, String pwd, IMqtt3 cb)
			throws Exception {
		m_isClosed = false;//@20240517 add
		return _connect(ip, port, ka, user, pwd, cb, false);

	}

	public String info() {

		String t = "";
		for (String k : m_subscriptions.keySet()) {
			t += k + ",";
		}

		String s = String.format("%s|alive:%s s|queue:%s,%s,%s,%s|ka:%s|sub:%s|"//

				, ((m_broker == null) ? "cli" : "acp")//

				, m_aliveTime.end_sec()//

				, m_clientQos2Recv.size()//
				, m_pubDataQueue.size()//
				, m_acceptQos2Recv.size()//
				, m_subAckPi.size()//

				, m_keepAlive//
				, t//

		);

		return s;

	}

};