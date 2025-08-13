package home.lib.net.tms.mqtt3;

public interface IMqttBroker3 {

	public boolean checkUser(String clientId, byte[] user,byte[] pwd);

	

}
