/**
 * Pages.java (bttagwriter)
 *
 * Copyright 2012 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.ui;

/**
 * 
 */
public enum Pages {
    START(0), ABOUT(1), BT_SELECT(2), EXTRA_OPTIONS(3), TAG(4), SUCCESS(5);
    
    private final int mValue;
    
    private Pages(int value) {
        mValue = value;
    }
    
    public int toInt() {
        return mValue;
    }
    
    public boolean equal(int value) {
        return toInt() == value;
    }
}
