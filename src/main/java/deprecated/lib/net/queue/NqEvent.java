package deprecated.lib.net.queue;

import home.lib.lang.StdStream;

import java.util.Arrays;

//import home.dev.lang.uStdC;

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
@Deprecated
final public class NqEvent {

	final static public int EventNone = 0;
	final static public int EventConnect = 100;
	final static public int EventRecevied = 200;
	final static public int EventClose = 300;
	final static public int EventServerClose = 301;
	final static public int EventAccept = 400;
	// final static public int EventStep2Done=500;

	final static public long DEFAULT_BYTES_SIZE = 12;// just i supposed..

	public java.net.Socket sk = null;

	// data

	public INqPacketInterface IPacket = null;
	
	//

	public byte[] buf = null;
	// public int len;
	// public String event_str = "";
	// public String sk_str="";
	// public long cid=0;

	// noraml
	// accept / close / recv
	//
	// stream
	// step1 , step2-doing, step2-done , step3
	// public String msg = "";
	public int msg = 0;

	// int p; // pos
	// int e; // cnt
	long sendNumber; // sending number

	/**
	 * 
	 */
	public NqEvent() {
		//buf = null;
		// len = 0;
	}

	/**
	 * 
	 * @param b
	 * @param pos
	 * @param len
	 */
	void copyOf(byte[] b,  int len) {

		buf = new byte[len];
		
		System.arraycopy(b, 0, buf, 0, len);
	}

	/**
	 * 
	 * @return
	 */
	public Object obj() {
		StdStream s = new StdStream(buf);

		try {
			return s.readObject();
		} catch (Exception e) {
			return null;
		}
 
	}

}
