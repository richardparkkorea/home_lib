package home.lib.net.tms.mqtt3ssl;

public interface MqttServerSocketListener {

	public void selectorReceived(MqttSocket ch, byte[] rxd);

	public void selectorDisconnected(MqttSocket ch);

	public Object selectorAccepteded(MqttSocket ch);

	public boolean selectorConnected(MqttSocket channel, Object userojb);

}
