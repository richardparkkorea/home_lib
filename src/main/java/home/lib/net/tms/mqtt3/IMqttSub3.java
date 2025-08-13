package home.lib.net.tms.mqtt3;

public interface IMqttSub3 {

	public void onReceived(MqttClient3 source, String topic, byte[] payload);

	public void subscribeSuccess(MqttClient3 source, String topic);

	public void subscribeFailed(MqttClient3 source, String topic);

}
