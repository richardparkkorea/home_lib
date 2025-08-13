package home.lib.net.nio;

import java.nio.channels.SocketChannel;

public interface NqSelectorListener {

	 
 
	/**
	 * 	 * not null - relays the return value to the target client 
	 * null- not relay


	 * @param svr
	 * @param ask
	 * @return
	 * @throws Exception
	 */
	public NqChannelListener accepted(NqSelector svr,SocketChannel ask) throws Exception ;
	
	//public void connected(MqServer svr,MqClient ask);

//	public void disconnected(MqSelector svr,MqClient ask);

//	public void sendSucceeded(MqServer svr,MqClient ask,MqItem e);
//
//	public void sendFailed(MqServer svr,MqClient ask,MqItem e);
//	
//	public boolean putPath(MqClient ask,String name);
}
