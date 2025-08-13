package deprecated.stable.lib.net.mq2.dev.copy190515;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import home.lib.io.FilenameUtils;
import home.lib.io.JobTask;
import home.lib.io.JobTaskInterface;
import home.lib.lang.UserException;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

final public class MqClient implements MqChannelInterface, JobTaskInterface {

	private class UserJob {

		public UserJob(MqItem a) {
			m = a;
		}

		MqItem m = null;
		int count = 0;// send count
	}

	private MqClient _this = this;
	private MqBootstrap m_bootstrap = null;
	private String m_path = null;
	//// support for connected sockets , not accepted sockets

	private JobTask<UserJob> m_jt = new JobTask<UserJob>(this);
	private MqChannel m_channel = null;
	private MqClientInterface m_inter = null;
	private MqPacket m_parser = null;
	private InetSocketAddress m_bind = null;
	// private long m_socketBufferSize = 1024 * 8;
	private long m_sendIndex = 0;
	private long m_reconnectCount = 0;
	private ArrayList<MqItem> m_receivedItemList = new ArrayList<MqItem>();
	private ArrayList<MqItem> m_returnItemList = new ArrayList<MqItem>();
	private String m_ip = null;
	private int m_port = 0;
	// private boolean m_isKeepConnection = true;
	private TimeUtil m_reconnectionInterval = new TimeUtil();

	private TimeUtil m_tmrCheckLink = new TimeUtil();
	// private double m_recevieTimeoutSec = 60;
	private TimeUtil m_recvTime = new TimeUtil();

	private Object syncReturn = new Object();
	private MqItem m_returnItem = null;
	private long m_sendTaskFullErrorCount = 0;

	protected long m_receivedPacketCount = 0;
	protected long m_sendPacketCount = 0;
	protected long m_resendPacketCount = 0;

	protected long m_receivedBytes = 0;
	protected long m_sentBytes = 0;

	private long m_sentCount = 0;

	private double m_reconnectableIntervalSec = 6;
	private double m_recevieTimeoutSec = 60;

	/**
	 * 
	 */
	public MqClient(MqBootstrap bs) {

		m_bootstrap = bs;
		m_channel = new MqChannel();

		m_jt.setMaxTask(bs.m_jt_maxQueue);
		m_jt.resendMs(bs.m_jt_resendMs);
		m_jt.TimeoutMs(bs.m_jt_timeoutMs);

		this.setReceiveTimeoutSec(m_bootstrap.getReceiveTimeoutSec());
		this.setReconnectableIntervalSec(m_bootstrap.getReconnectableIntervalSec());

	}

	public void resetSentCount() {
		m_sentCount = 0;
	}

	/**
	 * 
	 * @param maxQueue
	 * @param resend
	 * @param timeout
	 * @return
	 */
	public MqClient jobOpt(int maxQueue, long resend, long timeout) {
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
	public MqChannel getChannel() {
		return m_channel;
	}

	/**
	 * 
	 * @param ri
	 */
	private MqClient setReconnectableIntervalSec(double ri) {
		m_reconnectableIntervalSec = ri;
		return this;

	}

	/**
	 * 
	 * @param to
	 */
	private MqClient setReceiveTimeoutSec(double to) {
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
	public MqClient setIpBind(InetSocketAddress b) throws Exception {

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
	 * setpath and keep connection ( in bootstrap )
	 * 
	 * @param s
	 * @return
	 */
	public MqClient setPath(String s) throws Exception {

		if (m_path != null) {
			throw new UserException("id(%s) already set", m_path);
		}

		if (isAccept()) {
			throw new UserException("accept socket has not permission");
		}

		if (s.indexOf("*") != -1 || s.indexOf('?') != -1 || s.indexOf('|') != -1 || s.indexOf(':') != -1
				|| s.indexOf('<') != -1 || s.indexOf('>') != -1) {
			throw new UserException("id can not content the symbols( * ? : | < >  )  path(" + s);
		}

		m_path = s;

		if (m_bootstrap.putPathOfClient(_this) == false) {
			throw new UserException("it's not connected!");
		}

		// if (m_bootstrap.putPathOfClient(_this) == false) {
		// m_path = null;//
		// throw new UserException("name(%s) is exist!", s);
		//
		// }

		return this;
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
	public MqClient connect(final String ip, final int port, final MqClientInterface l) throws Exception {

		if (isAlive()) {
			throw new UserException("channel is alive");
		}
		if (isAccept()) {
			throw new UserException("accept socket has not permission");
		}

		m_inter = l;
		m_ip = ip;
		m_port = port;
		m_channel.setIpBind(m_bind).setSocketBufferSize(m_bootstrap.getSocketBufferSize()).connect(ip, port, this);

		m_reconnectionInterval.start();
		return this;
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public MqClient reconnect() throws Exception {

		if (isAlive())
			return this;

		if (isAccept()) {
			// return this;
			throw new UserException("accept socket doesn't alloc to use reconnect func ");
		}

		if (m_ip == null || m_port == 0)
			return this;

		m_reconnectionInterval.start();

		m_reconnectCount++;
		m_channel.setIpBind(m_bind).setSocketBufferSize(m_bootstrap.getSocketBufferSize()).connect(m_ip, m_port, this);

		return this;
	}

	/**
	 * 
	 * 
	 * @return
	 */
	public boolean isReconnectable() {

		if (m_reconnectionInterval.end_sec() < this.getReconnectableIntervalSec())
			return false;

		debug("%s isReconnectable %s < %s (%s)", TimeUtil.now(), m_reconnectionInterval.end_sec(),
				this.getReconnectableIntervalSec(), this);

		m_reconnectionInterval.start();
		return true;

	}

	/**
	 * 
	 * 
	 * @param d
	 * @return
	 */
	public MqClient waitConnect(double d) {
		TimeUtil t = new TimeUtil();

		while (t.end_sec() < d && isAlive() == false) {
			TimeUtil.sleep(10);
		}

		return this;
	}

	/**
	 * 
	 * @param ch
	 * @param l
	 * @return
	 * @throws Exception
	 */
	public MqClient accept(AsynchronousSocketChannel ch, final MqClientInterface l) throws Exception {

		if (isAlive()) {
			throw new UserException("channel is alive");
		}

		m_inter = l;
		m_channel.setIpBind(m_bind).setSocketBufferSize(m_bootstrap.getSocketBufferSize()).accept(ch, this);

		return this;
	}

	/**
	 * 
	 * @param ms
	 * @param ch
	 * @param l
	 * @return
	 * @throws Exception
	 */
	public MqClient accept(MqSelector ms, SocketChannel ch, final MqClientInterface l) throws Exception {

		if (isAlive()) {
			throw new UserException("channel is alive");
		}

		m_inter = l;
		m_channel.setIpBind(m_bind).setSocketBufferSize(m_bootstrap.getSocketBufferSize()).accept(ms, ch, this);

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
		m_bootstrap.remove(this);

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
	public boolean isAccept() {
		return m_channel.isAccept();
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
	public void recv(MqChannel ask, byte[] buf, int len) {

		try {
			// debug(" recv (%d) ", len);

			m_receivedBytes += len;
			m_receivedPacketCount++;

			m_parser.append(buf, len);

			MqItem m = null;
			while ((m = m_parser.poll()) != null) {

				// debug("recv (%c) isAccept(%s) ", m.code , isAccept() );

				if (isAccept())
					doRecvInAccepter(m);
				else
					doRecv(m);

			} // while

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	@Override
	public void connected(MqChannel ask) {
		// debug("connected");

		try {

			m_parser = new MqPacket().setBufferSize(m_channel.socketBufferSize());
			m_channel = ask;// alloc
			m_jt.clear();
			m_bootstrap.put(this);
		} catch (Exception e) {

			e.printStackTrace();
			ask.close();
			return;
		}

		try {
			MqItem a = new MqItem(new byte[0]);
			sendAck((byte) 'l', a);// send 'link'
		} catch (Exception e) {

		}

		m_inter.connected(this);

	}

	/**
	 * 
	 */
	@Override
	public void disconnected(MqChannel ask) {

		if (isAccept()) {
			m_bootstrap.remove(this);
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
	 * @param re
	 * @return
	 * @throws Exception
	 */
	private boolean searchInRecentlyRecevied(MqItem re) {

		synchronized (m_receivedItemList) {
			for (int i = 0; i < m_receivedItemList.size(); i++) {

				MqItem lr = m_receivedItemList.get(i);

				if (lr.sendIdx == re.sendIdx && lr.when == re.when && lr.from.equals(re.from)) {
					debug("ignore received data [sidx(%s) from(%s) ", re.sendIdx, re.from);
					return true;
				}
			} // for(i

			//
			//
			MqItem newOne = new MqItem();
			newOne.sendIdx = re.sendIdx;
			newOne.when = re.when;
			newOne.from = re.from;
			newOne.to = re.to;

			//
			//
			m_receivedItemList.add(newOne);
			if (m_receivedItemList.size() > 64) {
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
	private MqItem searchInRecentlyReturns(MqItem s) {

		// debug("returned item.size=%d ", m_returnItemList.size());
		synchronized (m_returnItemList) {

			//
			for (int i = 0; i < m_returnItemList.size(); i++) {

				MqItem lr = m_returnItemList.get(i);

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
	private void putInReturnList(MqItem m) throws Exception {

		synchronized (m_returnItemList) {
			// remove data older than 60 secones
			int p = 0;
			for (int i = 0; i < m_returnItemList.size(); i++) {

				MqItem e = m_returnItemList.get(p);
				if ((System.currentTimeMillis() - e.when) > ((16 * 1000))) {
					m_returnItemList.remove(p);
				} else {
					p++;
				}
			} // for

			m_returnItemList.add(MqItem.fromBytes(m.toBytes()));
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
	private void doRecv(MqItem m) throws Exception {

		if (isAccept())
			return;

		m_recvTime.start();

		if (m.code == (byte) 's') {// receive normal data

			// debug( "dorecv (%s) (%s) ", getId(), m.to );

			// server(cli)->client
			if (FilenameUtils.wildcardMatch(getPath(), m.getTo()) == false)
				return;

			sendAck((byte) 'a', m);

			if (searchInRecentlyRecevied(m)) { // was it receive before?

				if ((m.option & MqItem.ASK_RETURN) == MqItem.ASK_RETURN) {// ask return?
					// already returned?
					MqItem r = searchInRecentlyReturns(m);
					if (r != null) {
						sendReturn(r.to, r);// send return
					}
				}
				return;
			}

			try {

				// prepare return item
				MqItem ret = new MqItem();
				ret.code = 'r';
				ret.sendIdx = (m_sendIndex++);
				ret.respIdx = m.sendIdx;// set response index
				ret.when = m.when;// copy when it sent
				ret.from = this.getPath();
				ret.to = m.from;// alloc sender name

				MqItem rs = m_inter.recv(this, m);

				if (rs != null && (m.option & MqItem.ASK_RETURN) == MqItem.ASK_RETURN) {

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

					MqProperties pr = m.getProperties();

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
			System.out.println("return error = " + s + " len=" + m.data.length);
			this.getChannel().close();
			throw new UserException(s);

		} else if (m.code == (byte) 'a') {// get ack

			checkResp(m);// send ok

		}

	}

	/**
	 * 
	 * 
	 * @param m
	 */
	private void doRecvInAccepter(MqItem m) {

		if (isAccept() == false)
			return;

		m_recvTime.start();

		if (m.code == (byte) 's') {// receive normal data

			sendAck((byte) 'a', m);

			if (searchInRecentlyRecevied(m))// received check
				return;

			if (m_path == null) {

				if (m_inter.putPath(_this, m.from)) {

					m_path = m.from;// copy name
					m_bootstrap.putPathOfClient(_this);
				} else {
					sendError(String.format("id(%s) is not allow", m.from));
					m_channel.close();
				}
			}

			MqItem rs = m_inter.recv(this, m);// recevied data check in interface.recv

			if (rs != null) {
				m_bootstrap.sendTo(this, m);// send to targets
			}

		} else if (m.code == (byte) 'r') {// get return value

			sendAck((byte) 'a', m);

			if (searchInRecentlyRecevied(m))// received check
				return;

			MqItem rs = m_inter.recv(this, m);
			if (rs != null) {
				m_bootstrap.sendTo(this, m);// send to targets
			}

		} else if (m.code == (byte) 'l') {// check link(ask)

			// debug("recv 'link' (%s)", m.from );
			if (m_path == null) {
				if (m_inter.putPath(_this, m.from)) {
					m_path = m.from;// copy name
					m_bootstrap.putPathOfClient(_this);
				} else {
					sendError(String.format("id(%s) is not allow", m.from));
					m_channel.close();// close
				}
			}
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
	private void checkResp(MqItem r) {

		synchronized (m_jt) {

			for (int h = 0; h < m_jt.size(); h++) {

				UserJob j = m_jt.get(h);

				if (j.m.sendIdx == r.respIdx && j.m.when == r.when && r.code == (byte) 'a') {
					m_jt.setResp(j, true);
					// debug("get ack (%s)isAccept(%s)", getId(), isAccept());
				}

			} // for(h

		} // sync

	}

	/**
	 * 
	 * 
	 * @param s
	 */
	public static void debug(String s, Object... args) {

		System.out.println("mqcliet [" + String.format(s, args));
	}

	/**
	 * 
	 * 
	 * 
	 */
	protected long doSendTask() {

		synchronized (m_jt) {

			// int rcnt=0;
			int end = 8;// (int) m_jt.size();
			int maxloop = 0;
			UserJob u = null;

			do {
				u = m_jt.autoPeek(end);

				if (u != null && isAlive()) {
					// debug("doSendTask");
					try {
						byte[] b = MqPacket.make(u.m);

						m_sentBytes += b.length;
						m_sendPacketCount++;
						m_channel.send(b);

						if (u.count > 0) {
							m_resendPacketCount++;
						}
						u.count++;

						m_sentCount++;
						// return true;
					} catch (Exception e) {

						e.printStackTrace();
					}

				}

				maxloop++;
			} while (u != null && maxloop < end);

			return m_sentCount;
		} // sync

	}

	/**
	 * 
	 * @param m
	 */
	public void send(String to, MqItem m) throws Exception {
		synchronized (m_jt) {

			m.from = m_path;
			m.code = 's';
			m.to = to;
			m.sendIdx = (m_sendIndex++);
			if (m_jt.add(new UserJob(m)) == false) {
				m_sendTaskFullErrorCount++;
				throw new UserException("mqclient[(%s)send queue is full (max:%s)", getPath(), m_jt.getMaxTask());
			}

		} // sync

		doSendTask();// send it immediately
	}

	/**
	 * 
	 * server(cli)->server(cli)
	 * 
	 * @param to
	 * @param m
	 */
	protected void sendDistribute(String to, MqItem m) {
		synchronized (m_jt) {

			if (m_jt.add(new UserJob(m)) == false) {
				m_sendTaskFullErrorCount++;
				debug("(%s)send distribute to(%s) queue is full (max:%s)", getPath(), to, m_jt.getMaxTask());
			}

		} // sync

		doSendTask();// send it immediately
	}

	protected void sendReturn(String to, MqItem m) throws Exception {
		synchronized (m_jt) {

			m.sendIdx = (m_sendIndex++);
			if (m_jt.add(new UserJob(m)) == false) {
				m_sendTaskFullErrorCount++;
				throw new UserException("mqclient[(%s)send return to(%s) queue is full (max:%s)", getPath(), to,
						m_jt.getMaxTask());
			}

		} // sync

		doSendTask();// send it immediately
	}

	/**
	 * 
	 * @param m
	 */
	private void sendAck(byte code, MqItem m) {

		if (code == (byte) 'l' && isAccept()) {// when line link send (from server)
			MqProperties p = new MqProperties();

			p.setInteger("to", (int) this.getReceiveTimeoutSec());// timeout
			p.setInteger("bs", (int) this.getSocketBufferSize());// buffer size
			p.setInteger("ri", (int) this.getReconnectableIntervalSec());// reconnect interval
			try {
				sendAck(code, m, p.toBytes());
			} catch (Exception e) {
				e.printStackTrace();
				sendAck(code, m, null);
			}
		} else {
			sendAck(code, m, null);
		}

	}

	/**
	 * 
	 * @param code
	 * @param m
	 * @param data
	 */
	private void sendAck(byte code, MqItem m, byte[] data) {

		MqItem a = new MqItem(new byte[0]);
		a.from = m_path;
		a.code = code;
		a.sendIdx = (m_sendIndex++);
		a.respIdx = m.sendIdx;
		a.when = m.when;
		if (data != null) {
			a.data = data;
		}

		try {

			byte[] b = MqPacket.make(a);

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
		sendAck((byte) 'e', new MqItem(), b);// return err
	}

	/**
	 * 
	 * @param to
	 * @param m
	 * @return
	 * @throws Exception
	 */
	public MqItem get(String to, MqItem m) throws Exception {
		return get(to, m, m_bootstrap.getWaitReturnTimeoutSec() * 1000);
	}

	/**
	 * 
	 * @param m
	 */
	public MqItem get(String to, MqItem m, double timeout_ms) throws Exception {

		m_returnItem = null;

		m.option |= (byte) 0x01;// ask return

		send(to, m);

		TimeUtil t = new TimeUtil();
		while (t.end_ms() < timeout_ms) {

			TimeUtil.sleep(10);

			synchronized (syncReturn) {

				if (m_returnItem != null) {
					return MqItem.fromBytes(m_returnItem.toBytes());
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
			sendAck((byte) 'l', new MqItem());// send 'link'
		}

		// time out check
		if (getLastReceivedTime() > rtimeout && rtimeout != 0) {

			debug("socket timeout!!! isAccept(%s) recvLastTime(%f/%f) getId(%s) ", _this.isAccept(),
					_this.getLastReceivedTime(), rtimeout, _this.getPath());

			m_recvTime.start();// timer initialize
			getChannel().close();
		}

	}

	public long getSendTaskBufferFullErrorCount() {
		return m_sendTaskFullErrorCount;
	}
}
