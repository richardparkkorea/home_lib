package org.xcopy.net;

import java.nio.channels.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import home.lib.log.ILogger;
import home.lib.log.ULogger;
import home.lib.net.tms.TmsBroker;
import home.lib.net.tms.TmsChannel;
import home.lib.net.tms.TmsChannelInterface;
import home.lib.net.tms.TmsEvent;
import home.lib.net.tms.TmsItem;
import home.lib.util.TimeUtil;


public class MyBroker implements ILogger {

	static {

	}

	MyBroker _this = this;
	// NqFactory m_bs = new NqFactory("mqbroker wrap(svr bs)");
	int m_port = -1;
	IMyBrokerListener m_inter = null;
	// NqServer m_svr = null;
	// boolean m_useSelector = true;
	String m_bindIp = "0.0.0.0";

	TmsBroker m_brk = null;

	private ThreadPoolExecutor m_executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

	// Map<NqConnector, String> m_cli = new HashMap<NqConnector, String>();

	// public NqFactory svr_bs() {
	// return m_bs;
	// }
	//
	// public void setMaxReceivableSize(long n) {
	//
	// m_bs.setSocketBufferSize(n);
	// }

	public void setActionListener(IMyBrokerListener l) {
		m_inter = l;
	}

	public MyBroker(int port, IMyBrokerListener l) throws Exception {

		this("0.0.0.0", port, l, true);
	}

	public MyBroker(String bindIp, int port, IMyBrokerListener l, boolean useSelector) throws Exception {

		m_bindIp = bindIp;
		// m_useSelector = useSelector;

		// m_bs.jobOpt(64, 0, 0);
		//
		// m_bs.setSocketBufferSize(1024 * 32);
		// m_bs.setReceiveTimeoutSec(60);
		// m_bs.setWaitReturnTimeoutSec(12);

		m_inter = l;
		m_port = port;

		// m_svr = new NqServer(m_bs);

	}

	//public Future<Object> start(int startTimeOutSec) throws Exception {
	
	public void start(int startTimeOutSec) throws Exception {
		
		
		
		
		

		m_brk = new TmsBroker("local", m_bindIp, m_port,this,
				new TmsChannelInterface() {

			@Override
			public TmsItem onReceived(TmsChannel ch, byte[] rxd, TmsItem item) {

				return item;
			}

			@Override
			public void onDisconnected(TmsChannel ch) {

				
			}

			@Override
			public Object onAccepteded(TmsChannel ch) {
 
				ch.setLoggedIn(true);
				return null;
			}

			@Override
			public boolean onConnected(TmsChannel channel) {

				return true;
			}

			@Override
			public void onError(TmsChannel ch, byte[] err) {
				
				try {
					debug(ULogger.VERBOSE,"error=%s", new String(err));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				
			}

			@Override
			public void onLink(TmsChannel ch) {
				 

				debug(ULogger.VERBOSE,"addClientId(%s)  ", ch.getPath() );

				if (m_inter.addClientId(_this, ch.getPath(), ch.getIdName(), ch.getPwd(), ch.getChannel())) {
					ch.setLoggedIn(true);

				} else {
					ch.close();
				}
				
				
			}
			
		}
		
				);
		m_brk.jobOpt(8, 0);
		m_brk.jobPeekCount(8);
		m_brk.setReceiveTimeoutSec(60);
		m_brk.setSocketBufferSize(1024*1024*10);
		m_brk.waitBind(startTimeOutSec);
	
		
		

//		Callable<Object> task = new Callable<Object>() {
//
//			@Override
//			public Object call() throws Exception {
//				
//			
//				while (m_brk.isAlive()) {
//					
//					
//					if( m_brk.getEventCount()==0) {
//						TimeUtil.sleep(1);
//						continue;
//					}
//					
//					
//
//					TmsEvent e;
//					while ((e = m_brk.pollEvent()) != null) {
//
//						debug(ULogger.VERBOSE,"brk(p:%s)  evt=%s path(%s) ", m_brk.getPort(),   e.event , e.ch.getPath() );
//
//						try {
//
//							// if (e.ch.getUserObject() instanceof MySocket) {
//							// MySocket cl = (MySocket) e.ch.getUserObject();
//							// cl.addEvent(e.ch, e.event, e.rxd, e.item);
//							// }
//							//
//							// else
//
//							if (e.event == 'l') {
//								
//								debug(ULogger.VERBOSE,"addClientId(%s)  ", e.ch.getPath() );
//
//								if (m_inter.addClientId(_this, e.ch.getPath(), e.ch.getIdName(), e.ch.getPwd(), e.ch.getChannel())) {
//									e.ch.setLoggedIn(true);
//
//								} else {
//									e.ch.close();
//								}
//
//							} else if (e.event == 'a') {
//
//								e.ch.setLoggedIn(true);
//								// 
//								// e.ch.getChannel());
//								// if (rs != null) {
//								// e.ch.setUserObject(rs);
//								// }
//
//							} else if (e.event == 'e') {
//								try {
//									debug(ULogger.VERBOSE,"error=%s", new String(e.rxd));
//								} catch (Exception ex) {
//									ex.printStackTrace();
//								}
//
//							} else {
//								
//							 
//
//								debug(ULogger.VERBOSE,"event= %s   ", e.event  );
//							}
//
//						} catch (Exception exx) {
//							exx.printStackTrace();
//						}
//					} // while( pollevent
//
//				} // while(alive
//				
//				
//				
//				
//
//				return true;
//			}
//		};
//		
//		
//	
//		//LogCommon.fl("mybroker.start");
//
//
//		Future<Object> res = m_executor.submit(task);
//	
////		
//		
//		return res;

		// return this;
	}

	public void close() {

		// m_svr.close();
		if (m_brk != null) {
			
			
		//	LogCommon.fl("m_brk.cleanUp();");
			m_brk.cleanUp();
		}
	}

	public int getChannelCount() {

		// return m_bs.getClients("*").length;

		return m_brk.count();

	}

	//
	public String getNameFromChannel(SocketChannel ch) {

		// synchronized (m_cli) {
		// for (NqConnector c : m_cli.keySet()) {
		//
		// if (c.getChannel().getSocket() == ch)
		// return c.getPath();
		//
		// } // for
		// } // sync
		//
		// return null;

		TmsChannel tc = m_brk.get(ch);
		if (tc != null)
			return tc.getPath();

		return null;

	}

	public String[] getChannelNames() {

		return getChannelNames(null);
	}

	//
	//
	public void channelClose(SocketChannel ch) {

		m_brk.close(ch);

		// m_svr.channelClose(ch);
		// try {
		// ch.close();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

	}

	// /**
	// *
	// * return channels
	// *
	// * @param who
	// * -wildcardmatch<br>
	// * null' - return all channels
	// */
	public SocketChannel[] getChannels(String who) {

		return m_brk.getChannels(who, true);

	}

	//
	// /**
	// *
	// * @param who-
	// * if this is null, means return all names
	// * @return
	// */
	public String[] getChannelNames(String who) {

		return m_brk.getChannelNames(who, true);

	}

	public boolean isAlive() {
		return m_brk.isAlive();
	}

	// public void send(SocketChannel socket, byte[] data) {
	//
	// }

	public void setChannelTimeoutSec(long sec) {
		m_brk.setReceiveTimeoutSec(sec);
	}

	/**
	 * 
	 * @param fmt
	 * @param args
	 */
	public void debug(int lvl, String fmt, Object... args) {

		// m_svr.debug(fmt, args);

		if (m_brk != null) {
			m_brk.debug(lvl,fmt, args);
		}
	}

	/**
	 * 
	 * 
	 * 
	 */
	@Override
	public String toString() {
		String s = "mytcpbroker wrap+" + m_brk.toString();

		// s += "<br>";
		// s += m_svr.toString();

		// s+="<br>";
		// s+= NqChannel.selctor();

		return s;
	}

	public void setLogger(ILogger handle) {

		m_brk.setLogger(handle);

	}

	public TmsBroker getBroker() {
		return m_brk;
	}

	public void setMaxReceivableSize(long bufferSize) {

		m_brk.setSocketBufferSize(bufferSize);
	}

	public TmsBroker svr_bs() {
		return m_brk;
	}

	public void setChannelTimeout(long to) {
		m_brk.setReceiveTimeoutSec(to);

	}

	public long getSocketBufferSize() {
		return m_brk.getSocketBufferSize();
	}

	public void jobOpt(int maxQueue, int resend, int timeout) {

		m_brk.jobOpt(maxQueue, timeout);

	}

	public void jobPeekCount(int i) {
		m_brk.jobPeekCount(i);

	}

	public void setReconnectableIntervalSec(int i) {
		m_brk.setReceiveTimeoutSec(i);

	}

	public void setSocketBufferSize(int i) {
		m_brk.setSocketBufferSize(i);

	}

	@Deprecated
	public String[] getPathInfo() {

		return getChannelNames();
	}

	


	@Override
	public int e(Exception e) {
		
		
		//LogCommon.fl("%s", UserException.getStackTrace(e));
		
		e.printStackTrace();
		return 0;
	}

	@Override
	public int e(int arg0, String arg1, Exception e) {
		
		
		//LogCommon.fl("%s", UserException.getStackTrace(e));
		e.printStackTrace();
		return 0;
	}

	@Override
	public int l(String arg0, Object... arg1) {
		
		System.out.println(String.format(arg0,arg1));
		return 0;
	}

	@Override
	public int l(int level, String arg1, String arg2, Object... arg3) {
		
//		if(level==ULogger.DEBUG ) {
//			LogCommon.fl(arg1+arg2,arg3);
//		}
		
		System.out.println(String.format(arg2,arg3));
		return 0;
	}
}