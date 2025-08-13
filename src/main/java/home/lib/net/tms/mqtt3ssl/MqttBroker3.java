package home.lib.net.tms.mqtt3ssl;

import java.io.IOException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

import home.lib.io.FilenameUtils;
import home.lib.lang.Timer2;
import home.lib.lang.Timer2Task;
import home.lib.lang.UserException;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

public class MqttBroker3 implements MqttServerSocketListener {
	enum State {
		Disconnected, // Also the initial state
		Connecting, // connect and sends a fake publish to avoid circular cnx
		Connected, // this->broker is connected and circular cnx avoided
	}

	// private static ILogger m_logger = null;
	final public static int VERBOSE = 0x00000001;
	final public static int LOG = 0x00000002;
	final public static int DEBUG = 0x00000004;
	final public static int WARNING = 0x00000008;
	final public static int EXCEPTION = 0x00000010;

	public static long debugLevel = EXCEPTION;;

	// final public static long Verbose = 0x1;
	//
	// final public static long Debug = 0x2;

	// ArrayList<MqttClient> clients = new ArrayList<MqttClient>();

	Map<MqttSocket, MqttClient3> m_clients = new HashMap<MqttSocket, MqttClient3>();

	MqttServerSocket m_server = new MqttServerSocket(this);

	// String auth_user = "guest";
	// String auth_password = "guest";
	// State m_state = State.Disconnected;

	// MqttClient broker = null;
	int m_port = 0;

	Timer2 myTimer = null;

	IMqttBroker3 m_callback = null;

	private long m_queueCountOfClients = 0;

	private long m_bytesSizeOfClients = 0;

	private long m_qos12CountOfClients;

	String m_pwd;
	String m_jks;
	private long m_eventCountOfClientsPerSec = 0;

	private long con_count = 0;
	private long discon_count = 0;
	private long recv_count = 0;
	private long m_eventCountOfClients;

	public MqttBroker3 setCallBack(IMqttBroker3 cb) {
		m_callback = cb;
		return this;
	}

	// String c_str() {
	// return "127.0.0.1";
	// }

	MqttServerSocket getSelector() {
		return m_server;
	}

	// MqttClient sys_cli;

	// public void sys_publish(String t, String p) {
	// try {
	// byte[] d = p.getBytes();
	// sys_cli.publish(t, d, d.length, 0);
	// } catch (Exception e) {
	// debug(e);
	// }
	// }

	// public:
	// TODO limit max number of clients

	public MqttBroker3(int port) {
		m_port = port;

		// m_server.setChannelTimeoutSec(60 * 60*24);

		// #ifdef TCP_ASYNC
		// server->onClient(onClient, this);
		// #endif
	}

	public int getPort() {
		return m_port;
	}

	/**
	 * 
	 * 
	 * @return
	 * @throws Exception
	 */
	public MqttBroker3 setJks(String pwd, String jks) {

		m_pwd = pwd;
		m_jks = jks;
		return this;

	}

	/**
	 * 
	 * @param pwd
	 * @param jks
	 * @return
	 * @throws Exception
	 */
	public MqttBroker3 begin() throws Exception {
		// server->begin();
		if (isAlive())
			throw new UserException("it's alive");

		m_server.bind("0.0.0.0", m_port, m_pwd, m_jks).waitForBind(8);

		// server = new SyncSocketServer("0.0.0.0", m_port, this);
		// server.receiveTimeoutSec(600);

		if (myTimer == null) {

			myTimer = new Timer2().schedule(new Timer2Task() {

				TimeUtil sec = new TimeUtil();
				TimeUtil uptime = new TimeUtil();

				@Override
				public void start(Timer2 tmr) {

				}

				@Override
				public void run(Timer2 tmr) {
					myLoop();

					if (sec.end_ms() >= 1000) {
						sec.start();
						// sys_publish("$SYS/broker/uptime", "" + uptime.end_sec());
					}

				}

				@Override
				public void stop(Timer2 tmr) {

				}

			}, 1, 100);

		}

		// link $sys client
		// sys_cli = new MqttClient().setId("sys").keepConnection().connect("127.0.0.1", m_port);

		return this;

	}

	public void close() {

		try {
			if (m_server != null)
				m_server.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			m_clients.clear();

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (myTimer != null) {
			myTimer.cancel();
		}

		m_server = null;
		myTimer = null;

	}

	private void myLoop() {
		// #ifndef TCP_ASYNC
		// WiFiClient client = server->available();
		//
		// if (client)
		// {
		// onClient(this, &client);
		// }
		// #endif
		// if (broker != null) {
		// // TODO should monitor broker's activity.
		// // 1 When broker disconnect and reconnect we have to re-subscribe
		// broker.loop();
		// }

		// for(auto it=clients.begin(); it!=clients.end(); it++)
		// use index because size can change during the loop
		// for (int i = 0; i < clients.size(); i++) {
		// MqttClient client = clients.get(i);
		MqttClient3[] cls = null;
		synchronized (m_clients) {
			cls = m_clients.values().toArray(new MqttClient3[0]);
		} // sync

		if (cls != null) {

			long bytes = 0;
			long cnt = 0;
			long qcnt = 0;

			for (MqttClient3 client : cls) {

				if (client.isAlive()) {

					client.myLoop();

					bytes += client.publishQueueBytesSize();

					cnt += client.publishQueueSize();

					qcnt += client.qos12QueueSize();

					TimeUtil.sleep(30);

				} else {

					// debug("before client.size=%d ", m_clients.size());

					removeClient(client.handle());

					// debug("Client %s Disconnected client.size=%d handle=%s server=%s ", client.getId(),
					// m_clients.size(), client.handle(), m_server);

					break;
				}

				if (isAlive() == false)
					return;
			} // for

			m_queueCountOfClients = cnt;

			m_bytesSizeOfClients = bytes;

			m_qos12CountOfClients = qcnt;

			m_eventCountOfClientsPerSec = m_eventCountOfClients;
			m_eventCountOfClients = 0;
		} // if
	}

	// void connect(String host) throws Exception {
	// connect(host, 1833);
	// }
	//
	// void connect(String host, int port) throws Exception {
	// if (broker == null)
	// broker = new MqttClient();
	//
	// broker.connect(host, port);
	// broker.parent = this; // Because connect removed the link
	// }

	// boolean connected() {
	// return m_state == State.Connected;
	// }

	public int clientsCount() {
		return m_clients.size();
	}

	public boolean isAlive() {
		return (m_server != null && m_server.isAlive());
	}

	// void dump() {
	// dump("");
	// }

	// void dump(String indent)
	// {
	// for(MqttClient client: clients)
	// client.dump(indent);
	// }

	// private:
	// friend class MqttClient;

	// static void onClient(void*, TcpClient*);
	// static void onClient(MqttBroker broker_ptr, SyncSocket client)
	// {
	// //MqttBroker* broker = static_cast<MqttBroker*>(broker_ptr);
	//
	// broker_ptr.addClient(new MqttClient(broker_ptr, client));
	// debug("New client");
	// }
	//
	//

	boolean checkUser(String clientId, byte[] user, byte[] pwd) {

		if (m_callback != null) {
			return m_callback.checkUser(clientId, user, pwd);
		}
		// return compareString(user, user, len);
		// debug("checkUser(%s)", user);

		return true;
	}

	// boolean checkUser(byte[] user, byte len) {
	// return compareString(user, user, len);
	// }
	//
	// boolean checkPassword(byte[] password, byte len) {
	// return compareString(password, password, len);
	// }

	byte publish(MqttClient3 source, Topic topic, byte[] data) throws Exception {
		byte retval = MqttMessage.MqttError_MqttOk;

		// debug("publish ");
		// int i = 0;
		MqttClient3[] arr = new MqttClient3[0];

		synchronized (m_clients) {
			arr = m_clients.values().toArray(new MqttClient3[0]);
		}

		for (MqttClient3 client : arr) {

			// debug("brk.publish pi=%s topic=%s data.len=%s ", msg.packet_identify, topic.str(), data.length);
			if (source.equals(client) == false) {// not me
				retval = client.publishIfSubscribed(topic, data);
			}

		}
		return retval;
	}

	// byte subscribe(Topic topic, byte qos) {
	// if (broker != null && broker.connected()) {
	// return broker.subscribe(topic, qos);
	// }
	// return MqttMessage.MqttError_MqttNowhereToSend;
	// }

	// For clients that are added not by the broker itself
	void addClient(MqttSocket ask, MqttClient3 client) {
		synchronized (m_clients) {

			// int nnn=m_clients.size();

			m_clients.put(ask, client);

			// int bbb=m_clients.size();

			// int ccc=bbb;

			// MqttClient3 aaa=m_clients.get(ask);

			// String sss=aaa.getId();

			// sys_publish("$SYS/broker/clients/connected", "" + m_clients.size());
		}
	}

	void removeClient(MqttSocket ask) {
		if (ask == null)
			return;

		synchronized (m_clients) {
			m_clients.remove(ask);

			// sys_publish("$SYS/broker/clients/disconnected", "" + m_clients.size());
		}

	}

	MqttClient3 getClient(MqttSocket ask) {

		synchronized (m_clients) {
			return m_clients.get(ask);
		} // sync

	}

	boolean compareString(byte[] good, byte[] str, byte str_len) {
		return Arrays.equals(good, str);

	}

	public static void debug(String f, Object... a) {
		debug(VERBOSE, f, a);
	}

	public static void debug(long lvl, String f, Object... a) {

		// if (m_logger == null) {

		if ((debugLevel & lvl) != 0) {

			System.out.println(TimeUtil.now() + " " + String.format(f, a));
		}
		// } else {
		//
		// m_logger.l((int) lvl, "mqtt", f, a);
		//
		// }

	}

	public static void debug(Exception e) {

		System.out.println(UserException.getStackTrace(e));
		// debug(MyLogger.EXCEPTION, "mqtt", "%s", UserException.getStackTrace(e));

	}

	@Override
	public void selectorReceived(MqttSocket ask, byte[] rxd) {

		recv_count++;

		MqttClient3 cli = getClient(ask);

		if (cli != null) {
			cli.selectorReceived(ask, rxd);
		}
		// return null;
	}

	@Override
	public void selectorDisconnected(MqttSocket ask) {
		discon_count++;

		removeClient(ask);

		MqttClient3 cli = getClient(ask);
		if (cli != null) {
			cli.selectorDisconnected(ask);
		}

	}

	@Override
	public Object selectorAccepteded(MqttSocket ask) {
		con_count++;

		try {
			addClient(ask, new MqttClient3(this, ask));

			// int nn0=m_clients.size();

			MqttClient3 cli = getClient(ask);

			// int nn=m_clients.size();

			// MqttClient3 xxx=m_clients.get(ask);

			if (cli != null) {
				cli.selectorAccepteded(ask);
			}
		} catch (Exception ex) {
			ex.printStackTrace();

			try {
				ask.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return null;
	}

	@Override
	public boolean selectorConnected(MqttSocket channel, Object userojb) {

		debug("connected");

		return false;
	}

	// public static void setLogger(ILogger l) {
	// m_logger = l;
	// }

	public String selectorStatus() {
		return "" + m_server;
	}

	public long getQueueCountOfClients() {
		return m_queueCountOfClients;
	}

	public long getQueueBytesOfClient() {
		return m_bytesSizeOfClients;
	}

	public Map<String, ArrayList<MqttClient3>> getClientIds() {
		return getClientIds(null);
	}

	public Map<String, ArrayList<MqttClient3>> getClientIds(String mask) {

		MqttClient3[] cl = null;

		synchronized (m_clients) {
			cl = m_clients.values().toArray(new MqttClient3[0]);
		}

		Map<String, ArrayList<MqttClient3>> m = new HashMap<String, ArrayList<MqttClient3>>();
		for (MqttClient3 c : cl) {

			// if (mask == null || c.getId().equals(mask)) {
			if (mask == null || FilenameUtils.wildcardMatch(c.getId(), mask)) {

				if (m.containsKey(c.getId()) == false) {
					m.put(c.getId(), new ArrayList<MqttClient3>());
					m.get(c.getId()).add(c);
				} else {
					m.get(c.getId()).add(c);
				}
			} // if

		} // for

		return m;
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 */
	public static String help() {

		String s = " ./gca.sh 114.200.254.181\r\n"//
				+ "\r\n"//
				+ "\r\n"//

				+ "openssl pkcs12 -export -in 114.200.254.181.crt -inkey 114.200.254.181.key -out keystore.p12 -name mykey -CAfile ca.crt -caname root -chain\r\n"//
				+ "\r\n"//
				+ "\r\n"//

				+ "keytool -importkeystore -srckeystore keystore.p12 -srcstoretype PKCS12 -destkeystore keystore.jks -deststoretype JKS ";//

		return s;
	}

	@Override
	public String toString() {

		String s = String.format(
				"mqt3tsslbrk port(%s) cli(%s) con(%s) discon(%s)  cc(%s) qcnt(%s) qbytes(%s) qsize(%s) eps(%s)",
				getPort(), clientsCount(), con_count, discon_count, MqttClient3.mgrClientCount(),
				StringUtil.formatCount(m_queueCountOfClients), StringUtil.formatBytesSize(m_bytesSizeOfClients),
				StringUtil.formatCount(m_qos12CountOfClients), StringUtil.formatCount(m_eventCountOfClientsPerSec));

		return s;

	}

	public void increaseEventCount() {
		m_eventCountOfClients++;

	}

};