package comm.protocol;

/**
 * This class represents the status information sent by the clifton.
 * It is included in the telemetry data
 * The status information are encoded into a single byte.
 */
public class StatusInfo {
    /**
     * Is the autonomus mode activated.
     */
    private static final int IS_AUTONOMOUS_MASK = 0b00000001;
    /**
     * The packed information.
     */
    private final byte packedInfo;

    /**
     * Constructor for the Class SatusInfo.
     *
     * @param pInfo data packed as byte
     */
    public StatusInfo(final byte pInfo) {
        this.packedInfo = pInfo;
    }

    /**
     * This method checks if the clifton is in autonomous mode.
     *
     * @return boolean - true if the clifton is in autonomous mode,
     * false otherwise
     */
    public boolean isAutonomous() {
        return (packedInfo & IS_AUTONOMOUS_MASK) == 1;
    }

    /**
     * This method returns the byte representation of the status information.
     *
     * @return byte - the byte representation of the status information
     */
    public byte value() {
        return packedInfo;
    }
}
