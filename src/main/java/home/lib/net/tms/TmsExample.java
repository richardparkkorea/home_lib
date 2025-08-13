package home.lib.net.tms;

import java.util.Random;
import java.util.TimerTask;

import javax.swing.JFrame;

import home.lib.lang.Timer2;
import home.lib.log.FrmDbg;
import home.lib.net.tms.TmsChannel.Qos;
import home.lib.util.TimeUtil;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JCheckBox;

public class TmsExample extends JFrame {

	// public static void main(String[] args) {
	//
	// TmsExample te = new TmsExample();
	//
	// te.setVisible(true);
	//
	// new Thread(new Runnable() {
	// public void run() {
	// te.start(args);
	// }
	// }).start();
	//
	// }
	//
	// JLabel lblNewLabel;
	// JCheckBox chkRandomeDisconnect;
	//
	// public TmsExample() {
	//
	// JPanel panel = new JPanel();
	// getContentPane().add(panel, BorderLayout.CENTER);
	// panel.setLayout(new BorderLayout(0, 0));
	//
	// lblNewLabel = new JLabel("New label");
	// panel.add(lblNewLabel, BorderLayout.CENTER);
	//
	// chkRandomeDisconnect = new JCheckBox("random disconnect");
	// panel.add(chkRandomeDisconnect, BorderLayout.NORTH);
	//
	// this.setPreferredSize(new Dimension(100, 100));
	// this.setSize(new Dimension(274, 265));
	// }
	//
	// public long eventRxCount = 0;
	//
	// public void start(String[] args) {
	//
	//
	//
	//
	// TimeUtil past = new TimeUtil();
	// System.out.println("nq test start");
	//
	// if( args.length==0) {
	//
	//
	// System.out.println("java -cp jxcopy.jar home.lib.net.tms.TmsExample -ip=127.0.0.1 -port=12345 -cnt=100 ");
	// System.exit(0);
	// }
	//
	//
	// int h=0;
	// String ip="127.0.0.1";
	// int port=12345;
	// int cnt=10;
	//
	// for (String s : args) {
	// s = s.trim();
	// System.out.format("[%s] (%s) \r\n", h++, s);
	//
	//
	//
	// if( s.indexOf("-ip")==0) {
	// ip=s.split("=")[1].trim();
	// }
	//
	// else if( s.indexOf("-port")==0) {
	// port = Integer.valueOf(s.split("=")[1].trim());
	// }
	//
	// else if( s.indexOf("-cnt")==0) {
	// cnt = Integer.valueOf(s.split("=")[1].trim());
	// }
	//
	//
	// } // for
	//
	//
	//
	// test();
	//
	// try {
	// TmsBroker tf = new TmsBroker("local", "0.0.0.0", port).waitBind(10);
	// tf.jobOpt(8, 6000);
	// tf.jobPeekCount(8);
	// tf.setReceiveTimeoutSec(60);
	//
	//
	//
	// long aid = tf.connectw(ip,port, true, "/tms/a", null, "id", "pwd", 10000).get().getId();
	// tf.connectw(ip,port, true, "/tms/b", null, "id", "pwd", 10000).get();
	// tf.connectw(ip,port, true, "/tms/c", null, "id", "pwd", 10000).get();
	//
	// long nid[] = new long[cnt];
	// for (int i = 0; i < nid.length; i++) {
	//
	// try {
	// // nid[i] = tf.connectw("127.0.0.1", 12345, true, "/tms/" + i, 10000).get();
	// nid[i] = tf.connect(ip,port, true, "/tms/" + i, null, "id", "pwd", null,null).getId();
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// TimeUtil.sleep(1);
	//
	// }
	//
	// // TmsConnector tcA = tf.get(aid);
	// new Timer2().schedule(new TimerTask() {
	// public void run() {
	// String str = ("time[" + TimeUtil.now());
	//
	// try {
	// // tf.send("/tms/a", "/tms/b", str.getBytes());
	//
	// long t1 = System.currentTimeMillis();
	//
	// TmsChannel tc = tf.get(aid);
	// if (tc != null) {
	// byte[] rr = tc.get("/tms/b", str.getBytes());
	//
	// // tc.waitReturn(sidx, 10000);
	// if (rr != null) {
	// long t2 = System.currentTimeMillis();
	// // System.out.format("get ret in %s ms (%s) \r\n", (t2 - t1), new String(rr));
	// } else {
	// long t2 = System.currentTimeMillis();
	// // System.out.format("get ret fail! %s ms \r\n", (t2 - t1));
	// }
	// }
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// // TODO Auto-generated catch block
	// // e.printStackTrace();
	// }
	// }
	// }, 1000, 10);
	//
	// new Timer2().schedule(new TimerTask() {
	// Random rand = new Random();
	// TimeUtil t=new TimeUtil();
	//
	// public void run() {
	// String str = ("time[" + TimeUtil.now());
	//
	// try {
	// for (int i = 0; i < 1; i++) {
	// TmsChannel tc = tf.get(nid[i]);
	// if (tc != null) {
	// long sidx = tc.send("*", str.getBytes(), Qos.AtLeastOnce);
	//
	// }
	//
	// //System.out.format( " s=%s r=%s \r\n", chkRandomeDisconnect.isSelected(), (rand.nextInt()%100) );
	// if (chkRandomeDisconnect.isSelected() && t.end_sec()>6) {
	// t.start();
	// int n = (rand.nextInt() % (nid.length));
	//
	// if (n > 10) {
	// System.out.format("close ch=%s \r\n", n );
	// tf.close(nid[n]);
	// }
	//
	// }
	// }
	//
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// // e.printStackTrace();
	// }
	// }
	// }, 1000, 1);
	//
	// new Timer2().schedule(new TimerTask() {
	// public void run() {
	// String str = ("time[" + TimeUtil.now());
	//
	// try {
	// for (int i = 3; i <= 3; i++) {
	// TmsChannel tc = tf.get(nid[i]);
	// if (tc != null) {
	// long sidx = tc.send("*", str.getBytes());
	// }
	// }
	//
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// // e.printStackTrace();
	// }
	// }
	// }, 1000, 1);
	//
	// new Timer2().schedule(new TimerTask() {
	// public void run() {
	//
	// try {
	// // tf.close("/tms/a");
	// // tf.send("/tms/a", "/tms/b", str.getBytes());
	// } catch (Exception e) {
	// }
	// }
	// }, 32000, 32000);
	//
	// TimeUtil t0 = new TimeUtil();
	//
	// while (tf.isAlive()) {
	//
	// TmsEvent e;
	// while ((e = tf.pollEvent()) != null) {
	// eventRxCount++;
	//
	// try {
	//
	// e.ch.setLoggedIn(true);
	//
	// if (e.event == 'e') { // login
	//
	// // e.ch.setLoggedIn(true);
	//
	// System.out.format("%s] path[%s] evt(%s) msg(%s) \r\n", TimeUtil.now(), e.ch.getPath(),
	// e.event, new String(e.rxd));
	//
	// } else if (e.event == 'l') { // login
	//
	// e.ch.setLoggedIn(true);
	//
	// System.out.format("%s] path[%s] evt(%s) idname(%s) pwd(%s) \r\n", TimeUtil.now(),
	// e.ch.getPath(), e.event, e.ch.getIdName(), e.ch.getPwd());
	//
	// } else if (e.event == 'r') {
	//
	// // if (e.item.getTo().indexOf("*") == -1) {
	// if (e.item.isAskReturn()) {
	// // System.out.format("%s] path[%s] e(%s) rxd.len(%s) from(%s) to(%s) %s \r\n",
	// // TimeUtil.now(), e.ch.getPath(), e.event, e.rxd.length, e.item.from, e.item.to,
	// // new String(e.rxd));
	//
	// e.ch.sendReturn(e.item, TimeUtil.now().getBytes());
	// }
	//
	// // String str = ("res>" + new String(e.rxd));
	// // tf.send("/tms/con2", "*", str.getBytes());
	//
	// } else
	// System.out.format("%s] path[%s] evt(%s) \r\n", TimeUtil.now(), e.ch.getPath(),
	// e.event);
	//
	// } catch (Exception exx) {
	// exx.printStackTrace();
	// }
	// }
	//
	// if (t0.end_sec() > 1) {
	// t0.start();
	//
	// // System.out.println(" event count = " + tf.getEventCount());
	// // System.out.println(tf);
	// // System.out.format("erxCount=%d \r\n", (int) (eventRxCount / past.end_sec()));
	//
	// try {
	//
	// lblNewLabel.setText("<html>" + tf.toString().replace(")", ")<br>"));
	// } catch (Exception ex0) {
	//
	// }
	//
	// System.gc();
	//
	// }
	//
	// if (tf.getEventCount() == 0) {
	// Thread.sleep(1);
	// }
	// }
	//
	// } catch (Exception ex) {
	// // TODO Auto-generated catch block
	// ex.printStackTrace();
	// }
	//
	// System.out.println("nq test end");
	//
	// }
	//
	// /**
	// *
	// */
	// public static void test() {
	//
	// int i;
	// byte[] bb = new byte[200];
	// for (i = 0; i < bb.length; i++) {
	// bb[i] = (byte) i;
	// }
	//
	// long start = System.nanoTime();
	//
	// for (i = 0; i < 100000; i++) {
	//
	// byte[] aa = TmsDlePacket.encode(bb);
	//
	// byte[] cc = TmsDlePacket.decode(aa, aa.length);
	//
	// }
	//
	// long end = System.nanoTime();
	//
	// System.out.format("time = %d ms\r\n", (end - start) / 1000000);
	//
	// // System.exit(0);
	//
	// }

}
