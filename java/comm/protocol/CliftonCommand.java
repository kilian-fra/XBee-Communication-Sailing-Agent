package comm.protocol;

import static comm.Constants.BYTE_MAX;
import static comm.Constants.BYTE_SIZE_BITS;

/*
 * This class represents a command sent by the laptop to the clifton
 * A command consists of an ID and optional data.
 */
public record CliftonCommand(ID id, byte[] data) {

    /**
     * The command ID for the heartbeat command.
     */
    private static final int CMD_HEARTBEAT = 0;

    /**
     * The command ID for the start route command.
     */
    private static final int CMD_START_ROUTE = 1;

    /**
     * The command ID for the stop route command.
     */
    private static final int CMD_STOP_ROUTE = 2;

    /**
     * The command ID for the set course command.
     */
    private static final int CMD_SET_COURSE = 3;

    /**
     * This enum represents the different types of commands.
     */
    public enum ID {
        /**
         * This command is used to check if the connection is still alive.
         */
        HEARTBEAT,
        /**
         * This command is used to start the route.
         */
        START_ROUTE,
        /**
         * This command is used to stop the route.
         */
        STOP_ROUTE,
        /**
         * This command is used to set the course.
         */
        SET_COURSE;

        /**
         * This function converts the byte to the command ID.
         * @param b - the byte to be converted
         * @return ID - the command ID
         */
        public static ID fromByte(final byte b) {
            return switch (b) {
                case CMD_HEARTBEAT -> HEARTBEAT;
                case CMD_START_ROUTE -> START_ROUTE;
                case CMD_STOP_ROUTE -> STOP_ROUTE;
                case CMD_SET_COURSE -> SET_COURSE;
                default -> throw new IllegalArgumentException("Invalid command ID");
            };
        }
    }

    /**
     * The maximum size of a command in bytes.
     */
    public static final int MAX_COMMAND_SIZE = 3;

    /**
     * The index of the command ID in the byte array.
     */
    public static final int COMMAND_IDX = 0;

    /**
     * This method converts the command to a byte array.
     * @return byte[] - the command as a byte array
     */
    public byte[] toByteArray() {
        if (data.length <= 0) {
            return new byte[] {(byte) id.ordinal() };
        }

        byte[] result = new byte[data.length + 1];
        result[0] = (byte) id.ordinal();
        System.arraycopy(data, 0, result, 1, data.length);
        return result;
    }

    /**
     * This function creates the command to signal-
     * the clifton to start the route.
     * @return CliftonCommand - the command to start the route
     */
    public static CliftonCommand startRoute() {
        return new CliftonCommand(ID.START_ROUTE, new byte[] {0, 0});
    }

    /**
     * This function creates the command to signal-
     * the clifton to stop the route.
     * @return CliftonCommand - the command to stop the route
     */
    public static CliftonCommand stopRoute() {
        return new CliftonCommand(ID.STOP_ROUTE, new byte[] {0, 0});
    }

    /**
     * This function creates the command to set the new course.
     * @param course - the course to be set
     * @return CliftonCommand - the command to set the new course
     */
    public static CliftonCommand setCourse(final int course) {
        return new CliftonCommand(ID.SET_COURSE, new byte[] {
            (byte) (course & BYTE_MAX),
            (byte) ((course >> BYTE_SIZE_BITS) & BYTE_MAX)
        });
    }

    /**
     * This function creates the heartbeat command.
     * @return CliftonCommand - the heartbeat command
     */
    public static CliftonCommand heartbeat() {
        return new CliftonCommand(ID.HEARTBEAT, new byte[] {0, 0});
    }

    /**
     * This method returns the string representation of the command.
     */
    @Override
    public String toString() {
        switch (id) {
            case HEARTBEAT -> {
                return "HEARTBEAT";
            }

            case START_ROUTE -> {
                return "START_ROUTE";
            }

            case STOP_ROUTE -> {
                return "STOP_ROUTE";
            }

            case SET_COURSE -> {
                return "SET_COURSE (" + new UnsignedShort(data[0], data[1]).getAsInt() + ")";
            }

            default -> {
                return "UNKNOWN";
            }
        }
    }
}
