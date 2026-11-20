import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * UDP client with sequence numbers (Step 11).
 * <p>
 * Reads lines of text from the standard input and sends each one in a
 * datagram, prefixed with a sequence number starting at 0
 * (e.g. {@code "12:hello"}), so that the server can detect missing
 * or out-of-order messages. The client stops when the standard input is closed.
 * <p>
 * Usage: {@code java UDPClient <host> <port>}
 */
public class UDPClient {

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
     * Reads lines from the standard input and sends each one, numbered,
     * in a datagram, until the standard input is closed.
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
                // Prefix the message with its sequence number: "12:hello"
                String numbered = nextSeq + ":" + line;
                byte[] data = numbered.getBytes(StandardCharsets.UTF_8);

                DatagramPacket packet =
                        new DatagramPacket(data, data.length, serverAddress, serverPort);
                socket.send(packet);
                System.out.println(numbered);
                nextSeq++;
            }
        }
        System.out.println("Client stopped.");
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