package home.lib.net.tms.mqtt;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import home.lib.log.MyLogger;
import home.lib.net.tms.TmsSelector;
import home.lib.net.tms.mqtt.file.MqttFile;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

public class MqttTest {

	public static void main(String[] args) throws Exception {

		
		TmsSelector.debugLevel=0xff;
		
		
		MqttBroker.debugLevel |= MyLogger.VERBOSE;
		MqttBroker.debugLevel |= MyLogger.EXCEPTION;
		MqttBroker.debugLevel |= MyLogger.DEBUG;

		test2();//tiny.mqtt
		//test3();//mosquitto
		
		
		System.out.println("mqtt max buff size= "+ StringUtil.formatBytesSize( MqttMessage.MaxBufferLength));
		
		
		//test1();
	}

	public static void test1() throws Exception {

		MqttClient sub = new MqttClient();

		sub.setId("sub1"+System.currentTimeMillis());
		sub.connect("127.0.0.1", 1883).waitForConnack(3);

		TimeUtil.sleep(100);

		// sub.subscribe(new Topic("/hello"));

		sub.subscribe( ("log/CQueue.MSG_KTS_LOG"), new IMqtt() {

			@Override
			public void callback(MqttClient source, String topic, byte[] payload ) {

				System.out.format("callback topic=[%s]  payload=[%s] \r\n", topic ,
						new String(payload));

			}
		});

		//
//		//
//		MqttClient mc = new MqttClient();
//
//		mc.setId("client1");
//
//		mc.connect("114.200.254.181", 1883).waitForConnack(3);
//
//		 
//		mc.publish("/hello", "payload~", 0);
//
//		TimeUtil.sleep(1000);
//
//		sub.unsubscribe( ("/hello"));
//
//		sub.subscribe(  "/hello2" , new IMqtt() {
//
//			@Override
//			public void callback(MqttClient source, String topic, byte[] payload ) {
//				System.out.format("callback topic=[%s]  payload=[%s] \r\n", topic ,
//						new String(payload));
//
//			}
//		});

		// System.out.println("connect="+mc.connected() );


		
	}

	//
	//

	public static void test2() throws Exception {

		System.out.println("mqtt test");
//
		MqttBroker brk = new MqttBroker(1883);

		brk.begin();
		
		 
	 
//
//		byte qos=2;
//		//
		MqttClient sub = new MqttClient();
		//
		sub.setId("sub1");
		sub.connect("127.0.0.1", 1883).waitForConnack(3);
		sub.subscribe("a/+/c", new IMqtt() {
 
			@Override
			public void callback(MqttClient source, String topic, byte[] payload) {
 
				System.out.format("callback topic=[%s]  payload=[%s] \r\n", topic,
						new String(Arrays.copyOf(payload, payload.length )));

				
			}
		});  
		
		
		TimeUtil.sleep(1000);
		
		for( SocketChannel c : brk.getSelector().getChannels() ) {
			
			brk.getSelector().channelClose(c);
		}
		
		
		TimeUtil.sleep(1000);
		
		brk.close();
		
		TimeUtil.sleep(1000);

		System.out.println("brk recovering...");

		
		MqttBroker brk2 = new MqttBroker(1883);

		brk2.begin();
		
		//sub.close();

		

		while (true) {
			TimeUtil.sleep(3000);
			
			System.out.println( brk2 );

			// mc.loop();
			// sub.loop();
		}
		
		
		
// 
//		
//		
//		for(int i=0;i<100;i++) {
//			MqttClient sub = new MqttClient();
//			//
//			sub.setId("sub-"+i);
//			// 
//			// //mc.connect("114.200.254.181", 1883);
//			sub.connect("127.0.0.1", 1883).waitForConnack(3);
//			
//			sub.subscribe(new Topic("a/+/c"),(byte)0, new IMqtt() {
//				
//							@Override
//							public void callback(MqttClient source, Topic topic, byte[] payload, int payload_length) {
//				
//								System.out.format("callback topic=[%s]  payload=[%s] \r\n", topic.str(),
//										new String(Arrays.copyOf(payload, payload_length)));
//				
//							}
//						}); 
//		}

		
		
//		// 
////		//
////		//
//		MqttClient mc = new MqttClient();
//		//
//		mc.setId("client1");
//		// 
//		// //mc.connect("114.200.254.181", 1883);
//		mc.connect("127.0.0.1", 1883).waitForConnack(3);
//
//		Topic t = new Topic("a/x/c");
//		//mc.publish(t, "payload" + TimeUtil.now() + "qos0~", 0);
//		// mc.publish(t, "payload" + TimeUtil.now() + "qos1~", 1);
//		// mc.publish(t, "payload" + TimeUtil.now() + "qos2~", 2);
//		
//		mc.publish(t, "start~~~~~~~~~"  , 2);
//		
//		long cnt=0;
//		long sps=0;
//		long bk_sps=0;
//		
//		TimeUtil aSec=new TimeUtil();
//
//		while (true) {
//			TimeUtil.sleep(3000);
//
//			// mc.loop();
//			// sub.loop();
//			// brk.loop();
//			String ss=String.format("payload %s  qos0~  cnt=%s  rcnt=%s  sps= %s", TimeUtil.now(),(cnt++), "",  bk_sps);
//			
//		 	mc.publish(t, ss  , 2);
//		 	sps++;
//		 	
//		 	
//		 	if( aSec.end_sec()>1.0) {
//		 		aSec.start();
//		 		
//		 		bk_sps=sps;
//		 		sps=0;
//		 	}
//
//		}
////		
		 

	}

	public static void test3() throws Exception {

		System.out.println("mqtt test");

		//
		//
		MqttClient mc = new MqttClient();

		mc.setId("pub1");

		mc.connect("114.200.254.181", 1883).waitForConnack(3);
//
		String t = ("a/b/c");
		// mc.publish(t, "payload" + TimeUtil.now() + "qos0~", 0);
		// mc.publish(t, "payload" + TimeUtil.now() + "qos1~", 1);

		///
		//
		//
		MqttClient sub = new MqttClient();
		//
		sub.setId("sub1");
		sub.connect("114.200.254.181", 1883).waitForConnack(3);
		sub.subscribe( ("a/+/c"), new IMqtt() {

			@Override
			public void callback(MqttClient source, String topic, byte[] payload) {

				System.out.format("callback topic=[%s] payload=[%s] \r\n", topic ,
						new String(payload));

			}
		});  
		//

		while (true) {
			TimeUtil.sleep(3000);

			// mc.publish(t, "payload" + TimeUtil.now() + "qos1~", 1);

			// mc.publish(t, "payload" + TimeUtil.now() + "qos2~", 2);

			// mc.loop();
			// sub.loop();
			// brk.loop();
			try {
				
				
				 
				 mc.publish(t , "payload" + TimeUtil.now() + "qos0~", 0);

			} catch (Exception ex) {
				ex.printStackTrace();
			}

		}

	}

	public static void test4() throws Exception {

		System.out.println("mqtt test");

		//
		MqttClient sub = new MqttClient();
		//
		sub.setId("subJ");
		sub.connect("114.200.254.181", 1883);

		// TimeUtil.sleep(100);
		sub.subscribe( ("/hello"), new IMqtt() {

			@Override
			public void callback(MqttClient source, String topic, byte[] payload) {

				System.out.format("callback topic=[%s]  payload=[%s] \r\n", topic ,
						new String(payload));

			}
		});

		//
		//
		//

		while (true) {
			TimeUtil.sleep(100);

			// mc.loop();
			// sub.loop();
			// sub.loop();

		}

	}

}
