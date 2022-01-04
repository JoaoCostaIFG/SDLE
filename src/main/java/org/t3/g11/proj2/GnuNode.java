package org.t3.g11.proj2;

import org.t3.g11.proj2.message.Descriptor;
import org.t3.g11.proj2.message.IdentifiedMessage;
import org.t3.g11.proj2.message.UnidentifiedMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.*;
import java.util.concurrent.*;

public class GnuNode {
    public static final String NEIGH = "NEIGH";
    public static final String NUMNEIGH = "NUMNEIGH";
    public static final String MYNEIGH = "MYNEIGH";
    public static final String NEIGHOK = "NEIGHOK";
    public static final String NEIGHERR = "NEIGHERR";
    public static final String DROPOK = "DROPOK";
    public static final String DROPERR = "DROPERR";
    public static final String DROP = "DROP";

    public static final int PING_FREQ = 5;
    public static final int MAX_NEIGH = 2;

    private final HashMap<String, GnuNodeInfo> neigh;
    private final String id;
    private ZContext zctx;
    private final ZMQ.Socket routerSock;
    private final ZMQ.Socket dealerSock;
    private final String routerSockIP;
    private final List<String> deadNeighbors;
    private final int capacity;

    private final ExecutorService receiver = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService sender = Executors.newFixedThreadPool(GnuNode.MAX_NEIGH);

    public GnuNode(String id, String routerIP, String dealerIP) {
        this.id = id;
        deadNeighbors = new ArrayList<>();
        this.neigh = new HashMap<>();
        this.routerSockIP = routerIP;
        this.zctx = new ZContext();

        this.routerSock = zctx.createSocket(SocketType.ROUTER);
        this.routerSock.bind(this.routerSockIP);

        this.dealerSock = zctx.createSocket(SocketType.DEALER);
        this.dealerSock.bind(dealerIP);

        this.capacity = (int) ((Math.random() * (10 - 2)) + 2);
    }

    public void init() {
        this.bootstrap();
        scheduler.scheduleAtFixedRate(this::ping, 1, PING_FREQ, TimeUnit.SECONDS);
        this.receive();
    }

    public boolean pickNeighborToDrop(String newNeigh) {
        ZMsg msg = new UnidentifiedMessage(GnuNode.NUMNEIGH, new ArrayList<>()).newZMsg();
        ZMQ.Socket skt = this.zctx.createSocket(SocketType.REQ);
        try {
            skt.connect(newNeigh);
        } catch (Exception e) {
            System.err.println("Failed to connect to endpoint!");
        }
        msg.send(skt);
        UnidentifiedMessage reply = new UnidentifiedMessage(ZMsg.recvMsg(skt));

        int n_neigh = Integer.parseInt(reply.getArg(0));

        if (this.neigh.size() + 1 <= GnuNode.MAX_NEIGH) {
            // we have room
            ZMsg neighMsg = new UnidentifiedMessage(GnuNode.NEIGH,
                    Arrays.asList(this.routerSockIP, String.valueOf(this.neigh.size()), String.valueOf(this.capacity))).newZMsg();
            neighMsg.send(skt);
            UnidentifiedMessage um = new UnidentifiedMessage(ZMsg.recvMsg(skt));
            skt.close();
            if (um.getCmd().equals(GnuNode.NEIGHOK)) {
                this.neigh.put(newNeigh, new GnuNodeInfo(n_neigh, Integer.parseInt(um.getArg(0))));
                return true;
            } else {
                return false;
            }

        } else {
            String highestNeigh = Collections.max(neigh.entrySet(),
                    Comparator.comparingInt(v -> ((GnuNodeInfo) v).nNeighbors)).getKey();
//            System.out.println("Highest: " + this.neigh.get(highestNeigh) + "  Local: " + (n_neigh + 1));
            if (this.neigh.get(highestNeigh).capacity > n_neigh + 1) {
                ZMsg neighMsg = new UnidentifiedMessage(GnuNode.NEIGH,
                        Arrays.asList(this.routerSockIP, String.valueOf(this.neigh.size()), String.valueOf(this.capacity))).newZMsg();
                neighMsg.send(skt);
                UnidentifiedMessage um = new UnidentifiedMessage(ZMsg.recvMsg(skt));
                skt.close();
                if (um.getCmd().equals(GnuNode.NEIGHOK)) {
                    this.neigh.remove(highestNeigh);
                    this.neigh.put(newNeigh, new GnuNodeInfo(n_neigh, Integer.parseInt(um.getArg(0))));
                    return true;
                } else {
                    return false;
                }
            }
        }

        skt.close();
        return false;
    }

    private void bootstrap() {
        if (this.id.equals("1")) return;

        this.pickNeighborToDrop("tcp://*:8081");
    }

    public void send(String toSend, String currentHop) {
        ZMsg msg = new Descriptor("QUERY", Arrays.asList(this.id, currentHop, String.valueOf(Descriptor.MAX_TTL), toSend)).newZMsg();
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

    public void receive() {
        receiver.execute(() -> {
            ZMQ.Poller plr = zctx.createPoller(2);
            plr.register(this.routerSock, ZMQ.Poller.POLLIN);
            plr.register(this.dealerSock, ZMQ.Poller.POLLIN);

            while (plr.poll() >= 0) {
                if (plr.pollin(0)) {
                    IdentifiedMessage msg = new IdentifiedMessage(ZMsg.recvMsg(this.routerSock));
                    String type = msg.getCmd();
                    ZMsg reply;
//                    System.out.println("RECEIVED " + type + " CMD");
                    switch (type) {
                        case GnuNode.NUMNEIGH -> {
                            reply = new IdentifiedMessage(msg.getIdentity(), GnuNode.MYNEIGH, Collections.singletonList(String.valueOf(this.neigh.size()))).newZMsg();
                            reply.send(this.routerSock);
                        }
                        case GnuNode.NEIGH -> {

                            boolean neighOk = this.handleNeigh(msg);
                            if (neighOk) {
                                reply = new IdentifiedMessage(msg.getIdentity(), GnuNode.NEIGHOK, new ArrayList<>(this.capacity)).newZMsg();
                                reply.send(this.routerSock);
                                this.neigh.put(msg.getArg(0), new GnuNodeInfo(Integer.parseInt(msg.getArg(1)), Integer.parseInt(msg.getArg(1))));
                            } else {
                                reply = new IdentifiedMessage(msg.getIdentity(), GnuNode.NEIGHERR, new ArrayList<>()).newZMsg();
                                reply.send(this.routerSock);
                                reply.send(this.routerSock);
                            }
                        }
                        case GnuNode.DROP -> {
                            boolean canDrop = this.handleDrop();
                            if (canDrop) {
                                reply = new IdentifiedMessage(msg.getIdentity(), GnuNode.DROPOK, new ArrayList<>()).newZMsg();
                                reply.send(this.routerSock);
                                this.neigh.remove(msg.getArg(0));

                            } else {
                                reply = new IdentifiedMessage(msg.getIdentity(), GnuNode.DROPERR, new ArrayList<>()).newZMsg();
                                reply.send(this.routerSock);
                            }
                        }
                        case Descriptor.PING -> {
                            List<String> addresses = new ArrayList<>(List.of(this.routerSockIP));
                            addresses.addAll(this.neigh.keySet());
                            reply = new IdentifiedMessage(msg.getIdentity(), Descriptor.PONG, addresses).newZMsg();
                            reply.send(this.routerSock);
                        }
                        case Descriptor.QUERY -> {

                        }
                    }
                }
            }
        });
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
            ZMsg toSend = new UnidentifiedMessage("DROP", Collections.singletonList(this.routerSockIP)).newZMsg();
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

    private void ping() {
        for (String s : deadNeighbors) {
            this.neigh.remove(s);
        }

//        System.out.println("My Neighbors:");
//        for (Map.Entry<String, Integer> entry : this.neigh.entrySet()) {
//            System.out.println("\t- " + entry.getKey());
//        }
        List<String> pingArgs = List.of(this.id, "0", String.valueOf(Descriptor.PING_TTL), this.routerSockIP);
        Descriptor pingMsg = new Descriptor(Descriptor.PING, pingArgs);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        for (String neighborAdd : this.neigh.keySet()) {
            this.deadNeighbors.add(neighborAdd);
            executor.submit(() -> {
                ZMQ.Socket sock = zctx.createSocket(SocketType.REQ);
                if (!sock.connect(neighborAdd)) {
                    deadNeighbors.add(neighborAdd);
                    return;
                }
                try {
                    sock.connect(neighborAdd);
                } catch (Exception e) {
                    System.err.println("Failed to connect to endpoint!");
                }
                pingMsg.newZMsg().send(sock);

                UnidentifiedMessage reply = new UnidentifiedMessage(ZMsg.recvMsg(sock));

                if (!reply.getCmd().equals("PONG")) return;
//                System.out.println("Received PONG cmd");

                Set<String> unknownAdds = new HashSet<>();

                this.neigh.get(neighborAdd).nNeighbors = reply.getArgCount() - 1;

                for (int i = 0; i < reply.getArgCount(); ++i) {
                    String address = reply.getArg(i);
                    if (address.equals(this.routerSockIP))
                        continue;
                    if (!this.neigh.containsKey(address))
                        unknownAdds.add(address);
                }

                // TODO: only do this periodically
                for (String newNeighbor : unknownAdds) {
                    this.pickNeighborToDrop(newNeighbor);
                }

                this.deadNeighbors.remove(neighborAdd);
            });
        }
    }
}
