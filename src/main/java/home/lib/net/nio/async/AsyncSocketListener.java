package home.lib.net.nio.async;

public interface AsyncSocketListener {

	public void recv(AsyncSocket ask,byte[] buf, int len);

	public void connected(AsyncSocket ask);

	public void disconnected(AsyncSocket ask);

}


 
 