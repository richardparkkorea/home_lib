package deprecated.lib.net.nsock;

import home.lib.lang.StdStream;

import java.nio.channels.SocketChannel;
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
final public class NonEvent {

	final static public String EventNone = "none";
	final static public String EventConnect = "connect";
	final static public String EventRecevied = "receive";
	final static public String EventClose = "close";
	final static public String EventServerClose = "server_close";
	final static public String EventAccept ="accpet";
	// final static public int EventStep2Done=500;

	final static public long DEFAULT_BYTES_SIZE = 12;// just i supposed..

	public SocketChannel sk = null;

	// data

	public NonSocketInterface Isocket = null;
	
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
	public String msg = "";

	// int p; // pos
	// int e; // cnt
	long sendNumber; // sending number

	/**
	 * 
	 */
	public NonEvent() {
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
