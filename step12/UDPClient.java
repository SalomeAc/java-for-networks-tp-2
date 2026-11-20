import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class UDPClient {

    public static final int TIMEOUT_MS = 1000;   // time to wait for an ACK (Step 12)
    public static final int MAX_ATTEMPTS = 5;    // 1 send + up to 4 retransmissions

    private final InetAddress serverAddress;
    private final int serverPort;
    private int nextSeq = 0; // sequence number of the next message (Step 11)

    public UDPClient(String host, int port) throws UnknownHostException {
        this.serverAddress = InetAddress.getByName(host);
        this.serverPort = port;
    }

    /** Reads lines from standard input and sends each one reliably. */
    public void launch() throws IOException {
        try (DatagramSocket socket = new DatagramSocket();
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            System.out.println("Sending to " + serverAddress.getHostAddress() + ":" + serverPort
                               + " (Ctrl+D on Linux/macOS, Ctrl+Z then Enter on Windows to quit)");

            String line;
            while ((line = in.readLine()) != null) {
                sendReliably(socket, nextSeq, line);
                nextSeq++;
            }
        }
        System.out.println("Client stopped.");
    }

    /**
     * Sends "seq:text" and waits for "ACK:seq".
     * Retransmits after each timeout, up to MAX_ATTEMPTS attempts.
     * Returns true if the message was acknowledged.
     */
    private boolean sendReliably(DatagramSocket socket, int seq, String text) throws IOException {
        byte[] data = (seq + ":" + text).getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);

        byte[] ackBuffer = new byte[100];

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            socket.send(packet);
            System.out.println("sent: " + seq + ":" + text + "   (attempt " + attempt + ")");

            // Wait for the right ACK until the deadline of this attempt
            long deadline = System.currentTimeMillis() + TIMEOUT_MS;
            while (true) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) break;            // this attempt has timed out
                socket.setSoTimeout((int) remaining);

                DatagramPacket ack = new DatagramPacket(ackBuffer, ackBuffer.length);
                try {
                    socket.receive(ack);
                } catch (SocketTimeoutException e) {
                    break;                            // this attempt has timed out
                }

                String reply = new String(ack.getData(), 0, ack.getLength(),
                                          StandardCharsets.UTF_8);
                if (reply.equals("ACK:" + seq)) {
                    System.out.println("  received " + reply);
                    return true;
                }
                // An old ACK (e.g. for a previous retransmission): ignore it, keep waiting
                System.out.println("  ignored " + reply);
            }

            System.out.println("  timeout: no ACK:" + seq + " after " + TIMEOUT_MS + " ms");
        }

        System.out.println("  ERROR: message #" + seq + " not acknowledged after "
                           + MAX_ATTEMPTS + " attempts");
        return false;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java UDPClient <host> <port>");
            return;
        }

        try {
            UDPClient client = new UDPClient(args[0], Integer.parseInt(args[1]));
            client.launch();
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port: " + args[1]);
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
    }
}