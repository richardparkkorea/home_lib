package deprecated.lib.net.nsock;

import java.net.Socket;
import java.nio.channels.SocketChannel;

/**
 * 
 * @author root
 * 
 */
@Deprecated
public interface NonSocketInterface {

	public long test1();

	/**
	 * 
	 * @param p
	 * @param s
	 * @return
	 */
	public NonSocketInterface getNew(NonSocket p, SocketChannel s);

	/**
	 * 
	 * @return
	 */
	public SocketChannel getChannel();

	/**
	 * 
	 * @param bufData
	 * @param bufLen
	 * @return
	 * 
	 *         null - error 1 - succeed
	 */
	//public NonEvent parseReceivedData(byte[] bufData, int bufLen);

	/**
	 * 
	 * @param sndBuf
	 * @param sndLen
	 * @return
	 * 
	 *         null - error byte[0] - no response otherwise - succeed
	 */
	public byte[] sendData(byte[] sndBuf, int sndLen);

	public Object sendData(Object o);

	/**
	 * 
	 * @param buf
	 * @param s
	 * @return
	 */
	public int setReturnData(byte[] buf, int s);

	public int setReturnData(Object o);
	
	
	

	/**
	 * 
	 * inside functions
	 * @param buf
	 */
 	public void doReceive(byte[] buf,int len);
 	
 	public void cleanUp();
 	
 	public boolean isReserveToClose();
 

}
