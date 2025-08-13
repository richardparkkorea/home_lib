package deprecated.stable1.lib.net.mq.old0;

import java.nio.channels.SocketChannel;

 
public interface MqBrokerListener {
	public void addChannel(MqBroker svr, SocketChannel sk) ;
	/**
	 * 
	 * call it before add to name-list
	 * //true is allow , false is refuse
	 * @param id
	 * @param pwd
	 * @param sk
	 * @return
	 */
	 
	public boolean addClientId(MqBroker svr, String id, String pwd, SocketChannel sk) ;

	public void removeChannel(MqBroker svr, SocketChannel sk) ;

	/**
	 * if the result is null of actionPerformed, then the packet is no longer
	 * proceed.
	 * 
	 * return e is ok
	 * null is block  
	 * 
	 * @param e
	 * @return
	 */
	public MqBundle actionPerformed(MqBroker svr,  SocketChannel sk, MqBundle e)  ;

	/**
	 * if you use Queue without ProtocolOption then use the below fucntion.
	 * 
 	 */
	//public byte[] actionPerformed(MqBroker svr, SocketChannel sk, byte[] b) ;

	public void log(MqBroker svr, Exception e);

	public void log(MqBroker svr, int level, String s);

	public void startUp(MqBroker svr ) ;

	public void finishUp(MqBroker svr );
}
