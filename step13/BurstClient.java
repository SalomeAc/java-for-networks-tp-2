import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Step 13: sends N numbered datagrams as fast as possible, without waiting for ACKs.
 * Each datagram contains "seq/N", so the server knows how many were sent in total.
 *
 * Usage: java BurstClient <host> <port> [N]   (default N = 10000)
 */
public class BurstClient {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java BurstClient <host> <port> [N]");
            return;
        }
        InetAddress address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);
        int n = args.length >= 3 ? Integer.parseInt(args[2]) : 10000;

        try (DatagramSocket socket = new DatagramSocket()) {
            long start = System.nanoTime();

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