package deprecated.lib.net.mq;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import home.lib.util.TimeUtil;

public class ASocketChannel {

	ASocketChannel _this = this;

	ASocketChannelInterface m_listener = null;
	public AsynchronousSocketChannel m_channel = null;
	ReadHandler readHandler = new ReadHandler();
	WriteHandler writeHandler = new WriteHandler();
	
	boolean m_connecting = false;
	public boolean isConnecting() {
		return m_connecting;
	}

	private Object m_user_obj = null;

	public Object getUserObject() {
		return m_user_obj;
	}

	public void setUserObject(Object o) {
		m_user_obj = o;
	}

	/**
	 * rx buffer size = (1024*8) examples<br>
	 * ASocketchannel(ip, port, l, 1024 * 8, 3000,buffer length, user object );<br>
	 * 
	 * @param ip
	 * @param port
	 * @param l
	 * @throws Exception
	 */

	public ASocketChannel(String ip, int port, ASocketChannelInterface l) throws Exception {

		this(ip, port, l, 1024 * 8, 3000, null, null);

	}

	/**
	 * 
	 * @param ip
	 * @param port
	 * @param l
	 * @param rxTxBufLen
	 * @throws Exception
	 */
	public ASocketChannel(String ip, int port, ASocketChannelInterface l, int rxTxBufLen, long connectionTimeout)
			throws Exception {
		this(ip, port, l, rxTxBufLen, connectionTimeout, null, null);
	}

 
 	/**
	 * connect , wait
	 * @param ip
	 * @param port
	 * @param l
	 * @param rxTxBufLen
	 * @param connectionTimeout
	 * @param bind
	 * @param uo
	 * @throws Exception
	 */
	  
	public ASocketChannel(String ip, int port, ASocketChannelInterface l, int rxTxBufLen, long connectionTimeout,
			InetSocketAddress bind, Object uo) throws Exception {

		m_listener = l;
		m_user_obj = uo;

		m_channel = AsynchronousSocketChannel.open();

		if (bind != null) {
			m_channel.bind(bind);
		}

		SocketAddress serverAddr = new InetSocketAddress(ip, port);

		m_connecting = true;
		connectTimeout(connectionTimeout);
		Future<Void> result = m_channel.connect(serverAddr);

		result.get();
		m_connecting = false;

		Attachment attach = new Attachment();
		attach.channel = m_channel;
		attach.buffer = ByteBuffer.allocate(rxTxBufLen);
		attach.isRead = true;
		attach.buffer.flip();

		//
		//
		//
		try {
			m_listener.connected(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// readHandler.channel = m_channel;
		// writeHandler.channel = m_channel;
		// readWriteHandler.start();

		m_channel.write(attach.buffer, attach, readHandler);// start read
		// attach.mainThread.join();

	}

	/**
	 * 
	 * 
	 * @param ch
	 * @param l
	 * @param rxTxBufLen
	 * @throws Exception
	 */
	public ASocketChannel(AsynchronousSocketChannel ch, ASocketChannelInterface l, int rxTxBufLen) throws Exception {

		m_listener = l;

		m_channel = ch;

		Attachment attach = new Attachment();
		attach.channel = m_channel;
		attach.buffer = ByteBuffer.allocate(rxTxBufLen);
		attach.isRead = true;
		attach.buffer.flip();

		//
		//
		//
		try {
			m_listener.connected(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// readHandler.channel = m_channel;
		// writeHandler.channel = m_channel;
		// readWriteHandler.start();

		m_channel.write(attach.buffer, attach, readHandler);// start read
		// attach.mainThread.join();

	}

	
  
 	 /**
 	 * connect  , no waiting
	 * SocketAddress serverAddr = new InetSocketAddress(ip, port);
	  * 
 	  * @param addr
 	  * @param l
 	  * @param rxTxBufLen
 	  * @param bind
 	  * @param uo
 	  */
 	public ASocketChannel(final SocketAddress addr, ASocketChannelInterface l, final int rxTxBufLen,
			final InetSocketAddress bind, Object uo) {

		m_listener = l;
		m_user_obj = uo;
		 
		

		new Thread(new Runnable() {
			public void run() {

				try {
					m_connecting = true;
					m_channel = AsynchronousSocketChannel.open();

					if (bind != null) {
						m_channel.bind(bind);
					}
 

					Future<Void> result = m_channel.connect(addr);

					result.get();

					Attachment attach = new Attachment();
					attach.channel = m_channel;
					attach.buffer = ByteBuffer.allocate(rxTxBufLen);
					attach.isRead = true;
					attach.buffer.flip();

					//
					//try {
						m_listener.connected(_this);
					//} catch (Exception e) {
					//	e.printStackTrace();
					//}

					m_channel.write(attach.buffer, attach, readHandler);// start read

				} catch (Exception e) {
					_this.close_event();
					System.out.format( "ASocketchannel.err  addr(%s) %s \r\n", addr, e.toString().trim() );
					
 
				} finally {
					m_connecting = false;
				}

			}
		}).start();
	}
	
	/**
	 * 
	 * @return
	 */
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

					if (m_connecting == false)// connect success
						return;
				}

				// if still wait to connect then we will define it's fail!
				if (m_connecting) {

					try {
						m_channel.close();
					} catch (IOException e) {

						e.printStackTrace();
					}
					m_channel = null;

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

		//
		// if (m_channel.isOpen() == false) {
		// //m_channel=null;
		// return;
		// }

		try {
			// System.out.println("||||||||||||||||||||||||||| >>>> close test 3");
			m_channel.close();

		} catch (Exception e) {
			// e.printStackTrace();
		}

		// m_channel=null;

	}

	public void close_event() {

		if (m_channel == null)
			return;

		// //
		try {
			m_channel.close();
		} catch (IOException e1) {

		}
		//
		try {
			m_listener.disconnected(this);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		m_channel = null;

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

		//
		//
		boolean isWriting = false;
		Deque<ByteBuffer> writeData = new LinkedList<ByteBuffer>();

		public Attachment() {
			buffer = ByteBuffer.allocate(0);
		}
	}

	/**
	 * 
	 * @author richardpark
	 *
	 */
	class ReadHandler implements CompletionHandler<Integer, Attachment> {
		// AsynchronousSocketChannel channel = null;

		@Override
		public void completed(Integer result, Attachment attach) {

			// System.out.println("completed | " + result + " | " + attach);

			if (attach == null)
				return;

			if (result == -1) {

				close_event();

				return;
			}

			attach.buffer.flip();
			int limits = attach.buffer.limit();
			byte bytes[] = new byte[limits];
			attach.buffer.get(bytes, 0, limits);

			// System.out.format(" read len= %d (%s) \r\b", bytes.length ,attach );
			//
			try {
				m_listener.recv(_this, bytes, bytes.length);
			} catch (Exception e) {
				e.printStackTrace();
			}

			attach.isRead = true;
			attach.buffer.clear();
			attach.channel.read(attach.buffer, attach, this);

			// System.out.format("read (%s) \r\n", attach);

		}

		/**
		 * 
		 * 
		 * @param e
		 * @param attach
		 */
		@Override
		public void failed(Throwable e, Attachment attach) {
			e.printStackTrace();
			close_event();
		}

	}

	Attachment writeAttach = new Attachment();

	class WriteHandler implements CompletionHandler<Integer, Attachment> {
		// AsynchronousSocketChannel channel = null;

		@Override
		public void completed(Integer result, Attachment attach) {

			// System.out.println("completed | " + result + " | " + attach);

			if (attach == null)
				return;

			if (result == -1) {

				close_event();

				return;
			}

			synchronized (attach.writeData) {
				try {

					ByteBuffer buffer = attach.writeData.getFirst();

					if (buffer.remaining() > 0) {
						attach.channel.write(attach.writeData.getFirst(), attach, this);// send remains
						return;
					}
					buffer.clear();
					attach.writeData.removeFirst();

				} catch (Exception e) {

					e.printStackTrace();
				}

				if (attach.writeData.size() > 0) {
					attach.channel.write(attach.writeData.getFirst(), attach, this);// start read
					attach.isWriting = true;
				} else {

					attach.isWriting = false;
				}

			} // sync
				// System.out.format("write (%s) \r\n", attach);

		}

		/**
		 * 
		 * 
		 * @param e
		 * @param attach
		 */
		@Override
		public void failed(Throwable e, Attachment attach) {
			e.printStackTrace();
			close_event();
		}

	}

	public boolean send(byte[] data) {

		if (isAlive() == false)
			return false;

		synchronized (writeAttach.writeData) {

			ByteBuffer buffer = ByteBuffer.allocate(data.length);
			buffer.put(data);
			buffer.flip();

			writeAttach.channel = m_channel;
			writeAttach.writeData.add(buffer);

			if (writeAttach.isWriting) {
				return true;
			}

			writeAttach.isWriting = true;

			m_channel.write(writeAttach.buffer, writeAttach, writeHandler);// start read

		} // sync
		return true;

	}

	
}
