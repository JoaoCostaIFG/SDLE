package org.t3.g11.proj2.nuttela;

import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.hash.BloomFilter;
import org.t3.g11.proj2.nuttela.message.PongMessage;
import org.t3.g11.proj2.nuttela.message.Query;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class GnuNodeInfo {
    public static final int DEAD = -1;
    public static final int MAYBE_DEAD = 0;
    public static final int ALIVE = 1;
    public static final int DETERMINING = 2;

    public static final int BLOOMSIZE = 500;
    public static final float BLOOMMISSCHANCE = 0.01f;

    private final MinMaxPriorityQueue<QueuedQuery> queuedQuerries;

    public int id;
    public int nNeighbors;
    public int capacity;
    public InetSocketAddress address;
    public BloomFilter<String> bloomFilter;
    public int state; // -1 - dead; 0 - maybe dead; 1 - alive; 2 - determining;

    public GnuNodeInfo(int id, int nNeighbors, int capacity, InetSocketAddress address, BloomFilter<String> bloomFilter) {
        this.id = id;
        this.nNeighbors = nNeighbors;
        this.capacity = capacity;
        this.state = 1;
        this.address = address;
        this.bloomFilter = bloomFilter;

        this.queuedQuerries = MinMaxPriorityQueue.create();
    }

    public void updateInfo(PongMessage pongMessage) {
        this.nNeighbors = pongMessage.getNNeighbors();
        this.address = pongMessage.getAddr();
        this.bloomFilter = pongMessage.getBloomFilter();
    }

    public int getId() {
        return this.id;
    }

    public boolean maybeDead() {
        return this.state == GnuNodeInfo.MAYBE_DEAD;
    }

    public boolean isDead() {
        return this.state == GnuNodeInfo.DEAD;
    }

    public boolean isAlive() {
        return this.state == GnuNodeInfo.ALIVE;
    }

    public void setDead() {
        if (this.state == GnuNodeInfo.DETERMINING)
            this.state = GnuNodeInfo.MAYBE_DEAD;
        else
            this.state = GnuNodeInfo.DEAD;
    }

    public void setAlive() {
        this.state = GnuNodeInfo.ALIVE;
    }

    public void setDetermining() {
        if (this.state == GnuNodeInfo.ALIVE) this.state = GnuNodeInfo.DETERMINING;
    }

    public InetAddress getInetAddr() {
        return this.address.getAddress();
    }

    public InetSocketAddress getAddr() {
        return this.address;
    }

    public int getPort() {
        return this.address.getPort();
    }

    public void queueQuery(Query query, double virtTime) {
        synchronized (this.queuedQuerries) {
            final int weight = this.capacity;
            final double prevFinishTag = this.queuedQuerries.peekLast().getFinishTag();
            this.queuedQuerries.add(new QueuedQuery(query, virtTime, prevFinishTag, weight, this.id));
        }
    }

    public QueuedQuery peekNextQuery() {
        synchronized (this.queuedQuerries) {
            return this.queuedQuerries.peekFirst();
        }
    }

    public QueuedQuery popNextQuery() {
        synchronized (this.queuedQuerries) {
            return this.queuedQuerries.poll();
        }
    }
}
