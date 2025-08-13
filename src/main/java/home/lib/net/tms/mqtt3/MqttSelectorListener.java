package home.lib.net.tms.mqtt3;

import java.nio.channels.SocketChannel;

public interface MqttSelectorListener {

	public void received(SocketChannel ch, byte[] rxd);

	public void disconnected(SocketChannel ch);

	public Object accepteded(SocketChannel ch);

	public boolean connected(SocketChannel channel, Object userojb);

}
