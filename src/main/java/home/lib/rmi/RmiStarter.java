package home.lib.rmi;

public class RmiStarter { 
	/** * * @param clazzToAddToServerCodebase a class that should be in the java.rmi.server.codebase property. */
	public RmiStarter(Class clazzToAddToServerCodebase) {
		
		System.setProperty("java.rmi.server.codebase", clazzToAddToServerCodebase.getProtectionDomain().getCodeSource()
				.getLocation().toString());
		
		System.setProperty("java.security.policy", PolicyFileLocator.getLocationOfPolicyFile());

		
		//System.out.println("java.rmi.server.codebase="
		//		+ clazzToAddToServerCodebase.getProtectionDomain().getCodeSource().getLocation().toString());

		// if (System.getSecurityManager() == null) {
		// System.setSecurityManager(new SecurityManager());
		// }
	}

	/** * extend this class and do RMI handling here */
	// public abstract void doCustomRmiHandling();
}
