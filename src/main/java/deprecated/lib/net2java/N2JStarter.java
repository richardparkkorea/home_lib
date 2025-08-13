package deprecated.lib.net2java;

import home.lib.lang.Application;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Deprecated
public class N2JStarter implements Runnable {

	String libFileName = null;
	boolean m_isAlive = false;

	String m_id = null;
	String m_ip = null;
	String m_port = null;
	int m_debugLevel = 0;

	public N2JStarter(String fn, String id, String ip, String port, int dl) {
		libFileName = fn;
		m_id = id;
		m_ip = ip;
		m_port = port;
		m_debugLevel = dl;
		
		
		
		new Thread(this).start();

		// if (libname != null) {
		// new Thread(new WinExec(libname)).start();
		// // new Thread(new WinExec()).start();
		// Thread.sleep(10);
		// }

	}

	public boolean isAlive() {
		return m_isAlive;
	}

	public void run() {

		while (true) {

			ProcessBuilder builder;
			try {
				Thread.sleep(100);
				m_isAlive = true;
				System.out.println("N2JStarter start!");

				List<String> command = new ArrayList<String>();
				command.add(libFileName);
				command.add(m_id);
				command.add(m_ip);
				command.add("" + m_port);
				command.add("" + Application.getProcessId(""));
				command.add("" + m_debugLevel);

				builder = new ProcessBuilder(command);
				Map<String, String> environ = builder.environment();

				Process process = builder.start();
				//
				// Runnable closeChildThread = new Runnable() {
				// public void run() {
				// System.out.println("close child process");
				// process.destroy();
				//
				// }
				// };
				//
				// Runtime.getRuntime().addShutdownHook(new Thread(closeChildThread));

				InputStream is = process.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
				System.out.println("N2JStarter terminated!");

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				m_isAlive = false;
			}
		}// while
	}
}