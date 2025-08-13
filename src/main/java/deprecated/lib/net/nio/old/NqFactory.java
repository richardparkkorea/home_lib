package deprecated.lib.net.nio.old;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import home.lib.io.FilenameUtils;
import home.lib.lang.Timer2;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

  public class NqFactory {

	////////////////////////////////////////////////////////////////////

	private NqFactory _this = this;
	private Map<NqConnector, String> m_clients = new HashMap<NqConnector, String>();

	private Map<String, ArrayList<NqConnector>> m_groups = new HashMap<String, ArrayList<NqConnector>>();

	private NqConnector[] m_clientArr = new NqConnector[0];/// for speed up & sync

	private Timer2 s_send = null;
	private Timer2 s_keepAlive = null;
	/**
	 * 
	 */
	private String m_name = "bootstrap";
	private double m_dReceiveTimeoutSec = 60;
	private double m_reconnectableInterval = 18.0;
	private long m_lSocketBufferSize = 1024 * 32;

	private double m_waitReturnTimeoutSec = 10;
	protected int m_jt_maxQueue = 64;
	protected long m_jt_resendMs = 0;
	protected long m_jt_timeoutMs = 0;
	// protected int m_jt_maxHit = 0;

	protected long m_sendPacketCountPerSec = 0;
	protected long m_sendBytesCountPerSec = 0;

	/**
	 * 
	 * 
	 * 
	 */
	public NqFactory(String name) {
		m_name = name;
		s_send = new Timer2().schedule(new sendTask(), 10, 10);
		s_keepAlive = new Timer2().schedule(new keepAliveTask(), 10, 10);

	}

	public String getName() {
		return m_name;
	}

	/**
	 * 
	 * @param c
	 */
	protected void put(NqConnector c) {
		synchronized (m_clients) {

			// debug("s_clients.containsKey(c) = " + s_clients.containsKey(c));
			if (m_clients.containsKey(c) == false) {

				m_clients.put(c, "");
				m_clientArr = m_clients.keySet().toArray(new NqConnector[0]);
				// m_clientsName.put( name, c);
			}
		} // sync
	}

	/**
	 * use it in MqClient
	 * 
	 * @param mc
	 */
	protected boolean putPathOfClient(NqConnector mc) {
		// synchronized (m_names) {
		//
		// if (mc.getPath() != null && mc.getPath().length() > 0) {
		// if (m_names.containsKey(mc.getPath()) == false) {
		//
		// m_names.put(mc.getPath(), mc);// add new
		//
		// return true;
		//
		// }
		// }
		//
		// return false;
		// }

		synchronized (m_clients) {

			m_clients.put(mc, mc.getPath());// set path
			m_clientArr = m_clients.keySet().toArray(new NqConnector[0]);

			// return true;

		}

		synchronized (m_groups) {
			String path = mc.getPath();

			if (m_groups.containsKey(path) == false) {
				m_groups.put(path, new ArrayList<NqConnector>());
			}

			m_groups.get(path).add(mc);
		} // sync

		debug("(%s) put path(%s)", m_name, mc.getPath());

		return true;
	}

	/**
	 * 
	 * @param c
	 */
	protected void remove(NqConnector c) {
		synchronized (m_clients) {

			// String n = c.getPath();

			m_clients.remove(c);

			m_clientArr = m_clients.keySet().toArray(new NqConnector[0]);

			// m_clientsName.remove(n);

		} /// sync

		//
		//
		synchronized (m_groups) {
			String path = c.getPath();

			if (m_groups.containsKey(path)) {
				m_groups.get(path).remove(c);
				if (m_groups.get(path).size() == 0) {
					m_groups.remove(path);
				}
			}

		} // sync

		debug("(%s)remove path(%s)", m_name, c.getPath());

		// synchronized (m_names) {
		//
		// String[] l = m_names.keySet().toArray(new String[0]);
		// for (String n : l) {
		// MqClient fc = m_names.get(n);
		// if (fc != null && fc == c) {
		// m_names.remove(n);
		// return;
		// }
		// } // for
		// } // sync

	}

	/**
	 * 
	 */
	protected void clear() {
		// synchronized (m_clients) {

		for (NqConnector c : m_clients.keySet()) {

			try {
				c.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

		} // for
		m_clients.clear();

		// } /// sync
	}

	/**
	 * 
	 * @return
	 */
	public int count() {
		return m_clients.size();
	}

	/**
	 * 
	 * @author richard
	 *
	 */
	class sendTask extends TimerTask {
		TimeUtil t = new TimeUtil();
		long count = 0;
		long bytes=0;

		public void run() {

			long ss = System.currentTimeMillis();
			// long old=m_resendPacketCount;
			// synchronized (m_clients) {
			// MqClient[] cl = null;
			// synchronized (m_clients) {
			// cl = m_clients.keySet().toArray(new MqClient[0]);
			// } // sync

			// synchronized (m_clients) {
			for (NqConnector c : m_clientArr) {

				try {
					
					long rs[]=c.doSendTask(); 
					count += rs[0];
					bytes +=rs[1];
					c.resetSentCount();

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// }
			// } // sync
			long ee = System.currentTimeMillis();
			//
			/// if (t.end_sec() > 1) {
			// t.start();

			if ((ee - ss) > 100) {// bigger than 100ms?
				debug("sendTask. send time=%d  ", (ee - ss));
			}
			// }

			if (t.end_ms() > 1000) {
				t.start();
				m_sendPacketCountPerSec = count;
				m_sendBytesCountPerSec=bytes;
				count = 0;
				bytes=0;
			}

		}
	}

	/**
	 * 
	 * @author richard
	 *
	 */
	class keepAliveTask extends TimerTask {

		TimeUtil t = new TimeUtil();

		public void run() {
			// synchronized (m_clients) {
			// MqClient[] cl = null;
			// synchronized (m_clients) {
			// cl = m_clients.keySet().toArray(new MqClient[0]);
			// } // sync

			//
			//
			for (NqConnector c : m_clientArr) {

				if (c.isAlive()) {
					c.doCheckLink();
				}
			} // for

			int nn = 0;
			if (t.end_sec() > 1) {
				t.start();

				// debug("chk reconn "+m_clients.keySet().size()+ " "+getName() );

				int cnt=0;
				for (NqConnector c : m_clientArr) {

					if (c.isAlive() == false && c.isAccepted() == false && c.isReconnectable()
							&& c.getKeepConnection()) {
						try {

							debug("(%s)reconnect~ %s ", getName(), c.getPath());

							c.reconnect();

							if((cnt++)>128)
								return;
							
						} catch (Exception e) {
							e.printStackTrace();
						}

						// return;
					}

				} // for
			} // if(1 sec
				// } // sync
		}
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

		long taskfullerr = 0;

		long alives = 0;

		long rsend = 0;

		long tasks = 0;

		long resendms=0;
		
		// MqClient[] cl = null;
		// synchronized (m_clients) {
		// cl = m_clients.keySet().toArray(new MqClient[0]);
		// } // sync

		for (NqConnector c : m_clientArr) {

			reconn += c.getReconnectCount();

			taskfullerr += c.getSendTaskBufferFullErrorCount();

			succeed += c.getJt().getSucceedCount();

			fail += c.getJt().getFailCount();

			sb += c.m_sentBytes;
			rb += c.m_receivedBytes;

			sp += c.m_sendPacketCount;
			rp += c.m_receivedPacketCount;

			rsend += c.m_resendPacketCount;

			tasks += c.getJt().size();

			if (c.isAlive())
				alives++;
			
			
			resendms+=c.getJt().getResendMs();

		}
		
		if( m_clientArr.length>0 && resendms>0) {
			resendms/=m_clientArr.length;
		}
		// } // sync

		String s = String.format(
				"NqFactory(%s) channels(%s),  alives(%s),  send bytes(%s), recv bytes(%s), send(%s) recv(%s)  send ok(%s), "
				+ " send fail(%s), reconnect(%s), queue err(%s),  resend(%s) queue(%s) send per sec(%s) bytes per sec(%s) jt(%s,%s,%s) resend (%s) ms ",
				getName(), m_clients.size(), alives, StringUtil.formatBytesSize(sb), StringUtil.formatBytesSize(rb),
				StringUtil.formatCount(sp), StringUtil.formatCount(rp), StringUtil.formatCount(succeed),
				StringUtil.formatCount(fail), StringUtil.formatCount(reconn), StringUtil.formatCount(taskfullerr),
				StringUtil.formatCount(rsend), tasks, m_sendPacketCountPerSec, StringUtil.formatBytesSize(m_sendBytesCountPerSec),

				m_jt_maxQueue, m_jt_resendMs, m_jt_timeoutMs , resendms

		);

		return s;
	}

	/**
	 * use wildCardMatch
	 * 
	 * @param m
	 */
	public void sendTo(NqConnector mc, NqItem m) {
		if (m.to == null)
			return;
		if (m.to.trim().length() == 0)
			return;
		if (m.code != (byte) 's' && m.code != (byte) 'r') // allow 'send' action
			return;

		// //
		// // sendto
		ArrayList<NqConnector> group = null;

		synchronized (m_groups) {
			group = m_groups.get(m.to);
		}

		if (group != null) {
			// debug("send to group=%s", m.to );
			try {
				for (NqConnector c : group) {
					c.sendDistribute(m.to, m);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}

		//
		// send wild

		int cnt = 0;
		long ss = System.nanoTime();
		// synchronized (m_clients) {
		for (NqConnector c : m_clientArr) {

			/// debug(" sendto id(%s) code(%c) from(%s)to(%s)", c.getId(), m.code, m.getFrom(), m.getTo() );

			if (mc.equals(c) == false) {// not itself

				if (m.to.equals("*") == false)
					System.out.println(m.to);

				if (m.to.equals("*") || FilenameUtils.wildcardMatch(c.getPath(), m.getTo())) {

					try {
						c.sendDistribute(m.to, m);
						cnt++;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			} // if

		} // for

		long ee = System.nanoTime();
		double t = ((double) (ee - ss) / 1000000);
		if (t > 100)
			debug("sendTo: spend time (%.3f)ms cnt=%d", t, cnt);

		// }

		// debug("sendto------------1");
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
	public NqFactory setSocketBufferSize(long s) {
		this.m_lSocketBufferSize = s;
		return this;
	}

	/**
	 * 
	 * @param d
	 * @return
	 */
	public NqFactory setReceiveTimeoutSec(double d) {
		m_dReceiveTimeoutSec = d;
		return this;
	}

	/**
	 * 
	 * @return
	 */
	public double getReceiveTimeoutSec() {
		return m_dReceiveTimeoutSec;
	}

	/**
	 * 
	 * 
	 * @param s
	 */
	public static void debug(String s, Object... args) {

		System.out.println("mqbootstrap [" + String.format(s, args));
	}

	/**
	 * 
	 * @return
	 */
	public double getWaitReturnTimeoutSec() {
		return m_waitReturnTimeoutSec;
	}

	/**
	 * 
	 * @param d
	 * @return
	 */
	public NqFactory setWaitReturnTimeoutSec(double d) {
		m_waitReturnTimeoutSec = d;
		return this;
	}

	/**
	 * 
	 * @param maxQueue
	 * @param resend
	 * @param timeout
	 * @return
	 */

	public NqFactory jobOpt(int maxQueue, long resend, long timeout) {

		m_jt_maxQueue = maxQueue;
		m_jt_resendMs = resend;
		m_jt_timeoutMs = timeout;

		// MqClient[] cl = null;
		// synchronized (m_clients) {
		// cl = m_clients.keySet().toArray(new MqClient[0]);
		// } // sync

		for (NqConnector c : m_clientArr) {

			c.jobOpt(m_jt_maxQueue, m_jt_resendMs, m_jt_timeoutMs);

		}

		return this;
	}

	// /**
	// *
	// *
	// * @param id
	// * @return
	// */
	// public boolean isExist(String id) {
	// if (id == null || id.trim().length() == 0)
	// return false;
	//
	// // MqClient[] cl = null;
	// // synchronized (m_clients) {
	// // cl = m_clients.keySet().toArray(new MqClient[0]);
	// // } // sync
	//
	// // for (MqClient c : m_clientArr) {
	// //
	// // if (c.getId() != null && c.getId().trim().equals(id.trim())) {
	// // return true;
	// // }
	// //
	// // }
	//
	// // return false;
	// synchronized (m_names) {
	// return m_names.containsKey(id);
	// } // sync
	// }

	public boolean close(String path) {
		if (path == null || path.trim().length() == 0)
			return false;

		debug("(%s) close(%s)", m_name, path);
		// MqClient[] cl = null;
		// synchronized (m_clients) {
		// cl = m_clients.keySet().toArray(new MqClient[0]);
		// } // sync

		// for (MqClient c : m_clientArr) {
		//
		// if (c.getId() != null && c.getId().trim().equals(id.trim())) {
		// c.close();
		// return true;
		// }
		//
		// }

		synchronized (m_clients) {

			for (NqConnector c : m_clientArr) {
				if (c.getPath().equals(path)) {
					c.close();
				}
			} // for

		} // sync

		// MqClient mc = m_names.get(id);
		// if (mc != null) {
		// mc.close();
		// return true;
		// }

		return false;

	}

	/**
	 * 
	 * @param idFilter
	 * @return
	 */
	public NqConnector[] getClients(String idFilter) {

		synchronized (m_clients) {

			ArrayList<NqConnector> arr = new ArrayList<NqConnector>();
			for (NqConnector c : m_clients.keySet()) {

				if (idFilter.equals("*") || FilenameUtils.wildcardMatch(c.getPath(), idFilter)) {
					arr.add(c);

				}

			} // for
			return arr.toArray(new NqConnector[0]);
		} // sync

	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 */
	public String[] getPathInfo() {

		ArrayList<String> arr = new ArrayList<String>();

		for (String s : m_groups.keySet()) {

			long queue = 0;
			long succeed = 0;
			long fail = 0;

			long sb = 0, rb = 0, sp = 0, rp = 0, rsend = 0;

			for (NqConnector c : m_groups.get(s)) {

				queue += c.getJt().size();
				succeed += c.getJt().getSucceedCount();
				fail += c.getJt().getFailCount();

				sb += c.m_sentBytes;
				rb += c.m_receivedBytes;

				sp += c.m_sendPacketCount;
				rp += c.m_receivedPacketCount;

				rsend += c.m_resendPacketCount;

			}
			;

			String r = String.format("%s| link(%s) queue(%s) succeed(%s) fail(%s) send(%s) recv(%s) resend(%s) ", s,
					m_groups.get(s).size(), StringUtil.formatCount(queue), StringUtil.formatCount(succeed),
					StringUtil.formatCount(fail),
					// StringUtil.formatCount(sb),
					// StringUtil.formatCount(rb),
					StringUtil.formatCount(sp), StringUtil.formatCount(rp), StringUtil.formatCount(rsend)

			);

			arr.add(r);
		}

		return arr.toArray(new String[arr.size()]);
	}

	/**
	 * 
	 * @param sec
	 * @return
	 */
	public NqFactory setReconnectableIntervalSec(double sec) {
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
	 * 
	 * 
	 * @param ip
	 * @param port
	 * @param listen
	 * @return
	 * @throws Exception
	 */
	public NqServer createServer(String ip, int port, NqServerListener listen) throws Exception {

		NqServer svr = new NqServer(this);
		svr.select(ip, port, listen);
		return svr;

	}

	/**
	 * 
	 * @param ip
	 * @param port
	 * @param listen
	 * @return
	 * @throws Exception
	 */
	public NqConnector createConnector(String ip, int port, NqConnectorListener listen) throws Exception {

		NqConnector con = new NqConnector(this);
		con.connect(ip, port, listen);
		return con;

	}

}
