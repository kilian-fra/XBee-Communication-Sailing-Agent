package comm.protocol;

import static comm.Constants.BYTE_MAX;
import static comm.Constants.BYTE_SIZE_BITS;

import comm.Constants.Place;

/**
 * This class represents an unsigned short value which is
 * not nativly supported in Java.
 */
public class UnsignedShort {
    /**
     * The value of the unsigned short.
     */
    private final byte[] value = new byte[2];

    /**
     * Constructor for the Class UnsignedShort.
     *
     * @param first - the value of the unsigned short
     * @param second - the value of the unsigned short
     */
    public UnsignedShort(final byte first, final byte second) {
        this.value[Place.FIRST.ordinal()] = first;
        this.value[Place.SECOND.ordinal()] = second;
    }

    /**
     * Constructor for the Class UnsignedShort.
     *
     * @param val - the value of the unsigned short
     */
    public UnsignedShort(final byte[] val) {
        this.value[Place.FIRST.ordinal()] = val[Place.FIRST.ordinal()];
        this.value[Place.SECOND.ordinal()] = val[Place.SECOND.ordinal()];
    }

    /**
     * Constructor for the Class UnsignedShort.
     * @param val
     */
    public UnsignedShort(final int val) {
        this.value[Place.SECOND.ordinal()] = (byte) ((val >> BYTE_SIZE_BITS) & BYTE_MAX);
        this.value[Place.FIRST.ordinal()] = (byte) (val & BYTE_MAX);
    }

    /**
     * This method returns the value as an integer.
     *
     * @return int - the value as an integer
     */
    public int getAsInt() {
        return (value[Place.SECOND.ordinal()] & BYTE_MAX) << BYTE_SIZE_BITS
                | (value[Place.FIRST.ordinal()] & BYTE_MAX);
    }

    /**
     * This method returns the value as a byte array.
     *
     * @return byte[] - the value as a byte array
     */
    public byte[] value() {
        return value;
    }
}
