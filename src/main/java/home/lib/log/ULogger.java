package home.lib.log;

import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import home.lib.lang.Timer2;
import home.lib.lang.Timer2Task;
import home.lib.lang.UserException;

import home.lib.util.TimeUtil;


@Deprecated 
final public class ULogger implements ILogger {

	final public static int LOG = 10;
	final public static int DEBUG = 10;
	final public static int WARNING = 20;
	final public static int EXCEPTION = 30;
	final public static int VERBOSE = 40;

	private DatagramSocket m_ds = null;

	private String m_ip = "127.0.0.1";// debug ip

	// private int m_dport = 1234;// debug port
	private Map<Integer, String> m_ports = new HashMap<Integer, String>();

	private String m_from = "";

	private Gson m_gson = new GsonBuilder().create();

	private String m_encode = null;// "utf-8";

	private Map<Socket, String> m_tcpClient = new HashMap<Socket, String>();

	private Timer2 m_timer = null;

	/**
	 * 
	 * 
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		int dport = 1234;
		boolean udp_debugger = false;
		

		if (args.length == 0) {
			System.out.println("udp debugger");
			System.out.println("java -cp jxcopy.jar home.lib.log.ULogger -udbg=1234 -f=\" or +and -deny \" ");
			System.out.println("java -cp jxcopy.jar home.lib.log.ULogger -udbgui ");
			// System.exit(0);
		}

		String filter="";
		// System.out.format("args.length=%d \r\n", args.length);
		int i = 0;
		for (String s : args) {
			s = s.trim();
			System.out.format("[%s] (%s) \r\n", i++, s);

			if (s.indexOf("-udbgui") == 0) {

				System.out.println("start frmDbg");
				
				FrmDbg frame = new FrmDbg();
				
				frame.setVisible(true);

				return;

			} else if (s.indexOf("-udbg=") == 0) {

				udp_debugger = true;
				dport = Integer.valueOf(s.split("=")[1].trim());

			} else if (s.indexOf("-f=") == 0) {
				
				filter= s.substring(3);

			} 

		} // for

		if (udp_debugger) {

			System.out.format("start udp svr port=%s \n", dport);

			new MyLoggerReceiver().start( null,dport, filter ).get();
			//

			return;
		}

		// test
		
		System.out.println("start frmDbg");
		FrmDbg frame = new FrmDbg();
		frame.setVisible(true);
		
		

		ULogger ul = new ULogger(123);
		MyLoggerReceiver ur=new MyLoggerReceiver();
		ur.start("127.0.0.1", 123, filter);
		ur.addListener(new MyLoggerListener() {

			@Override
			public void ulogReceive(Object dp, String when, String form, int level, String str) {

				System.out.println( str );
				
			}

			@Override
			public void ulogClosed(Object o) {
				// TODO Auto-generated method stub
				
			}
			
		});

		for (i = 0; i < 10; i++) {
			ul.l("ulog(tcp mode)= %s ", TimeUtil.now());
			TimeUtil.sleep(1000);
		}
		ur.close();
		TimeUtil.sleep(100);
		
		System.out.println("client.cnt="+ul.getTcpClientCount() );
		
		

		System.out.println("end. ");
		//System.exit(0);

	}

	/**
	 * return new isntance<br>
	 * it will ignore port number 0
	 * 
	 * @param ip
	 * @param port
	 * @return
	 */
	public static ULogger _new(String ip, int port) {
		try {
			// return new ULogger(ip, 0);
			return new ULogger(ip, port);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * it will ignore port number 0
	 * 
	 * @param ip
	 * @param port
	 * @return
	 * @throws Exception
	 */
	public ULogger(String ip, int port) throws Exception {

		m_ip = ip;

		if (port != 0) {
			addPort(port);
		}

		if (m_ds == null) {
			m_ds = new DatagramSocket();
		}

		// return this;
	}

	public int getTcpClientCount() {
		return m_tcpClient.size();
	}

	public ULogger(int port) throws Exception {

		m_timer = new Timer2().schedule(new Timer2Task() {

			@Override
			public void start(Timer2 tmr) {

			}

			@Override
			public void run(Timer2 tmr) {

				try {
					ServerSocket serverSocket = new ServerSocket(port);

					while (true) {
						Socket socket = serverSocket.accept();

						new ClientThread(socket);

					} // while

				} catch (Exception e) {
					e.printStackTrace();
				}

			}

			@Override
			public void stop(Timer2 tmr) {

			}

		}, 10);

	}

	/**
	 * 
	 * @author richard
	 *
	 */
	private class ClientThread {
		public ClientThread(Socket socket) {

			m_timer = new Timer2().schedule(new Timer2Task() {

				@Override
				public void start(Timer2 tmr) {

				}

				@Override
				public void run(Timer2 tmr) {

					System.out.println("ulogger] start new client thread ");
					byte[] buf = new byte[256];
					try {
						synchronized (m_tcpClient) {
							m_tcpClient.put(socket, TimeUtil.now());
						} // sync

						try {
							InputStream is = socket.getInputStream();
							while (true) {

								int n = is.read(buf);
								if (n < 0) {
									socket.close();
									return;
								}

								TimeUtil.sleep(100);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}

					} finally {

						synchronized (m_tcpClient) {
							m_tcpClient.remove(socket);
						} // sync
					}

				}

				@Override
				public void stop(Timer2 tmr) {

				}

			}, 10);

		}
	}

	
	/**
	 * 
	 * @param e
	 * @return
	 */
	public ULogger setEncode(String e) {
		m_encode = e;
		return this;
	}

	/**
	 * port range( 0 ~ 65535 )
	 * 
	 * @param port
	 */
	public void addPort(int port) {
		synchronized (m_ports) {

			if (0 <= port && port < 65535) {
				if (m_ports.containsKey(port) == false) {
					m_ports.put(port, "ok");
				}
			}
		} // sync
	}

	public void removePort(int port) {
		synchronized (m_ports) {
			m_ports.remove(port);
		} // sync
	}

	/**
	 * 
	 * 
	 * @param level
	 * @param format
	 * @param args
	 */
	public int udp(String from, int level, String format, Object... args) {

		int sendCount = 0;

		
		 

		try {

			String s = "";

			if (args == null || args.length == 0) {
				s = format;
			} else {
				s = String.format(format, args);
			}

			ULoggerData d = new ULoggerData();

			d.level = level;// normal
			d.from = from;
			d.str = s;
			d.when = TimeUtil.now();

			String str = m_gson.toJson(d);

			// String str=s;

			InetAddress iaddr = InetAddress.getByName(m_ip);

			synchronized (m_ports) {
				for (int p : m_ports.keySet()) {

					byte[] bs = null;

					if (m_encode == null) {
						bs = str.getBytes();
					} else {
						bs = str.getBytes(m_encode);
					}

					DatagramPacket dp = new DatagramPacket(bs, bs.length, iaddr, p);

					if (m_ds != null) {
						m_ds.send(dp);
					sendCount++;
					}
				} // for
			} // sync

			//
			//
			synchronized (m_tcpClient) {
				for (Socket sk : m_tcpClient.keySet()) {

					try {
						sk.getOutputStream().write(str.getBytes());
					} catch (Exception e) {
						;
					}

				} // for

			} // sync

		} catch (Exception e) {
			e.printStackTrace();
		}

		// Log.fl(format, args);

		return sendCount;

	}

	/**
	 * 
	 */
	public int l(String fmt, Object... args) {
		try {
			return udp(m_from, LOG, fmt, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;

	}

	public int l(int level, String from, String fmt, Object... args) {
		try {
			return udp(from, level, fmt, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;

	}

	/**
	 * 
	 */
	public int e(Exception e) {

		return udp(m_from, EXCEPTION, UserException.getStackTrace(e));
	}

	public int e(int level, String from, Exception e) {

		return udp(from, EXCEPTION, UserException.getStackTrace(e));
	}

	/**
	 * 
	 * 
	 * @param prefix
	 * @return
	 */
	public ULogger setFrom(String from) {
		m_from = from;
		return this;

	}

}
