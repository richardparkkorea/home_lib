package org.xcopy.net;

import java.nio.channels.SocketChannel;

import home.lib.lang.UserException;
import home.lib.net.mq.MqBundle;


 
 
public interface IMyBrokerListener {
	public void addChannel(MyBroker svr, SocketChannel sk) ;
	/**
	 * 
	 * call it before add to name-list
	 * //true is allow , false is refuse
	 * @param id
	 * @param pwd
	 * @param sk
	 * @return
	 */
	 
	public boolean addClientId(MyBroker svr, String path, String id, String pwd, SocketChannel sk) ;

	public void removeChannel(MyBroker svr, SocketChannel sk) ;

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
	public MqBundle actionPerformed(MyBroker svr,  SocketChannel sk, MqBundle e)  ;

	/**
	 * if you use Queue without ProtocolOption then use the below fucntion.
	 * 
 	 */
	//public byte[] actionPerformed(MqBroker svr, SocketChannel sk, byte[] b) ;

	public void log(MyBroker svr, Exception e);

	public void log(MyBroker svr, int level, String s);

	public void startUp(MyBroker svr ) ;

	public void finishUp(MyBroker svr );
	
 	
	public void returnError(MyBroker mqBroker, Object svr, Object con, UserException e);
	
	public boolean login(MyBroker mqBroker, Object svr, Object nqConnector, String id, String pwd);
	
 
}
