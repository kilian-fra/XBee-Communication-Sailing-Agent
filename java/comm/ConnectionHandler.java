package comm;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue; //Thread safe queue
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import comm.protocol.CliftonCommand;
import comm.protocol.TelemetryData;

import utils.observer_pattern.Observable;

import static comm.Constants.TELEMETRY_SIZE;

/**
 * This class is responsible for handling the connection-
 *  between the laptop and the clifton
 * It is responsible for sending commands to the clifton and-
 *  receiving telemetry data from the clifton
 * by using a subescriber pattern
 * The class is provided by a Controller and is useable for other components.
 */
public class ConnectionHandler extends Observable {
    /**
     * Represents the connection.
     */
    private final IConnection connection;
    /**
     * Holds reference to the Threat that handels data Transmission.
     */
    private Thread transmitWorkerThread;
    /**
     * Holds reference to the Threat that waits for data to be received.
     */
    private Thread receiveWorkerThread;
    /**
     * Queue of commands to be sent to the clifton.
     */
    private final Queue<CliftonCommand> cliftonCommands;
    /**
     * Flag to signal Thrads if they should be running.
     */
    private AtomicBoolean workerThreadsRunning;
    /**
     * Holds the Data received from the Clifton.
     */
    private TelemetryData currentTelemetryData;
    /**
     * Flag if the last heartbeat was send successfuly.
     */
    private AtomicBoolean isHeartbeatSuccess;
    /**
     * Timeout for Heartbeat.
     */
    private static final long HEARTBEAT_INTERVAL = 1000;
    /**
     * Holds a heartbeat.
     */
    private static final CliftonCommand HEARTBEAT_COMMAND =
            CliftonCommand.heartbeat();

    /**
     * The logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(ConnectionHandler.class);


    /**
     * Constructor.
     *
     * @param conn - the connection to the clifton (currently,
     * only XbeeConnection is supported).
     */
    public ConnectionHandler(final IConnection conn) {
        this.connection = conn;
        this.transmitWorkerThread = null;
        this.receiveWorkerThread = null;
        this.cliftonCommands = new ConcurrentLinkedQueue<>();
        this.workerThreadsRunning = new AtomicBoolean(false);
        this.currentTelemetryData = null;
        this.isHeartbeatSuccess = new AtomicBoolean(false);
    }

    /**
     * This method closes the connection and stops the worker thread.
     */
    public void close() {
        this.connection.close();
        //Signale the worker thread to stop executing
        this.workerThreadsRunning.set(false);
        try {
            this.transmitWorkerThread.join();
            this.receiveWorkerThread.join();
        } catch (InterruptedException ignored) { }
    }

    /**
     * This method checks if the connection is established.
     *
     * @return boolean - true if the connection is established, false otherwise
     */
    public boolean isConnected() {
        return this.connection.isConnected()
                && this.workerThreadsRunning.get()
                && this.isHeartbeatSuccess.get();
    }

    /**
     * This method returns the current telemetry data.
     *
     * @return TelemetryData - the current telemetry data
     */
    public TelemetryData getCurrentTelemetryData() {
        return this.currentTelemetryData;
    }

    /**
     * This method sends a command to the clifton.
     *
     * @param command - the command to be sent
     */
    public void sendCommand(final CliftonCommand command) {
        this.cliftonCommands.add(command);
    }

    /**
     * This method intializes the connection and starts the worker thread.
     *
     * @return boolean - true if the connection was successfully established,
     * false otherwise
     */
    public boolean start() {
        this.connection.create();
        if (!this.connection.isConnected()) {
            LOGGER.error("XBeeSerialConnection could not be established");
            return false;
        }

        //Initialize and start both worker threads
        this.workerThreadsRunning.set(true);
        this.isHeartbeatSuccess.set(true);
        this.transmitWorkerThread = new Thread(this::transmitWorker);
        this.receiveWorkerThread = new Thread(this::receiveWorker);

        try {
            this.transmitWorkerThread.start();
            this.receiveWorkerThread.start();
        } catch (Exception e) {
            this.connection.close();
            LOGGER.error("Failed to start worker threads", e);
            return false;
        }

        LOGGER.info("ConnectionHandler started successfully");
        return true;
    }

    /**
     * Worker thread to publish commands to the clifton and handle heartbeat.
     */
    private void transmitWorker() {

        long lastTransmissionTime = System.currentTimeMillis();

        while (this.workerThreadsRunning.get()) {
            //Process all commands in the queue (if any available)
            while (!this.cliftonCommands.isEmpty() && this.isHeartbeatSuccess.get()) {
                final var command = this.cliftonCommands.poll();
                if (!this.connection
                        .sendData(command.toByteArray())) {
                    LOGGER.error("Failed to write to serial port"
                        + " while sending command");
                } else {
                    LOGGER.info("Command transmitted: {}", command.toString());
                }

                lastTransmissionTime = System.currentTimeMillis();
            }

            /*
             * Check if heartbeat is required.
             * This is the case when there are no commands to send for at least
             * 1 second
             */
            if (System.currentTimeMillis()
                    - lastTransmissionTime > HEARTBEAT_INTERVAL) {
                if (!this.connection
                        .sendData(HEARTBEAT_COMMAND.toByteArray())) {
                    LOGGER.error("Failed to write to serial port "
                        + "while sending Heartbeat");
                }

                LOGGER.info("heartbeat transmitted");
                lastTransmissionTime = System.currentTimeMillis();
            }

            //Sleep for 1ms, to reduce CPU usage
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) { }
        }

        LOGGER.info("transmitWorker exited");
    }

    /**
     * Worker thread to receive telemetry data from the clifton.
     */
    private void receiveWorker() {
        while (this.workerThreadsRunning.get()) {
            final var byteData = this.connection
                                    .receiveData(TELEMETRY_SIZE);

            //Convert the byte array to a structured telemetry data object
            final var telemetryData = TelemetryData
                                        .fromByteArray(byteData.orElseGet(
                                                        () -> new byte[0]));
            if (telemetryData.isEmpty()) {
                //No data or wrong data received -> connection lost
                //or is not fully established yet
                this.isHeartbeatSuccess.set(false);
                this.announceChange();
                LOGGER.error("Sail Agent disconnected...Attempting to connect");
                continue;
            }

            LOGGER.info("Telemetry Data: {}", telemetryData.toString());

            //Reconnect, if connection was lost
            if (!this.isHeartbeatSuccess.get()) {
                LOGGER.info("Sail Agent connected");
                this.isHeartbeatSuccess.set(true);
            }

            this.currentTelemetryData = telemetryData.get();
            this.announceChange();
        }

        LOGGER.info("receiveWorker exited");
    }
}
