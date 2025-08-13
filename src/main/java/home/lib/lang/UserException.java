package home.lib.lang;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class UserException extends Exception {

	public UserException(String f, Object... a) {
		super(String.format(f, a));
	}

	Object obj = null;

	public void setUserObject(Object o) {
		obj = o;
	}

	public Object getUserObject() {
		return obj;
	}

	
	
	
	/**
	 * 
	 * @param aThrowable
	 * @return
	 */
	public static String getStackTrace(Throwable aThrowable) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}
}
