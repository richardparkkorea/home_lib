package deprecated.lib.net.nio.old;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import home.lib.io.FilenameUtils;
import home.lib.io.JobTask;
import home.lib.io.JobTaskInterface;
import home.lib.lang.UserException;
import home.lib.util.AverageUtil;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

final public class NqConnector implements NqChannelListener, JobTaskInterface {

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
	private NqFactory m_bootstrap = null;
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

	private double m_resp_average_ms = 300;// ms

	// private MqSelector m_selector=null;

	/**
	 * 
	 */
	public NqConnector(NqFactory bs) {

		m_bootstrap = bs;

		// m_selector = bs.getSelector();
		m_channel = new NqChannel();

		m_jt.setMaxTask(bs.m_jt_maxQueue);
		m_jt.resendMs(bs.m_jt_resendMs);
		m_jt.TimeoutMs(bs.m_jt_timeoutMs);

		this.setReceiveTimeoutSec(m_bootstrap.getReceiveTimeoutSec());
		this.setReconnectableIntervalSec(m_bootstrap.getReconnectableIntervalSec());

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

		if (isAccepted()) {
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
	public NqConnector connect(final String ip, final int port, final INioConnectorListener l) throws Exception {

		if (isAlive()) {
			throw new UserException("channel is alive");
		}
		if (isAccepted()) {
			throw new UserException("accept socket has not permission");
		}

		debug("connect ");

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
	public NqConnector reconnect() throws Exception {

		if (isAlive())
			return this;

		if (isAccepted()) {
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

		if (m_channel.isConnecting()) {
			m_reconnectionInterval.start();
			debug("still connecting");
			return false;
		}

		if (m_reconnectionInterval.end_sec() < this.getReconnectableIntervalSec())
			return false;

		debug("%s isReconnectable %s < %s (%s)", TimeUtil.now(), m_reconnectionInterval.end_sec(),
				this.getReconnectableIntervalSec(), getPath() );

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

		ch.socket().setSendBufferSize((int) m_bootstrap.getSocketBufferSize());
		ch.socket().setReceiveBufferSize((int) m_bootstrap.getSocketBufferSize());

		m_inter = l;
		m_channel.setIpBind(m_bind).setSocketBufferSize(m_bootstrap.getSocketBufferSize()).accepted(ms, ch, this);

		// try {

		m_parser = new NqPacket().setBufferSize(m_channel.socketBufferSize());

		// MqChannel mch = new MqChannel();

		// m_channel.connect(sel, ch, this);

		// m_channel = mch;// alloc

		m_jt.clear();
		m_bootstrap.put(this);

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
	public boolean isAccepted() {
		return m_channel.isAccept();
	}

	/**
	 * 
	 * @return
	 */
	public boolean isConnected() {
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

		try {

			TimeUtil t=new TimeUtil();
			
			// debug(" recv (%d) ", len);
			if(m_parser==null || buf==null ) {
				debug("parser(%s) or buf(%s) error",m_parser,buf );
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
				if (isAccepted()) {

//					if (m_channel.selector().userage() > 90) {
//						if (m.code == 's') {// send packets ignore by overload
//							debug("[%s]  ignore a packet. server is busy, userage(%s)%%",getPath(),m_channel.selector().userage() );
//							continue;
//						}
//					}

					doRecvInAccepter(m);

				} else {
					doRecv(m);
				}
				
				
				if( t.end_ms()>100) {
					debug("%s doRecv receive action use %s ms ",getPath(),  t.end_ms() );
				}

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

			// set buffer size
			ch.socket().setSendBufferSize((int) m_bootstrap.getSocketBufferSize());
			ch.socket().setReceiveBufferSize((int) m_bootstrap.getSocketBufferSize());

			m_parser = new NqPacket().setBufferSize(m_channel.socketBufferSize());

			// MqChannel mch = new MqChannel();

			// m_channel.connected(MqChannel.connect_selector(), ch, this);

			// m_channel = mch;// alloc

			m_jt.clear();
			m_bootstrap.put(this);

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

		m_reconnectionInterval.start();

		// accept or keep == false
		if (isAccepted() || (getKeepConnection() == false)) {
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

		if (isAccepted())
			return;

		m_recvTime.start();

		// debug("recv (%s)", getPath());

		if (m.code == (byte) 's') {// receive normal data

			// debug( "dorecv (%s) (%s) ", getId(), m.to );

			// server(cli)->client
			if (FilenameUtils.wildcardMatch(getPath(), m.getTo()) == false)
				return;

			sendAck((byte) 'a', m);

			if (searchInRecentlyRecevied(m)) { // was it receive before?

				if ((m.option & NqItem.ASK_RETURN) == NqItem.ASK_RETURN) {// ask return?
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

				NqItem rs = m_inter.received(this, m, m.data);

				if (rs != null && (m.option & NqItem.ASK_RETURN) == NqItem.ASK_RETURN) {

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
			System.out.println("return error = " + s + " len=" + m.data.length);
			this.getChannel().close();
			throw new UserException(s);

		} else if (m.code == (byte) 'a') {// get ack

			// debug("redv (%s)", m.sendIdx );
			checkResp(m);// send ok

		}

	}

	/**
	 * 
	 * 
	 * @param m
	 */
	private void doRecvInAccepter(NqItem m) {

		if (isAccepted() == false)
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

			NqItem rs = m_inter.received(this, m, m.data);// recevied data check in interface.recv

			if (rs != null) {
				m_bootstrap.sendTo(this, m);// send to targets
			}

		} else if (m.code == (byte) 'r') {// get return value

			sendAck((byte) 'a', m);

			if (searchInRecentlyRecevied(m))// received check
				return;

			NqItem rs = m_inter.received(this, m, m.data);
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
			sendAck((byte) 'l', new NqItem());// reply 'link' command

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
	private void checkResp(NqItem r) {

		synchronized (m_jt) {

			for (int h = 0; h < m_jt.size(); h++) {

				UserJob j = m_jt.get(h);

				if (j.m.sendIdx == r.respIdx && j.m.when == r.when && r.code == (byte) 'a') {
					m_jt.setResp(j, true);
					// debug("get ack (%s)isAccept(%s)", getId(), isAccept());

					getAutoResendMs(j);

				}

			} // for(h

		} // sync

	}

	/**
	 * 
	 * @param j
	 */
	private void getAutoResendMs(UserJob j) {
		m_ack.sum(System.nanoTime() - j.send_time);

		if (m_ack.isTimeover(6000)) {

			double l = (double) m_ack.getLong() * 1.4;

			double d = (long) (((double) l / 1000000));// min 100ms ~

			m_resp_average_ms = d;

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
	public static void debug(String s, Object... args) {

		System.out.println(TimeUtil.now() + " mqconnector [" + String.format(s, args));
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
				u = m_jt.autoPeek(end);

				if (u != null && isAlive()) {
					// debug("doSendTask");

					u.set_send_time();

					try {
						byte[] b = NqPacket.make(u.m);

						m_sentBytes += b.length;
						m_sendPacketCount++;
						m_channel.send(b);

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
	 * @param m
	 */
	public void send(String to, NqItem m) throws Exception {

		// if the ACK response is slow then the server is overloaded
		double delay = (m_resp_average_ms - 300);
		if (delay > 0) {
			TimeUtil.sleep((int) delay);
			debug("send.give a delay %s ", delay);
		}

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
	protected void sendDistribute(String to, NqItem m) {
		synchronized (m_jt) {

			if (m_jt.add(new UserJob(m)) == false) {
				m_sendTaskFullErrorCount++;
				// debug("(%s)send distribute to(%s) queue is full (max:%s)", getPath(), to, m_jt.getMaxTask());
			}

		} // sync

		doSendTask();// send it immediately
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
	private void sendAck(byte code, NqItem m) {

		if (code == (byte) 'l' && isAccepted()) {// when line link send (from server)
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
	 * @param m
	 * @return
	 * @throws Exception
	 */
	public NqItem get(String to, NqItem m) throws Exception {
		return get(to, m, m_bootstrap.getWaitReturnTimeoutSec() * 1000);
	}

	/**
	 * 
	 * @param m
	 */
	public NqItem get(String to, NqItem m, double timeout_ms) throws Exception {

		m_returnItem = null;

		m.option |= (byte) 0x01;// ask return

		send(to, m);

		TimeUtil t = new TimeUtil();
		while (t.end_ms() < timeout_ms) {

			TimeUtil.sleep(10);

			synchronized (syncReturn) {

				if (m_returnItem != null) {
					return NqItem.fromBytes(m_returnItem.toBytes());
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

			if (isConnected()) {

				sendAck((byte) 'l', new NqItem());// send 'link'
			}
		}

		// time out check
		if (getLastReceivedTime() > rtimeout && rtimeout != 0) {

			debug("socket timeout!!! isAccept(%s) recvLastTime(%f/%f) getId(%s) ", _this.isAccepted(),
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

	@Override
	public Object _lock() {
		return m_channel._lock();
	}
	
	
	
	 
	
	
}
