package home.lib.net.nio.sync;


public interface SyncSocketListener {

	public void recv(SyncSocket ask,byte[] buf, int len);

	public void connected(SyncSocket ask);

	public void disconnected(SyncSocket ask);

}


 
 