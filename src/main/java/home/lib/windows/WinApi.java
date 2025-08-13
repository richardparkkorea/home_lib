package home.lib.windows;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import home.lib.lang.Application;

/**
 * 
 *
 * //cd E:\10_WhileWorking\7_Á¦Ç°\NextPcGeneration_v2015\_homelib\src
 * 
 * //"C:\Program Files (x86)\Java\jdk1.8.0_261\bin\javac" ".\home\lib\windows\WinApi.java"
 * 
 * //"C:\Program Files (x86)\Java\jdk1.8.0_261\bin\javah" -classpath . home.lib.windows.WinApi
 * 
 * //"C:\Program Files (x86)\Java\jdk1.8.0_261\bin\javap" -s -p java.lang.Double
 * 
 * @author richardpark
 *
 */
public class WinApi {

	// Load the library
	static {

//		String os = System.getProperty("os.name").toLowerCase();
//
//		if (os.contains("win")) {
//
//			
//			//InputStream in = getClass().getResourceAsStream("icon.jpg");
//
//			
//			// x86
//			// amd64
//			String osArch = System.getProperty("os.arch");
//			System.out.println("OS Architecture : " + os +" "+osArch);
//
//			if (osArch.endsWith("86")) {
//				System.loadLibrary("myJni32");
//
//			}
//			if (osArch.endsWith("64")) {
//
//				System.loadLibrary("myJni64");
//			}
//
//		}

	}
	
	
	
	public static File getResourceAsFile(Object cls, String resourcePath,String ext) {
		try {
			// InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath);
			InputStream in = cls.getClass().getResourceAsStream(resourcePath);
			if (in == null) {
				return null;
			}

			File tempFile = File.createTempFile(String.valueOf(in.hashCode()), ext);
			tempFile.deleteOnExit();

			try (FileOutputStream out = new FileOutputStream(tempFile)) {
				// copy stream
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
				}
			}
			return tempFile;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static File  loadLib() throws Exception {
		
		
		File f=null;
		//Application.addLibraryPath("c:\\dll");

		String os = System.getProperty("os.name").toLowerCase();

		if (os.contains("win")) {

			// InputStream in = getClass().getResourceAsStream("icon.jpg");

			// x86
			// amd64
			String osArch = System.getProperty("os.arch");
			

			if (osArch.endsWith("86")) {
				f = getResourceAsFile(new WinApiResouce(), "myJni32.dll",".dll");

			}
			if (osArch.endsWith("64")) {
				f = getResourceAsFile(new WinApiResouce(), "myJni64.dll",".dll");
			}

			Application.addLibraryPath(f.getParent());

			System.out.println("OS Architecture : " + os + " " + osArch);
			System.out.println(f.getAbsolutePath());
			System.out.println(f.getParent());
			System.out.println(f.getName());
					

			String fn = f.getName();
			System.loadLibrary(fn.substring(0, fn.length() - 4));
			//System.loadLibrary(f.getName());

		}
		
		return f;
		
	}

	final public static long KEYEVENTF_EXTENDEDKEY = 0x0001;
	final public static long KEYEVENTF_KEYUP = 0x0002;
	final public static long KEYEVENTF_UNICODE = 0x0004;
	final public static long KEYEVENTF_SCANCODE = 0x0008;

	final public static long MOUSEEVENTF_MOVE = 0x0001; /* mouse move */
	final public static long MOUSEEVENTF_LEFTDOWN = 0x0002; /* left button down */
	final public static long MOUSEEVENTF_LEFTUP = 0x0004; /* left button up */
	final public static long MOUSEEVENTF_RIGHTDOWN = 0x0008; /* right button down */
	final public static long MOUSEEVENTF_RIGHTUP = 0x0010; /* right button up */
	final public static long MOUSEEVENTF_MIDDLEDOWN = 0x0020; /* middle button down */
	final public static long MOUSEEVENTF_MIDDLEUP = 0x0040; /* middle button up */
	final public static long MOUSEEVENTF_XDOWN = 0x0080; /* x button down */
	final public static long MOUSEEVENTF_XUP = 0x0100; /* x button down */
	final public static long MOUSEEVENTF_WHEEL = 0x0800; /* wheel button rolled */
	final public static long MOUSEEVENTF_HWHEEL = 0x01000; /* hwheel button rolled */
	final public static long MOUSEEVENTF_MOVE_NOCOALESCE = 0x2000; /* do not coalesce mouse moves */
	final public static long MOUSEEVENTF_VIRTUALDESK = 0x4000; /* map to entire virtual desktop */
	final public static long MOUSEEVENTF_ABSOLUTE = 0x8000; /* absolute move */

	/*
	 * Virtual Keys, Standard Set
	 */
	final public static long VK_LBUTTON = 0x01;
	final public static long VK_RBUTTON = 0x02;
	final public static long VK_CANCEL = 0x03;
	final public static long VK_MBUTTON = 0x04; /* NOT contiguous with L & RBUTTON */

	// #if(_WIN32_WINNT >= =0x0500)
	final public static long VK_XBUTTON1 = 0x05; /* NOT contiguous with L & RBUTTON */
	final public static long VK_XBUTTON2 = 0x06; /* NOT contiguous with L & RBUTTON */
	// #endif /* _WIN32_WINNT >= =0x0500 */

	/*
	 * =0x07 : reserved
	 */

	final public static long VK_BACK = 0x08;
	final public static long VK_TAB = 0x09;

	/*
	 * =0x0A - =0x0B : reserved
	 */

	final public static long VK_CLEAR = 0x0C;
	final public static long VK_RETURN = 0x0D;

	/*
	 * =0x0E - =0x0F : unassigned
	 */

	final public static long VK_SHIFT = 0x10;
	final public static long VK_CONTROL = 0x11;
	final public static long VK_MENU = 0x12;
	final public static long VK_PAUSE = 0x13;
	final public static long VK_CAPITAL = 0x14;

	final public static long VK_KANA = 0x15;
	final public static long VK_HANGEUL = 0x15; /* old name - should be here for compatibility */
	final public static long VK_HANGUL = 0x15;

	/*
	 * =0x16 : unassigned
	 */

	final public static long VK_JUNJA = 0x17;
	final public static long VK_FINAL = 0x18;
	final public static long VK_HANJA = 0x19;
	final public static long VK_KANJI = 0x19;

	/*
	 * =0x1A : unassigned
	 */

	final public static long VK_ESCAPE = 0x1B;

	final public static long VK_CONVERT = 0x1C;
	final public static long VK_NONCONVERT = 0x1D;
	final public static long VK_ACCEPT = 0x1E;
	final public static long VK_MODECHANGE = 0x1F;

	final public static long VK_SPACE = 0x20;
	final public static long VK_PRIOR = 0x21;
	final public static long VK_NEXT = 0x22;
	final public static long VK_END = 0x23;
	final public static long VK_HOME = 0x24;
	final public static long VK_LEFT = 0x25;
	final public static long VK_UP = 0x26;
	final public static long VK_RIGHT = 0x27;
	final public static long VK_DOWN = 0x28;
	final public static long VK_SELECT = 0x29;
	final public static long VK_PRINT = 0x2A;
	final public static long VK_EXECUTE = 0x2B;
	final public static long VK_SNAPSHOT = 0x2C;
	final public static long VK_INSERT = 0x2D;
	final public static long VK_DELETE = 0x2E;
	final public static long VK_HELP = 0x2F;

	/*
	 * VK_0 - VK_9 are the same as ASCII '0' - '9' (=0x30 - =0x39) =0x3A - =0x40 : unassigned VK_A - VK_Z are the same
	 * as ASCII 'A' - 'Z' (=0x41 - =0x5A)
	 */

	final public static long VK_LWIN = 0x5B;
	final public static long VK_RWIN = 0x5C;
	final public static long VK_APPS = 0x5D;

	/*
	 * =0x5E : reserved
	 */

	final public static long VK_SLEEP = 0x5F;

	final public static long VK_NUMPAD0 = 0x60;
	final public static long VK_NUMPAD1 = 0x61;
	final public static long VK_NUMPAD2 = 0x62;
	final public static long VK_NUMPAD3 = 0x63;
	final public static long VK_NUMPAD4 = 0x64;
	final public static long VK_NUMPAD5 = 0x65;
	final public static long VK_NUMPAD6 = 0x66;
	final public static long VK_NUMPAD7 = 0x67;
	final public static long VK_NUMPAD8 = 0x68;
	final public static long VK_NUMPAD9 = 0x69;
	final public static long VK_MULTIPLY = 0x6A;
	final public static long VK_ADD = 0x6B;
	final public static long VK_SEPARATOR = 0x6C;
	final public static long VK_SUBTRACT = 0x6D;
	final public static long VK_DECIMAL = 0x6E;
	final public static long VK_DIVIDE = 0x6F;
	final public static long VK_F1 = 0x70;
	final public static long VK_F2 = 0x71;
	final public static long VK_F3 = 0x72;
	final public static long VK_F4 = 0x73;
	final public static long VK_F5 = 0x74;
	final public static long VK_F6 = 0x75;
	final public static long VK_F7 = 0x76;
	final public static long VK_F8 = 0x77;
	final public static long VK_F9 = 0x78;
	final public static long VK_F10 = 0x79;
	final public static long VK_F11 = 0x7A;
	final public static long VK_F12 = 0x7B;
	final public static long VK_F13 = 0x7C;
	final public static long VK_F14 = 0x7D;
	final public static long VK_F15 = 0x7E;
	final public static long VK_F16 = 0x7F;
	final public static long VK_F17 = 0x80;
	final public static long VK_F18 = 0x81;
	final public static long VK_F19 = 0x82;
	final public static long VK_F20 = 0x83;
	final public static long VK_F21 = 0x84;
	final public static long VK_F22 = 0x85;
	final public static long VK_F23 = 0x86;
	final public static long VK_F24 = 0x87;

	public static native String version_info();

	// https://msdn.microsoft.com/en-us/library/windows/desktop/ms646260(v=vs.85).aspx

	public static native void mouse_event(long flags, long dx, long dy, long data);

	// https://msdn.microsoft.com/en-us/library/windows/desktop/ms646304(v=vs.85).aspx
	public static native void keybd_event(byte vk, byte scan, long flags);

	public static native void sendInputMouse(long dx, long dy, long mouseData, long dwFlags, long time);

	public static native void sendInputKey(long vk, long scan, long dwFlags, long time);

	public static native void sendInputHw(long msg, long lparam, long wparam);

	/*
	 * GetSystemMetrics() codes
	 */

	final public static int SM_CXSCREEN = 0;
	final public static int SM_CYSCREEN = 1;
	final public static int SM_CXVSCROLL = 2;
	final public static int SM_CYHSCROLL = 3;
	final public static int SM_CYCAPTION = 4;
	final public static int SM_CXBORDER = 5;
	final public static int SM_CYBORDER = 6;
	final public static int SM_CXDLGFRAME = 7;
	final public static int SM_CYDLGFRAME = 8;
	final public static int SM_CYVTHUMB = 9;
	final public static int SM_CXHTHUMB = 10;
	final public static int SM_CXICON = 11;
	final public static int SM_CYICON = 12;
	final public static int SM_CXCURSOR = 13;
	final public static int SM_CYCURSOR = 14;
	final public static int SM_CYMENU = 15;
	final public static int SM_CXFULLSCREEN = 16;
	final public static int SM_CYFULLSCREEN = 17;
	final public static int SM_CYKANJIWINDOW = 18;
	final public static int SM_MOUSEPRESENT = 19;
	final public static int SM_CYVSCROLL = 20;
	final public static int SM_CXHSCROLL = 21;
	final public static int SM_DEBUG = 22;
	final public static int SM_SWAPBUTTON = 23;
	final public static int SM_RESERVED1 = 24;
	final public static int SM_RESERVED2 = 25;
	final public static int SM_RESERVED3 = 26;
	final public static int SM_RESERVED4 = 27;
	final public static int SM_CXMIN = 28;
	final public static int SM_CYMIN = 29;
	final public static int SM_CXSIZE = 30;
	final public static int SM_CYSIZE = 31;
	final public static int SM_CXFRAME = 32;
	final public static int SM_CYFRAME = 33;
	final public static int SM_CXMINTRACK = 34;
	final public static int SM_CYMINTRACK = 35;
	final public static int SM_CXDOUBLECLK = 36;
	final public static int SM_CYDOUBLECLK = 37;
	final public static int SM_CXICONSPACING = 38;
	final public static int SM_CYICONSPACING = 39;
	final public static int SM_MENUDROPALIGNMENT = 40;
	final public static int SM_PENWINDOWS = 41;
	final public static int SM_DBCSENABLED = 42;
	final public static int SM_CMOUSEBUTTONS = 43;
	//
	//// #if(WINVER >= 0x0400)
	// final public static int SM_CXFIXEDFRAME SM_CXDLGFRAME /* ;win40 name change */
	// final public static int SM_CYFIXEDFRAME SM_CYDLGFRAME /* ;win40 name change */
	// final public static int SM_CXSIZEFRAME SM_CXFRAME /* ;win40 name change */
	// final public static int SM_CYSIZEFRAME SM_CYFRAME /* ;win40 name change */
	//
	// final public static int SM_SECURE 44
	// final public static int SM_CXEDGE 45
	// final public static int SM_CYEDGE 46
	// final public static int SM_CXMINSPACING 47
	// final public static int SM_CYMINSPACING 48
	// final public static int SM_CXSMICON 49
	// final public static int SM_CYSMICON 50
	// final public static int SM_CYSMCAPTION 51
	// final public static int SM_CXSMSIZE 52
	// final public static int SM_CYSMSIZE 53
	// final public static int SM_CXMENUSIZE 54
	// final public static int SM_CYMENUSIZE 55
	// final public static int SM_ARRANGE 56
	// final public static int SM_CXMINIMIZED 57
	// final public static int SM_CYMINIMIZED 58
	// final public static int SM_CXMAXTRACK 59
	// final public static int SM_CYMAXTRACK 60
	// final public static int SM_CXMAXIMIZED 61
	// final public static int SM_CYMAXIMIZED 62
	// final public static int SM_NETWORK 63
	// final public static int SM_CLEANBOOT 67
	// final public static int SM_CXDRAG 68
	// final public static int SM_CYDRAG 69
	// #endif /* WINVER >= 0x0400 */
	// final public static int SM_SHOWSOUNDS 70
	// #if(WINVER >= 0x0400)
	// final public static int SM_CXMENUCHECK 71 /* Use instead of GetMenuCheckMarkDimensions()! */
	// final public static int SM_CYMENUCHECK 72
	// final public static int SM_SLOWMACHINE 73
	// final public static int SM_MIDEASTENABLED 74
	// #endif /* WINVER >= 0x0400 */
	//
	// #if (WINVER >= 0x0500) || (_WIN32_WINNT >= 0x0400)
	// final public static int SM_MOUSEWHEELPRESENT 75
	// #endif
	// #if(WINVER >= 0x0500)
	// final public static int SM_XVIRTUALSCREEN 76
	// final public static int SM_YVIRTUALSCREEN 77
	// final public static int SM_CXVIRTUALSCREEN 78
	// final public static int SM_CYVIRTUALSCREEN 79
	// final public static int SM_CMONITORS 80
	// final public static int SM_SAMEDISPLAYFORMAT 81
	// #endif /* WINVER >= 0x0500 */
	// #if(_WIN32_WINNT >= 0x0500)
	// final public static int SM_IMMENABLED 82
	// #endif /* _WIN32_WINNT >= 0x0500 */
	// #if(_WIN32_WINNT >= 0x0501)
	// final public static int SM_CXFOCUSBORDER 83
	// final public static int SM_CYFOCUSBORDER 84
	// #endif /* _WIN32_WINNT >= 0x0501 */
	//
	// #if(_WIN32_WINNT >= 0x0501)
	// final public static int SM_TABLETPC 86
	// final public static int SM_MEDIACENTER 87
	// final public static int SM_STARTER 88
	// final public static int SM_SERVERR2 89
	// #endif /* _WIN32_WINNT >= 0x0501 */
	//
	// #if(_WIN32_WINNT >= 0x0600)
	// final public static int SM_MOUSEHORIZONTALWHEELPRESENT 91
	// final public static int SM_CXPADDEDBORDER 92
	// #endif /* _WIN32_WINNT >= 0x0600 */
	//
	// #if(WINVER >= 0x0601)
	//
	// final public static int SM_DIGITIZER 94
	// final public static int SM_MAXIMUMTOUCHES 95
	// #endif /* WINVER >= 0x0601 */
	//
	// #if (WINVER < 0x0500) && (!defined(_WIN32_WINNT) || (_WIN32_WINNT < 0x0400))
	// final public static int SM_CMETRICS 76
	// #elif WINVER == 0x500
	// final public static int SM_CMETRICS 83
	// #elif WINVER == 0x501
	// final public static int SM_CMETRICS 91
	// #elif WINVER == 0x600
	// final public static int SM_CMETRICS 93
	// #else
	// final public static int SM_CMETRICS 97
	// #endif
	//
	// #if(WINVER >= 0x0500)
	// final public static int SM_REMOTESESSION 0x1000
	//
	//
	// #if(_WIN32_WINNT >= 0x0501)
	// final public static int SM_SHUTTINGDOWN 0x2000
	// #endif /* _WIN32_WINNT >= 0x0501 */
	//
	// #if(WINVER >= 0x0501)
	// final public static int SM_REMOTECONTROL 0x2001
	// #endif /* WINVER >= 0x0501 */
	//
	// #if(WINVER >= 0x0501)
	// final public static int SM_CARETBLINKINGENABLED 0x2002
	// #endif /* WINVER >= 0x0501 */
	//
	// #if(WINVER >= 0x0602)
	// final public static int SM_CONVERTIBLESLATEMODE 0x2003
	// final public static int SM_SYSTEMDOCKED 0x2004
	// #endif /* WINVER >= 0x0602 */
	//
	// #endif /* WINVER >= 0x0500 */

	public static native int GetSystemMetrics(int index);

	public static native int isRunAsAdministrator();

	public static native String elevateNow();

	/**
	 * 
	 * 
	 * 
	 * 
	 */
	public boolean isWindowsOs() {

		if (System.getProperty("os.name").toLowerCase().trim().indexOf("win") == 0)
			return true;

		return false;
	}

}
