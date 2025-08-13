package home.lib.net.mq;

import java.nio.channels.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import home.lib.io.FilenameUtils;
import home.lib.lang.UserException;
import home.lib.net.nio.NqFactory;
import home.lib.net.nio.NqChannel;
import home.lib.net.nio.NqConnector;
import home.lib.net.nio.NqItem;
import home.lib.net.nio.NqServer;
import home.lib.net.nio.NqServerListener;

public class MqBroker extends NqServerListener {

	static {

	}

	MqBroker _this = this;
	NqFactory m_bs = new NqFactory("mqbroker wrap(svr bs)");
	int m_port = -1;
	MqBrokerListener m_inter = null;
	NqServer m_svr = null;
	boolean m_useSelector = true;
	String m_bindIp = "0.0.0.0";

	Map<NqConnector, String> m_cli = new HashMap<NqConnector, String>();

	public NqFactory svr_bs() {
		return m_bs;
	}

	public void setMaxReceivableSize(long n) {

		m_bs.setSocketBufferSize(n);
	}

	public void setActionListener(MqBrokerListener l) {
		m_inter = l;
	}

	public MqBroker(int port, MqBrokerListener l) throws Exception {

		this("0.0.0.0", port, l, true);
	}

	public MqBroker(String bindIp, int port, MqBrokerListener l, boolean useSelector) throws Exception {

		m_bindIp = bindIp;
		m_useSelector = useSelector;

		m_bs.jobOpt(64, 0, 0);

		m_bs.setSocketBufferSize(1024 * 32);
		m_bs.setReceiveTimeoutSec(60);
		m_bs.setWaitReturnTimeoutSec(12);

		m_inter = l;
		m_port = port;

		m_svr = new NqServer(m_bs);

	}

	public NqServer start(int startTimeOutSec) throws Exception {

		// try {
		//

		// if (m_useSelector) {
		m_svr.select(m_bindIp, m_port, this).waitBind(startTimeOutSec);// thread-pool use
		
		if(startTimeOutSec>0 && m_svr.isAlive()==false) {
			throw new UserException( "mqbroker failed to start");
			
		}
		// } else {
		// m_svr.bind(m_bindIp, m_port, this).waitBind(6);// thread-pool use
		// }

		// } catch (Exception e) {
		// TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		return m_svr;
	}

	public void close() {

		m_svr.close();
	}

	public int getChannelCount() {

		return m_bs.getClients("*").length;

	}

	//
	public String getNameFromChannel(SocketChannel ch) {

		synchronized (m_cli) {
			for (NqConnector c : m_cli.keySet()) {

				if (c.getChannel().getSocket() == ch)
					return c.getPath();

			} // for
		} // sync

		return null;

	}

	public String[] getChannelNames() {

		return getChannelNames(null);
	}

	//
	//
	public void channelClose(SocketChannel ch) {

		m_svr.channelClose(ch);
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

		ArrayList<SocketChannel> arr = new ArrayList<SocketChannel>();

		synchronized (m_cli) {
			for (NqConnector c : m_cli.keySet()) {

				if (who == null || FilenameUtils.wildcardMatch(c.getPath(), who)) {

					arr.add(c.getChannel().getSocket());

				}

			} // for
		} // sync

		return arr.toArray(new SocketChannel[arr.size()]);

	}

	//
	// /**
	// *
	// * @param who-
	// * if this is null, means return all names
	// * @return
	// */
	public String[] getChannelNames(String who) {

		ArrayList<String> arr = new ArrayList<String>();

		synchronized (m_cli) {
			for (NqConnector c : m_cli.keySet()) {

				if (who == null || FilenameUtils.wildcardMatch(c.getPath(), who)) {

					arr.add(c.getPath());

				}

			} // for
		} // sync

		return arr.toArray(new String[arr.size()]);

	}

	public boolean isAlive() {
		return m_svr.isAlive();
	}

	// public void send(SocketChannel socket, byte[] data) {
	//
	// }

	public void setChannelTimeout(long l) {
		m_bs.setReceiveTimeoutSec(l / 1000);
	}

	/**
	 * 
	 * @param fmt
	 * @param args
	 */
	public void debug(String fmt, Object... args) {

		//m_svr.debug(fmt, args);
		
		if(m_bs!=null) {
			m_bs.debug(this, fmt, args);
		}
	}

	/**
	 * 
	 * 
	 * 
	 */
	@Override
	public String toString() {
		String s = "mqbroker wrap+" + m_bs.toString();

		s += "<br>";
		s += m_svr.toString();

		// s+="<br>";
		// s+= NqChannel.selctor();

		return s;
	}

	@Override
	public NqItem recv(NqServer svr, NqConnector ask, NqItem m, byte[] dataOfItem) {

		return m;
	}

	@Override
	public void connected(NqServer svr, NqConnector ask) {

		// TODO Auto-generated method stub
		m_inter.addChannel(_this, ask.getChannel().getSocket());

	}

	@Override
	public void disconnected(NqServer svr, NqConnector ask) {

		synchronized (m_cli) {
			m_cli.remove(ask);
		}

		m_inter.removeChannel(this, ask.getChannel().getSocket());

	}

	@Override
	public void sendSucceeded(NqServer svr, NqConnector ask, NqItem e) {

	}

	@Override
	public void sendFailed(NqServer svr, NqConnector ask, NqItem e) {

	}

	@Override
	public boolean putPath(NqConnector ask, String name) {

		if (m_inter.addClientId(_this, name, "", ask.getChannel().getSocket()) == false)
			return false;

		synchronized (m_cli) {
			m_cli.put(ask, name);
		}

		return true;
	}

	
	@Override
	public boolean login(NqServer svr,NqConnector nqConnector, String id, String pwd) {
 
		return m_inter.login(this,svr,nqConnector,id,pwd);
	}

	
	@Override
	public void returnError(NqServer svr, NqConnector con, UserException e) {
		  m_inter.returnError(this, svr,con,e);
	}
	
	
}