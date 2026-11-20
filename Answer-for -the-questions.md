## Step4
What does the server display? 

The server displays what the two clients typed : he first displays for client 1 and then for client 2.

## Step5
What does the server receive with a 64-byte buffer?

The server receives the first 64 bytes of the message; the rest is lost.

Is the client informed? 

No. No error appears on the client side, and the message is displayed on the terminal without any problem.

Is the rest of the message received in a second datagram? 

No. UDP does not split messages, each send is a single datagram, and the extra bytes are discarded by the receiving host’s network stack.

## Step6
Which sizes are received? 

The received size are 1,000 bytes and 65,507 bytes. 

What happens when sending the last one? 

The last one 65508 sent fails on the client side. Java throws an IOException (“The message is larger than the maximum supported by the underlying transport: sendto”), so nothing reaches the network and the server receives nothing. The sender is informed immediately. 


## Step7
How many IP packets do you see for the second one, and why? 

There are 7 IP packets because the Ethernet MTU is 1500 bytes, so an IP packet can carry at most 1500 − 20 = 1480 bytes of data. The 10,008-byte datagram is split into 6 full fragments (8,880 bytes) and one last fragment of 1,128 bytes.

## Step8
Why is the size of the receive buffer chosen by the receiver and not by the sender?

UDP has no negotiation between the two ends. The sender writes a datagram to the network and never learns how the receiver handles it, so it cannot know how much space the receiving application has allocated. Only the receiver knows its buffer size, so only the receiver can decide how much of an incoming datagram it can keep. 

Why is it a bad idea to send datagrams larger than about 1 400 bytes on a real network, even if UDP accepts up to 65 507 bytes? 

Most networks use an Ethernet MTU of 1500 bytes. A datagram larger than the path MTU is split by IP into fragments, as the Step 7 capture showed with 7 packets for 10,000 bytes. And fragmentation causes several problems: loss amplification, overhead, filtering problems, latency and jitter.

## Step9
Are they lost? 

Yes. The client sent “I’m sending a message before starting the server” to port 8080 while nothing was listening. When the server started, it displayed nothing, so that message never arrived.

Does the client receive an error? 

No. The client printed the send message and kept running without any error.

Then start the server: does it receive the messages sent before? 

No. Only the message sent after the server started was received.

Which ICMP messages does the destination send back? 

An ICMP Destination unreachable (Type 3), Port unreachable (Code 3) for each datagram.

Does the server notice when a client stops? 

No. The server printed nothing when each client stopped. UDP has no closing message, so the server only sees datagrams as they arrive.

What changes in the address displayed by the server each time a new client starts? 

The IP address stays 127.0.0.1 because all clients run on the same machine, but the source port changes each time: 57758, 59588 and 58360 for “client run 1”, “client run 2” and “client run 3”.

## Step10
Compare with TCP: what would happen in experiments a) and c) with the HTTP client of TP 1? 

Experiment a (messages sent before the server starts): The TCP client would fail immediately with “connection refused,” because the three-way handshake cannot complete without a listening server. The client would know the request was not delivered. With UDP, the client got no error and the message was silently lost.

Experiment c (starting and stopping the client while the server runs): Each TCP client would open its own connection with a SYN / SYN-ACK / ACK handshake, and the server would accept it. When the client stopped, the connection would close with a FIN or RST, so the server would see each client arrive and leave. With UDP, the server saw only datagrams with different source ports and noticed nothing when the clients stopped.

Why is UDP nevertheless used for DNS, video streaming or online games? 

Because its connectionless, lightweight design avoids TCP’s handshake and retransmission delays when speed or real-time flow matters more than guaranteed delivery.

## Step13
Explain the differences. Where are the datagrams lost when both programs run on the same machine? 

The datagrams are lost in the receiver’s socket receive buffer, because the application reads them too slowly. This is why case c) loses packets while case a) does not, even though both run on the same machine and use the same network path.

## Step14
Check that the client retransmits the lost messages, and that the server detects duplicates (a message received twice because its acknowledgment was lost). How can the server detect them? 

Each message carries a sequence number, so the server can tell whether it has already processed a given number. It keeps track of the numbers it has delivered. When a datagram arrives : if its number is new, the server displays the message, records the number, and sends ACK:n. If its number was already recorded, the server treats it as a duplicate.

## Step15
How should the timeout of Step 12 be chosen? What happens if it is too short or too long? 

The timeout should be a little longer than the typical round-trip time (RTT) between client and server, plus a safety margin for variation in delay. It cannot be fixed in advance for every network, so it is usually adapted to measured RTTs.

Too short: the client retransmits before the acknowledgment has had time to arrive. 
Too long: the client waits too long after a real loss before retrying. 

Which mechanisms of TCP have you re-implemented, and which ones are still missing? 

Re-implemented: Sequence numbers; Acknowledgments; Retransmission on timeout; Duplicate detection; Retry limit.

Still missing: Reliable in-order delivery; Cumulative acknowledgments and sliding window; Adaptive retransmission timeout; Flow control; Congestion control; Connection management; Checksum over the payload and the sequence number.

## Step17
Start several receivers on your own machine, in different terminals: do they all receive each message? (Several programs can listen on the same multicast port, which is not possible with the UDPServer of Section 1: why?) 

Yes, they all receive each message. Two MulticastReceiver instances on the same machine (192.168.35.63, interface wlan3) both display hello and world when the client sends them to 239.0.0.1:8888. This works where UDPServer failed because of how the two socket types handle a shared port.

## Step18
Which hosts receive it? 

The broadcast was sent from 192.168.35.63 to 192.168.35.255:8080. It arrived at the UDPServer running on another machine in the room (tpreseau@d055-pc3), which printed 192.168.35.63:64263 (14 bytes) > Hello everyone.

Can it reach another network? 

No, not in general. Routers do not forward broadcasts between subnets: Limited broadcast (255.255.255.255) is never forwarded by a router. It stays on the local segment. Directed broadcast (192.168.35.255), is only delivered within the target subnet. Most routers block it by default, because forwarding it lets attackers amplify traffic toward a network from outside.

## Step19
To send the same message to 20 receivers, compare multicast with 20 unicast datagrams: how many datagrams does the sender emit in each case?

With multicast, the sender emits one datagram to the group address, whatever the number of receivers. With unicast, it emits 20 datagrams, one per receiver, each with its own IP and UDP headers.

What changes for the network ? 

The sender builds, sends and handles acknowledgments for one packet instead of twenty; On the sender’s uplink, the multicast stream uses the bandwidth of one copy. The unicast version uses twenty times that bandwidth, since each copy crosses the same link; With multicast, routers and switches duplicate the packet only where the paths to members diverge, so copies travel only once over shared links. With unicast, the same data crosses the shared links up to twenty times; The cost of unicast grows linearly with the number of receivers. Multicast keeps the sender’s cost constant.

What are the limitations of broadcast compared with multicast? 

No selection: every host on the subnet receives the broadcast, even those that do not need it. Multicast reaches only the members of the group.

Processing cost: each host’s network stack must handle every broadcast frame, which wastes CPU on uninterested machines.

No routing: routers do not forward broadcasts, so they stay on one subnet. Multicast can be routed across networks with multicast routing protocols and a suitable TTL.

Security: directed broadcasts can be abused for amplification attacks, so networks usually block them.

Not available in IPv6: IPv6 has no broadcast; it uses multicast for the same purposes.

Why are multicast and broadcast possible with UDP but not with TCP? 

TCP is a connection-oriented protocol between exactly two endpoints. Its guarantees depend on that:

Handshake and state: a TCP connection is set up with a three-way handshake between one client and one server. Multicast or broadcast has many receivers, so there is no single peer to handshake with.

Acknowledgments and retransmission: TCP needs each receiver to acknowledge each segment, so the sender can retransmit lost data. With many receivers, every one of them would have to acknowledge every segment, which does not scale and would cause an acknowledgment storm.

Sequence and flow control: TCP maintains per-connection sequence numbers, windows and congestion state. Each receiver would have different state, so one sender cannot keep a single consistent stream for all of them.

