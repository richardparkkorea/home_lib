package home.lib.net.tms.mqtt3;


interface MqttSocketListener {

	public void recv(MtSocket ask,byte[] buf, int len);

	public void connected(MtSocket ask);

	public void disconnected(MtSocket ask);

}


 
 