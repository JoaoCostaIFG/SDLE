package org.t3.g11.proj2.nuttela;

public class GnuNodeInfo {
    public int nNeighbors;
    public int capacity;

    public GnuNodeInfo(int nNeighbors, int capacity) {
        this.nNeighbors = nNeighbors;
        this.capacity = capacity;
    }

    public GnuNodeInfo(int nNeighbors) {
        this.nNeighbors = nNeighbors;
        this.capacity = 0;
    }
}
