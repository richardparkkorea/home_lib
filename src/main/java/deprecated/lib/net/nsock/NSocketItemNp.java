package deprecated.lib.net.nsock;

import home.lib.lang.Thread2;
import home.lib.lang.StdStream;
import home.lib.util.TimeUtil;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * 
 *  
 * 
 * 
 * 
 * 
 *  
 */
@Deprecated
public final class NSocketItemNp implements NonSocketInterface {

	private NonSocket m_parent = null;

	protected SocketChannel m_sock;

	private int m_closeReservation = 0;

	private TimeUtil m_lastReceivedTime = new TimeUtil();

	public NSocketItemNp() {
		this(null,null);	
	}

	public NSocketItemNp(NonSocket p, SocketChannel s) {
		m_parent = p;
		m_sock = s;
	}

	@Override
	public long test1() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public NonSocketInterface getNew(NonSocket p, SocketChannel s) {
		return new NSocketItemNp(p, s);
	}

	@Override
	public SocketChannel getChannel() {
		return m_sock;
	}

	@Override
	public byte[] sendData(byte[] sndBuf, int sndLen) {
		try {

			ByteBuffer buf = ByteBuffer.allocate(sndLen);

			buf.put(sndBuf, 0, sndLen);

			buf.flip();

			m_sock.write(buf);

		} catch (Exception e) {
			System.out.println(e);
			// e.printStackTrace();
			return null;
		}
		return new byte[] { (byte) sndLen };
	}

	@Override
	public Object sendData(Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int setReturnData(byte[] buf, int s) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int setReturnData(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void doReceive(byte[] buf, int len) {

		m_lastReceivedTime.start();

		NonEvent ev = new NonEvent();

		ev.msg = NonEvent.EventRecevied;
		ev.copyOf(buf, len);
		ev.sendNumber = 0L;
		ev.Isocket=this;
		ev.sk=m_sock;

		m_parent.myEventMgr.addEvent(ev);

	}

	@Override
	public void cleanUp() {

		// System.out.println("cleanup");

		m_closeReservation = 1;
		try {
			m_sock.close();
			// m_sock = null;
		} catch (Exception e) {
			;
		}
		// m_th.wait2();
	}

	/**
	 * 
	 *  
	 * 
	 */
	@Override
	public boolean isReserveToClose() {
		if (m_closeReservation == 1 || m_lastReceivedTime.end() > m_parent.m_receiveTimeout) {
			return true;
		}

		return false;
	}

} // class
