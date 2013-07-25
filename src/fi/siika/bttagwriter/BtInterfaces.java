/**
 * BtInterfaces.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter;

import android.bluetooth.IBluetooth;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Class hiding the ugly way application now uses to get interfaces not
 * provided by normal application APIs
 */
public class BtInterfaces {
	
	
	private final static String TAG = "BtInterfaces";
    
    /**
     * As application API does not offer direct access to pairing of new
     * bluetooth devices this has to be done via interface class
     * @return Bluetooth interface class
     */
    public static IBluetooth getBluetooth() {

    	IBluetooth ibt = null;

    	try {

    	    Class c2 = Class.forName("android.os.ServiceManager");

    	    Method m2 = c2.getDeclaredMethod("getService", String.class);
    	    IBinder b = (IBinder) m2.invoke(null, "bluetooth");
    	    
    	    Class c3 = Class.forName("android.bluetooth.IBluetooth");

    	    Class[] s2 = c3.getDeclaredClasses();

    	    Class c = s2[0];
    	    // printMethods(c);
    	    Method m = c.getDeclaredMethod("asInterface", IBinder.class);

    	    m.setAccessible(true);
    	    ibt = (IBluetooth) m.invoke(null, b);

    	} catch (Exception e) {
    	    Log.e(TAG, "Bluetooth interface problem: " + e.getMessage());
    	}
    	
    	return ibt;
    }
}
