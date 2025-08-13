package home.lib.net.tms;

import java.nio.channels.SocketChannel;

public interface TmsSelectorListener {

	public TmsItem received(SocketChannel ch, byte[] rxd);

	public void disconnected(SocketChannel ch);

	public Object accepteded(SocketChannel ch);

	public boolean connected(SocketChannel channel, Object userojb);

}
