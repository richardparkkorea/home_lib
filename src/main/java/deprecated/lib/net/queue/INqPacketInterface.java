package deprecated.lib.net.queue;

import java.net.Socket;

/**
 * 
 * @author root
 * 
 */
public interface INqPacketInterface {

	public long test1();

	/**
	 * 
	 * @param p
	 * @param s
	 * @return
	 */
	public INqPacketInterface providerNewHandle(NqServer p, Socket s);
	
	/**
	 * 
	 * @return
	 */
	public Socket socket();
	
	/**
	 * 
	 * 
	 */
	public void close();


	/**
	 * 
	 * @param bufData
	 * @param bufLen
	 * @return
	 * 
	 * 	 null - error 
	 *   1 - succeed
	 */
	public NqEvent recvData(byte[] bufData, int bufLen);

	/**
	 * 
	 * @param sndBuf
	 * @param sndLen
	 * @return
	 * 
	 *       null - error 
	 *       byte[0] - no response 
	 *       otherwise - succeed

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
	
	

	
	
	
	
	
	
	
	
	
}
