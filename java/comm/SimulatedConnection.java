package comm;

import java.util.Optional;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.plaf.DimensionUIResource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import comm.protocol.CliftonCommand;
import comm.protocol.StatusInfo;
import comm.protocol.TelemetryData;
import comm.protocol.UnsignedShort;

import static comm.protocol.CliftonCommand.COMMAND_IDX;

/**
 * A simulated Connection, that opens a simple application to input the values
 * to be simulated.
 * The simulated values are treated as if received by the communication module.
 * To use this SimulatedConnection, simply pass it to the ConnectionHandler
 * instead of the XBeeSerialConnection.
 *
 * @author Malte Fischer
 * @author Kilian Franke
 */
public final class SimulatedConnection implements IConnection {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * The initial width of the application window.
     */
    private static final int WIDTH_OF_APP = 500;

    /**
     * The initial height of the application window.
     */
    private static final int HEIGHT_OF_APP = 500;

    /**
     * The width of the text field of the menu entries.
     */
    private static final int WIDTH_OF_MENU_TEXTFIELD = 150;

    /**
     * The height of the text field of the menu entries.
     */
    private static final int HEIGHT_OF_MENU_TEXTFIELD = 30;

    /**
     * The interval for sending telemetry data in milliseconds.
     */
    private static final int SEND_TELEMETRY_INTERVAL = 125;

    /**
     * Whether to send the data every SEND_TELEMETRY_INTERVAL (true), or wait until
     * the user clicks update (false).
     */
    private static final boolean SIMULATE_HIGH_FREQUENCY = false;

    /**
     * The main frame of the test-data input application.
     */
    private JFrame jFrame;

    /**
     * The text field for the agent speed.
     */
    private JTextField agentSpeedInput;

    /**
     * The text field for the agent direction.
     */
    private JTextField agentDirectionInput;

    /**
     * The text field for the wind direction.
     */
    private JTextField windDirectionInput;

    /**
     * The text field for the wind speed.
     */
    private JTextField windSpeedInput;

    /**
     * The text field for the x position of the agent.
     */
    private JTextField agentPositionXInput;

    /**
     * The text field for the y position of the agent.
     */
    private JTextField agentPositionYInput;

    /**
     * The text field for the battery percentage of the agent.
     */
    private JTextField batteryInput;

    /**
     * The check box if the mode is autonomous.
     */
    private JCheckBox autonomousCheckBox;

    /**
     * The check box if the agent should be simulated as 'connected'.
     */
    private JCheckBox connectedCheckBox;

    /**
     * If the receiveData method is allowed to read the input values. Used for the
     * update button.
     */
    private boolean isAllowedToUpdate = true;

    @Override
    public void create() {
        this.jFrame = new JFrame("Testinput für des autonomen Seglers");
        this.jFrame.setSize(WIDTH_OF_APP, HEIGHT_OF_APP);

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));

        agentSpeedInput = new JTextField(String.valueOf(0));
        mainContent.add(createMenuEntry("Agentspeed (cm/s)", agentSpeedInput));

        agentDirectionInput = new JTextField(String.valueOf(0));
        mainContent.add(createMenuEntry("Agentdirection", agentDirectionInput));

        windDirectionInput = new JTextField(String.valueOf(0));
        mainContent.add(createMenuEntry("Winddirection", windDirectionInput));

        windSpeedInput = new JTextField(String.valueOf(0));
        mainContent.add(createMenuEntry("Windspeed (cm/s)", windSpeedInput));

        agentPositionXInput = new JTextField(String.valueOf(0));
        mainContent.add(createMenuEntry("AgentPositionX", agentPositionXInput));

        agentPositionYInput = new JTextField(String.valueOf(0));
        mainContent.add(createMenuEntry("AgentPositionY", agentPositionYInput));

        batteryInput = new JTextField(String.valueOf(0));
        mainContent.add(createMenuEntry("Battery (%)", batteryInput));

        JPanel autonomousJPanel = new JPanel();
        JLabel autonomousJLabel = new JLabel("Autonomous?");
        this.autonomousCheckBox = new JCheckBox();
        autonomousJPanel.add(autonomousJLabel);
        autonomousJPanel.add(autonomousCheckBox);
        mainContent.add(autonomousJPanel);

        JPanel connectedJPanel = new JPanel();
        JLabel connectedJLabel = new JLabel("Connected?");
        this.connectedCheckBox = new JCheckBox();
        this.connectedCheckBox.setSelected(true);
        connectedJPanel.add(connectedJLabel);
        connectedJPanel.add(connectedCheckBox);
        mainContent.add(connectedJPanel);

        if (!SIMULATE_HIGH_FREQUENCY) {
            JButton updateButton = new JButton("Update");
            updateButton.addActionListener((e) -> {
                isAllowedToUpdate = true;
            });
            mainContent.add(updateButton);
        }

        jFrame.getContentPane().add(mainContent);

        this.jFrame.setVisible(true);
    }

    /**
     * Create a input menu entry with a label and a text field.
     *
     * @param labelText the text displayed as label
     * @param textField the text field
     * @return the JPanel as the container of the menu entry
     */
    private JPanel createMenuEntry(final String labelText, final JTextField textField) {
        JPanel panel = new JPanel();
        JLabel label = new JLabel(labelText);
        textField.setPreferredSize(new DimensionUIResource(WIDTH_OF_MENU_TEXTFIELD, HEIGHT_OF_MENU_TEXTFIELD));
        textField.addActionListener((e) -> isAllowedToUpdate = true);
        panel.add(label);
        panel.add(textField);
        return panel;
    }

    @Override
    public void close() {
        jFrame.setVisible(false);
    }

    @Override
    public boolean sendData(final byte[] data) {
        switch (CliftonCommand.ID.fromByte(data[COMMAND_IDX])) {
            case HEARTBEAT:
                // Do nothing. As this class is not intended to test the communication part, we
                // do not care if the heartbeat is successful.
                break;

            case START_ROUTE:
                LOGGER.info("Sail agent received start route");
                break;

            case STOP_ROUTE:
                LOGGER.info("Sail agent received stop route");
                break;

            case SET_COURSE:
                final var course = new UnsignedShort(data[1], data[2]);
                LOGGER.info("Sail agent received course: " + course.getAsInt());
                break;

            default:
                LOGGER.info("Sail agent received unknown command");
                break;
        }

        return true;
    }

    @Override
    public Optional<byte[]> receiveData(final int length) {
        do {
            try {
                Thread.sleep(SEND_TELEMETRY_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!isAllowedToUpdate && !SIMULATE_HIGH_FREQUENCY);

        if (!SIMULATE_HIGH_FREQUENCY) {
            isAllowedToUpdate = false;
        }

        if (!connectedCheckBox.isSelected()) {
            return Optional.empty();
        }

        byte statusInfo = autonomousCheckBox.isSelected() ? (byte) 1 : (byte) 0;

        // Fill out telemetry data
        final var telemetryData = new TelemetryData(
                new UnsignedShort(getNumber(windDirectionInput)),
                new UnsignedShort(getNumber(windSpeedInput)),
                new UnsignedShort(getNumber(agentSpeedInput)),
                (short) getNumber(agentPositionXInput),
                (short) getNumber(agentPositionYInput),
                (byte) getNumber(batteryInput),
                new UnsignedShort(getNumber(
                        agentDirectionInput)),
                new StatusInfo(statusInfo));

        LOGGER.info("Sending telemetry data: "
                + telemetryData.toString());

        return Optional.of(telemetryData.toByteArray());
    }

    /**
     * Parse the input of a text field as a number.
     *
     * @param textField the text field to extract the value from
     * @return the value of the text field as int
     */
    private int getNumber(final JTextField textField) {
        try {
            return Integer.parseInt(textField.getText());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Optional<byte[]> receiveData() {
        return this.receiveData(0);
    }

    @Override
    public boolean isConnected() {
        return true;
    }

}
