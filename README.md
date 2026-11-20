# Java for networks – TP 2: UDP client-server with Java sockets

ENSEA, 3RTS – Year 2026-27 – Instructor: Linda Senigagliesi
Salomé Acosta Montaño, Manuela Carmen Neyla Etah Ebouga

A UDP client and server written with Java datagram sockets, used to explore
what distinguishes UDP from TCP: the size limits of datagrams, the absence of
connections, the lack of reliability (partially fixed with sequence numbers and
acknowledgments), and one-to-many communication with multicast.

The answers to the questions of the lab, with our observations and screenshots,
are in [`Answer-for -the-questions.md`](Answer-for%20-the-questions.md).

## Requirements

- Java 17 or later (`javac` and `java` in the PATH)
- Optional for the tests: `ncat` (from Nmap) and Wireshark

## Project structure

Each folder contains the version of the code for one or more steps of the lab.
The folders are independent: each one is compiled and run on its own.

| Folder | Steps | Files | Content |
|---|---|---|---|
| `step1` | 1–2 | `UDPServer` | Server displaying the received strings (1024-byte buffer) |
| `step3` | 3–4 | `UDPClient`, `UDPServer` | Client sending each typed line in a datagram |
| `steps5-9` | 5–9 | `UDPServer`, `UDPClient`, `UDPSizeTest` | Configurable receive buffer, test of the datagram size limits |
| `step11` | 11 | `UDPServer`, `UDPClient` | Sequence numbers; detection of missing and out-of-order messages |
| `step12` | 12 | `UDPServer`, `UDPClient` | Acknowledgments, timeout and retransmission |
| `step13` | 13 | `BurstClient`, `CountingServer` | Measurement of the loss rate with bursts of datagrams |
| `step14` | 14 | `UDPServer`, `UDPClient` | Simulated losses and detection of duplicates |
| `step16` | 16–18 | `MulticastReceiver`, `UDPClient` | Multicast group (join, leave) and broadcast |

Steps 8, 10, 15 and 19 contain only questions, answered in the answers file.

## How to compile and run

Go into the folder of the step, compile, then run the programs in separate
terminals. For example, for Step 12:

```
cd step12
javac *.java
java UDPServer 8080
java UDPClient localhost 8080
```

The client reads the lines typed on the keyboard and stops when the standard
input is closed (Ctrl+D on Linux/macOS, Ctrl+Z then Enter on Windows).
Lines can also be sent from a pipe, e.g. in PowerShell:
`1..10 | ForEach-Object { "msg$_" } | java UDPClient localhost 8080`

## Command-line arguments

Arguments in brackets are optional; the default value is used when they are omitted.

| Program | Arguments | Default |
|---|---|---|
| `UDPServer` (step 1) | `[port]` | 8080 |
| `UDPServer` (steps 5–12) | `[port] [bufferSize]` | 8080, 1024 bytes |
| `UDPServer` (step 14) | `[port] [bufferSize] [dropRate]` | 8080, 1024, 0 (no loss) |
| `UDPClient` | `<host> <port>` | required |
| `UDPSizeTest` | `[host] [port]` | localhost, 8080 |
| `BurstClient` | `<host> <port> [N]` | N = 10000 |
| `CountingServer` | `[port] [delayMs]` | 8080, 0 ms |
| `MulticastReceiver` | `[group] [port] [interface]` | 239.0.0.1, 8888, interface used for the Internet |

Examples:

```
java UDPServer 8080 64            # Step 5: 64-byte receive buffer
java UDPServer 8080 1024 0.2      # Step 14: 20 % of datagrams and ACKs dropped
java CountingServer 8080 2        # Step 13 c: 2 ms pause after each datagram
java MulticastReceiver 239.0.0.1 8888 192.168.1.10
java UDPClient 239.0.0.1 8888     # Step 17: send to the multicast group
```

`MulticastReceiver` accepts the commands `leave`, `join` and `quit` while it runs.
Its interface can be given by name (e.g. `wlan1`) or by one of its IP addresses.

For more reference, in the report there are screenshots of how we ran every step
from the terminal.