package richard.homelib;


import org.xcopy2.XcMain2;
import org.xcopy3.MqttTest4;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		System.out.println("Hello World!");

		if (args.length >= 1 && args[0].trim().equals("-mqtt3ssl")) {
			try {
				MqttTest4.test(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		} else {
			// if (args.length >= 1 && args[0].equals("-xcmain")) {
			XcMain2.start(args);
			return; 
			// }

		}

	}
}
