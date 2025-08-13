package home.lib.net.nio;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import home.lib.io.FilenameUtils;
import home.lib.io.JobTask;
import home.lib.io.JobTaskInterface;
import home.lib.lang.UserException;
import home.lib.util.AverageUtil;
import home.lib.util.DataStream;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

final public class NqConnector implements NqChannelListener, JobTaskInterface {

	public int debugLocate = 0;

	/**
	 * 
	 * 
	 * @author richard
	 *
	 */
	private class UserJob {

		public UserJob(NqItem a) {
			m = a;
		}

		NqItem m = null;
		int count = 0;// send count
		long send_time = 0;

		public void set_send_time() {
			if (send_time == 0) {
				send_time = System.nanoTime();// remeber when it send
			}
		}
	}

	private NqConnector _this = this;
	private NqFactory m_factory = null;
	private String m_path = null;
	//// support for connected sockets , not accepted sockets

	private JobTask<UserJob> m_jt = new JobTask<UserJob>(this);
	private NqChannel m_channel = null;
	private INioConnectorListener m_inter = null;
	private NqPacket m_parser = null;
	private InetSocketAddress m_bind = null;
	// private long m_socketBufferSize = 1024 * 8;
	private long m_sendIndex = 0;
	private long m_reconnectCount = 0;
	private ArrayList<NqItem> m_receivedItemList = new ArrayList<NqItem>();
	private ArrayList<NqItem> m_returnItemList = new ArrayList<NqItem>();
	private String m_ip = null;
	private int m_port = 0;
	// private boolean m_isKeepConnection = true;
	private TimeUtil m_reconnectionInterval = new TimeUtil();

	private TimeUtil m_tmrCheckLink = new TimeUtil();
	// private double m_recevieTimeoutSec = 60;
	private TimeUtil m_recvTime = new TimeUtil();

	private Object syncReturn = new Object();
	private NqItem m_returnItem = null;
	private long m_sendTaskFullErrorCount = 0;

	protected long m_receivedPacketCount = 0;
	protected long m_sendPacketCount = 0;
	protected long m_resendPacketCount = 0;

	protected long m_receivedBytes = 0;
	protected long m_sentBytes = 0;

	private long m_sentCount = 0;

	private double m_reconnectableIntervalSec = 6;
	private double m_recevieTimeoutSec = 60;

	private boolean m_keepConnection = false;

	AverageUtil m_ack = new AverageUtil();
	private long m_sentBytesPerSec = 0;

	// private double m_resp_average_ms = 300;// ms

	protected long m_ignore_recv_s = 0;
	private String m_id;
	private String m_pwd;
	private boolean m_connecterLogin = false; // 'i' code use
	private boolean m_accepterLogin = false;
	private TimeUtil m_aliveTime = new TimeUtil();
	// private TimeUtil m_login_retry_timer = null;

	/**
	 * 
	 */

	public NqConnector(NqFactory bs) throws Exception {
		this(bs, null);
	}

	public NqConnector(NqFactory bs, NqSelector sel) throws Exception {

		m_factory = bs;

		// m_selector = bs.getSelector();
		m_channel = new NqChannel(sel);

		m_jt.setMaxTask(bs.m_jt_maxQueue);
		m_jt.resendMs(bs.m_jt_resendMs);
		m_jt.TimeoutMs(bs.m_jt_timeoutMs);

		this.setReceiveTimeoutSec(m_factory.getReceiveTimeoutSec());
		this.setReconnectableIntervalSec(m_factory.getReconnectableIntervalSec());

	}

	public void resetSentCount() {
		m_sentCount = 0;
		m_sentBytesPerSec = 0;
	}

	/**
	 * 
	 * @param maxQueue
	 * @param resend
	 * @param timeout
	 * @return
	 */
	public NqConnector jobOpt(int maxQueue, long resend, long timeout) {
		m_jt.setMaxTask(maxQueue);
		m_jt.resendMs(resend);
		m_jt.TimeoutMs(timeout);
		return this;
	}

	/**
	 * 
	 * @return
	 */
	public JobTask getJt() {
		return m_jt;
	}

	/**
	 * 
	 * @return
	 */
	public NqChannel getChannel() {
		return m_channel;
	}

	/**
	 * 
	 * @param ri
	 */
	private NqConnector setReconnectableIntervalSec(double ri) {
		m_reconnectableIntervalSec = ri;
		return this;

	}

	/**
	 * 
	 * @param to
	 */
	private NqConnector setReceiveTimeoutSec(double to) {
		m_recevieTimeoutSec = to;
		return this;

	}

	/**
	 * 
	 * @return
	 */
	private double getReconnectableIntervalSec() {
		return m_reconnectableIntervalSec;
	}

	/**
	 * 
	 * @return
	 */
	private double getReceiveTimeoutSec() {
		return m_recevieTimeoutSec;
	}

	/**
	 * 
	 * @param b
	 * @return
	 * @throws Exception
	 */
	public NqConnector setIpBind(InetSocketAddress b) throws Exception {

		m_bind = b;

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
	 * set by server
	 * 
	 * @param l
	 */
	protected void setSocketBufferSize(long l) {
		m_parser.setBufferSize(l);
	}

	/**
	 * 
	 * @return
	 */
	public long getSocketBufferSize() {
		return m_parser.getBufferSize();
	}

	/**
	 * 
	 * setpath
	 * 
	 * @param s
	 * @return
	 */
	public NqConnector setPath(String s) throws Exception {

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

		// if (m_bootstrap.putPathOfClient(_this) == false) {
		// throw new UserException("it's not connected!");
		// }

		return this;
	}

	
	/**
	 * 
	 * @param s
	 * @return
	 */
	static public boolean checkPathFormat(String s)  {

		if (s.indexOf("*") != -1 || s.indexOf('?') != -1 || s.indexOf('|') != -1 || s.indexOf(':') != -1
				|| s.indexOf('<') != -1 || s.indexOf('>') != -1) {
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public int sendQueueCount() {
		return m_jt.size();
	}

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
	 * @param ip
	 * @param port
	 * @param l
	 * @return
	 * @throws Exception
	 */
	// public NqConnector connect(final String ip, final int port, final INioConnectorListener l) throws Exception {
	//
	// return connect("" + System.nanoTime(), "", ip, port, l);
	// }

	public NqConnector connect(String id, String pwd, final String ip, final int port, final INioConnectorListener l)
			throws Exception {

		if (isAlive()) {
			throw new UserException("channel is alive");
		}
		if (isAccepter()) {
			throw new UserException("accept socket has not permission");
		}

		if (isConnecting()) {
			throw new UserException("channel is connecting");
		}

		if (getPath() == null || getPath().trim().length() == 0) {
			throw new UserException("channel name is empty");
		}

		debug("connect ");

		m_id = id;
		m_pwd = pwd;
		//
		m_inter = l;
		m_ip = ip;
		m_port = port;
		m_channel.setIpBind(m_bind).setSocketBufferSize(m_factory.getSocketBufferSize()).connect(ip, port, this);

		m_reconnectionInterval.start();
		return this;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isConnecting() {
		return m_channel.isConnecting();
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public NqConnector reconnect() throws Exception {

		if (isAlive())
			return this;

		if (isAccepter()) {
			// return this;
			throw new UserException("accept socket doesn't alloc to use reconnect func ");
		}

		if (m_ip == null || m_port == 0)
			return this;

		m_reconnectionInterval.start();

		m_reconnectCount++;
		m_channel.setIpBind(m_bind).setSocketBufferSize(m_factory.getSocketBufferSize()).connect(m_ip, m_port, this);

		return this;
	}

	/**
	 * 
	 * 
	 * @return
	 */
	public boolean isReconnectable() {

		if (m_channel.isConnecting()) {
			m_reconnectionInterval.start();
			// debug("still connecting");
			return false;
		}

		if (m_reconnectionInterval.end_sec() < this.getReconnectableIntervalSec())
			return false;
		
		
		//c.isAlive() == false && c.isAccepter() == false && c.isReconnectable() && c.getKeepConnection()

		debug("%s isReconnectable %s < %s (%s) isalive(%s) isaccepter(%s) keep con(%s) ", TimeUtil.now(), m_reconnectionInterval.end_sec(),
				this.getReconnectableIntervalSec(), getPath(), isAlive(), isAccepter(), getKeepConnection() );

		m_reconnectionInterval.start();
		return true;

	}

	/**
	 * 
	 * 
	 * @param d
	 * @return
	 */
	public NqConnector waitConnect(double d) {
		TimeUtil t = new TimeUtil();

		while (t.end_sec() < d && isAlive() == false) {
			TimeUtil.sleep(10);
		}

		// login!
		try {
			checkConnectorLogin((long) (d * 1000));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return this;
	}
	//
	// /**
	// *
	// * @param ch
	// * @param l
	// * @return
	// * @throws Exception
	// */
	// public MqClient accept(AsynchronousSocketChannel ch, final MqClientInterface l) throws Exception {
	//
	// if (isAlive()) {
	// throw new UserException("channel is alive");
	// }
	//
	// m_inter = l;
	// m_channel.setIpBind(m_bind).setSocketBufferSize(m_bootstrap.getSocketBufferSize()).accept(ch, this);
	//
	// return this;
	// }

	/**
	 * 
	 * @param ms
	 * @param ch
	 * @param l
	 * @return
	 * @throws Exception
	 */
	public NqConnector accept(NqSelector ms, SocketChannel ch, final INioConnectorListener l) throws Exception {

		if (isAlive()) {
			throw new UserException("channel is alive");
		}

		m_reconnectionInterval.start();
		m_recvTime.start();// timer initialize

		ch.socket().setSendBufferSize((int) m_factory.getSocketBufferSize());
		ch.socket().setReceiveBufferSize((int) m_factory.getSocketBufferSize());

		m_inter = l;
		m_channel.setIpBind(m_bind).setSocketBufferSize(m_factory.getSocketBufferSize()).accepted(ms, ch, this);

		// try {

		m_parser = new NqPacket().setBufferSize(m_channel.socketBufferSize());

		// MqChannel mch = new MqChannel();

		// m_channel.connect(sel, ch, this);

		// m_channel = mch;// alloc

		m_jt.clear();
		m_factory.put(this);

		// } catch (Exception e) {
		//
		// e.printStackTrace();
		// try {
		// ch.close();
		// } catch (IOException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		// return null;
		// }

		try {
			NqItem a = new NqItem(new byte[0]);
			sendAck((byte) 'l', a);// send 'link'
		} catch (Exception e) {

		}

		m_inter.connected(this);

		return this;
	}

	/**
	 * socket close KeepConnection is disable
	 * 
	 * m_bootstrap.remove(this);
	 * 
	 * 
	 */
	public void close() {

		debug("(%s) call close", getPath());
		// m_isKeepConnection = false;
		m_factory.remove(this);

		m_channel.close();

	}

	/**
	 * 
	 * @return
	 */
	public boolean isAlive() {
		// if (m_channel == null)
		// return false;

		return m_channel.isAlive();
	}

	/**
	 * 
	 * @return
	 */
	public boolean isAccepter() {
		return m_channel.isAccept();
	}

	/**
	 * 
	 * @return
	 */
	public boolean isConnector() {
		return (m_channel.isAccept() == false);
	}

	// /**
	// *
	// * @return
	// */
	// public boolean isKeepConnection() {
	// return m_isKeepConnection;
	// }
	//
	// /**
	// *
	// * @param b
	// * @return
	// */
	// public MqClient setKeepConnection(boolean b) {
	// m_isKeepConnection = b;
	// return this;
	// }

	/**
	 * 
	 */
	@Override
	public void received(SocketChannel ch, byte[] buf, int len) {

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

			NqItem m = null;
			while ((m = m_parser.poll()) != null) {

				// debug("recv (%c) isAccept(%s) from(%s) to(%s) ", m.code , isAccepted() , m.from, m.to );

				// m.to.equals(anObject)

				// give some delay if the userage is over than 50%

				t.start();
				if (isAccepter()) {

					if (m_channel.selector().isPeddingOverload()) {
						if (m.code == 's' && m.to.length() > 0 && m.to.charAt(0) == '*') {// broadcast packets are igone
							// debug("[%s] ignore a packet. server is busy, userage(%s)%%", getPath(),
							// m_channel.selector().userage());
							m_ignore_recv_s++;
							continue;
						}
					}

					debugLocate = 2;
					doRecvInAccepter(m);

				} else {
					debugLocate = 3;
					doRecv(m);
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
	@Override
	public void connected(SocketChannel ch) {
		debug("connected  path(%s) sb(%s)", getPath(), StringUtil.formatBytesSize(m_channel.socketBufferSize()));

		try {

			m_reconnectionInterval.start();
			m_recvTime.start();// timer initialize
			m_aliveTime.start();

			// set buffer size
			ch.socket().setSendBufferSize((int) m_factory.getSocketBufferSize());
			ch.socket().setReceiveBufferSize((int) m_factory.getSocketBufferSize());

			m_parser = new NqPacket().setBufferSize(m_channel.socketBufferSize());

			// MqChannel mch = new MqChannel();

			// m_channel.connected(MqChannel.connect_selector(), ch, this);

			// m_channel = mch;// alloc

			m_jt.clear();
			m_factory.put(this);

			if (isConnector()) {
				m_factory.putPathOfClient(_this);
			} else {
				// isaccepted
				// putpath in recvinaccept
			}

		} catch (Exception e) {

			e.printStackTrace();
			try {
				ch.close();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return;
		}

		try {
			NqItem a = new NqItem(new byte[0]);
			sendAck((byte) 'l', a);// send 'link'
		} catch (Exception e) {

		}

		m_inter.connected(this);

	}

	/**
	 * 
	 */
	@Override
	public void disconnected(SocketChannel ask) {

		m_connecterLogin = false;

		m_reconnectionInterval.start();

		// accept or keep == false
		if (isAccepter() || (getKeepConnection() == false)) {
			m_factory.remove(this);
		}

		m_inter.disconnected(this);

		// debug("disconnected ");
	}

	/**
	 * 
	 */
	@Override
	public void doSucceeded(JobTask jt, Object e) {

		// m_bootstrap.m_sendSucceed++;

		if (e instanceof UserJob) {
			m_inter.sendSucceeded(this, ((UserJob) e).m);
		}
	}

	/**
	 * 
	 */
	@Override
	public void doFailed(JobTask jt, Object e) {
		// m_bootstrap.m_sendFail++;
		if (e instanceof UserJob) {
			m_inter.sendFailed(this, ((UserJob) e).m);
		}
	}

	/**
	 * 
	 * put recevied datas in queue without data bytse, because it can bring some memory problem
	 * 
	 * @param re
	 * @return
	 * @throws Exception
	 */
	private boolean searchInRecentlyRecevied(NqItem re) {

		synchronized (m_receivedItemList) {
			for (int i = 0; i < m_receivedItemList.size(); i++) {

				NqItem lr = m_receivedItemList.get(i);

				if (lr.sendIdx == re.sendIdx && lr.when == re.when && lr.from.equals(re.from)) {
					// debug(" ignore received data isAccept(%s) [sidx(%s) from(%s) here(%s) code(%c) ", isAccepted(),
					// re.sendIdx, re.from, getPath(), re.code);
					return true;
				}
			} // for(i

			// until after 16 seconds
			int p = 0;
			for (int i = 0; i < m_receivedItemList.size(); i++) {

				NqItem e = m_receivedItemList.get(p);
				if ((System.currentTimeMillis() - e.when) > ((16 * 1000))) {
					m_receivedItemList.remove(p);
				} else {
					p++;
				}
			} // for

			//
			//
			NqItem newOne = new NqItem();
			newOne.sendIdx = re.sendIdx;
			newOne.when = re.when;
			newOne.from = re.from;
			newOne.to = re.to;

			//
			//
			m_receivedItemList.add(newOne);
			if (m_receivedItemList.size() > 256) {
				m_receivedItemList.remove(0);
			}

		} // sync

		return false;
	}

	/**
	 * 
	 * @param re
	 * @return
	 * @throws Exception
	 */
	private NqItem searchInRecentlyReturns(NqItem s) {

		// debug("returned item.size=%d ", m_returnItemList.size());
		synchronized (m_returnItemList) {

			//
			for (int i = 0; i < m_returnItemList.size(); i++) {

				NqItem lr = m_returnItemList.get(i);

				if (lr.respIdx == s.sendIdx && lr.when == s.when && lr.to.equals(s.from)) {
					return lr;
				}
			} // for(i

		} // sync

		return null;
	}

	/**
	 * 
	 * @param m
	 */
	private void putInReturnList(NqItem m) throws Exception {

		synchronized (m_returnItemList) {
			// remove data older than 16 secones
			int p = 0;
			for (int i = 0; i < m_returnItemList.size(); i++) {

				NqItem e = m_returnItemList.get(p);
				if ((System.currentTimeMillis() - e.when) > ((16 * 1000))) {
					m_returnItemList.remove(p);
				} else {
					p++;
				}
			} // for

			m_returnItemList.add(NqItem.fromBytes(m.toBytes()));

			// max limit is 32
			if (m_returnItemList.size() > 32) {
				m_returnItemList.remove(0);
			}

		} // sync

	}

	public double getLastReceivedTime() {
		return m_recvTime.end_sec();
	}

	/**
	 * 
	 * @param m
	 */
	private void doRecv(NqItem m) throws Exception {

		if (isAccepter())
			return;

		m_recvTime.start();
		debugLocate = 30;

		// debug("recv (%s)", getPath());

		if (m.code == (byte) 's') {// receive normal data

			// debug( "dorecv (%s) (%s) ", getId(), m.to );

			// server(cli)->client
			if (FilenameUtils.wildcardMatch(getPath(), m.getTo()) == false)
				return;

			sendAck((byte) 'a', m);

			if (searchInRecentlyRecevied(m)) { // was it receive before?

				if ((m.option & NqItem.WAIT_RETURN) == NqItem.WAIT_RETURN) {// ask return?
					// already returned?
					NqItem r = searchInRecentlyReturns(m);
					if (r != null) {
						sendReturn(r.to, r);// send return
					}
				}
				return;
			}

			try {

				// prepare return item
				NqItem ret = new NqItem();
				ret.code = 'r';
				ret.sendIdx = (m_sendIndex++);
				ret.respIdx = m.sendIdx;// set response index
				ret.when = m.when;// copy when it sent
				ret.from = this.getPath();
				ret.to = m.from;// alloc sender name

				debugLocate = 31;
				NqItem rs = m_inter.received(this, m, m.data);
				debugLocate = 32;
				if (rs != null && (m.option & NqItem.WAIT_RETURN) == NqItem.WAIT_RETURN) {

					ret.data = rs.data;// copy data
					sendReturn(ret.to, ret);// send return
					putInReturnList(ret);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (m.code == (byte) 'r') {// get return value

			if (FilenameUtils.wildcardMatch(getPath(), m.getTo()) == false)
				return;

			sendAck((byte) 'a', m);

			if (searchInRecentlyRecevied(m))// received check
				return;

			synchronized (syncReturn) {
				m_returnItem = m;
			} // sync

		} else if (m.code == (byte) 'l') {// check link(ask)

			if (m.data != null && m.data.length != 0) {
				try {

					NqProperties pr = NqProperties.fromBytes(m.data);

					int to = pr.getInteger("to");// receive timeout
					int ri = pr.getInteger("ri");// reconnect interval
					int bs = pr.getInteger("bs");// buffer size

					if (this.getReceiveTimeoutSec() != to || this.getReconnectableIntervalSec() != ri
							|| this.getSocketBufferSize() != bs) {
						this.setReceiveTimeoutSec(to);
						this.setReconnectableIntervalSec(ri);
						this.setSocketBufferSize(bs);
						debug("(%s) recv[ server info   timeout(%s) reconnect interval(%s)  buffer size(%s) ",
								getPath(), to, ri, StringUtil.formatBytesSize(bs));
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		} else if (m.code == (byte) 'e') {// retrun error from server

			String s = new String(m.data);
			debug("return error = " + s + " len=" + m.data.length);

	 
			try {
				m_inter.returnError(this, new UserException(s));
			} catch (Exception e) {
				e.printStackTrace();
			}
			//// to be remend 191014. this.getChannel().close();
			// throw new UserException(s);

		} else if (m.code == (byte) 'a') {// get ack

			// debug("redv (%s)", m.sendIdx );
			checkResp(m);// send ok

		} else if (m.code == (byte) 'i') {// get id/pwd checked response

			try {
				// DataStream ds = new DataStream(m.data);
				// String id = ds.readString();
				// String pwd = ds.readString();
				m_connecterLogin = true;// id chekc ok!
				checkResp(m);// send ok

				debug("get response login(%s,%s) respidx=%x ", m_id, m_pwd, m.respIdx);
				//
				// if (m_id.equals(id) && m_pwd.equals(pwd)) {
				// m_connecterLogin = true;// id chekc ok!
				// return;
				// }
				//
			} catch (Exception e) {
				debug(UserException.getStackTrace(e));

			}
		}

		debugLocate = 39;
	}

	/**
	 * (accepter use)
	 * 
	 * @return
	 */
	private boolean isAuthorized() {
		return (m_accepterLogin || m_factory.enableLogin() == false);
	}

	/**
	 * 
	 * @param m
	 * @return
	 */
	private boolean checkPath(NqItem m) {

		if (m_path == null) {// try to register
			if (m_inter.putPath(_this, m.from)) {

				m_path = m.from;// copy name
				m_factory.putPathOfClient(_this);

				return true;// registration success
			} else {
				sendError(String.format("path(%s) is not allow", m.from));
				m_channel.close();
				return false;// registration failure
			}
		}
		return true;//// registration success already
	}

	/**
	 * 
	 * 
	 * @param m
	 */
	private void doRecvInAccepter(NqItem m) {

		if (isAccepter() == false)
			return;

		m_recvTime.start();

		if (m.code == (byte) 's' && isAuthorized()) {// receive normal data

			// sendAck((byte) 'a', m);
			//
			// if (searchInRecentlyRecevied(m))// received check
			// return;

			// if (m_path == null) {
			//
			// if (m_inter.putPath(_this, m.from)) {
			//
			// m_path = m.from;// copy name
			// m_factory.putPathOfClient(_this);
			// } else {
			// sendError(String.format("path(%s) is not allow", m.from));
			// m_channel.close();
			// return;
			// }
			// }

			if (checkPath(m) == false)
				return;

			NqItem rs = m_inter.received(this, m, m.data);// recevied data check in interface.recv(in server)

			if (rs != null) {

				try {
					if (m_factory.sendTo(this, m)) {// if( no error
						sendAck((byte) 'a', m); // send ack
					} else {
						sendError("some of the destination queues are full");
					}

				} catch (Exception e) {

					sendError(e.toString());
				}
			}

		} else if (m.code == (byte) 'r' && isAuthorized()) {// get return value

			// if (searchInRecentlyRecevied(m))// received check
			// return;

			NqItem rs = m_inter.received(this, m, m.data);
			if (rs != null) {

				try {
					if (m_factory.sendTo(this, m)) {
						sendAck((byte) 'a', m);
					} else {
						sendError("some of the destination queues are full");
					}
				} catch (Exception e) {

					sendError(e.toString());

				}
			}

		} else if (m.code == (byte) 'l') {// check link(ask)

			// debug("recv 'link' (%s)", m.from );
			// if (m_path == null) {
			// if (m_inter.putPath(_this, m.from)) {
			// m_path = m.from;// copy name
			// m_factory.putPathOfClient(_this);
			// } else {
			// sendError(String.format("path(%s) is not allow", m.from));
			// m_channel.close();// close
			// return;
			// }
			//
			// }
			if (checkPath(m) == false)
				return;

			sendAck((byte) 'l', new NqItem());// reply 'link' command

			// debug("recv(link)~~~~~~~~~~~~~"+isAccept() );

		} else if (m.code == (byte) 'a') {// get ack

			checkResp(m);// send ok

		} else if (m.code == (byte) 'i') {// id/pwd check

			// if (searchInRecentlyRecevied(m))// received check
			// return;

			try {
				DataStream ds = new DataStream(m.data);
				String id = ds.readString();
				String pwd = ds.readString();

				debug("request login(%s,%s) sendidx=%x ", id, pwd, m.sendIdx);

				if (m_inter.login(this, id, pwd) == false) {
					
					debug("request login fail!!! " );
					
					m_channel.close();
					return;
				}

				m_accepterLogin = true;
				sendAck((byte) 'i', m, m.data);

				//
				if (checkPath(m) == false)
					return;

			} catch (Exception e) {
				m_channel.close();
				debug(UserException.getStackTrace(e));

			}

		} 

	}

	/**
	 * 
	 * 
	 * @param r
	 */
	private void checkResp(NqItem r) {

		synchronized (m_jt) {

			for (int h = 0; h < m_jt.size(); h++) {

				UserJob j = m_jt.get(h);

				if (j.m.sendIdx == r.respIdx && j.m.when == r.when && (r.code == (byte) 'a' || r.code == (byte) 'i')) {
					m_jt.setResp(j, true);
					// debug("get ack (%s)isAccept(%s)", getId(), isAccept());

					getAutoResendMs(j);

				}

			} // for(h

		} // sync

	}

	/**
	 * statistics
	 * 
	 * @param j
	 */
	private void getAutoResendMs(UserJob j) {
		m_ack.sum(System.nanoTime() - j.send_time);

		if (m_ack.isTimeover(3000)) {

			double l = (double) m_ack.getLong() * 1.4;

			double d = (long) (((double) l / 1000000));// min 100ms ~

			// m_resp_average_ms = d;

			// debug("ack average= %.3f ms ", d );
			if (d > 0) {

				double old = m_jt.getResendMs();

				if (d > old)
					m_jt.resendMs((long) (old * 1.1)); // + 0.1 %
				else
					m_jt.resendMs((long) (old * 0.9)); // - 0.1 %
			}
		}

	}

	/**
	 * 
	 * 
	 * @param s
	 */
	public void debug(String s, Object... args) {

		m_factory.debug(this, " mqconnector [" + String.format(s, args));
	}

	public void debug(Exception e) {

		debug("%s", UserException.getStackTrace(e));
	}

	/**
	 * 
	 * 
	 * 
	 */
	protected long[] doSendTask() {

		synchronized (m_jt) {

			// int rcnt=0;
			int end = 8;// (int) m_jt.size();
			int maxloop = 0;
			UserJob u = null;

			do {
				u = m_jt.autoPeek(m_factory.jobPeekCount());

				if (u != null && isAlive()) {
					// debug("doSendTask");

					u.set_send_time();

					try {
						byte[] b = NqPacket.make(u.m);

						m_sentBytes += b.length;
						m_sendPacketCount++;

						m_channel.send(b);// send!

						if (u.count > 0) {
							m_resendPacketCount++;
						}
						u.count++;

						m_sentCount++;
						m_sentBytesPerSec += b.length;
						// return true;
					} catch (Exception e) {

						e.printStackTrace();
					}

				}

				maxloop++;
			} while (u != null && maxloop < end);

			return new long[] { m_sentCount, m_sentBytesPerSec };
		} // sync

	}

	/**
	 * connector to connector(other)
	 * 
	 * @param to
	 * @param da
	 * @throws Exception
	 */
	public void send(String to, byte[] da) throws Exception {
		send(to, da, false, false);
	}

	public NqItem send(String to, byte[] da, boolean waitAck) throws Exception {
		return send(to, da, waitAck, false);
	}

	synchronized private NqItem send(String to, byte[] da, boolean waitAck, boolean waitReturn) throws Exception {

		NqItem m = new NqItem();
		m.data = da;

		if (waitReturn) {

			m.option |= (byte) NqItem.WAIT_RETURN;// ask return

		}

		UserJob job = null;

		synchronized (m_jt) {

			m.from = m_path;
			m.code = 's';
			m.to = to;
			m.sendIdx = (m_sendIndex++);

			job = new UserJob(m);

			if (m_jt.add(job) == false) {
				m_sendTaskFullErrorCount++;
				debug("mqclient[(%s)send queue is full (max:%s)", getPath(), m_jt.getMaxTask());

				// TimeUtil.sleep(10);
				// m_inter.sendFailed(this, m);
				return null;// false;
			}

		} // sync

		doSendTask();// send

		if (waitAck) {
			TimeUtil t = new TimeUtil();
			while (t.end_ms() < m_jt.getTimeoutMs()) {
				synchronized (m_jt) {
					UserJob j = m_jt.find(job);
					//
					if (j == null) { // mean is timeover or already got reaponse
						return m;// true;
					}
				}
			} // while
			return null;// false;
		}

		return m;// true;
	}

	/**
	 * 
	 * @param timeout_ms
	 * @return
	 * @throws Exception
	 */
	public boolean checkConnectorLogin(long timeout_ms) throws Exception {

		if (isConnector() == false)// connector use
			return false;

		if (m_connecterLogin) // login already done.
			return true;

		//
		// //check retry timer ( first time or every X sec)
		// if (m_login_retry_timer == null || m_login_retry_timer.end_sec() > m_reconnectableIntervalSec) {
		//
		// if (m_login_retry_timer == null) {
		// m_login_retry_timer = new TimeUtil();
		// }
		// m_login_retry_timer.start();
		// // continue;
		// } else {
		// return false;
		// }

		NqItem m = new NqItem();
		m.data = new DataStream().writeString(m_id).writeString(m_pwd).copyOf();

		UserJob job = null;

		synchronized (m_jt) {

			m.from = m_path;
			m.code = 'i';
			m.to = "*";
			m.sendIdx = (m_sendIndex++);

			job = new UserJob(m);

			if (m_jt.add(job) == false) {
				m_sendTaskFullErrorCount++;
				debug("mqclient[(%s)send queue is full (max:%s) - err", getPath(), m_jt.getMaxTask());
				return false;
			}

		} // sync

		doSendTask();// send

		// if (true) {
		TimeUtil t = new TimeUtil();
		while (t.end_ms() < timeout_ms) {
			synchronized (m_jt) {
				UserJob j = m_jt.find(job);
				//
				if (j == null) { // mean is timeover or already got reaponse
					return m_connecterLogin;// true;
				}
			}
		} // while
		return m_connecterLogin;// false;
		// }

		// return true;
	}

	/**
	 * 
	 */
	public void checkAccepterLogin() {

		if (isAccepter()) {

			if (m_accepterLogin == false) {

				if (m_aliveTime.end_sec() > this.m_recevieTimeoutSec) {
					debug("[in accepter] not login! (%s) ", m_aliveTime.end_sec());
					m_channel.close();
				}

			} // if(

		} // if(accepter

	}

	/**
	 * 
	 * server(cli)->server(cli)
	 * 
	 * @param to
	 * @param m
	 */
	protected boolean sendDistribute(String to, NqItem m) {
		synchronized (m_jt) {

			if (m_jt.add(new UserJob(m)) == false) {
				m_sendTaskFullErrorCount++;
				debug("(%s)send distribute to(%s) queue is full (max:%s)", getPath(), to, m_jt.getMaxTask());

				m_inter.sendFailed(this, m);
				return false;
			}

		} // sync

		doSendTask();// send
		return true;
	}

	/**
	 * 
	 * 
	 * @param to
	 * @param m
	 * @throws Exception
	 */
	protected void sendReturn(String to, NqItem m) throws Exception {
		synchronized (m_jt) {

			m.sendIdx = (m_sendIndex++);
			if (m_jt.add(new UserJob(m)) == false) {
				m_sendTaskFullErrorCount++;

				debug("mqclient[(%s)send return to(%s) queue is full (max:%s)", getPath(), to, m_jt.getMaxTask());

				m_inter.sendFailed(this, m);
				return;
			}

		} // sync

		doSendTask();// send
	}

	/**
	 * 
	 * @param m
	 */
	private void sendAck(byte code, NqItem m) {

		if (code == (byte) 'l' && isAccepter()) {// when line link send (from server)
			NqProperties p = new NqProperties();

			p.setInteger("to", (int) this.getReceiveTimeoutSec());// timeout
			p.setInteger("bs", (int) this.getSocketBufferSize());// buffer size
			p.setInteger("ri", (int) this.getReconnectableIntervalSec());// reconnect interval
			try {
				sendAck(code, m, p.toBytes());
			} catch (Exception e) {
				e.printStackTrace();
				sendAck(code, m, null);
			}
		}
		// else if( code==(byte)'a' && m_channel.selector().userage()>80 ) {
		//
		// NqProperties p = new NqProperties();
		//
		// p.setInteger("userage", (int)m_channel.selector().userage() );// userage
		//
		// }
		else {
			sendAck(code, m, null);
		}

	}

	/**
	 * 
	 * @param code
	 * @param m
	 * @param data
	 */
	private void sendAck(byte code, NqItem m, byte[] data) {

		NqItem a = new NqItem(new byte[0]);
		a.from = m_path;
		a.code = code;
		a.sendIdx = (m_sendIndex++);
		a.respIdx = m.sendIdx;
		a.when = m.when;
		if (data != null) {
			a.data = data;
		}

		try {

			byte[] b = NqPacket.make(a);

			m_sentBytes += b.length;
			m_sendPacketCount++;
			m_channel.send(b);
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
		sendAck((byte) 'e', new NqItem(), b);// return err
	}

	/**
	 * 
	 * @param to
	 * @param da
	 * @return
	 * @throws Exception
	 */
	public byte[] get(String to, byte[] da) throws Exception {
		return get(to, da, m_factory.getWaitReturnTimeoutSec() * 1000);
	}

	/**
	 * 
	 * @param to
	 * @param da
	 * @param timeout_ms
	 * @return
	 * @throws Exception
	 */
	synchronized public byte[] get(String to, byte[] da, double timeout_ms) throws Exception {

		m_returnItem = null;

		NqItem sn = send(to, da, false, true);

		TimeUtil t = new TimeUtil();
		while (t.end_ms() < timeout_ms) {

			TimeUtil.sleep(10);

			synchronized (syncReturn) {

				if (m_returnItem != null) {

					NqItem rn = NqItem.fromBytes(m_returnItem.toBytes());

					// debug("nqconnector.get -chk ret %s==%s %s==%s %s==%s \r\n" ,
					// rn.from,to , sn.sendIdx,rn.respIdx, sn.when, rn.when
					// );

					if (rn.from.equals(to) && sn.sendIdx == rn.respIdx && sn.when == rn.when) {
						return rn.getData();// 191205
					}
				}
			} // sync
		}

		throw new UserException("return is null.");
	}

	/**
	 * 
	 */
	protected void doCheckLink() {

		double rtimeout = this.getReceiveTimeoutSec();

		int sec0 = (int) ((rtimeout - 3) / 2);

		if (m_tmrCheckLink.end_sec() > sec0) // send it two times
		{
			m_tmrCheckLink.start();

			if (isConnector()) {

				sendAck((byte) 'l', new NqItem());// send 'link'
			}
		}

		// time out check
		if (getLastReceivedTime() > rtimeout && rtimeout != 0) {

			debug("socket timeout!!! isAccept(%s) recvLastTime(%f/%f) getId(%s) ", _this.isAccepter(),
					_this.getLastReceivedTime(), rtimeout, _this.getPath());

			m_recvTime.start();// timer initialize
			getChannel().close();
		}

	}

	/**
	 * 
	 * @return
	 */
	public long getSendTaskBufferFullErrorCount() {
		return m_sendTaskFullErrorCount;
	}

	/**
	 * 
	 * @return
	 */
	public boolean getKeepConnection() {
		return m_keepConnection;
	}

	/**
	 * 
	 * @param k
	 */
	public void setKeepConnection(boolean k) {
		this.m_keepConnection = k;
	}

	/**
	 * 
	 * 
	 */
	public void setKeepConnectionBeforeConnected() {

		this.m_keepConnection = true;
		m_factory.put(this);

	}

	@Override
	public Object _lock() {
		return m_channel._lock();
	}

	@Override
	public String toString() {

		String s = "";

		s += String.format("nqconnector@%s  keepCon(%s) %s ", hashCode(), this.getKeepConnection(), m_channel);
		return s;
	}

	/**
	 * add 191205
	 * 
	 * @return
	 */
	public String getRemoteAddress() {

		try {
			return this.getChannel().getChannel().getRemoteAddress().toString();
		} catch (Exception e) {
			// e.printStackTrace();
			return null;
		}

	}

}
