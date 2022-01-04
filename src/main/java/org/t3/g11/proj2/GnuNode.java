package org.t3.g11.proj2;

import org.t3.g11.proj2.message.Descriptor;
import org.t3.g11.proj2.message.IdentifiedMessage;
import org.t3.g11.proj2.message.UnidentifiedMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    private final HashMap<String, Integer> neigh;
    private final String id;
    private final ZContext zctx;
    private final ZMQ.Socket dealerSock;
    private final ZMQ.Socket routerSock;
    private final String routerSockIP;
    private List<String> deadNeighbors;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public GnuNode(String id, String dealerIP, String routerIP) {
        this.id = id;
        deadNeighbors = new ArrayList<>();
        this.zctx = new ZContext();
        this.dealerSock = zctx.createSocket(SocketType.DEALER);
        this.routerSock = zctx.createSocket(SocketType.ROUTER);
        this.dealerSock.bind(dealerIP);
        this.routerSockIP = routerIP;
        this.routerSock.bind(routerIP);
        this.neigh = new HashMap<>();

        this.bootstrap();

        scheduler.scheduleAtFixedRate(this::ping, 1, PING_FREQ, TimeUnit.SECONDS);
    }

    public boolean pickNeighborToDrop(String newNeigh) {
        ZMsg msg = new UnidentifiedMessage(GnuNode.NUMNEIGH, new ArrayList<>()).newZMsg();
        ZMQ.Socket skt = this.zctx.createSocket(SocketType.REQ);
        skt.connect(newNeigh);
        msg.send(skt);
        UnidentifiedMessage reply = new UnidentifiedMessage(ZMsg.recvMsg(skt));

        int n_neigh = Integer.parseInt(reply.getArg(0));

        if (this.neigh.size() + 1 <= GnuNode.MAX_NEIGH) {
            // we have room
            ZMsg neighMsg = new UnidentifiedMessage(GnuNode.NEIGH, Arrays.asList(this.routerSockIP, String.valueOf(this.neigh.size()))).newZMsg();
            neighMsg.send(skt);
            UnidentifiedMessage um = new UnidentifiedMessage(ZMsg.recvMsg(skt));
            skt.close();
            if (um.getCmd().equals(GnuNode.NEIGHOK)) {
                this.neigh.put(newNeigh, n_neigh);
                return true;
            } else {
                return false;
            }

        } else {
            String highestNeigh = Collections.max(neigh.entrySet(),
                    Comparator.comparingInt(Map.Entry::getValue)).getKey();
            System.out.println("Highest: " + this.neigh.get(highestNeigh) + "  Local: " + (n_neigh + 1));
            if (this.neigh.get(highestNeigh) > n_neigh + 1) {
                ZMsg neighMsg = new UnidentifiedMessage(GnuNode.NEIGH, Arrays.asList(this.routerSockIP, String.valueOf(this.neigh.size()))).newZMsg();
                neighMsg.send(skt);
                UnidentifiedMessage um = new UnidentifiedMessage(ZMsg.recvMsg(skt));
                skt.close();
                if (um.getCmd().equals(GnuNode.NEIGHOK)) {
                    this.neigh.remove(highestNeigh);
                    this.neigh.put(newNeigh, n_neigh);
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

    public void send(String toSend) {
//        ZMsg msg = new ZMsg();
//        msg.add("Ola");
//        System.out.println("sending");
//        msg.send(this.dealerSock);
//        System.out.println("sent");
//        ZMsg replyZMsg = ZMsg.recvMsg(this.dealerSock);
//        System.out.println(replyZMsg.toString());
    }

    public String receive() {

        ZMQ.Poller plr = zctx.createPoller(2);
        plr.register(this.routerSock, ZMQ.Poller.POLLIN);
        plr.register(this.dealerSock, ZMQ.Poller.POLLIN);

        while (plr.poll() >= 0) {
            if (plr.pollin(0)) {
                IdentifiedMessage msg = new IdentifiedMessage(ZMsg.recvMsg(this.routerSock));
                String type = msg.getCmd();
                ZMsg reply;
                System.out.println("RECEIVED " + type + " CMD");
                switch (type) {
                    case GnuNode.NUMNEIGH -> {
                        reply = new IdentifiedMessage(msg.getIdentity(), GnuNode.MYNEIGH, Collections.singletonList(String.valueOf(this.neigh.size()))).newZMsg();
                        reply.send(this.routerSock);
                    }
                    case GnuNode.NEIGH -> {

                        boolean neighOk = this.handleNeigh(msg);
                        if (neighOk) {
                            reply = new IdentifiedMessage(msg.getIdentity(), GnuNode.NEIGHOK, new ArrayList<>()).newZMsg();
                            reply.send(this.routerSock);
                            this.neigh.put(msg.getArg(0), Integer.parseInt(msg.getArg(1)));
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
                }
            }
        }
        return "";
    }

    private boolean handleDrop() {
        return this.neigh.size() != 1;
    }

    private boolean handleNeigh(UnidentifiedMessage um) {
        if (this.neigh.size() < GnuNode.MAX_NEIGH)
            return true;

        Map.Entry<String, Integer> maxEntry = null;
        for (Map.Entry<String, Integer> entry : this.neigh.entrySet()) {
            if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
                maxEntry = entry;
            }
        }
        if (maxEntry.getValue() > Integer.parseInt(um.getArg(1)) + 1) {
            ZMQ.Socket skt = this.zctx.createSocket(SocketType.REQ);
            skt.connect(maxEntry.getKey());
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

        System.out.println("My Neighbors:");
        for (Map.Entry<String, Integer> entry : this.neigh.entrySet()) {
            System.out.println("\t- " + entry.getKey());
        }
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
                sock.connect(neighborAdd);
                pingMsg.newZMsg().send(sock);

                UnidentifiedMessage reply = new UnidentifiedMessage(ZMsg.recvMsg(sock));

                if (!reply.getCmd().equals("PONG")) return;
                System.out.println("Received PONG cmd");

                Set<String> unknownAdds = new HashSet<>();
                this.neigh.put(neighborAdd, reply.getArgCount() - 1);
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
