package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;

public class DropMessage extends GnuMessage {
    private int id;

    public DropMessage(InetSocketAddress addr, int id) {
        super(GnuNodeCMD.DROP, addr);
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
