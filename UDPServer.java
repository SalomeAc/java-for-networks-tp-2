import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class UDPServer {

    public static final int DEFAULT_PORT = 8080;
    public static final int DEFAULT_BUFFER_SIZE = 1024; // max accepted message size in bytes

    private final int port;
    private final int bufferSize;
    private boolean running = false;

    /** Constructor with an explicit port and receive buffer size. */
    public UDPServer(int port, int bufferSize) {
        this.port = port;
        this.bufferSize = bufferSize;
    }

    /** Constructor with an explicit port and the default buffer size. */
    public UDPServer(int port) {
        this(port, DEFAULT_BUFFER_SIZE);
    }

    /** Default constructor: default port and default buffer size. */
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
                // New packet each time, so its length is reset to the full buffer
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                try {
                    socket.receive(packet); // blocks until a datagram arrives
                } catch (IOException e) {
                    System.err.println("Receive error: " + e.getMessage());
                    continue;
                }

                // Only decode the bytes actually received (at most bufferSize)
                String msg = new String(packet.getData(), 0, packet.getLength(),
                                        StandardCharsets.UTF_8);

                // The byte count makes the truncation visible in Step 5
                System.out.println(packet.getAddress().getHostAddress() + ":"
                                   + packet.getPort() + " (" + packet.getLength()
                                   + " bytes) > " + msg);
            }
        } finally {
            running = false;
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
            // e.g. port already in use
            System.err.println("Cannot open socket on port: " + e.getMessage());
        }
    }
}