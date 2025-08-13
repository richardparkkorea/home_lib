package home.lib.net.mq;


public interface IASocketChannelInterface {

	public void recv(ASocketChannel ask,byte[] buf, int len);

	public void connected(ASocketChannel ask);

	public void disconnected(ASocketChannel ask);

}


 
 