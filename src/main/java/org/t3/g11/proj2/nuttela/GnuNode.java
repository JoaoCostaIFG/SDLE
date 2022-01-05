package org.t3.g11.proj2.nuttela;

import org.t3.g11.proj2.nuttela.message.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class GnuNode implements Runnable {
    public static final int RECEIVETIMEOUT = 5000;
    public static final int PING_FREQ = 5;
    public static final int MAX_NEIGH = 2;
    public static final String CTRLADDR = "inproc://ctrl";
    public static final int MAX_TTL = 4;
    public static final int PING_TTL = 1;

    private final Integer id;

    private final ConcurrentHashMap<String, List<Integer>> messagesSent;
    private final ConcurrentHashMap<Integer, GnuNodeInfo> neighbors;
    private final CopyOnWriteArraySet<InetSocketAddress> hostsCache;

    private final InetSocketAddress addr;
    private final int capacity;
    private final ExecutorService executors;
    private final ExecutorService timeouts;
    private final ServerSocket serverSocket;

    public GnuNode(Integer id, String address, int port) throws IOException {
        this.id = id; // TODO hash username

        this.neighbors = new ConcurrentHashMap<>();
        this.messagesSent = new ConcurrentHashMap<>();
        this.hostsCache = new CopyOnWriteArraySet<>();

        int max_reqs = Runtime.getRuntime().availableProcessors() + 1;
        this.executors = Executors.newFixedThreadPool(max_reqs);
        this.timeouts = Executors.newCachedThreadPool();
        this.capacity = (int) ((Math.random() * (10 - 2)) + 2);

        this.addr = new InetSocketAddress(address, port);
        this.serverSocket = new ServerSocket(this.addr.getPort(), 100, this.addr.getAddress());
    }

    private void bootstrap() {
        if (this.id == 1) return;
        this.pickNeighborToDrop(new InetSocketAddress("localhost", 8081));
    }

    public boolean pickNeighborToDrop(InetSocketAddress newNeighAddr) {
        try (Socket socket = new Socket(newNeighAddr.getAddress(), newNeighAddr.getPort())) {
            // ask if peer wants to become neighbor
            GnuMessage msg = new NumNeighMessage(this.addr, this.neighbors.size());
            ObjectOutputStream oss = new ObjectOutputStream(socket.getOutputStream());
            oss.writeObject(msg);
            // wait reply
            ObjectInputStream iss = new ObjectInputStream(socket.getInputStream());
            MyNeighMessage reply = (MyNeighMessage) iss.readObject();
            // check if it wants
            if (reply.getNeighbors() == -1) return false;

            if (this.neighbors.size() + 1 <= GnuNode.MAX_NEIGH) {
                // we have room
                msg = new NeighMessage(this.addr, this.id, this.neighbors.size(), this.capacity);
                oss.writeObject(msg);
                return true;
            } else {
                Integer highestNeigh = Collections.max(neighbors.entrySet(),
                        Comparator.comparingInt(e -> (e.getValue()).nNeighbors)).getKey();
                GnuNodeInfo toDrop = this.neighbors.get(highestNeigh);
                if (toDrop.nNeighbors > reply.getNeighbors() + 1) {
                    Socket dropSocket = new Socket(toDrop.address.getAddress(), toDrop.address.getPort());
                    ObjectOutputStream dropOos = new ObjectOutputStream(dropSocket.getOutputStream());
                    DropMessage dm = new DropMessage(this.addr, this.id);
                    dropOos.writeObject(dm);

                    ObjectInputStream dropOis = new ObjectInputStream(dropSocket.getInputStream());
                    GnuMessage dropReply = (GnuMessage) iss.readObject();

                    this.neighbors.remove(highestNeigh);
                    msg = new NeighMessage(this.addr, this.id, this.neighbors.size(), this.capacity);
                    oss.writeObject(msg);
                    return true;
                }
            }
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("Failed to connect to endpoint!");
            return false;
        }
        return false;
    }

    public void send(String guid, String toSend, String currentHop) {
        if (!this.messagesSent.containsKey(guid))
            this.messagesSent.put(guid, new ArrayList<>());
        int nTries = 0;

        while (nTries < 5) {
            for (Map.Entry<Integer, GnuNodeInfo> neighbour :
                    this.neighbors.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(n -> n.capacity))).toList()) {

                if (this.messagesSent.get(guid).contains(neighbour.getKey())) continue;
                this.messagesSent.get(guid).add(neighbour.getKey());

                // TODO it would be nice to have more checks here
                if (neighbour.getValue().state == 0) continue;
                ZMsg msg = GnuNodeCMD.QUERY.getMessage(
                        Arrays.asList(String.valueOf(this.id), currentHop, String.valueOf(GnuNode.MAX_TTL), toSend));
                try {
                    ZMQ.Socket skt = this.zctx.createSocket(SocketType.REQ);
                    skt.connect(neighbour.getValue().address);
                    msg.send(skt);
                    ZMsg msgReply = ZMsg.recvMsg(skt);
                    UnidentifiedMessage reply = new UnidentifiedMessage(msgReply);
                    if (reply.getCmd().equals("QUERYHIT")) {

                        return;
                    }
                } catch (Exception e) {
                    System.err.println("Failed to communicate with endpoint!");
                    continue;
                }
                break;
            }
            nTries++;
            this.messagesSent.get(guid).clear();
        }
    }

    private boolean handleDrop() {
        return this.neighbors.size() != 1;
    }

    private boolean handleNeigh(NeighMessage um) {
        if (this.neighbors.size() < GnuNode.MAX_NEIGH)
            return true;

        Map.Entry<Integer, GnuNodeInfo> maxEntry = null;
        for (Map.Entry<Integer, GnuNodeInfo> entry : this.neighbors.entrySet()) {
            if (maxEntry == null || (entry.getValue().nNeighbors > maxEntry.getValue().nNeighbors)) {
                maxEntry = entry;
            }
        }

        if (maxEntry.getValue().nNeighbors > um.getNeighbors() + 1) {

            DropMessage dropMessage = new DropMessage(this.addr, maxEntry.getKey());
            GnuMessage reply;
            try (Socket socket = new Socket(um.getAddr().getAddress(), um.getAddr().getPort())) {
                ObjectOutputStream oss = new ObjectOutputStream(socket.getOutputStream());
                oss.writeObject(dropMessage);

                ObjectInputStream iss = new ObjectInputStream(socket.getInputStream());
                reply = (GnuMessage) iss.readObject();
            } catch (ClassNotFoundException | IOException e) {
                System.err.println("Failed to connect to endpoint!");
                return false;
            }

            if (reply.getCmd() == GnuNodeCMD.DROPOK) {
                this.neighbors.remove(maxEntry.getKey());
                return true;
            }
        }
        return false;
    }

    // TODO: PONG caching
    private void ping() {
        // TODO DEBUG purposes
        System.out.println("Neighboors:");
        for (Map.Entry<Integer, GnuNodeInfo> neigh : this.neighbors.entrySet()) {
            System.out.println("\t" + neigh.getKey() + " - " + neigh.getValue().state);
        }

        ping_loop:
        for (Map.Entry<Integer, GnuNodeInfo> e : this.neighbors.entrySet()) {
            GnuNodeInfo peerNode = e.getValue();
            if (peerNode.state == 1)
                peerNode.setDetermining();

            // we will determine their state now
            GnuMessage pingMsg = GnuNodeCMD.PING.getMessage(this.addr);

            // try to connect and send the request
            PongMessage reply;
            try (Socket sockPing = new Socket(peerNode.address.getAddress(), peerNode.address.getPort())) {
                sockPing.setSoTimeout(GnuNode.RECEIVETIMEOUT);
                ObjectOutputStream oss = new ObjectOutputStream(sockPing.getOutputStream());
                oss.writeObject(pingMsg);
                System.out.println("SENT PING");
                // try to receive the reply
                InputStream is = sockPing.getInputStream();
                ObjectInputStream iss = new ObjectInputStream(is);
                reply = (PongMessage) iss.readObject();
            } catch (ClassNotFoundException | IOException exception) {
                System.out.println("Failed to connect to " + e.getKey());
                peerNode.setDead();
                continue ping_loop;
            }

            // process the reply
            System.out.println("RECEIVED PONG CMD");
            peerNode.setAlive(); // peer is good
            // update the hosts cache
            peerNode.nNeighbors = reply.getNeighAddrs().size();
            Set<InetSocketAddress> unknownAddrs = new HashSet<>();
            for (InetSocketAddress address : reply.getNeighAddrs()) {
                if (!this.hostsCache.add(address))
                    unknownAddrs.add(address);
            }
            // TODO only do this periodically
            for (InetSocketAddress newNeighbor : unknownAddrs) {
                this.pickNeighborToDrop(newNeighbor);
            }
        }
    }

    /**
     * Server dispatcher loop: accepts connections and spawns a thread to handle them.
     */
    @Override
    public void run() {
        // TODO should this be here?
        this.bootstrap();

        // schedule pings
        ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();
        pingScheduler.scheduleAtFixedRate(this::ping, 1, PING_FREQ, TimeUnit.SECONDS);

        while (!Thread.currentThread().isInterrupted()) {
            Socket reqSocket;
            GnuMessage reqMsg;
            try {
                reqSocket = this.serverSocket.accept();
                ObjectInputStream ois = new ObjectInputStream(reqSocket.getInputStream());
                reqMsg = (GnuMessage) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Receiving failed with exception:");
                e.printStackTrace();
                continue;
            }

            // handle request (the thread is now responsible for the socket)
            this.executors.execute(() -> this.handleMessage(reqSocket, reqMsg));

            /*
            if (plr.pollin(1)) {
                IdentifiedMessage msg = new IdentifiedMessage(ZMsg.recvMsg(this.socket));
                String type = msg.getCmd();
                ZMsg reply;
                System.out.println("RECEIVED " + type + " CMD");
                switch (type) {
                    case GnuNodeCMD.NUMNEIGH.toString() -> {
                        reply = new IdentifiedMessage(
                                msg.getIdentity(),
                                GnuNodeCMD.MYNEIGH.toString(),
                                Collections.singletonList(String.valueOf(this.neigh.size()))
                        ).newZMsg();
                        reply.send(this.routerSock);
                    }
                    case GnuNodeCMD.NEIGH.toString() -> {
                        boolean neighOk = this.handleNeigh(msg);
                        if (neighOk) {
                            reply = new IdentifiedMessage(msg.getIdentity(), GnuNodeCMD.NEIGHOK.toString(), new ArrayList<>(this.id, this.capacity)).newZMsg();
                            reply.send(this.routerSock);
                            this.neigh.put(msg.getArg(0), new GnuNodeInfo(Integer.parseInt(msg.getArg(1)), Integer.parseInt(msg.getArg(1))));
                        } else {
                            reply = new IdentifiedMessage(msg.getIdentity(), GnuNodeCMD.NEIGHERR.toString(), new ArrayList<>()).newZMsg();
                            reply.send(this.routerSock);
                            reply.send(this.routerSock);
                        }
                    }
                    case GnuNodeCMD.DROP.toString() -> {
                        boolean canDrop = this.handleDrop();
                        if (canDrop) {
                            reply = new IdentifiedMessage(msg.getIdentity(), GnuNodeCMD.DROPOK.toString(), new ArrayList<>()).newZMsg();
                            reply.send(this.routerSock);
                            this.neigh.remove(msg.getArg(0));

                        } else {
                            reply = new IdentifiedMessage(msg.getIdentity(), GnuNodeCMD.DROPERR.toString(), new ArrayList<>()).newZMsg();
                            reply.send(this.routerSock);
                        }
                    }
                    case Descriptor.PING -> {
                        List<String> addresses = new ArrayList<>(List.of(this.routerSockAddr));
                        addresses.addAll(this.neigh.keySet());
                        reply = new IdentifiedMessage(msg.getIdentity(), Descriptor.PONG, addresses).newZMsg();
                        reply.send(this.routerSock);
                    }
                    case Descriptor.QUERY -> {

                    }
                }
            }
             */
        }

        pingScheduler.shutdownNow();
    }

    public void handleMessage(Socket reqSocket, GnuMessage reqMsg) {
        System.out.println("RECEIVED " + reqMsg.getCmd() + " CMD");

        GnuMessage reply;
        switch (reqMsg.getCmd()) {
            case PING -> {
                try {
                    this.handlePing(reqSocket, reqMsg);
                } catch (IOException e) {
                    System.err.println("Ping handling failed.");
                    e.printStackTrace();
                }
            }
            case NUMNEIGH -> {
                reply = new MyNeighMessage(this.addr, this.neighbors.size());
                try {
                    ObjectOutputStream oss = new ObjectOutputStream(reqSocket.getOutputStream());
                    oss.writeObject(reply);
                } catch (IOException e) {
                    System.err.println("NUMNEIGH handling failed.");
                }
            }
            case NEIGH -> {
                boolean neighOk = this.handleNeigh((NeighMessage) reqMsg);

                if (neighOk) {

                    reply = new IdentifiedMessage(msg.getIdentity(), GnuNodeCMD.NEIGHOK.toString(), List.of(String.valueOf(this.id), String.valueOf(this.capacity))).newZMsg();
                    reply.send(this.serverSocket);
                    this.neighbors.put(Integer.parseInt(msg.getArg(0)), new GnuNodeInfo(Integer.parseInt(msg.getArg(2)), Integer.parseInt(msg.getArg(3)), msg.getArg(1)));
                } else {
                    reply = new IdentifiedMessage(msg.getIdentity(), GnuNodeCMD.NEIGHERR.toString(), new ArrayList<>()).newZMsg();
                    reply.send(this.serverSocket);
                    reply.send(this.serverSocket);
                }
            }
            case DROP -> {
                boolean canDrop = this.handleDrop();
                if (canDrop) {
                    reply = new IdentifiedMessage(msg.getIdentity(), GnuNodeCMD.DROPOK.toString(), new ArrayList<>()).newZMsg();
                    reply.send(this.serverSocket);
                    this.neighbors.remove(Integer.parseInt(msg.getArg(0)));

                } else {
                    reply = new IdentifiedMessage(msg.getIdentity(), GnuNodeCMD.DROPERR.toString(), new ArrayList<>()).newZMsg();
                    reply.send(this.serverSocket);
                }
            }
        }
    }

    /**
     * Got a ping so we send a PONG.
     *
     * @param reqSocket
     * @param reqMsg
     */
    private void handlePing(Socket reqSocket, GnuMessage reqMsg) throws IOException {
        // craft reply
        List<InetSocketAddress> addresses = new ArrayList<>(List.of(this.addr));
        for (Map.Entry<Integer, GnuNodeInfo> entry : this.neighbors.entrySet()) {
            InetSocketAddress eAddr = entry.getValue().address;
            if (!reqMsg.getAddr().equals(eAddr)) addresses.add(eAddr);
        }
        GnuMessage replyMsg = new PongMessage(this.addr, addresses);

        // reply to the same socket
        OutputStream os = reqSocket.getOutputStream();
        ObjectOutputStream oss = new ObjectOutputStream(os);
        oss.writeObject(replyMsg);

        reqSocket.close();
    }

    private void handlePing(Socket reqSocket, GnuMessage reqMsg) throws IOException {

    }
}
