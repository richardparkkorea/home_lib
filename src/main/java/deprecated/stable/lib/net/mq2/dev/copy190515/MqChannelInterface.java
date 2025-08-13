package deprecated.stable.lib.net.mq2.dev.copy190515;

public interface MqChannelInterface {

	public void recv(MqChannel ask,byte[] buf, int len);

	public void connected(MqChannel ask);

	public void disconnected(MqChannel ask);

}


 
 