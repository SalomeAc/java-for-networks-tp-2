import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * UDP server with a configurable receive buffer (Steps 5 to 9).
 * <p>
 * Receives datagrams containing UTF-8 strings and prints each one on the
 * standard output, prefixed with the address and port of the sender and the
 * number of bytes received. Messages longer than the receive buffer are
 * truncated, which makes the size limits of datagrams visible.
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
     * Starts the server: receives datagrams in an infinite loop and prints them
     * with their size. This method never returns normally.
     *
     * @throws SocketException if the socket cannot be opened (e.g. port already in use)
     */
    public void launch() throws SocketException {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            running = true;
            System.out.println("Started: " + this);

            byte[] buffer = new byte[bufferSize];

            while (true) {
                // New packet each time: receive() shrinks the packet's length
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    System.err.println("Receive error: " + e.getMessage());
                    continue;
                }

                // Decode only the bytes actually received (at most bufferSize)
                String msg = new String(packet.getData(), 0, packet.getLength(),
                                        StandardCharsets.UTF_8);

                // The byte count makes the truncation visible
                System.out.println(packet.getAddress().getHostAddress() + ":"
                                   + packet.getPort() + " (" + packet.getLength()
                                   + " bytes) > " + msg);
            }
        } finally {
            running = false;
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