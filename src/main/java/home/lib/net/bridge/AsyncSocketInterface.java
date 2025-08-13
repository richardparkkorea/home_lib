package home.lib.net.bridge;

public interface AsyncSocketInterface {

	public void recv(AsyncSocket ask,byte[] buf, int len);

	public void connected(AsyncSocket ask);

	public void disconnected(AsyncSocket ask);

}


 
 