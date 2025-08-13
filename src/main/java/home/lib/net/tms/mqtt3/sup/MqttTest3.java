package home.lib.net.tms.mqtt3.sup;

import home.lib.lang.UserException;
import home.lib.net.mq.MqBundle;
import home.lib.net.tms.TmsSelector;
import home.lib.net.tms.mqtt3.IMqtt3;
import home.lib.net.tms.mqtt3.IMqttSub3;
import home.lib.net.tms.mqtt3.MqttBroker3;
import home.lib.net.tms.mqtt3.MqttClient3;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

import java.util.*;



public class MqttTest3 {

	public static void main(String[] args) throws Exception {

		TmsSelector.debugLevel = 0xff;

		// MqttBroker3.debugLevel |= MyLogger.VERBOSE;
		// MqttBroker3.debugLevel |= MyLogger.EXCEPTION;
		// MqttBroker3.debugLevel |= MyLogger.DEBUG;

		// test2();//tiny.mqtt
		// test3();//mosquitto

		//System.out.println("mqtt max buff size= " + StringUtil.formatBytesSize(MqttMessage.MaxBufferLength));

		// test1();

		new MqttTest3().test3();

	}

	static long rxcnt = 0;
	static long rxcnts = 0;
	static int connectedcnt = 0;

	public static void test1() throws Exception {

		System.out.println("mqtt test");
		//
		MqttBroker3 brk = new MqttBroker3(1883);

		brk.begin();

		int a = 1;
		// while( a==1)
		{
			TimeUtil.sleep(3000);

		}

		int qos = 2;

		System.out.println("brk=" + brk.isAlive());

		for (int i = 0; i < 100; i++) {
			MqttClient3 mysub = new MqttClient3();

			mysub.setId("sub1" + System.nanoTime());
			mysub.connect("127.0.0.1", 1883, new IMqtt3() {

				Map<String, String> rx = new HashMap<String, String>();

				@Override
				public void onConnected(MqttClient3 source) {
					System.out.println("client conected " + source.getId() + "  " + (connectedcnt++));

					try {
						long pi = source.subscribe(("house/1"), qos, new IMqttSub3() {

							@Override
							public void onReceived(MqttClient3 source, String topic, byte[] payload) {

								// System.out.format("callback topic=[%s] payload=[%s] \r\n", topic, new
								// String(payload));
								rxcnt++;

								String rr = new String(payload);

								if (rx.containsKey(rr)) {
									System.out.println("same data receviced!!!!  " + rx.size() + "  " + rr);
									System.exit(0);
								}

								rx.put(rr, "");

								try {

									String s = String.format("from(%s) relay q=%s  %s", source.getId(),
											source.publishQueueSize(), new String(payload));

									source.publish("house/resp", s, 0);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}

							@Override
							public void subscribeSuccess(MqttClient3 source, String topic) {

								System.out.println("subscribed  " + source.getId() + "  " + topic);

							}
							// onSubackSuccess
							// onSubackFail

							@Override
							public void subscribeFailed(MqttClient3 source, String topic) {

								source.discon(false);

							}

						});

						System.out.println("client subscribed ");
					} catch (Exception e) {
						e.printStackTrace();
					}

				}

				@Override
				public void deliveryComplete(long pi, long past_ms) {

				}

				@Override
				public void onDisonnected(MqttClient3 source) {
					// TODO Auto-generated method stub

				}

			});

			// mysub.waitForConnack(8);

		} // for

		TimeUtil.sleep(100);

		MqttClient3 sub = new MqttClient3();

		sub.setId("sub1" + System.currentTimeMillis());
		sub.connect("127.0.0.1", 1883, new IMqtt3() {

			@Override
			public void onConnected(MqttClient3 source) {
				// TODO Auto-generated method stub

			}

			@Override
			public void deliveryComplete(long pi, long past_ms) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onDisonnected(MqttClient3 source) {
				// TODO Auto-generated method stub

			}

		});

		TimeUtil.sleep(100);

		sub.waitForConnack(8);

		MqttClient3 pub1 = new MqttClient3();

		pub1.setId("pubb1" + System.currentTimeMillis());
		pub1.connect("127.0.0.1", 1883, new IMqtt3() {

			@Override
			public void onConnected(MqttClient3 source) {
				// TODO Auto-generated method stub

			}

			@Override
			public void deliveryComplete(long pi, long past_ms) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onDisonnected(MqttClient3 source) {
				// TODO Auto-generated method stub

			}

		});

		pub1.waitForConnack(8);

		int cnt = 0;
		String t = "house/resp";

		TimeUtil aa = new TimeUtil();

		long txcnt = 0, txcnt1sec = 0;

		while (true) {
			TimeUtil.sleep(0);

			// mc.loop();
			// sub.loop();
			// brk.loop();
			String ss = "";

			try {
				ss = String.format("payload %s  qos0~  cnt=%s  queue.size=%s  pps=%s  tps=%s ", TimeUtil.now(), (cnt++),
						sub.pubQueueSize(), StringUtil.formatCount(rxcnts), StringUtil.formatCount(txcnt1sec));

				if (sub.isAlive() && sub.isConnected() && sub.publishQueueSize() < 10) {
					sub.publish(t, "sub1 " + ss, 2);
					txcnt++;
				}

				if (pub1.isAlive() && pub1.isConnected() && pub1.publishQueueSize() < 10) {
					pub1.publish(t, "pub1 " + ss, 2);
					txcnt++;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (aa.end_sec() > 1) {
				aa.start();
				System.out.println(brk);
				rxcnts = rxcnt;
				rxcnt = 0;
				txcnt1sec = txcnt;
				txcnt = 0;
			}

		}

		//
		// //
		// MqttClient mc = new MqttClient();
		//
		// mc.setId("client1");
		//
		// mc.connect("114.200.254.181", 1883).waitForConnack(3);
		//
		//
		// mc.publish("/hello", "payload~", 0);
		//
		// TimeUtil.sleep(1000);
		//
		// sub.unsubscribe( ("/hello"));
		//
		// sub.subscribe( "/hello2" , new IMqtt() {
		//
		// @Override
		// public void callback(MqttClient source, String topic, byte[] payload ) {
		// System.out.format("callback topic=[%s] payload=[%s] \r\n", topic ,
		// new String(payload));
		//
		// }
		// });

		// System.out.println("connect="+mc.connected() );

	}

	// //
	// //
	//
	// public static void test2() throws Exception {
	//
	// System.out.println("mqtt test");
	// //
	// MqttBroker3 brk = new MqttBroker3(1883);
	//
	// brk.begin();
	//
	// //
	// // byte qos=2;
	// // //
	// MqttClient3 sub = new MqttClient3();
	// //
	// sub.setId("sub1");
	// sub.connect("127.0.0.1", 1883).waitForConnack(3);
	// sub.subscribe("a/+/c", new IMqttSub3() {
	//
	// @Override
	// public void callback(MqttClient3 source, String topic, byte[] payload) {
	//
	// System.out.format("callback topic=[%s] payload=[%s] \r\n", topic,
	// new String(Arrays.copyOf(payload, payload.length)));
	//
	// }
	//
	// @Override
	// public void deliveryComplete(long pi, long past_ms) {
	// // TODO Auto-generated method stub
	//
	// }
	// });
	//
	// TimeUtil.sleep(1000);
	//
	// for (SocketChannel c : brk.getSelector().getChannels()) {
	//
	// brk.getSelector().channelClose(c);
	// }
	//
	// TimeUtil.sleep(1000);
	//
	// brk.close();
	//
	// TimeUtil.sleep(1000);
	//
	// System.out.println("brk recovering...");
	//
	// MqttBroker3 brk2 = new MqttBroker3(1883);
	//
	// brk2.begin();
	//
	// // sub.close();
	//
	// while (true) {
	// TimeUtil.sleep(3000);
	//
	// System.out.println(brk2);
	//
	// // mc.loop();
	// // sub.loop();
	// }
	//
	// //
	// //
	// //
	// // for(int i=0;i<100;i++) {
	// // MqttClient sub = new MqttClient();
	// // //
	// // sub.setId("sub-"+i);
	// // //
	// // // //mc.connect("114.200.254.181", 1883);
	// // sub.connect("127.0.0.1", 1883).waitForConnack(3);
	// //
	// // sub.subscribe(new Topic("a/+/c"),(byte)0, new IMqtt() {
	// //
	// // @Override
	// // public void callback(MqttClient source, Topic topic, byte[] payload, int payload_length) {
	// //
	// // System.out.format("callback topic=[%s] payload=[%s] \r\n", topic.str(),
	// // new String(Arrays.copyOf(payload, payload_length)));
	// //
	// // }
	// // });
	// // }
	//
	// // //
	// //// //
	// //// //
	// // MqttClient mc = new MqttClient();
	// // //
	// // mc.setId("client1");
	// // //
	// // // //mc.connect("114.200.254.181", 1883);
	// // mc.connect("127.0.0.1", 1883).waitForConnack(3);
	// //
	// // Topic t = new Topic("a/x/c");
	// // //mc.publish(t, "payload" + TimeUtil.now() + "qos0~", 0);
	// // // mc.publish(t, "payload" + TimeUtil.now() + "qos1~", 1);
	// // // mc.publish(t, "payload" + TimeUtil.now() + "qos2~", 2);
	// //
	// // mc.publish(t, "start~~~~~~~~~" , 2);
	// //
	// // long cnt=0;
	// // long sps=0;
	// // long bk_sps=0;
	// //
	// // TimeUtil aSec=new TimeUtil();
	// //
	// // while (true) {
	// // TimeUtil.sleep(3000);
	// //
	// // // mc.loop();
	// // // sub.loop();
	// // // brk.loop();
	// // String ss=String.format("payload %s qos0~ cnt=%s rcnt=%s sps= %s", TimeUtil.now(),(cnt++), "", bk_sps);
	// //
	// // mc.publish(t, ss , 2);
	// // sps++;
	// //
	// //
	// // if( aSec.end_sec()>1.0) {
	// // aSec.start();
	// //
	// // bk_sps=sps;
	// // sps=0;
	// // }
	// //
	// // }
	// ////
	//
	// }
	//
	// public static void test3() throws Exception {
	//
	// System.out.println("mqtt test");
	//
	// //
	// //
	// MqttClient3 mc = new MqttClient3();
	//
	// mc.setId("pub1");
	//
	// mc.connect("114.200.254.181", 1883).waitForConnack(3);
	// //
	// String t = ("a/b/c");
	// // mc.publish(t, "payload" + TimeUtil.now() + "qos0~", 0);
	// // mc.publish(t, "payload" + TimeUtil.now() + "qos1~", 1);
	//
	// ///
	// //
	// //
	// MqttClient3 sub = new MqttClient3();
	// //
	// sub.setId("sub1");
	// sub.connect("114.200.254.181", 1883).waitForConnack(3);
	// sub.subscribe(("a/+/c"), new IMqttSub3() {
	//
	// @Override
	// public void callback(MqttClient3 source, String topic, byte[] payload) {
	//
	// System.out.format("callback topic=[%s] payload=[%s] \r\n", topic, new String(payload));
	//
	// }
	//
	// @Override
	// public void deliveryComplete(long pi, long past_ms) {
	// // TODO Auto-generated method stub
	//
	// }
	// });
	// //
	//
	// while (true) {
	// TimeUtil.sleep(3000);
	//
	// // mc.publish(t, "payload" + TimeUtil.now() + "qos1~", 1);
	//
	// // mc.publish(t, "payload" + TimeUtil.now() + "qos2~", 2);
	//
	// // mc.loop();
	// // sub.loop();
	// // brk.loop();
	// try {
	//
	// mc.publish(t, "payload" + TimeUtil.now() + "qos0~", 0);
	//
	// } catch (Exception ex) {
	// ex.printStackTrace();
	// }
	//
	// }
	//
	// }
	//
	// public static void test4() throws Exception {
	//
	// System.out.println("mqtt test");
	//
	// //
	// MqttClient3 sub = new MqttClient3();
	// //
	// sub.setId("subJ");
	// sub.connect("114.200.254.181", 1883);
	//
	// // TimeUtil.sleep(100);
	// sub.subscribe(("/hello"), new IMqttSub3() {
	//
	// @Override
	// public void callback(MqttClient3 source, String topic, byte[] payload) {
	//
	// System.out.format("callback topic=[%s] payload=[%s] \r\n", topic, new String(payload));
	//
	// }
	//
	// @Override
	// public void deliveryComplete(long pi, long past_ms) {
	// // TODO Auto-generated method stub
	//
	// }
	// });
	//
	// //
	// //
	// //
	//
	// while (true) {
	// TimeUtil.sleep(100);
	//
	// // mc.loop();
	// // sub.loop();
	// // sub.loop();
	//
	// }
	//
	// }
//
//	class Response {
//		TimeUtil past = new TimeUtil();
//		byte[] val = null;
//		String key = null;
//
//		public Response(String k, byte[] v) {
//			val = v;
//			key = k;
//		}
//	};
//
//	Map<String, Response> m_response = new HashMap<String, Response>();
//
//	public void test2() throws Exception {
//
//		System.out.println("mqtt test");
//		//
//		MqttBroker3 brk = new MqttBroker3(1883);
//
//		brk.begin();
//
//		int a = 1;
//		// while( a==1)
//		{
//			TimeUtil.sleep(3000);
//
//		}
//
//		int qos = 2;
//
//		System.out.println("brk=" + brk.isAlive());
//
//		for (int i = 0; i < 1; i++) {
//			MqttClient3 mysub = new MqttClient3();
//
//			mysub.setId("sub1" + System.nanoTime());
//			mysub.connect("127.0.0.1", 1883, new IMqtt3() {
//
//				@Override
//				public void onConnected(MqttClient3 source) {
//					System.out.println("client conected " + source.getId() + "  " + (connectedcnt++));
//
//					try {
//						long pi = source.subscribe(("callmethod/#"), qos, new IMqttSub3() {
//
//							@Override
//							public void onReceived(MqttClient3 source, String topic, byte[] payload) {
//
//								// System.out.format("callback topic=[%s] payload.len=%d \r\n", topic, payload.length);
//								String[] tl = topic.split("/");
//
//								try {
//
//									String method = tl[tl.length - 2];
//									String ns = tl[tl.length - 1];
//									Object args[] = (Object[]) bytes2obj(payload);
//
//									Object r = doMethod(new MqttTest3(), method, args);
//
//									byte[] rbs = obj2bytes(r);
//									// String s=String.format("from(%s) relay q=%s %s", source.getId(),
//									// source.publishQueueSize(), new String(payload) );
//
//									source.publish(topic + "/resp", rbs, 2);
//								} catch (Exception e) {
//									// TODO Auto-generated catch block
//									e.printStackTrace();
//								}
//
//							}
//
//							@Override
//							public void subscribeSuccess(MqttClient3 source, String topic) {
//
//								System.out.println("subscribed  " + source.getId() + "  " + topic);
//
//							}
//							// onSubackSuccess
//							// onSubackFail
//
//							@Override
//							public void subscribeFailed(MqttClient3 source, String topic) {
//
//								source.discon(false);
//
//							}
//
//						});
//
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//
//				}
//
//				@Override
//				public void deliveryComplete(long pi, long past_ms) {
//
//				}
//
//				@Override
//				public void onDisonnected(MqttClient3 source) {
//					// TODO Auto-generated method stub
//
//				}
//
//			});
//
//			// mysub.waitForConnack(8);
//
//		} // for
//
//		TimeUtil.sleep(100);
//
//		MqttClient3 pub = new MqttClient3();
//
//		pub.setId("pub" + System.currentTimeMillis());
//		pub.connect("127.0.0.1", 1883, new IMqtt3() {
//
//			@Override
//			public void onConnected(MqttClient3 source) {
//				System.out.println("client conected " + source.getId() + "  " + (connectedcnt++));
//
//				try {
//					long pi = source.subscribe(("callmethod/#"), qos, new IMqttSub3() {
//
//						@Override
//						public void onReceived(MqttClient3 source, String topic, byte[] payload) {
//
//							// System.out.format("callback topic=[%s] payload.len=%d \r\n", topic, payload.length);
//							String[] tl = topic.split("/");
//
//							try {
//
//								String method = tl[tl.length - 3];
//								String ns = tl[tl.length - 2];
//								String resp = tl[tl.length - 1];
//
//								synchronized (m_response) {
//									for (Response r : m_response.values()) {
//										if (r.past.end_sec() > 30) {
//											m_response.remove(r.key);
//											r.val = null;
//										}
//									}
//								} // sync
//
//								m_response.put(ns, new Response(ns, payload));
//
//								//System.out.format("m(%s) r(%s) ns(%s)  \n", method, resp, ns);
//								// Object args[] = (Object[]) bytes2obj(payload);
//
//								// doMethod(new MqttTest3(), method, args);
//
//								// String s=String.format("from(%s) relay q=%s %s", source.getId(),
//								// source.publishQueueSize(), new String(payload) );
//
//								// source.publish("house/resp", s, 0);
//							} catch (Exception e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//
//						}
//
//						@Override
//						public void subscribeSuccess(MqttClient3 source, String topic) {
//
//							System.out.println("subscribed  " + source.getId() + "  " + topic);
//
//						}
//						// onSubackSuccess
//						// onSubackFail
//
//						@Override
//						public void subscribeFailed(MqttClient3 source, String topic) {
//
//							source.discon(false);
//
//						}
//
//					});
//
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//
//			}
//
//			@Override
//			public void deliveryComplete(long pi, long past_ms) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void onDisonnected(MqttClient3 source) {
//				System.out.println("client disconected " + source.getId() + "  " + (connectedcnt--));
//
//			}
//
//		});
//
//		// pub.waitForConnack(3);
//		int index = 0;
//
//		TimeUtil aa = new TimeUtil();
//
//		while (true) {
//			TimeUtil.sleep(10);
//
//			// mc.loop();
//			// sub.loop();
//			// brk.loop();
//			// String ss = "";
//
//			String s = "message " + (index++);
//
//			try {
//				if (pub.isAlive() && pub.isConnected() && pub.publishQueueSize() < 10) {
//
//					// byte[] b = obj2bytes(s);
//					// pub.publish("callmethod/hello", b, b.length, qos);
//
//					String r = (String) callMethod(pub, "callmethod/hello", 1000, s);
//
//					System.out.println("resp=" + r);
//
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//
//		}
//
//		//
//		// //
//		// MqttClient mc = new MqttClient();
//		//
//		// mc.setId("client1");
//		//
//		// mc.connect("114.200.254.181", 1883).waitForConnack(3);
//		//
//		//
//		// mc.publish("/hello", "payload~", 0);
//		//
//		// TimeUtil.sleep(1000);
//		//
//		// sub.unsubscribe( ("/hello"));
//		//
//		// sub.subscribe( "/hello2" , new IMqtt() {
//		//
//		// @Override
//		// public void callback(MqttClient source, String topic, byte[] payload ) {
//		// System.out.format("callback topic=[%s] payload=[%s] \r\n", topic ,
//		// new String(payload));
//		//
//		// }
//		// });
//
//		// System.out.println("connect="+mc.connected() );
//
//	}
//
//	public String hello(String s) {
//		System.out.format("hello(%s)\n", s);
//
//		return "resp" + s;
//	}
//
//	static Object doMethod(Object cls, String methodName, Object... args) throws Exception {
//
//		Class c = cls.getClass();
//
//		Method m = null;
//
//		if (args != null && args.length != 0) {
//
//			Class[] cArg = new Class[args.length];
//			for (int h = 0; h < cArg.length; h++) {
//
//				Class ac = args[h].getClass();
//
//				if (ac.equals(Long.class))
//					cArg[h] = long.class;
//				else if (ac.equals(Integer.class))
//					cArg[h] = int.class;
//				else if (ac.equals(Double.class))
//					cArg[h] = double.class;
//				else if (ac.equals(Byte.class))
//					cArg[h] = byte.class;
//				else if (ac.equals(Short.class))
//					cArg[h] = short.class;
//				else
//					cArg[h] = args[h].getClass();
//
//			}
//
//			m = c.getMethod(methodName, cArg);
//		} else
//			m = c.getMethod(methodName, null);
//
//		return m.invoke(cls, args);
//
//	}
//
//	/**
//	 * 
//	 * @param object
//	 * @return
//	 * @throws IOException
//	 */
//	public static byte[] obj2bytes(Object object) throws IOException {
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		ObjectOutput out = new ObjectOutputStream(bos);
//		out.writeObject(object);
//		return bos.toByteArray();
//
//	}
//
//	/**
//	 * 
//	 * @param bytes
//	 * @return
//	 * @throws IOException
//	 * @throws ClassNotFoundException
//	 */
//	public static Object bytes2obj(byte[] bytes) throws IOException, ClassNotFoundException {
//		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
//		ObjectInput in = new ObjectInputStream(bis);
//		return in.readObject();
//
//	}
//
//	synchronized Object callMethod(MqttClient3 cli, String topic, long timeout, Object... args) throws Exception {
//
//		String ns = "" + System.nanoTime();
//
//		// String t = topic;// + "/" + methodName + "/" + ns;
//		String t = topic + "/" + ns;
//
//		byte[] b = obj2bytes(args);
//		cli.publish(t, b, b.length, 2);
//
//		TimeUtil to = new TimeUtil();
//
//		while (to.end_ms() < timeout) {
//			TimeUtil.sleep(10);
//
//			synchronized (m_response) {
//
//				Response r = m_response.remove(ns);
//
//				if (r != null) {
//					if (r.val == null)
//						return null;
//
//					return bytes2obj(r.val);
//				}
//			} // sync
//
//		}
//
//		return null;
//
//	}
	//
	// public Object callMethod(String to, String method, Object... args) throws Exception {
	// return callMethod(to, (long) 8000, method, args);
	// }
	
	
public String hello(String s) {
	System.out.format("hello(%s)\n", s);

	return "resp" + s;
}
	
	public void test3() throws Exception {

		System.out.println("mqtt test");
		//
		MqttBroker3 brk = new MqttBroker3(1883);

		brk.begin();

		int a = 1;
		// while( a==1)
		{
			TimeUtil.sleep(3000);

		}

		int qos = 2;

		System.out.println("brk=" + brk.isAlive());
		
		
		//public MySocketWrap(TmsBroker br, String id, String ip, int port, IMySocketListener ltr) throws Exception {
			
		
		MySocketSup msk=new MySocketSup(null,"hello","127.0.0.1", 1883,new IMySocketSupListener() {

			@Override
			public MqBundle actionPerformed(MySocketSup ms, MqBundle e) {

				System.out.format(" actionPerformed \n", e.getString("say"));
				e.setString("res","goodbye");
				return e;
			}

			@Override
			public void log(MySocketSup ms, Exception e) {
				System.out.format(" %s \n", UserException.getStackTrace(e));
				
			}

			@Override
			public void log(MySocketSup ms, int level, String s) {
				System.out.format(" %s  \n",s);
				
			}

			@Override
			public void connected(MySocketSup ms) {
				System.out.format("connected  \n");
				
			}

			@Override
			public void disconnected(MySocketSup ms) {
				System.out.format("disconnected  \n");
				
			}} );
		
		msk.setLinkedClassHandle(new MqttTest3());
		msk.connect(8000, true);

		


		MySocketSup msk2=new MySocketSup(null,"helloworld","127.0.0.1", 1883,new IMySocketSupListener() {

			@Override
			public MqBundle actionPerformed(MySocketSup ms, MqBundle e) {

				System.out.format(" actionPerformed %s\n", e.getString("say"));
				e.setString("res","goodbye");
				return e;
			}

			@Override
			public void log(MySocketSup ms, Exception e) {
				System.out.format(" %s \n", UserException.getStackTrace(e));
				
			}

			@Override
			public void log(MySocketSup ms, int level, String s) {
				System.out.format(" %s \n",s);
				
			}

			@Override
			public void connected(MySocketSup ms) {
				System.out.format("connected  \n");
				
			}

			@Override
			public void disconnected(MySocketSup ms) {
				System.out.format("disconnected  \n");
				
			}} );
		
		msk2.setLinkedClassHandle(new MqttTest3());
		msk2.connect(8000, true);

		
		

		MySocketSup msk3=new MySocketSup(null,"sample","127.0.0.1", 1883,new IMySocketSupListener() {

			@Override
			public MqBundle actionPerformed(MySocketSup ms, MqBundle e) {

				System.out.format(" actionPerformed %s \n", e.getString("say"));
				e.setString("res","goodbye");
				return e;
			}

			@Override
			public void log(MySocketSup ms, Exception e) {
				System.out.format(" %s \n", UserException.getStackTrace(e));
				
			}

			@Override
			public void log(MySocketSup ms, int level, String s) {
				System.out.format(" %s \n",s);
				
			}

			@Override
			public void connected(MySocketSup ms) {
				System.out.format("connected  \n");
				
			}

			@Override
			public void disconnected(MySocketSup ms) {
				System.out.format("disconnected  \n");
				
			}} );
		
		msk3.setLinkedClassHandle(new MqttTest3());
		msk3.connect(8000, true);
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		

		// pub.waitForConnack(3);
		int index = 0;

		TimeUtil aa = new TimeUtil();

		while (true) {
			TimeUtil.sleep(100);

			// mc.loop();
			// sub.loop();
			// brk.loop();
			// String ss = "";

			String s = "message " + (index++);

			try {
				if (msk.isAlive() ) {

					// byte[] b = obj2bytes(s);
					// pub.publish("callmethod/hello", b, b.length, qos);

					TimeUtil t=new TimeUtil();
					String r = (String)msk.callMethod( "helloworld","hello", s);

					System.out.println("resp=" + r+"  spend="+t.end_ms() );
					
					
					MqBundle m=new MqBundle();
					m.setString("say", "hi");
					msk.sendBroadcast(m);
					
					
					MqBundle rr=msk2.sendTo("hello", m, 1000);
					
					System.out.println( "rr="+rr );
					

				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		//
		// //
		// MqttClient mc = new MqttClient();
		//
		// mc.setId("client1");
		//
		// mc.connect("114.200.254.181", 1883).waitForConnack(3);
		//
		//
		// mc.publish("/hello", "payload~", 0);
		//
		// TimeUtil.sleep(1000);
		//
		// sub.unsubscribe( ("/hello"));
		//
		// sub.subscribe( "/hello2" , new IMqtt() {
		//
		// @Override
		// public void callback(MqttClient source, String topic, byte[] payload ) {
		// System.out.format("callback topic=[%s] payload=[%s] \r\n", topic ,
		// new String(payload));
		//
		// }
		// });

		// System.out.println("connect="+mc.connected() );

	}

}
