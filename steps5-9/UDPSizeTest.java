import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * Test of the size limits of UDP datagrams (TP 2, Step 6).
 * <p>
 * Sends datagrams of increasing size to a UDP server and reports, for each
 * size, whether {@code send()} succeeded. The last size is one byte above the
 * maximum UDP payload over IPv4 (65 535 - 8 - 20 = 65 507 bytes), so its
 * sending is expected to fail.
 * <p>
 * Usage: {@code java UDPSizeTest [host] [port]} (default: localhost 8080)
 */
public class UDPSizeTest {

    /** Host used when none is given. */
    public static final String DEFAULT_HOST = "localhost";

    /** Port used when none is given. */
    public static final int DEFAULT_PORT = 8080;

    /** Sizes of the datagrams sent, in bytes. */
    private static final int[] SIZES = {1000, 10000, 65507, 65508};

    /** Pause between two datagrams, so that the server output stays readable. */
    private static final int PAUSE_MS = 500;

    /** Not instantiable: all the work is done in {@link #main(String[])}. */
    private UDPSizeTest() {
    }

    /**
     * Sends one datagram of each size in {@link #SIZES} and reports the result.
     *
     * @param args optional host, then optional port of the server
     * @throws Exception if the host is unknown, the socket cannot be created
     *                   or the pause is interrupted
     */
    public static void main(String[] args) throws Exception {
        String host = args.length >= 1 ? args[0] : DEFAULT_HOST;
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        InetAddress address = InetAddress.getByName(host);

        System.out.println("Sending to " + address.getHostAddress() + ":" + port);

        try (DatagramSocket socket = new DatagramSocket()) {
            for (int size : SIZES) {
                // Payload of 'size' bytes, all the ASCII character 'x' (1 byte each)
                byte[] data = new byte[size];
                Arrays.fill(data, (byte) 'x');

                DatagramPacket packet = new DatagramPacket(data, data.length, address, port);

                try {
                    socket.send(packet);
                    System.out.println(size + " bytes: sent");
                } catch (IOException e) {
                    // Caught here so that one failure does not stop the other tests
                    System.out.println(size + " bytes: FAILED -> " + e.getMessage());
                }

                Thread.sleep(PAUSE_MS);
            }
        }
    }
}