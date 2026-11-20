import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Burst client used to measure packet loss (Step 13).
 * <p>
 * Sends N numbered datagrams as fast as possible, without waiting for any
 * acknowledgment. Each datagram contains {@code "seq/N"}, so that the
 * {@code CountingServer} knows how many datagrams were sent in total,
 * even if some of them are lost.
 * <p>
 * Usage: {@code java BurstClient <host> <port> [N]} (default N = 10000)
 */
public class BurstClient {

    /** Number of datagrams sent when N is not given. */
    public static final int DEFAULT_COUNT = 10000;

    /** Not instantiable: all the work is done in {@link #main(String[])}. */
    private BurstClient() {
    }

    /**
     * Sends the burst of datagrams and displays how long it took.
     *
     * @param args the host and the port of the server, then optionally N
     * @throws Exception if the host is unknown or a datagram cannot be sent
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java BurstClient <host> <port> [N]");
            return;
        }
        InetAddress address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);
        int n = args.length >= 3 ? Integer.parseInt(args[2]) : DEFAULT_COUNT;

        try (DatagramSocket socket = new DatagramSocket()) {
            long start = System.nanoTime();

            // No pause and no ACK: send as fast as possible
            for (int seq = 0; seq < n; seq++) {
                byte[] data = (seq + "/" + n).getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(data, data.length, address, port));
            }

            double ms = (System.nanoTime() - start) / 1_000_000.0;
            System.out.printf("Sent %d datagrams to %s:%d in %.1f ms%n",
                              n, address.getHostAddress(), port, ms);
        }
    }
}