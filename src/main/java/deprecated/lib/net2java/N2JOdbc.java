package deprecated.lib.net2java;

import java.util.Random;

import home.lib.net.bridge.CBundle;
import home.lib.net.bridge.CTcpLinker;
import home.lib.net.bridge.CTcpLinkerListener;

@Deprecated
public class N2JOdbc {

	public long m_timeout = 10 * 1000;

	CTcpLinker tcpl;
	String m_targetId = "net2java";
	String m_ip = null;
	int m_port = 0;

	 
	public N2JOdbc(String myid, String tid, String ip, int port) throws Exception {

		 
		try {

			m_ip = ip;
			m_port = port;
			m_targetId = tid;
			//
			//
			tcpl = new CTcpLinker(myid, ip, port);// ("main", "127.0.0.1", port);
			tcpl.setActionListener(new EventClientActionListener());
			// m_eLinker.start();
			tcpl.setMaxBuffer(1024 * 512);
			tcpl.connect();
			tcpl.register();
			tcpl.setResendInterval(1000);
			tcpl.sustainConnection(true);

			// System.out.println("created link");

		} catch (Exception e) {
			e.printStackTrace();
		}

		// String cstr = "Provider=Microsoft.JET.OlEDB.4.0;Data Source=c:\\dll\\test.mdb";
		// String sql = "select * from testtable";
		//
		// int count = 0;
		// boolean br;
		// do {
		//
		// int maxr = r.getIntegerExtra("rowcount");
		// for (int i = 0; i < maxr; i++) {
		// System.out.println(" " + r.getStringExtra(String.format("c_%d_%d", i, 0)) + "  byte length:"
		// + b.To(b).length);
		// count++;
		// }
		//
		// } while (br == true && count < 10000);

	}

	public boolean openOdbc(String cstr) throws Exception {
		CBundle r;
		CBundle b = new CBundle();
		b.setString("keyword=lib_part", "odbc");
		b.setString("cmd", "odbc_connect");
		b.setString("cstr", cstr);
		b.setString("sql", "");
		b.setString("handle", this.toString() );
		
		r = tcpl.sendTo(m_targetId, b, m_timeout);
		// System.out.println("from: "+r);
		// System.out.println("1. result=" + r.getBooleanExtra("r"));

		return r.getBoolean("r");
	}

	public CBundle getReader(String cstr, String sql, CBundle param) throws Exception {

		String pl[] = param.getKeys();
		for (int i = 0; i < pl.length; i++) {
			if (sql.indexOf(pl[i]) == -1) {
				throw new Exception(String.format("the %s is not use in SQL query", pl[i]));
			}
		}

		CBundle r;
		CBundle b = new CBundle();
		b.setString("keyword=lib_part", "odbc");
		b.setString("cmd", "odbc_getreader");
		b.setString("cstr", cstr);
		b.setString("sql", sql);
		b.setString("handle", this.toString() );
		b.setByteArray("param", CBundle.To(param));
		r = tcpl.sendTo(m_targetId, b, m_timeout);
		// System.out.println("from: "+r.remote_error );
		// System.out.println("2. result=" + r.getBooleanExtra("r"));
		return r;// r.getBooleanExtra("r");
	}

	public CBundle read(String cstr, String sql) throws Exception {

		CBundle r;
		CBundle b = new CBundle();
		b.setString("keyword=lib_part", "odbc");
		b.setString("cmd", "odbc_read");
		b.setString("cstr", cstr);
		b.setString("sql", sql);
		b.setString("handle", this.toString() );

		r = tcpl.sendTo(m_targetId, b, m_timeout);

		String[] kl = r.getKeys();
		if (kl.length == 0)
			return null;// meaning is no results.

		return r;
	}

	public boolean closeReader(String cstr, String sql) throws Exception {

		CBundle r;
		CBundle b = new CBundle();
		b.setString("keyword=lib_part", "odbc");
		b.setString("cmd", "odbc_closereader");
		b.setString("cstr", cstr);
		b.setString("sql", sql);
		b.setString("handle", this.toString() );
		r = tcpl.sendTo(m_targetId, b, m_timeout);
		// System.out.println("from: "+r.remote_error );
		// System.out.println("4. result=" + r.getBooleanExtra("r"));

		return r.getBoolean("r");
	}

	public boolean closeOdbc(String cstr) throws Exception {

		CBundle r;
		CBundle b = new CBundle();
		b.setString("keyword=lib_part", "odbc");
		b.setString("cmd", "odbc_close");
		b.setString("cstr", cstr);
		b.setString("sql", "");
		b.setString("handle", this.toString() );
		r = tcpl.sendTo(m_targetId, b, m_timeout);
		// System.out.println("from: "+r.remote_error );
		// System.out.println("5. result=" + r.getBooleanExtra("r"));

		return r.getBoolean("r");

	}

	public void odbcGC() throws Exception {
		CBundle r;
		CBundle b = new CBundle();
		b.setString("keyword=lib_part", "odbc");
		b.setString("cmd", "odbc_gc");
		b.setString("cstr", "");
		b.setString("sql", "");
		b.setString("handle", this.toString() );
		r = tcpl.sendTo(m_targetId, b, m_timeout);
		// System.out.println("from: "+r.remote_error );
		// System.out.println("6. result=" + r.getBooleanExtra("r"));
	}

	public CBundle executeQuery(String cstr, String sql, CBundle param) throws Exception {

		String pl[] = param.getKeys();
		for (int i = 0; i < pl.length; i++) {
			if (sql.indexOf(pl[i]) == -1) {
				throw new Exception(String.format("the %s is not use in SQL query", pl[i]));
			}
		}

		CBundle r;
		CBundle b = new CBundle();
		b.setString("keyword=lib_part", "odbc");
		b.setString("cmd", "odbc_executeQuery");
		b.setString("cstr", cstr);
		b.setString("sql", sql);
		b.setString("handle", this.toString() );

		b.setByteArray("param", CBundle.To(param));
		r = tcpl.sendTo(m_targetId, b, m_timeout);
		// System.out.println("from: "+r.remote_error );
		// System.out.println("4. result=" + r.getBooleanExtra("r"));

		// return r.getIntegerExtra("result");
		return r;
	}

	public boolean test__0() throws Exception {

		CBundle r;
		CBundle b = new CBundle();

		b.setString("keyword=lib_part", "odbc");
		b.setString("cmd", "test_0");
		b.setString("cstr", "");
		b.setString("sql", "");
		b.setString("handle", this.toString() );

		Random rd = new Random();

		b.setByte("bt", (byte) rd.nextInt());
		b.setBoolean("b", rd.nextBoolean());
		b.setInteger("i", rd.nextInt());
		b.setLong("l", rd.nextLong());
		b.setSDouble("d", rd.nextDouble());
		b.setString("s", "" + System.currentTimeMillis());
		b.setByteArray("b[", ("" + System.currentTimeMillis()).getBytes());

		r = tcpl.sendTo(m_targetId, b, m_timeout);

		if (b.getByte("bt") != r.getByte("bt"))
			return false;
		if (b.getBoolean("b") != r.getBoolean("b"))
			return false;
		if (b.getInteger("i") != r.getInteger("i"))
			return false;
		if (b.getLong("l") != r.getLong("l"))
			return false;
		if (b.getDouble("d") != r.getDouble("d"))
			return false;
		if (b.getString("s").equals(r.getString("s")) == false)
			return false;

		byte ba1[] = b.getByteArray("b[");
		byte ba2[] = r.getByteArray("b[");

		if (ba1.length != ba2.length)
			return false;

		for (int i = 0; i < ba1.length; i++) {
			if (ba1[i] != ba2[i])
				return false;
		}

		return true;// r.getBooleanExtra("r");
	}

	class EventClientActionListener extends CTcpLinkerListener {

		@Override
		public CBundle actionPerformed(CBundle e) {

			// String s = e.getStringExtra("give me a return");
			// if (s != null) {
			// e.putExtra("return is", String.format("good to see '%s' at %d ", s, System.currentTimeMillis()));
			// return e;
			// }

			// doTcpReceive(re.data);
			// doTcpReceive(e.getByteArrayExtra("buf"));
			return null;
		}

		@Override
		public CBundle actionPerformed(byte[] buf, int len) {
			return null;
		}

		@Override
		public void log(Exception e) {
			e.printStackTrace();
		}

		@Override
		public void log(int level, String s) {
			System.out.println(s);

		}

		@Override
		public void startUp() {
		}

		@Override
		public void finishUp() {
		}

	}

	public void sample1() throws Exception {

		try {
			CBundle r;
			CBundle b = new CBundle();
			int port = 1234;

			// new Thread(new WinExec()).start();
			// new Thread(new WinExec()).start();

			// Thread.sleep(1000);

			String fn = "";//"C:\\10_WhileWorking\\test\\MeterProgram_v2015\\net2java\\libertyOdbcExecute\\bin\\Debug\\net2java.exe";
			String cstr = "Provider=Microsoft.JET.OlEDB.4.0;Data Source=c:\\dll\\test.mdb";
			String sql = "select * from testtable where n=@n";

			N2JOdbc odbc = new N2JOdbc("main", "net2java", "127.0.0.1", port);

			//new Thread(new N2JStarter(fn, "net2java", "127.0.0.1", "" + port)).start();

			odbc.openOdbc(cstr);
			CBundle param = new CBundle();
			param.setInteger("@n", 1);
			// param.putExtra("@str", "123");
			CBundle cr = odbc.getReader(cstr, sql, param);

			for (int i = 0; i < cr.getInteger("columnCount"); i++) {
				System.out.println("column" + i + "  " + cr.getString("column" + i));
			}

			int count = 0;
			boolean br = true;
			do {

				r = odbc.read(cstr, sql);

				// System.out.println(" " + r.getString(String.format("c_%d_%d", i, 0)) + "  byte length:"
				// + CBundle.To(b).length);

			} while (br == true && count < 1000);

			odbc.closeReader(cstr, sql);

			param = new CBundle();
			param.setInteger("@n", 1);
			param.setString("@str", "123");

			r = odbc.executeQuery(cstr, "insert into testTable(n,str) values ( @n,@str) ", param);
			int iResult = r.getInteger("result");
			System.out.println("iresult = " + iResult);

			odbc.closeOdbc(cstr);

		} catch (Exception e) {
			// System.out.println(e.);
			e.printStackTrace();
		}
		System.exit(0);
	}

}
