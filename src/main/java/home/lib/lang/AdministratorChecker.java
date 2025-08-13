package home.lib.lang;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.prefs.Preferences;

import static java.lang.System.setErr;
import static java.util.prefs.Preferences.systemRoot;

public class AdministratorChecker
{
    public static final boolean IS_RUNNING_AS_ADMINISTRATOR;

    static
    {
        IS_RUNNING_AS_ADMINISTRATOR = isRunningAsAdministrator();
    }

    private static boolean isRunningAsAdministrator()
    {
        Preferences preferences = systemRoot();

        synchronized (System.err)
        {
            setErr(new PrintStream(new OutputStream()
            {
                @Override
                public void write(int b)
                {
                }
            }));

            try
            {
                preferences.put("richard", "you can call me richard"); // SecurityException on Windows
                preferences.remove("richard");
                preferences.flush(); // BackingStoreException on Linux
                return true;
            } catch (Exception exception)
            {
                return false;
            } finally
            {
                setErr(System.err);
            }
        }
    }
    
    
    public static boolean hasWritePermission(String dir) {
    	try {
    	      File f = new File( dir+File.pathSeparator+System.currentTimeMillis()+"."+System.nanoTime());
    	      if (f.createNewFile()) {
    	        //System.out.println("File created: " + myObj.getName());
    	    	  
    	      } else {
    	        //System.out.println("File already exists.");
    	      }
    	      f.delete();
    	      
    	      return true;
    	    } catch (IOException e) {
    	      //System.out.println("An error occurred.");
    	      //e.printStackTrace();
    	    }
    	return false;
    }
}
    
