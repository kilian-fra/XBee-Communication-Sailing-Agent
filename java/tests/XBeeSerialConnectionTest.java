package comm;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for XBeeSerialConnection.
 */
public class XBeeSerialConnectionTest {

    /**
     * The baud rate for the serial connection.
     */
    private static final int BAUD_RATE = 38400;

    /**
     * The port for the Clifton connection.
     */
    private static final String CLIFTON_PORT = "COM1";

    /**
     * The port for the GUI connection.
     */
    private static final String GUI_PORT = "COM2";

    /**
     * The IConnection instance for Clifton.
     */
    private IConnection clifton;

    /**
     * The IConnection instance for GUI.
     */
    private IConnection gui;

    /**
     * Sets up the test environment before each test.
     * Initializes the Clifton and GUI connections.
     */
    @BeforeEach
    public void setUp() {
        this.clifton = XBeeSerialConnection.create(CLIFTON_PORT, BAUD_RATE);
        this.gui = XBeeSerialConnection.create(GUI_PORT, BAUD_RATE);
        this.clifton.create();
        this.gui.create();
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
     * Tests if the connections to Clifton and GUI are successfully established.
     */
    @Test
    public void testConnection() {
        assertTrue(this.clifton.isConnected());
        assertTrue(this.gui.isConnected());
    }

    /**
     * Tests data transmission between Clifton and GUI connections.
     * Sends data from Clifton and verifies it is received correctly by GUI.
     */
    @Test
    public void testDataTransmission() {
        final var data = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        assertTrue(this.clifton.sendData(data));

        final var receivedData = this.gui.receiveData(5);
        assertTrue(receivedData.isPresent());
        assertArrayEquals(data, receivedData.get());
    }
}
