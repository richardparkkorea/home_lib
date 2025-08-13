package home.lib.net.tms.mqtt3;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

import home.lib.io.FilenameUtils;
import home.lib.lang.Timer2;
import home.lib.lang.Timer2Task;
import home.lib.lang.UserException;
import home.lib.log.ILogger;
import home.lib.log.MyLogger;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

public class MqttBroker3 implements MqttSelectorListener {
	enum State {
		Disconnected, // Also the initial state
		Connecting, // connect and sends a fake publish to avoid circular cnx
		Connected, // this->broker is connected and circular cnx avoided
	}

	private static ILogger m_logger = null;

	public static long debugLevel = MyLogger.EXCEPTION;;

	// final public static long Verbose = 0x1;
	//
	// final public static long Debug = 0x2;

	// ArrayList<MqttClient> clients = new ArrayList<MqttClient>();

	Map<SocketChannel, MqttClient3> m_clients = new HashMap<SocketChannel, MqttClient3>();

	MqttSelector m_server = new MqttSelector(this);

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

	public MqttBroker3 setCallBack(IMqttBroker3 cb) {
		m_callback = cb;
		return this;
	}

	// String c_str() {
	// return "127.0.0.1";
	// }

	MqttSelector getSelector() {
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

	// ~MqttBroker();

	public MqttBroker3 begin() throws Exception {
		// server->begin();
		if (isAlive())
			throw new UserException("it's alive");

		m_server.bind("0.0.0.0", m_port, 1024).waitForBind(8);

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
		for (MqttClient3 client : m_clients.values()) {

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
	void addClient(SocketChannel ask, MqttClient3 client) {
		synchronized (m_clients) {
			m_clients.put(ask, client);

			// sys_publish("$SYS/broker/clients/connected", "" + m_clients.size());
		}
	}

	void removeClient(SocketChannel ask) {
		if (ask == null)
			return;

		synchronized (m_clients) {
			m_clients.remove(ask);

			// sys_publish("$SYS/broker/clients/disconnected", "" + m_clients.size());
		}

	}

	MqttClient3 getClient(SocketChannel ask) {

		synchronized (m_clients) {
			return m_clients.get(ask);
		} // sync

	}

	boolean compareString(byte[] good, byte[] str, byte str_len) {
		return Arrays.equals(good, str);

	}

	public static void debug(String f, Object... a) {
		debug(MyLogger.VERBOSE, f, a);
	}

	public static void debug(long lvl, String f, Object... a) {

		if (m_logger == null) {

			if ((debugLevel & lvl) != 0) {

				System.out.println(TimeUtil.now() + " " + String.format(f, a));
			}
		} else {

			m_logger.l((int) lvl, "mqtt", f, a);

		}

	}

	public static void debug(Exception e) {

		System.out.println(UserException.getStackTrace(e));
		// debug(MyLogger.EXCEPTION, "mqtt", "%s", UserException.getStackTrace(e));

	}

	long con_count = 0;
	long discon_count = 0;
	long recv_count = 0;

	@Override
	public void received(SocketChannel ask, byte[] rxd) {

		recv_count++;

		MqttClient3 cli = getClient(ask);

		if (cli != null) {
			cli.received(ask, rxd);
		}
		// return null;
	}

	@Override
	public void disconnected(SocketChannel ask) {
		discon_count++;

		removeClient(ask);

		MqttClient3 cli = getClient(ask);
		if (cli != null) {
			cli.disconnected(ask);
		}

	}

	@Override
	public Object accepteded(SocketChannel ask) {
		con_count++;

		try {
			addClient(ask, new MqttClient3(this, ask));

			MqttClient3 cli = getClient(ask);

			if (cli != null) {
				cli.accepteded(ask);
			}
		} catch (Exception ex) {
			ex.printStackTrace();

			try {
				ask.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return null;
	}

	@Override
	public boolean connected(SocketChannel channel, Object userojb) {

		debug("connected");

		return false;
	}

	public static void setLogger(ILogger l) {
		m_logger = l;
	}

	@Override
	public String toString() {

		String s = String.format("mqttbrk port(%s) cli(%s) con(%s) discon(%s)  cc(%s) qcnt(%s) qbytes(%s) qsize(%s)",
				getPort(), clientsCount(), con_count, discon_count, MqttClient3.mgrClientCount(),
				StringUtil.formatCount(m_queueCountOfClients), StringUtil.formatBytesSize(m_bytesSizeOfClients),
				StringUtil.formatCount(m_qos12CountOfClients));

		return s;

	}

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

			//if (mask == null || c.getId().equals(mask)) {
			if (mask == null || FilenameUtils.wildcardMatch(c.getId(), mask)) {
			

				if (m.containsKey(c.getId()) == false) {
					m.put(c.getId(), new ArrayList<MqttClient3>());
					m.get(c.getId()).add(c);
				}
				else {
					m.get(c.getId()).add(c);
				}
			} // if

		} // for

		return m;
	}

};