package org.t3.g11.proj2.nuttela;

import java.net.InetSocketAddress;

public class GnuNodeInfo {
    public int nNeighbors;
    public int capacity;
    public InetSocketAddress address;
    public int state; // 0 - dead; 1 - alive; 2 - determining;

    public GnuNodeInfo(int nNeighbors, int capacity, InetSocketAddress address) {
        this.nNeighbors = nNeighbors;
        this.capacity = capacity;
        this.state = 1;
        this.address = address;
    }

    public GnuNodeInfo(int nNeighbors) {
        this.nNeighbors = nNeighbors;
        this.capacity = 0;
    }

    public void setDead() {
        this.state = 0;
    }

    public void setAlive() {
        this.state = 1;
    }

    public void setDetermining() {
        this.state = 2;
    }
}
