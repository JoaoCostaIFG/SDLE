package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;

public class NumNeighMessage extends GnuMessage {
    private final int nNeighbors;

    public NumNeighMessage(InetSocketAddress addr, int n_neighbors) {
        super(GnuNodeCMD.NUMNEIGH, addr);
        this.nNeighbors = n_neighbors;
    }

    public int getNeighbors() {
        return this.nNeighbors;
    }
}
