package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;

public class MyNeighMessage extends GnuMessage {
    public static int REJECT = -1; // it doesn't want to become a neighbor

    private int id;
    private int n_neighbors;
    private int capacity;

    public MyNeighMessage(InetSocketAddress addr, int n_neighbors, int capacity, int id) {
        super(GnuNodeCMD.MYNEIGH, addr);
        this.n_neighbors = n_neighbors;
        this.capacity = capacity;
        this.id = id;
    }

    public int getNeighbors() {
        return this.n_neighbors;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public int getId() {
        return this.id;
    }
}
