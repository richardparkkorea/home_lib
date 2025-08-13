package deprecated.lib.net.mq_old;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;

import deprecated.lib.net.mq2.dev_old.MqChannel;
import deprecated.lib.net.mq2.dev_old.MqChannelInterface;

public class ASocketChannel implements MqChannelInterface {

	MqChannel m_ch = new MqChannel();
	ASocketChannelInterface m_inter = null;

	public boolean isConnecting() {
		return false;

	}

	public Object getUserObject() {
		return m_ch.getUserObject();
	}

	public void setUserObject(Object o) {
		m_ch.setUserObject(o);
	}

	public ASocketChannel(String ip, int port, ASocketChannelInterface l) throws Exception {

		this(ip, port, l, 1024 * 8, 3000, null, null);

	}

	public ASocketChannel(String ip, int port, ASocketChannelInterface l, int rxTxBufLen, long connectionTimeout)
			throws Exception {
		this(ip, port, l, rxTxBufLen, connectionTimeout, null, null);
	}

	public ASocketChannel(String ip, int port, ASocketChannelInterface l, int rxTxBufLen, long connectionTimeout,
			InetSocketAddress bind, Object uo) throws Exception {

		//
		m_inter = l;
		m_ch.setUserObject(uo).setIpBind(bind);
		m_ch.setSocketBufferSize(rxTxBufLen).connect(ip, port, this).waitConnect(connectionTimeout / 1000);

	}

	public ASocketChannel(AsynchronousSocketChannel ch, ASocketChannelInterface l, int rxTxBufLen) throws Exception {

		m_inter = l;
		m_ch.setSocketBufferSize(rxTxBufLen).accept(ch, this);

	}

	public ASocketChannel(final SocketAddress ad, ASocketChannelInterface l, final int rxTxBufLen,
			final InetSocketAddress bind, Object uo) throws Exception {

		InetSocketAddress addr = (InetSocketAddress) ad;
		// ((InetSocketAddress) address).getPort();

		InetAddress inaddr = addr.getAddress();

		Inet4Address in4addr = (Inet4Address) inaddr;
		byte[] ip4bytes = in4addr.getAddress(); // returns byte[4]
		String ip = in4addr.toString().substring(1);

		int port = addr.getPort();

		System.out.format(" lan2  (%s:%d) \r\n", ip, port);

		m_inter = l;

		m_ch.setUserObject(uo);
		m_ch.setSocketBufferSize(rxTxBufLen).connect(ip, port, this);

	}

	// /**
	// *
	// * @return
	// */
	public AsynchronousSocketChannel getChannel() {
		return m_ch.getChannel();
	}

	/**
	 * 
	 * 
	 * 
	 * @param milliSec
	 */

	//
	// private void connectTimeout(long milliSec) {
	//
	// }

	/**
	 * 
	 * @return
	 */
	public boolean isAlive() {
		return m_ch.isAlive();
	}

	public void close() {
		m_ch.close();
	}

	public boolean send(byte[] data) {
		return m_ch.send(data);
	}

	////////////////////////////////////////////////////////////////////
	@Override
	public void recv(MqChannel ask, byte[] buf, int len) {
		// TODO Auto-generated method stub
		m_inter.recv(this, buf, len);

	}

	@Override
	public void connected(MqChannel ask) {
		m_inter.connected(this);

	}

	@Override
	public void disconnected(MqChannel ask) {
		m_inter.disconnected(this);

	}

}
