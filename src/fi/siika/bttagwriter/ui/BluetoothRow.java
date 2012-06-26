/**
 * BluetoothRow.java (bttagwriter)
 *
 * Copyright 2012 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.ui;


/**
 * Row used to present a Bluetooth device
 */
public class BluetoothRow {
    private final String name;
    private final String address;
    private boolean paired = false;
    private boolean deviceVisible = true;
    private boolean audio = false;
    
    public BluetoothRow (String name, String address, boolean paired, boolean audio) {
        this.name = name;
        this.address = address;
        this.paired = paired;
        this.audio = audio;
    }
    
    public String getName() {
        return name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public boolean isPaired() {
        return paired;
    }
    
    public void setDeviceVisible (boolean visible) {
        deviceVisible = visible;
    }
    
    public boolean isDeviceVisible() {
        return deviceVisible;
    }
    
    public boolean isAudio() {
        return audio;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return address.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BluetoothRow other = (BluetoothRow) obj;
        if (address == null) {
            if (other.getAddress() != null) {
                return false;
            }
        } else if (!address.equals(other.getAddress())) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return address;
    }
}
