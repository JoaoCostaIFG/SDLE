package org.t3.g11.proj2.nuttela;

import org.t3.g11.proj2.message.IdentifiedMessage;
import org.t3.g11.proj2.message.UnidentifiedMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.*;
import java.util.concurrent.*;

public class GnuNode implements Runnable {
    public static final int RECEIVETIMEOUT = 5000;
    public static final int PING_FREQ = 5;
    public static final int MAX_NEIGH = 2;
    public static final String CTRLADDR = "inproc://ctrl";
    public static final int MAX_TTL = 4;
    public static final int PING_TTL = 1;

    private final ZContext zctx;
    private final Integer id;
    private final ConcurrentHashMap<Integer, GnuNodeInfo> neighbors;
    private final CopyOnWriteArraySet<String> hostsCache;
    private final String routerSockAddr;
    private final int capacity;
    private final ExecutorService executors;

    private ZMQ.Socket socket;
    private ZMQ.Socket ctrlSock;

    public GnuNode(ZContext zctx, Integer id, String routerAddr) {
        this.zctx = zctx;
        this.id = id; // TODO hash username

        this.neighbors = new ConcurrentHashMap<>();
        this.hostsCache = new CopyOnWriteArraySet<>();

        this.routerSockAddr = routerAddr;
        this.executors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
        this.capacity = (int) ((Math.random() * (10 - 2)) + 2);
    }

    private void initSockets() {
        this.socket = zctx.createSocket(SocketType.ROUTER);
        this.socket.bind(this.routerSockAddr);
        this.ctrlSock = zctx.createSocket(SocketType.PULL);
        this.ctrlSock.bind(GnuNode.CTRLADDR);
    }

    private void bootstrap() {
        if (this.id == 1) return;
        this.pickNeighborToDrop("tcp://*:8080");
    }

    public boolean pickNeighborToDrop(String newNeighAddr) {
        ZMsg msg = GnuNodeCMD.NUMNEIGH.getMessage();
        ZMQ.Socket skt = this.zctx.createSocket(SocketType.REQ);
        try {
            if (!skt.connect(newNeighAddr)) {
                System.err.println("Failed to connect to endpoint!");
                return false;
            }
            msg.send(skt);
        } catch (Exception e) {
            System.err.println("Failed to connect to endpoint!");
            return false;
        }

        ZMsg msgReply;
        try {
            msgReply = ZMsg.recvMsg(skt);
        } catch (Exception e) {
            System.err.println("Received null message");
            return false;
        }

        UnidentifiedMessage reply = new UnidentifiedMessage(msgReply);

        int n_neigh = Integer.parseInt(reply.getArg(0));

        if (this.neighbors.size() + 1 <= GnuNode.MAX_NEIGH) {
            // we have room
            ZMsg neighMsg = GnuNodeCMD.NEIGH.getMessage(Arrays.asList(String.valueOf(this.id), this.routerSockAddr,
                    String.valueOf(this.neighbors.size()), String.valueOf(this.capacity)));
            neighMsg.send(skt);
            UnidentifiedMessage um = new UnidentifiedMessage(ZMsg.recvMsg(skt));
            skt.close();
            if (um.getCmd().equals(GnuNodeCMD.NEIGHOK.toString())) {
                this.neighbors.put(Integer.parseInt(um.getArg(0)), new GnuNodeInfo(n_neigh, Integer.parseInt(um.getArg(1)), newNeighAddr));
                return true;
            } else {
                return false;
            }

        } else {
            Integer highestNeigh = Collections.max(neighbors.entrySet(),
                    Comparator.comparingInt(e -> (e.getValue()).nNeighbors)).getKey();
//            System.out.println("Highest: " + this.neigh.get(highestNeigh) + "  Local: " + (n_neigh + 1));
            if (this.neighbors.get(highestNeigh).capacity > n_neigh + 1) {
                ZMsg neighMsg = GnuNodeCMD.NEIGH.getMessage(Arrays.asList(String.valueOf(this.id), this.routerSockAddr,
                        String.valueOf(this.neighbors.size()), String.valueOf(this.capacity)));
                neighMsg.send(skt);
                UnidentifiedMessage um = new UnidentifiedMessage(ZMsg.recvMsg(skt));
                skt.close();
                if (um.getCmd().equals(GnuNodeCMD.NEIGHOK.toString())) {
                    this.neighbors.remove(highestNeigh);
                    this.neighbors.put(Integer.parseInt(um.getArg(0)), new GnuNodeInfo(n_neigh, Integer.parseInt(um.getArg(1)), newNeighAddr));
                    return true;
                } else {
                    return false;
                }
            }
        }

        skt.close();
        return false;
    }

    public void send(String toSend, String currentHop) {
//        ZMsg msg = GnuNodeCMD.QUERY.getMessage(
//                Arrays.asList(this.id, currentHop, String.valueOf(GnuNode.MAX_TTL), toSend));
//        for (GnuNodeInfo peerToSend : this.neighbors.values()) {
//            sender.execute(() -> {
//                int nTries = 0;
//                while (nTries < 5) {
//                    ZMQ.Socket skt = zctx.createSocket(SocketType.REQ);
//                    try {
//                        skt.connect(peerToSend.address);
//                    } catch (Exception e) {
//                        System.err.println("Failed to connect to endpoint!");
//                    }
//                    msg.send(skt);
//                    UnidentifiedMessage reply = new UnidentifiedMessage(ZMsg.recvMsg(skt));
//                    if (reply.getCmd().equals("QUERYHIT")) return;
//                    else nTries++;
//                }
//            });
//        }
    }

    private boolean handleDrop() {
        return this.neighbors.size() != 1;
    }

    private boolean handleNeigh(UnidentifiedMessage um) {
        if (this.neighbors.size() < GnuNode.MAX_NEIGH)
            return true;

        Map.Entry<Integer, GnuNodeInfo> maxEntry = null;
        for (Map.Entry<Integer, GnuNodeInfo> entry : this.neighbors.entrySet()) {
            if (maxEntry == null || (entry.getValue().nNeighbors > maxEntry.getValue().nNeighbors)) {
                maxEntry = entry;
            }
        }
        if (maxEntry.getValue().nNeighbors > Integer.parseInt(um.getArg(2)) + 1) {
            ZMQ.Socket skt = this.zctx.createSocket(SocketType.REQ);
            try {
                skt.connect(maxEntry.getValue().address);
            } catch (Exception e) {
                System.err.println("Failed to connect to endpoint!");
                return false;
            }
            ZMsg toSend = new UnidentifiedMessage("DROP", Arrays.asList(String.valueOf(this.id), this.routerSockAddr)).newZMsg();
            toSend.send(skt);
            UnidentifiedMessage reply = new UnidentifiedMessage(ZMsg.recvMsg(skt));
            skt.close();
            if (reply.getCmd().equals("DROPOK")) {
                this.neighbors.remove(maxEntry.getKey());
                return true;
            }
        }
        return false;
    }

    @Override
    public void run() {
        this.initSockets();
        this.bootstrap();

        ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();
        pingScheduler.scheduleAtFixedRate(() -> {
            // TODO is dis bad?
            ZMQ.Socket pushSocket = this.zctx.createSocket(SocketType.PUSH);
            pushSocket.connect(GnuNode.CTRLADDR);
            GnuNodeCMD.PING.getMessage().send(pushSocket);
        }, 1, PING_FREQ, TimeUnit.SECONDS);

        ZMQ.Poller plr = zctx.createPoller(2);
        plr.register(this.ctrlSock, ZMQ.Poller.POLLIN);
        plr.register(this.socket, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted() && plr.poll(3000) >= 0) {
            if (plr.pollin(0)) {
                ZMsg zMsg = ZMsg.recvMsg(this.ctrlSock);
                UnidentifiedMessage msg = new UnidentifiedMessage(zMsg);
                this.handleCtrlMsg(msg);
            }
            if (plr.pollin(1)) {
//                ZMsg zMsg = ZMsg.recvMsg(this.ctrlSock);
//                IdentifiedMessage msg = new IdentifiedMessage(zMsg);
//                System.out.println(msg.getCmd());
                this.handleMessage(ZMsg.recvMsg(this.socket));
            }
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

    public void handleMessage(ZMsg message) {
        IdentifiedMessage msg = new IdentifiedMessage(message);
        GnuNodeCMD cmd;
        try {
            cmd = GnuNodeCMD.valueOf(msg.getCmd());
        } catch (IllegalArgumentException e) {
            // unrecognized command
            System.err.printf("Unknown command: [command=%s]\n", msg.getCmd());
            return;
        }

        ZMsg reply;
        System.out.println("RECEIVED " + cmd + " CMD");
        switch (cmd) {
            case NUMNEIGH -> {
                reply = new IdentifiedMessage(
                        msg.getIdentity(),
                        GnuNodeCMD.MYNEIGH.toString(),
                        Collections.singletonList(String.valueOf(this.neighbors.size()))
                ).newZMsg();
                reply.send(this.socket);
            }
            case NEIGH -> {
                boolean neighOk = this.handleNeigh(msg);
                if (neighOk) {
                    reply = new IdentifiedMessage(msg.getIdentity(), GnuNodeCMD.NEIGHOK.toString(), List.of(String.valueOf(this.id), String.valueOf(this.capacity))).newZMsg();
                    reply.send(this.socket);
                    this.neighbors.put(Integer.parseInt(msg.getArg(0)), new GnuNodeInfo(Integer.parseInt(msg.getArg(2)), Integer.parseInt(msg.getArg(3)), msg.getArg(1)));
                } else {
                    reply = new IdentifiedMessage(msg.getIdentity(), GnuNodeCMD.NEIGHERR.toString(), new ArrayList<>()).newZMsg();
                    reply.send(this.socket);
                    reply.send(this.socket);
                }
            }
            case DROP -> {
                boolean canDrop = this.handleDrop();
                if (canDrop) {
                    reply = new IdentifiedMessage(msg.getIdentity(), GnuNodeCMD.DROPOK.toString(), new ArrayList<>()).newZMsg();
                    reply.send(this.socket);
                    this.neighbors.remove(Integer.parseInt(msg.getArg(0)));

                } else {
                    reply = new IdentifiedMessage(msg.getIdentity(), GnuNodeCMD.DROPERR.toString(), new ArrayList<>()).newZMsg();
                    reply.send(this.socket);
                }
            }
            case PING -> {
                List<String> addresses = new ArrayList<>(List.of(String.valueOf(this.id)));
                for (Map.Entry<Integer, GnuNodeInfo> entry : this.neighbors.entrySet()) {
                    addresses.add(entry.getValue().address);
                }
                reply = new IdentifiedMessage(msg.getIdentity(), GnuNodeCMD.PONG.toString(), addresses).newZMsg();
                reply.send(this.socket);
            }
//            case Descriptor.QUERY -> {
//
//            }
        }
    }

    private void handleCtrlMsg(UnidentifiedMessage msg) {
        GnuNodeCMD cmd;
        try {
            cmd = GnuNodeCMD.valueOf(msg.getCmd());
        } catch (IllegalArgumentException e) {
            // unrecognized command
            System.err.printf("Unknown command: [command=%s]\n", msg.getCmd());
            return;
        }

        switch (cmd) {
            case PING:
                this.ping();
                break;
            default:
                // ignore
                break;
        }
    }

    // TODO: PONG caching
    private void ping() {

        System.out.println("Neighboors:");
        for (Map.Entry<Integer, GnuNodeInfo> neigh : this.neighbors.entrySet()) {
            System.out.println("\t" + neigh.getKey() + " - " + neigh.getValue().state);
        }

        for (Map.Entry<Integer, GnuNodeInfo> e : this.neighbors.entrySet()) {
            // we will determine their state now
            this.executors.execute(() -> {
                List<String> pingArgs = List.of(String.valueOf(this.id), "0", String.valueOf(GnuNode.PING_TTL));
                ZMsg pingMsg = GnuNodeCMD.PING.getMessage(pingArgs);
                GnuNodeInfo peerNode = e.getValue();
                if (peerNode.state == 1)
                    peerNode.setDetermining();

                // try to connect and send the request
                ZMQ.Socket sock = this.zctx.createSocket(SocketType.REQ);
                sock.setReceiveTimeOut(GnuNode.RECEIVETIMEOUT);
                try {
                    if (!sock.connect(e.getValue().address)) {
                        System.out.println("Failed to connect to " + e.getKey());
                        peerNode.setDead();
                        return;
                    }
                    pingMsg.send(sock);
                    System.out.println("SENT PING");
                } catch (Exception exception) {
                    System.err.println("Failed to connect to endpoint!");
                    peerNode.setDead();
                    return;
                }
                // try to receive the reply
                ZMsg replyZMsg = ZMsg.recvMsg(sock);
                if (replyZMsg == null) {
                    System.out.println("RECEIVED nullllllllll");
                    peerNode.setDead();
                    return;
                }
                // process the reply
                UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);
                System.out.println("RECEIVED PONG : " + reply.getArg(0));
                if (!reply.getCmd().equals("PONG")) {
                    peerNode.setDead();
                    return;
                }
                System.out.println("RECEIVED PONG CMD");
                // it's good
                peerNode.setAlive();
                // update the hosts cache
                // TODO pong messages header
                peerNode.nNeighbors = reply.getArgCount() - 1;
                Set<String> unknownAddrs = new HashSet<>();
                for (int i = 1; i < reply.getArgCount(); ++i) {
                    String address = reply.getArg(i);
                    if (address.equals(this.routerSockAddr)) continue;
                    if (!this.hostsCache.add(address))
                        unknownAddrs.add(address);
                }
                // TODO only do this periodically
                for (String newNeighbor : unknownAddrs) {
                    this.pickNeighborToDrop(newNeighbor);
                }
            });
        }
    }
}
