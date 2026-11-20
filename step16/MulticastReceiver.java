import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Receiver of a UDP multicast group (Steps 16 and 17).
 * <p>
 * Joins a multicast group on a network interface and displays every message
 * sent to the group, prefixed with the address and port of the sender.
 * While it runs, the user can type {@code leave} to leave the group,
 * {@code join} to join it again, and {@code quit} to stop the receiver.
 * <p>
 * Usage: {@code java MulticastReceiver [group] [port] [interface]}
 * (default: 239.0.0.1 8888, interface used for the Internet).
 * The interface can be given by its name (e.g. {@code wlan1}) or by one of
 * its IP addresses (e.g. {@code 192.168.35.63}).
 */
public class MulticastReceiver {

    /** Multicast group used when none is given (range reserved for local use). */
    public static final String DEFAULT_GROUP = "239.0.0.1";

    /** Port used when none is given. */
    public static final int DEFAULT_PORT = 8888;

    /** Size of the receive buffer, in bytes. */
    private static final int BUFFER_SIZE = 1024;

    /** Address and port of the multicast group. */
    private final InetSocketAddress group;

    /** Network interface on which the group is joined. */
    private final NetworkInterface netIf;

    /** Socket receiving the datagrams of the group (null until launched). */
    private MulticastSocket socket;

    /** True while the receiver is a member of the group. */
    private boolean joined = false;

    /**
     * Creates a receiver for the given multicast group.
     *
     * @param groupAddress the multicast address of the group (224.0.0.0 to 239.255.255.255)
     * @param port         the UDP port of the group
     * @param netIf        the network interface on which to join the group
     * @throws IllegalArgumentException if the address is not a multicast address
     */
    public MulticastReceiver(InetAddress groupAddress, int port, NetworkInterface netIf) {
        if (!groupAddress.isMulticastAddress()) {
            throw new IllegalArgumentException(groupAddress.getHostAddress()
                                               + " is not a multicast address");
        }
        this.group = new InetSocketAddress(groupAddress, port);
        this.netIf = netIf;
    }

    /**
     * Joins the group and displays the received messages until {@code quit} is typed.
     *
     * @throws IOException if the socket cannot be opened or the group cannot be joined
     */
    public void launch() throws IOException {
        try (MulticastSocket s = new MulticastSocket(group.getPort())) {
            socket = s;
            join();
            System.out.println("Started: " + this);
            System.out.println("Type 'leave', 'join' or 'quit'.");
            startCommandThread();

            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    s.receive(packet);
                } catch (SocketException e) {
                    break;   // the socket was closed by 'quit'
                }
                String msg = new String(packet.getData(), 0, packet.getLength(),
                                        StandardCharsets.UTF_8);
                System.out.println(packet.getAddress().getHostAddress() + ":"
                                   + packet.getPort() + " > " + msg);
            }
        }
        System.out.println("Receiver stopped.");
    }

    /**
     * Joins the multicast group, if not already a member.
     *
     * @throws IOException if the group cannot be joined
     */
    public synchronized void join() throws IOException {
        if (!joined) {
            socket.joinGroup(group, netIf);
            joined = true;
            System.out.println("Joined group " + group.getAddress().getHostAddress());
        }
    }

    /**
     * Leaves the multicast group, if currently a member.
     *
     * @throws IOException if the group cannot be left
     */
    public synchronized void leave() throws IOException {
        if (joined) {
            socket.leaveGroup(group, netIf);
            joined = false;
            System.out.println("Left group " + group.getAddress().getHostAddress());
        }
    }

    /**
     * Starts a background thread reading the commands typed by the user,
     * so that the receiver can keep receiving at the same time.
     */
    private void startCommandThread() {
        Thread thread = new Thread(this::readCommands);
        thread.setDaemon(true);   
        thread.start();
    }

    /** Reads and executes the commands {@code leave}, {@code join} and {@code quit}. */
    private void readCommands() {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = in.readLine()) != null) {
                switch (line.trim()) {
                    case "leave" -> leave();
                    case "join" -> join();
                    case "quit" -> {
                        socket.close();   // unblocks receive() in the main loop
                        return;
                    }
                    default -> System.out.println("Unknown command (use leave, join or quit)");
                }
            }
        } catch (IOException e) {
            System.err.println("Command error: " + e.getMessage());
        }
    }

    /**
     * Describes the state of the receiver.
     *
     * @return the group, the interface and whether the receiver is a member
     */
    @Override
    public String toString() {
        return "MulticastReceiver[group=" + group.getAddress().getHostAddress() + ":"
               + group.getPort() + ", interface=" + netIf.getName()
               + " (" + netIf.getDisplayName() + "), joined=" + joined + "]";
    }

    /**
     * Finds a network interface from its name or from one of its IP addresses.
     *
     * @param nameOrAddress the name (e.g. "wlan1") or an IP address of the interface
     * @return the interface, or null if none matches
     */
    static NetworkInterface findInterface(String nameOrAddress) {
        try {
            NetworkInterface byName = NetworkInterface.getByName(nameOrAddress);
            if (byName != null) {
                return byName;
            }
            return NetworkInterface.getByInetAddress(InetAddress.getByName(nameOrAddress));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Finds the interface the system uses to reach the Internet.
     * Connecting a UDP socket sends nothing: it only asks the system which
     * local address, and therefore which interface, it would use.
     *
     * @return that interface, or null if it cannot be determined
     */
    static NetworkInterface defaultInterface() {
        try (DatagramSocket probe = new DatagramSocket()) {
            probe.connect(InetAddress.getByName("8.8.8.8"), 53);
            return NetworkInterface.getByInetAddress(probe.getLocalAddress());
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Prints the active interfaces that support multicast, with their IPv4 addresses.
     *
     * @throws SocketException if the interfaces cannot be listed
     */
    static void listInterfaces() throws SocketException {
        System.out.println("Available interfaces:");
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (ni.isUp() && ni.supportsMulticast()) {
                StringBuilder addresses = new StringBuilder();
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a.getAddress().length == 4) {   // IPv4 only
                        addresses.append(' ').append(a.getHostAddress());
                    }
                }
                System.out.println("  " + ni.getName() + " (" + ni.getDisplayName() + ")"
                                   + addresses);
            }
        }
    }

    /**
     * Starts a receiver with the group, port and interface given on the
     * command line, or with the default values.
     *
     * @param args optional group, then optional port, then optional interface
     * @throws IOException if the receiver cannot be started
     */
    public static void main(String[] args) throws IOException {
        InetAddress groupAddress = InetAddress.getByName(
                args.length >= 1 ? args[0] : DEFAULT_GROUP);
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        NetworkInterface netIf = (args.length >= 3) ? findInterface(args[2]) : defaultInterface();

        if (netIf == null) {
            System.err.println("Network interface not found.");
            listInterfaces();
            return;
        }

        try {
            new MulticastReceiver(groupAddress, port, netIf).launch();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }
}