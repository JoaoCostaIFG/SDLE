package org.t3.g11.proj2.nuttela;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class GnuNode implements Runnable {
    public static final int RECEIVETIMEOUT = 5000;
    public static final int PING_FREQ = 5;
    public static final int MAX_TOPOLOGY_FREQ = 5;
    public static final int TOPOLOGY_AGGRESSIVENESS = 1;
    public static final int MIN_NEIGH = 1;
    public static final int MAX_NEIGH = 2;
    public static final int HYSTERESIS_FACTOR = 1;

    protected final Integer id;

    protected final ConcurrentHashMap<Integer, Set<Integer>> sentTo; // guid => neighbors
    protected final ConcurrentHashMap<Integer, GnuNodeInfo> neighbors;
    protected final CopyOnWriteArraySet<HostsCacheInfo> hostsCache;

    protected final InetSocketAddress addr;
    protected final int capacity;
    protected final ExecutorService executors;
    protected final ExecutorService timeouts;
    protected final ScheduledExecutorService checkTopologyScheduler;
    protected final ServerSocket serverSocket;
    protected final Peer peer;
    protected final int maxNeigh;

    protected BloomFilter<String> bloomFilter;

    public GnuNode(Peer peer, int id, InetSocketAddress addr, int maxNeigh) throws IOException {
        this.peer = peer;
        this.id = id; // TODO hash username
        this.addr = addr;
        this.maxNeigh = maxNeigh;
        this.buildBloom(Collections.emptySet());

        this.neighbors = new ConcurrentHashMap<>();
        this.sentTo = new ConcurrentHashMap<>();
        this.hostsCache = new CopyOnWriteArraySet<>();

        int max_reqs = Runtime.getRuntime().availableProcessors() + 1;
        this.executors = Executors.newFixedThreadPool(max_reqs);
        this.timeouts = Executors.newCachedThreadPool();
        this.checkTopologyScheduler = Executors.newSingleThreadScheduledExecutor();
        this.capacity = (int) ((Math.random() * (10 - 2)) + 2);

        this.serverSocket = new ServerSocket(this.addr.getPort(), 100, this.addr.getAddress());
    }

    public void addToBloom(String newEntry) {
        this.bloomFilter.put(newEntry);
    }

    public void buildBloom(Set<String> subs) {
        BloomFilter<String> newBloom = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                GnuNodeInfo.BLOOMSIZE, GnuNodeInfo.BLOOMMISSCHANCE);
        for (String s : subs) {
            newBloom.put(s);
        }
        this.bloomFilter = newBloom;
    }

    protected void bootstrap(InetSocketAddress bootstrapEndPoint) {
        this.pickNeighborToDrop(bootstrapEndPoint);
    }

    protected void bootstrap() {
        this.bootstrap(BootstrapGnuNode.NODEENDPOINT);
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
     * Returns true if connection w
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
                oss.writeObject(new NeighMessage(this.addr, this.id, this.neighbors.size(), this.capacity, this.bloomFilter));
                this.neighbors.put(reply.getId(),
                        new GnuNodeInfo(reply.getId(), reply.getNeighbors(), reply.getCapacity(), reply.getAddr(), reply.getBloomFilter()));
                return true;
            }

            // need to drop a neighbor -> filter neighbors with lower capacity -> get the one with the msot neighbors
            List<Map.Entry<Integer, GnuNodeInfo>> dropCandidates = this.neighbors.entrySet().stream()
                    .filter(e -> e.getValue().capacity < reply.getCapacity()).toList();
            if (dropCandidates.isEmpty()) { // reject Y
                oss.writeObject(new NeighMessage(this.addr, this.id, -1, this.capacity, this.bloomFilter));
                return true;
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
                this.neighbors.put(reply.getId(), new GnuNodeInfo(reply.getId(), reply.getNeighbors(), reply.getCapacity(), reply.getAddr(), reply.getBloomFilter()));
                oss.writeObject(new NeighMessage(this.addr, this.id, this.neighbors.size(), this.capacity, this.bloomFilter));
                this.dropNeigh(toDrop);
                return true;
            }

            // otherwise just reject Y
            oss.writeObject(new NeighMessage(this.addr, this.id, -1, this.capacity, null));
            return true;
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
                if (neighInfo.maybeDead()) continue;
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
            if (peerNode.state == GnuNodeInfo.ALIVE)
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
            this.hostsCache.addAll(reply.getNeighAddrs());
            // update node info
            peerNode.updateInfo(reply);
        }
    }

    protected double getSatisfaction() {
        if (this.neighbors.size() < MIN_NEIGH) return 0;

        double satLevel = 0;
        for (GnuNodeInfo neigh : this.neighbors.values()) {
            satLevel += ((float) neigh.capacity / neigh.nNeighbors);
        }
        satLevel /= this.capacity;

        if (satLevel > 1.0 || this.neighbors.size() >= MAX_NEIGH)
            return 1;

        return satLevel;
    }

    private void dropDeadHosts() {
        List<Integer> toDrop = new ArrayList<>();
        for (Map.Entry<Integer, GnuNodeInfo> entry : this.neighbors.entrySet()) {
            if (entry.getValue().isDead()) {
                toDrop.add(entry.getKey());
            }
        }

        for (Integer drop : toDrop) {
            this.neighbors.remove(drop);
        }
    }

    protected void checkTopology() {
        System.out.println("Starting check topology");
        this.dropDeadHosts(); // cleanup host ca
        double satisfaction = this.getSatisfaction();
        if (satisfaction < 1.0) {
            this.addNewNeighs();
        }
        // schedule next topology check with new delay according to satisfaction level
        long delay = Math.round((MAX_TOPOLOGY_FREQ * Math.pow(TOPOLOGY_AGGRESSIVENESS, satisfaction - 1)) * 1000);
        this.checkTopologyScheduler.schedule(this::checkTopology, delay, TimeUnit.MILLISECONDS);
    }

    protected void addNewNeighs() {
        Set<InetSocketAddress> neighborsAdrr = new HashSet<>();
        for (GnuNodeInfo ni : this.neighbors.values())
            neighborsAdrr.add(ni.getAddr());
        List<HostsCacheInfo> possibleNeighbors =
                this.hostsCache.stream().filter(x -> !neighborsAdrr.contains(x.address)).toList();
        Collections.shuffle(possibleNeighbors);
        possibleNeighbors = possibleNeighbors.subList(0, 10);
        System.out.println(possibleNeighbors);
        HostsCacheInfo highestCap = Collections.max(possibleNeighbors, Comparator.comparingInt(e -> (e.capacity)));
        if (highestCap.capacity < this.capacity) {
            Random rand = new Random();
            highestCap = possibleNeighbors.get(rand.nextInt(possibleNeighbors.size()));
        }
        this.pickNeighborToDrop(highestCap.address);
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
        // schedule topology adaptation
        this.checkTopologyScheduler.schedule(this::checkTopology, 5, TimeUnit.SECONDS);

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
                    results.add(new Result(Integer.parseInt(post.get("guid")), Long.parseLong(post.get("timestamp")),
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
                this.peer.getPeerData().addPost(post.author, post.guid, content, post.ciphered, post.date);
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
        List<HostsCacheInfo> addresses = new ArrayList<>(List.of(new HostsCacheInfo(true, this.addr, this.capacity)));
        for (Map.Entry<Integer, GnuNodeInfo> entry : this.neighbors.entrySet()) {
            InetSocketAddress eAddr = entry.getValue().address;
            if (!reqMsg.getAddr().equals(eAddr))
                addresses.add(new HostsCacheInfo(!entry.getValue().isDead(), entry.getValue().address, entry.getValue().capacity));
        }
        // reply to the same socket
        GnuMessage pongMsg = new PongMessage(this.addr, addresses, this.capacity, this.bloomFilter);
        try {
            oos.writeObject(pongMsg);
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
                        this.capacity, this.id, this.bloomFilter);
                oos.writeObject(reply);

            } else {
                reply = new MyNeighMessage(this.addr, -1, this.capacity, this.id, null);
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

        GnuNodeInfo newNeighInfo = new GnuNodeInfo(neighReply.getId(), neighReply.getNeighbors(),
                neighReply.getCapacity(), neighReply.getAddr(), neighReply.getBloomFilter());
        if (this.neighbors.size() < this.maxNeigh) {
            this.neighbors.put(neighReply.getId(), newNeighInfo);
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
            this.neighbors.put(neighReply.getId(), newNeighInfo);
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
