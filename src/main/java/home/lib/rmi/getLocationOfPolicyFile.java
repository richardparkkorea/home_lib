package home.lib.rmi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


class PolicyFileLocator {
	// public static final String POLICY_FILE_NAME = "/mypolicy";
	//
	// public static String getLocationOfPolicyFile() {
	// try {
	// File tempFile = File.createTempFile("rmi-base", ".policy");
	// InputStream is = PolicyFileLocator.class.getResourceAsStream(POLICY_FILE_NAME);
	// BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
	// int read = 0;
	// while ((read = is.read()) != -1) {
	// writer.write(read);
	// }
	// writer.close();
	// tempFile.deleteOnExit();
	// return tempFile.getAbsolutePath();
	// } catch (IOException e) {
	// throw new RuntimeException(e);
	// }
	// }

	public static String getLocationOfPolicyFile() {

		// System.out.println( LazyInitialization.temporaryDirectory() );

		try {
			File tempFile = File.createTempFile("my_rmi_policy", ".policy");

			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

			String policy = "	 grant    {   \n" + "    permission java.security.AllPermission;   \n" + "  };   \n";

			writer.write(policy.toCharArray());

			writer.close();
			tempFile.deleteOnExit();

			//System.out.println("tempFile.getAbsolutePath()=" + tempFile.getAbsolutePath());

			return tempFile.getAbsolutePath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
