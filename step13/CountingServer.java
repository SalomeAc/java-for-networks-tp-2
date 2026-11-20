import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Server that counts the datagrams of a burst to measure packet loss
 * (TP 2, Step 13).
 * <p>
 * Receives the datagrams sent by {@code BurstClient}, of the form
 * {@code "seq/N"}, and counts them. A burst is considered finished after
 * {@link #END_OF_BURST_MS} milliseconds without any datagram; the server then
 * prints the number received, the loss rate and the number of datagrams that
 * arrived out of order, and waits for the next burst.
 * <p>
 * An optional delay after each datagram simulates a slow server (case c).
 * <p>
 * Usage: {@code java CountingServer [port] [delayMs]} (default: 8080, 0)
 */
public class CountingServer {

    /** Port used when none is given. */
    public static final int DEFAULT_PORT = 8080;

    /** Silence after which a burst is considered finished, in milliseconds. */
    private static final int END_OF_BURST_MS = 2000;

    /** Not instantiable: all the work is done in {@link #main(String[])}. */
    private CountingServer() {
    }

    /**
     * Receives bursts forever and prints the loss rate of each one.
     *
     * @param args optional port, then optional delay after each datagram in milliseconds
     * @throws Exception if the socket cannot be opened or the server is interrupted
     */
    public static void main(String[] args) throws Exception {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        int delayMs = args.length >= 2 ? Integer.parseInt(args[1]) : 0;

        try (DatagramSocket socket = new DatagramSocket(port)) {
            socket.setSoTimeout(END_OF_BURST_MS);
            System.out.println("CountingServer on port " + port + ", delay " + delayMs
                               + " ms, OS receive buffer = "
                               + socket.getReceiveBufferSize() + " bytes");

            byte[] buffer = new byte[100];
            int received = 0;     // datagrams received in the current burst
            int total = 0;        // N, read from the datagrams
            int outOfOrder = 0;   // datagrams arriving after a higher number
            int highest = -1;     // highest sequence number seen

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                } catch (SocketTimeoutException e) {
                    if (received > 0) {
                        // Silence: the burst is over, print the result
                        int lost = total - received;
                        System.out.printf("Received %d / %d datagrams -> lost %d (%.2f %%), "
                                          + "out of order: %d%n",
                                          received, total, lost, 100.0 * lost / total, outOfOrder);
                        received = 0; total = 0; outOfOrder = 0; highest = -1;
                    }
                    continue;
                }

                // Payload "seq/N"
                String msg = new String(packet.getData(), 0, packet.getLength(),
                                        StandardCharsets.UTF_8).trim();
                String[] parts = msg.split("/");
                int seq = Integer.parseInt(parts[0]);
                total = Integer.parseInt(parts[1]);

                if (received == 0) System.out.println("Burst started...");
                received++;
                if (seq < highest) outOfOrder++;
                else highest = seq;

                if (delayMs > 0) Thread.sleep(delayMs);   // case c: slow server
            }
        }
    }
}