import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * UDP client sending broadcast datagrams (Step 18).
 * <p>
 * Same as the simple {@code UDPClient}, but broadcast is enabled on its socket,
 * so that it can send to a broadcast address: either the limited broadcast
 * address {@code 255.255.255.255}, or the broadcast address of the local
 * network (e.g. {@code 192.168.1.255}). Each line typed is sent in a datagram
 * that every host of the local network receives, if a program listens on the port.
 * <p>
 * Usage: {@code java BroadcastClient <broadcastAddress> <port>}
 */
public class BroadcastClient {

    /** Broadcast address the datagrams are sent to. */
    private final InetAddress broadcastAddress;

    /** Port the datagrams are sent to. */
    private final int port;

    /**
     * Creates a client that sends to the given broadcast address.
     *
     * @param address the broadcast address (e.g. "255.255.255.255")
     * @param port    the destination UDP port
     * @throws UnknownHostException if the address is not valid
     */
    public BroadcastClient(String address, int port) throws UnknownHostException {
        this.broadcastAddress = InetAddress.getByName(address);
        this.port = port;
    }

    /**
     * Reads lines from the standard input and broadcasts each one in a datagram,
     * until the standard input is closed.
     *
     * @throws IOException if the socket cannot be created or a datagram cannot be sent
     */
    public void launch() throws IOException {
        try (DatagramSocket socket = new DatagramSocket();
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            socket.setBroadcast(true);   // allow sending to a broadcast address

            System.out.println("Broadcasting to " + broadcastAddress.getHostAddress() + ":" + port
                               + " (Ctrl+D on Linux/macOS, Ctrl+Z then Enter on Windows to quit)");

            String line;
            while ((line = in.readLine()) != null) {
                byte[] data = line.getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(data, data.length, broadcastAddress, port));
            }
        }
        System.out.println("Client stopped.");
    }

    /**
     * Starts a client towards the broadcast address and port given on the command line.
     *
     * @param args the broadcast address and the port
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java BroadcastClient <broadcastAddress> <port>");
            return;
        }

        try {
            new BroadcastClient(args[0], Integer.parseInt(args[1])).launch();
        } catch (UnknownHostException e) {
            System.err.println("Invalid address: " + args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port: " + args[1]);
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
    }
}