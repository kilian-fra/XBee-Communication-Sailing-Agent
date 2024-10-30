package comm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import comm.protocol.TelemetryData;

/**
 * This class is used to test the TelemetryData class.
 * The fromByteArray method is tested to ensure the raw byte data
 * received from the clifton is correctly converted to a TelemetryData object.
 */
public class TelemetryDataTest {

    /**
     * This class is used to store the raw byte data and the expected.
     */
    private static class TestData {
        /**
         * rawByteData - the raw byte data to be converted to a TelemetryData object.
         */
        private final byte[] rawByteData;
        /**
         * expectedTelemetryData - the expected TelemetryData object as a string.
         */
        private final String expectedTelemetryData;

        /**
         * This constructor initializes the raw byte data and the expected
         * TelemetryData object.
         *
         * @param bytes - the raw byte data
         * @param expected - the expected TelemetryData object as a string
         */
        TestData(final byte[] bytes, final String expected) {
            this.rawByteData = bytes;
            this.expectedTelemetryData = expected;
        }
        /**
         * This method returns the raw byte data.
         *
         * @return the raw byte data
         */
        public byte[] getRawByteData() {
            return rawByteData;
        }

        /**
         * This method returns the expected TelemetryData object as string.
         *
         *
         * @return the expected TelemetryData object as String
         */
        public String getExpectedTelemetryData() {
            return expectedTelemetryData;
        }
    }

    /**
     * This list is used to store the raw byte data and the expected TelemetryData object.
     */
    private static final List<TestData> TELEM_TEST_DATA = List.of(
        new TestData(
            /*
            data.windDirection = 359;
            data.windSpeed = 1000;
            data.agentSpeed = 500;
            data.agentPosX = 30000;
            data.agentPosY = 16000;
            data.batteryStatus = 70;
            data.agentDirection = 180;
            data.statusInfo.isAutonomous = 1;
            */
            new byte[] {
                (byte) 0x67, (byte) 0x01, (byte) 0xe8,
                (byte) 0x03, (byte) 0xf4, (byte) 0x01,
                (byte) 0x30, (byte) 0x75, (byte) 0x80,
                (byte) 0x3e, (byte) 0x46, (byte) 0xb4,
                (byte) 0x00, (byte) 0x01
            },
            "359, 1000, 500, 30000, 16000, 70, 180, 1"
        ),
        new TestData(
            /*
            data.windDirection = __UINT16_MAX__;
            data.windSpeed = __UINT16_MAX__;
            data.agentSpeed = __UINT16_MAX__;
            data.agentPosX = __INT16_MAX__;
            data.agentPosY = __INT16_MAX__;
            data.batteryStatus = __UINT8_MAX__;
            data.agentDirection = __UINT16_MAX__;
            data.statusInfo.isAutonomous = 1;
            */
            new byte[] {
                (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0x7f, (byte) 0xff,
                (byte) 0x7f, (byte) 0x64, (byte) 0xff,
                (byte) 0xff, (byte) 0x01
            },
            "65535, 65535, 65535, 32767, 32767, 100, 65535, 1"
        ),
        new TestData(
            /*
            data.windDirection = 0;
            data.windSpeed = 9000;
            data.agentSpeed = 700;
            data.agentPosX = 3000;
            data.agentPosY = 6500;
            data.batteryStatus = 30;
            data.agentDirection = 10;
            data.statusInfo.isAutonomous = 0;
            */
            new byte[] {
                (byte) 0x00, (byte) 0x00, (byte) 0x28,
                (byte) 0x23, (byte) 0xbc, (byte) 0x02,
                (byte) 0xb8, (byte) 0x0b, (byte) 0x64,
                (byte) 0x19, (byte) 0x1e, (byte) 0x0a,
                (byte) 0x00, (byte) 0x00
            },
            "0, 9000, 700, 3000, 6500, 30, 10, 0"
        )
    );

    /**
     * This method tests the fromByteArray method of the TelemetryData class.
     */
    @Test
    public void testFromByteArray() {
        TELEM_TEST_DATA.stream().forEach(data -> {
            final var telemetryData = TelemetryData
                                        .fromByteArray(data.getRawByteData()).get();

            assertEquals(telemetryData.getAsTestString(), data.getExpectedTelemetryData());
        });
    }
}
