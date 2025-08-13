package home.lib.net.tms.mqtt3ssl;


public interface MqttSocketListener {

	public void received(MqttSocket ask,byte[] buf, int len);

	public void connected(MqttSocket ask);

	public void disconnected(MqttSocket ask);

}


 
 