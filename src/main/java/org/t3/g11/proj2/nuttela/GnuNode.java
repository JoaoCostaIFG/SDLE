package org.t3.g11.proj2.nuttela;

import org.t3.g11.proj2.nuttela.message.*;
import org.t3.g11.proj2.peer.Peer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

public class GnuNode implements Runnable {
    public static final int RECEIVETIMEOUT = 5000;
    public static final int PING_FREQ = 5;
    public static final int UPDATE_FREQ = 5;
    public static final int MAX_NEIGH = 2;
    public static final int HYSTERESIS_FACTOR = 1;

    private final Integer id;

    private final ConcurrentHashMap<Integer, List<Integer>> sentTo;
    private final ConcurrentHashMap<Integer, GnuNodeInfo> neighbors;
    private final CopyOnWriteArraySet<InetSocketAddress> hostsCache;

    private final InetSocketAddress addr;
    private final int capacity;
    private final ExecutorService executors;
    private final ExecutorService timeouts;
    private final ServerSocket serverSocket;
    private final Peer peer;

    public GnuNode(Peer peer, int id, String address, int port) throws IOException {
        this.peer = peer;
        this.id = id; // TODO hash username

        this.neighbors = new ConcurrentHashMap<>();
        this.sentTo = new ConcurrentHashMap<>();
        this.hostsCache = new CopyOnWriteArraySet<>();

        int max_reqs = Runtime.getRuntime().availableProcessors() + 1;
        this.executors = Executors.newFixedThreadPool(max_reqs);
        this.timeouts = Executors.newCachedThreadPool();
        this.capacity = (int) ((Math.random() * (10 - 2)) + 2);

        this.addr = new InetSocketAddress(address, port);
        this.serverSocket = new ServerSocket(this.addr.getPort(), 100, this.addr.getAddress());
    }

    public static int IdFromName(String name) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(name.getBytes());
            ByteBuffer wrapped = ByteBuffer.wrap(hash);
            return wrapped.getInt();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return -1;
        }
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
            if (reply.getNeighbors() == MyNeighMessage.REJECT) return false;

            if (this.neighbors.size() + 1 <= GnuNode.MAX_NEIGH) {
                // we have room
                msg = new NeighMessage(this.addr, this.id, this.neighbors.size(), this.capacity);
                oss.writeObject(msg);
                this.neighbors.put(reply.getId(), new GnuNodeInfo(reply.getNeighbors(), reply.getCapacity(), reply.getAddr()));
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
                    GnuMessage dropReply = (GnuMessage) dropOis.readObject();

                    if (dropReply.getCmd() != GnuNodeCMD.DROPOK) {
                        NeighMessage nm = new NeighMessage(this.addr, this.id, -1, this.capacity);
                        oss.writeObject(nm);
                        return false;
                    }

                    this.neighbors.remove(highestNeigh);
                    this.neighbors.put(reply.getId(), new GnuNodeInfo(reply.getNeighbors(), reply.getCapacity(), reply.getAddr()));
                    msg = new NeighMessage(this.addr, this.id, this.neighbors.size(), this.capacity);
                    oss.writeObject(msg);
                    return true;
                } else {
                    NeighMessage nm = new NeighMessage(this.addr, this.id, -1, this.capacity);
                    oss.writeObject(nm);
                }
            }
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("Failed to connect to endpoint!");
            return false;
        }
        return false;
    }

    public void query(QueryMessage qm) {
        if (!this.sentTo.containsKey(qm.getGuid()))
            this.sentTo.put(qm.getGuid(), new ArrayList<>());
        int nTries = 0;

        // TODO incremental sleep retries
        while (nTries < 5) {
            for (Map.Entry<Integer, GnuNodeInfo> neighbour : this.neighbors.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(n -> n.capacity))).toList()) {

                if (this.sentTo.get(qm.getGuid()).contains(neighbour.getKey())) continue;
                this.sentTo.get(qm.getGuid()).add(neighbour.getKey());

                // TODO it would be nice to have more checks here
                if (neighbour.getValue().state == 0) continue;
                try (Socket sendSkt = new Socket(neighbour.getValue().address.getAddress(), neighbour.getValue().address.getPort())) {
                    ObjectOutputStream oss = new ObjectOutputStream(sendSkt.getOutputStream());
                    oss.writeObject(qm);
                    // wait for ack
                    ObjectInputStream iss = new ObjectInputStream(sendSkt.getInputStream());
                    iss.readObject();
                    return;
                } catch (Exception e) {
                    System.err.println("Couldn't connect to neighbor " + neighbour.getKey());
                }
            }
            nTries++;
            this.sentTo.get(qm.getGuid()).clear();
        }
    }

    public void updatePosts() {
        for (String sub : peer.getSubs()) {
            try {
                this.query(new QueryMessage(this.addr, this.id, new Query(sub)));
            } catch (Exception e) {
                System.err.println("Problem getting info about user: " + sub);
            }
        }
    }

    // TODO: PONG caching
    private void ping() {
        // TODO DEBUG purposes
        System.out.println("Neighboors:");
        for (Map.Entry<Integer, GnuNodeInfo> neigh : this.neighbors.entrySet()) {
            System.out.println("\t" + neigh.getKey() + " - " + neigh.getValue().state);
        }

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
                // try to receive the reply
                InputStream is = sockPing.getInputStream();
                ObjectInputStream iss = new ObjectInputStream(is);
                reply = (PongMessage) iss.readObject();
            } catch (ClassNotFoundException | IOException exception) {
                System.err.println("Failed to connect to " + e.getKey());
                peerNode.setDead();
                continue;
            }

            // process the reply
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

        ScheduledExecutorService queryScheduler = Executors.newSingleThreadScheduledExecutor();
        queryScheduler.scheduleAtFixedRate(this::updatePosts, 1, UPDATE_FREQ, TimeUnit.SECONDS);

        while (!Thread.currentThread().isInterrupted()) {
            Socket reqSocket;
            try {
                reqSocket = this.serverSocket.accept();
                ObjectInputStream ois = new ObjectInputStream(reqSocket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(reqSocket.getOutputStream());
                // handle request (the thread is now responsible for the socket)
                this.executors.execute(() -> this.handleMessage(ois, oos));
            } catch (IOException e) {
                System.err.println("Receiving failed with exception:");
                e.printStackTrace();
            }
        }

        pingScheduler.shutdownNow();
    }

    public void handleMessage(ObjectInputStream ois, ObjectOutputStream oos) {
        GnuMessage reqMsg;
        try {
            reqMsg = (GnuMessage) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Invalid message");
            e.printStackTrace();
            return;
        }
        //System.out.println("RECEIVED " + reqMsg.getCmd() + " CMD");

        switch (reqMsg.getCmd()) {
            case PING -> this.handlePing(oos, reqMsg);
            case NUMNEIGH -> this.handleNumNeigh(ois, oos, (NumNeighMessage) reqMsg);
            case NEIGH -> this.handleNeigh((NeighMessage) reqMsg);
            case DROP -> this.handleDrop(oos, (DropMessage) reqMsg);
            case QUERY -> this.handleQuery(oos, (QueryMessage) reqMsg);
            case QUERYHIT -> this.handleQueryHit((QueryHitMessage) reqMsg);
        }

        try {
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleQuery(ObjectOutputStream oos, QueryMessage reqMsg) {
        try {
            GnuMessage ackMsg = GnuNodeCMD.ACK.getMessage(this.addr);
            oos.writeObject(ackMsg);
        } catch (Exception e) {
            System.err.println("Couldn't connect to query relayer peer");
            e.printStackTrace();
            return;
        }

        // TODO this is only searching by ID
        if (reqMsg.getQuery().getQueryString().equals(this.peer.getPeerData().getSelfUsername())) {
            try (Socket sendSkt = new Socket(reqMsg.getAddr().getAddress(), reqMsg.getAddr().getPort())) {
                ObjectOutputStream oss = new ObjectOutputStream(sendSkt.getOutputStream());

                List<Result> results = new ArrayList<>();
                for (HashMap<String, String> post : this.peer.getSelfPeerPosts()) {
                    results.add(new Result(Integer.parseInt(post.get("timestamp")),
                            post.get("ciphered"), post.get("author")));
                }

                QueryHitMessage qhm = new QueryHitMessage(this.addr, reqMsg.getGuid(), results);
                oss.writeObject(qhm);
            } catch (Exception e) {
                System.err.println("Couldn't connect to initiator peer");
                e.printStackTrace();
            }
        } else {
            if (reqMsg.decreaseTtl() != 0)
                this.query(reqMsg);
        }
    }

    private void handleQueryHit(QueryHitMessage reqMsg) {
        List<Result> hitPosts = reqMsg.getResultSet();
        for (Result post : hitPosts) {
            String content;
            try {
                content = peer.decypherText(post.ciphered, post.author);
            } catch (Exception e) {
                System.err.println("Got a post from someone that does not exist (does not have keys)");
                e.printStackTrace();
                continue;
            }

            try {
                peer.getPeerData().addPost(post.author, content, post.ciphered);
            } catch (SQLException throwables) {
                System.err.println(":C");
                throwables.printStackTrace();
            }

        }
    }

    /**
     * Got a ping so we send a PONG.
     */
    private void handlePing(ObjectOutputStream oos, GnuMessage reqMsg) {
        // craft reply
        List<InetSocketAddress> addresses = new ArrayList<>(List.of(this.addr));
        for (Map.Entry<Integer, GnuNodeInfo> entry : this.neighbors.entrySet()) {
            InetSocketAddress eAddr = entry.getValue().address;
            if (!reqMsg.getAddr().equals(eAddr)) addresses.add(eAddr);
        }
        GnuMessage replyMsg = new PongMessage(this.addr, addresses);

        try {
            // reply to the same socket
            oos.writeObject(replyMsg);
        } catch (IOException e) {
            System.err.println("Ping handling failed.");
            e.printStackTrace();
        }
    }

    private void handleNumNeigh(ObjectInputStream ois, ObjectOutputStream oos, NumNeighMessage msg) {
        GnuMessage reply;
        NeighMessage neighReply;

        GnuNodeInfo maxEntry = null;
        for (Map.Entry<Integer, GnuNodeInfo> entry : this.neighbors.entrySet()) {
            if (maxEntry == null ||
                    (entry.getValue().nNeighbors > maxEntry.nNeighbors)) {
                maxEntry = entry.getValue();
            }
        }

        try {
            if (maxEntry == null || this.neighbors.size() < GnuNode.MAX_NEIGH || maxEntry.nNeighbors > msg.getNeighbors() + GnuNode.HYSTERESIS_FACTOR) {
                reply = new MyNeighMessage(this.addr, this.neighbors.size(),
                        this.capacity, this.id);
                oos.writeObject(reply);

            } else {
                reply = new MyNeighMessage(this.addr, -1,
                        this.capacity, this.id);
                oos.writeObject(reply);
                return;
            }
            neighReply = (NeighMessage) ois.readObject();
            if (neighReply.getNeighbors() == -1) return;
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("NUMNEIGH handling failed.");
            e.printStackTrace();
            return;
        }

        this.handleNeigh(neighReply);
    }

    private void handleNeigh(NeighMessage neighReply) {
        if (this.neighbors.size() < GnuNode.MAX_NEIGH) {
            this.neighbors.put(neighReply.getId(),
                    new GnuNodeInfo(neighReply.getNeighbors(), neighReply.getCapacity(), neighReply.getAddr()));
            return;
        }

        Map.Entry<Integer, GnuNodeInfo> maxEntry = null;
        for (Map.Entry<Integer, GnuNodeInfo> entry : this.neighbors.entrySet()) {
            if (maxEntry == null ||
                    (entry.getValue().nNeighbors > maxEntry.getValue().nNeighbors)) {
                maxEntry = entry;
            }
        }

        if (maxEntry.getValue().nNeighbors > neighReply.getNeighbors() + 1) {
            DropMessage dropMessage = new DropMessage(this.addr, this.id);
            GnuMessage reply;
            try (Socket socket = new Socket(maxEntry.getValue().address.getAddress(), maxEntry.getValue().address.getPort())) {
                ObjectOutputStream oss = new ObjectOutputStream(socket.getOutputStream());
                oss.writeObject(dropMessage);

                ObjectInputStream iss = new ObjectInputStream(socket.getInputStream());
                reply = (GnuMessage) iss.readObject();
            } catch (ClassNotFoundException | IOException e) {
                System.err.println("NEIGH handling failed.");
                return;
            }

            if (reply.getCmd() == GnuNodeCMD.DROPOK) {
                this.neighbors.remove(maxEntry.getKey());
                this.neighbors.put(neighReply.getId(),
                        new GnuNodeInfo(neighReply.getNeighbors(), neighReply.getCapacity(), neighReply.getAddr()));
            }
        }
    }

    private void handleDrop(ObjectOutputStream oos, DropMessage reqMsg) {
        GnuMessage reply;
        try {
            if (this.neighbors.size() != 1) {
                reply = GnuNodeCMD.DROPOK.getMessage(this.addr);
                oos.writeObject(reply);
                this.neighbors.remove(reqMsg.getId());
            } else {
                reply = GnuNodeCMD.DROPERR.getMessage(this.addr);
                oos.writeObject(reply);
            }
        } catch (IOException e) {
            System.err.println("DROP handling failed.");
        }
    }
}
