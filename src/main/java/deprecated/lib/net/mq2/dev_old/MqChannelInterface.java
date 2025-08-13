package deprecated.lib.net.mq2.dev_old;

public interface MqChannelInterface {

	public void recv(MqChannel ask,byte[] buf, int len);

	public void connected(MqChannel ask);

	public void disconnected(MqChannel ask);

}


 
 