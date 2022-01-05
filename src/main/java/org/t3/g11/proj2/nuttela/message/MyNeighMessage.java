package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;

public class MyNeighMessage extends GnuMessage {
    private int n_neighbors;

    public MyNeighMessage(InetSocketAddress addr, int n_neighbors) {
        super(GnuNodeCMD.MYNEIGH, addr);
        this.n_neighbors = n_neighbors;
    }

    public int getNeighbors() {
        return this.n_neighbors;
    }
}
