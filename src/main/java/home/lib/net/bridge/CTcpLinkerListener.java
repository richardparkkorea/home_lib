package home.lib.net.bridge;

public class CTcpLinkerListener {

	public CBundle actionPerformed(CBundle e)   {
		return null;
	}
	public CBundle actionPerformed(byte[] buf,int len)  {
		return null;
	}

	public void log(Exception e) {
		e.printStackTrace();
	}

	public void log(int level, String s) {
		System.out.println(s);
	}

	public void startUp() {
	}

	public void finishUp() {
	}

	 
}
