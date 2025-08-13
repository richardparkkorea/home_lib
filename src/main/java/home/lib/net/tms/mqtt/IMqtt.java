package home.lib.net.tms.mqtt;

public interface IMqtt {
	 
	public void callback(  MqttClient source,   String topic, byte[] payload);
	
	
	
}
