package comm;

import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class represents a point to point connection between the
 * laptop and the clifton via. the XBee devices.
 */
public final class XBeeSerialConnection implements IConnection {
    /**
     * The port to the serial interface.
     */
    private final String port;
    /**
     * The baud rate to use.
     */
    private final int baudRate;
    /**
     * The serial port.
     */
    private SerialPort serialPort;
    /**
     * The input stream.
     */
    private InputStream inputStream;
    /**
     * The output stream.
     */
    private OutputStream outputStream;
    /**
     * The connection status.
     */
    private boolean isConnected;

    /**
     * The read and write timeout.
     */
    private static final int READ_WRITE_TIMEOUT = 3000;
    /**
     * The data bits.
     */
    private static final int DATA_BITS = 8;

    /**
     * The logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(XBeeSerialConnection.class);


    /**
     * Constructor for the class XBeeSerialConnection.
     * @param sPort - the port to the serial interface
     * @param bRate - the baud rate to use
     */
    private XBeeSerialConnection(final String sPort, final int bRate) {
        this.port = sPort;
        this.baudRate = bRate;
    }

    /**
     * This method creates an instance of the class XBeeSerialConnection.
     *
     * @param sPort - the port to the serial interface
     * @param bRate - the baud rate to use
     * @return IConnection - the created instance
     */
    public static IConnection create(final String sPort, final int bRate) {
        return new XBeeSerialConnection(sPort, bRate);
    }

    /**
     * This method creates the connection.
     */
    @Override
    public void create() {
        this.serialPort = SerialPort.getCommPort(this.port);
        this.serialPort.setBaudRate(this.baudRate);

        //Configure the serial port
        this.serialPort.setComPortParameters(this.baudRate,
                                             DATA_BITS,
                                             SerialPort.ONE_STOP_BIT,
                                             SerialPort.NO_PARITY);
        this.serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING
                                        | SerialPort.TIMEOUT_WRITE_BLOCKING,
                                        READ_WRITE_TIMEOUT,
                                        READ_WRITE_TIMEOUT);

        if (this.serialPort.openPort()) {
            this.isConnected = true;
            this.serialPort.flushIOBuffers();
            this.inputStream = this.serialPort.getInputStream();
            this.outputStream = this.serialPort.getOutputStream();
        } else {
            this.isConnected = false;
            LOGGER.error("Failed to open XBee Serial Port: {}",
                this.serialPort.getLastErrorCode());
        }
    }

    /**
     * This method closes the connection.
     */
    @Override
    public void close() {
        if (this.serialPort != null && this.serialPort.isOpen()) {
            this.serialPort.closePort();
            this.isConnected = false;
        }
    }

    /**
     * This method sends data to the output stream.
     *
     * @param data - the data to be sent
     * @return boolean - true if the data was successfully sent,
     * false otherwise
     * @throws IOException
     */
    @Override
    public boolean sendData(final byte[] data) {
        try {
            this.outputStream.write(data);
            //this.outputStream.flush();
            return true;
        } catch (IOException e) { //SerialPortTimeoutException
            return false;
        }
    }

    /**
     * This method receives a known amount of data from the input stream.
     *
     * @param length - the amount of data to be received
     * @return Optional<byte[]> - the received data
     * @throws IOException
     */
    @Override
    public Optional<byte[]> receiveData(final int length) {
        final var data = new byte[length];
        try {
            int totalBytesRead = 0;
            while (totalBytesRead < length) {
                final var bytesRead = this.inputStream
                    .read(data, totalBytesRead, length - totalBytesRead);
                if (bytesRead == -1) {
                    // End of stream reached prematurely
                    LOGGER.error("End of stream reached prematurely");
                }
                totalBytesRead += bytesRead;
            }
            return Optional.of(data);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * This method receives an unknown amount of data from the input stream.
     *
     * @return Optional<byte[]> - the received data
     * @throws IOException
     */
    @Override
    public Optional<byte[]> receiveData() {
        try {
            return Optional.of(this.inputStream.readAllBytes());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * This method checks if the connection is established.
     *
     * @return boolean - true if the connection is established, false otherwise
     */
    @Override
    public boolean isConnected() {
        return this.isConnected;
    }
}
