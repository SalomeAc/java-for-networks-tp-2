import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

// Before modifications of step 5

public class UDPServer {

    public static final int DEFAULT_PORT = 8080;
    public static final int MAX_SIZE = 1024; // max accepted message size in bytes

    private final int port;
    private boolean running = false;

    /** Constructor with an explicit listening port. */
    public UDPServer(int port) {
        this.port = port;
    }

    /** Default constructor: listens on DEFAULT_PORT. */
    public UDPServer() {
        this(DEFAULT_PORT);
    }

    /** Starts the server: receives datagrams forever and prints them. */
    public void launch() throws SocketException {
        // try-with-resources closes the socket if an exception ends the loop
        try (DatagramSocket socket = new DatagramSocket(port)) {
            running = true;
            System.out.println("Started: " + this);

            byte[] buffer = new byte[MAX_SIZE];

            while (true) {
                // Create the packet inside the loop (or reset its length):
                // after a receive(), getLength() shrinks to the size of the
                // last datagram, which would truncate the next ones.
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                try {
                    socket.receive(packet); // blocks until a datagram arrives
                } catch (IOException e) {
                    System.err.println("Receive error: " + e.getMessage());
                    continue;
                }

                // Only decode the bytes actually received
                String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                System.out.println(packet.getAddress().getHostAddress() + ":" + packet.getPort() + " > " + msg);
            }
        } finally {
            running = false;
        }
    }

    @Override
    public String toString() {
        return "UDPServer[port=" + port + ", running=" + running + "]";
    }

    public static void main(String[] args) {
        UDPServer server;
        if (args.length >= 1) {
            server = new UDPServer(Integer.parseInt(args[0]));
        } else {
            server = new UDPServer();
        }

        try {
            server.launch();
        } catch (SocketException e) {
            // port already in use, or port < 1024 without root rights
            System.err.println("Cannot open socket on port: " + e.getMessage());
        }
    }
}