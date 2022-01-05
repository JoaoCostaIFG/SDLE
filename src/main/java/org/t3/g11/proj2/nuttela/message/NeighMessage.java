package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;

public class NeighMessage extends GnuMessage {
    private int id;
    private int n_neighbors;
    private int capacity;

    public NeighMessage(InetSocketAddress addr, int id, int n_neighbors, int capacity) {
        super(GnuNodeCMD.MYNEIGH, addr);
        this.id = id;
        this.n_neighbors = n_neighbors;
        this.capacity = capacity;
    }

    public int getId() {
        return this.id;
    }

    public int getNeighbors() {
        return this.n_neighbors;
    }

    public int getCapacity() {
        return this.capacity;
    }
}