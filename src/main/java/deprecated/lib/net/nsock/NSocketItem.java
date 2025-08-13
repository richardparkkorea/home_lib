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
 * <p>
 * Title:
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * 
 * <p>
 * Company:
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */
//class ReturnReserveStruct {
//	public ReturnReserveStruct() {
//	}
//
//	// public int usable = 0;
//	public long sendNumber = 0;
//	public Socket sk = null;
//	//public uTime tu = new uTime(); // reset timer
//}

/**
 * 
 * @author root
 * 
 */
@Deprecated
class NqStreamPacketHeader {
	public NqStreamPacketHeader() {
	}

	SocketChannel sk;

	byte[] buf;

	long sendingNumber = 0;

	byte[] reserve;

}

/**
 * 
 * <p>
 * Title:
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * 
 * <p>
 * Company:
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */
public final class NSocketItem implements NonSocketInterface {

	private NonSocket m_parent = null;

	protected SocketChannel m_sock;
	// conf
	// public int m_maxBufferSize;
	private long m_resendIntervalTime;

	//
	public static byte[] byteArr0 = new byte[0];

	// std_send
	protected Object m_syncSendLock = new Object();
	protected int m_isReceivedReturnVal;
	protected NqStreamPacketHeader m_lastResponsePacket = new NqStreamPacketHeader();
	protected int m_currentSendNumber; // a section..

	// recv
	protected Object m_syncRecvLock = new Object();
	protected NqStreamPacketHeader m_lastReceivedPacket = new NqStreamPacketHeader();
	private ArrayList<Long> m_receivedNumbersBeforeThisTime = new ArrayList<Long>();

	// relate return
	// NqByteBufferWrap m_returnBufOnReceiver = new NqByteBufferWrap();
	// protected byte[] m_returnBufOnReceiver = null;
	// protected int m_returnBufferLenOnReceiver;

	// return:receiver
	protected Object m_returnBufferLock = new Object();
	// return:sender
	protected byte[] m_returnBufOnSender;
	protected int m_returnBufLenOnSender;
	protected int m_returnBufReceivedSize;

	//
	//
	//
	protected NqStreamPacketHeader m_packetPrepareForAnswering = new NqStreamPacketHeader();

	protected StdStream m_receviedDataCollector = new StdStream();
 
	public NSocketItem() {
		this(null,null);
	}
	
	public NSocketItem(NonSocket p, SocketChannel s) {

		m_parent = p;
		m_sock = s;
		//
		setReturnBufferOnSender(null, 0);
		//
		// initialize();
		// vals
		// m_maxBufferSize = 1024 * 8;// 8k
		m_currentSendNumber = 0;
		m_resendIntervalTime = 3000; // 1.5 second

	}

	/**
 * 
 */
	// initialize to recv buf...
	// void initialize() {
	// m_lastReceivedPacket.sendingNumber = 0;
	// setReturnData(null, 0);
	// }

	/**
	 * 
	 * 
	 * 
	 * 
	 * @return
	 */

	private static int packetHeaderSize() {
		return 18;
	}

	@Override
	public SocketChannel getChannel() {
		return m_sock;
	}

	/**
	 * 
	 * @param htext
	 *            String
	 * @param data
	 *            byte[]
	 * @param size
	 *            int
	 * @param sn
	 *            int
	 * @return byte[]
	 */
	private static byte[] packetWriteBytes(char pname, byte[] data, int size, long sn, byte[] reserve1) {

		StdStream stm = new StdStream();

		// System.out.println("test : " + ht.length );

		// head
		stm.writeByte((byte) pname);// 1
		stm.writeInt32(size);// 4
		stm.writeByte((byte) 's');// 1
		stm.writeInt64(sn);// 8
		stm.writeBytes(reserve1, 4);// reserve//4

		// data
		if (size != 0) {
			stm.writeBytes(data, size);
		}
		// tail
		stm.writeByte((byte) pname);// 1
		stm.writeInt32(size);// 4
		stm.writeByte((byte) 'e');// 1
		stm.writeInt64(sn);// 8
		stm.writeBytes(reserve1, 4);// reserve//4

		return stm.toByteArray();
	}

	/**
	 * 
	 * @param htext
	 *            String
	 * @param buf
	 *            byte[]
	 * @param bufLen
	 *            int
	 * @return int
	 */
	private int packetReadBytes(char pname, byte[] buf, int bufLen, NqStreamPacketHeader outVal) {

		// check front,tail
		if (bufLen < packetHeaderSize() * 2)
			return -1;

		StdStream stm = new StdStream(buf);
		stm.setPos(0);

		// front
		byte sht = stm.readByte();
		if (pname != sht)
			return -2;

		int len = stm.readInt32();
		byte f = stm.readByte();
		if (f != 's')
			return -3;

		long sn = stm.readInt64();

		byte[] rev1 = stm.readBytes(4);

		// data
		if (len > (bufLen - packetHeaderSize() * 2))
			return -4; // buffer over

		// data
		if (len > 0) {
			outVal.buf = stm.readBytes(len);
		} else {
			outVal.buf = new byte[0];
		}

		byte eht = stm.readByte();
		if (pname != eht)
			return -5;

		int len2 = stm.readInt32();
		byte f2 = stm.readByte();
		if (f2 != 'e')
			return -6;
		long sn2 = stm.readInt64();
		byte[] rev2 = stm.readBytes(4);

		if (sn != sn2 || len != len2)
			return -7;

		if (Arrays.equals(rev1, rev2) == false)
			return -7;

		outVal.sendingNumber = sn;
		outVal.reserve = rev1;

		return len;
	}

	/**
	 * 
	 * @param buf
	 * @param bufLen
	 * @return
	 * 
	 * 
	 *         -1 = error packet 0 = need more datas otherwise = ok
	 */
	private int checkPacketData(byte[] buf, int bufLen) {

		// check front,tail
		if (bufLen < packetHeaderSize() * 2)
			return 0;

		StdStream stm = new StdStream(buf);
		stm.setPos(0);

		// front
		byte sht = stm.readByte();
		if (sht != 's' && sht != 'r')
			return -1;// -2;

		int len = stm.readInt32();
		byte f = stm.readByte();
		if (f != 's')
			return -1;// -3;

		long sn = stm.readInt64();
		byte[] rev1 = stm.readBytes(4);

		// data
		if (len > (bufLen - packetHeaderSize() * 2))
			return 0; // buffer over

		// data
		try {
			stm.movePos(len);// jump
		} catch (Exception e) {
			return -1;
		}
		byte eht = stm.readByte();
		if (eht != 's' && eht != 'r')
			return -1;// -5;

		int len2 = stm.readInt32();
		byte f2 = stm.readByte();
		if (f2 != 'e')
			return -1;// -6;
		long sn2 = stm.readInt64();
		byte[] rev2 = stm.readBytes(4);

		if (sn != sn2 || len != len2)
			return -1;// -7;
		if (Arrays.equals(rev1, rev2) == false)
			return -1;// -7;

		return (len + packetHeaderSize() * 2);
	}

	/**
	 * 
	 * @param buf
	 *            byte[]
	 * @param s
	 *            int
	 * @return int
	 */
	private int setReturnBufferOnSender(byte[] buf, int s) {
		synchronized (m_returnBufferLock) {

			m_returnBufLenOnSender = s;
			if (s == 0)
				return 0;

			m_returnBufOnSender = buf;
			m_returnBufLenOnSender = s;
			return s;
		}
	}

	/**
	 * 
	 * @param buf
	 *            byte[]
	 * @param s
	 *            int
	 * @return int
	 */
	// private int copyReturnValueOnSender(byte[] buf, int s) {
	// synchronized (m_returnBufferLock) {
	// if (m_returnBufOnSender == null || m_returnBufLenOnSender == 0)
	// return 0;
	//
	// int cl = Math.min(s, m_returnBufLenOnSender);
	//
	// // System.out.println("c r v : "+ m_returnBufOnSender.length +
	// // "   "+ buf.length +"  "+ cl );
	//
	// // memcpy(m_returnBufOnSender, buf, cl);
	// m_returnBufOnReceiver = new byte[cl];
	//
	// // System.out.println( ""+buf+ " "+m_returnBufOnReceiver + " "+cl );
	//
	// System.arraycopy(buf, 0, m_returnBufOnReceiver, 0, cl);
	//
	// m_returnBufReceivedSize = s;
	//
	// m_returnBufLenOnSender = 0; // not recevie more datas
	// // m_returnBufOnSender[cl]=0;
	//
	// return cl;
	// }
	// }
 
	/**
	 * 
	 */
	@Override
	public int setReturnData(byte[] buf, int len) {

		if (m_parent == null)
			return 0;

		synchronized (m_syncRecvLock) {

			synchronized (m_returnBufferLock) {

				// System.out.println("test-1 "+m_returnBufOnReceiver + "  "+m_parent.m_maxBufferSize );

				// check: max buffer size
				if (len > m_parent.m_maxBufferSize) {
					// FileLog::prt("result buffer size over (%d/%d) ", s,
					// m_max_buffer_size);
					// m_returnBufferLenOnReceiver = 0;
					return 0;
				}

				// m_returnBufferLenOnReceiver = s;
				if (len == 0)
					return 0;

				// m_returnBufOnReceiver.realloc(s + 1);
				// m_returnBufOnReceiver = new byte[s];
				// memcpy(m_returnBufOnReceiver, buf, s); // copy

				// System.arraycopy(buf, 0, m_returnBufOnReceiver, 0, s);
				// m_returnBufOnReceiver = Arrays.copyOf(buf,len);

				// m_returnBufOnReceiver.buf[s] = 0;

				m_packetPrepareForAnswering.sk = m_sock;
				m_packetPrepareForAnswering.sendingNumber = m_lastReceivedPacket.sendingNumber;
				m_packetPrepareForAnswering.buf = Arrays.copyOf(buf, len);

				// if (m_packetPrepareForAnswering.sendingNumber == m_lastReceivedPacket.sendingNumber) {

				int r = sendResponse(m_lastReceivedPacket.sendingNumber);

				// System.out.println("test-2 "+m_rData.sendingNumber+"   "+r );
				// init

				// m_packetPrepareForAnswering.sendingNumber = 0;
				// }

				return len;
			}
		}
	}

	/**
	 * 
 	 * 
	 * 
	 *         null - error byte[0] - no response otherwise - succeed
	 * 
	 * 
	 */
	@Override
	public byte[] sendData(byte[] sndBuf, int sndLen) {

		if (m_parent == null)
			return null;
		
		
		

		// debug("sendstream");

		synchronized (m_syncSendLock) {

			double time_out = m_parent.m_sendTimeout;

			// init return buf
			m_returnBufOnSender = null;
			m_returnBufLenOnSender = 0;

			byte[] recvBuf = new byte[m_parent.m_maxBufferSize];
			setReturnBufferOnSender(recvBuf, recvBuf.length);

			// recv events
			m_isReceivedReturnVal = 0;
			m_returnBufReceivedSize = 0;

			// sending number
			m_currentSendNumber++;
			// int sending_number = m_send_number; //copy to local

			// set local variables
			int resend_start = 0;
			TimeUtil resend_time = new TimeUtil();
			TimeUtil wait_time = new TimeUtil();
			//
			long sending_number = m_currentSendNumber;// System.currentTimeMillis();

			long startSendTime = System.currentTimeMillis();

			// System.out.println("test- send "+sending_number );

			wait_time.start();

			resend_start = 1;
			resend_time.start();

			byte[] reserve1 = new byte[] { 0, 0, 0, 0 };
			byte[] s1sbuf = packetWriteBytes('s', sndBuf, sndLen, sending_number, reserve1);

			// System.out.println(" send : " + String.valueOf(s1sbuf[0]) +"  " +
			// sending_number );

			while (true) {
				// System.out.println("--3  " + (wait_time.end() * 1000) + "   " + time_out);

				if (resend_start == 1 || (int) resend_time.end_ms() > m_resendIntervalTime * 3) {

					// int r1 = WindowAPI.send(sk, s1sbuf, s1sbuf.length, 0);

					try {
						// m_sock.getOutputStream().write(s1sbuf, 0, s1sbuf.length);
					
				 	 	//System.out.println("senddata======>");
				 	 	
				 	 	//byte[] bb=Arrays.copyOf(s1sbuf, s1sbuf.length);
						
				 	 	
						 

						//ByteBuffer buf=ByteBuffer.wrap(s1sbuf, 0, s1sbuf.length);
					 	
				 	 	ByteBuffer buf = ByteBuffer.allocate( s1sbuf.length );

						buf.put(s1sbuf, 0, s1sbuf.length);
						
						buf.flip();
						
						m_sock.write(buf);
						 
					} catch (Exception e) {
						System.out.println(e);
						//e.printStackTrace();
						return null;
					}
					//
					// if (r1 == 0 || r1 == -1) {
					// return null;
					// }

					resend_time.start();
					resend_start = 0;
				}

				if (time_out == 0.0)
					return byteArr0;

				//
				// sleep with long interval(30 ms)
				if (m_resendIntervalTime > 30) {
					Thread2.sleep(1);
				}

				if (m_isReceivedReturnVal == 1) {
					m_isReceivedReturnVal = 0;

					wait_time.start(); // timer reset

					synchronized (m_syncRecvLock) {
						if (sending_number == m_lastResponsePacket.sendingNumber) {
							this.m_resendIntervalTime = (System.currentTimeMillis() - startSendTime);

							if (m_returnBufReceivedSize > recvBuf.length) {

								// System.out.println("test--1");
								return byteArr0;// return buffer over!
							}

							return Arrays.copyOf(m_lastResponsePacket.buf, m_returnBufReceivedSize);
							// return m_returnBufReceivedSize; // return: success
						}
					}
				}

				if (wait_time.end() > time_out || time_out == 0.0) {
					// System.out.println("test--2 "+time_out);
					return byteArr0;
				}

			} // while

			// return 0; //return : fail

		} // sync
	}

	/**
	 * 
	 * @param sk
	 *            Socket
	 * @param step
	 *            int
	 * @param dd
	 *            NqStreamPacketHeader
	 * @param bufData
	 *            byte[]
	 * @param bufLen
	 *            int
	 * @return int
	 */
	private int sendResponse(long compareForSendingNumber) {

		if (m_parent == null)
			return 0;

		synchronized (m_returnBufferLock) {

			if (m_packetPrepareForAnswering.sendingNumber != compareForSendingNumber)
				return 0;

			byte[] reserve1 = new byte[] { 'r', 0, 0, 0 };
			byte[] bs = packetWriteBytes('r', m_packetPrepareForAnswering.buf, m_packetPrepareForAnswering.buf.length,
					m_packetPrepareForAnswering.sendingNumber, reserve1);

			// int r;
			try {
				// m_packetPrepareForAnswering.sk.getOutputStream().write(bs, 0, bs.length);
				m_packetPrepareForAnswering.sk.write(ByteBuffer.wrap(bs, 0, bs.length));
				// sk.getOutputStream().flush();
			} catch (Exception e) {
				return 0;
			}
			return bs.length;
			// return WindowAPI.send(sk, bs, bs.length, 0);
		}

	}

	/**
	 * return
	 * 
	 * null - error 1 - succeed
	 */

	 
	public NonEvent parseReceivedData(byte[] bufData, int bufLen) {

		
		//System.out.println( "parsedata : " + bufLen );
		
		if (m_parent == null)
			return null;

		synchronized (m_syncRecvLock) {

			//
			// all received data assemble in stream buffer
			//
			if (m_receviedDataCollector.length() > m_parent.m_maxBufferSize * 5) {
				// System.out.println("reset\r\n");
				m_receviedDataCollector.reset();
				return null;
			}

			//
			//
			m_receviedDataCollector.writeBytes(bufData, bufLen);

			//
			//
			if (checkPacketData(m_receviedDataCollector.bufferHandle(), m_receviedDataCollector.length()) == -1) {
				// System.out.println("reset\r\n");
				m_receviedDataCollector.reset();// check to packet format

				return null;

			} else {
				int n = checkPacketData(m_receviedDataCollector.bufferHandle(), m_receviedDataCollector.length());
				if (n > 0) {

					// byte[] rb = Arrays.copyOfRange(m_receviedDataCollector.bufferHandle(), 0, n);
					// m_receviedDataCollector.moveToFront(n);

					try {
						byte[] tmp = m_receviedDataCollector.popFont(n);
					} catch (Exception e) {
						e.printStackTrace();
					}

					//
					//
					NonEvent in_out_val = new NonEvent();

					// in_out_val.len = 0;
					in_out_val.msg = NonEvent.EventNone;
					in_out_val.sk = m_sock;
					in_out_val.Isocket = this;

					// check: buf length
					if (bufLen < packetHeaderSize() * 2) {
						// System.out.println("=1");
						return null;
					}

					// System.out.println("test->recv1 00-> "+bufData.length+" "+bufLen + "  " );

					NqStreamPacketHeader data_st = new NqStreamPacketHeader();

					//
					//
					// arrival data
					//
					if (packetReadBytes('s', bufData, bufLen, data_st) >= 0) {
						// System.out.println("test->recv1 01-> "+bufData+" "+bufLen );
						//
						// is recevied?
						for (int i = 0; i < m_receivedNumbersBeforeThisTime.size(); i++) {
							long it = m_receivedNumbersBeforeThisTime.get(i);
							if (it == data_st.sendingNumber) {

								synchronized (m_returnBufferLock) {
									sendResponse(data_st.sendingNumber);
								}
								// System.out.println("=2");
								return null; // already received
							}
						}

						// initialize();

						if (m_parent.m_maxBufferSize < data_st.buf.length) {
							return null;
						}

						//
						//
						m_lastReceivedPacket.sendingNumber = data_st.sendingNumber;

						// in_out_val.msg = NqEvent.EventStep2Done;
						in_out_val.msg = NonEvent.EventRecevied;
						in_out_val.copyOf(data_st.buf, data_st.buf.length);
						in_out_val.sendNumber = m_lastReceivedPacket.sendingNumber;

						//
						//
						m_receivedNumbersBeforeThisTime.add(data_st.sendingNumber);
						if (m_receivedNumbersBeforeThisTime.size() > 1024) {
							m_receivedNumbersBeforeThisTime.remove(0);
						}
						// }

						// for reponse performance

						// m_packetPrepareForAnswering.sendingNumber = m_lastReceivedPacket.sendingNumber;
						// m_packetPrepareForAnswering.sk = m_sock;
						// m_packetPrepareForAnswering.tu.start();

						// tux.start();
						// /return data_st.buffer.length;
						return in_out_val;

						// }

					}

					//
					// get response?
					//
					//
					if (packetReadBytes('r', bufData, bufLen, data_st) >= 0) {

						// System.out.println("test->recv1 02-> "+bufData+" "+bufLen + " "+data_st.sendingNumber + " "+
						// this.m_send_number);
						if (data_st.sendingNumber == this.m_currentSendNumber) {

							if (data_st.buf.length > 0 && data_st.reserve[0] == 'r') {

								// if (m_returnBufReceivedSize == 0) {
								// // byte[] bu = bufferM(bufData, data_st.buffer.length);
								// // `(bu, data_st.buffer.length);
								// //copyReturnValueOnSender(data_st.buf, data_st.buf.length);
								// }

								m_returnBufReceivedSize = data_st.buf.length;
								m_lastResponsePacket.buf = data_st.buf;
								m_lastResponsePacket.sendingNumber = data_st.sendingNumber;

								m_isReceivedReturnVal = 1;
							} else {
								// System.out.println("ignore to 0 length data");
							}
						}
					} // if("pt-r-01" )

				}// if (n > 0) {
			}// //} else {

			// System.out.println("=4");
			return null;
		} // sync
	} // method

	@Override
	public long test1() {
		// TODO Auto-generated method stub
		return m_resendIntervalTime;
	}

	/**
	 * create new handle and return
	 * 
	 * @param p
	 * @param s
	 * @return
	 */
	@Override
	public NonSocketInterface getNew(NonSocket p, SocketChannel s) {
		// TODO Auto-generated method stub
		return new NSocketItem(p, s);
	}

	/**
	 * 
	 */
	@Override
	public Object sendData(Object o) {

		StdStream s = new StdStream();

		try {
			s.writeObject(o);

			byte[] b = sendData(s.bufferHandle(), s.length());

			if (b != null && b.length>0) {
				StdStream r = new StdStream(b);
				return r.readObject();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * 
	 */
	@Override
	public int setReturnData(Object o) {

		StdStream s = new StdStream();

		try {
			s.writeObject(o);
		} catch (Exception e) {
			return 0;
		}
		return setReturnData(s.bufferHandle(), s.length());
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 */
	protected TimeUtil m_lastReceivedTime = new TimeUtil();
	protected int m_closeReservation = 0;

	/**
	 * 
	 * 
	 * 
	 *  
	 * 
	 * 
	 */
	@Override
	public void doReceive(byte[] buf,int len) {
		m_lastReceivedTime.start();

		int wantKeepContinue = 0;
		do {
			wantKeepContinue = 0;

			NonEvent ev = this.parseReceivedData(buf, len);

			if (ev != null) {
				wantKeepContinue = 1;
				m_parent.myEventMgr.addEvent(ev);
			} else {
				// System.out.println("err 2 : "+rr+"  ="+ev.msg);
			}

		} while (wantKeepContinue == 1);

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
		if ( m_closeReservation == 1
				|| m_lastReceivedTime.end() > m_parent.m_receiveTimeout) {
			return true;
		}

		return false;
	}

	// public static void memcpy(byte[] a, byte[] b, int n) {
	// System.arraycopy(b, 0, a, 0, n);
	// }

} // class
