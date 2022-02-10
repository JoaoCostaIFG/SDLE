package org.t3.g11.proj2.nuttela;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Objects;

public class HostsCacheInfo implements Serializable {
    public boolean isAlive;
    public InetSocketAddress address;
    public int capacity;

    public HostsCacheInfo(boolean isAlive, InetSocketAddress address, int capacity) {
        this.isAlive = isAlive;
        this.address = address;
        this.capacity = capacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostsCacheInfo that = (HostsCacheInfo) o;
        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}
