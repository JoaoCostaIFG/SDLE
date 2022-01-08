package org.t3.g11.proj2.nuttela.message;

import com.google.common.hash.BloomFilter;
import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;

public class MyNeighMessage extends GnuIdMessage {
    public static int REJECT = -1; // it doesn't want to become a neighbor

    private final int n_neighbors;
    private final int capacity;
    private final BloomFilter<String> bloomFilter;

    public MyNeighMessage(InetSocketAddress addr, int id, int nNeighbors, int capacity, BloomFilter<String> bloomFilter) {
        super(GnuNodeCMD.MYNEIGH, addr, id);
        this.n_neighbors = nNeighbors;
        this.capacity = capacity;
        this.bloomFilter = bloomFilter;
    }

    public int getNeighbors() {
        return this.n_neighbors;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public BloomFilter<String> getBloomFilter() {
        return this.bloomFilter;
    }
}
