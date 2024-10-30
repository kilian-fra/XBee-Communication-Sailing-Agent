package comm;

import org.junit.jupiter.api.Test;

import static comm.protocol.CliftonCommand.heartbeat;
import static comm.protocol.CliftonCommand.startRoute;
import static comm.protocol.CliftonCommand.stopRoute;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import static comm.protocol.CliftonCommand.setCourse;

/**
 * This class is used to test the CliftonCommand class.
 */
public class CliftonCommandTest {

    /**
     * The raw bytes for the heartbeat command.
     */
    private static final byte[] HEARTBEAT_RAW
        = new byte[] { 0x00, 0x00, 0x00 };

    /**
     * The raw bytes for the start route command.
     */
    private static final byte[] START_ROUTE_RAW
        = new byte[] { 0x01, 0x00, 0x00 };
    /**
     * The raw bytes for the stop route command.
     */
    private static final byte[] STOP_ROUTE_RAW
        = new byte[] { 0x02, 0x00, 0x00 };
    /**
     * The course to set.
     */
    private static final int COURSE = 359;
    /**
     * The raw bytes for the set course command.
     */
    private static final byte[] SET_COURSE_RAW
        = new byte[] { 0x03, 0x67, 0x01 };

    /**
     * This method tests the creation of the command.
    */
    @Test
    public void testCommandCreation() {
        assertTrue(Arrays.equals(heartbeat().toByteArray(), HEARTBEAT_RAW));
        assertTrue(Arrays.equals(startRoute().toByteArray(), START_ROUTE_RAW));
        assertTrue(Arrays.equals(stopRoute().toByteArray(), STOP_ROUTE_RAW));
        assertTrue(Arrays.equals(setCourse(COURSE).toByteArray(), SET_COURSE_RAW));
    }
}
