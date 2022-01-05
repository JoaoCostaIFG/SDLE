package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;
import java.util.List;

public class PongMessage extends GnuMessage {
    protected final List<InetSocketAddress> addrs;

    public PongMessage(InetSocketAddress addr, List<InetSocketAddress> addrs) {
        super(GnuNodeCMD.PONG, addr);
        this.addrs = addrs;
    }

    public List<InetSocketAddress> getNeighAddrs() {
        return this.addrs;
    }
}
