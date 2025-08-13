package home.lib.net.tms.mqtt;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import home.lib.lang.Timer2;
import home.lib.lang.Timer2Task;
import home.lib.lang.UserException;
import home.lib.log.MyLogger;
import home.lib.net.nio.sync.SyncSocket;
import home.lib.net.nio.sync.SyncSocketListener;
import home.lib.net.tms.TmsItem;
import home.lib.net.tms.TmsSelectorListener;
import home.lib.util.DataStream;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

public class MqttClientExperimental {// implements TmsSelectorListener, SyncSocketListener {
//	// using CallBack = void (*)( MqttClient* source, Topic& topic, byte[] payload, int payload_length);
//
//	// private static long g_seq = 0;
//
//	// private final long m_seq = ((g_seq++) << 16);
//
//	// enum Flags
//	// {
//	final public static int Flags_FlagUserName = 128;
//	final public static int Flags_FlagPassword = 64;
//	final public static int Flags_FlagWillRetain = 32; // unsupported
//	final public static int Flags_FlagWillQos = 16 | 8; // unsupported
//	final public static int Flags_FlagWill = 4; // unsupported
//	final public static int Flags_FlagCleanSession = 2; // unsupported
//	final public static int Flags_FlagReserved = 1;
//	// };
//
//	class PubItem {
//
//		long resendIntervalSec = 3;
//		//
//		//
//
//		Topic t;
//		MqttMessage m;
//		byte qos = 0;
//		byte[] data = null;
//		TimeUtil past = new TimeUtil();
//		TimeUtil resend = new TimeUtil();
//
//		PubItem(Topic tt, byte qq, MqttMessage mm, byte[] dd) {
//			t = tt;
//			m = mm;
//			qos = qq;
//			data = dd;
//		}
//
//		PubItem(Topic tt, byte qq, byte[] dd) {
//			t = tt;
//			m = null;
//			qos = qq;
//			data = dd;
//		}
//
//	}
//
//	//
//	// static
//	//
//	static ArrayList<MqttClientExperimental> clientArr = new ArrayList<MqttClientExperimental>();
//
//	public static int add(MqttClientExperimental c) {
//		synchronized (clientArr) {
//
//			if (clientArr.contains(c) == false) {
//				clientArr.add(c);
//			}
//
//			return clientArr.size();
//		} // sync
//
//	}
//
//	static boolean remove(MqttClientExperimental c) {
//		synchronized (clientArr) {
//			return clientArr.remove(c);
//		} // sync
//	}
//
//	static boolean isKeepConnection(MqttClientExperimental c) {
//		synchronized (clientArr) {
//			return clientArr.contains(c);
//		} // sync
//	}
//
//	static int clientCount() {
//		return clientArr.size();
//	}
//
//	static Timer2 myTimer = null;
//
//	TimeUtil aliveTime = new TimeUtil();// time elapsed since created
//
//	//
//	// memebers
//	//
//
//	// server side
//	// Map<Long, PubItem> m_store = new HashMap<Long, PubItem>();
//
//	Map<Long, PubItem> clientQos2Recv = new HashMap<Long, PubItem>();
//
//	Map<Long, PubItem> pubList = new HashMap<Long, PubItem>();
//
//	Map<Long, PubItem> acceptQos2Recv = new HashMap<Long, PubItem>();
//
//	TimeUtil tmrCheckStat = new TimeUtil();
//
//	ArrayList<Long> subAckPi = new ArrayList<Long>();
//
//	// Map<Long, PubReleaseItem> pubRelList = new HashMap<Long, PubReleaseItem>();
//	// ArrayList<Long> pubRelDoneList = new ArrayList<Long>();
//
//	// Map<Long, MqttMessage> pubDoneList = new HashMap<Long, MqttMessage>();
//
//	// long packetIdentifier = 0x1234;
//
//	boolean m_isConnected = false;
//	byte mqtt_flags;
//	int m_keepAlive = 0;
//	long m_checkAlive;
//	MqttMessage message = new MqttMessage();
//
//	SyncSocket m_client = null;
//
//	// TODO having a pointer on MqttBroker may produce larger binaries
//	// due to unecessary function linked if ever parent is not used
//	// (this is the case when MqttBroker isn't used except here)
//	MqttBroker m_broker = null; // connection to local broker
//
//	// TmsSelector selector = null;
//	long m_pubSec = 6;
//	// AsyncSocket ssocket=null;
//
//	// boolean self_generated = false;// is selector self-generated?
//
//	SocketChannel m_channel = null; // connection to remote broker
//
//	ArrayList<Topic> subscriptions = new ArrayList<Topic>();
//
//	String m_clientId = "";
//
//	IMqtt m_callback = null;
//
//	TimeUtil m_reconnectionInerval = new TimeUtil();
//
//	public long receviedCount = 0;
//
//	long packetIdentifierCount = 1;
//
//	String m_ip = "";
//
//	int m_port = 0;
//
//	String m_user = null;
//
//	String m_pwd = null;
//
//	boolean workInCallBack = false;
//
//	TimeUtil m_tmNoRecv = new TimeUtil();
//
//	/**
//	 * 
//	 * 
//	 * 
//	 * @throws Exception
//	 */
//	public MqttClientExperimental() throws Exception {
//
//		m_broker = null;
//		m_checkAlive = System.currentTimeMillis() + 5000; // TODO MAGIC client expires after 5s if no CONNECT msg
//
//	}
//
//	//
//	// // private constructor used by broker only
//	public MqttClientExperimental(MqttBroker broker, SocketChannel new_client) throws Exception {
//
//		m_channel = new_client;
//
//		m_broker = broker;
//
//		m_checkAlive = System.currentTimeMillis() + 5000; // TODO MAGIC client expires after 5s if no CONNECT msg
//	}
//	//
//	//
//
//	long getPacketIdentifier() {
//		return ((packetIdentifierCount++) & 0xffff);// System.currentTimeMillis();
//	}
//
//	/**
//	 * 
//	 * @param ip
//	 * @param port
//	 * @return
//	 * @throws Exception
//	 */
//	public MqttClientExperimental connect(String ip, int port) throws Exception {
//		return _connect(ip, port, 60, null, null, 6);
//	}
//
//	@Deprecated
//	public MqttClientExperimental connect(String ip, int port, int ka, String user, String pwd) throws Exception {
//		return _connect(ip, port, ka, user, pwd, 0.0);
//	}
//
//	/**
//	 * 
//	 * @param ip
//	 * @param port
//	 * @param ka
//	 * @param user
//	 * @param pwd
//	 * @param waitConnact
//	 *            - wait for a connact
//	 * @return
//	 * @throws Exception
//	 */
//	public synchronized MqttClientExperimental _connect(String ip, int port, int ka, String user, String pwd, double waitConnact)
//			throws Exception {
//		
//		
//
//		if (isAlive())
//			throw new UserException("it's alive");
//
//		if (ip.trim().length() == 0 || port == 0)
//			throw new UserException("connection info err (%s:%s)", m_ip, m_port);
//		
//		
//		
//		debug("_connect %s %s ",ip, port);
//		
//
//		// if( connected())
//		// return this;
//		// debug("cnx: closing");
//		m_ip = ip;
//		m_port = port;
//		m_user = user;
//		m_pwd = pwd;
//		m_keepAlive = ka;
//
//		discon(true);
//
//		m_checkAlive = System.currentTimeMillis() + 5000;
//
//		if (m_broker != null) {
//			throw new UserException("accept sockets couldn't connect()");
//		}
//
//		m_client = new SyncSocket(ip, port, this);
//
//		if (m_client.isAlive() == false) {
//			throw new UserException("connect fail");
//		}
//
//		byte f = 0;
//
//		if (user != null && user.length() > 0) {
//			f |= (byte) 0x80;
//		}
//		if (pwd != null && pwd.length() > 0) {
//			f |= (byte) 0x40;
//		}
//
//		MqttMessage msg = new MqttMessage(MqttMessage.Type_Connect);
//
//		msg.add("MQTT".getBytes(), 4);// msg.add("MQTT",4);
//		msg.add((byte) 0x4); // Mqtt protocol version 3.1.1
//		msg.add((byte) f); // Connect flags TODO user / name
//
//		msg.add((byte) 0x00); // keep_alive
//		msg.add((byte) m_keepAlive);
//		msg.add(m_clientId);
//
//		if (user != null && user.length() > 0) {
//			msg.add(user);
//		}
//		if (pwd != null && pwd.length() > 0) {
//			msg.add(pwd);
//		}
//
//		if (isAlive() == false) {
//			throw new UserException("connect fail!. socket is not alive (name:%s)", getId());
//		}
//
//		// debug("cnx: mqtt connecting");
//		msg.sendTo(this);
//		msg.reset();
//		// debug("cnx: mqtt sent " + (dbg_ptr)mqtt->parent);
//
//		checkAlive(0);
//
//		add(this);
//		creatTimer();// static func
//
//		if (waitConnact > 0) {
//			TimeUtil t = new TimeUtil();
//			while (t.end_sec() < waitConnact && m_isConnected == false) {
//				TimeUtil.sleep(10);
//			}
//
//			if (m_isConnected == false) {
//				m_client.close();
//				throw new UserException("connact is not receviced (name:%s)", m_clientId);
//			}
//		}
//
//		return this;
//		// onConnect(this, client, null, null);
//	}
//
//	/**
//	 * keep connection before connecting
//	 * 
//	 * @return
//	 */
//	public MqttClientExperimental keepConnection() {
//		add(this);
//		return this;
//	}
//
//	/**
//	 * 
//	 * 
//	 * 
//	 */
//	private static void creatTimer() {
//		//
//		// static timer
//		if (myTimer == null) {
//			myTimer = new Timer2().schedule(new Timer2Task() {
//
//				int loop = 0;
//
//				@Override
//				public void start(Timer2 tmr) {
//
//				}
//
//				@Override
//				public void run(Timer2 tmr) {
//
//					MqttClientExperimental[] arr = null;
//
//					synchronized (clientArr) {
//						arr = clientArr.toArray(new MqttClientExperimental[0]);
//					} // sync
//
//					// MqttBroker.debug(MyLogger.DEBUG, String.format("alive.tmr=%s/%s ",loop, arr.length) );
//
//					if (arr.length == 0)
//						return;
//
//					if ((loop) >= arr.length)
//						loop = 0;
//
//					for (MqttClientExperimental c : arr) {
//						c.myLoop();
//						TimeUtil.sleep(30);
//					}
//
//					// debug(MyLogger.DEBUG, "do myLoop(id:%s)", arr[loop].getId());
//
//					loop++;
//
//				}
//
//				@Override
//				public void stop(Timer2 tmr) {
//
//				}
//
//			}, 10, 1000);
//		}
//	}
//
//	/**
//	 * now this is included in the connect() function.
//	 * 
//	 * @param sec
//	 * @return
//	 */
//	@Deprecated
//	public boolean waitForConnack(double sec) {
//
//		TimeUtil t = new TimeUtil();
//		while (t.end_sec() < sec && m_isConnected == false) {
//			TimeUtil.sleep(10);
//		}
//
//		return m_isConnected;
//
//	}
//
//	/**
//	 * 
//	 * @return
//	 */
//	public boolean isAlive() {
//
//		// aceept
//		if (m_broker != null) {
//			return (m_channel != null && m_broker.getSelector().isAlive(m_channel));
//		}
//
//		// connect
//		return (m_client != null && m_client.isAlive());
//
//	}
//
//	/**
//	 * 
//	 * @param bSendDisconnect
//	 */
//	void discon(boolean bSendDisconnect) {
//		// debug("close " + id().c_str());
//		// m_isConnected = false;
//		if (m_broker != null) // connected to a remote broker
//		{
//			if (bSendDisconnect && m_broker.getSelector().isAlive(m_channel)) {
//				message.create(MqttMessage.Type_Disconnect);
//				message.sendTo(this);
//			}
//
//			try {
//				m_broker.getSelector().channelClose(m_channel);
//			} catch (Exception e) {
//
//			}
//
//			// client->stop();
//			try {
//				m_channel.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			} finally {
//				m_channel = null;
//			}
//
//		}
//
//		if (m_client != null) {
//			if (bSendDisconnect && m_client.isAlive()) {
//				message.create(MqttMessage.Type_Disconnect);
//				message.sendTo(this);
//			}
//
//			try {
//				m_client.close();
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//				m_client = null;
//			}
//
//		}
//
//		pubList.clear();
//
//		acceptQos2Recv.clear();
//
//		clientQos2Recv.clear();
//
//	}
//
//	/**
//	 * 
//	 */
//	public void close() {
//
//		remove(this);
//		// if (myTimer != null) {
//		// myTimer.cancel();
//		// myTimer = null;
//		// }
//
//		discon(true);
//
//		m_broker = null;
//
//	}
//
//	/**
//	 * 
//	 * @param buf
//	 */
//	void write(byte[] buf) {
//		// if (client != null) {
//		// client.send(buf, length);
//
//		try {
//			if (m_broker != null) {
//
//				m_broker.getSelector().send(m_channel, buf);
//			} else if (m_client != null) {
//
//				m_client.send(buf);
//
//			}
//
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		// }
//	}
//
//	void sendAck(byte k, long pi) {
//		byte[] dd = new byte[] { k, 0x2, (byte) (pi >> 8), (byte) (pi & 0xff) };
//		write(dd);
//
//	}
//
//	public String getId() {
//		return m_clientId;
//	}
//
//	public MqttClientExperimental setId(String new_id) {
//		m_clientId = new_id;
//		return this;
//	}
//
//	synchronized void myLoop() {
//
//		// if the socket is not alive with connect mode
//		if (m_reconnectionInerval.end_sec() > 1) {
//			m_reconnectionInerval.start();
//
//			// if (m_broker == null && selector != null && selector.isAlive(m_client) == false) {
//
//			// keep connection(client)
//			if (m_broker == null) {
//
////				if (m_client != null) {
////					System.out.format("test br=%s cli=%s ka=%s ip=%s port=%s \r\n", m_broker, m_client.isAlive(),
////							m_keepAlive, m_ip, m_port);
////				}
//
//				if (m_client == null || m_client.isAlive() == false) {
//
//					if (m_keepAlive != 0) {
//						if (m_ip.trim().length() > 0 && m_port != 0) {
//							try {
//								_connect(m_ip, m_port, m_keepAlive, m_user, m_pwd, 0);
//							} catch (Exception e) {
//								e.printStackTrace();
//							}
//						} // if
//					}
//				} // if
//				else {
//
//					int to=(int) ((m_keepAlive * 1.2) + 3);
//					if (m_tmNoRecv.end_sec() > to ) {
//						
//						
//						debug("no recv err(%s sec)", m_tmNoRecv.end_sec() );
//						
//						m_tmNoRecv.start();
//						m_client.close();
//					}
//
//				}
//
//			} // if
//		}
//
//		// check timeout
//		if (m_checkAlive != 0 && (System.currentTimeMillis() > m_checkAlive)) {
//			// System.out.format("***************doloop (%s) ka(%s) ca(%s) \r\n", getId(), m_keepAlive , (int)((
//			// m_checkAlive- System.currentTimeMillis())/1000) );
//
//			if (m_broker != null) {
//				debug("accepter(%s) is timeover!", getId());
//				// close();
//				discon(true);
//				// debug("closed");
//
//			} else if (m_client != null && m_client.isAlive()) { // if (m_client != null && selector.isAlive(m_client))
//																	// {
//
//				// debug("pingreq");
//				// short pingreq = (byte)MqttMessage.Type_PingReq;// MqttMessage::Type::PingReq;
//				// client->write(( byte[])(&pingreq), 2);
//				byte[] dd = new byte[] { MqttMessage.Type_PingReq, 0 };
//
//				try {
//					write(dd);
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					// e.printStackTrace();
//					discon(false);
//					debug(e);
//
//				}
//
//				checkAlive(0);
//
//				// TODO when many MqttClient passes through a local broker
//				// there is no need to send one PingReq per instance.
//			}
//		}
//
//		if (isAlive()) {
//
//			//
//			// republish
//			{
//				PubItem[] pi;
//				synchronized (pubList) {
//					pi = pubList.values().toArray(new PubItem[0]);
//				}
//
//				for (PubItem p : pi) {
//
//					if (p.qos > 0 && p.resend.end_sec() > p.resendIntervalSec) {
//						p.resend.start();
//
//						p.m.setDup(true);
//
//						p.m.sendTo(this); // resend
//
//						debug("loopPublish: resend  pi=%s ", p.m.packet_identify);
//					}
//
//					if (p.past.end_sec() > this.m_keepAlive) {
//						debug("send err-1");
//						discon(true);//
//					}
//
//				} // for
//			}
//
//			//
//			// check timeover
//			if (tmrCheckStat.end_sec() > 3) {
//				tmrCheckStat.start();
//
//				PubItem[] lst;
//				synchronized (acceptQos2Recv) {
//					lst = acceptQos2Recv.values().toArray(new PubItem[0]);
//				}
//				for (PubItem p : lst) {
//					if (p.past.end_sec() > this.m_keepAlive) {
//						discon(false);
//						debug("accepter.qos2 send time over");
//					}
//				}
//
//				synchronized (clientQos2Recv) {
//					lst = clientQos2Recv.values().toArray(new PubItem[0]);
//				}
//				for (PubItem p : lst) {
//					if (p.past.end_sec() > this.m_keepAlive) {
//						discon(false);
//						debug("client.qos2 send time over");
//					}
//				}
//			}
//
//		}
//
//	}
//
//	// Publish from client to the world
//	// MqttError publish( Topic&, byte[] payload, int pay_length);
//
//	MqttMessage getPublishMessage(Topic topic, byte[] payload, int pay_length, int qos) throws Exception {
//		MqttMessage msg = new MqttMessage((byte) (MqttMessage.Type_Publish | (qos << 1)));
//		msg.add(topic);
//
//		if (qos != 0) {
//
//			long pi = getPacketIdentifier();
//			// add packet identifier
//			msg.add((byte) ((pi >> 8) & 0xff));
//			msg.add((byte) (pi & 0xff));
//
//			msg.packet_identify = pi;
//
//			// synchronized (pubDoneList) {
//			// pubDoneList.clear();
//			// pubDoneList.put(pi, msg);
//			// }
//
//		}
//
//		msg.add(payload, pay_length, false);
//		msg.complete();
//
//		return msg;
//	}
//
//	// publish from local client
//	public synchronized MqttClientExperimental publish(String tstr, byte[] payload, int pay_length, int qos) throws Exception {
//
//		byte res = 0;
//
//		if (isAlive() == false)
//			throw new UserException("not connect");
//
//		if (m_isConnected == false) {
//
//			if (aliveTime.end_sec() > 6) {
//				if (m_client != null) {
//					m_client.close();
//				}
//			}
//
//			throw new UserException("conack is not received");
//		}
//
//		if (workInCallBack && qos != 0) {
//			throw new UserException("publish qos=%s not support in callback func", qos);
//		}
//
//		Topic topic = new Topic(tstr);
//		//
//		//
//		// qos <<= 1;
//
//		MqttMessage msg = getPublishMessage(topic, payload, pay_length, qos);
//
//		// if (m_broker != null) {
//		// res = m_broker.publish(this, topic, msg);
//		// } else
//
//		// if (m_client != null)
//		res = msg.sendTo(this);
//		// else
//		// res = MqttMessage.MqttError_MqttNowhereToSend;
//
//		if (qos > 0) {
//			synchronized (pubList) {
//				pubList.clear();// it removed in puback, also publicomplete
//				pubList.put(msg.packet_identify, new PubItem(topic, (byte) qos, msg, null));
//			}
//
//			debug("pubList.size=%d", pubList.size());
//
//			long pi = msg.packet_identify;
//			TimeUtil t = new TimeUtil();
//			while (qos != 0 && pubList.containsKey(pi)) {
//
//				if (t.end_sec() > m_pubSec) {
//					return this;
//				}
//				TimeUtil.sleep(10);
//
//			} // whlie
//		}
//
//		return this;
//	}
//
//	public boolean qosWaitSeconds(long sec) {
//
//		m_pubSec = sec;
//
//		return true;
//	}
//
//	public MqttClientExperimental publish(String t, String s, int qos) throws Exception {
//		return publish(t, s.getBytes(), s.getBytes().length, qos);
//	}
//
//	public byte subscribe(String topic, IMqtt fun) throws Exception {
//
//		return subscribe(topic, (byte) 0, fun, 0.0);
//	}
//
//	public byte subscribe(String tstr, byte qos, IMqtt fun, double waitSuback) throws Exception {
//
//		if (isAlive() == false)
//			throw new UserException("not connect");
//
//		if (m_isConnected == false) {
//
//			throw new UserException("conack is not received");
//		}
//
//		// debug("subsribe(" + topic.c_str() + ")");
//		byte ret = MqttMessage.MqttError_MqttOk;
//
//		Topic topic = new Topic(tstr);
//
//		if (subscriptions.contains(topic) == false) {
//			subscriptions.add(topic);
//		}
//
//		m_callback = fun;
//		// subscriptions.add(topic);
//
//		if (m_broker == null) // remote broker
//		{
//			long pi = send_subcribe(topic, MqttMessage.Type_Subscribe, qos);
//
//			debug("subscribe t(%s) pi=%s", tstr, pi);
//			//
//			//
//			if (waitSuback > 0) {
//				TimeUtil t = new TimeUtil();
//				while (t.end_sec() < waitSuback && subAckPi.indexOf(pi) == -1) {
//					TimeUtil.sleep(10);
//				}
//
//				if (subAckPi.indexOf(pi) == -1) {
//					m_client.close();
//					throw new UserException("suback is not receviced (name:%s)", m_clientId);
//				}
//			}
//
//			return 0;
//
//		} else {
//			// return parent.subscribe(topic, qos);
//			return 0;
//		}
//	}
//
//	public byte unsubscribe(String t) throws Exception {
//
//		Topic topic = new Topic(t);
//
//		if (subscriptions.contains(topic)) {
//			subscriptions.remove(topic);
//			if (m_broker == null) //
//			{
//				send_unsubscribe(topic, MqttMessage.Type_UnSubscribe, (byte) 0);
//			}
//		}
//		return MqttMessage.MqttError_MqttOk;
//	}
//
//	byte sendTopic(Topic topic, byte type) throws Exception {
//		MqttMessage msg = new MqttMessage((byte) type, (byte) 2);
//
//		// TODO manage packet identifier
//		long pi = getPacketIdentifier();
//
//		msg.add((byte) ((pi >> 8) & 0xff));
//		msg.add((byte) (pi & 0xff));
//
//		msg.add(topic);
//
//		return msg.sendTo(this);
//	}
//
//	long send_subcribe(Topic topic, byte type, byte qos) throws Exception {
//		MqttMessage msg = new MqttMessage((byte) type, (byte) 2);
//
//		// TODO manage packet identifier
//		long pi = getPacketIdentifier();
//
//		msg.add((byte) ((pi >> 8) & 0xff));
//		msg.add((byte) (pi & 0xff));
//
//		msg.add(topic);
//		msg.add((byte) qos);
//
//		msg.sendTo(this);
//
//		return pi;
//	}
//
//	long send_unsubscribe(Topic topic, byte type, byte qos) throws Exception {
//		MqttMessage msg = new MqttMessage((byte) type, (byte) 2);
//
//		// TODO manage packet identifier
//		long pi = getPacketIdentifier();
//
//		msg.add((byte) ((pi >> 8) & 0xff));
//		msg.add((byte) (pi & 0xff));
//
//		msg.add(topic);
//		// msg.add((byte) qos);
//
//		// TODO instead we should wait (state machine) for SUBACK / UNSUBACK ?
//		msg.sendTo(this);
//
//		return pi;
//	}
//
//	// bool isSubscribedTo( Topic& topic) ;
//	boolean isSubscribedTo(Topic topic) {
//
//		// debug("isSubscribedTo sub.size=%s ", subscriptions.size());
//
//		for (Topic subscription : subscriptions) {
//
//			// System.out.format("isSubscribedTo (%s) (%s) \r\n", subscription.str(), topic.str() );
//
//			if (subscription.matches(topic))
//				return true;
//		}
//
//		return false;
//	}
//
//	void resubscribe() throws Exception {
//
//		if (subscriptions.size() != 0) {
//
//			for (Topic topic : subscriptions) {
//
//				send_subcribe(topic, MqttMessage.Type_Subscribe, (byte) 0);
//			}
//
//		}
//	}
//
//	// republish a received publish if it matches any in subscriptions
//	byte publishIfSubscribed(long pi, Topic topic, byte qos, MqttMessage msg) {
//		byte retval = MqttMessage.MqttError_MqttOk;
//
//		if (isSubscribedTo(topic)) {
//
//			retval = msg.sendTo(this);
//
//			if (qos > 0) {
//				synchronized (pubList) { //on broker
//					pubList.put(msg.packet_identify, new PubItem(topic, (byte) qos, msg, null)); 
//				}
//			}
//
//		}
//		return retval;
//	}
//
//	// void clientAlive(int more_seconds);
//
//	void checkAlive(int more_seconds) {
//		if (m_keepAlive != 0) {
//			m_checkAlive = System.currentTimeMillis() + 1000 * (m_keepAlive + more_seconds);
//		} else
//			m_checkAlive = 0;
//	}
//
//	void debug(String f, Object... a) {
//		MqttBroker.debug(f, a);
//	}
//
//	void debug(long lvl, String f, Object... a) {
//
//		MqttBroker.debug(lvl, f, a);
//	}
//
//	void debug(Exception e) {
//		MqttBroker.debug(e);
//	}
//
//	//
//	// void outstring(String prefix, byte[] p, byte len) {
//	// // return;
//	// // Serial << prefix << "='";
//	// // while(len--) Serial << (char)*p++;
//	// // Serial << '\'' << endl;
//	// }
//
//	void processMessage(MqttMessage in) throws Exception {
//
//		MqttMessage mesg = in.copyOf();
//
//		debug(MyLogger.DEBUG, "msg.len=" + mesg.buffer.copyOf().length + " mesg.buf= %s",
//				StringUtil.omitAfter(StringUtil.ByteArrayToHex(mesg.buffer.copyOf()), 100));
//
//		// }
//		// #endif
//		byte[] header = mesg.getVHeader();
//		// byte[] payload;
//		int len;
//		boolean bclose = true;
//
//		DataStream payload = new DataStream();
//
//		switch (mesg.type() & (byte) 0XF0) {
//		case MqttMessage.Type_Connect:// ::Type::Connect:
//
//			if (m_isConnected) {
//				debug("already connected");
//				break;
//			}
//
//			// System.out.println("header=" + StringUtil.ByteArrayToHex(header) + " len=" + header.length);
//			// payload = header+10;
//
//			mqtt_flags = header[7];
//			m_keepAlive = (header[8] << 8) | (header[9]);// ??
//			// if (strncmp("MQTT", header+2,4))
//			if (strncmp("MQTT", header, 2, 4) == false) {
//				debug("bad mqtt header");
//				break;
//			}
//			if (header[6] != 0x04) { // protocol ver 3.1.1
//				debug("unknown level");
//				discon(true);
//				break; // Level 3.1.1
//			}
//
//			int cur = 10;
//
//			// ClientId
//			cur = MqttMessage.getPayload(cur, header, payload);
//			m_clientId = new String(payload.copyOf());// ::string(payload, len);
//
//			setId(m_clientId);
//
//			debug("con id= %s ", m_clientId);
//			// payload += len;
//
//			if ((mqtt_flags & Flags_FlagWill) != 0) // Will topic
//			{
//				// mesg.getString(payload, len); // Will Topic
//				cur = MqttMessage.getPayload(cur, header, payload);
//
//				debug("will topic=%s ", new String(payload.copyOf()));
//				// outstring("WillTopic", payload, len);
//				// payload += len;
//
//				// mesg.getString(payload, len); // Will Message
//				cur = MqttMessage.getPayload(cur, header, payload);
//
//				debug("will message=%s ", new String(payload.copyOf()));
//				// outstring("WillMessage", payload, len);
//				// payload += len;
//			}
//			// FIXME forgetting credential is allowed (security hole)
//			// if ((!mqtt_flags & Flags_FlagUserName)!=0)
//			if ((mqtt_flags & Flags_FlagUserName) != 0) {
//				// mesg.getString(payload, len);
//				cur = MqttMessage.getPayload(cur, header, payload);
//				debug("user=%s ", new String(payload.copyOf()));
//				if (!m_broker.checkUser(new String(payload.copyOf())))
//					break;
//				// payload += len;
//			}
//			if ((mqtt_flags & Flags_FlagPassword) != 0) {
//				// mesg.getString(payload, len);
//				cur = MqttMessage.getPayload(cur, header, payload);
//				debug("pwd=%s ", new String(payload.copyOf()));
//				if (!m_broker.checkPassword(new String(payload.copyOf())))
//					break;
//				// payload += len;
//			}
//
//			debug("Connected client:" + m_clientId + ", keep alive=" + m_keepAlive + ".  "+ this.hashCode() );
//			bclose = false;
//			m_isConnected = true; {
//			MqttMessage msg = new MqttMessage(MqttMessage.Type_ConnAck);
//			msg.add((byte) 0); // Session present (not implemented)
//			msg.add((byte) 0); // Connection accepted
//			msg.sendTo(this);
//
//		}
//			break;
//
//		case MqttMessage.Type_ConnAck:// ::Type::ConnAck:
//
//			debug("id(%s) connack", getId());
//
//			m_isConnected = true;
//			bclose = false;
//			resubscribe();
//			break;
//
//		case MqttMessage.Type_SubAck:// ::Type::SubAck:
//
//			if (true) {
//
//				int v = mesg.vheader;
//
//				long pi = 0;
//				byte[] bb = mesg.buffer.copyOf();
//				pi |= ((bb[v + 0] & 0xff) << 8);
//				pi |= (bb[v + 1] & 0xff);
//
//				debug("id(%s) suback pi=%s vheader=%s", getId(), pi, mesg.vheader);
//
//				synchronized (subAckPi) {
//					subAckPi.add(pi);
//					if (subAckPi.size() > 32) {
//						subAckPi.remove(0);
//					}
//				}
//			}
//
//			// debug(MyLogger.VERBOSE, "msg.len=" + mesg.buffer.copyOf().length + " mesg.buf= %s",
//			// StringUtil.omitAfter(StringUtil.ByteArrayToHex(mesg.buffer.copyOf()), 100));
//
//			if (!m_isConnected)
//				break;
//			// Ignore acks
//			bclose = false;
//			break;
//		case MqttMessage.Type_PubAck:// e::Type::PubAck:
//
//			// debug("id(%s) puback m.size=%d sk=%s ch=%s", getId(), pubDoneList.size(), m_client, m_channel);
//			debug("id(%s) puback  ", getId());
//
//			if (!m_isConnected)
//				break;
//
//			if (true) {
//				byte[] bb = mesg.buffer.copyOf();
//
//				if (bb.length < 2 || bb.length != bb[1] + 2) {
//					debug("malform error %s", StringUtil.ByteArrayToHex(mesg.buffer.copyOf()));
//					break;
//				}
//
//				long pi = 0;
//				pi |= ((bb[2] & 0xff) << 8);
//				pi |= (bb[3] & 0xff);
//				//
//				synchronized (pubList) {
//					pubList.remove(pi);
//				}
//
//				debug("puback publist.size= %s ", pubList.size());
//
//			}
//
//			bclose = false;
//			break;
//
//		case MqttMessage.Type_PubReceived:// e::Type::PubAck:
//
//			debug("id(%s) pubrec", getId());
//			if (!m_isConnected)
//				break;
//
//			if (true) {
//				byte[] bb = mesg.buffer.copyOf();
//
//				if (bb.length < 2 || bb.length != bb[1] + 2) {
//					debug("malform error %s", StringUtil.ByteArrayToHex(mesg.buffer.copyOf()));
//					discon(false);
//					break;
//				}
//
//				long pi = 0;
//				pi = ((bb[2] & 0xff) << 8);
//				pi |= (bb[3] & 0xff);
//
//				//
//				// byte[] dd = new byte[] { MqttMessage.Type_PubRelease | 0x2, 0x2, bb[2], bb[3] };
//				//
//				// // client.send(dd);
//				// write(dd);
//
//				sendAck((byte) (MqttMessage.Type_PubRelease | 0x2), pi);
//			}
//
//			bclose = false;
//			break;
//
//		case MqttMessage.Type_PubRelease:// e::Type::PubAck: 50023346
//
//		// debug("id(%s) pubrel m.size=%d m.done.size=%d ", getId(), pubRelList.size(), pubRelDoneList.size());
//		// if (!m_isConnected)
//		// break;
//		//
//		// if (m_broker != null) {
//		// byte[] bb = mesg.buffer.copyOf();
//		//
//		// if (bb.length < 2 || bb.length != bb[1] + 2) {
//		// debug("malform error %s", StringUtil.ByteArrayToHex(mesg.buffer.copyOf()));
//		// discon(false);
//		// break;
//		// }
//		//
//		// long pi = 0;
//		// pi = ((bb[2] & 0xff) << 8);
//		// pi |= (bb[3] & 0xff);
//		//
//		// PubReleaseItem i = null;
//		//
//		// synchronized (pubRelList) {
//		// // if (pubRelList.containsKey(pi)) {
//		//
//		// i = pubRelList.remove(pi);
//		// // }
//		// }
//		//
//		// if (i != null) {
//		//
//		// if (pubRelDoneList.contains(pi) == false) {
//		//
//		// pubRelDoneList.add(pi);
//		// if (pubRelDoneList.size() > 128)
//		// pubRelDoneList.remove(0);
//		//
//		// m_broker.publish(this, i.t, i.m); // send to subscripes
//		//
//		// // debug("topic=%s tx=%s pubRelList.size= %s ", i.t.str(),
//		// // StringUtil.ByteArrayToHex(i.m.buffer.copyOf() ), pubRelList.size());
//		//
//		// byte[] dd = new byte[] { MqttMessage.Type_PubComplate, 0x2, bb[2], bb[3] };
//		//
//		// // client.send(dd);
//		// write(dd);
//		// } else {
//		// // debug("---")
//		// }
//		// }
//		// }
//		{
//			// debug("id(%s) pubrel ", getId());
//			if (!m_isConnected)
//				break;
//
//			byte[] bb = mesg.buffer.copyOf();
//
//			if (bb.length < 2 || bb.length != bb[1] + 2) {
//				debug("malform error %s", StringUtil.ByteArrayToHex(mesg.buffer.copyOf()));
//				discon(false);
//				break;
//			}
//
//			long pi = 0;
//			pi = ((bb[2] & 0xff) << 8);
//			pi |= (bb[3] & 0xff);
//
//			debug("id(%s) pubrel  pi=%s", getId(), pi);
//
//			if (m_broker != null) {
//
//				// byte[] dd = new byte[] { MqttMessage.Type_PubComplate, 0x2, bb[2], bb[3] };
//				// write(dd);
//				sendAck(MqttMessage.Type_PubComplate, pi);
//
//				PubItem i = null;
//				synchronized (acceptQos2Recv) {
//					i = acceptQos2Recv.remove(pi);
//				} // sync
//
//				if (i != null) {
//
//					m_broker.publish(this, i.t, i.qos, i.data); // send to subscripes
//					// System.out.println("mqtt.broker qos2 is not support yet");
//
//				}
//
//				//
//
//			} else {// client side
//
//				sendAck(MqttMessage.Type_PubComplate, pi);
//
//				PubItem i = null;
//				synchronized (clientQos2Recv) {
//					i = clientQos2Recv.remove(pi);
//				} // sync
//
//				if (i != null) {
//
//					try {
//						workInCallBack = true;
//
//						m_callback.callback(this, i.t.str(), i.data);
//					} finally {
//						workInCallBack = false;
//					}
//				}
//
//			}
//
//			debug(" pubrel  clientQos2Recv.size= %s  acceptQos2Recv.size=%s  subscribies=%s  pi=%s ",
//					acceptQos2Recv.size(), clientQos2Recv.size(), this.subscriptions.size(), pi);
//
//			bclose = false;
//		}
//			break;
//
//		case MqttMessage.Type_PubComplate:// e::Type::PubAck: 70023346
//
//			// debug("id(%s) pubcomp m.size=%d ", getId(), pubDoneList.size());
//			debug("id(%s) pubcomp  ", getId());
//
//			if (!m_isConnected)
//				break;
//
//			if (true) {
//				byte[] bb = mesg.buffer.copyOf();
//
//				if (bb.length < 2 || bb.length != bb[1] + 2) {
//					debug("malform error %s", StringUtil.ByteArrayToHex(mesg.buffer.copyOf()));
//					discon(false);
//					break;
//				}
//
//				long pi = 0;
//				pi |= ((bb[2] & 0xff) << 8);
//				pi |= (bb[3] & 0xff);
//
//				// debug("Type_PubComplate pi=%x ", pi);
//				// synchronized (pubDoneList) {
//				// pubDoneList.remove(pi);
//				// }
//
//				synchronized (pubList) {
//					pubList.remove(pi);
//				}
//
//				debug(" pubcomp  publist.size= %s pi=%s ", pubList.size(), pi);
//
//			}
//
//			bclose = false;
//			break;
//
//		case MqttMessage.Type_PingResp:// ::Type::PingResp:
//			// TODO: no PingResp is suspicious (server dead)
//			debug("id(%s) pingresp ", this.getId());
//			bclose = false;
//			break;
//
//		case MqttMessage.Type_PingReq:// ::Type::PingReq:
//			if (!m_isConnected)
//				break;
//
//			debug("id(%s) pingreq ", this.getId());
//
//			if (m_channel != null) {
//				// int pingreq = MqttMessage.Type_PingResp;// ::Type::PingResp;
//				// client->write(( byte[])(&pingreq), 2);
//				byte[] dd = new byte[] { MqttMessage.Type_PingResp, 0 };
//				// client.send(dd);
//				write(dd);
//
//				bclose = false;
//			} else {
//				debug("internal pingreq ?");
//			}
//			break;
//
//		case MqttMessage.Type_Subscribe:// ::Type::Subscribe:
//		case MqttMessage.Type_UnSubscribe:// ::Type::UnSubscribe:
//		{
//			if (!m_isConnected)
//				break;
//
//			// debug("sub.header=" + StringUtil.ByteArrayToHex(header) + " len=" + header.length);
//
//			long pi = 0;// packet identifier
//			pi |= ((header[0] & 0xff) << 8);
//			pi |= (header[1] & 0xff);
//
//			if ((byte) (mesg.type() & (byte) 0XF0) == (byte) MqttMessage.Type_Subscribe)
//				debug("brk:id(%s) subscribe pi=%s", getId(), pi);
//			else
//				debug("brk:id(%s) unsubscribe", getId());
//
//			cur = 2;// payload = header+2;
//
//			// debug("un/subscribe loop");
//			// while(payload < mesg.end())
//			while (cur < header.length) {
//				// mesg.getString(payload, len); // Topic
//				// debug("topic.len= (%s) header.len=%s cur=%s ", payload.length() , header.length,cur );
//
//				cur = MqttMessage.getPayload(cur, header, payload);
//
//				debug("topic.len= (%s) header.len=%s  cur=%s ", payload.length(), header.length, cur);
//				// outstring(" un/subscribes", payload, len);
//				// subscribe(Topic(payload, len));
//				Topic topic = new Topic(payload.copyOf(), payload.length());
//				// payload += len;
//				if ((byte) (mesg.type() & 0XF0) == (byte) MqttMessage.Type_Subscribe) {
//
//					// System.out.format(" header= %s \n", StringUtil.ByteArrayToHex(msg.));
//
//					// byte qos = *payload++;
//					byte qos = header[cur++];
//
//					if (qos != 0) {
//						debug("subscribe: Unsupported QOS=" + qos);
//					}
//
//					if (subscriptions.contains(topic) == false) {
//						subscriptions.add(topic);
//					}
//
//					MqttMessage msg = new MqttMessage(MqttMessage.Type_SubAck);
//
//					msg.add((byte) ((pi >> 8) & 0xff));
//					msg.add((byte) (pi & 0xff));
//					msg.add((byte) 0);
//					msg.sendTo(this);
//					//
//
//				} else {
//
//					subscriptions.remove(topic);
//
//					// MqttMessage msg = new MqttMessage(MqttMessage.Type_UnSuback);
//					// msg.add(header[0]);
//					// msg.add(header[1]);
//					// msg.sendTo(this);
//
//				}
//			}
//			// debug("end loop");
//			bclose = false;
//			// TODO SUBACK
//		}
//			break;
//
//		case MqttMessage.Type_UnSuback:// ::Type::UnSuback:
//			if (!m_isConnected)
//				break;
//
//			debug("id(%s) unsuback ", this.getId());
//
//			bclose = false;
//			break;
//
//		case MqttMessage.Type_Publish:// ::Type::Publish:
//
//			// debug("brk: id(%s) pub ", this.getId());
//			// #ifdef TINY_MQTT_DEBUG
//			// Serial + "publish " + mqtt_connected + '/' + (long) client + endl;
//			// #endif
//			if (m_isConnected || m_broker != null) {
//				byte qos = (byte) ((mesg.type() & 0x6) >> 1);
//
//				byte retain = (byte) (mesg.type() & 0x1);
//
//				byte dup = (byte) ((mesg.type() & 0x8) >> 3);
//
//				// System.out.format(" pub qos=%s retain=%s dup=%s \r\n", qos, retain, dup);
//
//				cur = 0;
//
//				// mesg.getString(payload, len);
//				cur = MqttMessage.getPayload(cur, header, payload);
//
//				Topic tp = new Topic(payload.copyOf(), payload.length());
//
//				// debug("pub.topic=%s", new String(payload.copyOf()));
//
//				long pi = 0;
//				if (qos != 0) {
//
//					pi |= ((header[cur + 0] & 0xff) << 8);
//					pi |= (header[cur + 1] & 0xff);
//
//					cur += 2;
//
//					// exactly once!
//
//				}
//
//				debug("brk:id(%s) pub pi=%s tp=[%s]", this.getId(), pi, tp.str());
//
//				byte[] data = Arrays.copyOfRange(header, cur, header.length);// rest of packet
//
//				if (qos != 0)
//					cur += 2; // ignore packet identifier if any
//				len = mesg.length() - cur;
//
//				//
//				//
//				//
//				if (m_broker == null) { // client
//
//					if (m_callback != null && isSubscribedTo(tp)) {
//
//						if (qos == 0) {
//
//							try {
//								workInCallBack = true;
//								m_callback.callback(this, tp.str(), data);
//							} finally {
//								workInCallBack = false;
//							}
//
//						} else if (qos == 1) {
//
//							sendAck(MqttMessage.Type_PubAck, pi);
//
//							try {
//								workInCallBack = true;
//
//								m_callback.callback(this, tp.str(), data);
//							} finally {
//								workInCallBack = false;
//							}
//
//						} else if (qos == 2) {
//							sendAck(MqttMessage.Type_PubReceived, pi);
//
//							synchronized (clientQos2Recv) {
//								if (clientQos2Recv.containsKey(pi) == false) {
//									clientQos2Recv.put(pi, new PubItem(tp, qos, data));
//								}
//							}
//
//						}
//
//						receviedCount++;
//
//					}
//				}
//
//				else if (m_broker != null) // broker
//				{
//					// debug("publishing to parent");
//
//					if (qos == 0) {
//						m_broker.publish(this, tp, qos, data);
//					}
//
//					else if (qos == 1) {
//
//						// MqttMessage newMsg = getPublishMessage(published, data, data.length, 0);
//						mesg.packet_identify = pi;
//						mesg.setDup(false);
//						m_broker.publish(this, tp, qos, data);
//
//						sendAck(MqttMessage.Type_PubAck, pi);
//
//					} else if (qos == 2) {
//
//						sendAck(MqttMessage.Type_PubReceived, pi);
//
//						synchronized (acceptQos2Recv) {
//							if (acceptQos2Recv.containsKey(pi) == false) {
//								mesg.packet_identify = pi;
//								mesg.setDup(false);
//								// acceptQos2Recv.put(pi, new PubItem(tp, qos, mesg));
//								acceptQos2Recv.put(pi, new PubItem(tp, qos, mesg, data));
//							}
//						}
//
//					}
//
//					// else if (qos == 2) {
//					//
//					// if (pubRelDoneList.contains(pi) == false) {
//					//
//					// MqttMessage nmsg = getPublishMessage(published, data, data.length, 0);
//					// // parent.publish(this, published, mesg);
//					// //
//					// synchronized (pubRelList) {
//					// if (pubRelList.containsKey(pi) == false) {
//					// pubRelList.clear();
//					// pubRelList.put(pi, new PubReleaseItem(published, nmsg));
//					// }
//					// } // sync
//					// }
//					//
//					// // debug("need to sned Type_PubReceived");
//					// byte[] dd = new byte[] { MqttMessage.Type_PubReceived, 0x2, (byte) (pi >> 8),
//					// (byte) (pi & 0xff) };
//					// // client.send(dd);
//					// write(dd);
//					//
//					// }
//
//				}
//				bclose = false;
//			}
//			break;
//
//		case MqttMessage.Type_Disconnect:// ::Type::Disconnect:
//			// TODO should discard any will msg
//			debug("id(%s) discon ", this.getId());
//
//			discon(false);
//
//			if (!m_isConnected)
//				break;
//			m_isConnected = false;
//
//			bclose = false;
//			break;
//
//		default:
//			bclose = true;
//			break;
//		}
//
//		if (bclose) {
//			debug("*************** Error msg %x", mesg.type());
//			// mesg.hexdump("-------ERROR ------");
//			// dump();
//			// Serial + endl;
//			discon(true);
//		} else {
//
//			if (m_broker != null) {
//				checkAlive(10);
//			}
//
//			// clientAlive(0);
//		}
//	}
//
//	boolean strncmp(String s, byte[] b, int from, int length) {
//
//		if (s.length() != (length))
//			return false;
//
//		for (int n = 0; n < s.length(); n++) {
//			if (s.charAt(n) != b[from + n])
//				return false;
//		}
//
//		return true;
//
//	}
//
//	public SocketChannel handle() {
//		return m_channel;
//	}
//
//	//
//	//
//	// server side
//	//
//	@Override
//	public TmsItem received(SocketChannel ch, byte[] rxd) {
//
//		for (int n = 0; n < rxd.length; n++) {
//
//			byte c = rxd[n];
//
//			message.incoming(c);
//
//			if (message.state == MqttMessage.State_Error) {
//
//				debug(MyLogger.WARNING, "MqttMessage.State_Error");
//				try {
//
//					ch.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//
//				}
//				return null;
//			}
//
//			if (message.type() != 0) {
//
//				debug(MyLogger.DEBUG, "recv type= %x   len=%s   state=%s ", message.type(), message.buffer.length(),
//						message.state);
//
//				try {
//					processMessage(message);
//				} catch (Exception e) {
//					debug(e);
//					try {
//						ch.close();
//					} catch (Exception ex) {
//
//					}
//				}
//				message.reset();
//			}
//		} // for
//
//		return null;
//	}
//
//	@Override
//	public void disconnected(SocketChannel ch) {
//		debug("id(%s) socket disconnected", getId());
//
//		m_isConnected = false;
//	}
//
//	@Override
//	public Object accepteded(SocketChannel ch) {
//		debug("accepted " + ch);
//		return null;
//	}
//
//	@Override
//	public boolean connected(SocketChannel channel, Object userojb) {
//		debug("connected");
//		return true;
//	}
//
//	//
//	//
//	//
//	// client side
//	//
//
//	@Override
//	public void recv(SyncSocket ask, byte[] buf, int len) {
//		
//		m_tmNoRecv.start();
//
//		byte[] rxd = Arrays.copyOf(buf, len);
//
//		debug(MyLogger.DEBUG, "client.recv=%s len=%s", StringUtil.omitAfter(StringUtil.ByteArrayToHex(rxd), 100),
//				rxd.length);
//
//		for (int n = 0; n < rxd.length; n++) {
//
//			byte c = rxd[n];
//
//			message.incoming(c);
//			if (message.type() != 0) {
//
//				debug(MyLogger.DEBUG, "recv type= %x", message.type());
//
//				try {
//					processMessage(message);
//				} catch (Exception e) {
//					debug(e);
//					try {
//						ask.close();
//					} catch (Exception ex) {
//
//					}
//				}
//				message.reset();
//			}
//		} // for
//
//	
//
//	}
//
//	@Override
//	public void connected(SyncSocket ask) {
//		debug("client.connected");
//		m_tmNoRecv.start();
//
//	}
//
//	@Override
//	public void disconnected(SyncSocket ask) {
//		debug("client.id(%s) socket disconnected", getId());
//
//		m_isConnected = false;
//
//	}

};