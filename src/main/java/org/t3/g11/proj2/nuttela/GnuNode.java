package org.t3.g11.proj2.nuttela;

import org.t3.g11.proj2.keyserver.BootstrapGnuNode;
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
import java.util.*;
import java.util.concurrent.*;

public class GnuNode implements Runnable {
    public static final int RECEIVETIMEOUT = 5000;
    public static final int PING_FREQ = 5;
    public static final int DROP_DEAD_FREQ = 10; //To only drop neighbors after 2 failed pings
    public static final int MAX_NEIGH = 2;
    public static final int HYSTERESIS_FACTOR = 1;

    protected final Integer id;

    protected final ConcurrentHashMap<Integer, Set<Integer>> sentTo; // guid => neighbors
    protected final ConcurrentHashMap<Integer, GnuNodeInfo> neighbors;
    protected final CopyOnWriteArraySet<InetSocketAddress> hostsCache;

    protected final InetSocketAddress addr;
    protected final int capacity;
    protected final ExecutorService executors;
    protected final ExecutorService timeouts;
    protected final ServerSocket serverSocket;
    protected final Peer peer;
    protected final int maxNeigh;

    public GnuNode(Peer peer, int id, InetSocketAddress addr, int maxNeigh) throws IOException {
        this.peer = peer;
        this.id = id; // TODO hash username
        this.addr = addr;
        this.maxNeigh = maxNeigh;

        this.neighbors = new ConcurrentHashMap<>();
        this.sentTo = new ConcurrentHashMap<>();
        this.hostsCache = new CopyOnWriteArraySet<>();

        int max_reqs = Runtime.getRuntime().availableProcessors() + 1;
        this.executors = Executors.newFixedThreadPool(max_reqs);
        this.timeouts = Executors.newCachedThreadPool();
        this.capacity = (int) ((Math.random() * (10 - 2)) + 2);

        this.serverSocket = new ServerSocket(this.addr.getPort(), 100, this.addr.getAddress());
    }

    public GnuNode(Peer peer, int id, String address, int port) throws IOException {
        this(peer, id, new InetSocketAddress(address, port), GnuNode.MAX_NEIGH);
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

    protected void bootstrap(InetSocketAddress bootstrapEndPoint) {
        this.pickNeighborToDrop(bootstrapEndPoint);
    }

    protected void bootstrap() {
        this.pickNeighborToDrop(BootstrapGnuNode.NODEENDPOINT);
    }

    protected void dropNeigh(GnuNodeInfo toDrop) {
        try (Socket dropSocket = new Socket(toDrop.address.getAddress(), toDrop.address.getPort())) {
            ObjectOutputStream oos = new ObjectOutputStream(dropSocket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(dropSocket.getInputStream());

            oos.writeObject(new DropMessage(this.addr, this.id));
            GnuMessage dropReply = (GnuMessage) ois.readObject();

            // we can temporarily go over the neighbor limit
            if (dropReply.getCmd() == GnuNodeCMD.DROPOK) {
                this.neighbors.remove(toDrop.getId());
            }
        } catch (ClassNotFoundException | IOException e) {
            this.neighbors.remove(toDrop.getId());
            System.err.println("Failed to connect to endpoint for drop!");
        }
    }

    /**
     * --->> NumNeigh
     * <<--- MyNeigh
     * --->> Neigh
     * <<--- ACK
     */
    public boolean pickNeighborToDrop(InetSocketAddress newNeighAddr) {
        try (Socket socket = new Socket(newNeighAddr.getAddress(), newNeighAddr.getPort())) {
            ObjectOutputStream oss = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream iss = new ObjectInputStream(socket.getInputStream());

            // ask if peer wants to become neighbor
            oss.writeObject(new NumNeighMessage(this.addr, this.neighbors.size()));
            MyNeighMessage reply = (MyNeighMessage) iss.readObject(); // wait reply
            if (reply.getNeighbors() == MyNeighMessage.REJECT) return false; // check if it wants to be our neighbor

            if (this.neighbors.size() + 1 <= this.maxNeigh) {
                // we have room
                oss.writeObject(new NeighMessage(this.addr, this.id, this.neighbors.size(), this.capacity));
                this.neighbors.put(reply.getId(),
                        new GnuNodeInfo(reply.getId(), reply.getNeighbors(), reply.getCapacity(), reply.getAddr()));
                return true;
            }

            // need to drop a neighbor -> filter neighbors with lower capacity -> get the one with the msot neighbors
            List<Map.Entry<Integer, GnuNodeInfo>> dropCandidates = this.neighbors.entrySet().stream()
                    .filter(e -> e.getValue().capacity < reply.getCapacity()).toList();
            if (dropCandidates.isEmpty()) { // reject Y
                oss.writeObject(new NeighMessage(this.addr, this.id, -1, this.capacity));
                return false;
            }
            GnuNodeInfo toDrop =
                    Collections.max(dropCandidates, Comparator.comparingInt(e -> e.getValue().nNeighbors)).getValue();
            // also need the neighbor with the most capacity
            GnuNodeInfo maxCap =
                    Collections.max(this.neighbors.entrySet().stream().toList(), Comparator.comparingInt(e -> e.getValue().capacity)).getValue();
            // test to accept
            if (reply.getCapacity() > maxCap.capacity ||
                    toDrop.nNeighbors > reply.getNeighbors() + GnuNode.HYSTERESIS_FACTOR) {
                // accept Y
                this.neighbors.put(reply.getId(), new GnuNodeInfo(reply.getId(), reply.getNeighbors(), reply.getCapacity(), reply.getAddr()));
                oss.writeObject(new NeighMessage(this.addr, this.id, this.neighbors.size(), this.capacity));
                this.dropNeigh(toDrop);
                return true;
            }

            // otherwise just reject Y
            oss.writeObject(new NeighMessage(this.addr, this.id, -1, this.capacity));
            return false;
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("Failed to connect to endpoint for neigh!");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * --->> Query
     * <<--- ACK
     */
    public void query(QueryMessage qm) {
        Set<Integer> neighSentTo;
        if (this.sentTo.containsKey(qm.getGuid())) {
            neighSentTo = this.sentTo.get(qm.getGuid());
        } else {
            neighSentTo = new HashSet<>();
            this.sentTo.put(qm.getGuid(), neighSentTo);
        }

        // TODO incremental sleep retries
        for (int nTries = 0; nTries < 5; ++nTries) {
            for (Map.Entry<Integer, GnuNodeInfo> neighbour : this.neighbors.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(n -> n.capacity))).toList()) {

                int neighId = neighbour.getKey();
                GnuNodeInfo neighInfo = neighbour.getValue();
                // if already sent
                if (!neighSentTo.add(neighId)) continue;

                // TODO tick thingy
                if (neighInfo.isDead()) continue;
                try (Socket sendSkt = new Socket(neighInfo.getInetAddr(), neighInfo.getPort())) {
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
            // clear the container so we retry the guys
            neighSentTo.clear();
        }
    }

    public void query(String sub) {
        this.query(new QueryMessage(this.addr, this.id, this.addr, sub));
    }

    /**
     * --->> Ping
     * <<--- Pong
     */
    // TODO: PONG caching
    protected void ping() {
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
                // try to receive the reply (Pong)
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

        ScheduledExecutorService dropDeadHostsScheduler = Executors.newSingleThreadScheduledExecutor();
        dropDeadHostsScheduler.scheduleAtFixedRate(this::dropDeadHosts, 12, DROP_DEAD_FREQ, TimeUnit.SECONDS);

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

    private void dropDeadHosts() {
        List<Integer> toDrop = new ArrayList<>();
        for (Map.Entry<Integer, GnuNodeInfo> entry : this.neighbors.entrySet()) {
            if (entry.getValue().state == 0) {
                toDrop.add(entry.getKey());
            }
        }

        for (Integer drop : toDrop) {
            this.hostsCache.remove(this.neighbors.get(drop).getAddr());
            this.neighbors.remove(drop);
        }
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

    /**
     * --->> Ack
     */
    protected void handleQuery(ObjectOutputStream oos, QueryMessage reqMsg) {
        try {
            GnuMessage ackMsg = GnuNodeCMD.ACK.getMessage(this.addr);
            oos.writeObject(ackMsg);
        } catch (Exception e) {
            System.err.println("Couldn't connect to query relayer peer");
            e.printStackTrace();
            return;
        }

        // TODO this is only searching by username
        if (reqMsg.getQuery().getQueryString().equals(this.peer.getPeerData().getSelfUsername())) {
            try (Socket sendSkt = new Socket(reqMsg.getInetAddr(), reqMsg.getPort())) {
                ObjectOutputStream oss = new ObjectOutputStream(sendSkt.getOutputStream());

                List<Result> results = new ArrayList<>();
                for (HashMap<String, String> post : this.peer.getSelfPeerPosts()) {
                    results.add(new Result(Long.parseLong(post.get("timestamp")),
                            post.get("ciphered"), post.get("author")));
                }

                QueryHitMessage qhm = new QueryHitMessage(this.addr, reqMsg.getGuid(), results);
                oss.writeObject(qhm);
            } catch (Exception e) {
                System.err.println("Couldn't connect to initiator peer");
                e.printStackTrace();
            }
        } else {
            if (reqMsg.decreaseTtl() != 0) {
                reqMsg.setAddr(this.addr); // update source address
                this.query(reqMsg);
            }
        }
    }

    /**
     * <<--- QueryHit
     */
    protected void handleQueryHit(QueryHitMessage reqMsg) {
        List<Result> hitPosts = reqMsg.getResultSet();
        for (Result post : hitPosts) {
            try {
                String content = this.peer.decypherText(post.ciphered, post.author);
                this.peer.getPeerData().addPost(post.author, content, post.ciphered, post.date);
            } catch (Exception e) {
                System.err.println("Got a post from someone that does not exist (does not have keys)");
                e.printStackTrace();
            }
        }
    }

    /**
     * <<--- Ping
     * --->> Pong
     */
    protected void handlePing(ObjectOutputStream oos, GnuMessage reqMsg) {
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

    /**
     * <<--- NumNeigh
     * --->> MyNeigh
     */
    protected void handleNumNeigh(ObjectInputStream ois, ObjectOutputStream oos, NumNeighMessage msg) {
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
            if (maxEntry == null || this.neighbors.size() < this.maxNeigh || maxEntry.nNeighbors > msg.getNeighbors() + GnuNode.HYSTERESIS_FACTOR) {
                reply = new MyNeighMessage(this.addr, this.neighbors.size(),
                        this.capacity, this.id);
                oos.writeObject(reply);

            } else {
                reply = new MyNeighMessage(this.addr, -1, this.capacity, this.id);
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

    /**
     * <<--- Neigh
     */
    protected void handleNeigh(NeighMessage neighReply) {
        // TODO might need to ACK sender

        if (this.neighbors.size() < this.maxNeigh) {
            this.neighbors.put(neighReply.getId(),
                    new GnuNodeInfo(neighReply.getId(), neighReply.getNeighbors(), neighReply.getCapacity(), neighReply.getAddr()));
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
            try (Socket socket = new Socket(maxEntry.getValue().getInetAddr(), maxEntry.getValue().getPort())) {
                ObjectOutputStream oss = new ObjectOutputStream(socket.getOutputStream());
                oss.writeObject(dropMessage);

                ObjectInputStream iss = new ObjectInputStream(socket.getInputStream());
                reply = (GnuMessage) iss.readObject();
            } catch (ClassNotFoundException | IOException e) {
                System.err.println("NEIGH handling failed.");
                return;
            }

            // we can temporarily go over the neighbor limit
            this.neighbors.put(neighReply.getId(),
                    new GnuNodeInfo(neighReply.getId(), neighReply.getNeighbors(), neighReply.getCapacity(), neighReply.getAddr()));
            if (reply.getCmd() == GnuNodeCMD.DROPOK) {
                this.neighbors.remove(maxEntry.getKey());
            }
        }
    }

    /**
     * <<--- Drop
     * --->> DropOk OR DropErr
     */
    protected void handleDrop(ObjectOutputStream oos, DropMessage reqMsg) {
        GnuMessage reply;
        try {
            if (this.neighbors.size() > 1) {
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
