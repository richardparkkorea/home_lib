package deprecated.lib.net.mq_old;

import java.nio.channels.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import deprecated.lib.net.mq2.dev_old.MqBootstrap;
import deprecated.lib.net.mq2.dev_old.MqClient;
import deprecated.lib.net.mq2.dev_old.MqItem;
import deprecated.lib.net.mq2.dev_old.MqServer;
import deprecated.lib.net.mq2.dev_old.MqServerInterface;
import home.lib.io.FilenameUtils;

public class MqBroker implements MqServerInterface {

	static {

	}

	MqBroker _this = this;
	MqBootstrap m_bs = new MqBootstrap("mqbroker wrap(svr bs)");
	int m_port = -1;
	MqBrokerListener m_inter = null;
	MqServer m_svr = null;
	boolean m_useSelector = true;
	String m_bindIp="0.0.0.0";

	Map<MqClient, String> m_cli = new HashMap<MqClient, String>();

	public MqBootstrap svr_bs() {
		return m_bs;
	}

	public void setMaxReceivableSize(long n) {

		m_bs.setSocketBufferSize(n);
	}

	public void setActionListener(MqBrokerListener l) {
		m_inter = l;
	}

	public MqBroker(int port, MqBrokerListener l) throws Exception {

 
 		this( "0.0.0.0", port,l,true );
	}
	public MqBroker(String bindIp, int port, MqBrokerListener l, boolean useSelector) throws Exception {

		m_bindIp=bindIp;
		m_useSelector = useSelector;

		m_bs.jobOpt(64, 0, 0);

		m_bs.setSocketBufferSize(1024 * 32);
		m_bs.setReceiveTimeoutSec(60);
		m_bs.setWaitReturnTimeoutSec(12);

		m_inter = l;
		m_port = port;
		

		m_svr = new MqServer(m_bs);

	}

	

	public void start() {

		try {
			//

			if (m_useSelector) {
				m_svr.select(m_bindIp, m_port, this).waitBind(6);// thread-pool use
			} else {
				m_svr.bind(m_bindIp, m_port, this).waitBind(6);// thread-pool use
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			for (MqClient c : m_cli.keySet()) {

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
			for (MqClient c : m_cli.keySet()) {

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
			for (MqClient c : m_cli.keySet()) {

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

		m_svr.debug(fmt, args);
	}

	/**
	 * 
	 * 
	 * 
	 */
	@Override
	public String toString() {
		return "mqbroker wrap+" + m_bs.toString();
	}

	@Override
	public MqItem recv(MqServer svr, MqClient ask, MqItem m) {

		return m;
	}

	@Override
	public void connected(MqServer svr, MqClient ask) {

		// TODO Auto-generated method stub
		m_inter.addChannel(_this, ask.getChannel().getSocket());

	}

	@Override
	public void disconnected(MqServer svr, MqClient ask) {

		synchronized (m_cli) {
			m_cli.remove(ask);
		}

		m_inter.removeChannel(this, ask.getChannel().getSocket());

	}

	@Override
	public void sendSucceeded(MqServer svr, MqClient ask, MqItem e) {

	}

	@Override
	public void sendFailed(MqServer svr, MqClient ask, MqItem e) {

	}

	@Override
	public boolean putPath(MqClient ask, String name) {

		if (m_inter.addClientId(_this, name, "", ask.getChannel().getSocket()) == false)
			return false;

		synchronized (m_cli) {
			m_cli.put(ask, name);
		}

		return true;
	}

}