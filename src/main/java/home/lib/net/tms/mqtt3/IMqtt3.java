package home.lib.net.tms.mqtt3;

public interface IMqtt3 {

	public void onConnected(MqttClient3 source);
	
	public void onDisonnected(MqttClient3 source);

	/**
	 * packet identify (it's a return value of Publish)
	 * 
	
	 * @param pi - pakcet indentity
	 * @param past_ms - processing completion time  
	 */
	public void deliveryComplete(long pi,long past_ms);

}
