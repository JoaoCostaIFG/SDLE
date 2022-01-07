package org.t3.g11.proj2.nuttela.message;

import com.google.common.hash.BloomFilter;
import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;

public class MyNeighMessage extends GnuMessage {
    public static int REJECT = -1; // it doesn't want to become a neighbor

    private final int id;
    private final int n_neighbors;
    private final int capacity;
    private final BloomFilter<String> bloomFilter;

    public MyNeighMessage(InetSocketAddress addr, int n_neighbors, int capacity, int id, BloomFilter<String> bloomFilter) {
        super(GnuNodeCMD.MYNEIGH, addr);
        this.n_neighbors = n_neighbors;
        this.capacity = capacity;
        this.id = id;
        this.bloomFilter = bloomFilter;
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

    public BloomFilter<String> getBloomFilter() { return this.bloomFilter; }
}
