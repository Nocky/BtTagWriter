/*
 * BtSecureSimplePairing.java (BT Tag Writer)
 *
 * https://github.com/alump/BtTagWriter
 *
 * Copyright 2011-2013 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.data;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

import fi.siika.bttagwriter.exceptions.OutOfSpaceException;

/**
 * Class providing parse and generate functions for Bluetooth Secure Simple
 * Pairing binaries
 */
public class BtSecureSimplePairing {

    /**
     * Mime type of Secure Simple Pairing data
     */
    public final static String MIME_TYPE = "application/vnd.bluetooth.ep.oob";

    /**
     * Using this would give 14 bytes more in small tags. But proper with
     * proper mime mifare ultralight can still fit the address. So this is
     * not used yet.
     */
    //public final static String SHORT_MIME_TYPE = "application/x-btc";


    /**
     * Check if given mime type
     *
     * @param mime Mime type string checked
     * @return true if mime is one of those used with this data
     */
    public static boolean validMimeType(String mime) {
        return (MIME_TYPE.equals(mime));
    }

    /*
     * Magic values are from:
     * https://www.bluetooth.org/Technical/AssignedNumbers/generic_access_profile.htm
     */
    private final static byte BYTE_SHORTENED_LOCAL_NAME = 0x08;
    private final static byte BYTE_COMPLETE_LOCAL_NAME = 0x09;
    private final static byte BYTE_CLASS_OF_DEVICE = 0x0D;
    private final static byte BYTE_SIMPLE_PAIRING_HASH = 0x0E;
    private final static byte BYTE_SIMPLE_PAIRING_RANDOMIZER = 0x0F;
    private final static byte BYTE_MANUFACTURER_SPECIFIC_DATA = -1; //-1 = 0xFF

    private final static String TAG = "BtSecureSimplePairing";

    private final static short SPACE_TOTAL_LEN_BYTES = 2;
    private final static short SPACE_ADDRESS_BYTES = 6;

    /*!
     * Minimum space needed in bytes
     */
    public final static short MIN_SIZE_IN_BYTES =
            SPACE_TOTAL_LEN_BYTES + SPACE_ADDRESS_BYTES;

    /**
     * Class containing the data we care about
     */
    public static class Data {
        private String mName;
        private String mAddress;
        private byte[] mDeviceClass;
        private byte[] mHash;
        private final byte[] mRandomizer;
        private byte[] mManufacturerData;

        public Data() {
            mName = "";
            mAddress = "00:00:00:00:00:00";
            // Use speaker as default
            // TODO: Is there way to really resolve this value?
            mDeviceClass = new byte[]{0x14, 0x04, 0x20};
            mHash = new byte[0];
            mRandomizer = new byte[0];
            mManufacturerData = null;
        }

        /**
         * Set name of device.
         *
         * @param name Name of device (value might be stored in simplified format)
         */
        public void setName(String name) {
            // Use ASCII to be sure
            mName = name.replaceAll("[^\\x20-\\x7e]", "");
        }

        /**
         * Get simplified name of device.
         *
         * @return Simplified name of device.
         */
        public String getName() {
            return mName;
        }

        /**
         * Get name as byte buffer
         *
         * @return Name as byte buffer
         */
        public byte[] getNameBuffer() {
            byte[] ret = null;

            if (!mName.isEmpty()) {
                try {
                    ret = mName.getBytes("ASCII");
                } catch (Exception e) {
                    ret = null;
                }
            }

            return ret;
        }

        /**
         * Will only accept valid addresses. But both lower and upper case
         * letters are accepted.
         *
         * @param address Address in string format (e.g. "00:00:00:00:00:00")
         */
        public void setAddress(String address) {
            String modAddress = address.toUpperCase();
            if (BluetoothAdapter.checkBluetoothAddress(modAddress)) {
                mAddress = modAddress;
            }
        }

        /**
         * Get address of device
         *
         * @return Address of device as string (eg. "00:00:00:00:00:00")
         */
        public String getAddress() {
            return mAddress;
        }

        /**
         * Get address as byte array
         *
         * @return Address of device as byte array
         */
        public byte[] getAddressByteArray() {
            byte[] ret = new byte[6];
            String[] parts = mAddress.split(":");
            for (int i = 0; i < 6; ++i) {
                ret[i] = (byte) Short.parseShort(parts[5 - i], 16);
            }
            return ret;
        }

        public void setDeviceClass(byte[] deviceClass) {
            if (deviceClass.length == 3) {
                mDeviceClass = deviceClass;
            } else {
                Log.e(TAG, "Length of device class not accepted: " + deviceClass.length);
            }
        }

        public byte[] getDeviceClass() {
            return mDeviceClass;
        }

        public boolean hasDeviceClass() {
            return mDeviceClass.length == 3;
        }

        public void setHash(byte[] hash) {
            mHash = hash;
        }

        public void setRandomizer(byte[] randomizer) {
            mHash = randomizer;
        }

        public byte[] getHash() {
            return mHash;
        }

        public byte[] getRandomizer() {
            return mRandomizer;
        }

        public void setManufacturerData(byte[] data) {
            mManufacturerData = data;
        }

        public byte[] getManufacturerData() {
            return mManufacturerData;
        }

    }

	/*
	 * How binary data is constructed:
	 * First two bytes = total length
	 * Next six bytes = address
	 * Repeat next until end:
	 * first byte = "data length"
	 * second byte = type
	 * "data length" bytes = data 
	 */

    /**
     * Generate binary Bluetooth Secure Simple Pairing content
     *
     * @param input     Information stored to binary output
     * @param maxLength How big byte array can be returned
     * @return Return binary content
     */
    public static byte[] generate(Data input, short maxLength)
            throws OutOfSpaceException {

        //TODO: Is 30k bytes enough? I assume so ;) (16th bit can't be used)
        short len = MIN_SIZE_IN_BYTES;

        byte[] manBytes = null;
        byte[] classBytes = null;
        byte[] nameBytes = null;
        boolean moreIn = true;

        // Manufacturer data is most important (as for now it contains PIN)
        if (len < maxLength) {
            manBytes = input.getManufacturerData();
            if (manBytes == null) {
                moreIn = true;
            } else if ((len + 2 + manBytes.length) <= maxLength) {
                len += 2 + manBytes.length;
            } else {
                manBytes = null;
                moreIn = false;
            }
        }

        // Device class is also important, as it will be used later to separate
        // different ways to connect the device.
        if (moreIn && len < maxLength) {
            classBytes = input.getDeviceClass();
            if (classBytes == null) {
                moreIn = true;
            } else if ((len + 2 + classBytes.length) <= maxLength) {
                len += 2 + classBytes.length;
            } else {
                classBytes = null;
                moreIn = false;
            }
        }

        // Name is nice to have but takes lots of space
        if (moreIn && len < maxLength) {
            nameBytes = input.getNameBuffer();
            if (nameBytes == null) {
                moreIn = true;
            } else if ((len + 2 + nameBytes.length) <= maxLength) {
                len += 2 + nameBytes.length;
            } else {
                nameBytes = null;
                moreIn = false;
            }
        }

        //Still check that we are inside the limits
        if (len > maxLength) {
            Log.w(TAG, "Not enough space in tag for content");
            throw new OutOfSpaceException("Not enough space for BT data");
        }

        byte data[] = new byte[len];
        int index = 0;

        // total length (2 bytes)
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putShort(len);
        data[index++] = buffer.get(1);
        data[index++] = buffer.get(0);

        // address
        final byte[] addressBuffer = input.getAddressByteArray();
        System.arraycopy(addressBuffer, 0, data, index, addressBuffer.length);
        index += addressBuffer.length;

        // complete local name
        index = addPart(index, data, nameBytes, BYTE_COMPLETE_LOCAL_NAME);

        // manufacturer specific data
        index = addPart(index, data, manBytes, BYTE_MANUFACTURER_SPECIFIC_DATA);

        // class of device
        index = addPart(index, data, classBytes, BYTE_CLASS_OF_DEVICE);

        return data;
    }

    /**
     * Add Bluetooth Secure Simple Pairing part
     * @param index Index where added
     * @param dest Destination buffer where added
     * @param data Data added
     * @param id ID of data
     * @return New position index
     */
    private static int addPart(int index, byte[] dest, byte[] data, byte id) {
        if (data != null) {
            dest[index++] = (byte) (data.length + 1);
            dest[index++] = id;
            System.arraycopy(data, 0, dest, index, data.length);
            index += data.length;
        }
        return index;
    }

    /**
     * Parse binary Bluetooth Secure Simple Pairing content. Not needed for application, could be
     * used in unit tests?
     *
     * @param binaryData Binary data
     * @return Binary data converted to class for easy access
     */
    @Deprecated
    public static Data parse(byte[] binaryData) throws Exception {
        Data data = new Data();

        //TODO: ignoring for now length bytes 0 and 1

        //TODO: There has to be nicer way to do this!!!
        int[] addressBuffer = new int[6];
        for (int i = 0; i < 6; i++) {
            addressBuffer[i] = (binaryData[7 - i]) & 0xff;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; ++i) {
            if (i > 0) {
                sb.append(":");
            }
            if (addressBuffer[i] < 0x10) {
                sb.append("0");
            }

            sb.append(Integer.toHexString(addressBuffer[i]));
        }
        data.setAddress(sb.toString());

        //Read the rest
        for (int i = 8; i < binaryData.length; ++i) {
            int dataLen = (binaryData[i]); //this includes type and data
            int dataType = (binaryData[i + 1]); //type bit

            byte[] dataArray = Arrays.copyOfRange(binaryData, i + 2,
                    i + 1 + dataLen);

            i = i + dataLen; //Update index for next round (for will add 1)

            switch (dataType) {
                case BYTE_COMPLETE_LOCAL_NAME:
                    data.setName(new String(dataArray, "UTF-8"));
                    break;
                case BYTE_SHORTENED_LOCAL_NAME:
                    //Do not override complete name if it exists
                    if (data.getName().isEmpty()) {
                        data.setName(new String(dataArray, "UTF-8"));
                    }
                    break;
                case BYTE_CLASS_OF_DEVICE:
                    data.setDeviceClass(dataArray);
                    break;
                case BYTE_SIMPLE_PAIRING_RANDOMIZER:
                    data.setRandomizer(dataArray);
                    break;
                case BYTE_SIMPLE_PAIRING_HASH:
                    data.setHash(dataArray);
                    break;
                default:
                    //There are many known elements we ignore here
                    Log.w(TAG, "Unknown element: " + dataType);
            }

        }

        Log.d(TAG, "Parsed data: '" + data.getAddress() + "' '"
                + data.getName() + "'");

        return data;
    }
}
