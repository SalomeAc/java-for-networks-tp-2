import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * UDP client with acknowledgments and retransmission (Steps 12 and 14).
 * <p>
 * Reads lines of text from the standard input and sends each one, numbered
 * (e.g. {@code "12:hello"}), to the server. After each send, it waits for the
 * acknowledgment {@code "ACK:12"}; if none arrives within {@link #TIMEOUT_MS},
 * it sends the message again, up to {@link #MAX_ATTEMPTS} times, then reports
 * an error. The client stops when the standard input is closed.
 * <p>
 * Usage: {@code java UDPClient <host> <port>}
 */
public class UDPClient {

    /** Time to wait for an acknowledgment before retransmitting, in milliseconds. */
    public static final int TIMEOUT_MS = 1000;

    /** Maximum number of sends of one message (1 send + up to 4 retransmissions). */
    public static final int MAX_ATTEMPTS = 5;

    /** Address of the server the datagrams are sent to. */
    private final InetAddress serverAddress;

    /** Port of the server the datagrams are sent to. */
    private final int serverPort;

    /** Sequence number of the next message to send. */
    private int nextSeq = 0;

    /**
     * Creates a client that sends to the given server.
     *
     * @param host name or IP address of the server (e.g. "localhost")
     * @param port UDP port of the server
     * @throws UnknownHostException if the host name cannot be resolved
     */
    public UDPClient(String host, int port) throws UnknownHostException {
        this.serverAddress = InetAddress.getByName(host);
        this.serverPort = port;
    }

    /**
     * Reads lines from the standard input and sends each one reliably,
     * until the standard input is closed.
     *
     * @throws IOException if the socket cannot be created or a datagram cannot be sent
     */
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
     * Sends {@code "seq:text"} and waits for {@code "ACK:seq"}, retransmitting
     * after each timeout, up to {@link #MAX_ATTEMPTS} attempts.
     *
     * @param socket the socket used to send the message and receive the ACK
     * @param seq    the sequence number of the message
     * @param text   the text of the message
     * @return true if the message was acknowledged, false after too many attempts
     * @throws IOException if a datagram cannot be sent or received
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

    /**
     * Starts a client towards the server given on the command line.
     *
     * @param args the host and the port of the server
     */
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