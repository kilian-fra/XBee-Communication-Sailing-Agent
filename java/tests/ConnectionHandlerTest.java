package comm;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import comm.protocol.TelemetryData;
import utils.observer_pattern.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for ConnectionHandler.
 */
public class ConnectionHandlerTest implements Observer {

    /**
     * The baud rate for the serial connection.
     */
    private static final int BAUD_RATE = 38400;

    /**
     * Pseudo telemetry data for testing.
     */
    private static final byte[] TELEMETRY_DATA
        = new byte[] { 0x01, 0x02, 0x03, 0x04,
                       0x05, 0x06, 0x07, 0x08,
                       0x09, 0x0A, 0x0B, 0x0C,
                       0x0D, 0x0E };

    /**
     * The port for the Clifton connection.
     */
    private static final String CLIFTON_PORT = "/dev/pts/1";

    /**
     * The port for the GUI connection.
     */
    private static final String GUI_PORT = "/dev/pts/2";

    /**
     * The IConnection instance for Clifton.
     */
    private IConnection clifton;

    /**
     * The ConnectionHandler instance for GUI.
     */
    private ConnectionHandler gui;

    /**
     * The received telemetry data.
     */
    private TelemetryData receivedTelemetryData;

    /**
     * Sets up the test environment before each test.
     * Initializes the Clifton and GUI connections.
     */
    @BeforeEach
    public void setUp() {
        this.clifton = XBeeSerialConnection.create(CLIFTON_PORT, BAUD_RATE);
        this.gui = new ConnectionHandler(XBeeSerialConnection.create(GUI_PORT, BAUD_RATE));
        this.clifton.create();
        assertTrue(this.clifton.isConnected());

        // Initialize the connection handler
        assertTrue(this.gui.start());
    }

    /**
     * Cleans up the test environment after each test.
     * Closes the Clifton and GUI connections.
     */
    @AfterEach
    public void cleanUp() {
        this.clifton.close();
        this.gui.close();
        assertFalse(this.clifton.isConnected());
        assertFalse(this.gui.isConnected());
    }

    /**
     * Tests the transmission of telemetry data.
     * Sends telemetry data from Clifton and verifies it is received correctly by
     * GUI.
     */
    @Test
    public void testTelemetryDataTransmission() {
        // Attach to GUI to receive telemetry data
        this.gui.attach(this);

        // Send telemetry data to the GUI
        assertTrue(this.clifton.sendData(TELEMETRY_DATA));

        final int sleepTime = 200;

        // Check if the telemetry data was received by the GUI
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignored) {
        }

        assertNotNull(this.receivedTelemetryData);
        final var receivedTelemetryDataBytes = this.receivedTelemetryData.toByteArray();
        for (int i = 0; i < TELEMETRY_DATA.length; i++) {
            assertEquals(TELEMETRY_DATA[i], receivedTelemetryDataBytes[i]);
        }
    }

    /**
     * Tests the heartbeat and reconnect functionality.
     * Verifies the GUI connection status before and after a delay.
     */
    @Test
    public void testHeartbeatAndReconnect() {
        assertTrue(this.gui.isConnected());

        final int sleepTime = 3500;

        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignored) {
        }

        assertFalse(this.gui.isConnected());

        //Send telem data and receive heartbeat from GUI
        final var heartbeatCmdSize = 3;
        assertTrue(this.clifton.sendData(TELEMETRY_DATA));
        assertTrue(this.clifton.receiveData(heartbeatCmdSize).isPresent());

        final var sleepTime2 = 10;

        //Wait, so the heartbeat flag can be set again
        try {
            Thread.sleep(sleepTime2);
        } catch (InterruptedException ignored) {
        }

        //Should be connected again
        assertTrue(this.gui.isConnected());
    }

    /**
     * Updates the received telemetry data.
     */
    @Override
    public void update() {
        this.receivedTelemetryData = this.gui.getCurrentTelemetryData();
    }
}
