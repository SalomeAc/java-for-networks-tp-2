import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * UDP server that checks sequence numbers (Step 11).
 * <p>
 * Receives messages of the form {@code "n:text"}, displays the sequence number
 * and the text, and reports, for each client, the messages that are missing,
 * that arrive out of order, or that are received twice.
 * <p>
 * Usage: {@code java UDPServer [port] [bufferSize]}
 */
public class UDPServer {

    /** Port used when none is given. */
    public static final int DEFAULT_PORT = 8080;

    /** Receive buffer size used when none is given, in bytes. */
    public static final int DEFAULT_BUFFER_SIZE = 1024;

    /** UDP port the server listens on. */
    private final int port;

    /** Size of the receive buffer; longer messages are truncated. */
    private final int bufferSize;

    /** True while the server is receiving datagrams. */
    private boolean running = false;

    /** What the server remembers about one client. */
    private static class ClientState {

        /** Next sequence number expected from this client. */
        private int expected = 0;

        /** Sequence numbers that were skipped and have not arrived yet. */
        private final Set<Integer> missing = new HashSet<>();
    }

    /** State of each client, identified by "address:port". */
    private final Map<String, ClientState> clients = new HashMap<>();

    /**
     * Creates a server with the given port and receive buffer size.
     *
     * @param port       the UDP port to listen on
     * @param bufferSize the size of the receive buffer, in bytes
     */
    public UDPServer(int port, int bufferSize) {
        this.port = port;
        this.bufferSize = bufferSize;
    }

    /**
     * Creates a server with the given port and {@link #DEFAULT_BUFFER_SIZE}.
     *
     * @param port the UDP port to listen on
     */
    public UDPServer(int port) {
        this(port, DEFAULT_BUFFER_SIZE);
    }

    /** Creates a server with {@link #DEFAULT_PORT} and {@link #DEFAULT_BUFFER_SIZE}. */
    public UDPServer() {
        this(DEFAULT_PORT, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Starts the server: receives datagrams in an infinite loop and checks
     * their sequence numbers. This method never returns normally.
     *
     * @throws SocketException if the socket cannot be opened (e.g. port already in use)
     */
    public void launch() throws SocketException {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            running = true;
            System.out.println("Started: " + this);

            byte[] buffer = new byte[bufferSize];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    System.err.println("Receive error: " + e.getMessage());
                    continue;
                }

                String msg = new String(packet.getData(), 0, packet.getLength(),
                                        StandardCharsets.UTF_8);
                msg = msg.stripTrailing(); 

                String sender = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                String prefix = sender + " (" + packet.getLength() + " bytes) ";

                handleMessage(sender, prefix, msg);
            }
        } finally {
            running = false;
        }
    }

    /**
     * Extracts the sequence number of a message, displays it, and reports
     * missing, out-of-order or duplicate messages.
     *
     * @param sender "address:port" of the client, used to find its state
     * @param prefix text displayed before the message (sender and size)
     * @param msg    the received message, of the form "n:text"
     */
    private void handleMessage(String sender, String prefix, String msg) {
        int sep = msg.indexOf(':');
        int seq;
        try {
            if (sep < 0) throw new NumberFormatException();
            seq = Integer.parseInt(msg.substring(0, sep));
        } catch (NumberFormatException e) {
            System.out.println(prefix + "[no sequence number] > " + msg);
            return;
        }
        String text = msg.substring(sep + 1);

        ClientState state = clients.computeIfAbsent(sender, k -> new ClientState());

        if (seq == state.expected) {
            // Normal case: the message
            System.out.println(prefix + "#" + seq + " > " + text);
            state.expected++;

        } else if (seq > state.expected) {
            // Gap: the numbers between expected and seq have not arrived
            for (int i = state.expected; i < seq; i++) {
                state.missing.add(i);
            }
            if (seq - state.expected == 1) {
                System.out.println("  !! MISSING #" + state.expected);
            } else {
                System.out.println("  !! MISSING #" + state.expected + " to #" + (seq - 1));
            }
            System.out.println(prefix + "#" + seq + " > " + text);
            state.expected = seq + 1;

        } else {
            // seq < expected: either a late message or one we already have
            if (state.missing.remove(seq)) {
                System.out.println(prefix + "#" + seq + " > " + text
                                   + "   !! OUT OF ORDER (arrived late)");
            } else {
                System.out.println(prefix + "#" + seq + " > " + text
                                   + "   !! DUPLICATE (already received)");
            }
        }
    }

    /**
     * Describes the state of the server.
     *
     * @return the port, the buffer size and whether the server is running
     */
    @Override
    public String toString() {
        return "UDPServer[port=" + port + ", bufferSize=" + bufferSize
               + ", running=" + running + "]";
    }

    /**
     * Starts a server with the port and buffer size given on the command line,
     * or with the default values.
     *
     * @param args optional port, then optional buffer size
     */
    public static void main(String[] args) {
        UDPServer server;
        try {
            if (args.length >= 2) {
                server = new UDPServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            } else if (args.length == 1) {
                server = new UDPServer(Integer.parseInt(args[0]));
            } else {
                server = new UDPServer();
            }
        } catch (NumberFormatException e) {
            System.err.println("Usage: java UDPServer [port] [bufferSize]");
            return;
        }

        try {
            server.launch();
        } catch (SocketException e) {
            System.err.println("Cannot open socket on port: " + e.getMessage());
        }
    }
}