package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;

public class GnuIdMessage extends GnuMessage {
    protected int id;

    public GnuIdMessage(GnuNodeCMD cmd, InetSocketAddress addr, int id) {
        super(cmd, addr);
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
