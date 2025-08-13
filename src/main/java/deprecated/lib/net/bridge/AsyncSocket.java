package deprecated.lib.net.bridge;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;

import home.lib.util.TimeUtil;

@Deprecated
public class AsyncSocket {

	AsyncSocket _this = this;

	AsyncSocketInterface m_listener = null;
	public AsynchronousSocketChannel m_channel = null;
	ReadWriteHandler readWriteHandler = new ReadWriteHandler();
	boolean m_waitConnect = false;

	private Object m_user_obj = null;

	public Object getUserObject() {
		return m_user_obj;
	}

	public void setUserObject(Object o) {
		m_user_obj = o;
	}

	/**
	 * rx buffer size = (1024*8)
	 * 
	 * @param ip
	 * @param port
	 * @param l
	 * @throws Exception
	 */

	public AsyncSocket(String ip, int port, AsyncSocketInterface l) throws Exception {

		this(ip, port, l, 1024 * 8, 3000);

	}

	/**
	 * 
	 * @param ip
	 * @param port
	 * @param l
	 * @param rxTxBufLen
	 * @throws Exception
	 */
	public AsyncSocket(String ip, int port, AsyncSocketInterface l, int rxTxBufLen, long connectionTimeout)
			throws Exception {

		m_listener = l;

		m_channel = AsynchronousSocketChannel.open();

		// m_channel.setOption(StandardSocketOptions.);

		SocketAddress serverAddr = new InetSocketAddress(ip, port);

		// System.out.println("-----------------before connect " + TimeUtil.now());

		m_waitConnect = true;
		connectTimeout(connectionTimeout);
		Future<Void> result = m_channel.connect(serverAddr);

		result.get();
		m_waitConnect = false;

		// System.out.println("-----------------after connect " + TimeUtil.now());

		// System.out.println("Connected");
		Attachment attach = new Attachment();
		attach.channel = m_channel;
		attach.buffer = ByteBuffer.allocate(rxTxBufLen);
		attach.isRead = true;
		// attach.mainThread = Thread.currentThread();

		// Charset cs = Charset.forName("UTF-8");
		// String msg = "Hello";
		// byte[] data = msg.getBytes(cs);
		// attach.buffer.put(data);
		attach.buffer.flip();

		//
		//
		//
		try {
			m_listener.connected(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		readWriteHandler.channel = m_channel;
		// readWriteHandler.start();

		m_channel.write(attach.buffer, attach, readWriteHandler);// start read
		// attach.mainThread.join();

	}

	public AsynchronousSocketChannel getChannel() {
		return m_channel;
	}

	/**
	 * 
	 * 
	 * 
	 * @param milliSec
	 */
	long m_connectTimeout = 3000;

	private void connectTimeout(long milliSec) {

		m_connectTimeout = milliSec;
		// set connect timeout 3 sec
		new Thread(new Runnable() {
			public void run() {
				TimeUtil tu0 = new TimeUtil();
				while (tu0.end_ms() < m_connectTimeout) {
					try {
						Thread.sleep(100);
						// System.out.println(" connecting : " + m_channel.isOpen());
					} catch (InterruptedException e) {

						e.printStackTrace();
					}

					if (m_waitConnect == false)// connect success
						return;
				}

				// if still wait to connect then we will define it's fail!
				if (m_waitConnect) {

					try {
						m_channel.close();
					} catch (IOException e) {

						e.printStackTrace();
					} finally {
						m_channel = null;
					}

				}

			}
		}).start();
	}

	/**
	 * 
	 * @return
	 */
	public boolean isAlive() {

		if (m_channel == null)
			return false;
		//
		if (m_channel.isOpen())
			return true;

		return false;
	}

	/**
	 * 
	 * 
	 */
	public void close() {

		if (m_channel == null)
			return;

		if (m_channel.isOpen() == false) {
			m_channel=null;
			return;
		}
			

		try {
			// System.out.println("||||||||||||||||||||||||||| >>>> close test 3");
			m_channel.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		m_channel = null;
	}

	/**
	 * 
	 * @param b
	 * @return
	 */
	public boolean send(byte[] b) {
		return readWriteHandler.send(b);
	}

	/**
	 * 
	 * 
	 * @author richardpark
	 *
	 */
	class Attachment {
		AsynchronousSocketChannel channel;
		ByteBuffer buffer;
		// Thread mainThread;
		boolean isRead;
	}

	/**
	 * 
	 * @author richardpark
	 *
	 */
	class ReadWriteHandler implements CompletionHandler<Integer, Attachment> {
		AsynchronousSocketChannel channel = null;

		@Override
		public void completed(Integer result, Attachment attach) {

			// System.out.println("completed | " + result + " | " + attach);

			if (attach == null)
				return;

			if (result == -1) {

				try {
					// System.out.println("||||||||||||||||||||||||||| >>>> close test 1 " + "completed | " + result
					// + " | " + attach + " " + channel.isOpen());

					channel.close();
				} catch (IOException e) {

				}
				//
				try {
					m_listener.disconnected(_this);
				} catch (Exception e) {
					e.printStackTrace();
				}
				m_channel=null;
				return;
			}

			attach.buffer.flip();
			// Charset cs = Charset.forName("UTF-8");
			int limits = attach.buffer.limit();
			byte bytes[] = new byte[limits];
			attach.buffer.get(bytes, 0, limits);

			//
			try {
				m_listener.recv(_this, bytes, bytes.length);
			} catch (Exception e) {
				e.printStackTrace();
			}

			attach.isRead = true;
			attach.buffer.clear();
			attach.channel.read(attach.buffer, attach, this);

		}

		/**
		 * 
		 * 
		 * @param e
		 * @param attach
		 */
		@Override
		public void failed(Throwable e, Attachment attach) {
			try {

				// System.out.println(" failed ===> ");
				if (m_channel != null)
					m_channel.close();

			} catch (Exception ex) {
				;
			}

			//
			try {
				m_listener.disconnected(_this);
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			m_channel = null;
			// e.printStackTrace();
		}

		public boolean send(byte[] data) {

			if (isAlive() == false)
				return false;

			ByteBuffer buffer = ByteBuffer.allocate(data.length);

			buffer.clear();
			// byte[] data = msg.getBytes();
			buffer.put(data);
			buffer.flip();

			try {
				// channel.write(buffer, 0, TimeUnit.SECONDS, null, this);
				int remind = data.length;
				int cnt = 0;
				while (remind > 0 && cnt < 1024) {// 171031
					remind -= (int) channel.write(buffer).get();

					cnt++;// prevent forever

					// if(remind!=0)
					// System.out.println("nio channel: remind="+remind );
				}
				// buffer.clear();
				return true;
			} catch (java.nio.channels.WritePendingException wpe) {

				System.out.println("nio channel :   " + wpe + "  len=" + data.length + "  " + channel.isOpen());

			} catch (Exception e) {

				System.out.println("nio channel : send err " + e);
				// //
				try {
					channel.close();
				} catch (IOException e1) {

				}
				//
				try {
					m_listener.disconnected(_this);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			return false;
		}

	}

}
