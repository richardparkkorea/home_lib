package deprecated.lib.net.mq;

public interface ASocketChannelInterface {

	public void recv(ASocketChannel ask,byte[] buf, int len);

	public void connected(ASocketChannel ask);

	public void disconnected(ASocketChannel ask);

}


 
 