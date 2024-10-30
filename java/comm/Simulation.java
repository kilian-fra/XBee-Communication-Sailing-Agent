package comm;

import comm.protocol.CliftonCommand;
import comm.protocol.TelemetryData;
import comm.protocol.UnsignedShort;
import comm.protocol.StatusInfo;
import java.util.Random;

import static comm.protocol.CliftonCommand.MAX_COMMAND_SIZE;
import static comm.protocol.CliftonCommand.COMMAND_IDX;

public class Simulation {
// +---------------------------------------------------+
// |                    Config                         |
// +---------------------------------------------------+
    /**
     * Initial Wind Direction.
     */
    private static final int INITIAL_DIRECTION = 0;

    /**
     * Maximal Change in Wind Direction.
     */
    private static final int MAX_CHANGE = 20;

    /**
     * initial Wind Speed.
     */
    private static final int INITIAL_SPEED = 0;

    /**
     * Maximal Change in Wind Speed.
     */
    private static final int MAX_CHANGE_SPEED = 200;

    /**
     * Minimal Wind Speed.
     */
    private static final int MIN_SPEED = 0;

    /**
     * Maximal Wind Speed.
     */
    private static final int MAX_SPEED = 3000;

    /**
     * The position of the agent in the x-axis.
     */
    private static final short AGENT_POS_X = 0;

    /**
     * The position of the agent in the y-axis.
     */
    private static final short AGENT_POS_Y = 0;

    /**
     * The speed of the agent.
     */
    private static final int AGENT_SPEED = 270;

    /**
     * The direction of the agent.
     */
    private static final int AGENT_DIRECTION = 10;

    /**
     * The battery status of the agent.
     */
    private static final byte BATTERY_STATUS = 100;

    /**
     * The status information.
     */
    private static final byte STATUS_INFO = 0b00000000;

    /**
     * The baud rate for the serial connection.
     */
    private static final int BAUD_RATE = 38400;

    /**
     * The interval for sending telemetry data in milliseconds.
     */
    private static final int SEND_TELEMETRY_INTERVAL = 1000;

    /**
     * The port for the Clifton connection.
     */
    private static final String CLIFTON_PORT = "/dev/pts/4";

    /**
     * The port for the GUI connection.
     */
    private static final String GUI_PORT = "/dev/pts/5";

// +---------------------------------------------------+
// |                 Config End                        |
// +---------------------------------------------------+

    /**
     * The connection handler for the GUI and Clifton.
     */
    private ConnectionHandler connectionHandler;

    /**
     * The connection to the Clifton.
     */
    private IConnection cliftonClient;

    /**
     * Flag to indicate if the simulation is running.
     */
    private volatile boolean running = false;

    /**
     * The worker threads for sending telemetry data and receiving commands.
     */
    private Thread sendTelemetryThread;

    /**
     * The worker thread for receiving commands.
     */
    private Thread receiveCommandsThread;

    /**
     * Current Wind Direction.
     */
    private int currentDirection = INITIAL_DIRECTION;

    /**
     * Random.
     */
    private Random random = new Random();

    /**
     * Max Generated Value.
     */
    private static final int MAX_RAND = 7;

    /**
     * Shift for Random value to Negatives.
     */
    private static final int SHIFT = MAX_RAND / 2;

    /**
     * Degrees in a Circle.
     */
    private static final int DEGREES = 360;

    /**
     * Current Wind Speed.
     */
    private int currentSpeed = INITIAL_SPEED;

    /**
     * Shift Wind Speed for negative values.
     */
    private static final int SHIFT_SPEED = MAX_CHANGE_SPEED / 2;

    /**
     * Starts the simulation.
     * @return true if the simulation started successfully, false otherwise.
     */
    public boolean start() {
        this.cliftonClient = XBeeSerialConnection
                                .create(CLIFTON_PORT, BAUD_RATE);
        this.connectionHandler = new ConnectionHandler(
                                    XBeeSerialConnection
                                        .create(GUI_PORT, BAUD_RATE));
        this.cliftonClient.create();

        if (!this.cliftonClient.isConnected()) {
            return false;
        }

        if (!this.connectionHandler.start()) {
            this.cliftonClient.close();
            return false;
        }

        this.running = true;

        // Start the worker threads
        this.sendTelemetryThread = new Thread(this::sendTelemetryWorker);
        this.receiveCommandsThread = new Thread(this::receiveCommandsWorker);

        this.sendTelemetryThread.start();
        this.receiveCommandsThread.start();

        return true;
    }

    /**
     * Stops the simulation.
     */
    public void stop() {
        this.running = false;

        try {
            this.sendTelemetryThread.join();
            this.receiveCommandsThread.join();
        } catch (InterruptedException e) { }
    }

    /**
     * Worker thread for sending the telemetry data to the GUI.
     */
    public void sendTelemetryWorker() {
        while (this.running) {

            //Generate telemetry data
            this.genNextDirection();
            this.getNextSpeed();

            //Fill out telemetry data
            final var telemetryData = new TelemetryData(
                new UnsignedShort(currentDirection),
                new UnsignedShort(currentSpeed),
                new UnsignedShort(AGENT_SPEED),
                AGENT_POS_X,
                AGENT_POS_Y,
                BATTERY_STATUS,
                new UnsignedShort(AGENT_DIRECTION),
                new StatusInfo(STATUS_INFO)
            );

            System.out.println("Sending telemetry data:\n"
                + telemetryData.toString());

            //Send telemetry data as byte array
            this.cliftonClient.sendData(telemetryData.toByteArray());

            try {
                Thread.sleep(SEND_TELEMETRY_INTERVAL);
            } catch (InterruptedException ignored) { }
        }
    }

    /**
     * Worker thread for receiving the commands from the GUI.
     */
    public void receiveCommandsWorker() {
        while (this.running) {
            //Receive command id as byte first
            final var command = this.cliftonClient
                                    .receiveData(MAX_COMMAND_SIZE)
                                    .orElse(new byte[] {Byte.MAX_VALUE, 0, 0});

            switch (CliftonCommand.ID.fromByte(command[COMMAND_IDX])) {
                case HEARTBEAT:
                    System.out.println("Sail agent received heartbeat");
                    break;

                case START_ROUTE:
                    System.out.println("Sail agent received start route");
                    break;

                case STOP_ROUTE:
                    System.out.println("Sail agent received stop route");
                    break;

                case SET_COURSE:
                    final var course = new UnsignedShort(command[1], command[2]);
                    System.out.println("Sail agent received course: " + course.getAsInt());
                    break;

                default:
                    System.out.println("Sail agent received unknown command");
                    break;
            }
        }
    }

    /**
     * Get the connection handler.
     * @return the connection handler.
     */
    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    /**
     * Generate new Wind Direction.
     */
    public void genNextDirection() {
        int change = random.nextInt(MAX_RAND) - SHIFT;
        int newDirection = currentDirection + change;

        if (newDirection < (INITIAL_DIRECTION - MAX_CHANGE + DEGREES) % DEGREES
                && !(newDirection <= (INITIAL_DIRECTION + MAX_CHANGE) % DEGREES)) {
            newDirection = INITIAL_DIRECTION - MAX_CHANGE;
        } else if (newDirection > (INITIAL_DIRECTION + MAX_CHANGE) % DEGREES
                && !(newDirection >= (INITIAL_DIRECTION - MAX_CHANGE + DEGREES)
                % DEGREES)) {
            newDirection = INITIAL_DIRECTION + MAX_CHANGE;
        }

        currentDirection = (newDirection + DEGREES) % DEGREES;
    }

    /**
     * Generate new Wind Speed.
     */
    public void getNextSpeed() {
        int change = (random.nextInt(MAX_CHANGE_SPEED) - SHIFT_SPEED);
        int newSpeed = currentSpeed + change;

        if (newSpeed < MIN_SPEED) {
            newSpeed = MIN_SPEED;
        } else if (newSpeed > MAX_SPEED) {
            newSpeed = MAX_SPEED;
        }
        currentSpeed = newSpeed;
    }
/*
    public static void main(String[] args) {
        final var simulation = new Simulation();
        if (simulation.start()) {
            System.out.println("Simulation started successfully");
        } else {
            System.out.println("Failed to start simulation");
        }

        simulation.getConnectionHandler()
            .sendCommand(CliftonCommand.setCourse(90));
    }
*/
}
