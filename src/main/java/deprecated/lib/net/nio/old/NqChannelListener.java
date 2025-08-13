package deprecated.lib.net.nio.old;

import java.nio.channels.SocketChannel;

public interface NqChannelListener {

	public void received(  SocketChannel ask,byte[] buf, int len);

	public void connected(  SocketChannel ask);

	public void disconnected(  SocketChannel ask);

	public Object _lock();
}


 
 