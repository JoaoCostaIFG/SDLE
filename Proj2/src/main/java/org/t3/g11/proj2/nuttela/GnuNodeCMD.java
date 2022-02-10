package org.t3.g11.proj2.nuttela;

import org.t3.g11.proj2.nuttela.message.GnuMessage;

import java.net.InetSocketAddress;

public enum GnuNodeCMD {
    NEIGH, // id, address, n_neigh, capacity
    NUMNEIGH,
    MYNEIGH, // n_neigh
    DROPOK,
    DROPERR,
    DROP, // id, address
    PING,
    PONG,
    QUERY,
    QUERYHIT;

    public GnuMessage getMessage(InetSocketAddress addr) {
        return new GnuMessage(this, addr);
    }
}
