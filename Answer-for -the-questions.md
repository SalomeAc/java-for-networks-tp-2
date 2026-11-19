## Step4
What does the server display? The server displays what the two clients typed : he first displays for client 1 and then for client 2.

## Step5
What does the server receive with a 64-byte buffer? The server receives the first 64 bytes of the message; the rest is lost.

Is the client informed? No. No error appears on the client side, and the message is displayed on the terminal without any problem.

Is the rest of the message received in a second datagram? No. UDP does not split messages, each send is a single datagram, and the extra bytes are discarded by the receiving host’s network stack.

## Step6
Which sizes are received? The received size are 1,000 bytes and 65,507 bytes. 

What happens when sending the last one? The last one 65508 sent fails on the client side. Java throws an IOException (“The message is larger than the maximum supported by the underlying transport: sendto”), so nothing reaches the network and the server receives nothing. The sender is informed immediately. 


## Step7
How many IP packets do you see for the second one, and why? There are 7 IP packets because the Ethernet MTU is 1500 bytes, so an IP packet can carry at most 1500 − 20 = 1480 bytes of data. The 10,008-byte datagram is split into 6 full fragments (8,880 bytes) and one last fragment of 1,128 bytes.

## Step8
Why is the size of the receive buffer chosen by the receiver and not by the sender? UDP has no negotiation between the two ends. The sender writes a datagram to the network and never learns how the receiver handles it, so it cannot know how much space the receiving application has allocated. Only the receiver knows its buffer size, so only the receiver can decide how much of an incoming datagram it can keep. 

Why is it a bad idea to send datagrams larger than about 1 400 bytes on a real network, even if UDP accepts up to 65 507 bytes? Most networks use an Ethernet MTU of 1500 bytes. A datagram larger than the path MTU is split by IP into fragments, as the Step 7 capture showed with 7 packets for 10,000 bytes. And fragmentation causes several problems: loss amplification, overhead, filtering problems, latency and jitter.

## Step9
Are they lost? 

Does the client receive an error? 

Then start the server: does it receive the messages sent before?

Which ICMP messages does the destination send back?

Does the server notice when a client stops? 

What changes in the address displayed by the server each time a new client starts?

## Step10
Compare with TCP: what would happen in experiments a) and c) with the HTTP client of TP 1?

Why is UDP nevertheless used for DNS, video streaming or online games?

## Step13

## Step14

## Step15

## Step17

## Step18

## Step19
