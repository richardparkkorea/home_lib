package home.lib.windows;

public class WinApiExamples {



	public static void main(String[] args) {

		try {

			
			WinApi.loadLib();
			

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(System.getProperty("os.name"));

		System.out.println("getver(" + WinApi.version_info());

		int VIRTUAL_X_MODIFIER = (65536 / WinApi.GetSystemMetrics(WinApi.SM_CXSCREEN));
		int VIRTUAL_Y_MODIFIER = (65536 / WinApi.GetSystemMetrics(WinApi.SM_CYSCREEN));

		int x = 100;
		int y = 100;
		boolean upDown = true;
		
	//	WinApi.elevateNow();
		
		/// WinApi.sendInputMouse(x*VIRTUAL_X_MODIFIER, y*VIRTUAL_Y_MODIFIER, 0, WinApi.MOUSEEVENTF_ABSOLUTE |
		/// WinApi.MOUSEEVENTF_VIRTUALDESK | WinApi.MOUSEEVENTF_MOVE, 0);

		// WinApi.sendInputMouse(0, 0, 0, (upDown ? WinApi.MOUSEEVENTF_LEFTDOWN : WinApi.MOUSEEVENTF_LEFTUP), 0);
		// WinApi.sendInputMouse(0, 0, 0, (upDown ? WinApi.MOUSEEVENTF_LEFTDOWN : WinApi.MOUSEEVENTF_LEFTUP), 0);

		System.exit(0);
	}

}
