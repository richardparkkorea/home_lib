package deprecated.lib.net.mq2.dev_old;

import home.lib.io.JobTask;

public interface MqServerInterface {

	 
	/**
	 * 
	 * @param svr
	 * @param ask
	 * @param m
	 * @return
	 * not null - relays the return value to the target client 
	 * null- not relay
	 */
	public MqItem recv(MqServer svr,MqClient ask, MqItem m);

	public void connected(MqServer svr,MqClient ask);

	public void disconnected(MqServer svr,MqClient ask);

	public void sendSucceeded(MqServer svr,MqClient ask,MqItem e);

	public void sendFailed(MqServer svr,MqClient ask,MqItem e);
	
	public boolean putPath(MqClient ask,String name);
}
