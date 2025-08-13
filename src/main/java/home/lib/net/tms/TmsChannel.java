package home.lib.net.tms;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import home.lib.io.FilenameUtils;
import home.lib.lang.LimitedArray;
import home.lib.lang.UserException;
import home.lib.log.ULogger;
import home.lib.util.AverageUtil2;
import home.lib.util.DataStream;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

/**
 * 
 * 
 * 20201120
 * 
 * // if the broker is busy then it gives a little delay (BROKER_USAGE_LIMIT)
 * 
 * 
 * 20201125
 * 
 * the changedrequest setting is not duplicated when entering multiple send()
 * 
 * 
 * 
 * 
 * 
 * @author richard
 *
 */
final public class TmsChannel implements TmsJobTaskInterface {

	final public static String last_modify = "20211114";

	public int debugLocate = 0;

	public enum Qos {
		// AtMostOnce,
		AtLeastOnce
	}

	/**
	 * 
	 * 
	 * @author richard
	 *
	 */
	private class UserJob {

		public UserJob(TmsItem a) {
			m = a;
		}

		TmsItem m = null;
		int count = 0;// send count
		long send_time = 0;

		public void set_send_time() {
			if (send_time == 0) {
				send_time = System.nanoTime();// remeber when it send
			}
		}

	}

	private long m_id = System.currentTimeMillis();

	// static Long s_lastId = Long.MAX_VALUE;

	private TmsChannel _this = this;

	private TmsBroker m_broker = null;

	private String m_path = null;

	private TmsJobTask<UserJob> m_sending_jt = new TmsJobTask<UserJob>(this);

	private TmsDlePacket m_parser = null;

	private long m_sendIndex = 0;

	private String m_ip = null;

	private int m_port = 0;

	private TimeUtil m_reconnectionInterval = new TimeUtil();

	private TimeUtil m_tmrCheckLink = new TimeUtil();

	private TimeUtil m_recvTime = new TimeUtil();

	// private long m_sendTaskFullErrorCount = 0;

	protected long m_receivedPacketCount = 0;

	protected long m_sendPacketCount = 0;

	protected long m_resendPacketCount = 0;

	protected long m_receivedBytes = 0;

	protected long m_sentBytes = 0;

	private long m_sentCount = 0;

	private long m_reconnectCount = 0;

	private long m_sentBytesPerSec = 0;

	// protected long m_ignore_recv_s = 0;

	private TimeUtil m_aliveTime = new TimeUtil();

	private SocketChannel m_channel;

	private TmsSelector m_selector = null;

	private boolean m_keepConnection;

	private boolean m_isAccept = false;

	private LimitedArray<TmsItem, Long> m_receivedItemList = new LimitedArray<TmsItem, Long>(1024);

	private LimitedArray<TmsItem, Long> m_returnItemList = new LimitedArray<TmsItem, Long>(64);

	private LimitedArray<Long, TmsItem> m_recentlyReturn = new LimitedArray<Long, TmsItem>(32);

	private LimitedArray<Long, TmsItem> m_recentlyAck = new LimitedArray<Long, TmsItem>(16);

	private Object m_userObj = null;

	private boolean m_loggedIn = false;

	private String m_idname = null;

	private String m_pwd = null;

	private int m_usageOfBroker = 0;

	final public static int BROKER_USAGE_LIMIT = 80; // 80 percent

	AverageUtil2 m_ackAverage = new AverageUtil2();

	private TmsEventQueue m_bindQueue = null;

	private TmsChannelInterface m_channelInterface = null;

	// private double m_connectorReceiveTimeoutSec = 60;// 1 min

	private boolean m_conLoggedIn = false;

	// private UserJob m_sending_job = null;

	/**
	 * 
	 */

	public TmsChannel(TmsBroker bs, TmsSelector sel, String path) throws Exception {

		m_broker = bs;
		m_selector = sel;

		if (path != null) {
			setPath(path);
		}

		// m_jt.setMaxTask(bs.m_jt_maxQueue);

		m_id = bs.idGenerate();

		// m_connectorReceiveTimeoutSec = m_broker.getReceiveTimeoutSec();

	}

	// public void resetSentCount() {
	// m_sentCount = 0;
	// m_sentBytesPerSec = 0;
	// }

	public void setUserObject(Object o) {
		m_userObj = o;
	}

	public Object getUserObject() {
		return m_userObj;
	}

	// /**
	// *
	// * @param maxQueue
	// * @param resend
	// * @param timeout
	// * @return
	// */
	// public TmsChannel jobOpt(int maxQueue) {
	// m_jt.setMaxTask(maxQueue);
	// return this;
	// }
	//
	// /**
	// *
	// * @return
	// */
	// public TmsJobTask getJt() {
	// return m_jt;
	// }

	/**
	 * 
	 * @param b
	 * @return
	 * @throws Exception
	 */
	public TmsChannel setIpBind(InetSocketAddress b) throws Exception {

		return this;
	}

	/**
	 * 
	 * @return
	 */
	public long getReconnectCount() {
		return m_reconnectCount;
	}

	/**
	 * 
	 * setpath
	 * 
	 * @param s
	 * @return
	 */
	private TmsChannel setPath(String s) throws Exception {

		if (m_path != null) {
			throw new UserException("id(%s) already set", m_path);
		}

		if (isAccepter()) {
			throw new UserException("accept socket has not permission");
		}

		if (s.indexOf("*") != -1 || s.indexOf('?') != -1 || s.indexOf('|') != -1 || s.indexOf(':') != -1
				|| s.indexOf('<') != -1 || s.indexOf('>') != -1) {
			throw new UserException("id can not content the symbols( * ? : | < >  )  path(" + s);
		}

		m_path = s;

		return this;
	}

	/**
	 * 
	 * @param s
	 * @return
	 */
	public static boolean checkPathName(String s) {

		if (s.trim().length() == 0)
			return false;

		if (s.indexOf("*") != -1 || s.indexOf('?') != -1 || s.indexOf('|') != -1 || s.indexOf(':') != -1
				|| s.indexOf('<') != -1 || s.indexOf('>') != -1) {
			// throw new UserException("id can not content the symbols( * ? : | < > ) path(" + s);
			return false;
		}

		return true;
	}

	// /**
	// *
	// * @return
	// */
	// public int sendQueueCount() {
	// return m_jt.size();
	// }

	/**
	 * 
	 * 
	 * @return
	 */
	public String getPath() {
		return m_path;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isConnecting() {

		return m_selector.isConnecting(m_channel);

		// return m_channel.isConnecting();
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public TmsChannel reconnect() throws Exception {

		if (isAlive())
			return this;

		if (isAccepter()) {
			// return this;
			throw new UserException("accept socket doesn't alloc to use reconnect func ");
		}

		if (m_ip == null || m_port == 0)
			return this;

		m_reconnectionInterval.start();

		if (isConnecting())
			return this;

		m_reconnectCount++;

		m_channel = m_selector.connect(m_ip, m_port, this);

		return this;
	}

	/**
	 * 
	 * 
	 * @return
	 */
	public boolean isReconnectable() {

		if (isConnecting()) {
			m_reconnectionInterval.start();
			// debug("still connecting");
			return false;
		}

		if (m_reconnectionInterval.end_sec() < m_broker.getReconnectableIntervalSec())
			return false;

		if (isAlive())
			return false;

		// c.isAlive() == false && c.isAccepter() == false && c.isReconnectable() && c.getKeepConnection()

		// debug("%s isReconnectable %s < %s (%s) isalive(%s) isaccepter(%s) keep con(%s) ", TimeUtil.now(),
		// m_reconnectionInterval.end_sec(), m_broker.getReconnectableIntervalSec(), getPath(), isAlive(),
		// isAccepter(), getKeepConnection());

		m_reconnectionInterval.start();
		return true;

	}

	/**
	 * 
	 * @param ch
	 * @return
	 * @throws Exception
	 */
	public TmsChannel doAccept(SocketChannel ch) throws Exception {

		if (isAlive()) {
			throw new UserException("channel is alive");
		}

		m_isAccept = true;

		m_channel = ch;

		getId();

		m_reconnectionInterval.start();
		m_recvTime.start();// timer initialize

		// debug("buffsize=" + m_broker.getSocketBufferSize());

		m_parser = new TmsDlePacket(m_broker.getSocketBufferSize());

		// m_jt.clear();

		try {
			TmsItem a = new TmsItem(new byte[0]);
			sendAck((byte) 'l', a);// send
		} catch (Exception e) {

		}

		return this;
	}


	/**
	 * 
	 */
	@Deprecated 
	public void close() {

		m_selector.channelClose(m_channel);

	}

	/**
	 * 
	 * @param keepConnection
	 */
	public void close(boolean keepConnection) {

		if (keepConnection) {
			m_selector.channelClose(m_channel);
		} else {
			m_broker.remove(getId());
		}

	}

	/**
	 * 
	 * @return
	 */
	public boolean isAlive() {

		return m_selector.isAlive(m_channel);
	}

	/**
	 * 
	 * @return
	 */
	public boolean isAccepter() {
		return m_isAccept;// m_channel.isAccept();

	}

	/**
	 * 
	 * @return
	 */
	public boolean isConnector() {

		return (!isAccepter());
	}

	/**
	 * 
	 */
	// @Override
	public void doReceived(SocketChannel ch, byte[] buf, int len) {

		debugLocate = 1;
		try {

			TimeUtil t = new TimeUtil();

			// debug(" recv (%d) ", len);
			if (m_parser == null || buf == null) {
				debug("parser(%s) or buf(%s) error", m_parser, buf);
				return;
			}

			m_receivedBytes += len;
			m_receivedPacketCount++;

			m_parser.append(buf, len);

			TmsItem m = null;
			while ((m = m_parser.poll()) != null) {

				t.start();
				if (isAccepter()) {

					debugLocate = 2;
					doRecvInAccepter(m);

				} else {
					debugLocate = 3;
					doRecvInConnector(m);
				}

				if (t.end_ms() > 1000) {
					debug("%s doRecv receive action use %s ms ", getPath(), t.end_ms());
				}
				debugLocate = 4;

			} // while

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	// @Override
	public void doConnected(SocketChannel ch) {
		// debug("connected path(%s) sb(%s)", getPath(), StringUtil.formatBytesSize(m_channel.socketBufferSize()));

		try {

			// m_channel = ch;

			m_reconnectionInterval.start();
			m_recvTime.start();// timer initialize
			m_aliveTime.start();

			// debug("buffsize=" + m_broker.getSocketBufferSize());

			m_parser = new TmsDlePacket(m_broker.getSocketBufferSize());

			// m_parser = new TmsDlePacket(1024*8);

			// m_jt.clear();
			// m_broker.put(this, ch);

			if (isConnector()) {
				m_broker.putPathOfClient(_this, false);
			} else {
				// isaccepted
				// putpath in recvinaccept
			}

		} catch (Exception e) {

			e.printStackTrace();

			m_selector.channelClose(ch);

			return;
		}

		try {
			TmsItem a = new TmsItem(new byte[0]);

			a.id = m_idname;
			a.pwd = m_pwd;
			sendAck((byte) 'l', a);//
			// sendAck((byte) 'l', a);//
			// sendAck((byte) 'l', a);//
		} catch (Exception e) {

		}

		// m_inter.connected(this);

	}

	/**
	 * 
	 */
	public void sendLink() {
		TmsItem a = new TmsItem(new byte[0]);

		a.id = m_idname;
		a.pwd = m_pwd;
		sendAck((byte) 'l', a);//
	}

	/**
	 * 
	 */

	public void doDisconnected(SocketChannel ask) {

		m_reconnectionInterval.start();

	}

	/**
	 * 
	 */
	@Override
	public void doSucceeded(TmsJobTask jt, Object e) {

		// m_bootstrap.m_sendSucceed++;

		if (e instanceof UserJob) {
			// m_inter.sendSucceeded(this, ((UserJob) e).m);
		}
	}

	/**
	 * 
	 */
	@Override
	public void doFailed(TmsJobTask jt, Object e) {
		// m_bootstrap.m_sendFail++;
		if (e instanceof UserJob) {
			// m_inter.sendFailed(this, ((UserJob) e).m);
		}
	}

	/**
	 * 
	 * @return
	 */

	public double getLastReceivedTimeSec() {
		return m_recvTime.end_sec();
	}

	/**
	 * 
	 * @param m
	 */
	private void doRecvInConnector(TmsItem m) throws Exception {

		if (isAccepter())
			return;

		m_recvTime.start();
		debugLocate = 30;

		// debug("recv (%s)", getPath());

		if (m.code == (byte) 's') {// receive normal data

			// server(cli)->client
			if (FilenameUtils.wildcardMatch(getPath(), m.getTo()) == false)
				return;

			try {

				debugLocate = 31;

				if (m_receivedItemList.contains(m)) {

					if (m.isAskReturn()) {

						TmsItem r = searchInRecentlyReturns(m);
						if (r != null) {
							connectorSendReturn(r.to, r);// send return
						}

					}

				} else {

					if (m_broker.addEvent(this, 'r', m.data, m)) {
						//

						if (m.isExactlyOnce()) {
							m_receivedItemList.add(new TmsItem(m.sendIdx, m.when, m.from, m.to));
						}

					}
				}

				// ----
				debugLocate = 32;

			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (m.code == (byte) 'r') {// get return value

			if (getPath().equals(m.getTo())) {
				checkResp(m);// send ok

				m_recentlyReturn.add(m.respIdx, m);
			}

			// debug("tmscon 'r' me(%s) from(%s) ", getPath(), m.getFrom());

		} else if (m.code == (byte) 'l') {// check link(ask)

			// debug("recv(link)~~~~~~~~~~~~~"+isAccepter()+" "+getPath() );

			m_usageOfBroker = m.qWeight;

			// if( m.data!=null) {
			// System.out.format( " connector: recv 'l' ds=%s \r\n", m.data.length );
			// }
			//
			//
			if (m.data != null && m.data.length > 4) {
				DataStream ds = new DataStream(m.data);
				long stx = ds.read32();
				if (stx == 0x20210908 && m.data.length == 10) {
					short timeout = ds.read16();
					long bs = ds.read32();

					m_parser.setBufferSize(bs);
					// m_connectorReceiveTimeoutSec = timeout;
					m_selector.setChannelTimeoutSec(m_channel, timeout);

					// System.out.format( " connector: recv 'l' to=%s bs=%s id=%s pwd=%s \r\n",timeout, bs, m.id, m.pwd
					// );
				}
			}

			try {
				if (m.pwd != null && m.pwd.equals("true")) {
					m_conLoggedIn = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (m.code == (byte) 'e') {// retrun error from server

			try {

				m_usageOfBroker = m.qWeight;
				// m_inter.returnError(this, new UserException(s));

				m_broker.addEvent(this, 'e', m.data, m);

			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (m.code == (byte) 'a') {// get ack

			checkResp(m);// send ok

			m_recentlyAck.add(m.respIdx, m);

		}

		debugLocate = 39;
	}

	/**
	 * 
	 * 
	 * @param m
	 */
	private void doRecvInAccepter(TmsItem m) {

		if (isAccepter() == false)
			return;

		m_recvTime.start();

		if (m.code == (byte) 's') {// receive normal data

			// m.fromIdInBroker = this.getId();// set pathId in broker

			if (m_path == null) {// try to register

				m_path = m.from;// copy name
				m_broker.putPathOfClient(_this, true);
			}

			if (m.timeout > 0) { // connecter (wait ack) -> accepter
				sendAck((byte) 'a', m); // accepter ( ack response ) -> connector
			}

			// if (m_selector.userage() > (BROKER_USAGE_LIMIT * 2)) {
			//
			// int howMany = m_broker.getSizeOfGroup(m.to, true);
			// if (howMany != 1) {
			// sendError(String.format(
			// "selector is busy.(userage:%s, port=%s, ch=%s) ignore (sednTo:%s, size=%s ) !",
			// m_selector.userage(), m_selector.getPort(), m_selector.getChannelCount(), m.to, howMany));
			// m_ignore_recv_s++;
			// return;
			// }
			// } else if (m_selector.userage() > BROKER_USAGE_LIMIT) {
			//
			// // sendError(String.format("Selector is busy.(userage:%s)", m_selector.userage()));
			//
			// }

			try {

				int r = 0;
				if (m.to != null && m.to.trim().length() != 0) {

					m.askAccepterId = this.getId();

					r = m_broker.sendToAccepters(this, m);
					// System.out.format("accepter sent=%s \r\n", r );
				}

				m_sendPacketCount += r;

				// ther's no target
				if (r == 0) {
					if ((m.option & TmsItem.ASK_RETURN) == TmsItem.ASK_RETURN) {// if it need to get a return value
						sendError(String.format("there is no target.(%s) -> (%s)", m.from, m.to));
					}
				}

				// if (r == 0 && m.to.equals("*") == false) {
				// sendError(String.format("there is no target. from(%s) to(%s) code(%s) loggedin(%s) ", m.from,
				// m.to, m.code, this.isLoggedIn()));
				// }

			} catch (Exception e) {

				sendError(e.toString());
			}

		} else if (m.code == (byte) 'r') {// get return value

			// m.fromIdInBroker = this.getId();// set pathId in broker
			if (m.askAccepterId != 0) {
				try {

					m_broker.sendToAccepter(m.askAccepterId, m);

					m_sendPacketCount++;

				} catch (Exception e) {

					sendError(e.toString());

				}
			}
			// }

		} else if (m.code == (byte) 'l') {// check link(ask)

			if (m_path == null) {
				// if (m_inter.putPath(_this, m.from)) {
				m_path = m.from;// copy name
				m_broker.putPathOfClient(_this, true);

			}

			if (m.id != null && m.pwd != null) {
				m_idname = m.id;
				m_pwd = m.pwd;
				m_broker.addEvent(this, 'l', null, null);
			}
			// else
			// debug("recv 'link' from(%s) to(%s)", m.from, m.to );

			// if (checkPath(m,true) == false)
			// return;

			// accepters data send to connector
			DataStream ds = new DataStream();
			ds.write32(0x20210908);
			ds.write16((short) m_selector.getChannelTimeoutSec(m_channel));// m_broker.getReceiveTimeoutSec());
			ds.write32((int) m_broker.getSocketBufferSize());

			// System.out.format("accepter : bs= \r\n", ds.copyOf().length );

			TmsItem ti = new TmsItem();
			ti.id = m.id;
			ti.pwd = "true";

			sendAck((byte) 'l', ti, ds.copyOf());// reply 'link' to connector.

			// debug("recv(link)~~~~~~~~~~~~~"+isAccept() );

		} else if (m.code == (byte) 'a') {// get ack

			checkResp(m);// send ok

		}

	}

	/**
	 * 
	 * 
	 * @param r
	 */
	private void checkResp(TmsItem r) {

		// synchronized (m_jt) {
		//
		// for (int h = 0; h < m_jt.size(); h++) {
		//
		// UserJob j = m_jt.get(h);
		//
		// if (j.m.sendIdx == r.respIdx && j.m.when == r.when && (r.code == (byte) 'a' || r.code == (byte) 'r')) {
		// m_jt.setResp(j, true);
		// // debug("get ack (%s)isAccept(%s)", getId(), isAccept());
		//
		// // getAutoResendMs(j);
		//
		// }
		//
		// } // for(h
		//
		// } // sync

		UserJob[] a = m_sending_jt.toArray(new UserJob[0]);
		for (int h = 0; h < a.length; h++) {

			UserJob j = a[h];

			if ((j.m.option & TmsItem.ASK_RETURN) == TmsItem.ASK_RETURN) {// if it need to get a return value
				if (j.m.sendIdx == r.respIdx && j.m.when == r.when && (r.code == (byte) 'r')) {
					m_sending_jt.setResp(j, true);

				}
			}

			else if (j.m.sendIdx == r.respIdx && j.m.when == r.when && (r.code == (byte) 'a' || r.code == (byte) 'r')) {
				m_sending_jt.setResp(j, true);

			}
		} // for(h

	}

	/**
	 * 
	 * 
	 * @param s
	 */
	public void debug(String s, Object... args) {

		m_broker.debug(ULogger.VERBOSE, " tmsconnector [" + String.format(s, args));
	}

	public void debug(Exception e) {

		m_broker.debug(e);
	}

	/**
	 * 
	 * 
	 * 
	 */
	protected void doSendTask(UserJob u) {

		if (u == null)
			return;

		// synchronized (m_jt) {

		// int rcnt=0;
		// int end = m_broker.jobPeekCount();
		// int maxloop = 0;
		// UserJob u = null;

		// do {
		// u = m_jt.autoPeek(m_broker.jobPeekCount());

		if (u != null && isAlive()) {
			// debug("doSendTask");

			u.set_send_time();

			try {
				byte[] b = TmsDlePacket.make(u.m);

				m_sentBytes += b.length;
				m_sendPacketCount++;

				// m_channel.send(b);// send!
				m_selector.send(m_channel, b);

				if (u.count > 0) {
					m_resendPacketCount++;
				}
				u.count++;

				m_sentCount++;
				m_sentBytesPerSec += b.length;

				m_broker.sumSendInfo(1, b.length);

				// return true;
			} catch (Exception e) {

				e.printStackTrace();
			}

		}

		// maxloop++;
		// } while (u != null && maxloop < end);

		// return new long[] { m_sentCount, m_sentBytesPerSec };
		// } // sync

		return;
	}

	/**
	 * 
	 * @param to
	 * @param da
	 * @param opt
	 * @param resendMs
	 * @param timeoutMs
	 * @return
	 * @throws Exception
	 */
	private long connectorSend(String to, byte[] da, byte opt, long resendMs, long timeoutMs) throws Exception {

		if (isAccepter()) {
			throw new UserException("Accept channel can't send data");
		}

		if (isAlive() == false) {
			throw new UserException("is not alive");
		}

		TmsItem m = new TmsItem();
		m.data = da;
		m.askAccepterId = 0;
		m.option = opt;
		m.resend = (int) resendMs;
		m.timeout = (int) timeoutMs;

		UserJob job = null;

		// synchronized (m_jt) {

		m.from = m_path;
		m.code = 's';
		m.to = to;
		m.sendIdx = (m_sendIndex++);

		job = new UserJob(m);

		// if (m_jt.add(job, m.resend, m.timeout) == false) {
		// m_sendTaskFullErrorCount++;

		// throw new UserException("task is full.(%d)", m_jt.getMaxTask());
		// }

		// } // sync

		if (m.timeout > 0) {

			m_sending_jt.add(job, m.resend, m.timeout);

			do {
				UserJob j = m_sending_jt.autoPeek(1);

				if (j != null) {
					doSendTask(j);// send
				}

			} while (m_sending_jt.size() > 0);

		} else {

			doSendTask(job);// send

		}

		//
		// // if the broker is busy then it gives a little delay
		// if (m_usageOfBroker > BROKER_USAGE_LIMIT) {
		// TimeUtil.sleep((m_usageOfBroker - BROKER_USAGE_LIMIT) * 1 + 1);
		// }

		return m.sendIdx;
	}

	/**
	 * 
	 * @param to
	 * @param da
	 * @return
	 * @throws Exception
	 */
	public long send(String to, byte[] da) throws Exception {
		return connectorSend(to, da, (byte) 0, 0, 0);
	}

	/**
	 * 
	 * @param to
	 * @param da
	 * @param q
	 * @return
	 * @throws Exception
	 */
	public long send(String to, byte[] da, Qos q) throws Exception {

		long aver = m_ackAverage.getAverage();
		long resend = ((aver == 0) ? 1000 : aver * 3);
		long timeout = m_broker.m_jt_timeoutMs;

		if (q == Qos.AtLeastOnce) {

			long sendId = connectorSend(to, da, (byte) 0, resend, timeout);
			waitAck(sendId, timeout);
			return sendId;

		} else {
			return connectorSend(to, da, (byte) 0, 0, 0);
		}

	}

	/**
	 * 
	 * @param sendId
	 * @param timeout
	 */
	private void waitAck(long sendId, long timeout) {

		long start = System.currentTimeMillis();

		TimeUtil t = new TimeUtil();
		while (t.end_ms() < timeout) {

			// synchronized (m_lastReturnCodes) {
			if (m_recentlyAck.contains(sendId)) {
				long end = System.currentTimeMillis();
				m_ackAverage.add(end - start);
				return;
			}
			// } // sync

			TimeUtil.sleep(1);
		}

	}

	/**
	 * 
	 * @param to
	 * @param da
	 * @param timeout
	 * @return
	 * @throws Exception
	 */
	public byte[] get(String to, byte[] da, long timeout) throws Exception {

		long aver = m_ackAverage.getAverage();
		long resend = ((aver == 0) ? 1000 : aver * 3);

		// System.out.println( "get.average = " + resend );

		long start = System.currentTimeMillis();

		long sendId = 0;

		if (timeout > 0)
			sendId = connectorSend(to, da, (byte) (TmsItem.ASK_RETURN | TmsItem.EXACTLY_ONCE), resend, timeout);
		else
			sendId = connectorSend(to, da, (byte) (TmsItem.EXACTLY_ONCE), 0, 0);

		TimeUtil t = new TimeUtil();
		while (t.end_ms() < timeout) {

			// synchronized (m_lastReturnCodes) {
			if (m_recentlyReturn.contains(sendId)) {
				TmsItem r = m_recentlyReturn.getValue(sendId);
				if (r != null) {

					long end = System.currentTimeMillis();
					m_ackAverage.add(end - start);

					// System.out.println( "get.average = " + (end-start) );

					return r.data;
				}
			}
			// } // sync

			TimeUtil.sleep(1);
		}
		return null;
	}

	/**
	 * 
	 * @param to
	 * @param da
	 * @return
	 * @throws Exception
	 */
	public byte[] get(String to, byte[] da) throws Exception {
		return get(to, da, m_broker.m_jt_timeoutMs);
	}

	/**
	 * 
	 * server(cli)->server(cli)
	 * 
	 * @param to
	 * @param m
	 */
	protected boolean sendDistribute(String to, TmsItem m) {
		// synchronized (m_jt) {
		//
		// if (m_jt.add(new UserJob(m), 0, 0) == false) {
		// m_sendTaskFullErrorCount++;
		// // debug("(%s)send distribute to(%s) queue is full (max:%s)", getPath(), to, m_jt.getMaxTask());
		//
		// sendError(String.format("(%s) accepter queue is full 1 (max:%s)", getPath(), m_jt.getMaxTask()));
		//
		// // m_inter.sendFailed(this, m);
		// return false;
		// }
		//
		// } // sync

		doSendTask(new UserJob(m));// send
		return true;
	}

	/**
	 * 
	 * 
	 * @param to
	 * @param m
	 * @throws Exception
	 */
	private void connectorSendReturn(String to, TmsItem m) throws Exception {
		// synchronized (m_jt) {
		//
		// m.sendIdx = (m_sendIndex++);
		// if (m_jt.add(new UserJob(m), 0, 0) == false) {
		// m_sendTaskFullErrorCount++;
		//
		// // debug("mqclient[(%s)send return to(%s) queue is full (max:%s)", getPath(), to, m_jt.getMaxTask());
		// sendError(String.format("(%s) accepter queue is full 2 (max:%s)", getPath(), m_jt.getMaxTask()));
		//
		// // m_inter.sendFailed(this, m);
		// return;
		// }
		//
		// } // sync

		doSendTask(new UserJob(m));// send
	}

	/**
	 * 
	 * @param rx
	 * @return
	 */
	private TmsItem searchInRecentlyReturns(TmsItem rx) {

		TmsItem arr[] = m_returnItemList.toArray(new TmsItem[0]);
		//
		for (TmsItem lr : arr) {

			if (lr.respIdx == rx.sendIdx && lr.when == rx.when && lr.to.equals(rx.from)) {
				return lr;
			}
		} // for(i

		return null;
	}

	/**
	 * 
	 * @param m
	 * @param data
	 * @throws Exception
	 */
	public boolean sendReturn(TmsItem m, byte[] data) throws Exception {

		if (m.isAskReturn()) {
			TmsItem ret = new TmsItem();
			ret.code = 'r';
			ret.sendIdx = (m_sendIndex++);
			ret.respIdx = m.sendIdx;// set response index
			ret.when = m.when;// copy when it sent
			ret.askAccepterId = m.askAccepterId;
			ret.from = this.getPath();
			ret.to = m.from;// alloc sender name
			ret.data = data;

			connectorSendReturn(ret.to, ret);// send return

			m_returnItemList.add(m);
			return true;

		}
		return false;
	}

	/**
	 * 
	 * @param code
	 * @param m
	 */
	private void sendAck(byte code, TmsItem m) {

		sendAck(code, m, null);

	}

	/**
	 * 
	 * @param code
	 * @param m
	 * @param data
	 */
	private void sendAck(byte code, TmsItem m, byte[] data) {

		// int weight = 0;
		// try {
		// if (m_broker.m_sendBytesCountPerSec != 0) {
		// weight = (int) (m_broker.m_taskQueueCount / m_broker.m_sendBytesCountPerSec);
		// }
		// } catch (Exception e) {
		//
		// }

		TmsItem a = new TmsItem(new byte[0]);
		a.from = m_path;
		a.to = m_path;
		a.code = code;
		a.sendIdx = (m_sendIndex++);
		a.respIdx = m.sendIdx;
		a.when = m.when;
		a.askAccepterId = 0;//
		a.qWeight = m_selector.userage();

		a.id = m.id;
		a.pwd = m.pwd;

		// a.fromIdInBroker = m.fromIdInBroker;
		if (data != null) {
			a.data = data;
		}

		try {

			byte[] b = TmsDlePacket.make(a);

			m_sentBytes += b.length;
			m_sendPacketCount++;

			m_selector.send(m_channel, b);
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param m
	 */
	private void sendError(String m) {
		byte[] b = m.getBytes();
		sendAck((byte) 'e', new TmsItem(), b);// return err
	}

	/**
	 * 
	 */
	protected void doConnectorLink() {

		double rtimeout = m_selector.getChannelTimeoutSec(m_channel); // m_connectorReceiveTimeoutSec;

		int sec0 = (int) ((rtimeout - 3) / 3) + 1;

		if (rtimeout > 600) {
			// System.out.println("sec0="+sec0+" "+getPath()+" rtimeout="+rtimeout);
		}

		if (m_tmrCheckLink.end_sec() > sec0) // send it two times
		{
			m_tmrCheckLink.start();

			// if( rtimeout>100)
			// System.out.println("connector timeout = "+ rtimeout );

			// if (getLastReceivedTime() > sec0) {

			if (isConnector()) {

				sendAck((byte) 'l', new TmsItem(new byte[0]));// send

				// debug("send 'link' (%s)", getPath() );
				return;
			}

		}

		// time out check
		if (getLastReceivedTimeSec() > rtimeout && rtimeout != 0) {

			debug("socket timeout!!! isAccept(%s) recvLastTime(%f/%f) getId(%s) ", _this.isAccepter(),
					_this.getLastReceivedTimeSec(), rtimeout, _this.getPath());

			m_recvTime.start();// timer initialize
			// getChannel().close();

			m_selector.channelClose(m_channel);
		}

	}

	protected void doAccepterLink() {

		double rtimeout = m_selector.getChannelTimeoutSec(m_channel);// m_broker.getReceiveTimeoutSec();

		int sec0 = (int) ((rtimeout - 3) / 3) + 1;

		if (m_tmrCheckLink.end_sec() > sec0) // send it two times
		{
			m_tmrCheckLink.start();

			// if( rtimeout>100)
			// System.out.println("accepter timeout = "+ rtimeout );
			//
			if (isLoggedIn() == false && isAccepter()) {

				m_selector.channelClose(m_channel);

				debug("if it's not logged in then the connection will be terminated. timeout(%s sec) getId(%s) -check to timeout sec of client",
						sec0, _this.getPath());

				m_recvTime.start();

				return;
			}

		}

		// time out check
		if (getLastReceivedTimeSec() > rtimeout && rtimeout != 0) {

			debug("socket timeout!!! isAccept(%s) recvLastTime(%f/%f) getId(%s) ", _this.isAccepter(),
					_this.getLastReceivedTimeSec(), rtimeout, _this.getPath());

			m_recvTime.start();// timer initialize
			// getChannel().close();

			m_selector.channelClose(m_channel);
		}

	}

	// /**
	// *
	// * @return
	// */
	// public long getSendTaskBufferFullErrorCount() {
	// return m_sendTaskFullErrorCount;
	// }

	/**
	 * 
	 * @return
	 */
	public boolean getKeepConnection() {
		return m_keepConnection;

	}

	// /**
	// *
	// * @param k
	// */
	public void setKeepConnection(boolean k, String ip, int port) {
		m_keepConnection = k;
		m_ip = ip;
		m_port = port;
	}

	public void setKeepConnection(boolean k) {
		m_keepConnection = k;
	}

	@Override
	public String toString() {

		String s = "";

		s += String.format(
				"tmschannel@%s  keepCon(%s) isConn(%s) recon(%s) q.err(%s) succeed(%s) fail(%s) sb(%s) rb(%s) resend(%s)   ch(%s) ",
				hashCode(),

				this.getKeepConnection(),

				isConnector(),

				StringUtil.formatCount(getReconnectCount()),

				-1, // StringUtil.formatCount(getSendTaskBufferFullErrorCount()),

				StringUtil.formatCount(m_sending_jt.getSucceedCount()),

				StringUtil.formatCount(m_sending_jt.getFailCount()),

				StringUtil.formatBytesSize(m_sentBytes),

				StringUtil.formatBytesSize(m_receivedBytes),

				StringUtil.formatCount(m_resendPacketCount),

				m_channel);
		return s;
	}

	/**
	 * add 191205
	 * 
	 * @return
	 */
	public String getRemoteAddress() {

		try {
			return this.getChannel().getRemoteAddress().toString();
		} catch (Exception e) {
			// e.printStackTrace();
			return null;
		}

	}

	public SocketChannel getChannel() {

		return m_channel;
	}

	public long getId() {
		return m_id;
	}

	public void gc() {
		m_receivedItemList.removeByTime(16);
		m_returnItemList.removeByTime(16);
		m_recentlyReturn.removeByTime(16);
		m_recentlyAck.removeByTime(16);
	}

	public void setIdPwd(String id, String pwd) {
		m_idname = id;
		m_pwd = pwd;
	}

	public void setLoggedIn(boolean b) {
		m_loggedIn = b;
	}

	public boolean isLoggedIn() {
		return m_loggedIn;
	}

	public String getIdName() {
		return m_idname;
	}

	public String getPwd() {
		return m_pwd;
	}

	public SocketChannel connect(String ip, int port) throws Exception {

		if (isConnecting())
			throw new UserException("connecting now...");

		if (isAlive()) {
			throw new UserException("channel is alive");
		}

		m_channel = m_selector.connect(ip, port, this);

		return m_channel;

	}

	public long getResendMs() {
		return m_ackAverage.getAverage();
	}

	@Deprecated
	public void setBindEventQueue(TmsEventQueue q) {
		m_bindQueue = q;
	}

	@Deprecated
	public TmsEventQueue getBindEventQeue() {
		return m_bindQueue;
	}

	public void setChannelInterface(TmsChannelInterface itr) {
		m_channelInterface = itr;
	}

	public TmsChannelInterface getChannelInterface() {
		return m_channelInterface;
	}

	// public long getlastReturnCode() {
	// return m_lastReturnCode;
	// }

	/**
	 * 
	 * @param sec
	 * @return
	 */
	public boolean waitingForConnection(double sec) {
		TimeUtil t = new TimeUtil();

		while (t.end_sec() < sec && isAlive() == false) {
			TimeUtil.sleep(100);
		}

		return isAlive();

	}

	/**
	 * 
	 * @param to
	 * @param timeout
	 * @param func
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public Object callMethod(String to, long timeout, String func, Object... params) throws Exception {

		return TmsFunc.callMethod(this, to, timeout, func, params);

	}

	/**
	 * 
	 * @param to
	 * @param func
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public Object callMethod(String to, String func, Object... params) throws Exception {

		return TmsFunc.callMethod(this, to, (long) (m_selector.getChannelTimeoutSec(m_channel) * 1000), func, params);

	}

	/**
	 * 
	 * @param cls
	 * @param rx
	 * @param e
	 * @return
	 * @throws Exception
	 */
	public boolean doMethod(Object cls, byte[] rx, TmsItem e) throws Exception {
		return TmsFunc.doMethod(cls, this, rx, e);

	}

	/**
	 * 
	 * @return
	 */
	public boolean isConnecterLoggedIn() {
		return m_conLoggedIn;
	}
}
