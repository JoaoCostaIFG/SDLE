package org.t3.g11.proj2.nuttela;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class HostsCacheInfo implements Serializable {
    public boolean isAlive;
    public InetSocketAddress address;
    public int capacity;

    public HostsCacheInfo(boolean isAlive, InetSocketAddress address, int capacity) {
        this.isAlive = isAlive;
        this.address = address;
        this.capacity = capacity;
    }
}
