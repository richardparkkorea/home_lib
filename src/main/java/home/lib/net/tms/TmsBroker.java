package home.lib.net.tms;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import home.lib.io.FilenameUtils;
import home.lib.lang.Timer2;
import home.lib.lang.UserException;
import home.lib.log.ILogger;
import home.lib.log.ULogger;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

/**
 * 
 * addevent flow : channel.interface -> channel.queue -> broker.interface, -> broker.queue
 * 
 * 
 * 20201112
 * 
 * accepter max task count = (clients.size +1 )
 * 
 * 20201119
 * 
 * can not send and receive data until successful login. ( see isLoggedIn(); )
 * 
 * 20201209
 * 
 * if(c.getPath()!=null) {//not receive name from client yet
 * 
 * 
 * 
 * @author richard
 *
 */
public class TmsBroker implements TmsSelectorListener {

	// private Object _conlock = new Object();

	private static ArrayList<TmsBroker> m_arrFactory = new ArrayList<TmsBroker>();

	public static TmsBroker[] getFactories() {
		synchronized (m_arrFactory) {
			return m_arrFactory.toArray(new TmsBroker[0]);
		}
	}
	////////////////////////////////////////////////////////////////////

	private TmsBroker _this = this;

	private Map<TmsChannel, SocketChannel> m_clients = new HashMap<TmsChannel, SocketChannel>();

	private Map<Long, TmsChannel> m_clientIds = new HashMap<Long, TmsChannel>();

	// private Map<SocketChannel, TmsChannel> m_channels = new HashMap<SocketChannel, TmsChannel>();

	private Map<String, ArrayList<TmsChannel>> m_connectGroups = new HashMap<String, ArrayList<TmsChannel>>();

	private Map<String, ArrayList<TmsChannel>> m_acceptGroups = new HashMap<String, ArrayList<TmsChannel>>();

	private TmsChannel[] m_clientArr = new TmsChannel[0];/// for speed up & sync

	// private Timer2 s_send = null;

	private Timer2 s_keepAlive = null;
	/**
	 * 
	 */
	private String m_name = "tmsbroker";

	// private double m_dReceiveTimeoutSec = 60;

	private double m_reconnectableInterval = 18.0;

	private long m_lSocketBufferSize = 1024 * 64;

	// protected int m_jt_maxQueue = 32;

	// protected long m_jt_resendMs = 0;

	protected long m_jt_timeoutMs = 6000;

	protected long m_sendPacketCountPerSec = 0;

	protected long m_sendBytesCountPerSec = 0;

	// private int m_jobPeekCount = 1;//

	private ILogger m_logger = null;

	private TmsSelector m_selector = new TmsSelector(this);

	// private LinkedList<TmsEvent> m_eQueue = new LinkedList<TmsEvent>();

	TmsEventQueue m_eQueue = new TmsEventQueue(1024 * 512);

	// private int m_maxEventCount = 1024 * 512;

	private ThreadPoolExecutor m_executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

	// private int m_directSentCount=0;
	// public int m_taskQueueCount = 0;

	private TimeUtil m_tmAlive = new TimeUtil();

	private int m_connectedCount;

	private int m_disconnectedCount;

	private Map<Long, String> m_idList = new HashMap<Long, String>();

	// private String m_bindIp = null;

	// private int m_bindPort = 0;

	TmsChannelInterface m_brokerChannelInterface = null;

	// private boolean m_aliveAtLeastOnce = false;

	long m_sum_send_count = 0;
	long m_sum_send_bytes = 0;

	/**
	 * 
	 * @param name
	 * @param ip
	 * @param port
	 * @throws Exception
	 */
	public TmsBroker(String name, String ip, int port) throws Exception {

		this(name, ip, port, null, null);

	}

	public TmsBroker(String name, String ip, int port, ILogger l) throws Exception {

		this(name, ip, port, l, null);

	}

	public TmsBroker(String name, String ip, int port, ILogger l, TmsChannelInterface inter) throws Exception {

		m_logger = l;
		m_name = name;

		m_brokerChannelInterface = inter;
		// s_send = new Timer2().schedule(new sendTask(), 10, 1);
		s_keepAlive = new Timer2().schedule(new keepAliveTask(), 10, 10);

		synchronized (m_arrFactory) {
			m_arrFactory.add(this);
		}

		select(ip, port);

	}

	/**
	 * 
	 * 
	 * @return
	 */
	public long idGenerate() {
		synchronized (m_idList) {

			long id = System.currentTimeMillis();
			// id generate
			while (m_idList.containsKey(id)) {

				id = System.currentTimeMillis();

				TimeUtil.sleep(1);
			}

			m_idList.put(id, "" + id);
			return id;
		} // sync
	}

	public boolean isAlive() {
		return m_selector.isAlive();
	}

	/**
	 * 
	 * @param ch
	 * @param event
	 * @param rx
	 * @param tm
	 * @return
	 */
	public boolean addEvent(TmsChannel ch, char event, byte[] rx, TmsItem tm) {
		synchronized (m_eQueue) {

			// is bind with interface handle?
			TmsChannelInterface ci = ch.getChannelInterface();
			if (ci != null) {

				switch (event) {
				case 'c':
					ci.onConnected(ch);
					return true;

				case 'a':
					ci.onAccepteded(ch);
					return true;

				case 'r':
					ci.onReceived(ch, rx, tm);
					return true;
				case 'x':
					ci.onDisconnected(ch);
					return true;
				case 'd':
					ci.onDisconnected(ch);
					return true;
				case 'e':
					ci.onError(ch, rx);
					return true;

				case 'l':
					ci.onLink(ch);
					return true;

				default:
					debug(ULogger.VERBOSE, "uknown event (%s)", event);
					return true;
				}

				// return true;//never reach at this line
			}

			// is bind with even queue?
			TmsEventQueue bq = ch.getBindEventQeue();
			if (bq != null) {
				try {
					return bq.add(ch, event, rx, tm);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// if (event == 'r' && m_eQueue.size() > m_maxEventCount) {
			// debug(this, "selector event queue is full (max:%d) \r\n", m_eQueue.size());
			// return false;
			// }
			//
			// if (m_eQueue.size() > m_maxEventCount * 2) {
			// debug(this, "event queue has been exceeded(%s) ", m_eQueue.size());
			// }
			//
			// m_eQueue.add(new TmsEvent(ch, event, rx, tm));
			// return true;

			//
			// if

			if (m_brokerChannelInterface != null) {

				switch (event) {
				case 'c':
					m_brokerChannelInterface.onConnected(ch);
					return true;

				case 'a':
					m_brokerChannelInterface.onAccepteded(ch);
					return true;

				case 'r':
					m_brokerChannelInterface.onReceived(ch, rx, tm);
					return true;
				case 'x':
					m_brokerChannelInterface.onDisconnected(ch);
					return true;
				case 'd':
					m_brokerChannelInterface.onDisconnected(ch);
					return true;
				case 'e':
					m_brokerChannelInterface.onError(ch, rx);
					return true;

				case 'l':
					m_brokerChannelInterface.onLink(ch);
					return true;

				}

				debug(ULogger.VERBOSE, "uknown event (%s)", event);
				return true;

			}

			//
			//
			return m_eQueue.add(ch, event, rx, tm);

		}
	}

	// public void setChannelInterface(TmsChannelInterface ti) {
	// m_brokerChannelInterface = ti;
	// }
	//
	// public TmsChannelInterface getChannelInterface() {
	// return m_brokerChannelInterface;
	// }

	/**
	 * 
	 * @return
	 */
	public TmsEvent pollEvent() {
		// synchronized (m_eQueue) {
		// return m_eQueue.pollFirst();
		// }

		return m_eQueue.poll();
	}

	/**
	 * 
	 * @return
	 */
	public int getEventCount() {
		// synchronized (m_eQueue) {
		// return m_eQueue.size();
		// }

		return m_eQueue.size();

	}

	/**
	 * 
	 * @param bindIp
	 * @param port
	 * @throws Exception
	 */
	private TmsBroker select(String bindIp, int port) throws Exception {

		if (isAlive()) {
			throw new UserException("server(port:%d) is alive", port);
		}

		m_selector.setLogger(m_logger);
		// m_aliveAtLeastOnce = false;

		m_selector.bind(bindIp, port);// .get();

		// m_bindIp = bindIp;

		// m_bindPort = port;

		return this;
	}

	/**
	 * 
	 * @param timeoutSec
	 * @return
	 * @throws Exception
	 */
	public TmsBroker waitBind(double timeoutSec) throws Exception {
		m_selector.waitBind(timeoutSec);

		if (isAlive() == false) {
			cleanUp();
			throw new UserException("selector start fail!");
		}
		return this;

	}

	public int getPort() {
		return m_selector.getPort();
	}

	@Override
	public TmsItem received(SocketChannel ch, byte[] rxd) {
		// TODO Auto-generated method stub

		TmsChannel tc = get(ch);
		if (tc != null) {
			tc.doReceived(ch, rxd, rxd.length);
		}
		return null;
	}

	@Override
	public boolean connected(SocketChannel ch, Object userobj) {

		m_connectedCount++;

		try {

			TmsChannel tc = (TmsChannel) userobj;

			// tc = new TmsConnector(this, m_selector);
			tc.doConnected(ch);

			putChannel(tc, ch);

			addEvent(tc, 'c', null, null);

			// System.out.println("connect:" + ch.getLocalAddress());

			return true;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

	}

	@Override
	public void disconnected(SocketChannel ch) {
		// TODO Auto-generated method stub

		m_disconnectedCount++;

		// try {
		// System.out.println("disconnect:" + ch.getLocalAddress());
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		TmsChannel tc = get(ch);
		if (tc != null) {
			tc.doDisconnected(ch);
			// tc.close();

			if (tc.isAccepter() || (tc.getKeepConnection() == false)) {
				this.removeChannel(tc);

			}

			if (tc.isAccepter())
				addEvent(tc, 'x', null, null);
			else
				addEvent(tc, 'd', null, null);

		}

	}

	@Override
	public Object accepteded(SocketChannel ch) {

		TmsChannel tc;
		try {

			tc = new TmsChannel(this, m_selector, null);
			tc.doAccept(ch);

			putChannel(tc, ch);

			addEvent(tc, 'a', null, null);

			// System.out.println("accept:" + ch.getLocalAddress() + " id=" + tc.getId());

			return tc;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 
	 * @return
	 */
	public String getName() {
		return m_name;
	}

	/**
	 * 
	 * @param c
	 * @param ch
	 */
	protected void putChannel(TmsChannel c, SocketChannel ch) throws Exception {

		if (c == null) {
			m_selector.channelClose(ch);
			throw new UserException("putChannel(%s,%s), handle is null", c, ch);
		}

		synchronized (m_clients) {

			// debug("s_clients.containsKey(c) = " + s_clients.containsKey(c));
			// if (m_clients.containsKey(c) == false) {

			m_clients.put(c, ch);
			m_clientArr = m_clients.keySet().toArray(new TmsChannel[0]);

			// if (ch != null) {
			// m_channels.put(ch, c);
			// }

			// m_clientsName.put( name, c);
			// }
		} // sync

		synchronized (m_clientIds) {

			// debug("s_clients.containsKey(c) = " + s_clients.containsKey(c));
			// if (m_clientIds.containsKey(c) == false) {

			m_clientIds.put(c.getId(), c);
			// }
		} // sync

	}

	public TmsChannel get(SocketChannel ch) {
		synchronized (m_clients) {

			// // return m_channels.get(ch);
			//
			// for (Map.Entry<TmsChannel, SocketChannel> e : m_clients.entrySet()) {
			// // System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
			//
			// if (e.getValue() == ch)
			// return e.getKey();
			// } // for
			//
			// return null;

			Object o = m_selector.getUserObject(ch);
			if (o instanceof TmsChannel)
				return (TmsChannel) o;

			debug(ULogger.VERBOSE, "get(ch)==null");
			// System.out.println( o );

			return null;

		} // sync
	}

	public TmsChannel get(long id) {
		synchronized (m_clients) {

			return m_clientIds.get(id);

		} // sync
	}

	public TmsChannel[] get(String path) {
		synchronized (m_clients) {

			ArrayList<TmsChannel> list = m_connectGroups.get(path);
			if (list == null)
				return new TmsChannel[0];

			return list.toArray(new TmsChannel[list.size()]);

		} // sync
	}

	/**
	 * use it in MqClient
	 * 
	 * @param mc
	 */
	protected boolean putPathOfClient(TmsChannel mc, boolean isAccept) {

		synchronized (m_clients) {

			// m_clients.put(mc, mc.getPath());// set path
			m_clientArr = m_clients.keySet().toArray(new TmsChannel[0]);

		}

		String path = mc.getPath();

		if (isAccept) {
			synchronized (m_acceptGroups) {

				if (path != null && path.length() > 0) {

					if (m_acceptGroups.containsKey(path) == false) {
						m_acceptGroups.put(path, new ArrayList<TmsChannel>());
					}

					if (m_acceptGroups.get(path).contains(mc) == false) {
						m_acceptGroups.get(path).add(mc);
					}
				}
			} // sync

		} else {
			synchronized (m_connectGroups) {

				if (path != null && path.length() > 0) {
					if (m_connectGroups.containsKey(path) == false) {
						m_connectGroups.put(path, new ArrayList<TmsChannel>());
					}

					if (m_connectGroups.get(path).contains(mc) == false) {
						m_connectGroups.get(path).add(mc);
					}
				}
			} // sync

		}

		debug(ULogger.VERBOSE, "(%s) put path(%s)", m_name, mc.getPath());

		return true;
	}

	/**
	 * 
	 * @param c
	 */
	protected void removeChannel(TmsChannel c) {
		if (c == null)
			return;

		synchronized (m_clients) {

			// String n = c.getPath();

			// m_channels.remove(c.getChannel());
			m_clients.remove(c);

			m_clientArr = m_clients.keySet().toArray(new TmsChannel[0]);

			// m_clientsName.remove(n);

		} /// sync

		synchronized (m_clientIds) {

			m_clientIds.remove(c.getId());

		} /// sync

		//
		String path = c.getPath();
		//
		synchronized (m_connectGroups) {

			if (m_connectGroups.containsKey(path)) {
				m_connectGroups.get(path).remove(c);
				if (m_connectGroups.get(path).size() == 0) {
					m_connectGroups.remove(path);
				}
			}

		} // sync

		synchronized (m_acceptGroups) {

			if (m_acceptGroups.containsKey(path)) {
				m_acceptGroups.get(path).remove(c);
				if (m_acceptGroups.get(path).size() == 0) {
					m_acceptGroups.remove(path);
				}
			}

		} // sync

		debug(ULogger.VERBOSE, "(%s)remove path(%s)", m_name, c.getPath());

	}

	/**
	 * 
	 * @param id
	 */
	public void remove(long id) {

		TmsChannel c = get(id);
		removeChannel(c);

		if (c != null) {
			c.close();
			c.setKeepConnection(false);
		}

	}

	/**
	 * 
	 */
	// protected void clear() {
	public void cleanUp() {
		// synchronized (m_clients) {

		debug(ULogger.DEBUG, "cleanUp(%s)", m_selector);

		try {
			// s_send.cancel();

			s_keepAlive.cancel();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {

			for (TmsChannel c : m_clients.keySet()) {

				try {
					c.setKeepConnection(false);
					c.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

			} // for
			m_clients.clear();

			m_clientIds.clear();

			m_clientArr = new TmsChannel[0];

			// m_channels.clear();

			m_connectGroups.clear();

			m_acceptGroups.clear();

		} finally {

			synchronized (m_arrFactory) {
				m_arrFactory.remove(this);
			}
		}

		try {
			m_selector.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// } /// sync
	}

	/**
	 * 
	 * @return
	 */
	public int count() {
		synchronized (m_clients) {
			return m_clients.size();
		}
	}

	public void sumSendInfo(long c, long b) {
		m_sum_send_count += c;
		m_sum_send_bytes += b;
	}

	/**
	 * 
	 * @author richard
	 *
	 */
	protected class keepAliveTask extends TimerTask {

		TimeUtil t = new TimeUtil();
		int clientCheckIndex = 0;
		TimeUtil stm = new TimeUtil();
		TimeUtil tmr1sec = new TimeUtil();

		public void run() {

			// checkSelectorBindStat();

			//
			// get a client
			if (m_clientArr.length == 0)
				return;

			TmsChannel c = null;
			synchronized (m_clients) {

				if (clientCheckIndex >= m_clientArr.length) {

					// System.out.println("keep alive loop ~~~~~~~~~~~~~~~~~~"+TimeUtil.now()+"~~~~~~~~~~~~~~
					// "+clientCheckIndex);
					clientCheckIndex = 0;
				}

				c = m_clientArr[clientCheckIndex++];
			} // sync

			//
			// check alive
			if (c.isAlive() && c.isConnector()) {
				c.doConnectorLink();
			}

			c.gc();// garbage callect

			if (c.isAccepter()) {
				// c.getJt().setMaxTask((count() + 1));

				c.doAccepterLink();

			}

			//
			// check reconnectable
			if (c.isAlive() == false && c.isAccepter() == false && c.isReconnectable()) {

				if (c.getKeepConnection()) {
					try {

						debug(ULogger.VERBOSE, "(%s)reconnect~ [%s] ", getName(), c.getPath());

						c.reconnect();

					} catch (Exception e) {
						// e.printStackTrace();
						debug(e);
					}

				} else {// 20200508

					debug(ULogger.VERBOSE, "remove (%s)  - getKeepConnection(%s) ", c.getPath(), c.getKeepConnection());
					_this.removeChannel(c);
				}

				// return;
			}
			//
			//

			if (tmr1sec.end_ms() > 1000) {
				tmr1sec.start();
				m_sendPacketCountPerSec = m_sum_send_count;// +m_directSentCount;
				m_sendBytesCountPerSec = m_sum_send_bytes;
				m_sum_send_count = 0;
				m_sum_send_bytes = 0;
				// m_directSentCount=0;
			}

		}

		// private void checkSelectorBindStat() {
		//
		// // now when the selector is closed it is bound again automatically
		// if (m_selector.isAlive()) {
		//
		// m_aliveAtLeastOnce = true;
		//
		// }
		//
		// if (m_aliveAtLeastOnce && stm.end_sec() > 3) {
		// stm.start();
		//
		// try {
		// if (m_selector.isAlive() == false) {
		// m_selector.close();
		// m_selector.bind(m_bindIp, m_bindPort);// .get();
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		//
		// }
		//
		// }
	}

	/**
	 * 
	 * @return
	 */
	@Deprecated
	public int getTaskQueueCount() {

		// int cnt = 0;
		// for (TmsChannel c : m_clientArr) {
		// cnt += c.getJt().size();
		// }
		// return cnt;
		return 0;
	}

	/**
	 * 
	 * 
	 */
	@Override
	public String toString() {

		long reconn = 0;
		long succeed = 0;
		long fail = 0;

		long sb = 0;// send bytes
		long rb = 0;

		long sp = 0;// send packet count
		long rp = 0;

		// long taskfullerr = 0;

		long aAlives = 0;

		long cAlives = 0;

		long rsend = 0;

		// long tasks = 0;

		long resendms = 0; // one sec

		long resendmsCnt = 1;

		// long irs = 0;

		long acc = 0;

		long conn = 0;

		int cqc = 0;// channels queue count

		for (TmsChannel c : m_clientArr) {

			reconn += c.getReconnectCount();

			// taskfullerr += c.getSendTaskBufferFullErrorCount();

			// succeed += c.getJt().getSucceedCount();

			// fail += c.getJt().getFailCount();

			sb += c.m_sentBytes;

			rb += c.m_receivedBytes;

			sp += c.m_sendPacketCount;

			rp += c.m_receivedPacketCount;

			rsend += c.m_resendPacketCount;

			// tasks += c.getJt().size();

			if (c.isAccepter()) {
				acc++;
				if (c.isAlive())
					aAlives++;
			} else {
				conn++;

				if (c.isAlive())
					cAlives++;
			}

			resendms += c.getResendMs();
			if (c.getResendMs() != 0)
				resendmsCnt++;

			// irs += c.m_ignore_recv_s;

			TmsEventQueue q = c.getBindEventQeue();
			if (q != null)
				cqc += q.size();

		}

		if (resendms > 0 && resendmsCnt > 0) {
			resendms /= resendmsCnt;
		} // sync

		// int tqc = getTaskQueueCount();// task queue count

		String s = String.format(
				"tmsbroker2(%s) sel.port(%s) brk.ch(%s, %s) sel.ch(%s , %s%% , rb=%s )  brk.acc(%s/%s) brk.conn(%s/%s) brk.alive(%s,%s)  txb(%s) rxb(%s)  send ok(%s) "
						+ " send fail(%s) conn(%s) disconn(%s) reconn(%s)    resend(%s)     spc(%s) bps(%s)  "
						+ " timeout(%s) reconn(%s)  sto(%s)  sel.pd(%s)  sel.cr(%s) brk.evt(%s)  bind.evt(%s) tmem(%s) fmem(%s) xmem(%s)",

				getName(), m_selector.getPort(), m_clients.size(), m_clientIds.size(), m_selector.getChannelCount(),
				m_selector.userage(), m_selector.getRebindCount(),

				aAlives, acc, cAlives, conn, isAlive(), TimeUtil.milli2ElapsedDate(m_tmAlive.end_ms()),

				StringUtil.formatBytesSize(sb), StringUtil.formatBytesSize(rb), StringUtil.formatCount(succeed),
				StringUtil.formatCount(fail),

				StringUtil.formatCount(m_connectedCount), StringUtil.formatCount(m_disconnectedCount),
				StringUtil.formatCount(reconn),

				StringUtil.formatCount(rsend), StringUtil.formatCount(m_sendPacketCountPerSec),
				StringUtil.formatBytesSize(m_sendBytesCountPerSec),

				getReceiveTimeoutSec(), //
				getReconnectableIntervalSec(), //

				m_jt_timeoutMs, // -1, resendms, m_jt_timeoutMs, jobPeekCount(),
				m_selector.pendingCount(), m_selector.changeRequestCount(), m_eQueue.size(), cqc,
				StringUtil.formatBytesSize(Runtime.getRuntime().totalMemory()),
				StringUtil.formatBytesSize(Runtime.getRuntime().freeMemory()),
				StringUtil.formatBytesSize(Runtime.getRuntime().maxMemory())

		);

		return s;
	}

	/**
	 * 
	 * @param id
	 * @param m
	 * @return
	 * @throws Exception
	 */
	protected int sendToAccepter(long id, TmsItem m) throws Exception {

		TmsChannel mc = null;
		synchronized (m_clientIds) {

			mc = m_clientIds.get(id);

		} // sync

		if (mc != null) {
			mc.sendDistribute(m.to, m);
			return 1;
		}

		return 0;

	}

	/**
	 * 
	 * @param mc
	 * @param m
	 * @return
	 * @throws Exception
	 */
	protected int sendToAccepters(TmsChannel mc, TmsItem m) throws Exception {

		int sentCnt = 0;
		// int errCnt = 0;// send error

		if (m.to == null)
			return 0;
		if (m.to.trim().length() == 0)
			return 0;
		if (m.code != (byte) 's' && m.code != (byte) 'r') // allow 'send' action
			return 0;

		if (mc.isLoggedIn() == false)
			return 0;

		// //
		// // sendto
		ArrayList<TmsChannel> group = null;

		synchronized (m_acceptGroups) {
			group = m_acceptGroups.get(m.to);
		}

		if (group != null) {

			// if (group.size() == 1) {
			//
			// m.option |= (byte) TmsItem.SINGLE_TARGET;
			//
			// }

			// debug("send to group=%s", m.to );
			try {
				for (TmsChannel c : group) {

					// System.out.println("isaccept="+ c.isAccepter()+ " " + c.getPath());

					if (mc.equals(c) == false && c.isAccepter()) {// not itself
						if (c.sendDistribute(m.to, m)) {

							sentCnt++;
							// m_directSentCount++;
						}

					}

				}
			} catch (Exception e) {
				debug(e);
				// errCnt = -1;
			}
			return sentCnt;
		}

		// the commands which want a response will not broadcast.
		if ((m.option & TmsItem.ASK_RETURN) == TmsItem.ASK_RETURN) {
			return 0;
		}

		long ss = System.nanoTime();
		// synchronized (m_clients) {
		for (TmsChannel c : m_clientArr) {

			/// debug(" sendto id(%s) code(%c) from(%s)to(%s)", c.getId(), m.code, m.getFrom(), m.getTo() );

			if (mc.equals(c) == false && c.isAccepter()) {// not itself and to accepters

				if (m.to.equals("*") || FilenameUtils.wildcardMatch(c.getPath(), m.getTo())) {

					try {

						if (c.isLoggedIn()) {
							if (c.sendDistribute(m.to, m)) {
								sentCnt++;
								// m_directSentCount++;
							}
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			} // if

		} // for

		long ee = System.nanoTime();
		double t = ((double) (ee - ss) / 1000000);
		if (t > 100)
			debug(ULogger.VERBOSE, "sendTo: spend time (%.3f)ms cnt=%d", t, sentCnt);

		// }

		// debug("sendto------------1");

		return sentCnt;
	}

	/**
	 * 
	 * @return
	 */
	public long getSocketBufferSize() {
		return m_lSocketBufferSize;
	}

	/**
	 * 
	 * @param s
	 * @return
	 */
	public TmsBroker setSocketBufferSize(long s) {
		this.m_lSocketBufferSize = s;

		return this;
	}

	/**
	 * 
	 * @param d
	 * @return
	 */
	public TmsBroker setReceiveTimeoutSec(double d) {
		// m_dReceiveTimeoutSec = d;

		m_selector.setChannelTimeoutSec(d);
		return this;
	}

	/**
	 * 
	 * @return
	 */
	public double getReceiveTimeoutSec() {
		// return m_dReceiveTimeoutSec;

		return m_selector.getChannelTimeoutSec();
	}

	public String debug(int level, String fmt, Object... args) {

		try {

			String s = String.format(fmt, args);

			if (m_logger != null)
				m_logger.l(level, " tmsbroker(p:" + m_selector + ")", "%s", s);
			else
				System.out.println(TimeUtil.now() + " tmsbroker(p:" + m_selector + ") " + s);

			return s;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String debug(Exception ex) {

		try {

			String s = UserException.getStackTrace(ex);

			if (m_logger != null)
				m_logger.e(ex);
			else
				System.out.println(TimeUtil.now() + " tmsselector(p:" + m_selector + ") " + s);

			return s;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Deprecated
	public TmsBroker jobOpt(int maxQueue, long timeout) {

		// m_jt_maxQueue = maxQueue;
		// // m_jt_resendMs = resend;
		m_jt_timeoutMs = timeout;
		//
		// for (TmsChannel c : m_clientArr) {
		//
		// c.jobOpt(m_jt_maxQueue);
		//
		// }

		return this;
	}

	/**
	 * 
	 * @param path
	 * @return
	 */
	public int close(String path) {
		int cnt = 0;

		if (path == null || path.trim().length() == 0)
			return 0;

		debug(ULogger.DEBUG, "(%s) close(%s)", m_name, path);

		synchronized (m_connectGroups) {

			ArrayList<TmsChannel> arr = m_connectGroups.get(path);
			for (TmsChannel c : arr) {
				c.close();
				cnt++;
			} // for

		} // sync

		return cnt;

	}

	public int close(long id) {

		synchronized (m_clients) {

			TmsChannel tc = m_clientIds.get(id);
			if (tc != null) {
				tc.close();
				return 1;
			}

		} // sync

		return 0;

	}

	public int close(SocketChannel ch) {

		TmsChannel tc = get(ch);
		if (tc != null) {
			tc.close();
			return 1;
		}
		return 0;

	}

	public void closeAll() {

		synchronized (m_clients) {

			for (TmsChannel c : m_clients.keySet()) {

				try {
					c.setKeepConnection(false);
					c.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

			} // for

		} // syn

	}
	//
	// /**
	// *
	// * @param idFilter
	// * @return
	// */
	// public TmsConnector[] getClients(String idFilter) {
	//
	// synchronized (m_clients) {
	//
	// ArrayList<TmsConnector> arr = new ArrayList<TmsConnector>();
	// for (TmsConnector c : m_clients.keySet()) {
	//
	// if (idFilter.equals("*") || FilenameUtils.wildcardMatch(c.getPath(), idFilter)) {
	// arr.add(c);
	//
	// }
	//
	// } // for
	// return arr.toArray(new TmsConnector[0]);
	// } // sync
	//
	// }

	/**
	 * 
	 * @param sec
	 * @return
	 */
	public TmsBroker setReconnectableIntervalSec(double sec) {
		m_reconnectableInterval = sec;
		return this;
	}

	/**
	 * 
	 * @return
	 */
	public double getReconnectableIntervalSec() {
		return m_reconnectableInterval;
	}

	/**
	 * 
	 * scope 1~128
	 * 
	 * 1 is sequential transfer otherwise none-sequential
	 * 
	 * @return
	 */
	public int jobPeekCount() {
		//
		// if (m_jobPeekCount <= 0)
		// m_jobPeekCount = 1;
		//
		// if (m_jobPeekCount > 128)
		// m_jobPeekCount = 128;
		//
		// return m_jobPeekCount;
		return 1;
	}

	@Deprecated
	public void jobPeekCount(int n) {
		// m_jobPeekCount = n;
	}

	/**
	 * 
	 * @param l
	 */
	public void setLogger(ILogger l) {
		m_logger = l;

		m_selector.setLogger(l);
	}

	/**
	 * 
	 * 
	 * 
	 * @param ip
	 * @param port
	 * @param keepConnection
	 * @param path
	 * @return
	 * @throws Exception
	 */

	@Deprecated
	private TmsChannel connect(String ip, int port, boolean keepConnection, String path, Object userobj, String id,
			String pwd, final TmsEventQueue eq, TmsChannelInterface tci) throws Exception {

		// synchronized (_conlock) {

		TmsChannel tc = new TmsChannel(this, m_selector, path);
		tc.setKeepConnection(keepConnection, ip, port);
		tc.setUserObject(userobj);
		tc.setIdPwd(id, pwd);
		tc.setBindEventQueue(eq);
		tc.setChannelInterface(tci);

		tc.connect(ip, port);

		if (keepConnection) {
			_this.putChannel(tc, null);
		}

		return tc;
		// }

	}

	/**
	 * 
	 * @param ip
	 * @param port
	 * @param keepConnection
	 * @param path
	 * @param userobj
	 * @param id
	 * @param pwd
	 * @param eq
	 * @param tci
	 * @param timeoutMs
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	private Future<TmsChannel> connectw(final String ip, final int port, final boolean keepConnection,
			final String path, final Object userobj, String id, String pwd, final TmsEventQueue eq,
			final TmsChannelInterface tci, final long timeoutMs) throws Exception {

		Callable<TmsChannel> task = new Callable<TmsChannel>() {

			@Override
			public TmsChannel call() throws Exception {

				TmsChannel tc = connect(ip, port, keepConnection, path, userobj, id, pwd, eq, tci);
				long nid = tc.getId();

				TimeUtil t = new TimeUtil();
				while (t.end_ms() < timeoutMs) {

					if (get(nid) != null && m_selector.isAlive(tc.getChannel())) {

						return tc;
					}

					TimeUtil.sleep(1);
				}

				try {
					tc.getChannel().close();
				} catch (Exception e) {
					;
				}
				remove(nid);
				// remove
				// tc.setKeepConnection(false, "", 0);
				// tc.close();
				throw new UserException("connect fail.(%s)", path);
			}
		};

		Future<TmsChannel> res = m_executor.submit(task);
		return res;
	}

	/**
	 * 
	 * @param ip
	 * @param port
	 * @param path
	 * @param id
	 * @param pwd
	 * @param tci
	 * @param userobj
	 * @param timeoutMs
	 * @return
	 * @throws Exception
	 */
	public TmsChannel login(final String ip, final int port, final String path, String id, String pwd,
			final TmsChannelInterface tci, final Object userobj, final long timeoutMs) throws Exception {

		// Callable<TmsChannel> task = new Callable<TmsChannel>() {
		//
		// @Override
		// public TmsChannel call() throws Exception {

		TimeUtil tmrLink = new TimeUtil();

		// TmsChannel tc = connect(ip, port, true, path, userobj, id, pwd, null, tci);

		TmsChannel tc = new TmsChannel(this, m_selector, path);

		try {
			tc.setKeepConnection(true, ip, port);
			tc.setUserObject(userobj);
			tc.setIdPwd(id, pwd);
			// tc.setBindEventQueue(eq);
			tc.setChannelInterface(tci);
			tc.connect(ip, port);

		} catch (Exception e) {

			tc.close(false);

			throw e;
		}

		long nid = tc.getId();

		TimeUtil t = new TimeUtil();
		while (t.end_ms() < timeoutMs) {

			if (get(nid) != null && m_selector.isAlive(tc.getChannel())) {

				if (tc.isConnecterLoggedIn()) {
					return tc;
				}

				if (tmrLink.end_sec() > 1) {
					tmrLink.start();
					tc.sendLink();// send id/pwd
				}
			}

			TimeUtil.sleep(10);
		}

		try {
			tc.getChannel().close();

		} catch (Exception e) {
			;
		}
		this.remove(nid);

		// remove
		// tc.setKeepConnection(false, "", 0);
		// tc.close();
		throw new UserException("connect fail.(%s)", path);
		// }
		// };
		//
		// Future<TmsChannel> res = m_executor.submit(task);
		// return res;
		// return null;
	}

	public TmsChannel login(final String ip, final int port, final String path, String id, String pwd,
			final TmsChannelInterface tci) throws Exception {
		return login(ip, port, path, id, pwd, tci, null, 10000);
	}

	public TmsChannel login(String path, final TmsChannelInterface tci) throws Exception {
		return login("127.0.0.1", getPort(), path, "no id", "no pwd", tci, null, 10000);
	}

	/**
	 * 
	 * @param ip
	 * @param port
	 * @param keepConnection
	 * @param path
	 * @param userobj
	 * @param id
	 * @param pwd
	 * @param timeoutMs
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	public Future<TmsChannel> connectw(final String ip, final int port, final boolean keepConnection, final String path,
			final Object userobj, String id, String pwd, final long timeoutMs) throws Exception {

		return connectw(ip, port, keepConnection, path, userobj, id, pwd, null, null, timeoutMs);
	}

	// @Deprecated
	// public Future<TmsChannel> connectw(final String ip, final int port, final boolean keepConnection, final String
	// path,
	// final Object userobj, String id, String pwd, TmsEventQueue eq, final long timeoutMs) throws Exception {
	//
	// return connectw(ip, port, keepConnection, path, userobj, id, pwd, eq, null, timeoutMs);
	// }
	@Deprecated
	public Future<TmsChannel> connectw(final String ip, final int port, final boolean keepConnection, final String path,
			final Object userobj, String id, String pwd, TmsChannelInterface tci, final long timeoutMs)
			throws Exception {

		return connectw(ip, port, keepConnection, path, userobj, id, pwd, null, tci, timeoutMs);
	}

	/**
	 * 
	 * @param from
	 * @param to
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public long send(String from, String to, byte[] data) throws Exception {

		TmsChannel tc[] = get(from);
		if (tc == null)
			throw new UserException("Not found path");

		// if (tc.isConnector()) {
		return tc[0].send(to, data);

	}

	/**
	 * 
	 * @param who
	 * @param amongAccepters
	 * @return
	 */
	public SocketChannel[] getChannels(String who, boolean amongAccepters) {

		ArrayList<SocketChannel> arr = new ArrayList<SocketChannel>();

		synchronized (m_clients) {
			for (TmsChannel c : m_clientArr) {

				if (who == null || FilenameUtils.wildcardMatch(c.getPath(), who)) {

					if (c.isAccepter() == amongAccepters) {
						arr.add(c.getChannel());
					}

				}
			} // for
		} // sync

		return arr.toArray(new SocketChannel[arr.size()]);

	}

	/**
	 * 
	 * @param who
	 * @param amongAccepters
	 * @return
	 */
	public String[] getChannelNames(String who, boolean amongAccepters) {

		ArrayList<String> arr = new ArrayList<String>();

		synchronized (m_clients) {
			for (TmsChannel c : m_clientArr) {

				if (who == null || FilenameUtils.wildcardMatch(c.getPath(), who)) {

					if (c.isAccepter() == amongAccepters) {

						if (c.getPath() != null) {//
							arr.add(c.getPath());
						}
					}

				}

			} // for
		} // sync

		return arr.toArray(new String[arr.size()]);

	}

	/**
	 * 
	 * @param who
	 * @param amongAccepters
	 * @return
	 */
	public int getSizeOfGroup(String who, boolean amongAccepters) {

		if (who == null)
			return 0;

		if (who.trim().length() == 0)
			return 0;

		if (who.trim().equals("*")) {

			if (amongAccepters)
				return m_acceptGroups.size();
			else
				return m_connectGroups.size();
		}

		if (amongAccepters) {

			synchronized (m_acceptGroups) {
				ArrayList<TmsChannel> group = m_acceptGroups.get(who);

				if (group != null) {
					return group.size();
				}

			}

		} else {

			synchronized (m_connectGroups) {
				ArrayList<TmsChannel> group = m_connectGroups.get(who);

				if (group != null) {
					return group.size();
				}

			}
		}

		return 0;
	}

	/**
	 * 
	 * @param amongAccepters
	 * @return
	 */
	public String[] getGroupNames(boolean amongAccepters) {

		if (amongAccepters) {

			synchronized (m_acceptGroups) {
				return m_acceptGroups.keySet().toArray(new String[0]);
			}

		} else {

			synchronized (m_connectGroups) {
				return m_connectGroups.keySet().toArray(new String[0]);
			}
		}

	}

}
