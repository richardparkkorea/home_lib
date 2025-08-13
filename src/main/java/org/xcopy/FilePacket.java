package org.xcopy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
 


public class FilePacket implements java.io.Serializable {
	// public String name;
	// public String address;
	// public transient int SSN;
	// public int number;
	//
	// public void mailCheck()
	// {
	// System.out.println("Mailing a check to " + name
	// + " " + address);
	// }

	//
	// FileOutputStream fileOut =
	// new FileOutputStream("/tmp/employee.ser");
	// ObjectOutputStream out = new ObjectOutputStream(fileOut);
	// out.writeObject(e);
	// out.close();
	// fileOut.close();
	//
	//
	
	
  
	
	

	public String from = "";
	public String to="";
	public long msgId=0;
	public String msg;
	public byte[] data;
	
	public String str;
	public String[] strArr;
	public long l;
	public long lArr[]=null;
	public double d;
	public boolean b;
	public String remote_error=null;//return Error Messsage set here 
	
	
	//
	//internal use
	public long privateWhen;
	public long privateSendIndex=0;
	public byte privateCode='s'; // 's'-send 'r'-response 'p'-post
	public String privateSocket=null;
	
	public FilePacket() {
		privateWhen=System.currentTimeMillis();
	}
	
	public FilePacket(String f, String t, String m) {
		this(f, t, 0, m, null);
	}

	public FilePacket(String f, String t, long mid) {
		this(f, t, mid, null, null);
	}

	public FilePacket(String f, String t, long mid, String m, byte[] b) {
		from = f;
		to = t;
		msgId = mid;
		msg = m;
		data = b;
		privateWhen=System.currentTimeMillis();
	}

	/**
	 * 
	 * @param o
	 * @return
	 */
	public static byte[] To(FilePacket o) {

		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			ObjectOutputStream out = new ObjectOutputStream(bos);
			out.writeObject(o);
			return bos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * 
	 * 
	 * @param b
	 * @return
	 */
	public static FilePacket From(byte[] b) {
		ByteArrayInputStream bis = new ByteArrayInputStream(b);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			FilePacket o = (FilePacket) in.readObject();
			return o;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	
	 

}