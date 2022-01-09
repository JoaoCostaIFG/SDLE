package org.t3.g11.proj2.nuttela;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.util.concurrent.AtomicDouble;
import org.t3.g11.proj2.nuttela.message.*;
import org.t3.g11.proj2.nuttela.message.query.Query;
import org.t3.g11.proj2.nuttela.message.query.TagQuery;
import org.t3.g11.proj2.nuttela.message.query.UserQuery;
import org.t3.g11.proj2.peer.PeerObserver;

import java.io.IOException;
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
    public static final int MAX_TOPOLOGY_FREQ = 10;
    public static final int TOPOLOGY_AGGRESSIVENESS = 64;
    public static final int MIN_NEIGH = 1;
    public static final int MAX_NEIGH = 2;
    public static final int HYSTERESIS_FACTOR = 1;

    protected final ConcurrentHashMap<Integer, Set<Integer>> sentTo; // guid => neighbors
    protected final ConcurrentHashMap<Integer, GnuNodeInfo> neighbors;
    protected final CopyOnWriteArraySet<HostsCacheInfo> hostsCache;

    protected final Integer id;
    protected final InetSocketAddress addr;
    protected final int capacity;
    protected final ExecutorService executors;
    protected final ExecutorService timeouts;
    protected final ScheduledExecutorService checkTopologyScheduler;
    protected final ServerSocket serverSocket;
    protected final int maxNeigh;

    protected BloomFilter<String> bloomFilter;
    private int bloomFilterCapacity = GnuNodeInfo.BLOOMSIZE;
    private PeerObserver peerObserver = null;

    private final Semaphore querySemaphore = new Semaphore(0, true);
    private AtomicDouble maxFinishTagServed = new AtomicDouble(0.0);
    private AtomicDouble currentStartTag = new AtomicDouble(-1.0);

    public GnuNode(int id, InetSocketAddress addr, int maxNeigh, int capacity) throws IOException {
        this.id = id; // TODO hash username
        this.addr = addr;
        this.maxNeigh = maxNeigh;
        this.capacity = capacity;

        // should be wrapped to grow
        this.buildBloom(Collections.emptySet());
        this.neighbors = new ConcurrentHashMap<>();
        this.sentTo = new ConcurrentHashMap<>();
        this.hostsCache = new CopyOnWriteArraySet<>();

        int max_reqs = Runtime.getRuntime().availableProcessors() + 1;
        this.executors = Executors.newFixedThreadPool(max_reqs);
        this.timeouts = Executors.newCachedThreadPool();
        this.checkTopologyScheduler = Executors.newSingleThreadScheduledExecutor();

        this.serverSocket = new ServerSocket(this.addr.getPort(), 100,
                this.addr.getAddress());
    }


    public GnuNode(int id, InetSocketAddress addr) throws IOException {
        this(id, addr, GnuNode.MAX_NEIGH, (int) ((Math.random() * (10 - 2)) + 2));
    }

    public void setObserver(PeerObserver peerObserver) {
        this.peerObserver = peerObserver;
    }

    public void addToBloom(String newEntry) {
        this.bloomFilter.put(newEntry);

        synchronized (this) {
            if (this.bloomFilter.approximateElementCount() > this.bloomFilterCapacity) {
                // needs to grow
                this.bloomFilterCapacity *= 2;

                BloomFilter<String> biggerFilter = BloomFilter.create(
                        Funnels.stringFunnel(StandardCharsets.UTF_8),
                        this.bloomFilterCapacity, GnuNodeInfo.BLOOMMISSCHANCE
                );

                biggerFilter.putAll(this.bloomFilter);
                this.bloomFilter = biggerFilter;
            }
        }
    }

    public void buildBloom(Set<String> subs) {
        BloomFilter<String> newBloom = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                this.bloomFilterCapacity, GnuNodeInfo.BLOOMMISSCHANCE);
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
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(dropSocket.getInputStream());

            oos.writeObject(new DropMessage(this.addr, this.id));
            oos.flush();
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
     * Returns true if connection was successful (alive). False otherwise.
     * --->> NumNeigh
     * <<--- MyNeigh
     * --->> Neigh
     * <<--- ACK
     */
    public boolean pickNeighborToDrop(InetSocketAddress newNeighAddr) {
        try (Socket socket = new Socket(newNeighAddr.getAddress(), newNeighAddr.getPort())) {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            // ask if peer wants to become neighbor
            oos.writeObject(new NumNeighMessage(this.addr, this.neighbors.size()));
            oos.flush();
            MyNeighMessage reply = (MyNeighMessage) ois.readObject(); // wait reply
            if (reply.getNeighbors() == MyNeighMessage.REJECT) return true; // check if it wants to be our neighbor

            if (this.neighbors.size() + 1 <= this.maxNeigh) {
                // we have room
                oos.writeObject(new NeighMessage(this.addr, this.id, this.neighbors.size(), this.capacity, this.bloomFilter));
                oos.flush();
                this.neighbors.put(reply.getId(),
                        new GnuNodeInfo(reply.getId(), reply.getNeighbors(), reply.getCapacity(), reply.getAddr(), reply.getBloomFilter()));
                return true;
            }

            // need to drop a neighbor -> filter neighbors with lower capacity -> get the one with the msot neighbors
            List<Map.Entry<Integer, GnuNodeInfo>> dropCandidates = this.neighbors.entrySet().stream()
                    .filter(e -> e.getValue().capacity < reply.getCapacity()).toList();
            if (dropCandidates.isEmpty()) { // reject Y
                oos.writeObject(new NeighMessage(this.addr, this.id, -1, this.capacity, this.bloomFilter));
                oos.flush();
                return true;
            }
            GnuNodeInfo toDrop =
                    Collections.max(dropCandidates, Comparator.comparingInt(e -> e.getValue().nNeighbors)).getValue();

            // also need the neighbor with the most capacity
            Optional<Map.Entry<Integer, GnuNodeInfo>> maxCap =
                    this.neighbors.entrySet().stream().max(Comparator.comparingInt(e -> e.getValue().capacity));

            if (maxCap.isPresent()) {
                // test to accept
                if (reply.getCapacity() > maxCap.get().getValue().capacity ||
                        toDrop.nNeighbors > reply.getNeighbors() + GnuNode.HYSTERESIS_FACTOR) {
                    // accept Y
                    this.neighbors.put(reply.getId(), new GnuNodeInfo(reply.getId(), reply.getNeighbors(), reply.getCapacity(), reply.getAddr(), reply.getBloomFilter()));
                    oos.writeObject(new NeighMessage(this.addr, this.id, this.neighbors.size(), this.capacity, this.bloomFilter));
                    oos.flush();
                    this.dropNeigh(toDrop);
                    return true;
                }
            }

            // otherwise just reject Y
            oos.writeObject(new NeighMessage(this.addr, this.id, -1, this.capacity, null));
            oos.flush();
            return true;
        } catch (ClassNotFoundException e) {
            System.err.println("Communication failed with neighbor!");
            e.printStackTrace();
            return true;
        } catch (IOException e) {
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
            List<Map.Entry<Integer, GnuNodeInfo>> sortedNeighs = new ArrayList<>(this.neighbors.entrySet());
            // neighbors are sorted by:
            // first - if they probably contain the content of the query
            // second - by its capacity
            sortedNeighs.sort(Map.Entry.comparingByValue(Comparator.comparingInt(n -> n.capacity)));
            Collections.reverse(sortedNeighs);
            // 0 and 1 swapped to get mightContain in descending order
            sortedNeighs.sort(Map.Entry.comparingByValue(Comparator.comparingInt(n ->
                    n.bloomFilter.mightContain(qm.getQuery().getQueryString()) ? 0 : 1)));

            for (Map.Entry<Integer, GnuNodeInfo> neighbour : sortedNeighs) {
                int neighId = neighbour.getKey();
                GnuNodeInfo neighInfo = neighbour.getValue();
                // if already sent
                if (!neighSentTo.add(neighId)) continue;

                if (neighInfo.maybeDead()) continue;
                try (Socket sendSkt = new Socket(neighInfo.getInetAddr(), neighInfo.getPort())) {
                    ObjectOutputStream oos = new ObjectOutputStream(sendSkt.getOutputStream());
                    oos.flush();
                    ObjectInputStream ois = new ObjectInputStream(sendSkt.getInputStream());
                    oos.writeObject(qm);
                    oos.flush();
                    return;
                } catch (Exception e) {
                    System.err.println("Couldn't connect to neighbor " + neighbour.getKey());
                }
            }
            // clear the container so we retry the guys
            neighSentTo.clear();
        }
    }

    public void query(Query query) {
        this.query(new QueryMessage(this.addr, this.id, query));
    }

    public Query genQueryUser(int neededHits, String queryString, long queryTimestamp) {
        return new UserQuery(this.addr, this.id, neededHits, queryString, queryTimestamp);
    }

    public Query genQueryTag(int neededHits, String queryString) {
        return new TagQuery(this.addr, this.id, neededHits, queryString);
    }

    /**
     * --->> Ping
     * <<--- Pong
     */
    protected void ping() {
        // TODO DEBUG debug purposes
        /*
        System.out.println("Neighboors:");
        for (Map.Entry<Integer, GnuNodeInfo> neigh : this.neighbors.entrySet()) {
            System.out.println("\t" + neigh.getKey() + " - " + neigh.getValue().state);
        }
         */

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
                ObjectOutputStream oos = new ObjectOutputStream(sockPing.getOutputStream());
                oos.flush();
                ObjectInputStream ois = new ObjectInputStream(sockPing.getInputStream());
                oos.writeObject(pingMsg);
                oos.flush();
                // try to receive the reply (Pong)
                reply = (PongMessage) ois.readObject();
            } catch (ClassNotFoundException | IOException exception) {
                peerNode.setDead();
                if (peerNode.isDead()) {
                    // peer is really dead
                    System.err.println("Failed to connect to " + e.getKey() + ". Is dead and not our neighbor anymore.");
                    this.neighbors.remove(e.getKey());
                } else {
                    System.err.println("Failed to connect to " + e.getKey() + ". May be dead.");
                }
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
        if (this.neighbors.size() < MIN_NEIGH) return 0.0;

        double satisfaction = 0.0;
        for (GnuNodeInfo neigh : this.neighbors.values()) {
            satisfaction += (float) neigh.capacity / neigh.nNeighbors;
        }
        satisfaction /= this.capacity;

        if (satisfaction > 1.0 || this.neighbors.size() >= GnuNode.MAX_NEIGH)
            return 1.0;
        return satisfaction;
    }

    protected void scheduleNextTopology(double satisfaction) {
        long delay = Math.round((MAX_TOPOLOGY_FREQ * Math.pow(TOPOLOGY_AGGRESSIVENESS, satisfaction - 1)) * 1000);
        this.checkTopologyScheduler.schedule(this::checkTopology, delay, TimeUnit.MILLISECONDS);
    }

    protected void checkTopology() {
        try {
            //System.out.println("Starting check topology");
            // clean up host caches
            this.hostsCache.removeIf(x -> !x.isAlive);
            double satisfaction = this.getSatisfaction();
            if (satisfaction < 1.0) {
                this.addNewNeighs();
            }
            // schedule next topology check with new delay according to satisfaction level
            this.scheduleNextTopology(satisfaction);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void addNewNeighs() {
        // get host cached that aren't our neighbors
        Set<InetSocketAddress> neighborsAdrr = new HashSet<>();
        for (GnuNodeInfo ni : this.neighbors.values())
            neighborsAdrr.add(ni.getAddr());
        List<HostsCacheInfo> possibleNeighbors = new ArrayList<>();
        for (HostsCacheInfo i : this.hostsCache) {
            if (!neighborsAdrr.contains(i.address))
                possibleNeighbors.add(i);
        }
        if (possibleNeighbors.isEmpty()) return;
        // select a small random subset of these possible neighbors from the cache
        Collections.shuffle(possibleNeighbors);
        possibleNeighbors = possibleNeighbors.subList(0, Math.min(possibleNeighbors.size(), 10));

        // get node with the most capacity grater than ours (if any)
        Optional<HostsCacheInfo> maxCapNodeOp = possibleNeighbors.stream().max(Comparator.comparingInt(x -> x.capacity));
        if (maxCapNodeOp.isPresent()) {
            HostsCacheInfo maxCapNode = maxCapNodeOp.get();
            if (!this.pickNeighborToDrop(maxCapNode.address))
                maxCapNode.isAlive = false;
            return;
        }
        // otherwise select random one
        HostsCacheInfo pairNode = possibleNeighbors.get(new Random().nextInt(possibleNeighbors.size()));
        if (!this.pickNeighborToDrop(pairNode.address))
            pairNode.isAlive = false;
    }

    /**
     * Server dispatcher loop: accepts connections and spawns a thread to handle them.
     */
    @Override
    public void run() {
        this.bootstrap();

        // schedule pings
        ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();
        pingScheduler.scheduleAtFixedRate(this::ping, 1, PING_FREQ, TimeUnit.SECONDS);
        // schedule query handling (fair-queued)
        ExecutorService queryExecutor = Executors.newSingleThreadExecutor();
        queryExecutor.execute(this::handleQueuedQueryLoop);
        // schedule topology adaptation
        this.scheduleNextTopology(this.getSatisfaction());

        while (!Thread.currentThread().isInterrupted()) {
            Socket reqSocket;
            try {
                reqSocket = this.serverSocket.accept();
                ObjectInputStream ois = new ObjectInputStream(reqSocket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(reqSocket.getOutputStream());
                oos.flush();
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
        //System.out.println("RECEIVED " + reqMsg);

        switch (reqMsg.getCmd()) {
            case PING -> this.handlePing(oos, reqMsg);
            case NUMNEIGH -> this.handleNumNeigh(ois, oos, (NumNeighMessage) reqMsg);
            case DROP -> this.handleDrop(oos, (DropMessage) reqMsg);
            case QUERY -> this.handleQuery((QueryMessage) reqMsg);
            case QUERYHIT -> this.handleQueryHit((QueryHitMessage) reqMsg);
        }

        try {
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
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
            oos.flush();
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
        GnuNodeInfo maxEntry = null;
        for (Map.Entry<Integer, GnuNodeInfo> entry : this.neighbors.entrySet()) {
            if (maxEntry == null ||
                    (entry.getValue().nNeighbors > maxEntry.nNeighbors)) {
                maxEntry = entry.getValue();
            }
        }

        try {
            if (maxEntry == null
                    || this.neighbors.size() < this.maxNeigh
                    || maxEntry.nNeighbors > msg.getNeighbors() + GnuNode.HYSTERESIS_FACTOR) {
                GnuMessage reply = new MyNeighMessage(this.addr, this.id, this.neighbors.size(),
                        this.capacity, this.bloomFilter);
                oos.writeObject(reply);
                oos.flush();
                NeighMessage neighReply = (NeighMessage) ois.readObject();
                if (neighReply.getNeighbors() == -1) return;
                this.handleNeigh(neighReply);
            } else {
                GnuMessage reply = new MyNeighMessage(this.addr, this.id, MyNeighMessage.REJECT,
                        this.capacity, null);
                oos.writeObject(reply);
                oos.flush();
            }
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("NUMNEIGH handling failed.");
            e.printStackTrace();
        }
    }

    /**
     * <<--- Neigh
     */
    protected void handleNeigh(NeighMessage neighReply) {
        GnuNodeInfo newNeighInfo = new GnuNodeInfo(neighReply.getId(), neighReply.getNeighbors(),
                neighReply.getCapacity(), neighReply.getAddr(), neighReply.getBloomFilter());
        if (this.neighbors.size() < this.maxNeigh) {
            this.neighbors.put(neighReply.getId(), newNeighInfo);
            return;
        }

        if (neighReply.getNeighbors() != NeighMessage.REJECT) {
            Map.Entry<Integer, GnuNodeInfo> maxEntry = null;
            for (Map.Entry<Integer, GnuNodeInfo> entry : this.neighbors.entrySet()) {
                if (maxEntry == null ||
                        (entry.getValue().nNeighbors > maxEntry.getValue().nNeighbors)) {
                    maxEntry = entry;
                }
            }

            if (maxEntry == null) {
                this.neighbors.put(neighReply.getId(), newNeighInfo);
                return;
            }

            DropMessage dropMessage = new DropMessage(this.addr, this.id);
            GnuMessage reply;
            try (Socket socket = new Socket(maxEntry.getValue().getInetAddr(), maxEntry.getValue().getPort())) {
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream iss = new ObjectInputStream(socket.getInputStream());
                oos.writeObject(dropMessage);
                oos.flush();
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
            if (this.neighbors.size() > GnuNode.MIN_NEIGH) {
                reply = GnuNodeCMD.DROPOK.getMessage(this.addr);
                this.neighbors.remove(reqMsg.getId());
            } else {
                reply = GnuNodeCMD.DROPERR.getMessage(this.addr);
            }
            oos.writeObject(reply);
            oos.flush();
        } catch (IOException e) {
            System.err.println("DROP handling failed.");
        }
    }

    /**
     * <<--- QueryMsg
     */
    protected void handleQuery(QueryMessage reqMsg) {
        Query query = reqMsg.getQuery();
        int neighId = reqMsg.getId();
        // queue query forwarding
        if (this.neighbors.containsKey(neighId)) {
            // TODO should be a single instruction
            double virtTime;
            double currentStartTag = this.currentStartTag.get();
            if (currentStartTag > 0.0) {
                // server is busy
                virtTime = currentStartTag;
            } else {
                // server is mimir
                virtTime = this.maxFinishTagServed.get();
            }
            this.neighbors.get(neighId).queueQuery(query, virtTime);
            this.querySemaphore.release();
        }
    }

    /**
     * <<--- QueryHit
     */
    protected void handleQueryHit(QueryHitMessage reqMsg) {
        List<Result> hitPosts = reqMsg.getResultSet();
        this.peerObserver.handleNewResults(reqMsg.getGuid(), hitPosts);
    }

    private void handleQueuedQuery(QueuedQuery queuedQuery) {
        Query query = queuedQuery.getQuery();
        // TODO this is only searching by username
        if (this.bloomFilter.mightContain(query.getQueryString()) && this.peerObserver != null) {
            // gather results
            List<Result> results = this.peerObserver.handleQuery(query);
            if (!results.isEmpty()) {
                // got a hit
                query.decreaseNeededHits(results.size());
                try (Socket sendSkt = new Socket(query.getSourceAddr(), query.getSourcePort())) {
                    ObjectOutputStream oss = new ObjectOutputStream(sendSkt.getOutputStream());
                    oss.flush();
                    ObjectInputStream ois = new ObjectInputStream(sendSkt.getInputStream());
                    QueryHitMessage qhm = new QueryHitMessage(this.addr, query.getGuid(), results);
                    oss.writeObject(qhm);
                    oss.flush();
                } catch (Exception e) {
                    System.err.println("Couldn't connect to initiator peer");
                    e.printStackTrace();
                }
            }
        }
        // maybe forward
        if (query.decreaseTtl() > 0 && query.getNeededHits() > 0) {
            // didn't get a hit (don't sub or result list is empty)
            if (!this.sentTo.containsKey(query.getGuid()))
                this.sentTo.put(query.getGuid(), new HashSet<>());
            this.sentTo.get(query.getGuid()).add(queuedQuery.getHopId());

            QueryMessage relayMsg = new QueryMessage(this.addr, this.id, query);
            this.query(relayMsg);
        }
    }

    private void handleQueuedQueryLoop() {
        try {
            while (!Thread.interrupted()) {
                this.querySemaphore.acquire();

                double earliest = -1.0;
                GnuNodeInfo frien = null;
                for (GnuNodeInfo i : this.neighbors.values()) {
                    QueuedQuery qq = i.peekNextQuery();
                    if (qq == null) continue;
                    if (frien == null || qq.getStartTag() < earliest) {
                        earliest = qq.getStartTag();
                        frien = i;
                    }
                }
                // very weird condition
                if (frien == null) {
                    System.err.println("Unexpected null when looking for the next query to handle.");
                    continue;
                }

                QueuedQuery queuedQuery = frien.popNextQuery();
                this.executors.execute(() -> this.handleQueuedQuery(queuedQuery));
            }
        } catch (InterruptedException e) {
            // bye bye
        }
    }
}
