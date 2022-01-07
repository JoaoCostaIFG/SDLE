package org.t3.g11.proj2.nuttela;

import com.google.common.hash.BloomFilter;
import org.t3.g11.proj2.nuttela.message.PongMessage;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class GnuNodeInfo {
    public static final int DEAD = 0;
    public static final int ALIVE = 1;
    public static final int DETERMINING = 2;
    public static final float BLOOMMISSCHANCE = 0.01f;

    public int id;
    public int nNeighbors;
    public int capacity;
    public InetSocketAddress address;
    public BloomFilter<String> bloomFilter;
    public int state; // 0 - dead; 1 - alive; 2 - determining;

    public GnuNodeInfo(int id, int nNeighbors, int capacity, InetSocketAddress address, BloomFilter<String> bloomFilter) {
        this.id = id;
        this.nNeighbors = nNeighbors;
        this.capacity = capacity;
        this.state = 1;
        this.address = address;
        this.bloomFilter = bloomFilter;
    }

    public void updateInfo(PongMessage pongMessage) {
        this.nNeighbors = pongMessage.getNNeighbors();
        this.address = pongMessage.getAddr();
        this.bloomFilter = pongMessage.getBloomFilter();
    }

    public int getId() {
        return this.id;
    }

    public boolean isDead() {
        return this.state == GnuNodeInfo.DEAD;
    }

    public void setDead() {
        this.state = GnuNodeInfo.DEAD;
    }

    public void setAlive() {
        this.state = GnuNodeInfo.ALIVE;
    }

    public void setDetermining() {
        this.state = GnuNodeInfo.DETERMINING;
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
}
