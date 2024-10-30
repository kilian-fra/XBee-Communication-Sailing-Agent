package comm.protocol;

import static comm.Constants.BYTE_MAX;
import static comm.Constants.BYTE_SIZE_BITS;
import static comm.Constants.TELEMETRY_SIZE;

import java.util.Optional;

import comm.Constants.Place;

/**
 * This class represents the telemetry data sent by the clifton.
 * @param windDirection - the direction of the wind
 * @param windSpeed - the speed of the wind
 * @param agentSpeed - the speed of the agent
 * @param agentPosX - the position of the agent in the x-axis
 * @param agentPosY - the position of the agent in the y-axis
 * @param batteryStatus - the battery status of the agent
 * @param agentDirection - the direction of the agent
 * @param statusInfo - the status information
 */
public record TelemetryData(
    UnsignedShort windDirection,
    UnsignedShort windSpeed,
    UnsignedShort agentSpeed,
    short agentPosX,
    short agentPosY,
    byte batteryStatus,
    UnsignedShort agentDirection,
    StatusInfo statusInfo
) {

    /**
     * This function creates a new TelemetryData object from a byte array.
     *
     * @param data - the byte array to be converted
     * @return Optional<TelemetryData> - the TelemetryData object if the byte
     *         array is valid, empty otherwise
     */
    public static Optional<TelemetryData> fromByteArray(final byte[] data) {
        return data == null || data.length != TELEMETRY_SIZE ? Optional.empty()
        : Optional.of(
        new TelemetryData(
            new UnsignedShort(data[Place.FIRST.ordinal()],
                    data[Place.SECOND.ordinal()]),
            new UnsignedShort(data[Place.THIRD.ordinal()],
                    data[Place.FOURTH.ordinal()]),
            new UnsignedShort(data[Place.FIFTH.ordinal()],
                    data[Place.SIXTH.ordinal()]),
            (short) (((data[Place.EIGHTH.ordinal()] & BYTE_MAX) << BYTE_SIZE_BITS)
                    | (data[Place.SEVENTH.ordinal()] & BYTE_MAX)),
            (short) (((data[Place.TENTH.ordinal()] & BYTE_MAX) << BYTE_SIZE_BITS)
                    | (data[Place.NINTH.ordinal()] & BYTE_MAX)),
            data[Place.ELEVENTH.ordinal()],
            new UnsignedShort(data[Place.TWELFTH.ordinal()],
                    data[Place.THIRTEENTH.ordinal()]),
            new StatusInfo(data[Place.FOURTEENTH.ordinal()])
        ));
    }

    /**
     * This method converts the telemetry data to a byte array.
     * It is mainly used for testing purposes
     *
     * @return byte[] - the byte array representation of the telemetry data
     */
    public byte[] toByteArray() {
        byte[] data = new byte[TELEMETRY_SIZE];
        data[Place.FIRST.ordinal()] = windDirection.value()
                                        [Place.FIRST.ordinal()];
        data[Place.SECOND.ordinal()] = windDirection.value()
                                        [Place.SECOND.ordinal()];
        data[Place.THIRD.ordinal()] = windSpeed.value()
                                        [Place.FIRST.ordinal()];
        data[Place.FOURTH.ordinal()] = windSpeed.value()
                                        [Place.SECOND.ordinal()];
        data[Place.FIFTH.ordinal()] = agentSpeed.value()
                                        [Place.FIRST.ordinal()];
        data[Place.SIXTH.ordinal()] = agentSpeed.value()
                                        [Place.SECOND.ordinal()];
        data[Place.SEVENTH.ordinal()] = (byte) (agentPosX & BYTE_MAX);
        data[Place.EIGHTH.ordinal()] = (byte) (agentPosX >> BYTE_SIZE_BITS);
        data[Place.NINTH.ordinal()] = (byte) (agentPosY & BYTE_MAX);
        data[Place.TENTH.ordinal()] = (byte) (agentPosY >> BYTE_SIZE_BITS);
        data[Place.ELEVENTH.ordinal()] = batteryStatus;
        data[Place.TWELFTH.ordinal()] = agentDirection.value()
                                        [Place.FIRST.ordinal()];
        data[Place.THIRTEENTH.ordinal()] = agentDirection.value()
                                            [Place.SECOND.ordinal()];
        data[Place.FOURTEENTH.ordinal()] = statusInfo.value();
        return data;
    }

    @Override
    public String toString() {
        return "TelemetryData [windDirection=" + windDirection.getAsInt()
                + ", windSpeed=" + windSpeed.getAsInt()
                + ", agentSpeed=" + agentSpeed.getAsInt()
                + ", agentPosX=" + agentPosX
                + ", agentPosY=" + agentPosY
                + ", batteryStatus=" + batteryStatus
                + ", agentDirection=" + agentDirection.getAsInt()
                + ", statusInfo=" + statusInfo.value()
                + "]";
    }

    /**
     * This method returns the telemetry data as a string.
     * It is mainly used for testing purposes
     *
     * @return String - the telemetry data as a string
     */
    public String getAsTestString() {
        return windDirection.getAsInt() + ", " + windSpeed.getAsInt() + ", "
                + agentSpeed.getAsInt() + ", " + agentPosX + ", " + agentPosY
                + ", " + (int) batteryStatus + ", " + agentDirection.getAsInt()
                + ", " + statusInfo.value();
    }
}
