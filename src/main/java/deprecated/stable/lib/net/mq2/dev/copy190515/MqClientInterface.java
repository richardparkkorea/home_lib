package deprecated.stable.lib.net.mq2.dev.copy190515;

import home.lib.io.JobTask;

public interface MqClientInterface {

	public MqItem recv(MqClient ask, MqItem m);

	public void connected(MqClient ask);

	public void disconnected(MqClient ask);

	public void sendSucceeded(MqClient ask,MqItem e);

	public void sendFailed(MqClient ask,MqItem e);
	
	public boolean putPath(MqClient ask,String name);
}
