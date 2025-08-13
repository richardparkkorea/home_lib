package deprecated.stable1.lib.net.mq.old0;

public interface ASocketChannelInterface {

	public void recv(ASocketChannel ask,byte[] buf, int len);

	public void connected(ASocketChannel ask);

	public void disconnected(ASocketChannel ask);

}


 
 