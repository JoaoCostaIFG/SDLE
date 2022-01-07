package org.t3.g11.proj2.nuttela.message;

import com.google.common.hash.BloomFilter;
import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;
import java.util.List;

public class PongMessage extends GnuMessage {
    protected final List<InetSocketAddress> addrs;
    protected int capacity;
    protected BloomFilter<String> bloomFilter;

    public PongMessage(InetSocketAddress addr, List<InetSocketAddress> addrs, int capacity, BloomFilter<String> bloomFilter) {
        super(GnuNodeCMD.PONG, addr);
        this.addrs = addrs;
        this.capacity = capacity;
        this.bloomFilter = bloomFilter;
    }

    public List<InetSocketAddress> getNeighAddrs() {
        return this.addrs;
    }

    public int getNNeighbors() {
        return this.addrs.size();
    }

    public int getCapacity() {
        return capacity;
    }

    public BloomFilter<String> getBloomFilter() {
        return bloomFilter;
    }
}
