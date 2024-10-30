package comm;

import java.util.Optional;

/**
 * This interface represents a point-to-point connection
 * between two devices -
 * (the laptop on the java side and the clifton on the other side)
 * This interface is implemented by the XbeeConnection class.
 */
public interface IConnection {
    /**
     * This method creates the connection.
     */
    void create();
    /**
     * This method closes the connection.
     */
    void close();
    /**
     * This method sends data to the peer.
     *
     * @param data - the data to be sent
     * @return boolean - true if the data was sent successfully, false otherwise
     */
    boolean sendData(byte[] data);
    /**
     * This method receives data from the peer.
     *
     * @param length - the length of the data to be received
     * @return Optional<byte[]> - the received data if available
     */
    Optional<byte[]> receiveData(int length);
    /**
     * This method receives data from the peer.
     *
     * @return Optional<byte[]> - the received data if available
     */
    Optional<byte[]> receiveData();
    /**
     * This method checks if the connection is established.
     *
     * @return boolean - true if the connection is established, false otherwise
     */
    boolean isConnected();
}
