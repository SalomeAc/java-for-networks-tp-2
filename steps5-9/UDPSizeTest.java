import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * Step 6: sends datagrams of increasing size to a UDP server
 * and reports, for each size, whether send() succeeded.
 *
 * Usage: java UDPSizeTest [host] [port]   (default: localhost 8080)
 */
public class UDPSizeTest {

    private static final int[] SIZES = {1000, 10000, 65507, 65508};

    public static void main(String[] args) throws Exception {
        String host = args.length >= 1 ? args[0] : "localhost";
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : 8080;
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
                    // Catch here so that one failure does not stop the other tests
                    System.out.println(size + " bytes: FAILED -> " + e.getMessage());
                }

                Thread.sleep(500); // small pause so the server output stays readable
            }
        }
    }
}