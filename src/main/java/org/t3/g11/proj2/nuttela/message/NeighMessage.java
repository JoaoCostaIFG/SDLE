package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;

public class NeighMessage extends GnuMessage {
    public static int REJECT = -1; // it doesn't want to become a neighbor

    private int id;
    private int n_neighbors; // if equal to -1, means that neigh message is rejected
    private int capacity;

    public NeighMessage(InetSocketAddress addr, int id, int n_neighbors, int capacity) {
        super(GnuNodeCMD.NEIGH, addr);
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