package home.lib.net.tms.mqtt.file;

import home.lib.log.MyLogger;
import home.lib.net.tms.TmsSelector;
import home.lib.net.tms.mqtt.IMqtt;
import home.lib.net.tms.mqtt.MqttBroker;
import home.lib.net.tms.mqtt.MqttClient;
import home.lib.util.TimeUtil;

public class MqttFile {
	


	public static void test2() throws Exception {

		System.out.println("mqtt test");
		
		TmsSelector.debugLevel = 0xff;

		MqttBroker.debugLevel |= MyLogger.VERBOSE;
		MqttBroker.debugLevel |= MyLogger.EXCEPTION;
		// MqttBroker.debugLevel |= MyLogger.DEBUG;
		
		
		//
		MqttBroker brk = new MqttBroker(1883);

		brk.begin();
//
//		Gson gson = new Gson();
//		String jsonString = "{'employee.name':'Bob','employee.salary':10000}";
//
//		Map<String, Object> m = new HashMap<String, Object>();
//
//		m.put("from", "richard");
//		m.put("when", "just now");
//		m.put("int", 123);
//
//		String json = gson.toJson(m);
//		System.out.println(json);
//
//		Map<String, String> r = gson.fromJson(json, Map.class);
//
//		for (Map.Entry<String, String> en : r.entrySet()) {
//
//			System.out.format("k=%s v=%s \n", en.getKey(), en.getValue());
//
//		}
		
//		MqttClient sub = new MqttClient();
//
//		sub.setId("sub1");
//		sub.connect("127.0.0.1", 1883);
//
//		//TimeUtil.sleep(100);
		
		
//
//		// sub.subscribe(new Topic("/hello"));
//
//		sub.subscribe( ("$SYS/broker/uptime"), new IMqtt() {
//
//			@Override
//			public void callback(MqttClient source, String topic, byte[] payload ) {
//
//				System.out.format("callback topic=[%s]  payload=[%s] \r\n", topic ,
//						new String(payload));
//
//			}
//		});
//		
		

		 MFileHandle receiver=new MFileHandle("a",1);
		 
		 
		// MFileHandle sender=new MFileHandle("b",2);
		// sender.upload("a",1,"d:\\tmp\\test");
		// sender.ls("a",1,"d:\\tmp");

	}
	
	 

	
}
