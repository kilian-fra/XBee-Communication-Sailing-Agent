package comm;

/**
 * This class contains the constants used for the communication.
 */
public final class Constants {

    /**
     * This class should not be instantiated.
     */
    private Constants() { }

    /**
     * The size of the telemetry data in bytes.
     */
    public static final int TELEMETRY_SIZE = 14;
    /**
     * The size of a byte in bits.
     */
    public static final int BYTE_SIZE_BITS = 8;
    /**
     * The maximum value of a byte.
     */
    public static final int BYTE_MAX = 0xFF;

    /**
     * This enum represents for example indices in a byte array.
     */
    public enum Place {
        /**
         * Represents the first index.
         */
        FIRST,
        /**
         * Represents the second index.
         */
        SECOND,
        /**
         * Represents the third index.
         */
        THIRD,
        /**
         * Represents the fourth index.
         */
        FOURTH,
        /**
         * Represents the fifth index.
         */
        FIFTH,
        /**
         * Represents the sixth index.
         */
        SIXTH,
        /**
         * Represents the seventh index.
         */
        SEVENTH,
        /**
         * Represents the eighth index.
         */
        EIGHTH,
        /**
         * Represents the ninth index.
         */
        NINTH,
        /**
         * Represents the tenth index.
         */
        TENTH,
        /**
         * Represents the eleventh index.
         */
        ELEVENTH,
        /**
         * Represents the twelfth index.
         */
        TWELFTH,
        /**
         * Represents the thirteenth index.
         */
        THIRTEENTH,
        /**
         * Represents the fourteenth index.
         */
        FOURTEENTH
    }
}
