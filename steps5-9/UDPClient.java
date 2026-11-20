import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class UDPClient {

    private final InetAddress serverAddress;
    private final int serverPort;

    public UDPClient(String host, int port) throws UnknownHostException {
        
        this.serverAddress = InetAddress.getByName(host);
        this.serverPort = port;
    }

    /** Reads lines from standard input and sends each one in a datagram. */
    public void launch() throws IOException {
        // No port given: the OS picks a free local port for the client.
        // One socket for the whole session, so the source port stays the same.
        try (DatagramSocket socket = new DatagramSocket();
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            System.out.println("Sending to " + serverAddress.getHostAddress() + ":" + serverPort
                               + " (Ctrl+D on Linux/macOS, Ctrl+Z then Enter on Windows to quit)");

            String line;
            // readLine() returns null when standard input is closed
            while ((line = in.readLine()) != null) {
                byte[] data = line.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet =
                        new DatagramPacket(data, data.length, serverAddress, serverPort);
                socket.send(packet);
            }
        }
        System.out.println("Client stopped.");
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