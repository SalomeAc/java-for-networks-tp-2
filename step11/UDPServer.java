package step11;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Step 11: the server now tracks the sequence numbers of messages from each client, and reports missing or out-of-order messages.

public class UDPServer {

    public static final int DEFAULT_PORT = 8080;
    public static final int DEFAULT_BUFFER_SIZE = 1024; // max accepted message size in bytes

    private final int port;
    private final int bufferSize;
    private boolean running = false;

    /** What the server remembers about one client (Step 11). */
    private static class ClientState {
        int expected = 0;                          // next sequence number expected
        Set<Integer> missing = new HashSet<>();    // numbers skipped, not received yet
    }

    // One state per client, identified by "address:port"
    private final Map<String, ClientState> clients = new HashMap<>();

    public UDPServer(int port, int bufferSize) {
        this.port = port;
        this.bufferSize = bufferSize;
    }

    public UDPServer(int port) {
        this(port, DEFAULT_BUFFER_SIZE);
    }

    public UDPServer() {
        this(DEFAULT_PORT, DEFAULT_BUFFER_SIZE);
    }

    /** Starts the server: receives datagrams forever and prints them. */
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
                msg = msg.stripTrailing(); // netcat adds a newline at the end

                String sender = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                String prefix = sender + " (" + packet.getLength() + " bytes) ";

                handleMessage(sender, prefix, msg);
            }
        } finally {
            running = false;
        }
    }

    /** Extracts the sequence number and reports missing or out-of-order messages. */
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
            // Normal case: the message we were waiting for
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

    @Override
    public String toString() {
        return "UDPServer[port=" + port + ", bufferSize=" + bufferSize
               + ", running=" + running + "]";
    }

    /** Usage: java UDPServer [port] [bufferSize] */
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