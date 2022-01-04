package org.t3.g11.proj2.nuttela;

import org.t3.g11.proj2.message.UnidentifiedMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.*;
import java.util.concurrent.*;

public class GnuNode implements Runnable {
    public static final int PING_FREQ = 5;
    public static final int MAX_NEIGH = 2;
    public static final String ctrlAddr = "inproc://ctrl";
    public static final int MAX_TTL = 4;
    public static final int PING_TTL = 1;

    private final ZContext zctx;
    private final String id;

    private final HashMap<String, GnuNodeInfo> neigh;
    private final List<String> deadNeigh;

    private final String routerSockAddr;
    private final int capacity;
    private ZMQ.Socket socket;
    private ZMQ.Socket ctrlSock;

    private final ExecutorService sender = Executors.newFixedThreadPool(GnuNode.MAX_NEIGH);

    public GnuNode(ZContext zctx, String id, String routerAddr) {
        this.zctx = zctx;
        this.id = id; // TODO hash username

        this.neigh = new HashMap<>();
        this.deadNeigh = new ArrayList<>();

        this.routerSockAddr = routerAddr;
        this.capacity = (int) ((Math.random() * (10 - 2)) + 2);
    }

    private void initSockets() {
        //this.routerSock = zctx.createSocket(SocketType.ROUTER);
        //this.routerSock.bind(this.routerSockAddr);
        this.socket = zctx.createSocket(SocketType.PEER);
        this.socket.bind(this.routerSockAddr);

        this.ctrlSock = zctx.createSocket(SocketType.PULL);
        this.ctrlSock.bind(GnuNode.ctrlAddr);
    }

    private void bootstrap() {
        if (this.id.equals("1")) return;
        this.pickNeighborToDrop("tcp://*:8080");
    }

    public boolean pickNeighborToDrop(String newNeighAddr) {
        ZMsg msg = new UnidentifiedMessage(GnuNodeCMD.NUMNEIGH.toString(), new ArrayList<>()).newZMsg();
        ZMQ.Socket skt = this.zctx.createSocket(SocketType.REQ);
        try {
            skt.connect(newNeighAddr);
        } catch (Exception e) {
            System.err.println("Failed to connect to endpoint!");
            return false;
        }
        msg.send(skt);
        UnidentifiedMessage reply = new UnidentifiedMessage(ZMsg.recvMsg(skt));

        int n_neigh = Integer.parseInt(reply.getArg(0));

        if (this.neigh.size() + 1 <= GnuNode.MAX_NEIGH) {
            // we have room
            ZMsg neighMsg = new UnidentifiedMessage(GnuNodeCMD.NEIGH.toString(),
                    Arrays.asList(this.routerSockAddr, String.valueOf(this.neigh.size()), String.valueOf(this.capacity))).newZMsg();
            neighMsg.send(skt);
            UnidentifiedMessage um = new UnidentifiedMessage(ZMsg.recvMsg(skt));
            skt.close();
            if (um.getCmd().equals(GnuNodeCMD.NEIGHOK.toString())) {
                this.neigh.put(newNeighAddr, new GnuNodeInfo(n_neigh, Integer.parseInt(um.getArg(0))));
                return true;
            } else {
                return false;
            }

        } else {
            String highestNeigh = Collections.max(neigh.entrySet(),
                    Comparator.comparingInt(v -> ((GnuNodeInfo) v).nNeighbors)).getKey();
//            System.out.println("Highest: " + this.neigh.get(highestNeigh) + "  Local: " + (n_neigh + 1));
            if (this.neigh.get(highestNeigh).capacity > n_neigh + 1) {
                ZMsg neighMsg = new UnidentifiedMessage(GnuNodeCMD.NEIGH.toString(),
                        Arrays.asList(this.routerSockAddr, String.valueOf(this.neigh.size()), String.valueOf(this.capacity))).newZMsg();
                neighMsg.send(skt);
                UnidentifiedMessage um = new UnidentifiedMessage(ZMsg.recvMsg(skt));
                skt.close();
                if (um.getCmd().equals(GnuNodeCMD.NEIGHOK.toString())) {
                    this.neigh.remove(highestNeigh);
                    this.neigh.put(newNeighAddr, new GnuNodeInfo(n_neigh, Integer.parseInt(um.getArg(0))));
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
        ZMsg msg = GnuNodeCMD.QUERY.getMessage(
                Arrays.asList(this.id, currentHop, String.valueOf(GnuNode.MAX_TTL), toSend));
        for (String addrToSend : this.neigh.keySet()) {
            sender.execute(() -> {
                int nTries = 0;
                while (nTries < 5) {
                    ZMQ.Socket skt = zctx.createSocket(SocketType.REQ);
                    try {
                        skt.connect(addrToSend);
                    } catch (Exception e) {
                        System.err.println("Failed to connect to endpoint!");
                    }
                    msg.send(skt);
                    UnidentifiedMessage reply = new UnidentifiedMessage(ZMsg.recvMsg(skt));
                    if (reply.getCmd().equals("QUERYHIT")) return;
                    else nTries++;
                }
            });
        }
    }

    private boolean handleDrop() {
        return this.neigh.size() != 1;
    }

    private boolean handleNeigh(UnidentifiedMessage um) {
        if (this.neigh.size() < GnuNode.MAX_NEIGH)
            return true;

        Map.Entry<String, GnuNodeInfo> maxEntry = null;
        for (Map.Entry<String, GnuNodeInfo> entry : this.neigh.entrySet()) {
            if (maxEntry == null || (entry.getValue().nNeighbors > maxEntry.getValue().nNeighbors)) {
                maxEntry = entry;
            }
        }
        if (maxEntry.getValue().nNeighbors > Integer.parseInt(um.getArg(1)) + 1) {
            ZMQ.Socket skt = this.zctx.createSocket(SocketType.REQ);
            try {
                skt.connect(maxEntry.getKey());
            } catch (Exception e) {
                System.err.println("Failed to connect to endpoint!");
                return false;
            }
            ZMsg toSend = new UnidentifiedMessage("DROP", Collections.singletonList(this.routerSockAddr)).newZMsg();
            toSend.send(skt);
            UnidentifiedMessage reply = new UnidentifiedMessage(ZMsg.recvMsg(skt));
            skt.close();
            if (reply.getCmd().equals("DROPOK")) {
                this.neigh.remove(maxEntry.getKey());
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
            pushSocket.connect(GnuNode.ctrlAddr);
            GnuNodeCMD.PING.getMessage().send(pushSocket);
        }, 1, PING_FREQ, TimeUnit.SECONDS);

        ZMQ.Poller plr = zctx.createPoller(2);
        plr.register(this.ctrlSock, ZMQ.Poller.POLLIN);
        plr.register(this.routerSock, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted() && plr.poll(3000) >= 0) {
            if (plr.pollin(0)) {
                ZMsg zMsg = ZMsg.recvMsg(this.ctrlSock);
                UnidentifiedMessage msg = new UnidentifiedMessage(zMsg);
                this.handleCtrlMsg(msg);
            }
            /*
            if (plr.pollin(1)) {
                IdentifiedMessage msg = new IdentifiedMessage(ZMsg.recvMsg(this.routerSock));
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
                            reply = new IdentifiedMessage(msg.getIdentity(), GnuNodeCMD.NEIGHOK.toString(), new ArrayList<>(this.capacity)).newZMsg();
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
            case NEIGH:
                break;
            case NUMNEIGH:
                break;
            case MYNEIGH:
                break;
            case NEIGHOK:
                break;
            case NEIGHERR:
                break;
            case DROPOK:
                break;
            case DROPERR:
                break;
            case DROP:
                break;
            case PING:
                ZMsg zMsg = new ZMsg();
                this.ping();
                break;
            case PONG:
                break;
            case PUSH:
                break;
            case QUERY:
                break;
            case QUERYHIT:
                break;
        }
    }

    private void ping() {
        for (String s : deadNeigh) {
            this.neigh.remove(s);
        }

        List<String> pingArgs = List.of(this.id, "0",
                String.valueOf(GnuNode.PING_TTL), this.routerSockAddr);
        ZMsg pingMsg = GnuNodeCMD.PING.getMessage(pingArgs);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

        for (String neighborAdd : this.neigh.keySet()) {
            this.deadNeigh.add(neighborAdd);
            executor.submit(() -> {
                ZMQ.Socket sock = zctx.createSocket(SocketType.REQ);
                if (!sock.connect(neighborAdd)) {
                    deadNeigh.add(neighborAdd);
                    return;
                }
                try {
                    sock.connect(neighborAdd);
                } catch (Exception e) {
                    System.err.println("Failed to connect to endpoint!");
                }
                pingMsg.send(sock);

                UnidentifiedMessage reply = new UnidentifiedMessage(ZMsg.recvMsg(sock));

                if (!reply.getCmd().equals("PONG")) return;

                Set<String> unknownAddrs = new HashSet<>();

                this.neigh.get(neighborAdd).nNeighbors = reply.getArgCount() - 1;

                for (int i = 0; i < reply.getArgCount(); ++i) {
                    String address = reply.getArg(i);
                    if (address.equals(this.routerSockAddr))
                        continue;
                    if (!this.neigh.containsKey(address))
                        unknownAddrs.add(address);
                }

                // TODO: only do this periodically
                for (String newNeighbor : unknownAddrs) {
                    this.pickNeighborToDrop(newNeighbor);
                }

                this.deadNeigh.remove(neighborAdd);
            });
        }
    }
}
