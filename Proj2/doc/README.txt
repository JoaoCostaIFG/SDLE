# SDLE Project #2

SDLE Project for group **T3G11** (FEUP, MEIC 2021/22).

## Project description

**Molater**, a twitter-like peer-to-peer social network.

## Group members

1. Davide Castro - `up201806512@edu.fe.up.pt`
2. Henrique Ribeiro - `up201806529@edu.fe.up.pt`
3. João Costa - `up201806560@edu.fe.up.pt`
4. Tiago Silva - `up201806516@edu.fe.up.pt`

## How to compile

This project uses [maven](https://maven.apache.org/what-is-maven.html) as a
build system. As such, the project is compiled by calling `mvn compile`. Be
aware that this will pull in the required dependencies.

## Usage

There are 2 executable files in this projects: the key-server instance, and the
peer program.

### Key-server

Key-servers are special peers with well-known addresses. They are responsible
for keeping the users public-keys (for authentication purposes), and to serve as
entry points to the network (bootstrapping). Peers come with these well-known
addresses in their host-cache by default. There can be multiple key-servers, but
for demonstration purposes, only 1 is needed.

Other than that, these servers act as normal peers that forward all queries
(they never save content, so they can serve query-hits).

To start a key-server instance, run:
`mvn exec:java -Dexec.mainClass=org.t3.g11.proj2.keyserver.KeyServer`.

### Peer

The peer program is the way a user can participate in the network. There are 2
UIs available: a cli, and a swing-based GUI. The cli is used by default. If the
user wishes to use the GUI, pass the `--gui` command-line argument to the
program.

To run the peer program, do:
`mvn exec:java -Dexec.mainClass=org.t3.g11.proj2.peer.Main <address> <port> [--gui]`,
where `<address>` and `<port>` are the addresses and port where the service will
be available for outsiders (listening socket).
